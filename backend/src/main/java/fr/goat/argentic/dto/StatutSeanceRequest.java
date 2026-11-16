package fr.goat.argentic.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import fr.goat.argentic.model.StatutSeance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StatutSeanceRequest(
        @Schema(description = "Nouveau statut de la séance", example = "CONFIRMEE")
        @NotNull(message = "Le statut est obligatoire")
        @JsonAlias("nouveauStatut")
        StatutSeance statut
) {
}
