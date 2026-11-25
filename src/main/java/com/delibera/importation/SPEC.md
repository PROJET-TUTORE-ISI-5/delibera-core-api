# M5 – Import : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.4 CU03 et CU07, §4.1, §4.2.2, §4.2.4, §4.2.7, §5.5, §5.9), cahier de conception (version en ligne de fin novembre 2026 : §2.2, §2.3, §6.1.3, §6.1.5, §6.1.8), cahier des charges technique (BF05, BF11, BNF04, BNF11).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Seule porte d'entrée des données dans la plateforme, qui ne se connecte jamais au système d'information des écoles (BNF11). Le module :
- lit les fichiers Excel (`.xlsx`) ou CSV (UTF-8) selon des modèles téléchargeables ;
- propose la correspondance des colonnes et contrôle la cohérence de chaque ligne ;
- produit un rapport d'anomalies ;
- enregistre les données avec leur provenance (`ImportFichier`), **sans jamais écraser une note**.

Il gère les étudiants et leurs inscriptions, et les notes de la session normale et du rattrapage.

Paquetage : `com.delibera.importation`. Le nom n'est pas `import`, mot réservé de Java.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF05 | Importer des fichiers Excel ou CSV selon des modèles standard : étudiants et inscriptions annuelles, maquette de l'année (UE, EC, crédits), inscriptions aux enseignements, notes par évaluation avec leur statut, acquis des années antérieures (2 à 3 ans) et équivalences. Écran de correspondance des colonnes et rapport d'anomalies. |
| BF11 | Importer les résultats de rattrapage, avant la délibération. |
| BNF04 | Les notes d'origine ne sont jamais écrasées. |
| BNF11 | Aucune connexion au système de l'école ; modèles d'import documentés, compatibles avec un export de la base existante (tables `data`, `evaluation`, `type_evaluation`, `enseignement`). |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU03 Importer les données | Gestionnaire académique | `DONNEES_IMPORTER` (périmètre : le niveau ou le cycle des données) |
| CU07 Saisir les résultats de rattrapage | Gestionnaire académique | `RATTRAPAGE_SAISIR` (périmètre : le niveau) |
| Consulter étudiants et inscriptions | Gestionnaire, jury, auditeur | `DOSSIER_CONSULTER` **[Proposition]** |

## 4. Cas d'utilisation

### CU03 – Importer les données

- **Préconditions** : l'année académique est `OUVERTE`.
- **Scénario principal** :
  1. Le gestionnaire choisit le type de données et l'année, et télécharge au besoin le modèle.
  2. Il téléverse un fichier `.xlsx` ou `.csv`.
  3. Le système propose la correspondance entre les colonnes du fichier et le modèle (**aperçu**). Le gestionnaire la valide.
  4. Le système contrôle la cohérence de chaque ligne (section 7), enregistre les lignes valides avec la référence du fichier, et affiche le nombre de lignes importées et d'anomalies.
- **Scénarios alternatifs** :
  - Anomalies : le système les liste ; le gestionnaire corrige le fichier ou exclut les lignes.
  - Fichier d'une année antérieure : les données sont rattachées à cette année, pour les crédits cumulés.
  - Format non reconnu : refus, avec proposition du modèle à télécharger.
  - Même fichier déjà importé (même empreinte SHA-256) : signalé au gestionnaire.
- **Postcondition** : données enregistrées avec leur fichier source ; aucune note écrasée ; rapport disponible.

### CU07 – Saisir les résultats de rattrapage

- **Préconditions** :
  - les notes de la session normale sont importées ;
  - l'école a organisé le rattrapage et en a établi la liste, **hors plateforme** ;
  - la délibération du niveau n'est **pas encore en séance**.
- **Scénario principal** : le gestionnaire choisit l'année, le niveau et la session de rattrapage, puis téléverse le fichier des notes RAT (type `NOTES_RATTRAPAGE`). Le système contrôle chaque ligne comme pour CU03, enregistre les notes dans la session de rattrapage et affiche le rapport.
- **Scénarios alternatifs** :
  - Note RAT sur un EC déjà validé en session normale : ligne signalée ; le gestionnaire la confirme ou l'exclut.
  - Pré-délibération déjà lancée : elle doit être relancée (M7) pour intégrer ces notes.
  - Délibération en séance ou clôturée : **import refusé**. Une correction passe par une réclamation (CU14).
