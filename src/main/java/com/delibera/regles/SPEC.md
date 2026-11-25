# M4 – Règles et paramètres : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.4 CU02 et CU13, §4.1, §4.2.3, §4.2.7, §4.3), cahier de conception (version en ligne de fin novembre 2026 : §4.2.2, §6.1.1, §6.1.4, algorithmes A2 et A5), cahier des charges technique (BF04, BF17, BNF02), Project Scope (§5, §18, T01 à T10).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

- Enregistrer, versionner et activer les **règles de passage**. Une règle est un ensemble de conditions combinées par ET ou par OU, **seuils limites compris**. Elle dit **qui passe** d'un niveau au suivant.
- Enregistrer, versionner et activer les **jeux de paramètres de délibération**. Ils disent **comment on calcule**.
- Choisir, pour un étudiant, la règle et le jeu de paramètres les plus précis (algorithme A2).

**Aucune règle ni valeur seuil n'est écrite dans le code (BNF02)**.

Paquetage : `com.delibera.regles`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF04 | Créer, activer et versionner des règles composées de conditions : crédits annuels, crédits cumulés, dette sur un niveau donné, UE obligatoire, moyenne minimale. Conditions combinées toutes par ET ou toutes par OU, éventuellement propres à une filière ou à une spécialité. Exemple de fin de cycle : crédits cumulés **et** absence de dette. |
| BF17 | Saisir et versionner les paramètres par école, cycle, niveau et année : crédits d'une année, note de validation, note éliminatoire, capitalisation des EC, mode de calcul des moyennes, combinaison de plusieurs CC, traitement des absences, délai de réclamation, profondeur de l'historique. |
| BNF02 | Tout se modifie depuis l'interface, sans modifier le programme. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU02 Gérer les règles de passage | Gestionnaire académique, d'après les règles fournies par le président du jury | `REGLE_GERER` (périmètre : le niveau de départ) |
| CU13 Gérer les paramètres de délibération | Administrateur d'école | `PARAM_GERER` |

## 4. Cas d'utilisation

### CU02 – Gérer les règles de passage

- **Préconditions** : le référentiel existe (école, cycle, niveaux) ; le président du jury a remis les règles, seuils limites compris.
- **Scénario principal** :
  1. Le gestionnaire crée une règle :
     - école ;
     - niveau de départ ;
     - éventuellement une filière, ou une spécialité, si la règle leur est propre ;
     - date d'effet.

     Le niveau d'arrivée est le niveau suivant du référentiel.
  2. Il ajoute des conditions. Chacune a :
     - une variable, un opérateur et une valeur ;
     - le niveau ou l'UE visés, si la variable l'exige ;
     - un ordre ;
     - l'indication **dérogeable** ou non ;
     - pour une condition dérogeable, son **seuil limite**. Exemple : crédits annuels `>=` 45, seuil limite 42.
  3. Il choisit la combinaison ET ou OU.
  4. Il active la règle. Le système vérifie la cohérence et enregistre la **version 1**.
- **Exemple de fin de cycle** : `CREDITS_CUMULES >= seuil` (dérogeable, avec son seuil limite) ET `DETTE_NIVEAU = 0` (non dérogeable).
- **Scénarios alternatifs** :
  - Une règle active existe déjà pour la même portée (niveau de départ, filière, spécialité) : le système crée une **nouvelle version**. L'ancienne reste liée aux délibérations passées (T09).
  - Condition incomplète : refus, avec le champ manquant signalé.
  - Seuil limite qui atteint la valeur de sa condition (42 est permis pour une condition à 45, mais pas 45), ou seuil limite donné pour une condition non dérogeable : refus.
  - Condition de crédits dérogeable sans seuil limite : elle n'ouvre **aucune marge**. L'étudiant qui y échoue est ajourné (T02).
- **Postcondition** : règle et conditions enregistrées, versionnées et journalisées.

### CU13 – Gérer les paramètres de délibération

- **Préconditions** : l'école et l'année existent.
- **Scénario principal** :
  1. L'administrateur choisit la portée (toute l'école, ou un cycle, ou un niveau) et l'année.
  2. Il saisit les crédits d'une année, la note de validation, la note éliminatoire (appliquée à chaque EC), la capitalisation des EC, le mode de rattrapage, le mode de moyenne, la combinaison des CC, le traitement des absences, le délai de réclamation et la profondeur d'historique.
  3. Le système vérifie la cohérence (note éliminatoire inférieure à la note de validation, notes entre 0 et 20) et active le jeu.
