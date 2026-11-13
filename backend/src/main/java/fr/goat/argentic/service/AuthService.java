package fr.goat.argentic.service;

import fr.goat.argentic.dto.LoginRequest;
import fr.goat.argentic.dto.LoginResponse;
import fr.goat.argentic.dto.MeResponse;
import fr.goat.argentic.exception.ResourceNotFoundException;
import fr.goat.argentic.model.Utilisateur;
import fr.goat.argentic.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.motDePasse(), utilisateur.getMotDePasse())) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        String token = jwtService.generateToken(utilisateur);
        return new LoginResponse(token);
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + email));

        return new MeResponse(utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole());
    }
}
