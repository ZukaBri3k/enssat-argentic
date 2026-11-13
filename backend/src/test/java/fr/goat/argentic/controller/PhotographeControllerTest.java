package fr.goat.argentic.controller;

import fr.goat.argentic.dto.PhotographeRequest;
import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.dto.SeanceAVenirResponse;
import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.exception.RegleMetierException;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.service.PhotographeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @DisplayName("POST /photographes renvoie 201, l'en-tête Location et le photographe créé")
    void createPhotographe_donneesValides_doitRetourner201() throws Exception {
        PhotographeRequest request = new PhotographeRequest("Camille Martin", "camille.martin@argentic.fr", Set.of(1L, 2L));
        when(photographeService.create(request)).thenReturn(camille);

        mockMvc.perform(post("/photographes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Camille Martin", "email": "camille.martin@argentic.fr", "specialiteIds": [1, 2]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/photographes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.specialites.length()").value(2));
    }

    @Test
    @DisplayName("POST /photographes renvoie 400 avec le détail par champ si nom, email ou spécialités sont invalides")
    void createPhotographe_donneesInvalides_doitRetourner400() throws Exception {
        mockMvc.perform(post("/photographes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": " ", "email": "pas-un-email", "specialiteIds": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nom").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.specialiteIds").value("Au moins une spécialité est requise"));
        verifyNoInteractions(photographeService);
    }

    @Test
    @DisplayName("POST /photographes renvoie 409 en ProblemDetail si l'email est déjà utilisé")
    void createPhotographe_emailDejaUtilise_doitRetourner409() throws Exception {
        when(photographeService.create(any())).thenThrow(
                new RegleMetierException("Un photographe utilise déjà l'adresse email camille.martin@argentic.fr"));

        mockMvc.perform(post("/photographes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Camille Martin", "email": "camille.martin@argentic.fr", "specialiteIds": [1]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Un photographe utilise déjà l'adresse email camille.martin@argentic.fr"));
    }

    @Test
    @DisplayName("PUT /photographes/{id} renvoie 200 et le photographe modifié")
    void updatePhotographe_donneesValides_doitRetourner200() throws Exception {
        PhotographeRequest request = new PhotographeRequest("Camille Martin", "camille.martin@argentic.fr", Set.of(1L, 2L));
        when(photographeService.update(1L, request)).thenReturn(camille);

        mockMvc.perform(put("/photographes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Camille Martin", "email": "camille.martin@argentic.fr", "specialiteIds": [1, 2]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Camille Martin"));
    }

    @Test
    @DisplayName("PUT /photographes/{id} renvoie 400 si aucune spécialité n'est fournie")
    void updatePhotographe_sansSpecialite_doitRetourner400() throws Exception {
        mockMvc.perform(put("/photographes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Camille Martin", "email": "camille.martin@argentic.fr"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.specialiteIds").value("Au moins une spécialité est requise"));
        verifyNoInteractions(photographeService);
    }

    @Test
    @DisplayName("DELETE /photographes/{id} renvoie 204 sans corps")
    void deletePhotographe_sansSeance_doitRetourner204() throws Exception {
        mockMvc.perform(delete("/photographes/2"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(photographeService).delete(2L);
    }

    @Test
    @DisplayName("DELETE /photographes/{id} renvoie 409 en ProblemDetail si le photographe a des séances")
    void deletePhotographe_avecSeances_doitRetourner409() throws Exception {
        doThrow(new RegleMetierException("Impossible de supprimer Camille Martin : il a des séances enregistrées. Vous pouvez le désactiver à la place."))
                .when(photographeService).delete(1L);

        mockMvc.perform(delete("/photographes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Impossible de supprimer Camille Martin : il a des séances enregistrées. Vous pouvez le désactiver à la place."));
    }

    @Test
    @DisplayName("GET /photographes/{id}/seances-a-venir renvoie 200 et les séances à venir")
    void getSeancesAVenir_photographeExistant_doitRetourner200() throws Exception {
        when(photographeService.findSeancesAVenir(1L)).thenReturn(List.of(new SeanceAVenirResponse(
                10L,
                LocalDateTime.of(2030, 6, 14, 14, 0),
                LocalDateTime.of(2030, 6, 14, 18, 0),
                "Salle des fêtes, Lannion",
                StatutSeance.CONFIRMEE,
                "Dupont Marie",
                new SpecialiteResponse(2L, "Mariage et fêtes"))));

        mockMvc.perform(get("/photographes/1/seances-a-venir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].debut").value("2030-06-14T14:00:00"))
                .andExpect(jsonPath("$[0].fin").value("2030-06-14T18:00:00"))
                .andExpect(jsonPath("$[0].statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$[0].clientIdentite").value("Dupont Marie"))
                .andExpect(jsonPath("$[0].specialite.libelle").value("Mariage et fêtes"));
    }

    @Test
    @DisplayName("GET /photographes/{id}/seances-a-venir renvoie 404 en ProblemDetail si le photographe n'existe pas")
    void getSeancesAVenir_idInconnu_doitRetourner404() throws Exception {
        when(photographeService.findSeancesAVenir(42L)).thenThrow(new ResourceNotFoundException("Photographe introuvable : 42"));

        mockMvc.perform(get("/photographes/42/seances-a-venir"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Photographe introuvable : 42"));
    }

    @Test
    @DisplayName("POST /photographes/{id}/desactivation renvoie 200 et le photographe inactif")
    void desactiverPhotographe_photographeActif_doitRetourner200() throws Exception {
        when(photographeService.desactiver(1L)).thenReturn(new PhotographeResponse(
                1L, "Camille Martin", "camille.martin@argentic.fr", false, camille.specialites()));

        mockMvc.perform(post("/photographes/1/desactivation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    @DisplayName("POST /photographes/{id}/desactivation renvoie 404 en ProblemDetail si le photographe n'existe pas")
    void desactiverPhotographe_idInconnu_doitRetourner404() throws Exception {
        when(photographeService.desactiver(42L)).thenThrow(new ResourceNotFoundException("Photographe introuvable : 42"));

        mockMvc.perform(post("/photographes/42/desactivation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Photographe introuvable : 42"));
    }

    @Test
    @DisplayName("POST /photographes/{id}/activation renvoie 200 et le photographe actif")
    void activerPhotographe_photographeInactif_doitRetourner200() throws Exception {
        when(photographeService.activer(1L)).thenReturn(camille);

        mockMvc.perform(post("/photographes/1/activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    @DisplayName("POST /photographes/{id}/activation renvoie 404 en ProblemDetail si le photographe n'existe pas")
    void activerPhotographe_idInconnu_doitRetourner404() throws Exception {
        when(photographeService.activer(42L)).thenThrow(new ResourceNotFoundException("Photographe introuvable : 42"));

        mockMvc.perform(post("/photographes/42/activation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Photographe introuvable : 42"));
    }
}