- **Scénarios alternatifs** :
  - Jeu déjà utilisé par une délibération clôturée : nouvelle version.
  - Plusieurs jeux couvrent un étudiant : le plus précis s'applique (niveau, puis cycle, puis école), et le système affiche lequel.
- **Postcondition** : un jeu actif, versionné et journalisé, s'applique à la portée choisie.

## 5. Modèle de données

Toutes les entités persistées héritent de `EntiteEcole`.

### ReglePassage

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | Le couple (`code`, `version`) est unique dans l'école |
| `libelle` | `String` | |
| `version` | `int` | Version métier, distincte de `verrou` |
| `active` | `boolean` | |
| `dateEffet` | `LocalDate` | |
| `combinaison` | `OperateurLogique` | `ET` ou `OU`. Les mélanges du type `(A ET B) OU C` ne sont pas prévus. |

Méthodes :
- `couvre(InscriptionAnnuelle ia) : boolean`
- `getPrecision() : int`
- `evaluer(ContexteEvaluation ctx) : ResultatEvaluation`
- `activer() : void`
- `nouvelleVersion() : ReglePassage`

Associations :
- fixée par une `Ecole` ;
- part d'un `Niveau` (1) ;
- peut se limiter à une `Filiere` (0..1) et à une `Specialite` (0..1) ;
- pose des `ConditionRegle` (composition, 1..*) ;
- figée par des délibérations (M7) ;
- appliquée à des situations (M6).

### ConditionRegle

| Attribut | Type | Règle |
| --- | --- | --- |
| `variable` | `VariableRegle` | |
| `operateur` | `OperateurComparaison` | |
| `valeur` | `BigDecimal` | |
| `derogeable` | `boolean` | Le jury peut délibérer sur l'échec de cette condition |
| `seuilLimite` | `BigDecimal` | Renseigné **seulement** si `derogeable` ; fourni par le président du jury |
| `ordre` | `int` | Ordre d'affichage et d'évaluation |

Méthodes :
- `estSatisfaite(ContexteEvaluation ctx) : boolean`
- `valeurObservee(ContexteEvaluation ctx) : BigDecimal`
- `atteintSeuilLimite(ContexteEvaluation ctx) : boolean` : applique l'opérateur au seuil limite au lieu de la valeur.

Associations :
- vise un `Niveau` (0..1), obligatoire pour `DETTE_NIVEAU` ;
- exige une `UE` (0..1), obligatoire pour `UE_VALIDEE`.

### ParametreDeliberation

| Attribut | Type | Règle |
| --- | --- | --- |
| `version` | `int` | Version métier |
| `actif` | `boolean` | |
| `creditsAnnee` | `int` | Par exemple 60 |
| `noteValidation` | `BigDecimal` | Entre 0 et 20, par exemple 10 |
| `noteEliminatoire` | `BigDecimal` | Entre 0 et 20 et `< noteValidation`, par exemple 7 (à confirmer) |
| `capitalisationEC` | `boolean` | Les EC validés d'une UE non validée gardent leurs crédits |
| `modeRattrapage` | `ModeRattrapage` | Saint Jean applique `REMPLACE_SN` |
| `modeMoyenne` | `ModeMoyenne` | À confirmer pour SJI et SJM |
| `combinaisonCC` | `CombinaisonCC` | À confirmer |
| `traitementAbsence` | `TraitementAbsence` | À confirmer |
| `delaiReclamationJours` | `int` | Délai après publication (M7) |
| `historiqueAnnees` | `int` | Profondeur des acquis importés : 2 à 3 ans |

Méthodes : `getPrecision() : int`, `nouvelleVersion() : ParametreDeliberation`.

Associations : régi par une `AnneeAcademique` (1) ; peut cibler un `Cycle` (0..1) ou un `Niveau` (0..1). Sans cible, le jeu porte sur toute l'école.

### Objets de valeur (non stockés, `record`)

