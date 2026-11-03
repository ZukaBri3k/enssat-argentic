package fr.goat.argentic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Photographe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private boolean actif = true;

    // Au moins une spécialité : règle vérifiée dans le service
    @ManyToMany
    @JoinTable(
            name = "photographe_specialite",
            joinColumns = @JoinColumn(name = "photographe_id"),
            inverseJoinColumns = @JoinColumn(name = "specialite_id"))
    private Set<Specialite> specialites = new HashSet<>();

    public Photographe(String nom, String email, Set<Specialite> specialites) {
        this.nom = nom;
        this.email = email;
        this.specialites = new HashSet<>(specialites);
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    public boolean possedeSpecialite(Specialite specialite) {
        return specialites.stream().anyMatch(s -> s.getId().equals(specialite.getId()));
    }
}
