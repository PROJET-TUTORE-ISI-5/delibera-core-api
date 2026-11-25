# AGENT.md – Règles pour les agents qui participent au développement

Ce fichier s'adresse à tout agent (assistant IA de code, agent autonome) qui modifie `delibera-core-api`. Il vaut aussi pour les membres de l'équipe. **Lisez-le en entier avant toute modification**, puis relisez la section 11 au début de chaque nouvelle fonctionnalité.

En cas de conflit, l'ordre de priorité est le suivant :
1. une consigne explicite d'un membre de l'équipe dans la conversation en cours ;
2. ce fichier ;
3. le `SPEC.md` du module concerné ;
4. le cahier de conception, puis le cahier d'analyse (section 1) ;
5. vos habitudes ou vos préférences.

---

## 1. Sources de vérité

| Document | Version | Ce qu'il fixe |
| --- | --- | --- |
| `SPEC.md` de chaque module | Tenu à jour dans le dépôt | Synthèse, module par module, de tout ce qui suit : cas d'utilisation, modèle typé, contraintes, services, algorithmes, routes proposées, tests d'acceptation, questions ouvertes |
| Cahier de conception | Version en ligne la plus récente (fin novembre 2026) | Architecture, modules M1 à M10, classes typées, interfaces de service, algorithmes A1 à A8, standard de programmation (section 4.2.2) |
| Cahier d'analyse | v2.3 | Cas d'utilisation CU00 à CU18, entités, contraintes OCL (§4.2.7), matrice des permissions (§3.5), séquences système |
| Cahier des charges technique | v2 | Besoins BF01 à BF22, besoins non fonctionnels BNF01 à BNF13 |
| Project Scope (Saint Jean) | V2 | Règles de passage, scénarios d'utilisation, **jeux de tests T01 à T10** |

Attention : la copie « v1.2 » du cahier de conception (8 novembre) et le fichier Word correspondant sont **dépassés**. Le seuil limite y est encore une marge de la délibération, alors qu'il appartient désormais à la condition de la règle ; CU07 y est encore rattaché à M6. Les `SPEC.md` suivent la version la plus récente.

Règles :
- **Commencez toujours par le `SPEC.md` du module.** Il cite ses sources (sections des cahiers) ; remontez-y en cas de doute.
- Les éléments marqués **[Proposition]** dans un `SPEC.md` ne viennent pas des cahiers. Faites-les valider par l'équipe **avant** de les implémenter.
- **N'inventez jamais une règle métier.** Si un document ne tranche pas, ou si deux documents se contredisent, arrêtez-vous et posez la question à l'équipe. Notez la question dans la section « Questions ouvertes » du `SPEC.md` du module.
- Les noms des classes, attributs, méthodes et associations viennent du modèle. Ne les renommez pas sans accord.
- Tout écart volontaire par rapport au cahier de conception est consigné dans `docs/decisions/` (section 9.4) et signalé à l'équipe, qui mettra le cahier à jour.

## 2. Règles métier à ne jamais enfreindre

- **Plateforme SaaS** : chaque école (SJI, SJM ou autre) est un client autonome. SJI et SJM n'ont aucune donnée en commun. Il n'y a pas d'« établissement » au-dessus des écoles.
- **Isolation** (BNF12) : l'école vient toujours de la session (jeton JWT), **jamais** d'un paramètre, d'un en-tête ou d'un corps de requête envoyé par le client. Toute entité qui appartient à une école hérite de `EntiteEcole`.
- **Indépendance** (BNF11) : aucune connexion au système d'information de Saint Jean. Les données entrent uniquement par import de fichiers Excel ou CSV (module M5).
- **Aucune règle ni valeur dans le code** (BNF02) : seuils, marges, crédits, note de validation, note éliminatoire, délais… sont des données en base (`ReglePassage`, `ConditionRegle`, `ParametreDeliberation`). Les nombres 45, 42, 10 ou 7 n'apparaissent que dans les tests.
- **Historique** (BNF04) : une note n'est jamais modifiée ni supprimée (`Note.remplacerPar`). Chaque recalcul crée une nouvelle version de la situation. Une délibération clôturée ne change plus, sauf réouverture tracée.
- **Traçabilité** (BNF03) : toute modification de note, de paramètre, de règle ou de décision, tout relèvement, toute connexion et tout refus d'accès sont journalisés (module M9). Le journal ne s'efface pas.
- **Rôle du jury** : la plateforme propose, le jury décide. L'état `SOUS_CONDITION` n'est jamais proposé automatiquement. Les décisions et les relèvements exigent un motif.
- **Calcul** : un EC n'est crédité qu'une fois (T10). La règle appliquée est la plus précise : spécialité, puis filière, puis niveau (A2). Les détails sont dans les algorithmes A3 à A7 : suivez-les à la lettre.
- **Six états de décision** : `ADMIS`, `ADMIS_APRES_RATTRAPAGE`, `SOUS_CONDITION`, `A_DELIBERER`, `AJOURNE`, `BLOQUE_POUR_DETTE`. Pas d'autre état.
- **Données personnelles** (loi n° 2024/017) : aucune donnée réelle d'étudiant dans le dépôt, les tests ou les journaux.

