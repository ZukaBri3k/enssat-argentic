package fr.goat.argentic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank(message = "Le type de client est obligatoire")
        String type,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "Format d'adresse email invalide")
        String email,

        @NotBlank(message = "L'adresse postale est obligatoire")
        String adressePostale,

        String nom,
        String prenom,
        String raisonSociale,
        String numeroSirenTva
) {}