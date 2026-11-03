package fr.goat.argentic.config;

import fr.goat.argentic.model.Client;
import fr.goat.argentic.model.ClientEntreprise;
import fr.goat.argentic.model.ClientParticulier;
import fr.goat.argentic.model.CoordonneesFacturation;
import fr.goat.argentic.model.Facture;
import fr.goat.argentic.model.Photographe;
import fr.goat.argentic.model.PrestationFacturee;
import fr.goat.argentic.model.Role;
import fr.goat.argentic.model.Seance;
import fr.goat.argentic.model.Specialite;
import fr.goat.argentic.model.StatutPaiement;
import fr.goat.argentic.model.StatutSeance;
import fr.goat.argentic.model.Utilisateur;
import fr.goat.argentic.repository.ClientRepository;
import fr.goat.argentic.repository.FactureRepository;
import fr.goat.argentic.repository.PhotographeRepository;
import fr.goat.argentic.repository.SeanceRepository;
import fr.goat.argentic.repository.SpecialiteRepository;
import fr.goat.argentic.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static fr.goat.argentic.model.StatutPaiement.ANNULEE;
import static fr.goat.argentic.model.StatutPaiement.EN_ATTENTE;
import static fr.goat.argentic.model.StatutPaiement.PAYEE;
import static fr.goat.argentic.model.StatutSeance.CONFIRMEE;
import static fr.goat.argentic.model.StatutSeance.PLANIFIEE;
import static fr.goat.argentic.model.StatutSeance.TERMINEE;

