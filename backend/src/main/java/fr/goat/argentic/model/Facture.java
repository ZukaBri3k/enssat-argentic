package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Au plus une facture par séance
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", unique = true)
    private Seance seance;

    @Column(nullable = false, unique = true, length = 32)
    private String numero;

    @Column(nullable = false)
    private LocalDate dateEmission;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montantHt;

    // En pourcentage, ex. 20.00
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;

    private LocalDate datePaiement;

    @Embedded
    private CoordonneesFacturation coordonneesClient;

    @Embedded
    private PrestationFacturee prestation;

    public Facture(Seance seance, String numero, LocalDate dateEmission, BigDecimal montantHt, BigDecimal tauxTva,
                   CoordonneesFacturation coordonneesClient, PrestationFacturee prestation) {
        this.seance = seance;
        this.numero = numero;
        this.dateEmission = dateEmission;
        this.montantHt = montantHt;
        this.tauxTva = tauxTva;
        this.coordonneesClient = coordonneesClient;
        this.prestation = prestation;
    }

    // Attribut dérivé /montantTTC : non stocké
    public BigDecimal getMontantTtc() {
        BigDecimal coefficient = BigDecimal.ONE.add(tauxTva.movePointLeft(2));
        return montantHt.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }
}
