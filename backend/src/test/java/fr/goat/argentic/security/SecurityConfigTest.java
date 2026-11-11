package fr.goat.argentic.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "jwtSecret", "une_cle_secrete_de_test_qui_fait_au_moins_32_octets_pour_hmac!");
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:5173");
    }

    @Test
    @DisplayName("Hiérarchie des rôles : GESTIONNAIRE hérite de tous les droits de SECRETAIRE")
    void roleHierarchy_gestionnaireImpliqueSecretaire() {
        RoleHierarchy hierarchy = securityConfig.roleHierarchy();

        Collection<? extends GrantedAuthority> reachable = hierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))
        );

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_GESTIONNAIRE", "ROLE_SECRETAIRE");
    }

    @Test
    @DisplayName("Hiérarchie des rôles : SECRETAIRE n'a pas les droits de GESTIONNAIRE")
    void roleHierarchy_secretaireNePossedePasGestionnaire() {
        RoleHierarchy hierarchy = securityConfig.roleHierarchy();

        Collection<? extends GrantedAuthority> reachable = hierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_SECRETAIRE"))
        );

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsOnly("ROLE_SECRETAIRE");
    }

    @Test
    @DisplayName("JwtAuthenticationConverter mappe la claim role vers ROLE_<ROLE>")
    void jwtAuthenticationConverter_mappeRoleVersRoleAuthority() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = new Jwt(
                "token-val",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "secretaire@argentic.fr", "role", "SECRETAIRE")
        );

        var authentication = converter.convert(jwt);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_SECRETAIRE");
    }
}
