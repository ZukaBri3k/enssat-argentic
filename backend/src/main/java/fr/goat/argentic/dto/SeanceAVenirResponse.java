package fr.goat.argentic.dto;

import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.StatutSeance;

import java.time.LocalDateTime;

// Résumé d'une séance à venir, affiché dans la confirmation de désactivation d'un photographe
public record SeanceAVenirResponse(
        Long id,
        LocalDateTime debut,
        LocalDateTime fin,
        String lieu,
        StatutSeance statut,
        String clientIdentite,
        SpecialiteResponse specialite
) {
    public static SeanceAVenirResponse fromEntity(Seance seance) {
        return new SeanceAVenirResponse(
                seance.getId(),
                seance.getDebut(),
                seance.getFin(),
                seance.getLieu(),
                seance.getStatut(),
                seance.getClient().getIdentite(),
                SpecialiteResponse.fromEntity(seance.getSpecialite())
        );
    }
}
