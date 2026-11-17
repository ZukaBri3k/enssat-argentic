package fr.goat.argentic.dto;

import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.StatutPaiement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Représentation complète d'une facture avec ses données figées et le montant TTC calculé")
public record FactureResponse(
        @Schema(description = "Identifiant unique de la facture", example = "1")
        Long id,

        @Schema(description = "Numéro séquentiel de la facture au format FAC-{année}-{0001}", example = "FAC-2026-0001")
        String numero,

        @Schema(description = "Identifiant de la séance facturée", example = "15")
        Long seanceId,

        @Schema(description = "Date d'émission de la facture", example = "2026-11-17")
        LocalDate dateEmission,

        @Schema(description = "Montant hors taxes (HT)", example = "250.00")
        BigDecimal montantHt,

        @Schema(description = "Taux de TVA appliqué en pourcentage", example = "20.00")
        BigDecimal tauxTva,

        @Schema(description = "Montant toutes taxes comprises (TTC) calculé", example = "300.00")
        BigDecimal montantTtc,

        @Schema(description = "Statut de paiement de la facture", example = "EN_ATTENTE")
        StatutPaiement statutPaiement,

        @Schema(description = "Date de règlement si la facture est PAYEE", example = "2026-11-27", nullable = true)
        LocalDate datePaiement,

        @Schema(description = "Coordonnées du client figées au moment de la génération")
        CoordonneesFacturation coordonneesClient,

        @Schema(description = "Détail de la prestation et du photographe figé au moment de la génération")
        PrestationFacturee prestation
) {

    public static FactureResponse fromEntity(Facture facture) {
        return new FactureResponse(
                facture.getId(),
                facture.getNumero(),
                facture.getSeance() != null ? facture.getSeance().getId() : null,
                facture.getDateEmission(),
                facture.getMontantHt(),
                facture.getTauxTva(),
                facture.getMontantTtc(),
                facture.getStatutPaiement(),
                facture.getDatePaiement(),
                facture.getCoordonneesClient(),
                facture.getPrestation()
        );
    }
}
