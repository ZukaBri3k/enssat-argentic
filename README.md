# enssat-argentic

Gestion d'un studio de photographie. Sujet et conception dans [`docs/`](docs), décisions dans [`docs/DECISIONS.md`](docs/DECISIONS.md).

## Lancer le backend en local

Prérequis : JDK 25, Docker.

```bash
cd backend
docker compose up -d                                  # MySQL sur localhost:3306
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev # API sur localhost:8080
```

Dans IntelliJ : renseigner `dev` dans *Active profiles* de la configuration de lancement `ArgenticApplication`.

### Données de dev

Avec le profil `dev`, une base vide est peuplée au démarrage (`DevDataSeeder`) ; les dates sont relatives au jour du lancement.
Pour repartir de zéro : `docker compose down -v` puis relancer.

| Compte | Mot de passe | Rôle |
|---|---|---|
| `secretaire@argentic.fr` | `secretaire` | `SECRETAIRE` |
| `gestionnaire@argentic.fr` | `gestionnaire` | `GESTIONNAIRE` |

Cas prévus pour tester :

- **Désactivation** : Hugo Durand a deux séances à venir ; Théo Laurent est déjà inactif.
- **Chevauchement** : Léa Moreau a deux séances dans 7 jours (10h-12h et 14h-16h).
- **Passage à TERMINEE** : une séance CONFIRMEE d'Hugo Durand il y a 5 jours.
- **Génération de facture** : trois séances TERMINEE sans facture (il y a 7, 10 et 15 jours).
- **Factures** : PAYEE, EN_ATTENTE et une ANNULEE.
- **Statistiques** : le classement par nombre de séances (Léa en tête) diffère du classement par durée (Lucas en tête).
