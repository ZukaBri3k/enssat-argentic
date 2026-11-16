package fr.goat.argentic.service;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.exception.DonneeInvalideException;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.ClientParticulier;
import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.SeanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeanceServiceTest {

    @Mock
    private SeanceRepository seanceRepository;

    @Mock
    private FactureRepository factureRepository;

    @InjectMocks
    private SeanceService seanceService;

    private Specialite specialite;
    private Photographe photographe;
    private ClientParticulier client;
    private Seance seance;

    @BeforeEach
    void setUp() {
        specialite = new Specialite("Mariage et fêtes");
        specialite.setId(2L);

        photographe = new Photographe("Lucas Bernard", "lucas.bernard@argentic.fr", Set.of(specialite));
        photographe.setId(10L);

        client = new ClientParticulier(
                "marie.dupont@mail.fr",
                "12 rue de Kerampont, 22300 Lannion",
                "Dupont",
                "Marie"
        );
        client.setId(20L);

        seance = new Seance(
                photographe,
                client,
                specialite,
                LocalDate.of(2026, 11, 20),
                LocalTime.of(14, 0),
                120,
                "Château de Rosanbo"
        );
        seance.setId(1L);
        seance.setStatut(StatutSeance.CONFIRMEE);
    }

    @Test
    @DisplayName("getSeance retourne les séances filtrées et calcule correctement la fin")
    void getSeance_avecFiltresValides_doitRetournerListeSeances() {
        LocalDate du = LocalDate.of(2026, 11, 1);
        LocalDate au = LocalDate.of(2026, 11, 30);

        when(seanceRepository.getSeances(du, au, 10L, 20L, StatutSeance.CONFIRMEE))
                .thenReturn(List.of(seance));

        List<SeanceResponse> result = seanceService.getSeance(du, au, 10L, 20L, StatutSeance.CONFIRMEE);

        assertThat(result).hasSize(1);
        SeanceResponse dto = result.get(0);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(dto.heureDebut()).isEqualTo(LocalTime.of(14, 0));
        assertThat(dto.dureeMinutes()).isEqualTo(120);
        assertThat(dto.debut()).isEqualTo(LocalDateTime.of(2026, 11, 20, 14, 0));
        assertThat(dto.fin()).isEqualTo(LocalDateTime.of(2026, 11, 20, 16, 0));
        assertThat(dto.lieu()).isEqualTo("Château de Rosanbo");
        assertThat(dto.statut()).isEqualTo(StatutSeance.CONFIRMEE);
        assertThat(dto.photographeId()).isEqualTo(10L);
        assertThat(dto.photographeNom()).isEqualTo("Lucas Bernard");
        assertThat(dto.clientId()).isEqualTo(20L);
        assertThat(dto.clientIdentite()).isEqualTo("Dupont Marie");
        assertThat(dto.specialite().libelle()).isEqualTo("Mariage et fêtes");

        verify(seanceRepository).getSeances(du, au, 10L, 20L, StatutSeance.CONFIRMEE);
    }

    @Test
    @DisplayName("getSeance lève RegleMetierException si la date du est après la date au")
    void getSeance_duApresAu_doitLeverRegleMetierException() {
        LocalDate du = LocalDate.of(2026, 12, 31);
        LocalDate au = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> seanceService.getSeance(du, au, null, null, null))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("postérieure");

        verifyNoInteractions(seanceRepository);
    }

    @Test
    @DisplayName("getSeanceById retourne le détail de la séance avec sa facture éventuelle")
    void getSeanceById_avecFacture_doitRetournerDetailComplet() {
        seance.setStatut(StatutSeance.TERMINEE);
        Facture facture = new Facture(
                seance,
                "FAC-2026-0001",
                LocalDate.of(2026, 11, 21),
                new BigDecimal("500.00"),
                new BigDecimal("20.00"),
                CoordonneesFacturation.depuis(client),
                PrestationFacturee.depuis(seance)
        );
        facture.setId(99L);

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.of(facture));

        SeanceResponse dto = seanceService.getSeanceById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.fin()).isEqualTo(LocalDateTime.of(2026, 11, 20, 16, 0));
        assertThat(dto.photographeNom()).isEqualTo("Lucas Bernard");
        assertThat(dto.clientIdentite()).isEqualTo("Dupont Marie");
        assertThat(dto.specialite().libelle()).isEqualTo("Mariage et fêtes");
        assertThat(dto.factureId()).isEqualTo(99L);
        assertThat(dto.factureNumero()).isEqualTo("FAC-2026-0001");
    }

    @Test
    @DisplayName("getSeanceById retourne le détail de la séance sans facture si aucune facture n'est émise")
    void getSeanceById_sansFacture_doitRetournerFactureNull() {
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.empty());

        SeanceResponse dto = seanceService.getSeanceById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.factureId()).isNull();
        assertThat(dto.factureNumero()).isNull();
    }

    @Test
    @DisplayName("getSeanceById lève ResourceNotFoundException si l'id n'existe pas")
    void getSeanceById_inexistant_doitLeverResourceNotFoundException() {
        when(seanceRepository.getSeanceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seanceService.getSeanceById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verifyNoInteractions(factureRepository);
    }

    // ==================== Tests changerStatut ====================

    @Test
    @DisplayName("changerStatut autorise la transition PLANIFIEE -> CONFIRMEE")
    void changerStatut_planifieeVersConfirmee_doitMettreAJourLeStatut() {
        seance.setStatut(StatutSeance.PLANIFIEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.empty());

        SeanceResponse result = seanceService.changerStatut(1L, StatutSeance.CONFIRMEE);

        assertThat(result.statut()).isEqualTo(StatutSeance.CONFIRMEE);
        assertThat(seance.getStatut()).isEqualTo(StatutSeance.CONFIRMEE);
        verify(seanceRepository).save(seance);
    }

    @Test
    @DisplayName("changerStatut autorise la transition PLANIFIEE -> ANNULEE")
    void changerStatut_planifieeVersAnnulee_doitMettreAJourLeStatut() {
        seance.setStatut(StatutSeance.PLANIFIEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.empty());

        SeanceResponse result = seanceService.changerStatut(1L, StatutSeance.ANNULEE);

        assertThat(result.statut()).isEqualTo(StatutSeance.ANNULEE);
        assertThat(seance.getStatut()).isEqualTo(StatutSeance.ANNULEE);
        verify(seanceRepository).save(seance);
    }

    @Test
    @DisplayName("changerStatut refuse la transition directe PLANIFIEE -> TERMINEE (409)")
    void changerStatut_planifieeVersTerminee_doitLeverRegleMetierException() {
        seance.setStatut(StatutSeance.PLANIFIEE);
        seance.setDate(LocalDate.now().minusDays(2));
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));

        assertThatThrownBy(() -> seanceService.changerStatut(1L, StatutSeance.TERMINEE))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("PLANIFIEE")
                .hasMessageContaining("TERMINEE");

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.PLANIFIEE);
        verify(seanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("changerStatut autorise la transition CONFIRMEE -> ANNULEE")
    void changerStatut_confirmeeVersAnnulee_doitMettreAJourLeStatut() {
        seance.setStatut(StatutSeance.CONFIRMEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.empty());

        SeanceResponse result = seanceService.changerStatut(1L, StatutSeance.ANNULEE);

        assertThat(result.statut()).isEqualTo(StatutSeance.ANNULEE);
        assertThat(seance.getStatut()).isEqualTo(StatutSeance.ANNULEE);
        verify(seanceRepository).save(seance);
    }

    @Test
    @DisplayName("changerStatut autorise la transition CONFIRMEE -> TERMINEE si la fin de la séance est passée")
    void changerStatut_confirmeeVersTerminee_finPassee_doitMettreAJourLeStatut() {
        seance.setStatut(StatutSeance.CONFIRMEE);
        seance.setDate(LocalDate.now().minusDays(1));
        seance.setHeureDebut(LocalTime.of(10, 0));
        seance.setDureeMinutes(60);

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.findBySeanceId(1L)).thenReturn(Optional.empty());

        SeanceResponse result = seanceService.changerStatut(1L, StatutSeance.TERMINEE);

        assertThat(result.statut()).isEqualTo(StatutSeance.TERMINEE);
        assertThat(seance.getStatut()).isEqualTo(StatutSeance.TERMINEE);
        verify(seanceRepository).save(seance);
    }

    @Test
    @DisplayName("changerStatut refuse la transition CONFIRMEE -> TERMINEE si la fin de la séance n'est pas encore passée (409)")
    void changerStatut_confirmeeVersTerminee_finDansLeFutur_doitLeverRegleMetierException() {
        seance.setStatut(StatutSeance.CONFIRMEE);
        seance.setDate(LocalDate.now().plusDays(1));
        seance.setHeureDebut(LocalTime.of(14, 0));
        seance.setDureeMinutes(120);

        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));

        assertThatThrownBy(() -> seanceService.changerStatut(1L, StatutSeance.TERMINEE))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("TERMINEE")
                .hasMessageContaining("fin");

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.CONFIRMEE);
        verify(seanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("changerStatut refuse la transition CONFIRMEE -> PLANIFIEE (409)")
    void changerStatut_confirmeeVersPlanifiee_doitLeverRegleMetierException() {
        seance.setStatut(StatutSeance.CONFIRMEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));

        assertThatThrownBy(() -> seanceService.changerStatut(1L, StatutSeance.PLANIFIEE))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("CONFIRMEE")
                .hasMessageContaining("PLANIFIEE");

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.CONFIRMEE);
        verify(seanceRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(StatutSeance.class)
    @DisplayName("changerStatut refuse toute transition depuis TERMINEE (statut final -> 409)")
    void changerStatut_depuisTerminee_doitLeverRegleMetierException(StatutSeance cible) {
        seance.setStatut(StatutSeance.TERMINEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));

        assertThatThrownBy(() -> seanceService.changerStatut(1L, cible))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("final");

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.TERMINEE);
        verify(seanceRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(StatutSeance.class)
    @DisplayName("changerStatut refuse toute transition depuis ANNULEE (statut final -> 409)")
    void changerStatut_depuisAnnulee_doitLeverRegleMetierException(StatutSeance cible) {
        seance.setStatut(StatutSeance.ANNULEE);
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));

        assertThatThrownBy(() -> seanceService.changerStatut(1L, cible))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("final");

        assertThat(seance.getStatut()).isEqualTo(StatutSeance.ANNULEE);
        verify(seanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("changerStatut lève DonneeInvalideException si le nouveau statut est null")
    void changerStatut_statutNull_doitLeverDonneeInvalideException() {
        assertThatThrownBy(() -> seanceService.changerStatut(1L, null))
                .isInstanceOf(DonneeInvalideException.class)
                .hasMessageContaining("statut");

        verifyNoInteractions(seanceRepository);
    }

    @Test
    @DisplayName("changerStatut lève ResourceNotFoundException si la séance n'existe pas")
    void changerStatut_seanceInexistante_doitLeverResourceNotFoundException() {
        when(seanceRepository.getSeanceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seanceService.changerStatut(999L, StatutSeance.CONFIRMEE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(seanceRepository, never()).save(any());
    }

    // ==================== Tests deleteSeance ====================

    @Test
    @DisplayName("deleteSeance supprime la séance si elle n'a pas de facture")
    void deleteSeance_sansFacture_doitSupprimerLaSeance() {
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(false);

        seanceService.deleteSeance(1L);

        verify(seanceRepository).delete(seance);
    }

    @Test
    @DisplayName("deleteSeance lève RegleMetierException (409) si la séance a une facture")
    void deleteSeance_avecFacture_doitLeverRegleMetierException() {
        when(seanceRepository.getSeanceById(1L)).thenReturn(Optional.of(seance));
        when(factureRepository.existsBySeanceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> seanceService.deleteSeance(1L))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("facture");

        verify(seanceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteSeance lève ResourceNotFoundException (404) si la séance n'existe pas")
    void deleteSeance_seanceInexistante_doitLeverResourceNotFoundException() {
        when(seanceRepository.getSeanceById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seanceService.deleteSeance(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verifyNoInteractions(factureRepository);
        verify(seanceRepository, never()).delete(any());
    }
}
