package fr.goat.argentic.controller;

import fr.goat.argentic.dto.SeanceResponse;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.service.SeanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.LongSummaryStatistics;

@RestController
@RequestMapping("/seances")
@RequiredArgsConstructor
@Tag(name = "Séances", description = "Consultation du planning et gestion des séances photo")
public class SeanceController {

    private final SeanceService seanceService;

    // GET /seances (avec filtres : du, au, photographeId, clientId, statut)

    @GetMapping
    @Operation(
            summary = "Séances avec des filtres",
            description = "Retourne la liste des séances en fonction des filtres."
    )
    public ResponseEntity<List<SeanceResponse>> getSeances(
            @Parameter(description = "Date de début (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam (required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin (YYYY-MM-DD)", example = "2026-12-31")
            @RequestParam (required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @Parameter(description = "Photographe", example = "1")
            @RequestParam(required = false) Long idPhotographe,
            @Parameter(description = "Client", example = "1")
            @RequestParam(required = false) Long idClient,
            @Parameter(description = "Statut", example = "PLANIFIEE")
            @RequestParam(required = false) StatutSeance statutSeance
    ) {
        return ResponseEntity.ok(seanceService.getSeance(du, au, idPhotographe, idClient, statutSeance));
    }

    // TODO: GET /seances/{id} (avec photographe, client, spécialité, fin calculée, facture éventuelle)

}
