package fr.goat.argentic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Données d'enregistrement du paiement d'une facture")
public record PaiementFactureRequest(

        @Schema(description = "Date de règlement de la facture (entre la date d'émission et aujourd'hui)", example = "2026-11-17")
        @NotNull(message = "La date de paiement est obligatoire")
        LocalDate datePaiement
) {
}