- `ContexteEvaluation` : valeurs d'un étudiant.
  - Champs : `creditsAnnuels : int`, `creditsCumules : int`, `dettesParNiveau : Map<Long,Integer>`, `moyenne : BigDecimal`, `uesValidees : Set<Long>`.
  - Méthode : `valeurDe(VariableRegle v, Long niveauId, Long ueId) : BigDecimal`.
- `ResultatEvaluation` : `satisfaite : boolean`, `echecs : List<ConditionRegle>`, `echecsNonDerogeables() : List<ConditionRegle>`.

### Énumérations

| Énumération | Valeurs |
| --- | --- |
| `VariableRegle` | `CREDITS_ANNUELS`, `CREDITS_CUMULES`, `DETTE_NIVEAU`, `UE_VALIDEE`, `MOYENNE_ANNUELLE` |
| `OperateurComparaison` | `SUP_EGAL`, `SUP`, `INF_EGAL`, `INF`, `EGAL`, `DIFFERENT` |
| `OperateurLogique` | `ET`, `OU` |
| `ModeRattrapage` | `REMPLACE_SN`, `MEILLEURE_NOTE` |
| `ModeMoyenne` | `PONDEREE_PAR_CREDITS`, `ARITHMETIQUE` |
| `CombinaisonCC` | `MOYENNE`, `MEILLEURE_NOTE` |
| `TraitementAbsence` | `ZERO`, `NON_EVALUE` |

## 6. Patron de conception imposé

- **Stratégie** : chaque valeur de `VariableRegle` sait extraire sa valeur d'un `ContexteEvaluation`, et chaque valeur de `OperateurComparaison` sait comparer deux `BigDecimal`. Ce sont des énumérations avec méthode.
- **Interpréteur** : `ReglePassage.evaluer` compose ses conditions (toutes en ET, ou toutes en OU) et renvoie un `ResultatEvaluation` avec la liste des échecs.
- Pas de `switch` géant dans un service, pas de règle écrite en Java.

## 7. Contraintes d'intégrité

```
context ConditionRegle inv:
  self.seuilLimite->notEmpty() implies self.derogeable

context ReglePassage inv porteeUnique:
  self.active implies ReglePassage.allInstances()->select(r |
      r <> self and r.active and r.ecole = self.ecole and r.niveau = self.niveau
      and r.filiere = self.filiere and r.specialite = self.specialite)->isEmpty()
```

Autres règles :
- Pour une condition de type `>=` ou `>`, le seuil limite est strictement inférieur à la valeur. Inversement pour `<=` ou `<` **[Proposition]**.
- Une filière de portée doit couvrir le niveau de départ, et une spécialité de portée doit appartenir à cette filière.
- Une condition `DETTE_NIVEAU` désigne un niveau ; une condition `UE_VALIDEE` désigne une UE ; les autres n'en désignent pas.
- Une règle ou un jeu de paramètres déjà figé par une délibération **ne se modifie jamais** : toute modification crée une nouvelle version (T09).
- Deux jeux de paramètres actifs n'ont pas la même portée pour la même année **[Proposition, par analogie avec les règles]**.

## 8. Algorithme A2 – Choix des paramètres et de la règle applicables

```text
FONCTION parametresApplicables(annee, niveau) : ParametreDeliberation
  jeux <- jeux de parametres actifs de l'ecole pour l'annee
  POUR portee DANS [ce niveau, le cycle du niveau, toute l'ecole] FAIRE  // du plus precis au plus general
    SI jeux contient un jeu de cette portee ALORS RETOURNER ce jeu
  FIN POUR
  ECHEC "aucun parametre actif"                                          // CU04, scenario alternatif
FIN

FONCTION regleApplicable(ia, d) : ReglePassage ou NUL
  candidates <- regles r de d.regles telles que r.niveau = ia.niveau
  SI ia.specialite != NUL ET une candidate a r.specialite = ia.specialite ALORS RETOURNER cette regle
  SI une candidate a r.filiere = ia.filiere ET r.specialite = NUL ALORS RETOURNER cette regle
  SI une candidate a r.filiere = NUL ET r.specialite = NUL ALORS RETOURNER cette regle
  RETOURNER NUL                                                          // dossier bloque, gestionnaire informe
FIN
```

