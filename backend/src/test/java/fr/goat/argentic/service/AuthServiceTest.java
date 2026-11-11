package fr.goat.argentic.service;

import fr.goat.argentic.dto.LoginRequest;
import fr.goat.argentic.dto.LoginResponse;
import fr.goat.argentic.model.Role;
import fr.goat.argentic.model.Utilisateur;
import fr.goat.argentic.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private static final String TEST_SECRET = "une_cle_secrete_de_test_qui_fait_au_moins_32_octets_pour_hmac!";

    private TestUtilisateurRepository utilisateurRepository;
    private TestPasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        utilisateurRepository = new TestUtilisateurRepository();
        passwordEncoder = new TestPasswordEncoder();

        SecretKeySpec secretKey = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(secretKey));
        JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

        jwtService = new JwtService(jwtEncoder, jwtDecoder);
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3600000L);

        authService = new AuthService(utilisateurRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Connexion réussie : identifiants valides renvoient un token JWT avec le rôle")
    void login_avecSucces_doitRetournerToken() {
        Utilisateur utilisateur = new Utilisateur("secretaire@argentic.fr", "encoded_secretaire", Role.SECRETAIRE);
        utilisateurRepository.saveUser(utilisateur);

        LoginRequest request = new LoginRequest("secretaire@argentic.fr", "secretaire");
        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();

        String username = jwtService.extractUsername(response.token());
        String role = jwtService.extractRole(response.token());
        assertThat(username).isEqualTo("secretaire@argentic.fr");
        assertThat(role).isEqualTo("SECRETAIRE");
    }

    @Test
    @DisplayName("Connexion échouée : utilisateur introuvable lève BadCredentialsException")
    void login_avecEmailInconnu_doitLeverBadCredentialsException() {
        LoginRequest request = new LoginRequest("inconnu@argentic.fr", "motdepasse");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");
    }

    @Test
    @DisplayName("Connexion échouée : mot de passe incorrect lève BadCredentialsException")
    void login_avecMotDePasseIncorrect_doitLeverBadCredentialsException() {
        Utilisateur utilisateur = new Utilisateur("secretaire@argentic.fr", "encoded_secretaire", Role.SECRETAIRE);
        utilisateurRepository.saveUser(utilisateur);

        LoginRequest request = new LoginRequest("secretaire@argentic.fr", "mauvais_mot_de_passe");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");
    }

    @Test
    @DisplayName("Récupération utilisateur courant réussie : renvoie MeResponse")
    void getCurrentUser_avecSucces_doitRetournerMeResponse() {
        Utilisateur utilisateur = new Utilisateur("gestionnaire@argentic.fr", "hash", Role.GESTIONNAIRE);
        utilisateur.setId(42L);
        utilisateurRepository.saveUser(utilisateur);

        fr.goat.argentic.dto.MeResponse me = authService.getCurrentUser("gestionnaire@argentic.fr");

        assertThat(me).isNotNull();
        assertThat(me.id()).isEqualTo(42L);
        assertThat(me.email()).isEqualTo("gestionnaire@argentic.fr");
        assertThat(me.role()).isEqualTo(Role.GESTIONNAIRE);
    }

    @Test
    @DisplayName("Récupération utilisateur courant échouée : lève ResourceNotFoundException")
    void getCurrentUser_avecEmailInconnu_doitLeverResourceNotFoundException() {
        assertThatThrownBy(() -> authService.getCurrentUser("inexistant@argentic.fr"))
                .isInstanceOf(fr.goat.argentic.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur non trouvé");
    }

    static class TestPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded_" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return ("encoded_" + rawPassword).equals(encodedPassword);
        }
    }

    static class TestUtilisateurRepository implements UtilisateurRepository {
        private final Map<String, Utilisateur> usersByEmail = new HashMap<>();

        public void saveUser(Utilisateur user) {
            usersByEmail.put(user.getEmail(), user);
        }

        @Override
        public Optional<Utilisateur> findByEmail(String email) {
            return Optional.ofNullable(usersByEmail.get(email));
        }

        @Override public void flush() {}
        @Override public <S extends Utilisateur> S saveAndFlush(S entity) { return entity; }
        @Override public <S extends Utilisateur> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<Utilisateur> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Utilisateur getOne(Long aLong) { return null; }
        @Override public Utilisateur getById(Long aLong) { return null; }
        @Override public Utilisateur getReferenceById(Long aLong) { return null; }
        @Override public <S extends Utilisateur> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Utilisateur> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends Utilisateur> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends Utilisateur> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Utilisateur> long count(Example<S> example) { return 0; }
        @Override public <S extends Utilisateur> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Utilisateur, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Utilisateur> S save(S entity) { return entity; }
        @Override public <S extends Utilisateur> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public Optional<Utilisateur> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Utilisateur> findAll() { return List.of(); }
        @Override public List<Utilisateur> findAllById(Iterable<Long> longs) { return List.of(); }
        @Override public long count() { return usersByEmail.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Utilisateur entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Utilisateur> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Utilisateur> findAll(Sort sort) { return List.of(); }
        @Override public Page<Utilisateur> findAll(Pageable pageable) { return Page.empty(); }
    }
}
