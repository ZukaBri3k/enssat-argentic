package fr.goat.argentic.dto;

import fr.goat.argentic.model.Photographe;

import java.util.Comparator;
import java.util.List;

public record PhotographeResponse(
        Long id,
        String nom,
        String email,
        boolean actif,
        List<SpecialiteResponse> specialites
) {
    public static PhotographeResponse fromEntity(Photographe photographe) {
        List<SpecialiteResponse> specialites = photographe.getSpecialites().stream()
                .map(SpecialiteResponse::fromEntity)
                .sorted(Comparator.comparing(SpecialiteResponse::id))
                .toList();
        return new PhotographeResponse(
                photographe.getId(),
                photographe.getNom(),
                photographe.getEmail(),
                photographe.isActif(),
                specialites
        );
    }
}
