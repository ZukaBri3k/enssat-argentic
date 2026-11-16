package fr.goat.argentic.controller;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.dto.StatutSeanceRequest;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.service.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/seances")
@RequiredArgsConstructor
@Tag(name = "Séances", description = "Consultation du planning et gestion des séances photo")
public class SeanceController {

    private final SeanceService seanceService;

    @GetMapping
    @Operation(
            summary = "Séances avec des filtres",
            description = "Retourne la liste des séances en fonction des filtres."
    )
    @ApiResponse(responseCode = "200", description = "Liste des séances")
    @ApiResponse(responseCode = "400", description = "Paramètre de filtre invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Période incohérente (du > au)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<SeanceResponse>> getSeances(
            @Parameter(description = "Date de début (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin (YYYY-MM-DD)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Photographe", example = "1")
            @RequestParam(required = false) Long idPhotographe,
            @Parameter(description = "Client", example = "1")
            @RequestParam(required = false) Long idClient,
            @Parameter(description = "Statut", example = "PLANIFIEE")
            @RequestParam(required = false) StatutSeance statutSeance
    ) {
        return ResponseEntity.ok(seanceService.getSeance(du, au, idPhotographe, idClient, statutSeance));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Séances par id",
            description = "Retourne le détail d'une séance (avec photographe, client, spécialité, fin calculée et facture éventuelle)."
    )
    @ApiResponse(responseCode = "200", description = "Détail de la séance")
    @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Séance introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SeanceResponse> getSeanceById(
            @Parameter(description = "Identifiant de la séance", example = "1")
            @PathVariable("id") Long idSeance
    ) {
        return ResponseEntity.ok(seanceService.getSeanceById(idSeance));
    }

    @PatchMapping("/{id}/statut")
    @Operation(
            summary = "Changer le statut d'une séance",
            description = "Modifie le statut d'une séance selon les transitions autorisées (PLANIFIEE -> CONFIRMEE | ANNULEE ; CONFIRMEE -> TERMINEE | ANNULEE ; TERMINEE et ANNULEE sont finaux). Le passage à TERMINEE n'est autorisé que si la fin (début + durée) de la séance est passée."
    )
    @ApiResponse(responseCode = "200", description = "Statut de la séance mis à jour")
    @ApiResponse(responseCode = "400", description = "Requête ou statut invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Séance introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Transition de statut non autorisée ou fin de séance non encore passée",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SeanceResponse> changerStatut(
            @Parameter(description = "Identifiant de la séance", example = "1")
            @PathVariable("id") Long idSeance,
            @Valid @RequestBody StatutSeanceRequest request
    ) {
        return ResponseEntity.ok(seanceService.changerStatut(idSeance, request.statut()));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Supprimer une séance",
            description = "Supprime définitivement une séance. Refusé (409) si la séance a une facture associée."
    )
    @ApiResponse(responseCode = "204", description = "Séance supprimée")
    @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Séance introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "La séance a une facture : impossible de la supprimer",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> deleteSeance(
            @Parameter(description = "Identifiant de la séance", example = "1")
            @PathVariable("id") Long idSeance
    ) {
        seanceService.deleteSeance(idSeance);
        return ResponseEntity.noContent().build();
    }

}
