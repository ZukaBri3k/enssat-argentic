package fr.goat.argentic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Données de génération d'une facture pour une séance terminée")
public record FactureRequest(

        @Schema(description = "Montant hors taxes (HT) de la prestation en euros", example = "250.00")
        @NotNull(message = "Le montant HT est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant HT doit être strictement supérieur à 0")
        @Digits(integer = 10, fraction = 2, message = "Le montant HT doit comporter au maximum 2 décimales")
        BigDecimal montantHt,

        @Schema(description = "Taux de TVA en pourcentage (optionnel, par défaut 20.00)", example = "20.00", defaultValue = "20.00")
        @DecimalMin(value = "0.00", message = "Le taux de TVA ne peut pas être négatif")
        @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100 %")
        @Digits(integer = 3, fraction = 2, message = "Le taux de TVA doit comporter au maximum 2 décimales")
        BigDecimal tauxTva
) {
}
