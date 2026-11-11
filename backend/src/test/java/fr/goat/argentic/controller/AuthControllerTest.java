package fr.goat.argentic.controller;

import fr.goat.argentic.dto.LoginRequest;
import fr.goat.argentic.dto.LoginResponse;
import fr.goat.argentic.dto.MeResponse;
import fr.goat.argentic.exception.GlobalExceptionHandler;
import fr.goat.argentic.model.Role;
import fr.goat.argentic.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private StubAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new StubAuthService();
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                && parameter.getParameterType().equals(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new Jwt(
                                "token-val",
                                Instant.now(),
                                Instant.now().plusSeconds(3600),
                                Map.of("alg", "HS256"),
                                Map.of("sub", "secretaire@argentic.fr", "role", "SECRETAIRE")
                        );
                    }
                })
                .build();
    }

    @Test
    @DisplayName("POST /auth/login avec identifiants valides renvoie 200 et le token")
    void login_avecIdentifiantsValides_doitRetourner200EtToken() throws Exception {
        authService.setHandler(req -> new LoginResponse("mocked.jwt.token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "secretaire@argentic.fr",
                                    "motDePasse": "secretaire"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"));
    }

    @Test
    @DisplayName("POST /auth/login avec mauvais identifiants renvoie 401 ProblemDetail")
    void login_avecMauvaisIdentifiants_doitRetourner401ProblemDetail() throws Exception {
        authService.setHandler(req -> {
            throw new BadCredentialsException("Identifiants invalides");
        });

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "inconnu@argentic.fr",
                                    "motDePasse": "mauvais"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Non authentifié"))
                .andExpect(jsonPath("$.detail").value("Identifiants invalides"));
    }

    @Test
    @DisplayName("POST /auth/login avec email vide renvoie 400 ProblemDetail")
    void login_avecEmailInvalide_doitRetourner400ProblemDetail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "motDePasse": "secretaire"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Données invalides"));
    }

    @Test
    @DisplayName("GET /auth/me renvoie l'utilisateur courant et son rôle")
    void me_avecTokenValide_doitRetournerUtilisateurEtRole() throws Exception {
        authService.setMeHandler(email -> new MeResponse(1L, "secretaire@argentic.fr", Role.SECRETAIRE));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("secretaire@argentic.fr"))
                .andExpect(jsonPath("$.role").value("SECRETAIRE"));
    }

    static class StubAuthService extends AuthService {
        private Function<LoginRequest, LoginResponse> handler;
        private Function<String, MeResponse> meHandler;

        public StubAuthService() {
            super(null, null, null);
        }

        public void setHandler(Function<LoginRequest, LoginResponse> handler) {
            this.handler = handler;
        }

        public void setMeHandler(Function<String, MeResponse> meHandler) {
            this.meHandler = meHandler;
        }

        @Override
        public LoginResponse login(LoginRequest request) {
            if (handler != null) {
                return handler.apply(request);
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public MeResponse getCurrentUser(String email) {
            if (meHandler != null) {
                return meHandler.apply(email);
            }
            throw new UnsupportedOperationException();
        }
    }
}
