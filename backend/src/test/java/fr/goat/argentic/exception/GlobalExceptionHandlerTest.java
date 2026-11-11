package fr.goat.argentic.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("400 Validation : renvoie ProblemDetail avec détail par champ")
    void validationErreur_doitRetourner400AvecDetailParChamp() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "pas-un-email",
                                    "nom": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.errors.email").value("Email invalide"))
                .andExpect(jsonPath("$.errors.nom").value("Nom obligatoire"));
    }

    @Test
    @DisplayName("404 ResourceNotFoundException : renvoie ProblemDetail 404")
    void resourceNotFound_doitRetourner404ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Ressource non trouvée"))
                .andExpect(jsonPath("$.detail").value("Élément introuvable"));
    }

    @Test
    @DisplayName("409 RegleMetierException : renvoie ProblemDetail 409")
    void regleMetier_doitRetourner409ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Règle métier violée"))
                .andExpect(jsonPath("$.detail").value("Règle violée"));
    }

    @Test
    @DisplayName("401 AuthenticationException : renvoie ProblemDetail 401")
    void unauthorized_doitRetourner401ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Non authentifié"));
    }

    @Test
    @DisplayName("403 AccessDeniedException : renvoie ProblemDetail 403")
    void forbidden_doitRetourner403ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Accès refusé"));
    }

    record DummyPayload(
            @Email(message = "Email invalide") String email,
            @NotBlank(message = "Nom obligatoire") String nom
    ) {}

    @RestController
    static class DummyController {
        @PostMapping("/test/validation")
        public void validate(@Valid @RequestBody DummyPayload payload) {}

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Élément introuvable");
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new RegleMetierException("Règle violée");
        }

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new BadCredentialsException("Non authentifié");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("Refusé");
        }
    }
}
