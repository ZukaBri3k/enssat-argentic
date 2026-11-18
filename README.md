# enssat-argentic

Application de gestion du studio de photographie **Argentic** (projet JEE, ENSSAT 2ᵉ année) : photographes, clients, planning des séances, facturation et statistiques.

Version finale : [**v1.0.1**](https://github.com/ZukaBri3k/enssat-argentic/releases/tag/v1.0.1).

## Fonctionnalités

| Domaine | Ce qu'on peut faire |
|---|---|
| **Photographes** | Lister, créer (avec compte de connexion optionnel), modifier, supprimer ; désactiver avec annulation des séances à venir ; spécialités (Reportage, Mariage et fêtes, Famille). |
| **Clients** | Particuliers et entreprises : lister, créer, modifier, supprimer. |
| **Séances** | Planning filtrable (période, photographe, client, statut) ; création et modification avec contrôle des chevauchements, de la spécialité et du photographe actif ; cycle PLANIFIEE → CONFIRMEE → TERMINEE / ANNULEE. |
| **Factures** | Génération pour une séance terminée (numéro `FAC-AAAA-NNNN`, HT + TVA), paiement ou annulation, PDF visualisable et téléchargeable. |
| **Statistiques** | Top 5 des photographes (nombre de séances, durée cumulée) et chiffre d'affaires sur une période. |

Les règles métier détaillées sont dans [`docs/DECISIONS.md`](docs/DECISIONS.md).

### Rôles

| Rôle | Accès |
|---|---|
| `SECRETAIRE` | Photographes, clients, séances, factures. |
| `GESTIONNAIRE` | Tout ce que fait la secrétaire + statistiques. |
| `PHOTOGRAPHE` | Consultation de son propre planning de séances (lecture seule). |

## Stack technique

- **Backend** (`backend/`) : Java 25, Spring Boot 4, Spring Data JPA (Hibernate), Spring Security + JWT, MySQL 8.4, springdoc-openapi (Swagger UI), OpenPDF, tests JUnit + Mockito.
- **Frontend** (`frontend/`) : React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui.

Le code backend est rangé par couche : `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/`, `config/`, `security/`.

## Lancer le projet en local

Prérequis : JDK 25, Docker, Node.js 22 et pnpm.

### Backend

```bash
cd backend
docker compose up -d       # MySQL sur localhost:3306
./mvnw spring-boot:run     # API sur localhost:8080 (profil dev par défaut)
```

Dans IntelliJ : lancer `ArgenticApplication` (le profil `dev` est actif par défaut via `application.properties`).

Documentation de l'API : <http://localhost:8080/swagger-ui.html>. Se connecter via `POST /auth/login`, puis renseigner le token dans *Authorize*.

### Frontend

```bash
cd frontend
pnpm install
pnpm dev                   # http://localhost:5173
```

L'URL de l'API est lue dans `VITE_API_URL` (`.env`, par défaut `http://localhost:8080`). Côté backend, l'origine du front autorisée par CORS se règle avec `app.cors.allowed-origins`.

### Tests

```bash
cd backend
./mvnw test
```

## Données de dev

Au démarrage, une base vide est peuplée (`DevDataSeeder`) ; les dates sont relatives au jour du lancement.
Pour repartir de zéro : `docker compose down -v` puis relancer.

| Compte | Mot de passe | Rôle |
|---|---|---|
| `secretaire@argentic.fr` | `secretaire` | `SECRETAIRE` |
| `gestionnaire@argentic.fr` | `gestionnaire` | `GESTIONNAIRE` |
| `<prenom>.<nom>@argentic.fr` (ex. `lea.moreau@argentic.fr`) | `photographe` | `PHOTOGRAPHE` |

Cas prévus pour tester :

- **Désactivation** : Hugo Durand a deux séances à venir ; Théo Laurent est déjà inactif.
- **Chevauchement** : Léa Moreau a deux séances dans 7 jours (10h-12h et 14h-16h).
- **Passage à TERMINEE** : une séance CONFIRMEE d'Hugo Durand il y a 5 jours.
- **Génération de facture** : trois séances TERMINEE sans facture (il y a 7, 10 et 15 jours).
- **Factures** : PAYEE, EN_ATTENTE et une ANNULEE.
- **Statistiques** : le classement par nombre de séances (Léa en tête) diffère du classement par durée (Lucas en tête).

## Base de données

Le schéma est généré par Hibernate à partir des entités. Les scripts livrés sont dans `docs/` :

- [`script_creation_bdd.sql`](docs/script_creation_bdd.sql) : création du schéma. Pour le régénérer (sans MySQL) : `./mvnw spring-boot:run -Dspring-boot.run.profiles=schema` depuis `backend/`.
- [`script_peuplement_bdd.sql`](docs/script_peuplement_bdd.sql) : même jeu de données que le seeder, à exécuter sur une base vide après le script de création :

```bash
mysql -u root -p photo_studio < docs/script_creation_bdd.sql
mysql -u root -p photo_studio < docs/script_peuplement_bdd.sql
```

## Déploiement

### Backend

Chaque release GitHub publiée déclenche la CI (`.github/workflows/release-backend-jar.yml`), qui construit le jar et l'attache à la release (`argentic-backend-<tag>.jar`). Pour le lancer :

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<hote>:3306/photo_studio \
SPRING_DATASOURCE_USERNAME=<utilisateur> \
SPRING_DATASOURCE_PASSWORD=<mot_de_passe> \
SPRING_PROFILES_ACTIVE=prod \
java -jar argentic-backend-v1.0.1.jar
```

Comme en local, une base vide est peuplée au premier démarrage avec les [données de dev](#données-de-dev). En production, surcharger aussi `APP_JWT_SECRET` et `APP_CORS_ALLOWED_ORIGINS`.

Une image Docker est également disponible : `backend/Dockerfile` (port 8080).

### Frontend

`frontend/Dockerfile` compile l'application et la sert avec Nginx (port 80). L'URL de l'API se passe au build :

```bash
docker build --build-arg VITE_API_URL=https://<api> -t argentic-front frontend
```

## Documentation

- [`docs/sujet.pdf`](docs/sujet.pdf) : sujet du projet.
- [`docs/diagramme_UC.pdf`](docs/diagramme_UC.pdf), [`docs/diagramme_classe.pdf`](docs/diagramme_classe.pdf), [`docs/diagramme_db.pdf`](docs/diagramme_db.pdf) : conception.
- [`docs/DECISIONS.md`](docs/DECISIONS.md) : décisions de conception et écarts par rapport aux diagrammes (fait foi en cas de divergence).
