package fr.goat.argentic.controller;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.service.SeanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SeanceControllerTest {

    @Mock
    private SeanceService seanceService;

    private MockMvc mockMvc;

    private final SeanceResponse seanceResponse = new SeanceResponse(
            1L,
            LocalDate.of(2026, 11, 20),
            LocalTime.of(14, 0),
            120,
            LocalDateTime.of(2026, 11, 20, 14, 0),
            LocalDateTime.of(2026, 11, 20, 16, 0),
            "Château de Rosanbo",
            StatutSeance.CONFIRMEE,
            10L,
            "Lucas Bernard",
            "lucas.bernard@argentic.fr",
            20L,
            "Dupont Marie",
            "marie.dupont@mail.fr",
            new SpecialiteResponse(2L, "Mariage et fêtes"),
            null,
            null
    );

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SeanceController(seanceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PATCH /seances/{id}/statut renvoie 200 et la séance mise à jour")
    void changerStatut_valide_doitRetourner200() throws Exception {
        when(seanceService.changerStatut(1L, StatutSeance.CONFIRMEE)).thenReturn(seanceResponse);

        mockMvc.perform(patch("/seances/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "CONFIRMEE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"));
    }

    @Test
    @DisplayName("PATCH /seances/{id}/statut renvoie 400 en ProblemDetail si le statut est manquant")
    void changerStatut_statutManquant_doitRetourner400() throws Exception {
        mockMvc.perform(patch("/seances/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.statut").exists());

        verifyNoInteractions(seanceService);
    }

    @Test
    @DisplayName("PATCH /seances/{id}/statut renvoie 409 en ProblemDetail si la transition est refusée")
    void changerStatut_transitionInvalide_doitRetourner409() throws Exception {
        when(seanceService.changerStatut(1L, StatutSeance.TERMINEE))
                .thenThrow(new RegleMetierException("Impossible de passer la séance à TERMINEE : la fin de la séance n'est pas encore passée."));

        mockMvc.perform(patch("/seances/1/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "TERMINEE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Impossible de passer la séance à TERMINEE : la fin de la séance n'est pas encore passée."));
    }

    @Test
    @DisplayName("PATCH /seances/{id}/statut renvoie 404 en ProblemDetail si la séance est introuvable")
    void changerStatut_seanceInconnue_doitRetourner404() throws Exception {
        when(seanceService.changerStatut(999L, StatutSeance.CONFIRMEE))
                .thenThrow(new ResourceNotFoundException("Séance non trouvée avec l'id : 999"));

        mockMvc.perform(patch("/seances/999/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"statut": "CONFIRMEE"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Séance non trouvée avec l'id : 999"));
    }

    @Test
    @DisplayName("DELETE /seances/{id} renvoie 204 sans corps")
    void deleteSeance_sansFacture_doitRetourner204() throws Exception {
        mockMvc.perform(delete("/seances/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(seanceService).deleteSeance(1L);
    }

    @Test
    @DisplayName("DELETE /seances/{id} renvoie 409 en ProblemDetail si la séance a une facture")
    void deleteSeance_avecFacture_doitRetourner409() throws Exception {
        doThrow(new RegleMetierException("Impossible de supprimer cette séance : une facture y est associée. Vous pouvez l'annuler à la place."))
                .when(seanceService).deleteSeance(1L);

        mockMvc.perform(delete("/seances/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Impossible de supprimer cette séance : une facture y est associée. Vous pouvez l'annuler à la place."));
    }
}
