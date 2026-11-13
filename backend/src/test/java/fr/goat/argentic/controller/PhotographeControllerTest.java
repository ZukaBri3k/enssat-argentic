package fr.goat.argentic.controller;

import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.service.PhotographeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PhotographeControllerTest {

    @Mock
    private PhotographeService photographeService;

    private MockMvc mockMvc;

    private final PhotographeResponse camille = new PhotographeResponse(
            1L, "Camille Martin", "camille.martin@argentic.fr", true,
            List.of(new SpecialiteResponse(1L, "Reportage"), new SpecialiteResponse(2L, "Mariage et fêtes")));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PhotographeController(photographeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /photographes sans filtre renvoie 200 et tous les photographes")
    void getPhotographes_sansFiltre_doitRetourner200() throws Exception {
        when(photographeService.findAll(null)).thenReturn(List.of(camille));

        mockMvc.perform(get("/photographes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Camille Martin"))
                .andExpect(jsonPath("$[0].email").value("camille.martin@argentic.fr"))
                .andExpect(jsonPath("$[0].actif").value(true))
                .andExpect(jsonPath("$[0].specialites[1].libelle").value("Mariage et fêtes"));
    }

    @Test
    @DisplayName("GET /photographes?actif=true transmet le filtre au service")
    void getPhotographes_filtreActif_doitTransmettreLeFiltre() throws Exception {
        when(photographeService.findAll(true)).thenReturn(List.of(camille));

        mockMvc.perform(get("/photographes").param("actif", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Camille Martin"));
    }

    @Test
    @DisplayName("GET /photographes?actif=abc renvoie 400 en ProblemDetail")
    void getPhotographes_filtreInvalide_doitRetourner400() throws Exception {
        mockMvc.perform(get("/photographes").param("actif", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /photographes/{id} renvoie 200 et le photographe avec ses spécialités")
    void getPhotographe_idExistant_doitRetourner200() throws Exception {
        when(photographeService.findById(1L)).thenReturn(camille);

        mockMvc.perform(get("/photographes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Camille Martin"))
                .andExpect(jsonPath("$.specialites.length()").value(2))
                .andExpect(jsonPath("$.specialites[0].libelle").value("Reportage"));
    }

    @Test
    @DisplayName("GET /photographes/{id} renvoie 404 en ProblemDetail si le photographe n'existe pas")
    void getPhotographe_idInconnu_doitRetourner404() throws Exception {
        when(photographeService.findById(42L)).thenThrow(new ResourceNotFoundException("Photographe introuvable : 42"));

        mockMvc.perform(get("/photographes/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Photographe introuvable : 42"));
    }
}
