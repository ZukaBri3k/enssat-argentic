package fr.goat.argentic.dto;

import fr.goat.argentic.model.Role;

public record MeResponse(
        Long id,
        String email,
        Role role
) {
}