## 3. Architecture et patron de développement

### 3.1. Monolithe modulaire

Une seule application Spring Boot, découpée en dix modules métier, un paquetage par module (cahier de conception, tableau 5) :

| Paquetage | Module | Cas d'utilisation |
| --- | --- | --- |
| `securite` | M1 Sécurité et accès | CU00, CU12, CU16, CU18 |
| `ecole` | M2 Écoles clientes | CU15 |
| `referentiel` | M3 Référentiel et maquettes | CU01 |
| `regles` | M4 Règles et paramètres | CU02, CU13 |
| `importation` | M5 Import | CU03, CU07 |
| `calcul` | M6 Moteur de calcul | CU04 à CU06 |
| `deliberation` | M7 Délibération et jury | CU04 à CU06, CU08, CU14, CU17 |
| `documents` | M8 Documents et statistiques | CU09, CU10 |
| `audit` | M9 Audit | CU11 |
| `notification` | M10 Notifications | CU12, CU17, CU18 |
| `commun` | Éléments partagés | — |

Tous sont sous `com.delibera`.

### 3.2. Couches dans chaque module

```
com.delibera.<module>
├── SPEC.md     spécifications du module (section 9.1)
├── README.md   état de l'implémentation (section 9.2)
├── web         contrôleurs REST, DTO de requête et de réponse, mappers MapStruct
├── service     interfaces de service (publiques) et leurs implémentations
├── domaine     entités JPA, énumérations, objets de valeur
└── depot       dépôts Spring Data
```

- Les appels vont dans un seul sens : `web` → `service` → `depot`. Un contrôleur n'appelle jamais un dépôt.
- **Les entités ne sortent jamais de la couche service.** Les contrôleurs reçoivent et renvoient des DTO (des `record` Java), convertis par MapStruct.
- Les transactions sont dans les services (`@Transactional`, avec `readOnly = true` pour les lectures), jamais dans les contrôleurs.

### 3.3. Dépendances entre modules

Un module n'utilise un autre module que par **ses interfaces de service**. Il n'accède jamais à ses dépôts, à ses entités internes ou à ses contrôleurs.

**Les dépendances autorisées de chaque module sont listées dans la section « Dépendances » de son `SPEC.md`.** Les grandes lignes (cahier de conception, section 5.2) :
- M7 est le seul orchestrateur d'une délibération.
- Seul M5 lit des fichiers, seul M8 produit des documents, seul M10 envoie des messages vers l'extérieur.
- **M6 calcule en mémoire, sans accès à la base : M7 charge les données et enregistre les résultats.**
- Tous les modules journalisent par M9 et peuvent utiliser `commun`.

Le cahier crée quelques dépendances dans les deux sens : M1 ↔ M2, M1 ↔ M7, M7 ↔ M8. Passez par les interfaces de service, n'écrivez pas d'appels en boucle, et reportez-vous aux questions ouvertes des `SPEC.md` concernés. Toute autre dépendance demande l'accord de l'équipe et une décision consignée (section 9.4).

### 3.4. Patrons de conception imposés

- **Supertype de couche** : toute entité hérite de `EntiteBase`, et toute entité rattachée à une école hérite de `EntiteEcole`.
- **Stratégie et Interpréteur** pour le moteur de règles : chaque `VariableRegle` et chaque `OperateurComparaison` porte son propre calcul, et une règle s'évalue comme une composition de conditions. Pas de `switch` géant ni de règle écrite en Java.
- **Objets de valeur** non persistés (`ContexteEvaluation`, `ResultatEvaluation`, `Cible`…) : des `record` immuables.
- **Retours de service qui ne sont pas des entités** (`ResultatConnexion`, `ApercuImport`, `RapportImport`…) : des `record`.

## 4. Conventions de code

### 4.1. Langue et nommage

- Style : **Google Java Style**, avec une indentation de 2 espaces et 100 colonnes au maximum (voir `.editorconfig`).
- **Noms métier en français**, **sans accents** dans les identifiants : `Deliberation`, `ReglePassage`, `lancerPreDeliberation`, `noteEliminatoire`.
- **Suffixes techniques en anglais** :

  | Rôle | Exemple |
  | --- | --- |
  | Contrôleur | `EcoleController` |
  | Interface de service | `EcoleService` |
  | Implémentation de service | `EcoleServiceImpl` |
  | Dépôt | `EcoleRepository` |
  | Mapper | `EcoleMapper` |
  | DTO de requête | `CreerEcoleRequest` |
  | DTO de réponse | `EcoleResponse` |