/**
 * Peuple une base vide avec un jeu de données de développement.
 * Toutes les dates sont relatives au jour du lancement : le jeu reste cohérent (passé / à venir) quel que soit le jour.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final BigDecimal TVA = new BigDecimal("20.00");

    private final SpecialiteRepository specialiteRepository;
    private final PhotographeRepository photographeRepository;
    private final ClientRepository clientRepository;
    private final SeanceRepository seanceRepository;
    private final FactureRepository factureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<Integer, Integer> compteursFactureParAnnee = new HashMap<>();
    private LocalDate aujourdhui;

    @Override
    @Transactional
    public void run(String... args) {
        if (specialiteRepository.count() > 0) {
            log.info("Base déjà peuplée : seed ignoré");
            return;
        }
        aujourdhui = LocalDate.now();

        utilisateurRepository.save(new Utilisateur("secretaire@argentic.fr", passwordEncoder.encode("secretaire"), Role.SECRETAIRE));
        utilisateurRepository.save(new Utilisateur("gestionnaire@argentic.fr", passwordEncoder.encode("gestionnaire"), Role.GESTIONNAIRE));

        Specialite reportage = specialiteRepository.save(new Specialite("Reportage"));
        Specialite mariage = specialiteRepository.save(new Specialite("Mariage et fêtes"));
        Specialite famille = specialiteRepository.save(new Specialite("Famille"));

        Photographe camille = photographe("Camille Martin", "camille.martin@argentic.fr", reportage, famille);
        Photographe lucas = photographe("Lucas Bernard", "lucas.bernard@argentic.fr", mariage);
        Photographe ines = photographe("Inès Petit", "ines.petit@argentic.fr", mariage, famille);
        Photographe hugo = photographe("Hugo Durand", "hugo.durand@argentic.fr", reportage);
        Photographe lea = photographe("Léa Moreau", "lea.moreau@argentic.fr", reportage, mariage, famille);
        Photographe theo = photographe("Théo Laurent", "theo.laurent@argentic.fr", famille);
        theo.desactiver();

        Client dupont = clientRepository.save(new ClientParticulier("marie.dupont@mail.fr",
                "12 rue de Kerampont, 22300 Lannion", "Dupont", "Marie"));
        Client lefebvre = clientRepository.save(new ClientParticulier("thomas.lefebvre@mail.fr",
                "4 place du Martray, 22220 Tréguier", "Lefebvre", "Thomas"));
        Client garcia = clientRepository.save(new ClientParticulier("sofia.garcia@mail.fr",
                "27 boulevard Thiers, 22700 Perros-Guirec", "Garcia", "Sofia"));
        Client roux = clientRepository.save(new ClientParticulier("antoine.roux@mail.fr",
                "3 rue Jean Savidan, 22300 Lannion", "Roux", "Antoine"));
        Client atelierBreizh = clientRepository.save(new ClientEntreprise("contact@atelier-breizh.fr",
                "15 rue de la Poterie, 22300 Lannion", "Atelier Breizh SARL", "FR40123456789"));
        Client lannionTech = clientRepository.save(new ClientEntreprise("com@lannion-tech.fr",
                "6 rue de Broglie, 22300 Lannion", "Lannion Tech SAS", "FR87987654321"));
        Client jardinsTregor = clientRepository.save(new ClientEntreprise("evenements@jardins-tregor.fr",
                "Route de Pleumeur, 22560 Trébeurden", "Les Jardins du Trégor", "FR55456789123"));

        // Séances passées terminées. Classement attendu des stats :
        // - nombre : Léa 5, Camille 4, Lucas 3, Inès 3, Hugo 2, Théo 1
        // - durée  : Lucas 1440 min, Léa 810, Camille 630, Inès 570, Hugo 420, Théo 60
        facturer(seance(lea, dupont, famille, -120, "10:00", 120, "Studio Argentic, Lannion", TERMINEE), "250.00", PAYEE);
        facturer(seance(lucas, lefebvre, mariage, -105, "14:00", 480, "Château de Tonquédec", TERMINEE), "1800.00", PAYEE);
        facturer(seance(camille, atelierBreizh, reportage, -100, "09:00", 180, "Atelier Breizh, Lannion", TERMINEE), "600.00", PAYEE);
        facturer(seance(ines, garcia, famille, -90, "11:00", 90, "Plage de Trestraou, Perros-Guirec", TERMINEE), "220.00", PAYEE);
        facturer(seance(lea, lannionTech, reportage, -80, "15:00", 240, "Lannion Tech, Lannion", TERMINEE), "750.00", PAYEE);
        facturer(seance(lucas, roux, mariage, -75, "13:00", 480, "Mairie de Perros-Guirec", TERMINEE), "1900.00", PAYEE);
        facturer(seance(hugo, jardinsTregor, reportage, -70, "10:00", 120, "Les Jardins du Trégor, Trébeurden", TERMINEE), "450.00", PAYEE);
        facturer(seance(theo, dupont, famille, -60, "09:30", 60, "Studio Argentic, Lannion", TERMINEE), "150.00", PAYEE);
        facturer(seance(camille, lefebvre, famille, -55, "14:00", 120, "Parc de Sainte-Anne, Lannion", TERMINEE), "260.00", PAYEE);
        facturer(seance(ines, garcia, mariage, -45, "10:00", 360, "Manoir de Kerhuel, Perros-Guirec", TERMINEE), "1400.00", PAYEE);
        facturer(seance(lea, roux, famille, -40, "16:00", 90, "Studio Argentic, Lannion", TERMINEE), "200.00", PAYEE);
        facturer(seance(hugo, atelierBreizh, reportage, -35, "08:00", 300, "Atelier Breizh, Lannion", TERMINEE), "900.00", EN_ATTENTE);
        facturer(seance(lucas, garcia, mariage, -30, "12:00", 480, "Abbaye de Bégard", TERMINEE), "2000.00", EN_ATTENTE);
        facturer(seance(camille, lannionTech, reportage, -25, "10:00", 180, "Lannion Tech, Lannion", TERMINEE), "550.00", ANNULEE);
        facturer(seance(lea, lefebvre, mariage, -20, "14:00", 240, "Salle des fêtes de Trégastel", TERMINEE), "1100.00", PAYEE);

        // Terminées sans facture : pour tester « Générer la facture »
        seance(ines, dupont, famille, -15, "09:00", 120, "Studio Argentic, Lannion", TERMINEE);
        seance(camille, jardinsTregor, reportage, -10, "10:00", 150, "Les Jardins du Trégor, Trébeurden", TERMINEE);
        seance(lea, atelierBreizh, reportage, -7, "11:00", 120, "Atelier Breizh, Lannion", TERMINEE);

        // Confirmée dont la fin est passée : pour tester le passage à TERMINEE
        seance(hugo, lannionTech, reportage, -5, "15:00", 60, "Lannion Tech, Lannion", CONFIRMEE);

        seance(lucas, dupont, mariage, -50, "10:00", 120, "Mairie de Lannion", StatutSeance.ANNULEE);

        // À venir. Hugo en a deux : pour tester la désactivation.
        // Léa a deux séances le même jour (10h-12h, 14h-16h) : pour tester le chevauchement.
        seance(camille, garcia, famille, 2, "10:00", 120, "Plage de Beg Léguer, Lannion", CONFIRMEE);
        seance(lucas, lefebvre, mariage, 3, "14:00", 480, "Château de Rosanbo", CONFIRMEE);
        seance(camille, atelierBreizh, reportage, 4, "16:00", 60, "Atelier Breizh, Lannion", StatutSeance.ANNULEE);
        seance(hugo, jardinsTregor, reportage, 5, "09:00", 180, "Les Jardins du Trégor, Trébeurden", PLANIFIEE);
        seance(hugo, dupont, reportage, 6, "11:00", 90, "Studio Argentic, Lannion", CONFIRMEE);
        seance(lea, lannionTech, reportage, 7, "10:00", 120, "Lannion Tech, Lannion", PLANIFIEE);
        seance(lea, roux, famille, 7, "14:00", 120, "Studio Argentic, Lannion", PLANIFIEE);
        seance(ines, garcia, mariage, 10, "15:00", 240, "Manoir de Kerhuel, Perros-Guirec", PLANIFIEE);
        seance(lucas, roux, mariage, 14, "10:00", 360, "Abbaye de Beauport, Paimpol", PLANIFIEE);
        seance(ines, lefebvre, famille, 21, "09:30", 120, "Studio Argentic, Lannion", PLANIFIEE);

        log.info("Seed terminé : {} photographes, {} clients, {} séances, {} factures, {} utilisateurs",
                photographeRepository.count(), clientRepository.count(), seanceRepository.count(),
                factureRepository.count(), utilisateurRepository.count());
    }

    private Photographe photographe(String nom, String email, Specialite... specialites) {
        return photographeRepository.save(new Photographe(nom, email, Set.of(specialites)));
    }

    private Seance seance(Photographe photographe, Client client, Specialite specialite, int decalageJours,
                          String heureDebut, int dureeMinutes, String lieu, StatutSeance statut) {
        Seance seance = new Seance(photographe, client, specialite, aujourdhui.plusDays(decalageJours),
                LocalTime.parse(heureDebut), dureeMinutes, lieu);
        seance.setStatut(statut);
        return seanceRepository.save(seance);
    }

    /** Facture émise le lendemain de la séance ; payée 10 jours après l'émission si PAYEE. */
    private void facturer(Seance seance, String montantHt, StatutPaiement statut) {
        LocalDate dateEmission = seance.getDate().plusDays(1);
        Facture facture = new Facture(seance, numeroFacture(dateEmission), dateEmission, new BigDecimal(montantHt), TVA,
                CoordonneesFacturation.depuis(seance.getClient()), PrestationFacturee.depuis(seance));
        facture.setStatutPaiement(statut);
        if (statut == PAYEE) {
            facture.setDatePaiement(dateEmission.plusDays(10));
        }
        factureRepository.save(facture);
    }

    // Les factures sont créées dans l'ordre chronologique, la séquence suit donc l'ordre d'émission
    private String numeroFacture(LocalDate dateEmission) {
        int annee = dateEmission.getYear();
        int sequence = compteursFactureParAnnee.merge(annee, 1, Integer::sum);
        return "FAC-%d-%04d".formatted(annee, sequence);
    }
}
