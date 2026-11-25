# delibera-core-api

API REST de **Delibera**, la plateforme de délibération des étudiants de Saint Jean Ingénieur (SJI) et Saint Jean School of Management (SJM), projet tutoré de la Branche ISI.

Chaque école est un client autonome du service (SaaS) : ses données, ses utilisateurs, ses règles de passage et ses paramètres sont isolés de ceux des autres écoles. La plateforme ne se connecte pas au système d'information des écoles ; les données entrent par import de fichiers Excel ou CSV.

La conception de référence est le **cahier de conception**, dans sa version en ligne la plus récente (fin novembre 2026) : architecture, modules, classes, algorithmes A1 à A8. Il s'appuie sur le cahier d'analyse. Chaque module en reprend les spécifications dans son fichier `SPEC.md`.

## Technologies

| Couche | Choix |
| --- | --- |
| Langage et cadre | Java 21, Spring Boot 4.1 (Spring MVC, Validation, Data JPA, Security, Mail, Actuator) |
| Persistance | PostgreSQL 16, Hibernate 7, Flyway |
| Sécurité | Jetons JWT (HS256), BCrypt, contrôle d'accès par `@PreAuthorize` |
| Documentation de l'API | springdoc-openapi (OpenAPI 3, Swagger UI) |
| Conversion entités ↔ DTO | MapStruct |
| Tests | JUnit 6 (Jupiter), Mockito, Testcontainers, couverture JaCoCo |
| Construction et livraison | Maven (wrapper inclus), Docker, GitHub Actions |

## Prérequis

- **JDK 21** (Temurin conseillé).
- **Docker Desktop** : base de développement (`compose.yaml`) et tests d'intégration (Testcontainers).
- Maven n'est pas nécessaire : utilisez `mvnw` (Linux, macOS) ou `mvnw.cmd` (Windows).

## Démarrer en local

1. Lancer PostgreSQL et Mailpit :

   ```bash
   docker compose up -d
   ```

