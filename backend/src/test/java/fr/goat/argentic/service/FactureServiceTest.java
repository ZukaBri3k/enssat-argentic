package fr.goat.argentic.service;

import fr.goat.argentic.dto.FactureRequest;
import fr.goat.argentic.dto.FactureResponse;
import fr.goat.argentic.dto.PaiementFactureRequest;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.verifyNoInteractions;

import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.model.ClientEntreprise;
import fr.goat.argentic.model.ClientParticulier;
import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link FactureService} (règles métier de facturation, docs/DECISIONS.md).
 */
@ExtendWith(MockitoExtension.class)
class FactureServiceTest {

    private static final BigDecimal TVA_DEFAUT = new BigDecimal("20.00");

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private FactureService factureService;

    private Specialite specialite;
    private Photographe photographe;
    private ClientParticulier clientParticulier;
    private ClientEntreprise clientEntreprise;
    private Seance seanceTerminee;

    @BeforeEach
    void setUp() {
        specialite = new Specialite("Mariage et fêtes");
        specialite.setId(2L);

        photographe = new Photographe("Lucas Bernard", "lucas.bernard@argentic.fr", Set.of(specialite));
        photographe.setId(10L);

        clientParticulier = new ClientParticulier(
                "marie.dupont@mail.fr",
                "12 rue de Kerampont, 22300 Lannion",
                "Dupont",
                "Marie"
        );
        clientParticulier.setId(20L);

        clientEntreprise = new ClientEntreprise(
                "contact@atelier-breizh.fr",
                "15 rue de la Poterie, 22300 Lannion",
                "Atelier Breizh SARL",
                "FR40123456789"
        );
        clientEntreprise.setId(21L);

        seanceTerminee = new Seance(
                photographe,
                clientParticulier,
                specialite,
                LocalDate.of(2026, 11, 10),
                LocalTime.of(14, 0),
                120,
                "Château de Rosanbo"
        );
        seanceTerminee.setId(1L);
        seanceTerminee.setStatut(StatutSeance.TERMINEE);
    }

    // ==================== Génération, consultation et copie figée ====================

    @Test
    @DisplayName("genererFacture pour un ClientEntreprise fige raison sociale, adresse, email et SIREN/TVA et arrondit le TTC HALF_UP")
    void genererFacture_clientEntrepriseEtArrondiHalfUp_doitFigerCoordonneesEtCalculerTtc() {
        seanceTerminee.setClient(clientEntreprise);
        int anneeCourante = LocalDate.now().getYear();
        FactureRequest request = new FactureRequest(new BigDecimal("99.99"), new BigDecimal("20.00"));

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(false);
        when(factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc("FAC-" + anneeCourante + "-"))
                .thenReturn(Optional.empty());
        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> {
            Facture facture = inv.getArgument(0);
            facture.setId(41L);
            return facture;
        });

        FactureResponse res = factureService.genererFacture(1L, request);

        // Modification ultérieure du client et du photographe : la copie figée reste inchangée
        clientEntreprise.setRaisonSociale("Nouvelle Raison SAS");
        clientEntreprise.setNumeroSirenTva("FR00000000000");
        photographe.setNom("Photographe Modifié");

