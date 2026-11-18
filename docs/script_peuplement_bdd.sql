-- Peuplement de la BDD avec le jeu de données fictif de développement.
-- Reprend les données de backend/src/main/java/fr/goat/argentic/config/DevDataSeeder.java.
--
-- À exécuter sur une base vide, après script_creation_bdd.sql :
--   mysql -u root -p photo_studio < docs/script_peuplement_bdd.sql
--
-- Comme le seeder, toutes les dates sont relatives au jour d'exécution :
-- le jeu reste cohérent (séances passées / à venir) quel que soit le jour.
--
-- Comptes de connexion :
--   secretaire@argentic.fr   / secretaire
--   gestionnaire@argentic.fr / gestionnaire

SET NAMES utf8mb4;

START TRANSACTION;

-- Mots de passe encodés en BCrypt (BCryptPasswordEncoder, comme l'application)
INSERT INTO utilisateur (id, email, mot_de_passe, role) VALUES
    (1, 'secretaire@argentic.fr', '$2a$10$r6UEB/i7RYENq4gpESKXi.qXMn3VAwYpojXFfw3r3FaxifwYHxRG2', 'SECRETAIRE'),
    (2, 'gestionnaire@argentic.fr', '$2a$10$p3QQ1RH1AJYYvQnK6.lqD.A.xIHSVkLNibXDw41RRhQ4Hssnh2zAa', 'GESTIONNAIRE');

INSERT INTO specialite (id, libelle) VALUES
    (1, 'Reportage'),
    (2, 'Mariage et fêtes'),
    (3, 'Famille');

-- Théo Laurent est désactivé
INSERT INTO photographe (id, nom, email, actif) VALUES
    (1, 'Camille Martin', 'camille.martin@argentic.fr', 1),
    (2, 'Lucas Bernard', 'lucas.bernard@argentic.fr', 1),
    (3, 'Inès Petit', 'ines.petit@argentic.fr', 1),
    (4, 'Hugo Durand', 'hugo.durand@argentic.fr', 1),
    (5, 'Léa Moreau', 'lea.moreau@argentic.fr', 1),
    (6, 'Théo Laurent', 'theo.laurent@argentic.fr', 0);

INSERT INTO photographe_specialite (photographe_id, specialite_id) VALUES
    (1, 1), (1, 3),
    (2, 2),
    (3, 2), (3, 3),
    (4, 1),
    (5, 1), (5, 2), (5, 3),
    (6, 3);

INSERT INTO client (id, type_client, email, adresse_postale, nom, prenom, raison_sociale, numero_siren_tva) VALUES
    (1, 'PARTICULIER', 'marie.dupont@mail.fr', '12 rue de Kerampont, 22300 Lannion', 'Dupont', 'Marie', NULL, NULL),
    (2, 'PARTICULIER', 'thomas.lefebvre@mail.fr', '4 place du Martray, 22220 Tréguier', 'Lefebvre', 'Thomas', NULL, NULL),
    (3, 'PARTICULIER', 'sofia.garcia@mail.fr', '27 boulevard Thiers, 22700 Perros-Guirec', 'Garcia', 'Sofia', NULL, NULL),
    (4, 'PARTICULIER', 'antoine.roux@mail.fr', '3 rue Jean Savidan, 22300 Lannion', 'Roux', 'Antoine', NULL, NULL),
    (5, 'ENTREPRISE', 'contact@atelier-breizh.fr', '15 rue de la Poterie, 22300 Lannion', NULL, NULL, 'Atelier Breizh SARL', 'FR40123456789'),
    (6, 'ENTREPRISE', 'com@lannion-tech.fr', '6 rue de Broglie, 22300 Lannion', NULL, NULL, 'Lannion Tech SAS', 'FR87987654321'),
    (7, 'ENTREPRISE', 'evenements@jardins-tregor.fr', 'Route de Pleumeur, 22560 Trébeurden', NULL, NULL, 'Les Jardins du Trégor', 'FR55456789123');

-- Photographes : 1 Camille, 2 Lucas, 3 Inès, 4 Hugo, 5 Léa, 6 Théo
-- Clients : 1 Dupont, 2 Lefebvre, 3 Garcia, 4 Roux, 5 Atelier Breizh, 6 Lannion Tech, 7 Jardins du Trégor
-- Spécialités : 1 Reportage, 2 Mariage et fêtes, 3 Famille
INSERT INTO seance (id, photographe_id, client_id, specialite_id, date_seance, heure_debut, duree_minutes, lieu, statut) VALUES
    -- Séances passées terminées (facturées plus bas). Classement attendu des stats :
    -- - nombre : Léa 5, Camille 4, Lucas 3, Inès 3, Hugo 2, Théo 1
    -- - durée  : Lucas 1440 min, Léa 810, Camille 630, Inès 570, Hugo 420, Théo 60
    (1, 5, 1, 3, CURDATE() - INTERVAL 120 DAY, '10:00', 120, 'Studio Argentic, Lannion', 'TERMINEE'),
    (2, 2, 2, 2, CURDATE() - INTERVAL 105 DAY, '14:00', 480, 'Château de Tonquédec', 'TERMINEE'),
    (3, 1, 5, 1, CURDATE() - INTERVAL 100 DAY, '09:00', 180, 'Atelier Breizh, Lannion', 'TERMINEE'),
    (4, 3, 3, 3, CURDATE() - INTERVAL 90 DAY, '11:00', 90, 'Plage de Trestraou, Perros-Guirec', 'TERMINEE'),
    (5, 5, 6, 1, CURDATE() - INTERVAL 80 DAY, '15:00', 240, 'Lannion Tech, Lannion', 'TERMINEE'),
    (6, 2, 4, 2, CURDATE() - INTERVAL 75 DAY, '13:00', 480, 'Mairie de Perros-Guirec', 'TERMINEE'),
    (7, 4, 7, 1, CURDATE() - INTERVAL 70 DAY, '10:00', 120, 'Les Jardins du Trégor, Trébeurden', 'TERMINEE'),
    (8, 6, 1, 3, CURDATE() - INTERVAL 60 DAY, '09:30', 60, 'Studio Argentic, Lannion', 'TERMINEE'),
    (9, 1, 2, 3, CURDATE() - INTERVAL 55 DAY, '14:00', 120, 'Parc de Sainte-Anne, Lannion', 'TERMINEE'),
    (10, 3, 3, 2, CURDATE() - INTERVAL 45 DAY, '10:00', 360, 'Manoir de Kerhuel, Perros-Guirec', 'TERMINEE'),
    (11, 5, 4, 3, CURDATE() - INTERVAL 40 DAY, '16:00', 90, 'Studio Argentic, Lannion', 'TERMINEE'),
    (12, 4, 5, 1, CURDATE() - INTERVAL 35 DAY, '08:00', 300, 'Atelier Breizh, Lannion', 'TERMINEE'),
    (13, 2, 3, 2, CURDATE() - INTERVAL 30 DAY, '12:00', 480, 'Abbaye de Bégard', 'TERMINEE'),
    (14, 1, 6, 1, CURDATE() - INTERVAL 25 DAY, '10:00', 180, 'Lannion Tech, Lannion', 'TERMINEE'),
    (15, 5, 2, 2, CURDATE() - INTERVAL 20 DAY, '14:00', 240, 'Salle des fêtes de Trégastel', 'TERMINEE'),

    -- Terminées sans facture : pour tester « Générer la facture »
    (16, 3, 1, 3, CURDATE() - INTERVAL 15 DAY, '09:00', 120, 'Studio Argentic, Lannion', 'TERMINEE'),
    (17, 1, 7, 1, CURDATE() - INTERVAL 10 DAY, '10:00', 150, 'Les Jardins du Trégor, Trébeurden', 'TERMINEE'),
    (18, 5, 5, 1, CURDATE() - INTERVAL 7 DAY, '11:00', 120, 'Atelier Breizh, Lannion', 'TERMINEE'),

    -- Confirmée dont la fin est passée : pour tester le passage à TERMINEE
    (19, 4, 6, 1, CURDATE() - INTERVAL 5 DAY, '15:00', 60, 'Lannion Tech, Lannion', 'CONFIRMEE'),

    (20, 2, 1, 2, CURDATE() - INTERVAL 50 DAY, '10:00', 120, 'Mairie de Lannion', 'ANNULEE'),

    -- À venir. Hugo en a deux : pour tester la désactivation.
    -- Léa a deux séances le même jour (10h-12h, 14h-16h) : pour tester le chevauchement.
    (21, 1, 3, 3, CURDATE() + INTERVAL 2 DAY, '10:00', 120, 'Plage de Beg Léguer, Lannion', 'CONFIRMEE'),
    (22, 2, 2, 2, CURDATE() + INTERVAL 3 DAY, '14:00', 480, 'Château de Rosanbo', 'CONFIRMEE'),
    (23, 1, 5, 1, CURDATE() + INTERVAL 4 DAY, '16:00', 60, 'Atelier Breizh, Lannion', 'ANNULEE'),
    (24, 4, 7, 1, CURDATE() + INTERVAL 5 DAY, '09:00', 180, 'Les Jardins du Trégor, Trébeurden', 'PLANIFIEE'),
    (25, 4, 1, 1, CURDATE() + INTERVAL 6 DAY, '11:00', 90, 'Studio Argentic, Lannion', 'CONFIRMEE'),
    (26, 5, 6, 1, CURDATE() + INTERVAL 7 DAY, '10:00', 120, 'Lannion Tech, Lannion', 'PLANIFIEE'),
    (27, 5, 4, 3, CURDATE() + INTERVAL 7 DAY, '14:00', 120, 'Studio Argentic, Lannion', 'PLANIFIEE'),
    (28, 3, 3, 2, CURDATE() + INTERVAL 10 DAY, '15:00', 240, 'Manoir de Kerhuel, Perros-Guirec', 'PLANIFIEE'),
    (29, 2, 4, 2, CURDATE() + INTERVAL 14 DAY, '10:00', 360, 'Abbaye de Beauport, Paimpol', 'PLANIFIEE'),
    (30, 3, 2, 3, CURDATE() + INTERVAL 21 DAY, '09:30', 120, 'Studio Argentic, Lannion', 'PLANIFIEE');

-- Factures des séances 1 à 15, émises le lendemain de la séance et payées 10 jours après l'émission si PAYEE.
-- Coordonnées du client et détail de la prestation recopiés depuis la séance (CoordonneesFacturation / PrestationFacturee).
-- Numéro FAC-<année>-<séquence>, séquence par année d'émission dans l'ordre chronologique.
INSERT INTO facture (seance_id, numero, date_emission, date_paiement, montant_ht, taux_tva, statut_paiement,
                     client_identite, client_adresse_postale, client_email, client_numero_siren_tva,
                     prestation_date, prestation_heure_debut, prestation_duree_minutes, prestation_type, photographe_nom)
SELECT s.id,
       CONCAT('FAC-', YEAR(s.date_seance + INTERVAL 1 DAY), '-',
              LPAD(ROW_NUMBER() OVER (PARTITION BY YEAR(s.date_seance + INTERVAL 1 DAY) ORDER BY s.date_seance, s.id), 4, '0')),
       s.date_seance + INTERVAL 1 DAY,
       IF(f.statut = 'PAYEE', s.date_seance + INTERVAL 11 DAY, NULL),
       f.montant_ht,
       20.00,
       f.statut,
       IF(c.type_client = 'PARTICULIER', CONCAT(c.nom, ' ', c.prenom), c.raison_sociale),
       c.adresse_postale,
       c.email,
       c.numero_siren_tva,
       s.date_seance,
       s.heure_debut,
       s.duree_minutes,
       sp.libelle,
       p.nom
FROM (VALUES
         ROW(1, 250.00, 'PAYEE'),
         ROW(2, 1800.00, 'PAYEE'),
         ROW(3, 600.00, 'PAYEE'),
         ROW(4, 220.00, 'PAYEE'),
         ROW(5, 750.00, 'PAYEE'),
         ROW(6, 1900.00, 'PAYEE'),
         ROW(7, 450.00, 'PAYEE'),
         ROW(8, 150.00, 'PAYEE'),
         ROW(9, 260.00, 'PAYEE'),
         ROW(10, 1400.00, 'PAYEE'),
         ROW(11, 200.00, 'PAYEE'),
         ROW(12, 900.00, 'EN_ATTENTE'),
         ROW(13, 2000.00, 'EN_ATTENTE'),
         ROW(14, 550.00, 'ANNULEE'),
         ROW(15, 1100.00, 'PAYEE')
     ) AS f (seance_id, montant_ht, statut)
JOIN seance s ON s.id = f.seance_id
JOIN client c ON c.id = s.client_id
JOIN photographe p ON p.id = s.photographe_id
JOIN specialite sp ON sp.id = s.specialite_id
ORDER BY s.date_seance, s.id;

COMMIT;
