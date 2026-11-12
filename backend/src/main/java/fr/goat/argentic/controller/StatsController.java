package fr.goat.argentic.controller;

import fr.goat.argentic.dto.TopPhotographeDureeResponse;
import fr.goat.argentic.dto.TopPhotographeSeancesResponse;
import fr.goat.argentic.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
