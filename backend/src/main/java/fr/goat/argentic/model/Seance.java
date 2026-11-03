package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photographe_id")
    private Photographe photographe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    // Type de séance
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialite_id")
    private Specialite specialite;

    @Column(name = "date_seance", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime heureDebut;

    @Column(nullable = false)
    private int dureeMinutes;

    @Column(nullable = false, length = 255)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSeance statut = StatutSeance.PLANIFIEE;

    public Seance(Photographe photographe, Client client, Specialite specialite,
                  LocalDate date, LocalTime heureDebut, int dureeMinutes, String lieu) {
        this.photographe = photographe;
        this.client = client;
        this.specialite = specialite;
        this.date = date;
        this.heureDebut = heureDebut;
        this.dureeMinutes = dureeMinutes;
        this.lieu = lieu;
    }

    public LocalDateTime getDebut() {
        return date.atTime(heureDebut);
    }

    // Attribut dérivé /fin : non stocké
    public LocalDateTime getFin() {
        return getDebut().plusMinutes(dureeMinutes);
    }

    public boolean estAVenir(LocalDateTime maintenant) {
        return getDebut().isAfter(maintenant);
    }

    public boolean estModifiable() {
        return !statut.estFinal();
    }

    public boolean chevauche(Seance autre) {
        return getDebut().isBefore(autre.getFin()) && autre.getDebut().isBefore(getFin());
    }
}
