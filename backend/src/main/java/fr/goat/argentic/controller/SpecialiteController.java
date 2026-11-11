package fr.goat.argentic.controller;

import fr.goat.argentic.dto.SpecialiteResponse;
import fr.goat.argentic.service.SpecialiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/specialites")
@RequiredArgsConstructor
@Tag(name = "Spécialités", description = "Consultation de la liste des spécialités (lecture seule)")
public class SpecialiteController {

    private final SpecialiteService specialiteService;

    @GetMapping
    @Operation(summary = "Lister toutes les spécialités", description = "Retourne la liste complète des spécialités photographiques disponibles.")
    public ResponseEntity<List<SpecialiteResponse>> getSpecialites() {
        return ResponseEntity.ok(specialiteService.findAll());
    }
}
