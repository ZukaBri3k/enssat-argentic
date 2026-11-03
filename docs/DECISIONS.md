# Décisions de conception

Complément aux diagrammes de `docs/` (`diagramme_UC.pdf`, `diagramme_classe.pdf`, `diagramme_db.pdf`).
Quand ce document et les diagrammes divergent, **ce document fait foi**.

## Architecture

- **Backend** : Spring Boot (jar autonome), API REST, MySQL. Déployé séparément du front.
- **Frontend** : React + Vite (app Node déployée à part). En dev, l'URL du back est configurable ; le back autorise l'origine du front via **CORS** (property).
- **Code backend rangé par couche** : `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `config/`, `security/`.
- **DTO** en `record` Java ; les entités JPA ne sont jamais exposées directement par l'API. Mapping manuel.
- **Erreurs** : `@RestControllerAdvice` qui renvoie des `ProblemDetail` (RFC 9457). Codes : `400` validation, `401`/`403` auth, `404` ressource absente, `409` règle métier violée.
- **Documentation API** : springdoc-openapi (Swagger UI).
- **Tests** : tests unitaires de la couche service (JUnit + Mockito) pour chaque règle métier.

## Base de données

- Schéma généré par Hibernate (`ddl-auto=update`). Pas de Flyway.
  Pour le livrable « scripts de la BDD », le DDL peut être exporté via `jakarta.persistence.schema-generation.scripts.*`.
- **Seed** : `DevDataSeeder` (`CommandLineRunner`, `@Profile("dev")`), exécuté **uniquement si la base est vide**, via les repositories, avec des dates relatives à `LocalDate.now()`. Mots de passe hashés avec le `PasswordEncoder`.
- MySQL local via `backend/compose.yml`.

### Écarts par rapport à `diagramme_db.pdf`

- Nouvelle table `utilisateur` (id, email UNIQUE, mot_de_passe bcrypt, role).
- `seance.type_seance VARCHAR` est remplacé par `seance.specialite_id` (FK vers `specialite`).
- `photographe.email` est UNIQUE. `client.email` n'est **pas** unique.

## Authentification

- **Spring Security + JWT** : `POST /auth/login` renvoie un token, puis `Authorization: Bearer <token>`. Stateless (`spring-boot-starter-oauth2-resource-server`, clé HMAC en property).
- Rôles :
  - `SECRETAIRE` : CRUD photographes / clients / séances, statuts, facturation.
  - `GESTIONNAIRE` : tout ce que fait la secrétaire **+ statistiques**.
- Pas d'écran de gestion des utilisateurs : les comptes viennent du seed.

## Photographes

- Au moins une spécialité. Les spécialités sont une liste fixe (Reportage, Mariage et fêtes, Famille), seedée, exposée en lecture seule (`GET /specialites`).
- Retirer une spécialité à un photographe qui a des séances **à venir non annulées** de ce type → `409`.
- Un photographe inactif ne peut être assigné à une séance (création **et** modification).
- **Désactivation** :
  - `GET /photographes/{id}/seances-a-venir` : le front l'appelle et affiche une confirmation s'il y a des séances.
  - `POST /photographes/{id}/desactivation` : `actif=false` + annulation de toutes les séances PLANIFIEE/CONFIRMEE à venir, **dans une même transaction**.
- **Suppression** : refusée (`409`) si le photographe a des séances (proposer la désactivation). Sinon suppression physique.

## Clients

- Héritage `SINGLE_TABLE` avec discriminant `type_client` (`PARTICULIER` / `ENTREPRISE`).
- Le type d'un client **n'est pas modifiable** après création.
- **Suppression** : refusée (`409`) si le client a des séances.

## Séances

- Type de séance = une **spécialité** ; le photographe doit posséder cette spécialité.
- Création et modification : début (date + heure) ≥ maintenant.
- Une séance **ne peut pas dépasser minuit** (début + durée ≤ 24:00 le même jour).
- Pas de chevauchement pour un même photographe ; les séances **ANNULEE sont ignorées** dans ce contrôle.
- Une séance PLANIFIEE ou CONFIRMEE est modifiable sur tous ses champs (photographe et client compris). TERMINEE et ANNULEE ne sont plus modifiables.
- Transitions de statut :
  - PLANIFIEE → CONFIRMEE | ANNULEE
  - CONFIRMEE → TERMINEE | ANNULEE
  - TERMINEE et ANNULEE sont finaux.
  - TERMINEE seulement si la **fin** (début + durée) est passée.
- **Suppression** : refusée (`409`) si la séance a une facture (l'annuler à la place).

## Facturation

- Génération uniquement pour une séance TERMINEE, au plus une facture par séance (`seance_id UNIQUE`).
- **Montant HT saisi** par la secrétaire à la génération ; taux de TVA par défaut **20,00 %**, modifiable.
- `montant_ttc = montant_ht × (1 + taux_tva / 100)`, non stocké.
- **Numéro** : `FAC-{année d'émission}-{séquence sur 4 chiffres}`, séquence remise à 1 chaque année ; calcul `max + 1` de l'année dans la transaction de génération, contrainte UNIQUE en garde-fou.
- Les coordonnées client, le détail de la prestation et le nom du photographe sont **copiés** dans la facture à la génération (figés). `prestation_type` contient le libellé de la spécialité.
- Statut de paiement : EN_ATTENTE → PAYEE (avec `date_paiement`) | ANNULEE.
- Une facture **ANNULEE est définitive** : la séance ne peut pas être refacturée (choix assumé, 1-1 strict).
- **PDF généré côté backend** (OpenPDF) : `GET /factures/{id}/pdf` → `application/pdf`.

## Statistiques (rôle GESTIONNAIRE)

- « Séance réalisée » = séance **TERMINEE**.
- Top 5 des photographes par nombre de séances réalisées.
- Top 5 des photographes par durée cumulée des séances réalisées.
- Chiffre d'affaires sur une période : somme des factures **PAYEE** dont la **`date_paiement`** est dans la période ; on renvoie le **HT** (le CA) et le TTC.
