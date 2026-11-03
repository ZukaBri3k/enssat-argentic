package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_client", length = 20)
@Getter
@Setter
@NoArgsConstructor
public abstract class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String adressePostale;

    protected Client(String email, String adressePostale) {
        this.email = email;
        this.adressePostale = adressePostale;
    }

    /** Nom affiché et copié sur les factures : « Nom Prénom » ou raison sociale. */
    public abstract String getIdentite();
}
