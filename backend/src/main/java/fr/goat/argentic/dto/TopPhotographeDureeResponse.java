package fr.goat.argentic.dto;

public record TopPhotographeDureeResponse(
        Long photographeId,
        String nom,
        String email,
        long duree
) {}