- **Effet au calcul** : à la pré-délibération, chaque note RAT remplace la SN de son EC (mode `REMPLACE_SN`, algorithme A3).

## 5. Types de données importables

| `TypeDonneesImport` | Contenu | Entités alimentées |
| --- | --- | --- |
| `ETUDIANTS_INSCRIPTIONS` | Étudiants et inscriptions annuelles : matricule, nom, prénom, niveau, filière, spécialité, classe, statut | `Etudiant`, `InscriptionAnnuelle` |
| `MAQUETTE` | UE, EC, crédits, types d'évaluation et pourcentages de l'année | `UE`, `EC`, `MaquetteUE`, `Enseignement`, `TypeEvaluation` (M3) |
| `INSCRIPTIONS_PEDAGOGIQUES` | Inscription de chaque étudiant aux enseignements (motif `NORMAL` ou `DETTE`) | `InscriptionPedagogique` |
| `NOTES` | Notes de la session normale (CC, TP, SN) par évaluation, avec leur statut | `Evaluation` (M3), `Note` |
| `NOTES_RATTRAPAGE` | Notes RAT de la session de rattrapage (CU07) | `Evaluation` (M3), `Note` |
| `ACQUIS` | EC acquis les années antérieures, et équivalences accordées | `AcquisEC` |

Les colonnes reprennent la structure des tables de Saint Jean (`data`, `evaluation`, `type_evaluation`, `enseignement`), pour qu'un simple export suffise. Les codes métier (`matricule`, `codeUE`, `codeEC`, codes de niveau et de filière) servent de clés de rattachement. Le format exact des modèles reste à fixer à partir d'exports anonymisés.

## 6. Modèle de données

Toutes les entités héritent de `EntiteEcole`.

### ImportFichier

| Attribut | Type | Règle |
| --- | --- | --- |
| `nomFichier` | `String` | |
| `typeDonnees` | `TypeDonneesImport` | |
| `dateImport` | `LocalDateTime` | |
| `nbLignes`, `nbAnomalies` | `int` | |
| `empreinteSha256` | `String` | Signale un second import du même fichier |

Méthodes : `valider() : RapportImport`, `rapport() : RapportImport`.

Il concerne une `AnneeAcademique`. Il alimente (0..1 – 0..*) : `Etudiant`, `InscriptionAnnuelle`, `InscriptionPedagogique`, `Note`, `AcquisEC`, `MaquetteUE`.

### Etudiant

