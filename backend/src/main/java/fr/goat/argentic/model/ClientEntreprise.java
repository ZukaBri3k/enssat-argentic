package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("ENTREPRISE")
@Getter
@Setter
@NoArgsConstructor
public class ClientEntreprise extends Client {

    // Nullable en base (SINGLE_TABLE) ; obligatoire pour une entreprise, vérifié à la validation
    @Column(length = 255)
    private String raisonSociale;

    @Column(length = 32)
    private String numeroSirenTva;

    public ClientEntreprise(String email, String adressePostale, String raisonSociale, String numeroSirenTva) {
        super(email, adressePostale);
        this.raisonSociale = raisonSociale;
        this.numeroSirenTva = numeroSirenTva;
    }

    @Override
    public String getIdentite() {
        return raisonSociale;
    }
}
