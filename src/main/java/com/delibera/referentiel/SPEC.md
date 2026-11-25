# M3 – Référentiel et maquettes : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.1 BF01 à BF03, §4.1, §4.2.1, §4.2.2, §4.2.7), cahier de conception (version en ligne de fin novembre 2026 : §5.1, §6.1.1, §6.1.2, §6.1.3), cahier des charges technique (BF01 à BF03).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Décrire l'organisation académique propre à chaque école :
- années académiques et semestres, sessions d'évaluation ;
- cycles, filières et niveaux couverts, spécialités, classes ;
- catalogue des UE et des EC ;
- maquette de chaque année : quelle UE est ouverte à quel niveau, à quelles filières, avec quels enseignements, crédits et types d'évaluation.

Paquetage : `com.delibera.referentiel`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF01 | Gérer les années académiques : créer, ouvrir, clôturer, archiver. |
| BF02 | Gérer cycles, filières (avec les niveaux qu'elles couvrent), spécialités et niveaux. |
| BF03 | Gérer les UE et les EC : saisie de la maquette ou import (M5, type `MAQUETTE`), pour chaque année académique. |
| BNF02 | Aucune valeur codée en dur : crédits, pourcentages et structure sont des données. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU01 Paramétrer le référentiel académique | Administrateur d'école | `REFERENTIEL_GERER` |
| Lire le référentiel (listes, filtres) | Tout utilisateur de l'école | Authentifié **[Proposition]** |

## 4. Cas d'utilisation – CU01 Paramétrer le référentiel académique

Le cahier d'analyse ne détaille pas CU01. Déroulé déduit de BF01 à BF03 et du modèle **[Proposition]** :

1. Créer l'année académique (code, dates), ses semestres et ses sessions d'évaluation (normale, rattrapage). Faire évoluer son statut : `EN_PREPARATION` → `OUVERTE` → `CLOTUREE` → `ARCHIVEE`.
2. Créer les cycles (Licence, Master, Ingénieur), leurs niveaux (numéro, libellé, diplômant) et la chaîne des niveaux suivants.
3. Créer les filières d'un cycle, avec les niveaux qu'elles couvrent, puis leurs spécialités.
4. Créer les classes d'une année (niveau, filière, spécialité éventuelle).
5. Alimenter le catalogue des UE et des EC de l'école.
6. Construire la maquette de l'année :
   - pour chaque UE ouverte, une `MaquetteUE` : niveau, semestre, filières et éventuelles spécialités, caractère obligatoire ;
   - ses `Enseignement` : EC et crédits ;
   - pour chaque enseignement, ses `TypeEvaluation` : nature et pourcentage.

**Exemple de SJI (cycle Ingénieur)** : Ing 1 → Ing 2 → Ing 3 → Ing 4 → Ing 5 (diplômant). La filière Tronc commun couvre Ing 1 et Ing 2 ; ISI, SRT, GC et GELEC couvrent chacune Ing 3, Ing 4 et Ing 5. Il n'existe qu'un seul Ing 3, partagé par les quatre filières.

## 5. Modèle de données

Toutes les entités héritent de `EntiteEcole`. Les codes sont uniques **au sein de l'école** et servent de clés aux fichiers d'import.

### AnneeAcademique

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | Unique dans l'école, par exemple `2026-2027` |
| `dateDebut`, `dateFin` | `LocalDate` | `dateDebut < dateFin` |
| `statut` | `StatutAnnee` | `EN_PREPARATION`, `OUVERTE`, `CLOTUREE`, `ARCHIVEE` |

Méthodes : `ouvrir()`, `cloturer()`, `archiver()`.

Associations : organisée par une `Ecole` ; comporte des `Semestre` (composition, 1..*) ; compte des `SessionEvaluation` (composition, 1..*) ; ouvre des `Classe`.

### Semestre

`libelle : String`, `rang : int` (unique dans l'année).

### SessionEvaluation [rattachement proposé à M3]

`type : TypeSession` (`NORMALE`, `RATTRAPAGE`), `dateDebut : LocalDate`, `dateFin : LocalDate`. Elle appartient à une année.

### Cycle

`code : String`, `libelle : String`. Un cycle regroupe des filières (1..*) et s'étend sur des niveaux (1..*).

### Filiere

| Attribut | Type |
| --- | --- |
| `code` | `String` |
| `libelle` | `String` |

Méthode : `couvre(Niveau niveau) : boolean`.

Associations : appartient à un cycle ; couvre des niveaux (plusieurs à plusieurs, 1..*) ; propose des spécialités (0..*).

### Specialite

`code : String`, `libelle : String`. Elle appartient à une filière, qui peut n'en avoir aucune (Tronc commun).

### Niveau

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | |
| `numero` | `int` | |
| `libelle` | `String` | |
| `diplomant` | `boolean` | Indépendant du niveau suivant : L3 est diplômant et a un suivant (M1) |

Méthode : `getNiveauSuivant() : Niveau`.

Associations :
- appartient à un cycle ;
- « précéder » : relation réflexive 0..1 / 0..1. Le niveau suivant peut être dans un autre cycle (L3 → M1). Un niveau sans suivant est une sortie de parcours.

### Classe

`libelle : String`. Elle relève d'un niveau, d'une année, d'une filière et éventuellement d'une spécialité.

### UE et EC (catalogues de l'école)

- UE : `codeUE : String`, `libelle : String`.
- EC : `codeEC : String`, `libelle : String`.

Un EC n'a ni niveau ni spécialité figés : ses liens annuels passent par la maquette.

### MaquetteUE

| Attribut | Type | Règle |
| --- | --- | --- |
| `obligatoire` | `boolean` | |
| `/credits` | dérivé | `getCredits()` = somme des crédits de ses `Enseignement` ; **non stocké** |

Méthodes : `getCredits() : int`, `estOuverteA(Filiere f, Specialite s) : boolean`.

Associations :
- programme une UE ;
- concerne un niveau ;
- se déroule en un semestre (donc dans une année) ;
- s'adresse à une ou plusieurs filières (1..*) ;
- peut se limiter à des spécialités (0..*) ;
- se compose d'`Enseignement` (composition, 1..*) ;
- peut être alimentée par un `ImportFichier` (0..1).

### Enseignement

`credits : int` (> 0), `getLibelle() : String` (le libellé est celui de l'EC). Il porte sur un EC, appartient à une `MaquetteUE` et prévoit des `TypeEvaluation` (1..*).

### TypeEvaluation [rattachement proposé à M3]

| Attribut | Type | Règle |
| --- | --- | --- |
| `nature` | `NatureEvaluation` | `CC`, `TP`, `SN` ou `RAT`, prise dans l'énumération et jamais déduite du libellé |
| `libelle` | `String` | |
| `pourcentage` | `BigDecimal` | Poids dans la note de l'EC |

Méthode : `estRattrapage() : boolean`.

### Evaluation [rattachement proposé à M3]

`libelle : String`, `dateEvaluation : LocalDate`. Elle relève d'un `TypeEvaluation` et d'une `SessionEvaluation`. Elle est souvent créée par l'import des notes (M5).

### Énumérations

| Énumération | Valeurs |
| --- | --- |
| `StatutAnnee` | `EN_PREPARATION`, `OUVERTE`, `CLOTUREE`, `ARCHIVEE` |
| `TypeSession` | `NORMALE`, `RATTRAPAGE` |
| `NatureEvaluation` | `CC`, `TP`, `SN`, `RAT` |

## 6. Contraintes d'intégrité (à vérifier dans les services et, si possible, en base)

```
context Filiere inv:
  self.niveau->forAll(n | n.cycle = self.cycle)              -- une filière ne couvre que des niveaux de son cycle

context Classe inv:
  self.filiere.niveau->includes(self.niveau)
  and (self.specialite->notEmpty() implies self.specialite.filiere = self.filiere)

context MaquetteUE inv:
  self.filiere->forAll(f | f.niveau->includes(self.niveau))
  and self.specialite->forAll(s | self.filiere->includes(s.filiere))

context MaquetteUE inv: self.enseignement->isUnique(ec)          -- un EC au plus une fois par MaquetteUE

context Enseignement inv centPourCent:
  self.typeEvaluation->reject(t | t.nature = NatureEvaluation::RAT).pourcentage->sum() = 100

context Evaluation inv sessionCoherente:
  self.sessionEvaluation.anneeAcademique
      = self.typeEvaluation.enseignement.maquetteUE.semestre.anneeAcademique
  and (self.typeEvaluation.nature = NatureEvaluation::RAT)
      = (self.sessionEvaluation.type = TypeSession::RATTRAPAGE)

context Enseignement inv memeEcole:
  self.ec.ecole = self.maquetteUE.ue.ecole
  and self.maquetteUE.semestre.anneeAcademique.ecole = self.ec.ecole
```

Autres règles :
- Un changement de maquette ne modifie jamais les années passées : la maquette est propre à une année.
- La chaîne des niveaux suivants ne forme pas de boucle **[Proposition]**.
- Les transitions de statut d'une année sont journalisées. Les imports (M5) exigent une année `OUVERTE`.

## 7. Interfaces de service

Le cahier de conception nomme l'interface `IReferentiel`, utilisée par M5 et M6, sans en donner les méthodes. Proposition :

```java
public interface ReferentielService {                       // [Proposition]
    AnneeAcademique anneeParCode(String code);
    Niveau niveauParCode(String code);
    Optional<Niveau> niveauSuivant(Long niveauId);
    Filiere filiereParCode(String code);
    UE ueParCode(String codeUE);
    EC ecParCode(String codeEC);
    List<MaquetteUE> maquettes(Long anneeId, Long niveauId);   // avec enseignements et types d'évaluation
    boolean filiereCouvre(Long filiereId, Long niveauId);
    SessionEvaluation session(Long anneeId, TypeSession type);
}
```

S'y ajoutent les services de gestion pour CU01 : création, modification et transitions de chaque entité.

## 8. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| GET, POST | `/api/v1/annees-academiques` | Lecture : authentifié ; écriture : `REFERENTIEL_GERER` |
| POST | `/api/v1/annees-academiques/{id}/ouverture`, `/cloture`, `/archivage` | `REFERENTIEL_GERER` |
| GET, POST | `/api/v1/annees-academiques/{id}/semestres`, `/sessions-evaluation` | Idem |
| GET, POST, PUT | `/api/v1/cycles`, `/api/v1/niveaux`, `/api/v1/filieres`, `/api/v1/specialites` | Idem |
| PUT | `/api/v1/filieres/{id}/niveaux` | `REFERENTIEL_GERER` (niveaux couverts) |
| GET, POST, PUT | `/api/v1/classes` | Idem |
| GET, POST, PUT | `/api/v1/ues`, `/api/v1/ecs` | Idem |
| GET, POST, PUT, DELETE | `/api/v1/maquettes` (avec enseignements et types d'évaluation) | Idem ; suppression interdite si des inscriptions existent |

## 9. Dépendances

| Vers | Pour |
| --- | --- |
| M9 Audit | Toute modification du référentiel |

Appelé par : M5 (contrôles d'import), M6 (maquettes, niveaux), M4 (portée des règles et des paramètres), M7 (année, niveau d'une délibération), M8 (libellés des documents).

## 10. Journal d'audit

Création, modification et suppression de chaque entité, et transitions de statut d'une année. Format : `REFERENTIEL_<ENTITE>_<ACTION>` **[Proposition]**.

## 11. Tests d'acceptation

- Une filière ne peut pas couvrir un niveau d'un autre cycle.
- Une classe ou une maquette ouverte à une filière qui ne couvre pas son niveau est refusée. Exemple : une maquette d'Ing 1 ouverte à ISI.
- Les pourcentages CC, TP et SN d'un enseignement doivent totaliser 100.
- Les crédits d'une `MaquetteUE` sont la somme de ses enseignements, et ne sont pas stockés.
- Le même EC ne peut pas figurer deux fois dans une `MaquetteUE`.
- Niveau suivant : `Ing 2 → Ing 3` ; L3 diplômant avec suivant M1 ; Ing 5 diplômant sans suivant.
- Isolation : le référentiel de SJM est invisible depuis SJI.

## 12. Questions ouvertes

1. CU01 n'a pas de description détaillée dans le cahier d'analyse : le déroulé ci-dessus est à valider.
2. Un même niveau peut-il mener à deux niveaux différents (par exemple L3 vers deux Masters distincts) ? Le modèle actuel n'en prévoit qu'un (multiplicité 0..1).
3. `TypeEvaluation`, `SessionEvaluation` et `Evaluation` ne sont attribuées à aucun module dans le tableau 5 : ce fichier les rattache à M3.
4. Structure exacte des tables de Saint Jean à reprendre dans les modèles d'import (`enseignement`, `type_evaluation`, `evaluation`) : il faut des exports anonymisés.
