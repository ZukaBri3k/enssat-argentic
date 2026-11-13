package fr.goat.argentic.controller;

import fr.goat.argentic.dto.PhotographeResponse;
import fr.goat.argentic.service.PhotographeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