`matricule : String` (unique dans l'école), `nom : String`, `prenom : String`, `getNomComplet() : String`. Il est accueilli par une `Ecole` et n'a pas de lien direct à une classe.

### InscriptionAnnuelle

| Attribut | Type |
| --- | --- |
| `dateInscription` | `LocalDate` |
| `statut` | `StatutInscription` (`NOUVEAU`, `PASSANT`, `REDOUBLANT`) |

Méthode : `getEnseignementsSuivis() : List<Enseignement>`.

Associations :
- effectuée par un étudiant ;
- concerne une année ;
- place en un niveau ;
- rattache à une filière ;
- oriente éventuellement vers une spécialité et affecte éventuellement à une classe ;
- comprend des `InscriptionPedagogique` (composition, 1..*).

**Une seule inscription annuelle par étudiant et par année.** Elle définit la population de chaque délibération.

### InscriptionPedagogique

`motif : MotifInscriptionEC` (`NORMAL`, `DETTE`), `getNotesActives() : List<Note>`. Elle porte sur un `Enseignement`, reçoit des `Note` et peut rattraper une `Dette` (M6).

### Note

| Attribut | Type | Règle |
| --- | --- | --- |
| `valeur` | `BigDecimal` | Entre 0 et 20, deux décimales ; nulle si le statut n'est pas `PRESENT` **[Proposition]** |
| `statut` | `StatutNote` | `PRESENT`, `ABSENT`, `ABSENT_JUSTIFIE`, `FRAUDE`, `DISPENSE` |
| `dateSaisie` | `LocalDateTime` | |
| `origine` | `OrigineNote` | `IMPORT` ou `SAISIE` |
| `active` | `boolean` | Une seule note active par inscription et par évaluation |

Méthode : `remplacerPar(Note nouvelle)` désactive l'ancienne note et la relie à la nouvelle. **Une note n'est jamais modifiée ni supprimée.**

Associations : reçue par une `InscriptionPedagogique` ; produite par une `Evaluation` ; « remplacer » (réflexive, 0..1) ; alimentée par un `ImportFichier`.

### AcquisEC

`credits : int`, `origine : OrigineAcquis` (`DELIBERATION`, `IMPORT_HISTORIQUE`, `EQUIVALENCE`), `dateAcquisition : LocalDate`.

- Détenu par un étudiant, il porte sur un EC et date d'une année.
- Il est accordé par une délibération (M7, à la clôture) ou alimenté par un import.
- **Unique par étudiant et par EC** : un EC n'est crédité qu'une fois (T10).

### Énumérations

| Énumération | Valeurs |
| --- | --- |
| `TypeDonneesImport` | `ETUDIANTS_INSCRIPTIONS`, `MAQUETTE`, `INSCRIPTIONS_PEDAGOGIQUES`, `NOTES`, `NOTES_RATTRAPAGE`, `ACQUIS` |
| `StatutInscription` | `NOUVEAU`, `PASSANT`, `REDOUBLANT` |
| `MotifInscriptionEC` | `NORMAL`, `DETTE` |
| `StatutNote` | `PRESENT`, `ABSENT`, `ABSENT_JUSTIFIE`, `FRAUDE`, `DISPENSE` |
| `OrigineNote` | `IMPORT`, `SAISIE` |
| `OrigineAcquis` | `DELIBERATION`, `IMPORT_HISTORIQUE`, `EQUIVALENCE` |

## 7. Contrôles de cohérence (une anomalie par ligne fautive)

**Rattachement et inscription**
- L'étudiant, l'EC, l'UE, le niveau et la filière doivent exister dans l'école : codes connus.
- La filière de l'inscription annuelle couvre son niveau, et la spécialité appartient à cette filière.
- L'étudiant doit être inscrit à l'enseignement de la note.

**Valeurs**
- La note est comprise entre 0 et 20, sinon elle doit avoir un statut d'absence.
- Le type d'évaluation et la session sont reconnus. Une note RAT n'appartient qu'à une session de rattrapage, et réciproquement.
- Une UE ne peut pas avoir de crédits nuls (maquette).

**Fichier**
- Pas de doublon dans le fichier : même étudiant et même évaluation.

Contraintes d'intégrité de l'analyse (§4.2.2, §4.2.7) :

```
context InscriptionAnnuelle inv:
  self.filiere.niveau->includes(self.niveau)
  and (self.specialite->notEmpty() implies self.specialite.filiere = self.filiere)

context InscriptionAnnuelle inv memeEcole:
  self.anneeAcademique.ecole = self.etudiant.ecole and self.niveau.cycle.ecole = self.etudiant.ecole

context InscriptionPedagogique inv inscriptionCoherente:
  let ia = self.inscriptionAnnuelle, m = self.enseignement.maquetteUE in
  m.semestre.anneeAcademique = ia.anneeAcademique
  and (self.motif = MotifInscriptionEC::NORMAL implies
         m.niveau = ia.niveau and m.filiere->includes(ia.filiere)
         and (m.specialite->notEmpty() implies m.specialite->includes(ia.specialite)))
  and (self.motif = MotifInscriptionEC::DETTE implies
         self.dette->notEmpty() and self.dette.ec = self.enseignement.ec
         and self.dette.situationAcademique.inscriptionAnnuelle.etudiant = ia.etudiant)

context Note inv:
  self.evaluation.typeEvaluation.enseignement = self.inscriptionPedagogique.enseignement

context InscriptionAnnuelle inv: self.inscriptionPedagogique->isUnique(enseignement)
context InscriptionPedagogique inv: self.note->select(n | n.active)->isUnique(evaluation)
context Note inv:
  self.noteRemplacee->notEmpty() implies
    self.noteRemplacee.evaluation = self.evaluation
    and self.noteRemplacee.inscriptionPedagogique = self.inscriptionPedagogique
    and not self.noteRemplacee.active
```

## 8. Interfaces de service (cahier de conception, §6.1.8)

```java
// CU03 ; CU07 avec le type NOTES_RATTRAPAGE
public interface ImportService {
    ApercuImport analyser(MultipartFile fichier, TypeDonneesImport type, Long anneeId);
    RapportImport importer(Long importId, Map<String, String> colonnes, Set<Integer> lignesExclues);
}
```

- `ApercuImport` (`record`) : identifiant de l'import, colonnes détectées, correspondance proposée, premières lignes, anomalies déjà détectées, empreinte déjà connue ou non.
- `RapportImport` (`record`) : lignes lues, importées et exclues, liste des anomalies (ligne, colonne, code, message).
- Le fichier téléversé reste en attente entre `analyser` et `importer`. Sa durée de conservation, et l'endroit où il est stocké, restent à préciser **[Proposition]**.
- Tout l'import d'un fichier est **atomique** : soit toutes les lignes retenues sont enregistrées, soit aucune.
- Lecture : Apache POI 5 pour `.xlsx`, Apache Commons CSV pour `.csv` (UTF-8). Ces dépendances sont à ajouter au `pom.xml` avec l'accord de l'équipe.

**[Proposition]** Service de lecture utilisé par M6 et M7 :

```java
public interface InscriptionService {
    List<InscriptionAnnuelle> inscriptions(Long anneeId, Long niveauId);   // avec IP, notes actives (A1)
    List<AcquisEC> acquis(Collection<Long> etudiantIds);
    void enregistrerAcquis(List<AcquisEC> acquis);                       // appelé par M7 à la clôture
}
```

## 9. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| GET | `/api/v1/imports/modeles/{type}` | `DONNEES_IMPORTER` ou `RATTRAPAGE_SAISIR` |
| POST | `/api/v1/imports` (multipart : `fichier`, `type`, `anneeId`) → aperçu | `DONNEES_IMPORTER` (`RATTRAPAGE_SAISIR` pour `NOTES_RATTRAPAGE`) |
| POST | `/api/v1/imports/{id}/validation` (correspondance, lignes exclues) → rapport | Idem |
| GET | `/api/v1/imports`, `/api/v1/imports/{id}` | Idem |
| GET | `/api/v1/etudiants`, `/api/v1/etudiants/{id}` | `DOSSIER_CONSULTER` |
| GET | `/api/v1/inscriptions-annuelles?anneeId=&niveauId=` | `DOSSIER_CONSULTER` |

Taille maximale d'un fichier : 10 Mo (`spring.servlet.multipart` dans `application.yml`).

## 10. Dépendances

| Vers | Pour |
| --- | --- |
| M3 Référentiel | Résoudre les codes (niveaux, filières, UE, EC, évaluations, sessions) ; créer la maquette importée |
| M7 Délibération | Savoir si la délibération du niveau est en séance ou clôturée (refus de CU07) **[Proposition]** |
| M9 Audit | Chaque import, et chaque note remplacée |

Appelé par : M6 (inscriptions, notes, acquis : lecture) et M7 (population de la délibération ; acquis créés à la clôture).

## 11. Journal d'audit

`IMPORT_ANALYSE`, `IMPORT_VALIDE` (type, fichier, empreinte, lignes et anomalies), `NOTE_REMPLACEE` (ancienne et nouvelle valeur, BNF03), `IMPORT_REFUSE` **[Proposition]**.

## 12. Tests d'acceptation

- Import valide d'un fichier `.xlsx` puis d'un `.csv` équivalent : mêmes données enregistrées, provenance conservée.
- Anomalies :
  - étudiant inconnu ;
  - étudiant non inscrit à l'enseignement ;
  - EC inconnu ;
  - note hors de 0 à 20 sans statut ;
  - filière qui ne couvre pas le niveau ;
  - note RAT dans une session normale.
- Lignes exclues : non enregistrées ; les autres le sont.
- Second import d'une note sur la même évaluation : l'ancienne devient inactive et reste en base, reliée à la nouvelle. Jamais d'écrasement.
- Second import du même fichier : signalé par l'empreinte.
- CU07 :
  - délibération en séance : import refusé ;
  - note RAT sur un EC validé en session normale : ligne signalée.
- Une seule inscription annuelle par étudiant et par année.
- Un `AcquisEC` déjà présent pour un étudiant et un EC n'est pas dupliqué (T10).
- Isolation : un import de SJI ne peut rattacher aucune ligne à un objet de SJM, même avec un code identique.

## 13. Questions ouvertes

1. Exports anonymisés des tables de SJI (`data`, `evaluation`, `type_evaluation`, `enseignement`), pour fixer les colonnes des modèles.
2. Où et combien de temps conserver le fichier téléversé entre l'aperçu et la validation ?
3. Une note corrigée par import après une délibération clôturée : refusée hors réouverture (CU14) ?
4. Saisie manuelle de notes (`OrigineNote.SAISIE`) : quel cas d'utilisation la permet ?
