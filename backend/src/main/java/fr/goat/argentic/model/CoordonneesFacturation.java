package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Coordonnées du client figées au moment de la génération de la facture. */
@Embeddable
public record CoordonneesFacturation(
        @Column(name = "client_identite", nullable = false, length = 320)
        String identite,

        @Column(name = "client_adresse_postale", nullable = false, columnDefinition = "TEXT")
        String adressePostale,

        @Column(name = "client_email", nullable = false, length = 254)
        String email,

        @Column(name = "client_numero_siren_tva", length = 32)
        String numeroSirenTva
) {
}
