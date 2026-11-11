package fr.goat.argentic.dto;

import fr.goat.argentic.model.Specialite;

public record SpecialiteResponse(
        Long id,
        String libelle
) {
    public static SpecialiteResponse fromEntity(Specialite specialite) {
        return new SpecialiteResponse(specialite.getId(), specialite.getLibelle());
    }
}
