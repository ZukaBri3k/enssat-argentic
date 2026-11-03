package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.LocalTime;

/** Détail de la séance figé au moment de la génération de la facture. */
@Embeddable
public record PrestationFacturee(
        @Column(name = "prestation_date", nullable = false)
        LocalDate date,

        @Column(name = "prestation_heure_debut", nullable = false)
        LocalTime heureDebut,

        @Column(name = "prestation_duree_minutes", nullable = false)
        int dureeMinutes,

        @Column(name = "prestation_type", nullable = false, length = 100)
        String typeSeance,

        @Column(name = "photographe_nom", nullable = false, length = 150)
        String nomPhotographe
) {

    public static PrestationFacturee depuis(Seance seance) {
        return new PrestationFacturee(seance.getDate(), seance.getHeureDebut(), seance.getDureeMinutes(),
                seance.getSpecialite().getLibelle(), seance.getPhotographe().getNom());
    }
}