Deux règles actives n'ont jamais la même portée : une seule candidate existe à chaque étape. En Ing 3, un étudiant d'ISI sans règle propre à sa filière reçoit la règle générale d'Ing 3.

L'évaluation de la règle et la proposition de décision (A5) relèvent de M6, qui appelle `ReglePassage.evaluer`.

## 9. Interfaces de service

```java
public interface ResolutionService {                       // cahier de conception, §6.1.8
    ParametreDeliberation parametresApplicables(AnneeAcademique annee, Niveau niveau);
    Optional<ReglePassage> regleApplicable(InscriptionAnnuelle ia, Deliberation d);
}
```

**[Proposition]** Services de gestion pour CU02 et CU13 :
- `RegleService` : `creer`, `ajouterCondition`, `activer`, `nouvelleVersion`, `reglesActives(niveauId)`, `historique(code)` ;
- `ParametreService` : `creer`, `activer`, `nouvelleVersion`, `jeuApplicable(anneeId, niveauId)`.

## 10. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| GET, POST | `/api/v1/regles-passage` | Lecture : `REGLE_GERER` ou `DOSSIER_CONSULTER` ; écriture : `REGLE_GERER` |
| GET | `/api/v1/regles-passage/{code}/versions` | Idem |
| POST | `/api/v1/regles-passage/{id}/activation` | `REGLE_GERER` |
| POST | `/api/v1/regles-passage/{id}/nouvelle-version` | `REGLE_GERER` |
| GET, POST | `/api/v1/parametres-deliberation` | `PARAM_GERER` |
| POST | `/api/v1/parametres-deliberation/{id}/activation` | `PARAM_GERER` |
| GET | `/api/v1/parametres-deliberation/applicable?anneeId=&niveauId=` | `PARAM_GERER` ou `PREDELIB_LANCER` |

## 11. Dépendances

| Vers | Pour |
| --- | --- |
| M3 Référentiel | Niveaux, filières, spécialités, UE, cycles, années |
| M9 Audit | Création, activation, versions |

Appelé par : M6 (`ReglePassage.evaluer` et `ResolutionService`) et M7 (figer les versions sur la délibération, A1).

## 12. Journal d'audit

`REGLE_CREEE`, `REGLE_ACTIVEE`, `REGLE_NOUVELLE_VERSION`, `PARAMETRES_CREES`, `PARAMETRES_ACTIVES`, `PARAMETRES_NOUVELLE_VERSION` **[Proposition]**, avec l'ancienne et la nouvelle valeur (BNF03).

## 13. Tests d'acceptation

**Tests unitaires du moteur de règles (couverture d'au moins 80 %)** :
- Chaque opérateur, y compris l'égalité exacte au seuil : 45 `>=` 45 est satisfaite (T01).
- Combinaisons ET et OU, avec la liste des échecs.
- `atteintSeuilLimite` : 42 crédits, condition `>=` 45, seuil limite 42 → vrai ; 41 → faux.
- `valeurDe` pour chaque variable, dont `DETTE_NIVEAU` (niveau visé) et `UE_VALIDEE` (UE visée).

**A2 (choix de la règle et des paramètres)** :
- La règle de spécialité l'emporte sur celle de filière, qui l'emporte sur la règle générale du niveau.
- Le jeu du niveau l'emporte sur celui du cycle, qui l'emporte sur celui de l'école.
- Aucune règle ni aucun paramètre : échec signalé.

**CU02 et CU13** :
- Seuil limite sur une condition non dérogeable : refus.
- Seuil limite égal à la valeur : refus.
- Activation d'une règle de même portée qu'une règle active : nouvelle version.
- Note éliminatoire supérieure ou égale à la note de validation : refus.

**T09** : une règle modifiée après une délibération garde sa version historique, identifiable sur la délibération.

## 14. Questions ouvertes

1. Valeurs de `modeMoyenne`, `combinaisonCC` et `traitementAbsence` retenues par SJI et SJM.
2. La note éliminatoire est-elle bien de 7 ?
3. Règles réelles de SJI et SJM (document du président du jury) pour les tests d'acceptation.
4. Une condition `MOYENNE_ANNUELLE` peut-elle être dérogeable avec un seuil limite ? A5 n'ouvre la marge qu'aux crédits.