2. Lancer l'API avec le profil `dev` :

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev        # Linux, macOS
   mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev      # Windows
   ```

   Dans IntelliJ IDEA, lancer `DeliberaApplication` avec le profil actif `dev`.

   **Variante sans `compose.yaml`** : lancer `TestDeliberaApplication` (dans `src/test/java`), qui démarre une base PostgreSQL jetable avec Testcontainers.

3. Adresses utiles :

   | Service | Adresse |
   | --- | --- |
   | API | http://localhost:8080/api/v1 |
   | Swagger UI | http://localhost:8080/swagger-ui.html |
   | État de santé | http://localhost:8080/actuator/health |
   | Courriels reçus (Mailpit) | http://localhost:8025 |

## Tests

```bash
./mvnw verify
```

Les tests d'intégration démarrent PostgreSQL dans Docker. Le rapport de couverture est produit dans `target/site/jacoco/index.html`. Le pipeline (`.github/workflows/ci-cd.yml`) lance la même commande à chaque push sur `main` ou `develop` et à chaque demande de fusion vers ces branches.

## Organisation du code

Un paquetage par module du cahier de conception (tableau 5) :

| Paquetage | Module | Cas d'utilisation |
| --- | --- | --- |
| `com.delibera.securite` | M1 Sécurité et accès | CU00, CU12, CU16, CU18 |
| `com.delibera.ecole` | M2 Écoles clientes | CU15 |
| `com.delibera.referentiel` | M3 Référentiel et maquettes | CU01 |
| `com.delibera.regles` | M4 Règles et paramètres | CU02, CU13 |
| `com.delibera.importation` | M5 Import | CU03, CU07 |
| `com.delibera.calcul` | M6 Moteur de calcul | CU04 à CU06 |
| `com.delibera.deliberation` | M7 Délibération et jury | CU04 à CU06, CU08, CU14, CU17 |
| `com.delibera.documents` | M8 Documents et statistiques | CU09, CU10 |
| `com.delibera.audit` | M9 Audit | CU11 |
| `com.delibera.notification` | M10 Notifications | CU12, CU17, CU18 |
| `com.delibera.commun` | Éléments partagés | — |

Dans chaque module :

- `SPEC.md` : spécifications fonctionnelles et techniques du module (cas d'utilisation, modèle, règles, services, algorithmes, routes, tests d'acceptation, questions ouvertes) ;
- `README.md` : état de l'implémentation, créé avec la première fonctionnalité du module ;
- les sous-paquetages `web` (contrôleurs REST et DTO), `service` (transactions et règles métier), `domaine` (entités JPA) et `depot` (dépôts Spring Data).

Éléments déjà en place dans `commun` :

- `EntiteBase` : identifiant, verrouillage optimiste (`verrou`) et dates techniques de toutes les entités.
- `EntiteEcole` : identifiant de l'école, pris dans la session à la création. Un filtre Hibernate, actif en permanence, limite toute lecture à l'école de l'utilisateur connecté, y compris les chargements par identifiant.
- `GestionnaireErreurs` : toutes les erreurs sont renvoyées au format RFC 9457, avec un `code` stable.

## Conventions

Les règles complètes de contribution (branches, documentation de chaque fonctionnalité, patron de développement, tests, interdits) sont dans [AGENT.md](AGENT.md). Elles s'appliquent aux agents comme aux membres de l'équipe. En résumé, d'après le cahier de conception (section 4.2.2) :

- **Couches** : contrôleur REST (DTO) → service (transactions, règles métier) → dépôt. Les entités ne sortent jamais de la couche service ; la conversion passe par MapStruct.
- **Nommage** : Google Java Style (indentation de 2 espaces, 100 colonnes). Les classes métier gardent les noms du modèle d'analyse (`Deliberation`, `ReglePassage`) ; les suffixes techniques sont en anglais (`DeliberationController`, `DeliberationService`, `DeliberationRepository`).
- **Aucune règle dans le code** : seuils, marges, crédits et conditions sont des données en base.
- **Isolation** : l'école vient toujours de la session, jamais d'un paramètre envoyé par le client.
- **API** : ressources au pluriel sous `/api/v1`, codes HTTP standard (201, 204, 400, 401, 403, 404, 409, 422).
- **Calculs** : notes en `BigDecimal` à deux décimales, arrondi `HALF_UP` une seule fois ; dates en UTC.
- **Schéma** : uniquement par migrations Flyway (`src/main/resources/db/migration`, `V<n>__<description>.sql`) ; Hibernate se contente de vérifier. Chaque table a sa séquence d'identifiants, `<table>_seq` avec un pas de 50, pour permettre l'enregistrement par lots.
- **Git** : branches `main`, `develop` et `feature/*` ; messages au format Conventional Commits (`feat: …`, `fix: …`) ; revue obligatoire avant fusion.
- **Secrets** : uniquement dans des variables d'environnement, jamais dans le dépôt. Aucune note ni donnée d'identité dans les journaux.

## Déploiement continu

**Chaque push sur `main` déploie l'API en production**, sur l'instance EC2 gérée par Terraform (dépôt `PROJET-TUTORE-ISI-5/infra`).

| Étape | Où | Détail |
| --- | --- | --- |
| 1. Compilation et tests | GitHub Actions | `./mvnw verify` (tests unitaires et d'intégration). Le JAR n'est produit que si tout passe. |
| 2. Envoi | GitHub Actions → EC2 (SSH) | Le JAR testé et `deploy/remote-deploy.sh` sont copiés sur l'instance. |
| 3. Sauvegarde | EC2 | `pg_dump` de la base avant la nouvelle version ; les 10 dernières sont gardées dans `/var/backups/delibera-api`. |
| 4. Activation | EC2 | Nouvelle version dans `/opt/delibera-api/releases/<commit>`, lien `current` basculé, service `delibera-api` redémarré. Flyway applique les migrations au démarrage. |
| 5. Contrôle de santé | EC2 | `/actuator/health` doit répondre `UP` en moins de 3 minutes, **sinon retour automatique à la version précédente**. |
| 6. Vérification | GitHub Actions → Internet | `http://<EC2_HOST>/api/v1/…` doit répondre 401 (Nginx, API et Spring Security en place). |

- **Retour arrière manuel** : Actions > CI/CD > *Run workflow* sur `main`, action `revenir`. La version qui tournait avant l'actuelle est réactivée, sans recompiler.
- **Redéploiement** : même écran, action `deployer`.
- **Sur l'instance** : `sudo journalctl -u delibera-api -f` (journal), `sudo systemctl status delibera-api`.
- **Configuration de production** : `/etc/delibera-api/delibera-api.env`, créé par Terraform. Les secrets (base, JWT) sont générés sur l'instance et n'en sortent pas.
- **Migrations** : une migration doit rester compatible avec la version précédente de l'API, car le retour arrière ne défait pas le schéma (AGENT.md, section 7). En dernier recours, restaurer la sauvegarde d'avant déploiement avec `pg_restore`.
- **GitHub** : variable `EC2_HOST` et secret `EC2_SSH_KEY`, les mêmes que pour `delibera-ui` (au niveau de l'organisation ou du dépôt). L'environnement `production` peut exiger une approbation avant chaque déploiement (Settings > Environments).

## Configuration (profil `prod`)

Voir `.env.example`. Variables principales : `DELIBERA_DB_URL`, `DELIBERA_DB_USER`, `DELIBERA_DB_PASSWORD`, `DELIBERA_JWT_SECRET` (au moins 32 caractères), `DELIBERA_CORS_ORIGINES`, `DELIBERA_SMTP_HOST`, `DELIBERA_SMTP_PORT`, `DELIBERA_SMTP_USER`, `DELIBERA_SMTP_PASSWORD`.

## Feuille de route

Quatre itérations de deux semaines (cahier de conception, section 4.1) :

1. **Socle** : sécurité, écoles clientes, référentiel et import (CU00, CU01, CU03, CU12, CU15, CU16, CU18).
2. **Calcul** : règles, paramètres, import des rattrapages et pré-délibération, validés par les tests T01 à T10 (CU02, CU04, CU07, CU13).
3. **Séance du jury** : composition du jury, cas critiques et décisions (CU05, CU06, CU17).
4. **Clôture et suivi** : validation, PV, listes, statistiques, audit, réclamations (CU08 à CU11, CU14).
