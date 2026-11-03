package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PARTICULIER")
@Getter
@Setter
@NoArgsConstructor
public class ClientParticulier extends Client {

    // Nullable en base (SINGLE_TABLE) ; obligatoire pour un particulier, vérifié à la validation
    @Column(length = 150)
    private String nom;

    @Column(length = 150)
    private String prenom;

    public ClientParticulier(String email, String adressePostale, String nom, String prenom) {
        super(email, adressePostale);
        this.nom = nom;
        this.prenom = prenom;
    }

    @Override
    public String getIdentite() {
        return nom + " " + prenom;
    }
}
