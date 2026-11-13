package fr.goat.argentic.controller;

import fr.goat.argentic.dto.PhotographeRequest;
import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.service.PhotographeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/photographes")
@RequiredArgsConstructor
@Tag(name = "Photographes", description = "Gestion des photographes du studio")
public class PhotographeController {

    private final PhotographeService photographeService;

    @GetMapping
    @Operation(
            summary = "Lister les photographes",
            description = "Retourne les photographes triés par nom, avec leurs spécialités. Le paramètre actif filtre sur le statut."
    )
    @ApiResponse(responseCode = "200", description = "Liste des photographes")
    @ApiResponse(responseCode = "400", description = "Valeur du filtre actif invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<List<PhotographeResponse>> getPhotographes(
            @Parameter(description = "Filtrer sur le statut actif (true) ou inactif (false). Absent : tous les photographes.", example = "true")
            @RequestParam(required = false) Boolean actif
    ) {
        return ResponseEntity.ok(photographeService.findAll(actif));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un photographe", description = "Retourne le détail d'un photographe avec ses spécialités.")
    @ApiResponse(responseCode = "200", description = "Photographe trouvé")
    @ApiResponse(responseCode = "400", description = "Identifiant invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Photographe introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PhotographeResponse> getPhotographe(@PathVariable Long id) {
        return ResponseEntity.ok(photographeService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un photographe", description = "Crée un photographe actif avec au moins une spécialité.")
    @ApiResponse(responseCode = "201", description = "Photographe créé")
    @ApiResponse(responseCode = "400", description = "Données invalides (nom, email, au moins une spécialité)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Spécialité introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PhotographeResponse> createPhotographe(@Valid @RequestBody PhotographeRequest request) {
        PhotographeResponse cree = photographeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(cree.id())
                .toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Modifier un photographe",
            description = "Modifie le nom, l'email et les spécialités. Retirer une spécialité est refusé si le photographe a des séances à venir non annulées de ce type."
    )
    @ApiResponse(responseCode = "200", description = "Photographe modifié")
    @ApiResponse(responseCode = "400", description = "Données invalides (nom, email, au moins une spécialité)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Photographe ou spécialité introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée, ou spécialité retirée alors que des séances à venir en dépendent",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PhotographeResponse> updatePhotographe(
            @PathVariable Long id,
            @Valid @RequestBody PhotographeRequest request
    ) {
        return ResponseEntity.ok(photographeService.update(id, request));
    }
}
