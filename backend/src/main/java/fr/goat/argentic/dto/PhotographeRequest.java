package fr.goat.argentic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PhotographeRequest(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "L'adresse email doit être valide")
        @Size(max = 254, message = "L'adresse email ne doit pas dépasser 254 caractères")
        String email,

        @NotEmpty(message = "Au moins une spécialité est requise")
        Set<@NotNull Long> specialiteIds,

        @Size(min = 4, max = 100, message = "Le mot de passe doit contenir entre 4 et 100 caractères")
        String motDePasse
) {
    public PhotographeRequest(String nom, String email, Set<Long> specialiteIds) {
        this(nom, email, specialiteIds, null);
    }
}
