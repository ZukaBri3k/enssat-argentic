package fr.goat.argentic.controller;

import fr.goat.argentic.dto.FactureRequest;
import fr.goat.argentic.dto.FactureResponse;
import fr.goat.argentic.dto.PaiementFactureRequest;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.service.FactureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Factures", description = "Génération, consultation, paiement et annulation des factures")
public class FactureController {

    private final FactureService factureService;

    @PostMapping("/seances/{id}/facture")
    @Operation(
            summary = "Générer la facture d'une séance terminée",
            description = "Génère une facture au statut initial EN_ATTENTE pour la séance indiquée. "
                    + "Le numéro séquentiel est au format FAC-{année}-{0001} (max + 1 par année d'émission). "
                    + "Les coordonnées du client, le détail de la prestation et le nom du photographe sont copiés et figés. "
                    + "Refusé (409) si la séance n'est pas au statut TERMINEE ou possède déjà une facture."
    )
    @ApiResponse(responseCode = "201", description = "Facture générée avec succès")
    @ApiResponse(responseCode = "400", description = "Montant HT ou taux de TVA invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Séance introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Séance non TERMINEE ou ayant déjà une facture associée",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<FactureResponse> genererFacture(
            @Parameter(description = "Identifiant de la séance à facturer", example = "1")
            @PathVariable("id") Long idSeance,
            @Valid @RequestBody FactureRequest request
    ) {
        FactureResponse creee = factureService.genererFacture(idSeance, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creee);
    }

    @GetMapping("/factures")
    @Operation(
            summary = "Lister les factures avec filtres",
            description = "Retourne la liste des factures filtrée par statut de paiement et/ou période d'émission (du / au)."
    )
    @ApiResponse(responseCode = "200", description = "Liste des factures")
    @ApiResponse(responseCode = "400", description = "Paramètre de filtre invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Période incohérente (du > au)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<FactureResponse>> getFactures(
            @Parameter(description = "Statut de paiement (EN_ATTENTE, PAYEE, ANNULEE)", example = "EN_ATTENTE")
            @RequestParam(required = false) StatutPaiement statut,
            @Parameter(hidden = true)
            @RequestParam(required = false) StatutPaiement statutPaiement,
            @Parameter(description = "Date de début d'émission (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin d'émission (YYYY-MM-DD)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
    ) {
        StatutPaiement statutEffectif = statut != null ? statut : statutPaiement;
        return ResponseEntity.ok(factureService.getFactures(statutEffectif, du, au));
    }

    @GetMapping("/factures/{id}")
    @Operation(
            summary = "Consulter une facture par son identifiant",
            description = "Retourne le détail complet d'une facture avec les coordonnées client figées, la prestation figée et le montant TTC calculé."
    )
    @ApiResponse(responseCode = "200", description = "Détail de la facture")
    @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Facture introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<FactureResponse> getFactureById(
            @Parameter(description = "Identifiant de la facture", example = "1")
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(factureService.getFactureById(id));
    }

    @PostMapping("/factures/{id}/paiement")
    @Operation(
            summary = "Enregistrer le paiement d'une facture",
            description = "Passe la facture au statut PAYEE avec la date de règlement saisie. "
                    + "La date de paiement doit être comprise entre la date d'émission et aujourd'hui. "
                    + "Refusé (409) si la facture n'est pas EN_ATTENTE ou si la date est hors de cet intervalle."
    )
    @ApiResponse(responseCode = "200", description = "Paiement enregistré, facture PAYEE")
    @ApiResponse(responseCode = "400", description = "Date de paiement manquante ou mal formée",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Facture introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Facture déjà PAYEE ou ANNULEE, ou date de paiement antérieure à l'émission ou dans le futur",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<FactureResponse> enregistrerPaiement(
            @Parameter(description = "Identifiant de la facture", example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody PaiementFactureRequest request
    ) {
        return ResponseEntity.ok(factureService.enregistrerPaiement(id, request));
    }

    @PostMapping("/factures/{id}/annulation")
    @Operation(
            summary = "Annuler une facture",
            description = "Passe la facture au statut ANNULEE. L'annulation est définitive : "
                    + "la séance associée ne pourra pas être refacturée. "
                    + "Refusé (409) si la facture n'est pas EN_ATTENTE."
    )
    @ApiResponse(responseCode = "200", description = "Facture annulée")
    @ApiResponse(responseCode = "404", description = "Facture introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Facture déjà PAYEE ou ANNULEE",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<FactureResponse> annulerFacture(
            @Parameter(description = "Identifiant de la facture", example = "1")
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(factureService.annulerFacture(id));
    }
}
