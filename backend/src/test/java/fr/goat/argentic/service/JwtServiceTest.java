package fr.goat.argentic.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import fr.goat.argentic.model.Role;
import fr.goat.argentic.model.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "une_cle_secrete_de_test_qui_fait_au_moins_32_octets_pour_hmac!";

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKeySpec secretKey = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

        jwtService = new JwtService(jwtEncoder, jwtDecoder);
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3600000L); // 1 heure
    }

    @Test
    @DisplayName("Génération de token pour SECRETAIRE : contient le rôle et l'email")
    void generateToken_roleSecretaire_doitContenirRoleEtEmail() {
        Utilisateur secretaire = new Utilisateur("secretaire@argentic.fr", "hash", Role.SECRETAIRE);

        String token = jwtService.generateToken(secretaire);

        assertThat(token).isNotBlank();
        Jwt decodedJwt = jwtDecoder.decode(token);
        assertThat(decodedJwt.getSubject()).isEqualTo("secretaire@argentic.fr");
        assertThat(decodedJwt.getClaimAsString("role")).isEqualTo("SECRETAIRE");
        assertThat(decodedJwt.getExpiresAt()).isAfter(decodedJwt.getIssuedAt());
    }

    @Test
    @DisplayName("Génération de token pour GESTIONNAIRE : contient le rôle GESTIONNAIRE")
    void generateToken_roleGestionnaire_doitContenirRoleGestionnaire() {
        Utilisateur gestionnaire = new Utilisateur("gestionnaire@argentic.fr", "hash", Role.GESTIONNAIRE);

        String token = jwtService.generateToken(gestionnaire);

        assertThat(token).isNotBlank();
        Jwt decodedJwt = jwtDecoder.decode(token);
        assertThat(decodedJwt.getSubject()).isEqualTo("gestionnaire@argentic.fr");
        assertThat(decodedJwt.getClaimAsString("role")).isEqualTo("GESTIONNAIRE");
    }

    @Test
    @DisplayName("Extraction du username et du rôle depuis le token")
    void extractUsernameEtRole_doiventExtraireLesRevendications() {
        String token = jwtService.generateToken("test@argentic.fr", Role.SECRETAIRE);

        String username = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);

        assertThat(username).isEqualTo("test@argentic.fr");
        assertThat(role).isEqualTo("SECRETAIRE");
    }
}