        assertThat(res.coordonneesClient().identite()).isEqualTo("Atelier Breizh SARL");
        assertThat(res.coordonneesClient().adressePostale()).isEqualTo("15 rue de la Poterie, 22300 Lannion");
        assertThat(res.coordonneesClient().email()).isEqualTo("contact@atelier-breizh.fr");
        assertThat(res.coordonneesClient().numeroSirenTva()).isEqualTo("FR40123456789");
        assertThat(res.prestation().nomPhotographe()).isEqualTo("Lucas Bernard");
        // 99.99 * 1.20 = 119.988 -> arrondi HALF_UP à 119.99
        assertThat(res.montantTtc()).isEqualByComparingTo("119.99");
    }

    private Facture factureAvecNumero(Seance seance, String numero, LocalDate dateEmission) {
        return new Facture(
                seance,
                numero,
                dateEmission,
                new BigDecimal("250.00"),
                TVA_DEFAUT,
                CoordonneesFacturation.depuis(clientParticulier),
                PrestationFacturee.depuis(seance)
        );
    }

    @Test
    @DisplayName("genererFacture crée une facture EN_ATTENTE pour une séance TERMINEE sans facture et retourne le DTO complet")
    void genererFacture_seanceTermineeSansFacture_doitCreerFactureEnAttenteEtRetournerDto() {
        int anneeCourante = LocalDate.now().getYear();
        FactureRequest request = new FactureRequest(new BigDecimal("250.00"), new BigDecimal("20.00"));

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(false);
        when(factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc("FAC-" + anneeCourante + "-"))
                .thenReturn(Optional.empty());
        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> {
            Facture facture = inv.getArgument(0);
            facture.setId(42L);
            return facture;
        });

        FactureResponse res = factureService.genererFacture(1L, request);

        assertThat(res.id()).isEqualTo(42L);
        assertThat(res.numero()).isEqualTo("FAC-" + anneeCourante + "-0001");
        assertThat(res.seanceId()).isEqualTo(1L);
        assertThat(res.dateEmission()).isEqualTo(LocalDate.now());
        assertThat(res.montantHt()).isEqualByComparingTo("250.00");
        assertThat(res.tauxTva()).isEqualByComparingTo("20.00");
        assertThat(res.montantTtc()).isEqualByComparingTo("300.00");
        assertThat(res.statutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(res.datePaiement()).isNull();
        assertThat(res.coordonneesClient().identite()).isEqualTo("Dupont Marie");
        assertThat(res.coordonneesClient().adressePostale()).isEqualTo("12 rue de Kerampont, 22300 Lannion");
        assertThat(res.coordonneesClient().email()).isEqualTo("marie.dupont@mail.fr");
        assertThat(res.coordonneesClient().numeroSirenTva()).isNull();
        assertThat(res.prestation().date()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(res.prestation().heureDebut()).isEqualTo(LocalTime.of(14, 0));
        assertThat(res.prestation().dureeMinutes()).isEqualTo(120);
        assertThat(res.prestation().typeSeance()).isEqualTo("Mariage et fêtes");
        assertThat(res.prestation().nomPhotographe()).isEqualTo("Lucas Bernard");

        verify(factureRepository).save(any(Facture.class));
    }

    @Test
    @DisplayName("genererFacture applique le taux de TVA par défaut de 20.00 % lorsque tauxTva est null")
    void genererFacture_sansTauxTvaFourni_doitAppliquerTauxParDefaut20() {
        int anneeCourante = LocalDate.now().getYear();
        FactureRequest request = new FactureRequest(new BigDecimal("400.00"), null);

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(false);
        when(factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc("FAC-" + anneeCourante + "-"))
                .thenReturn(Optional.empty());
        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> {
            Facture facture = inv.getArgument(0);
            facture.setId(43L);
            return facture;
        });

        FactureResponse res = factureService.genererFacture(1L, request);

        assertThat(res.tauxTva()).isEqualByComparingTo("20.00");
        assertThat(res.montantHt()).isEqualByComparingTo("400.00");
        assertThat(res.montantTtc()).isEqualByComparingTo("480.00");
        assertThat(res.statutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
    }

    @Test
    @DisplayName("genererFacture incrémente la séquence à max + 1 sur les factures existantes de la même année")
    void genererFacture_avecFacturesExistantesMemeAnnee_doitIncrementerSequenceMaxPlus1() {
        int anneeCourante = LocalDate.now().getYear();
        Facture existante = factureAvecNumero(
                seanceTerminee,
                "FAC-" + anneeCourante + "-0014",
                LocalDate.of(anneeCourante, 5, 12)
        );
        FactureRequest request = new FactureRequest(new BigDecimal("100.00"), new BigDecimal("5.50"));

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(false);
        when(factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc("FAC-" + anneeCourante + "-"))
                .thenReturn(Optional.of(existante));
        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> {
            Facture facture = inv.getArgument(0);
            facture.setId(44L);
            return facture;
        });

        FactureResponse res = factureService.genererFacture(1L, request);

        assertThat(res.numero()).isEqualTo("FAC-" + anneeCourante + "-0015");
        assertThat(res.montantTtc()).isEqualByComparingTo("105.50");
    }

    @ParameterizedTest(name = "statut={0}")
    @EnumSource(value = StatutSeance.class, names = {"PLANIFIEE", "CONFIRMEE", "ANNULEE"})
    @DisplayName("genererFacture lève RegleMetierException (409) si la séance n'est pas TERMINEE")
    void genererFacture_seanceNonTerminee_doitLeverRegleMetierException(StatutSeance statutNonTermine) {
        seanceTerminee.setStatut(statutNonTermine);
        FactureRequest request = new FactureRequest(new BigDecimal("250.00"), new BigDecimal("20.00"));

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));

        assertThatThrownBy(() -> factureService.genererFacture(1L, request))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("TERMINEE");

        verify(factureRepository, never()).save(any());
    }

    @Test
    @DisplayName("genererFacture lève RegleMetierException (409) si la séance possède déjà une facture (même ANNULEE)")
    void genererFacture_seanceAyantDejaUneFacture_doitLeverRegleMetierException() {
        FactureRequest request = new FactureRequest(new BigDecimal("250.00"), new BigDecimal("20.00"));

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seanceTerminee));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> factureService.genererFacture(1L, request))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("facture");

        verify(factureRepository, never()).save(any());
    }

    @Test
    @DisplayName("genererFacture lève ResourceNotFoundException (404) si la séance n'existe pas")
    void genererFacture_seanceInexistante_doitLeverResourceNotFoundException() {
        FactureRequest request = new FactureRequest(new BigDecimal("250.00"), new BigDecimal("20.00"));

        when(seanceRepository.getSeanceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.genererFacture(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verifyNoInteractions(factureRepository);
    }

    @Test
    @DisplayName("getFactures retourne les factures filtrées par statut et période d'émission")
    void getFactures_avecFiltresStatutEtPeriode_doitRetournerFacturesFiltrees() {
        LocalDate du = LocalDate.of(2026, 11, 1);
        LocalDate au = LocalDate.of(2026, 11, 30);
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.getFactures(StatutPaiement.EN_ATTENTE, du, au))
                .thenReturn(List.of(facture));

        List<FactureResponse> res = factureService.getFactures(StatutPaiement.EN_ATTENTE, du, au);

        assertThat(res).hasSize(1);
        FactureResponse dto = res.get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.numero()).isEqualTo("FAC-2026-0001");
        assertThat(dto.statutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(dto.montantHt()).isEqualByComparingTo("250.00");
        assertThat(dto.montantTtc()).isEqualByComparingTo("300.00");
        assertThat(dto.coordonneesClient().identite()).isEqualTo("Dupont Marie");
        assertThat(dto.prestation().nomPhotographe()).isEqualTo("Lucas Bernard");

        verify(factureRepository).getFactures(StatutPaiement.EN_ATTENTE, du, au);
    }

    @Test
    @DisplayName("getFactures lève RegleMetierException (409) si du est postérieure à au")
    void getFactures_duApresAu_doitLeverRegleMetierException() {
        LocalDate du = LocalDate.of(2026, 12, 31);
        LocalDate au = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> factureService.getFactures(null, du, au))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("postérieure");

        verifyNoInteractions(factureRepository);
    }

    @Test
    @DisplayName("getFactureById retourne le détail complet de la facture existante")
    void getFactureById_existante_doitRetournerFactureResponse() {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        FactureResponse res = factureService.getFactureById(10L);

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.numero()).isEqualTo("FAC-2026-0001");
        assertThat(res.seanceId()).isEqualTo(1L);
        assertThat(res.montantHt()).isEqualByComparingTo("250.00");
        assertThat(res.tauxTva()).isEqualByComparingTo("20.00");
        assertThat(res.montantTtc()).isEqualByComparingTo("300.00");
        assertThat(res.statutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(res.coordonneesClient().identite()).isEqualTo("Dupont Marie");
        assertThat(res.prestation().typeSeance()).isEqualTo("Mariage et fêtes");
    }

    @Test
    @DisplayName("getFactureById lève ResourceNotFoundException (404) si la facture est introuvable")
    void getFactureById_inexistante_doitLeverResourceNotFoundException() {
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.getFactureById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ==================== Paiement et annulation (#14) ====================

    @Test
    @DisplayName("enregistrerPaiement passe une facture EN_ATTENTE à PAYEE avec la date de paiement saisie")
    void enregistrerPaiement_factureEnAttente_doitPasserAPayeeAvecDatePaiement() {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);
        LocalDate datePaiement = LocalDate.of(2026, 11, 15);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        FactureResponse res = factureService.enregistrerPaiement(10L, new PaiementFactureRequest(datePaiement));

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.statutPaiement()).isEqualTo(StatutPaiement.PAYEE);
        assertThat(res.datePaiement()).isEqualTo(datePaiement);
    }

    @ParameterizedTest(name = "statut={0}")
    @EnumSource(value = StatutPaiement.class, names = {"PAYEE", "ANNULEE"})
    @DisplayName("enregistrerPaiement lève RegleMetierException (409) si la facture n'est pas EN_ATTENTE")
    void enregistrerPaiement_factureNonEnAttente_doitLeverRegleMetierException(StatutPaiement statut) {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);
        facture.setStatutPaiement(statut);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> factureService.enregistrerPaiement(10L, new PaiementFactureRequest(LocalDate.of(2026, 11, 15))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("EN_ATTENTE");

        assertThat(facture.getStatutPaiement()).isEqualTo(statut);
        assertThat(facture.getDatePaiement()).isNull();
    }

    @Test
    @DisplayName("enregistrerPaiement lève DonneeInvalideException (400) si la date de paiement est absente")
    void enregistrerPaiement_sansDatePaiement_doitLeverDonneeInvalideException() {
        assertThatThrownBy(() -> factureService.enregistrerPaiement(10L, new PaiementFactureRequest(null)))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("obligatoire");

        verifyNoInteractions(factureRepository);
    }

    @Test
    @DisplayName("enregistrerPaiement lève RegleMetierException (409) si la date de paiement précède la date d'émission")
    void enregistrerPaiement_datePaiementAvantEmission_doitLeverRegleMetierException() {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> factureService.enregistrerPaiement(10L, new PaiementFactureRequest(LocalDate.of(2026, 11, 10))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("émission");

        assertThat(facture.getStatutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
    }

    @Test
    @DisplayName("enregistrerPaiement lève RegleMetierException (409) si la date de paiement est dans le futur")
    void enregistrerPaiement_datePaiementFuture_doitLeverRegleMetierException() {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> factureService.enregistrerPaiement(10L, new PaiementFactureRequest(LocalDate.now().plusDays(1))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("futur");

        assertThat(facture.getStatutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
    }

    @Test
    @DisplayName("enregistrerPaiement lève ResourceNotFoundException (404) si la facture est introuvable")
    void enregistrerPaiement_factureInexistante_doitLeverResourceNotFoundException() {
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.enregistrerPaiement(999L, new PaiementFactureRequest(LocalDate.of(2026, 11, 15))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("annulerFacture passe une facture EN_ATTENTE à ANNULEE sans date de paiement")
    void annulerFacture_factureEnAttente_doitPasserAAnnulee() {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        FactureResponse res = factureService.annulerFacture(10L);

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.statutPaiement()).isEqualTo(StatutPaiement.ANNULEE);
        assertThat(res.datePaiement()).isNull();
    }

    @ParameterizedTest(name = "statut={0}")
    @EnumSource(value = StatutPaiement.class, names = {"PAYEE", "ANNULEE"})
    @DisplayName("annulerFacture lève RegleMetierException (409) si la facture n'est pas EN_ATTENTE")
    void annulerFacture_factureNonEnAttente_doitLeverRegleMetierException(StatutPaiement statut) {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);
        facture.setStatutPaiement(statut);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        assertThatThrownBy(() -> factureService.annulerFacture(10L))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("EN_ATTENTE");

        assertThat(facture.getStatutPaiement()).isEqualTo(statut);
    }

    @Test
    @DisplayName("annulerFacture lève ResourceNotFoundException (404) si la facture est introuvable")
    void annulerFacture_factureInexistante_doitLeverResourceNotFoundException() {
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.annulerFacture(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
