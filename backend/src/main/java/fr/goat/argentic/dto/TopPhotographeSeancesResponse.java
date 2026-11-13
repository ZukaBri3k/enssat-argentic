package fr.goat.argentic.dto;

public record TopPhotographeSeancesResponse(
        Long photographeId,
        String nom,
        String email,
        long nombreSeances
) {}