package fr.goat.argentic.service;

import fr.goat.argentic.dto.PhotographeRequest;
import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.repository.PhotographeRepository;
import fr.goat.argentic.repository.SeanceRepository;
import fr.goat.argentic.repository.SpecialiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotographeServiceTest {

    @Mock
    private PhotographeRepository photographeRepository;

    @Mock
    private SpecialiteRepository specialiteRepository;

    @Mock
    private SeanceRepository seanceRepository;

    @InjectMocks
    private PhotographeService photographeService;

    private Specialite reportage;
    private Specialite mariage;
    private Specialite famille;

    @BeforeEach
    void setUp() {
        reportage = specialite(1L, "Reportage");
        mariage = specialite(2L, "Mariage et fêtes");
        famille = specialite(3L, "Famille");
    }

    @Test
    @DisplayName("findAll sans filtre retourne tous les photographes avec leurs spécialités")
    void findAll_sansFiltre_doitRetournerTousLesPhotographes() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, mariage, reportage);
        Photographe lucas = photographe(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", false, famille);
        when(photographeRepository.findAllByOrderByNomAsc()).thenReturn(List.of(camille, lucas));

        List<PhotographeResponse> result = photographeService.findAll(null);

        assertThat(result).containsExactly(
                new PhotographeResponse(1L, "Camille Martin", "camille.martin@argentic.fr", true, List.of(
                        new SpecialiteResponse(1L, "Reportage"),
                        new SpecialiteResponse(2L, "Mariage et fêtes"))),
                new PhotographeResponse(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", false, List.of(
                        new SpecialiteResponse(3L, "Famille")))
        );
    }

    @Test
    @DisplayName("findAll avec actif=false ne retourne que les photographes inactifs")
    void findAll_filtreActif_doitRetournerUniquementLeStatutDemande() {
        Photographe lucas = photographe(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", false, famille);
        when(photographeRepository.findByActifOrderByNomAsc(false)).thenReturn(List.of(lucas));

        List<PhotographeResponse> result = photographeService.findAll(false);

        assertThat(result).containsExactly(
                new PhotographeResponse(2L, "Lucas Bernard", "lucas.bernard@argentic.fr", false, List.of(
                        new SpecialiteResponse(3L, "Famille")))
        );
    }

    @Test
    @DisplayName("findById retourne le photographe avec ses spécialités")
    void findById_idExistant_doitRetournerLePhotographe() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, reportage, mariage);
        when(photographeRepository.findById(1L)).thenReturn(Optional.of(camille));

        PhotographeResponse result = photographeService.findById(1L);

        assertThat(result).isEqualTo(new PhotographeResponse(1L, "Camille Martin", "camille.martin@argentic.fr", true, List.of(
                new SpecialiteResponse(1L, "Reportage"),
                new SpecialiteResponse(2L, "Mariage et fêtes"))));
    }

    @Test
    @DisplayName("findById lève ResourceNotFoundException si le photographe n'existe pas")
    void findById_idInconnu_doitLeverResourceNotFound() {
        when(photographeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photographeService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("create enregistre le photographe actif avec ses spécialités")
    void create_donneesValides_doitEnregistrerLePhotographe() {
        when(specialiteRepository.findAllById(Set.of(1L, 3L))).thenReturn(List.of(reportage, famille));
        when(photographeRepository.save(any(Photographe.class))).thenAnswer(invocation -> {
            Photographe enregistre = invocation.getArgument(0);
            enregistre.setId(10L);
            return enregistre;
        });

        PhotographeResponse result = photographeService.create(
                new PhotographeRequest("Inès Petit", "ines.petit@argentic.fr", Set.of(1L, 3L)));

        assertThat(result).isEqualTo(new PhotographeResponse(10L, "Inès Petit", "ines.petit@argentic.fr", true, List.of(
                new SpecialiteResponse(1L, "Reportage"),
                new SpecialiteResponse(3L, "Famille"))));
    }

    @Test
    @DisplayName("create lève RegleMetierException si l'email est déjà utilisé")
    void create_emailDejaUtilise_doitLeverRegleMetier() {
        when(photographeRepository.existsByEmailIgnoreCase("camille.martin@argentic.fr")).thenReturn(true);

        assertThatThrownBy(() -> photographeService.create(
                new PhotographeRequest("Camille Bis", "camille.martin@argentic.fr", Set.of(1L))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("camille.martin@argentic.fr");
        verify(photographeRepository, never()).save(any());
    }

    @Test
    @DisplayName("create lève ResourceNotFoundException si une spécialité n'existe pas")
    void create_specialiteInconnue_doitLeverResourceNotFound() {
        when(specialiteRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(reportage));

        assertThatThrownBy(() -> photographeService.create(
                new PhotographeRequest("Inès Petit", "ines.petit@argentic.fr", Set.of(1L, 99L))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(photographeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update modifie le nom, l'email et ajoute une spécialité")
    void update_donneesValides_doitModifierLePhotographe() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, reportage);
        when(photographeRepository.findById(1L)).thenReturn(Optional.of(camille));
        when(specialiteRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(reportage, mariage));

        PhotographeResponse result = photographeService.update(1L,
                new PhotographeRequest("Camille Martin-Durand", "camille.durand@argentic.fr", Set.of(1L, 2L)));

        assertThat(result).isEqualTo(new PhotographeResponse(1L, "Camille Martin-Durand", "camille.durand@argentic.fr", true, List.of(
                new SpecialiteResponse(1L, "Reportage"),
                new SpecialiteResponse(2L, "Mariage et fêtes"))));
    }

    @Test
    @DisplayName("update lève RegleMetierException si l'email appartient à un autre photographe")
    void update_emailDUnAutrePhotographe_doitLeverRegleMetier() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, reportage);
        when(photographeRepository.findById(1L)).thenReturn(Optional.of(camille));
        when(photographeRepository.existsByEmailIgnoreCaseAndIdNot("lucas.bernard@argentic.fr", 1L)).thenReturn(true);

        assertThatThrownBy(() -> photographeService.update(1L,
                new PhotographeRequest("Camille Martin", "lucas.bernard@argentic.fr", Set.of(1L))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("lucas.bernard@argentic.fr");
        assertThat(camille.getEmail()).isEqualTo("camille.martin@argentic.fr");
    }

    @Test
    @DisplayName("update lève RegleMetierException si une spécialité retirée a des séances à venir non annulées")
    void update_retraitSpecialiteAvecSeancesAVenir_doitLeverRegleMetier() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, reportage, mariage);
        when(photographeRepository.findById(1L)).thenReturn(Optional.of(camille));
        when(specialiteRepository.findAllById(Set.of(1L))).thenReturn(List.of(reportage));
        when(seanceRepository.findSpecialitesAvecSeancesAVenir(eq(1L), eq(Set.of(2L)), any(), any()))
                .thenReturn(List.of(mariage));

        assertThatThrownBy(() -> photographeService.update(1L,
                new PhotographeRequest("Camille Martin", "camille.martin@argentic.fr", Set.of(1L))))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("Mariage et fêtes");
        assertThat(camille.getSpecialites()).containsExactlyInAnyOrder(reportage, mariage);
    }

    @Test
    @DisplayName("update autorise le retrait d'une spécialité sans séance à venir non annulée")
    void update_retraitSpecialiteSansSeanceAVenir_doitRetirerLaSpecialite() {
        Photographe camille = photographe(1L, "Camille Martin", "camille.martin@argentic.fr", true, reportage, mariage);
        when(photographeRepository.findById(1L)).thenReturn(Optional.of(camille));
        when(specialiteRepository.findAllById(Set.of(1L))).thenReturn(List.of(reportage));
        when(seanceRepository.findSpecialitesAvecSeancesAVenir(eq(1L), eq(Set.of(2L)), any(), any()))
                .thenReturn(List.of());

        PhotographeResponse result = photographeService.update(1L,
                new PhotographeRequest("Camille Martin", "camille.martin@argentic.fr", Set.of(1L)));

        assertThat(result.specialites()).containsExactly(new SpecialiteResponse(1L, "Reportage"));
    }

    @Test
    @DisplayName("update lève ResourceNotFoundException si le photographe n'existe pas")
    void update_idInconnu_doitLeverResourceNotFound() {
        when(photographeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photographeService.update(42L,
                new PhotographeRequest("Camille Martin", "camille.martin@argentic.fr", Set.of(1L))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    private static Specialite specialite(Long id, String libelle) {
        Specialite specialite = new Specialite(libelle);
        specialite.setId(id);
        return specialite;
    }

    private static Photographe photographe(Long id, String nom, String email, boolean actif, Specialite... specialites) {
        Photographe photographe = new Photographe(nom, email, Set.of(specialites));
        photographe.setId(id);
        photographe.setActif(actif);
        return photographe;
    }
}
