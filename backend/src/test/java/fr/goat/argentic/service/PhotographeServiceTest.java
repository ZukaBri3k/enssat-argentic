package fr.goat.argentic.service;

import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.repository.PhotographeRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotographeServiceTest {

    @Mock
    private PhotographeRepository photographeRepository;

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
