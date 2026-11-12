package fr.goat.argentic.controller;

import fr.goat.argentic.dto.ChiffreAffairesResponse;
import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.service.StatsService;
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
import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Statistiques", description = "Indicateurs réservés au rôle GESTIONNAIRE")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/ping")
    @Operation(summary = "Vérifier l'accès aux statistiques")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/top-photographes/seances")
    @Operation(
            summary = "Top 5 des photographes par nombre de séances terminées",
            description = "Retourne les 5 photographes ayant réalisé le plus grand nombre de séances au statut TERMINEE."
    )
    public ResponseEntity<List<TopPhotographeSeancesResponse>> getTopPhotographesParSeances() {
        return ResponseEntity.ok(statsService.getTopPhotographesParSeances());
    }

    @GetMapping("/top-photographes/duree")
    @Operation(
            summary = "Top 5 des photographes par durée de séances terminées",
            description = "Retourne les 5 photographes ayant réalisé les plus longues séances cumulées au statut TERMINEE."
    )
    public ResponseEntity<List<TopPhotographeDureeResponse>> getTopPhotographesParDuree() {
        return ResponseEntity.ok(statsService.getTopPhotographesParDuree());
    }

    @GetMapping("/chiffre-affaires")
    @Operation(
            summary = "Chiffre d'affaires HT et TTC sur une période",
            description = "Retourne la somme des factures au statut PAYEE dont la date de paiement est comprise entre du et au inclus."
    )
    public ResponseEntity<ChiffreAffairesResponse> getChiffreAffaires(
            @Parameter(description = "Date de début (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin (YYYY-MM-DD)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au
    ) {
        return ResponseEntity.ok(statsService.getChiffreAffaires(du, au));
    }
}
