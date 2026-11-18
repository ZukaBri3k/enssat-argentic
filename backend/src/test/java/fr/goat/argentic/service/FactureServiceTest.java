package fr.goat.argentic.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import fr.goat.argentic.dto.FacturePdf;
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
 * Tests unitaires des règles métier de facturation (section « Facturation » de docs/DECISIONS.md).
 *
 * <p>Ce fichier est structuré en deux parties :
 * <ul>
 *   <li><strong>Partie 1 (active)</strong> : vérifie immédiatement sur le domaine et les repositories
 *   mockés l'ensemble des règles métier (éligibilité TERMINEE, unicité 1-1 même si ANNULEE,
 *   numérotation FAC-YYYY-NNNN, copie figée client/prestation, statut initial et calcul TTC/arrondi).</li>
 *   <li><strong>Partie 2 (prête à décommenter)</strong> : suite complète de tests Mockito ciblant
 *   {@code FactureService} ({@code genererFacture}, {@code getFactures}, {@code getFactureById})
 *   dès l'implémentation de la couche service et des DTOs {@code FactureRequest}/{@code FactureResponse}.</li>
 * </ul>
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

    // ==================== PARTIE 1 : Tests unitaires actifs des règles métier ====================

    @ParameterizedTest(name = "statut={0}")
    @EnumSource(StatutSeance.class)
    @DisplayName("1. Seule une séance TERMINEE est facturable (PLANIFIEE, CONFIRMEE et ANNULEE refusées)")
    void eligibiliteSeance_seuleSeanceTermineeEstFacturable(StatutSeance statut) {
        seanceTerminee.setStatut(statut);
        when(seanceRepository.findById(1L)).thenReturn(Optional.of(seanceTerminee));

        Seance seance = seanceRepository.findById(1L).orElseThrow();

        if (statut == StatutSeance.TERMINEE) {
            assertThat(estFacturable(seance)).isTrue();
            verifierEligibiliteSeance(seance);
        } else {
            assertThat(estFacturable(seance)).isFalse();
            assertThatThrownBy(() -> verifierEligibiliteSeance(seance))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("TERMINEE");
        }

        verify(seanceRepository).findById(1L);
    }

    @Test
    @DisplayName("2. Unicité 1-1 strict : une séance déjà facturée est détectée même si sa facture est ANNULEE")
    void uniciteFacture_seanceDejaFacturee_doitEtreDetecteeParRepository() {
        Facture factureAnnulee = new Facture(
                seanceTerminee,
                "FAC-2026-0005",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("250.00"),
                TVA_DEFAUT,
                CoordonneesFacturation.depuis(clientParticulier),
                PrestationFacturee.depuis(seanceTerminee)
        );
        factureAnnulee.setId(5L);
        factureAnnulee.setStatutPaiement(StatutPaiement.ANNULEE);

        when(factureRepository.existsBySeanceId(1L)).thenReturn(true);
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.of(factureAnnulee));
        when(factureRepository.existsBySeanceId(2L)).thenReturn(false);

        assertThat(factureRepository.findBySeanceId(1L))
                .isPresent()
                .get()
                .extracting(Facture::getStatutPaiement)
                .isEqualTo(StatutPaiement.ANNULEE);
        assertThat(factureRepository.existsBySeanceId(1L)).isTrue();
        assertThatThrownBy(() -> verifierUniciteFacture(1L))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("déjà");

        assertThat(factureRepository.existsBySeanceId(2L)).isFalse();
        verifierUniciteFacture(2L);

        verify(factureRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Numérotation : la première facture d'une année génère FAC-{annee}-0001")
    void numerotationFacture_premiereFactureDeLAnnee_doitGenerer0001() {
        when(factureRepository.findAll()).thenReturn(List.of());

        String numero = genererProchainNumero(2026, factureRepository.findAll());

        assertThat(numero).isEqualTo("FAC-2026-0001");
        assertThat(numero).matches("^FAC-\\d{4}-\\d{4}$");
        verify(factureRepository).findAll();
    }

    @Test
    @DisplayName("4. Numérotation : incrément max + 1 par année et remise à 0001 au changement d'année")
    void numerotationFacture_incrementationMaxPlusUnParAnnee_doitSuivreLaSequence() {
        Facture facture2025 = factureAvecNumero(seanceTerminee, "FAC-2025-0099", LocalDate.of(2025, 12, 31));
        Facture facture2026a = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 1, 10));
        Facture facture2026b = factureAvecNumero(seanceTerminee, "FAC-2026-0014", LocalDate.of(2026, 11, 5));
        facture2026b.setStatutPaiement(StatutPaiement.ANNULEE);

        when(factureRepository.findAll()).thenReturn(List.of(facture2025, facture2026a, facture2026b));

        List<Facture> existantes = factureRepository.findAll();
        String prochain2026 = genererProchainNumero(2026, existantes);
        String premier2027 = genererProchainNumero(2027, existantes);

        assertThat(prochain2026).isEqualTo("FAC-2026-0015");
        assertThat(premier2027).isEqualTo("FAC-2027-0001");
    }

    @Test
    @DisplayName("5. Copie figée ClientParticulier : fige identité, adresse et email sans SIREN/TVA")
    void copieFigeeClientParticulier_doitFigerIdentiteAdresseEmailSansSiren() {
        CoordonneesFacturation coordonnees = CoordonneesFacturation.depuis(clientParticulier);
        Facture facture = new Facture(
                seanceTerminee,
                "FAC-2026-0001",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("250.00"),
                TVA_DEFAUT,
                coordonnees,
                PrestationFacturee.depuis(seanceTerminee)
        );

        // Modification ultérieure du client : la copie figée sur la facture reste inchangée
        clientParticulier.setNom("Martin");
        clientParticulier.setPrenom("Claire");
        clientParticulier.setEmail("claire.martin@mail.fr");
        clientParticulier.setAdressePostale("99 avenue de Bretagne, 35000 Rennes");

        assertThat(facture.getCoordonneesClient().identite()).isEqualTo("Dupont Marie");
        assertThat(facture.getCoordonneesClient().adressePostale()).isEqualTo("12 rue de Kerampont, 22300 Lannion");
        assertThat(facture.getCoordonneesClient().email()).isEqualTo("marie.dupont@mail.fr");
        assertThat(facture.getCoordonneesClient().numeroSirenTva()).isNull();
    }

    @Test
    @DisplayName("6. Copie figée ClientEntreprise : fige raison sociale, adresse, email et SIREN/TVA")
    void copieFigeeClientEntreprise_doitFigerRaisonSocialeAdresseEmailEtSirenTva() {
        seanceTerminee.setClient(clientEntreprise);
        CoordonneesFacturation coordonnees = CoordonneesFacturation.depuis(clientEntreprise);
        Facture facture = new Facture(
                seanceTerminee,
                "FAC-2026-0002",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("600.00"),
                TVA_DEFAUT,
                coordonnees,
                PrestationFacturee.depuis(seanceTerminee)
        );

        // Modification ultérieure de l'entreprise : la copie figée reste inchangée
        clientEntreprise.setRaisonSociale("Nouvelle Raison SAS");
        clientEntreprise.setAdressePostale("1 avenue Autre, 29000 Quimper");
        clientEntreprise.setEmail("facturation@nouvelle-raison.fr");
        clientEntreprise.setNumeroSirenTva("FR00000000000");

        assertThat(facture.getCoordonneesClient().identite()).isEqualTo("Atelier Breizh SARL");
        assertThat(facture.getCoordonneesClient().adressePostale()).isEqualTo("15 rue de la Poterie, 22300 Lannion");
        assertThat(facture.getCoordonneesClient().email()).isEqualTo("contact@atelier-breizh.fr");
        assertThat(facture.getCoordonneesClient().numeroSirenTva()).isEqualTo("FR40123456789");
    }

    @Test
    @DisplayName("7. Copie figée Prestation : fige date, heure, durée, spécialité et nom du photographe")
    void copieFigeePrestation_doitFigerDateHeureDureeSpecialiteEtNomPhotographe() {
        PrestationFacturee prestation = PrestationFacturee.depuis(seanceTerminee);
        Facture facture = new Facture(
                seanceTerminee,
                "FAC-2026-0003",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("450.00"),
                TVA_DEFAUT,
                CoordonneesFacturation.depuis(clientParticulier),
                prestation
        );

        // Modification ultérieure du photographe, de la spécialité et de la séance
        photographe.setNom("Photographe Modifié");
        specialite.setLibelle("Reportage");
        seanceTerminee.setDate(LocalDate.of(2027, 1, 15));
        seanceTerminee.setHeureDebut(LocalTime.of(9, 30));
        seanceTerminee.setDureeMinutes(60);

        assertThat(facture.getPrestation().date()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(facture.getPrestation().heureDebut()).isEqualTo(LocalTime.of(14, 0));
        assertThat(facture.getPrestation().dureeMinutes()).isEqualTo(120);
        assertThat(facture.getPrestation().typeSeance()).isEqualTo("Mariage et fêtes");
        assertThat(facture.getPrestation().nomPhotographe()).isEqualTo("Lucas Bernard");
    }

    @Test
    @DisplayName("8. État initial : statut EN_ATTENTE, datePaiement null, dateEmission aujourd'hui et TTC 20%")
    void factureInitiale_doitEtreEnAttenteAvecDateEmissionAujourdhuiEtCalculerTtcParDefaut20() {
        LocalDate aujourdhui = LocalDate.now();
        Facture facture = new Facture(
                seanceTerminee,
                "FAC-" + aujourdhui.getYear() + "-0001",
                aujourdhui,
                new BigDecimal("250.00"),
                TVA_DEFAUT,
                CoordonneesFacturation.depuis(clientParticulier),
                PrestationFacturee.depuis(seanceTerminee)
        );

        when(factureRepository.save(any(Facture.class))).thenAnswer(inv -> {
            Facture enregistree = inv.getArgument(0);
            enregistree.setId(100L);
            return enregistree;
        });

        Facture sauvegardee = factureRepository.save(facture);

        assertThat(sauvegardee.getId()).isEqualTo(100L);
        assertThat(sauvegardee.getStatutPaiement()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(sauvegardee.getDatePaiement()).isNull();
        assertThat(sauvegardee.getDateEmission()).isEqualTo(aujourdhui);
        assertThat(sauvegardee.getMontantHt()).isEqualByComparingTo("250.00");
        assertThat(sauvegardee.getTauxTva()).isEqualByComparingTo("20.00");
        assertThat(sauvegardee.getMontantTtc()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("9. Calcul TTC : taux personnalisé (5,50%) et arrondi HALF_UP à 2 décimales (99,99 HT -> 119,99 TTC)")
    void calculTtc_avecTauxTvaPersonnaliseEtArrondi_doitCalculerMontantTtcExact() {
        Facture tvaReduite = new Facture(
                seanceTerminee,
                "FAC-2026-0010",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("100.00"),
                new BigDecimal("5.50"),
                CoordonneesFacturation.depuis(clientParticulier),
                PrestationFacturee.depuis(seanceTerminee)
        );

        Facture tvaArrondiHalfUp = new Facture(
                seanceTerminee,
                "FAC-2026-0011",
                LocalDate.of(2026, 11, 11),
                new BigDecimal("99.99"),
                new BigDecimal("20.00"),
                CoordonneesFacturation.depuis(clientParticulier),
                PrestationFacturee.depuis(seanceTerminee)
        );

        assertThat(tvaReduite.getMontantTtc()).isEqualByComparingTo("105.50");
        assertThat(tvaReduite.getMontantTtc().scale()).isEqualTo(2);

        // 99.99 * 1.20 = 119.988 -> arrondi RoundingMode.HALF_UP à 119.99
        assertThat(tvaArrondiHalfUp.getMontantTtc()).isEqualByComparingTo("119.99");
        assertThat(tvaArrondiHalfUp.getMontantTtc().scale()).isEqualTo(2);
    }

    private static boolean estFacturable(Seance seance) {
        return seance.getStatut() == StatutSeance.TERMINEE;
    }

    private static void verifierEligibiliteSeance(Seance seance) {
        if (!estFacturable(seance)) {
            throw new RegleMetierException("Impossible de facturer une séance au statut " + seance.getStatut()
                    + " : seule une séance TERMINEE peut être facturée.");
        }
    }

    private void verifierUniciteFacture(Long seanceId) {
        if (factureRepository.existsBySeanceId(seanceId)) {
            throw new RegleMetierException("Une facture existe déjà pour la séance #" + seanceId);
        }
    }

    private static String genererProchainNumero(int annee, List<Facture> facturesExistantes) {
        String prefixe = "FAC-" + annee + "-";
        int maxSequence = facturesExistantes.stream()
                .map(Facture::getNumero)
                .filter(numero -> numero != null && numero.startsWith(prefixe))
                .mapToInt(numero -> Integer.parseInt(numero.substring(prefixe.length())))
                .max()
                .orElse(0);
        return "%s%04d".formatted(prefixe, maxSequence + 1);
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

    // ==================== Génération PDF (#13) ====================

    @Test
    @DisplayName("genererFacturePdf génère un PDF valide contenant numéro, date d'émission, coordonnées client, prestation, HT, TVA, TTC et statut")
    void genererFacturePdf_factureExistante_doitContenirToutesLesDonneesFigees() throws Exception {
        Facture facture = factureAvecNumero(seanceTerminee, "FAC-2026-0001", LocalDate.of(2026, 11, 11));
        facture.setId(10L);

        when(factureRepository.findById(10L)).thenReturn(Optional.of(facture));

        FacturePdf res = factureService.genererFacturePdf(10L);

        assertThat(res.numero()).isEqualTo("FAC-2026-0001");
        assertThat(res.filename()).isEqualTo("FAC-2026-0001.pdf");
        assertThat(res.content()).isNotEmpty();
        assertThat(new String(res.content(), 0, 4)).isEqualTo("%PDF");

        String textePdf = extraireTextePdf(res.content());
        assertThat(textePdf)
                .contains("FAC-2026-0001")
                .contains("11/11/2026")
                .contains("Dupont Marie")
                .contains("12 rue de Kerampont, 22300 Lannion")
                .contains("marie.dupont@mail.fr")
                .contains("Mariage et fêtes")
                .contains("10/11/2026")
                .contains("14:00")
                .contains("120 min")
                .contains("Lucas Bernard")
                .contains("250,00")
                .contains("20,00")
                .contains("50,00")
                .contains("300,00")
                .contains("EN_ATTENTE");
    }

    @Test
    @DisplayName("genererFacturePdf utilise uniquement les données figées même si le client, la séance et le photographe ont été modifiés depuis")
    void genererFacturePdf_apresModificationEntitesLiees_doitUtiliserUniquementDonneesFigees() throws Exception {
        seanceTerminee.setClient(clientEntreprise);
        Facture facture = new Facture(
                seanceTerminee,
                "FAC-2026-0007",
                LocalDate.of(2026, 11, 12),
                new BigDecimal("600.00"),
                new BigDecimal("20.00"),
                CoordonneesFacturation.depuis(clientEntreprise),
                PrestationFacturee.depuis(seanceTerminee)
        );
        facture.setId(7L);
        facture.setStatutPaiement(StatutPaiement.PAYEE);
        facture.setDatePaiement(LocalDate.of(2026, 11, 15));

        // Modifications ultérieures des entités liées (ne doivent pas impacter le PDF)
        clientEntreprise.setRaisonSociale("Entreprise Renommée SAS");
        clientEntreprise.setAdressePostale("999 rue Modifiée, 75000 Paris");
        clientEntreprise.setEmail("nouveau@entreprise.fr");
        clientEntreprise.setNumeroSirenTva("FR99999999999");
        photographe.setNom("Autre Photographe");
        specialite.setLibelle("Spécialité Modifiée");
        seanceTerminee.setDureeMinutes(30);

        when(factureRepository.findById(7L)).thenReturn(Optional.of(facture));

        FacturePdf res = factureService.genererFacturePdf(7L);
        String textePdf = extraireTextePdf(res.content());

        assertThat(textePdf)
                .contains("FAC-2026-0007")
                .contains("Atelier Breizh SARL")
                .contains("15 rue de la Poterie, 22300 Lannion")
                .contains("contact@atelier-breizh.fr")
                .contains("FR40123456789")
                .contains("Mariage et fêtes")
                .contains("120 min")
                .contains("Lucas Bernard")
                .contains("600,00")
                .contains("120,00")
                .contains("720,00")
                .contains("PAYEE")
                .contains("15/11/2026")
                .doesNotContain("Entreprise Renommée SAS")
                .doesNotContain("999 rue Modifiée")
                .doesNotContain("nouveau@entreprise.fr")
                .doesNotContain("FR99999999999")
                .doesNotContain("Autre Photographe")
                .doesNotContain("Spécialité Modifiée");
    }

    @Test
    @DisplayName("genererFacturePdf lève ResourceNotFoundException (404) si la facture est introuvable")
    void genererFacturePdf_factureInexistante_doitLeverResourceNotFoundException() {
        when(factureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factureService.genererFacturePdf(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    private static String extraireTextePdf(byte[] pdfBytes) throws Exception {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page)).append('\n');
            }
            return sb.toString();
        }
    }
}
