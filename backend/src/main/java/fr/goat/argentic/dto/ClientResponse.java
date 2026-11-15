package fr.goat.argentic.dto;

import fr.goat.argentic.model.Client;
import fr.goat.argentic.model.ClientEntreprise;
import fr.goat.argentic.model.ClientParticulier;

public record ClientResponse(
        Long id,
        String type,           // "PARTICULIER" ou "ENTREPRISE"
        String email,
        String adressePostale,
        String identite,       // Le nom affiché (Nom Prénom ou Raison sociale)
        String nom,            // Rempli si particulier, null si entreprise
        String prenom,         // Rempli si particulier, null si entreprise
        String raisonSociale,  // Rempli si entreprise, null si particulier
        String numeroSirenTva  // Rempli si entreprise, null si particulier
) {
        public static ClientResponse fromEntity(Client client) {
                if (client instanceof ClientParticulier p) {
                        return new ClientResponse(
                                p.getId(),
                                "PARTICULIER",
                                p.getEmail(),
                                p.getAdressePostale(),
                                p.getIdentite(),
                                p.getNom(),
                                p.getPrenom(),
                                null,
                                null
                        );
                } else if (client instanceof ClientEntreprise e) {
                        return new ClientResponse(
                                e.getId(),
                                "ENTREPRISE",
                                e.getEmail(),
                                e.getAdressePostale(),
                                e.getIdentite(),
                                null,
                                null,
                                e.getRaisonSociale(),
                                e.getNumeroSirenTva()
                        );
                }
                throw new IllegalArgumentException("Type de client inconnu : " + client.getClass());
        }
}