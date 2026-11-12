package fr.goat.argentic.dto;

import fr.goat.argentic.model.StatutSeance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SeanceResponse(
        Long id,
        LocalDate date,
        LocalTime heureDebut,
        int dureeMinutes,
        LocalDateTime debut,
        LocalDateTime fin,
        String lieu,
        StatutSeance statut,
        Long photographeId,
        String photographeNom,
        String photographeEmail,
        Long clientId,
        String clientIdentite,
        String clientEmail,
        SpecialiteResponse specialite,
        Long factureId,
        String factureNumero
) {
    // méthode de mapping statique fromEntity(Seance seance, Facture facture)
    public static SeanceResponse fromEntity(fr.goat.argentic.model.Seance s) {
        return new SeanceResponse(
                s.getId(),
                s.getDate(),
                s.getHeureDebut(),
                s.getDureeMinutes(),
                s.getDebut(),
                s.getFin(),
                s.getLieu(),
                s.getStatut(),
                s.getPhotographe().getId(),
                s.getPhotographe().getNom(),
                s.getPhotographe().getEmail(),
                s.getClient().getId(),
                s.getClient().getIdentite(),
                s.getClient().getEmail(),
                SpecialiteResponse.fromEntity(s.getSpecialite()),
                null,
                null
        );
    }
}
