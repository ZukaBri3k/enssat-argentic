package fr.goat.argentic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record SeanceRequest(
        @Schema(description = "Date de la séance (YYYY-MM-DD)", example = "2026-11-20")
        @NotNull(message = "La date est obligatoire")
        @JsonAlias("dateSeance")
        LocalDate date,

        @Schema(description = "Heure de début (HH:mm)", example = "14:00")
        @NotNull(message = "L'heure de début est obligatoire")
        LocalTime heureDebut,

        @Schema(description = "Durée en minutes", example = "120")
        @NotNull(message = "La durée est obligatoire")
        @Min(value = 1, message = "La durée doit être d'au moins 1 minute")
        @JsonAlias("duree")
        Integer dureeMinutes,

        @Schema(description = "Lieu de la prestation", example = "Château de Rosanbo")
        @NotBlank(message = "Le lieu est obligatoire")
        @Size(max = 255, message = "Le lieu ne doit pas dépasser 255 caractères")
        String lieu,

        @Schema(description = "Identifiant de la spécialité (type de séance)", example = "2")
        @NotNull(message = "La spécialité est obligatoire")
        @JsonAlias("idSpecialite")
        Long specialiteId,

        @Schema(description = "Identifiant du photographe", example = "10")
        @NotNull(message = "Le photographe est obligatoire")
        @JsonAlias("idPhotographe")
        Long photographeId,

        @Schema(description = "Identifiant du client", example = "20")
        @NotNull(message = "Le client est obligatoire")
        @JsonAlias("idClient")
        Long clientId
) {
}