- Constantes en `MAJUSCULES_SOULIGNEES`. Codes d'erreur et codes de permission dans le même format.
- Commentaires, Javadoc, messages d'erreur et messages de commit : **en français**.
- Javadoc obligatoire sur les interfaces de service et sur toute méthode publique non évidente. Citez la référence quand elle existe : « algorithme A4 », « CU06 », « BNF12 ».

### 4.2. Calculs, dates, identifiants

- Notes, seuils, moyennes et pourcentages : `BigDecimal`, deux décimales, arrondi `RoundingMode.HALF_UP` **une seule fois**, à la fin du calcul de chaque note. `double` et `float` sont interdits pour ces valeurs.
- Dates : `LocalDate` et `LocalDateTime` en UTC. La conversion vers le fuseau de l'école se fait à l'affichage.
- Identifiants : l'`id` technique hérité d'`EntiteBase`. Les codes métier (`code`, `codeUE`, `codeEC`, `matricule`, `numero`) sont uniques **au sein d'une école** et servent de clés aux fichiers d'import.

### 4.3. Erreurs

- Une erreur prévue lève une sous-classe de `ExceptionMetier` (`commun.erreur`) : `RessourceIntrouvableException` (404), `RegleMetierException` (422), `ConflitException` (409) ou `AccesRefuseException` (403).
- Chaque erreur porte un **code stable** (`DELIBERATION_CLOTUREE`, `MOTIF_OBLIGATOIRE`…), documenté dans le README du module. Le client web s'appuie sur ce code, jamais sur le texte du message.
- `GestionnaireErreurs` renvoie toutes les erreurs au format RFC 9457. N'écrivez pas d'autre `@ControllerAdvice`.
- Ne capturez jamais une exception pour l'ignorer. Pas de `catch (Exception e) {}`.

### 4.4. Journaux applicatifs

- SLF4J uniquement (`LoggerFactory.getLogger`). `System.out` et `printStackTrace` sont interdits.
- **Aucune note, aucun nom, matricule, e-mail, mot de passe ou jeton** dans les journaux. Seuls les identifiants techniques sont permis.

## 5. API REST

- Préfixe `/api/v1`, ressources **au pluriel**, en minuscules avec des tirets : `/api/v1/deliberations/{id}/situations`, `/api/v1/regles-passage`.
- Verbes et codes HTTP :

  | Action | Verbe | Code de réussite |
  | --- | --- | --- |
  | Lire | GET | 200 |
  | Créer | POST | 201, avec l'en-tête `Location` |
  | Remplacer | PUT | 200 |
  | Modifier en partie | PATCH | 200 |
  | Supprimer | DELETE | 204 |
  | Action métier | POST sur une sous-ressource verbale (`/deliberations/{id}/cloture`) | 200 |

  Codes d'erreur : 400 (données invalides), 401 (non authentifié), 403 (non autorisé), 404, 409 (conflit, verrou), 422 (règle métier).
- Les DTO de requête sont validés par Bean Validation (`@Valid`, `@NotNull`, `@Size`…), avec des messages en français.
- Les listes longues sont paginées (`Pageable` de Spring Data).
- Les objets modifiables en séance (`Deliberation`, `SituationAcademique`, `DecisionJury`) exposent leur `verrou`, et le client le renvoie à chaque modification. Un écart donne 409.
- **Toute route est documentée dans Swagger (OpenAPI) dans le même commit que son code** : voir la section 5.1.
- Une route publique (sans jeton) doit être ajoutée explicitement dans `SecuriteConfig` et approuvée par l'équipe.

### 5.1. Documentation OpenAPI (Swagger) de chaque route

springdoc génère le contrat de l'API à partir du code : `/v3/api-docs`, affiché dans Swagger UI (`/swagger-ui.html`). Une route apparaît donc toujours dans Swagger. La règle porte sur **la qualité de sa documentation**, qui sert de contrat au client web et aux testeurs.

**Obligatoire pour chaque route :**

1. **Contrôleur** : `@Tag(name = "M<n> – <Nom du module>", description = "…")`. Un seul nom de tag par module, même s'il a plusieurs contrôleurs.
2. **Méthode** : `@Operation` avec :
   - `summary` : verbe à l'infinitif, moins de 80 caractères (« Créer une école cliente ») ;
   - `description` : cas d'utilisation, **permission exigée**, règles métier importantes.
