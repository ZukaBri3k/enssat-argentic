package fr.goat.argentic.controller;

import fr.goat.argentic.dto.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) StatutSeance statutSeance,
            Authentication authentication
    ) {
        if (estPhotographe(authentication)) {
            return ResponseEntity.ok(
                    seanceService.getSeancesPourPhotographe(authentication.getName(), du, au, idClient, statutSeance)
            );
        }
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
            @PathVariable("id") Long idSeance,
            Authentication authentication
    ) {
        if (estPhotographe(authentication)) {
            return ResponseEntity.ok(seanceService.getSeanceByIdPourPhotographe(idSeance, authentication.getName()));
        }
        return ResponseEntity.ok(seanceService.getSeanceById(idSeance));
    }

    private boolean estPhotographe(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PHOTOGRAPHE".equals(a.getAuthority()));
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

    @PostMapping
    @Operation(
            summary = "Créer une séance",
            description = "Crée une séance au statut initial PLANIFIEE. Début (date + heure) doit être >= maintenant. La séance ne peut pas dépasser minuit. Le photographe doit être actif et posséder la spécialité. Pas de chevauchement avec une autre séance non ANNULEE du photographe."
    )
    @ApiResponse(responseCode = "201", description = "Séance créée avec succès")
    @ApiResponse(responseCode = "400", description = "Données invalides ou manquantes",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Photographe, client ou spécialité introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Règle métier violée (début dans le passé, dépasse minuit, photographe inactif ou sans spécialité, chevauchement)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SeanceResponse> createSeance(@Valid @RequestBody SeanceRequest request) {
        SeanceResponse cree = seanceService.addSeance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cree);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Modifier une séance",
            description = "Modifie une séance existante. Refusé (409) si la séance est TERMINEE ou ANNULEE. Début >= maintenant. La séance ne peut pas dépasser minuit. Le photographe doit être actif et posséder la spécialité. Pas de chevauchement avec une autre séance non ANNULEE du même photographe (en excluant la séance elle-même)."
    )
    @ApiResponse(responseCode = "200", description = "Séance modifiée avec succès")
    @ApiResponse(responseCode = "400", description = "Données ou identifiant invalides",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Séance, photographe, client ou spécialité introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Séance TERMINEE ou ANNULEE, début dans le passé, dépasse minuit, photographe inactif ou sans spécialité, chevauchement",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<SeanceResponse> updateSeance(
            @Parameter(description = "Identifiant de la séance à modifier", example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody SeanceRequest request
    ) {
        SeanceResponse cree = seanceService.updateSeance(id, request);
        return ResponseEntity.ok(cree);
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