3. **Réponses** : un `@ApiResponse` pour chaque réponse possible.
   - **Réussite** (200, 201 ou 204) : avec le schéma du DTO renvoyé.
   - **Erreurs** : avec le type `application/problem+json` et le schéma `ProblemDetail`.
     - Sur toute route protégée : **401** et **403**.
     - Selon le cas : 400 (validation), 404, 409 (conflit, verrou) et 422 (règle métier).
     - La description cite les **codes d'erreur métier** possibles, par exemple `MOTIF_OBLIGATOIRE`.
4. **Paramètres** : `@Parameter(description = "…", example = "…")` sur chaque paramètre de chemin et de requête. Pagination : `@ParameterObject Pageable`.
5. **DTO** (`record` de requête et de réponse) : `@Schema(description = "…")` sur le `record` et sur chaque champ, avec un `example`. Les contraintes Bean Validation (`@NotBlank`, `@Size`, `@DecimalMin`…) suffisent pour « obligatoire », « minimum » et « maximum » : springdoc les reprend.

**Interdits** :
- une entité JPA dans un schéma ;
- un exemple avec une donnée réelle (nom, matricule, note d'un vrai étudiant) ;
- une route technique visible sans raison : la masquer avec `@Hidden`.

**Exemple :**

```java
@RestController
@RequestMapping("/api/v1/ecoles")
@Tag(name = "M2 – Écoles clientes", description = "Création, suspension et réactivation des écoles (CU15)")
class EcoleController {

  @Operation(
      summary = "Créer une école cliente",
      description = "Crée l'école et son premier administrateur, qui reçoit un lien d'activation. "
          + "CU15. Permission : ECOLE_GERER (administrateur de la plateforme).")
  @ApiResponse(responseCode = "201", description = "École créée",
      content = @Content(schema = @Schema(implementation = EcoleResponse.class)))
  @ApiResponse(responseCode = "400", description = "Données invalides",
      content = @Content(mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(responseCode = "401", description = "Jeton absent, invalide ou expiré",
      content = @Content(mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(responseCode = "403", description = "Permission ECOLE_GERER absente",
      content = @Content(mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(responseCode = "409", description = "CODE_ECOLE_DEJA_UTILISE : code déjà utilisé",
      content = @Content(mediaType = "application/problem+json",
          schema = @Schema(implementation = ProblemDetail.class)))
  @PostMapping
  ResponseEntity<EcoleResponse> creer(@Valid @RequestBody CreerEcoleRequest requete) {
    // ...
  }
}

@Schema(description = "Création d'une école cliente et de son premier administrateur")
record CreerEcoleRequest(
    @Schema(description = "Code unique de l'école", example = "ECO1")
        @NotBlank @Size(max = 20) String code,
    @Schema(description = "Nom de l'école", example = "École d'exemple")
        @NotBlank String nom) {}
```

**Vérification :**
- Ouvrir Swagger UI.
- Cliquer sur **Authorize** et coller un jeton JWT.
- Essayer la route avec un cas de réussite et un cas d'erreur.
- Le test `DocumentationOpenApiTest` parcourt `/v3/api-docs` et **fait échouer la construction** si une route n'a pas de résumé, si son tag n'a pas de description, si sa réponse de réussite n'est pas documentée, ou si une route protégée ne documente pas 401 et 403.

Swagger UI et `/v3/api-docs` sont désactivés en production (`application-prod.yml`).

## 6. Sécurité

- Chaque méthode publique d'une interface de service exposée porte `@PreAuthorize` avec **un code de permission** de la matrice §3.5 du cahier d'analyse :
  - `ECOLE_GERER`, `REFERENTIEL_GERER`, `REGLE_GERER`, `PARAM_GERER` ;
  - `UTILISATEUR_GERER`, `ROLE_GERER`, `JURY_CONSTITUER` ;
  - `DONNEES_IMPORTER`, `PREDELIB_LANCER`, `RATTRAPAGE_SAISIR` ;
  - `DOSSIER_CONSULTER`, `DOSSIER_STATUER`, `DELIB_VALIDER`, `PV_GENERER` ;
  - `RECLAMATION_TRAITER`, `REOUVERTURE_AUTORISER`, `STATS_CONSULTER`, `AUDIT_CONSULTER`.
- Le contrôle du périmètre (école, cycle, niveau) et de l'appartenance au jury passe par `ControleAccesService.verifier` (algorithme A8). Un refus donne 403 et il est journalisé.
- Mots de passe : empreinte BCrypt uniquement. Jetons d'activation et de réinitialisation : empreinte SHA-256 uniquement. Secret TOTP : chiffré en AES-256-GCM.
- Requêtes : JPQL ou méthodes dérivées de Spring Data, avec paramètres. Jamais de requête construite par concaténation de chaînes.
- Secrets : uniquement dans des variables d'environnement (`.env.example` documente les noms). Ne committez jamais de `.env`, de clé ou de mot de passe réel.
- L'administrateur de la plateforme n'accède à aucune donnée académique (BNF13).

## 7. Persistance et migrations

- Le schéma est géré **uniquement par Flyway** (`src/main/resources/db/migration`). Hibernate ne fait que vérifier (`ddl-auto: validate`). Ne passez jamais `ddl-auto` à `update` ou `create`.
- Nommage des migrations : `V<n>__<description_en_minuscules>.sql`, par exemple `V3__creer_tables_referentiel.sql`. Avant d'en créer une, regardez le dernier numéro sur `develop`. En cas de collision avec une autre branche, renumérotez **avant** la fusion.
- **Une migration déjà fusionnée dans `develop` ne se modifie jamais** : écrivez une nouvelle migration.
- **Une migration reste compatible avec la version précédente de l'API.** En cas d'échec d'un déploiement, le pipeline réactive automatiquement l'ancienne version, mais il ne défait pas le schéma. Pour renommer ou supprimer une colonne ou une table, procédez en deux livraisons :
  1. ajouter le nouvel élément, et faire fonctionner le code avec les deux ;
  2. supprimer l'ancien élément dans une livraison suivante.
- Pour chaque table :
  - nom en `snake_case` au singulier (`annee_academique`) ;
  - colonnes `id`, `verrou`, `cree_le` et `modifie_le` ;
  - une séquence `<table>_seq` avec `increment by 50` (enregistrement par lots, A1).
- Pour chaque table d'une école :
  - une colonne `ecole_id NOT NULL`, avec une clé étrangère vers `ecole` et un index ;
  - les codes métier uniques par école : `unique (ecole_id, code)`.
- Les contraintes OCL du cahier d'analyse (§4.2.7) deviennent des contraintes de base quand c'est possible (clés uniques, clés étrangères, `check`). Sinon, ce sont des contrôles dans le service, et chacun a son test.
- Notes : `numeric(5,2)`. Pas de suppression en cascade des données académiques.
- Performances (BNF06) : évitez les requêtes N+1 (jointures `fetch`, `@EntityGraph`, chargement groupé comme en A1). La pré-délibération de 500 étudiants doit tenir en moins de 30 s, et un recalcul en moins de 5 s.

## 8. Tests

### 8.1. Quels tests écrire

| Type | Outil | Ce qui est testé | Nom de la classe |
| --- | --- | --- | --- |
| Unitaire | JUnit (Jupiter), AssertJ, Mockito | Services, moteur de calcul et de règles, objets de valeur, sans Spring ni base | `<Classe>Test` |
| Contrôleur | `@WebMvcTest`, `@MockitoBean` | Routes, validation, codes HTTP, format des erreurs, `@PreAuthorize` | `<Classe>ControllerTest` |
| Intégration | `@SpringBootTest` + `TestcontainersConfiguration` | Dépôts, migrations, isolation des écoles, sécurité de bout en bout | `<Sujet>IntegrationTest` |
| Acceptation | Tests paramétrés JUnit | Scénarios T01 à T10 du Project Scope et cas réels de SJI | `ScenariosAcceptationTest` (module `calcul`) |

### 8.2. Règles obligatoires

- Chaque fonctionnalité est livrée **avec ses tests**, dans la même branche. Pas de « tests plus tard ».
- **Couverture d'au moins 80 %** sur `calcul` et `regles` (rapport JaCoCo : `target/site/jacoco/index.html`).
- Chaque algorithme (A1 à A8) a des tests unitaires qui reprennent les exemples du cahier de conception, par exemple « 42 crédits, seuil 45, marge 3 : `A_DELIBERER` ». Ajoutez les cas limites : égalité exacte au seuil, note absente, `DISPENSE`, `ABSENT`, EC éliminatoire.
- Chaque nouvelle entité rattachée à une école a un **test d'isolation** : des données de l'école A sont invisibles depuis l'école B, en recherche comme en chargement par identifiant.
- Chaque nouvelle route a au moins trois tests : 401 sans jeton, 403 sans la permission, et succès avec la permission. Sa documentation OpenAPI est vérifiée par `DocumentationOpenApiTest` (section 5.1).
- Chaque correction de bogue commence par un test qui reproduit le bogue.
- Nom des méthodes de test : une phrase en français, en camelCase, qui décrit le comportement (`uneDetteRepriseNeCompteQuAuCumul`). Corps en trois temps, séparés par des commentaires : `// Étant donné`, `// Quand`, `// Alors`.
- Données de test **fictives uniquement**. Construisez-les avec des fabriques dans `src/test/java/com/delibera/<module>/fixtures/`.
- Un test ne dépend ni de l'ordre d'exécution, ni de l'heure système (injectez un `Clock`), ni d'un autre test.
- `@Disabled` est interdit sans justification écrite et accord de l'équipe. Il est interdit de supprimer ou d'affaiblir un test pour faire passer la construction.

### 8.3. Lancer les tests

```bash
./mvnw verify        # Linux, macOS ; mvnw.cmd verify sous Windows
```

Docker doit tourner (Testcontainers). **`./mvnw verify` doit réussir avant tout commit poussé.**

## 9. Documentation de chaque fonctionnalité

Chaque module a deux fichiers dans son dossier (`src/main/java/com/delibera/<module>/`) :

| Fichier | Rôle | Qui le modifie |
| --- | --- | --- |
| `SPEC.md` | **Ce que le module doit faire** : spécifications fonctionnelles et techniques, tirées des cahiers | Seulement pour y reporter une décision de l'équipe |
| `README.md` | **Ce qui est fait** : état de l'implémentation, routes réelles, codes d'erreur, tests | À chaque fonctionnalité, dans la même branche que le code |

### 9.1. SPEC.md du module

- Il existe pour les dix modules. Il suit toujours le même plan :
  - sources ;
  - rôle et exigences couvertes ;
  - acteurs et permissions ;
  - cas d'utilisation ;
  - modèle de données typé ;
  - contraintes ;
  - interfaces de service et algorithmes ;
  - routes proposées ;
  - dépendances ;
  - journal d'audit ;
  - tests d'acceptation ;
  - questions ouvertes.
- C'est **la référence pour implémenter le module**. Ce qu'il marque **[Proposition]** doit être validé par l'équipe avant d'être codé.
- Un agent ne le modifie pas de sa propre initiative. Quand l'équipe tranche une question ouverte ou valide une proposition, l'agent reporte la décision dans le `SPEC.md`, dans la même branche, et retire la marque **[Proposition]** ou la question.
- Une nouvelle question découverte pendant le développement s'ajoute à la section « Questions ouvertes » du `SPEC.md`, et elle est signalée à l'équipe.
- Si le `SPEC.md` contredit un cahier, arrêtez-vous et signalez-le : l'un des deux doit être corrigé.

### 9.2. README de module

- Chaque module a un `README.md` **dans son dossier** : `src/main/java/com/delibera/<module>/README.md`. Il est créé avec la première fonctionnalité du module.
- Chaque nouvelle fonctionnalité le met à jour **dans la même branche** que le code.
- Les sous-dossiers techniques (`web`, `service`, `domaine`, `depot`) n'ont pas de README : celui du module les décrit.
- Le `package-info.java` du module reste aligné sur le `SPEC.md` et le README.

Modèle :

```markdown
# M<n> – <Nom du module>

## Rôle
Ce que fait le module, en deux ou trois phrases. Cas d'utilisation couverts.

## Fonctionnalités
| Fonctionnalité | Cas d'utilisation | État | Branche |
| --- | --- | --- | --- |
| Créer une école cliente | CU15 | Terminée | feature/ecole-creation |

## Routes de l'API
| Verbe | Route | Permission | Réponse | Erreurs |
| --- | --- | --- | --- | --- |

## Modèle
Entités, tables, contraintes, énumérations. Écarts éventuels avec le cahier de conception.

## Règles métier appliquées
Chaque règle, avec sa référence (A1…A8, OCL §4.2.7, BF/BNF, décision de l'équipe).

## Codes d'erreur
| Code | Statut HTTP | Situation |
| --- | --- | --- |

## Journal d'audit
Actions journalisées par le module.

## Tests
Classes de test et ce qu'elles couvrent.

## Écarts avec le SPEC.md
Ce qui diffère de la spécification, avec la décision correspondante (docs/decisions/).
```

### 9.3. README racine

Mettez à jour `README.md` à la racine quand une fonctionnalité change le démarrage, la configuration, les variables d'environnement ou l'état d'avancement des itérations.

### 9.4. Décisions

Tout écart par rapport au cahier de conception, tout choix technique notable et toute nouvelle dépendance font l'objet d'une fiche :
- fichier `docs/decisions/NNNN-titre-court.md` ;
- contenu : contexte, décision, conséquences, alternatives écartées.

Exemple : le choix de séquences plutôt que de colonnes auto-incrémentées pour les identifiants.

## 10. Git et branches

### 10.1. Branches

| Branche | Rôle | Qui écrit dessus |
| --- | --- | --- |
| `main` | Version en production : **chaque push sur `main` déploie l'API** (README, « Déploiement continu »). Étiquette `v0.<itération>.0` à la fin de chaque itération. | Fusion depuis `develop` uniquement, par l'équipe |
| `develop` | Intégration | Fusion des demandes de fusion relues |
| `feature/<module>-<sujet>` | Une fonctionnalité | L'agent ou le développeur qui l'a créée |
| `fix/<module>-<sujet>` | Une correction | Idem |
| `docs/<sujet>` | Documentation seule | Idem |

- Les branches de travail partent **toujours de `develop` à jour** :

  ```bash
  git switch develop && git pull
  git switch -c feature/<module>-<sujet>
  ```

- Nom de branche : en minuscules, avec des tirets, sans accents. `<module>` est le nom du paquetage. Exemples : `feature/ecole-creation`, `feature/securite-connexion-jwt`, `fix/calcul-arrondi-note-ec`.
- **Une branche = une fonctionnalité** (en général un cas d'utilisation, ou une partie cohérente d'un cas).
- Un agent ne pousse **jamais** directement sur `main` ou `develop`, ne fusionne jamais lui-même, et n'utilise jamais `git push --force` sur une branche partagée.

### 10.2. Commits

- Format **Conventional Commits**, en français : `<type>(<module>): <description à l'infinitif>`.
- Types : `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
- Exemples :

  ```
  feat(ecole): créer une école cliente et son premier administrateur
  test(calcul): automatiser les scénarios T01 à T10
  fix(calcul): arrondir la note d'EC une seule fois
  ```

- Un commit = un changement logique, qui compile.
- Un commit écrit par un agent se termine par la ligne `Co-Authored-By: <nom de l'agent> <adresse>`.
- Jamais de fichiers générés (`target/`), de secrets ni de données réelles dans un commit.

### 10.3. Demande de fusion

Vers `develop`. La description contient :

```markdown
## Fonctionnalité
<description> — cas d'utilisation : CUxx — module : Mx

## Changements
- ...

## Migrations
- Vn__... (nouvelles tables, contraintes)

## Tests
- ... (unitaires, intégration, isolation, sécurité) — `./mvnw verify` : OK

## Écarts avec le SPEC.md ou le cahier de conception
Aucun / voir docs/decisions/NNNN-...

## Propositions du SPEC.md validées
Aucune / liste, avec qui les a validées

## Questions ouvertes
...
```

La relecture par un membre de l'équipe est obligatoire avant la fusion.

## 11. Déroulé d'une nouvelle fonctionnalité (pour un agent)

1. **Lire** :
   - ce fichier et le `README.md` racine ;
   - le `SPEC.md` du module (cas d'utilisation, modèle, contraintes, services, algorithmes, routes, tests d'acceptation) ;
   - le README du module, et le `SPEC.md` des modules dont il dépend ;
   - en cas de doute, la section du cahier citée par le `SPEC.md`.
2. **Clarifier** : relever les éléments **[Proposition]** et les **questions ouvertes** du `SPEC.md` qui touchent la fonctionnalité, ainsi que tout point ambigu. Les **poser à l'équipe avant de coder**. Ne pas deviner une règle métier.
3. **Créer la branche** depuis `develop` à jour (section 10.1).
4. **Documenter le plan** : créer ou mettre à jour le README du module (fonctionnalité « en cours », routes prévues, entités, tests prévus).
5. **Implémenter dans cet ordre** :
   1. la migration Flyway ;
   2. les entités (`domaine`) ;
   3. les dépôts (`depot`) ;
   4. l'interface de service et son implémentation, avec leurs tests unitaires ;
   5. les DTO et les mappers ;
   6. les contrôleurs, avec leurs tests ;
   7. les tests d'intégration (isolation, sécurité) ;
   8. la documentation OpenAPI de chaque route (section 5.1), essayée dans Swagger UI.
6. **Vérifier** : `./mvnw verify` au vert, couverture regardée, Swagger UI cohérent.
7. **Finaliser la documentation** :
   - README du module (état « terminée ») ;
   - `SPEC.md`, si l'équipe a tranché une question ou validé une proposition ;
   - README racine, si besoin ;
   - fiche de décision, en cas d'écart.
8. **Committer et pousser** la branche, puis préparer la demande de fusion (section 10.3).
9. **Rendre compte** à l'équipe :
   - ce qui est fait et ce qui ne l'est pas ;
   - les écarts avec le `SPEC.md` ou le cahier de conception ;
   - les questions ouvertes, nouvelles ou restantes ;
   - les commandes pour vérifier.

## 12. Ce qu'un agent demande avant de faire

- Ajouter, retirer ou changer la version d'une dépendance Maven.
- Modifier `SecuriteConfig`, le filtre d'isolation, `EntiteBase` ou `EntiteEcole`.
- Modifier une migration existante, ou toucher à des données.
- S'écarter du `SPEC.md` ou du cahier de conception (nom, modèle, algorithme, dépendance entre modules).
- Implémenter un élément marqué **[Proposition]** dans un `SPEC.md`.
- Modifier un `SPEC.md` pour autre chose que reporter une décision de l'équipe ou ajouter une question ouverte.
- Modifier l'intégration continue, le `Dockerfile` ou `compose.yaml`.
- Supprimer des fichiers, ou toute action sur `main` ou `develop`.

## 13. Interdits

- Écrire une valeur de règle ou un seuil dans le code de production.
- Prendre l'école d'un paramètre envoyé par le client.
- Renvoyer une entité JPA depuis un contrôleur.
- Appeler le dépôt ou l'implémentation d'un autre module.
- Utiliser `double` ou `float` pour une note.
- Mettre `ddl-auto` à `update` ou `create`.
- Modifier une migration déjà fusionnée.
- Committer un secret, un fichier `.env` ou des données réelles.
- Désactiver, supprimer ou affaiblir un test pour faire passer la construction.
- Pousser sur `main` ou `develop`, forcer un push sur une branche partagée.
- Écrire une donnée personnelle ou une note dans les journaux.

## 14. Environnement technique

| Élément | Version | Point d'attention |
| --- | --- | --- |
| Java | 21 | Utiliser les `record`, `switch` par motif et `var` avec mesure |
| Spring Boot | 4.1.3 | Voir les pièges ci-dessous |
| Spring Framework | 7.0 | `HttpStatus.UNPROCESSABLE_CONTENT` (et non `UNPROCESSABLE_ENTITY`, déprécié) |
| Spring Security | 7.1 | Ressource serveur JWT, `@EnableMethodSecurity` |
| Hibernate | 7.4 | Filtre `@FilterDef(autoEnabled = true, applyToLoadByKey = true)` |
| Jackson | 3.x | Paquetage `tools.jackson.databind` ; les annotations restent dans `com.fasterxml.jackson.annotation` |
| JUnit | 6.0 (Jupiter) | |
| Testcontainers | 2.0 | `org.testcontainers.postgresql.PostgreSQLContainer`, artefacts `testcontainers-*` |
| PostgreSQL | 16 | |
| springdoc-openapi | 3.1 | |
| MapStruct | 1.6 | Les mappers sont des beans Spring (configuré dans `pom.xml`) |

**Pièges de Spring Boot 4 pour un agent habitué à Spring Boot 3 :**
- Starters renommés :
  - `spring-boot-starter-webmvc` (et non `-web`) ;
  - Flyway passe par `spring-boot-starter-flyway` ;
  - tests web par `spring-boot-starter-webmvc-test`.
- Annotations de test déplacées :
  - `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` ;
  - `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` ;
  - `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.
- `@MockBean` n'existe plus : utiliser `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- En cas de doute sur une API, vérifiez dans les sources de la version utilisée plutôt que de vous fier à votre mémoire.

**Commandes utiles :**

```bash
docker compose up -d                                         # PostgreSQL 16 + Mailpit (http://localhost:8025)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev        # API sur http://localhost:8080
./mvnw verify                                                # compilation + tous les tests + couverture
```

Swagger UI : http://localhost:8080/swagger-ui.html. Variante sans `compose.yaml` : lancer `TestDeliberaApplication` (dans `src/test/java`).

## 15. Définition de « terminé »

Une fonctionnalité est terminée quand **toutes** ces cases sont cochées :

- [ ] Le comportement correspond au `SPEC.md` du module (cas d'utilisation, contraintes, algorithmes), ou l'écart est consigné et accepté.
- [ ] Les éléments **[Proposition]** utilisés ont été validés par l'équipe, et le `SPEC.md` est mis à jour.
- [ ] Les tests d'acceptation du `SPEC.md` qui concernent la fonctionnalité sont automatisés.
- [ ] La migration Flyway est présente, avec la séquence, `ecole_id` et les contraintes.
- [ ] Les entités héritent de `EntiteBase` ou de `EntiteEcole`.
- [ ] Chaque méthode de service exposée a son `@PreAuthorize`, et le contrôle A8 est appliqué si un périmètre est en jeu.
- [ ] Les modifications sont journalisées (M9).
- [ ] Les tests unitaires, de contrôleur, d'isolation et de sécurité sont écrits et passent. La couverture est respectée pour `calcul` et `regles`.
- [ ] `./mvnw verify` réussit.
- [ ] Le README du module est à jour (routes, règles, codes d'erreur, tests, écarts avec le `SPEC.md`).
- [ ] Chaque route est documentée dans Swagger (section 5.1) : tag décrit, résumé, description avec la permission, toutes les réponses avec leurs codes d'erreur, paramètres et DTO décrits avec des exemples fictifs.
- [ ] `DocumentationOpenApiTest` passe, et la route a été essayée dans Swagger UI.
- [ ] Aucun secret, aucune donnée réelle, aucun seuil codé en dur.
- [ ] La branche est poussée et la demande de fusion vers `develop` est rédigée.
