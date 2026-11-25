# M6 – Moteur de calcul : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.4 CU04 à CU06, §4.1, §4.2.4, §4.2.7, §4.3), cahier de conception (version en ligne de fin novembre 2026 : §4.2.2, §6.1.5, §6.1.8, algorithmes A3 à A7), cahier des charges technique (BF06 à BF10, BNF01, BNF04, BNF06), Project Scope (§12, §18, T01 à T10).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Cœur du système. Pour un étudiant, le module calcule :
- la note de chaque EC ;
- le résultat de chaque UE, les crédits annuels et cumulés, et les dettes EC par EC ;
- l'évaluation de la règle de passage et la proposition de décision ;
- pour un cas critique, les options de relèvement et les UE prioritaires.

Il **calcule en mémoire, sans accès à la base** : M7 charge les données et enregistre les résultats.

Paquetage : `com.delibera.calcul`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF06 | Note de chaque EC selon la pondération des évaluations et la règle de rattrapage. UE validée si sa moyenne atteint la note de validation **et** qu'aucun EC n'est sous la note éliminatoire. |
| BF07 | UE validée : tous ses crédits. UE non validée : crédits de ses EC validés (capitalisation). Crédits annuels et cumulés, chaque EC n'étant crédité qu'une fois. |
| BF08 | Dettes EC par EC, avec leur année et leur niveau d'origine, reliées à la reprise de l'EC l'année suivante. |
| BF09 | Proposer un état avec la règle la plus précise. |
| BF10 | Cas critiques entre le seuil limite et le seuil, avec les notes d'EC à relever (jusqu'à la note de validation, ou jusqu'au minimum non éliminatoire). **Priorité n° 1 du projet.** |
| BNF01 | Résultats conformes aux tests T01 à T10 et aux cas réels de SJI. |
| BNF04 | Un EC crédité une seule fois ; chaque recalcul crée une nouvelle version. |
| BNF06 | 500 étudiants en moins de 30 s ; un recalcul en moins de 5 s. |

## 3. Acteurs et permissions

M6 n'expose **aucune route** et ne vérifie pas de permission : il est appelé par M7, qui contrôle l'accès (`PREDELIB_LANCER`, `DOSSIER_CONSULTER`, `DOSSIER_STATUER`).

## 4. Cas d'utilisation concernés (partie calcul)

- **CU04 Lancer la pré-délibération** : pour chaque étudiant, appliquer dans l'ordre :
  1. note de chaque EC, la note RAT remplaçant la SN ;
  2. EC éliminatoires ;
  3. validation des UE, ou capitalisation des EC ;
  4. crédits annuels, crédits cumulés et dettes ;
  5. règle la plus précise ;
  6. proposition, avec son explication ;
  7. pour un cas critique : options de relèvement et UE prioritaires.

  Cas particuliers : dossier « incomplet » si des notes manquent ; absences traitées selon `traitementAbsence` ; dossier bloqué si aucune règle ne couvre l'étudiant (signalé par M7).
- **CU05 Examiner les cas critiques** : les options de relèvement sont classées, par défaut avec le moins de points ajoutés. Si aucune option ne suffit, la liste est vide et le dossier est signalé.
- **CU06 Statuer sur un dossier** : après un relèvement, recalcul immédiat de la situation dans une **nouvelle version**, à partir de `ResultatEC.noteRetenue()`.

## 5. Modèle de données

Toutes les entités héritent de `EntiteEcole`. Elles sont calculées par M6 et enregistrées par M7 (section 10).

### SituationAcademique

| Attribut | Type | Règle |
| --- | --- | --- |
| `version` | `int` | Nouvelle version à chaque recalcul |
| `enVigueur` | `boolean` | **Une seule version en vigueur** par inscription et par délibération |
| `creditsAnnuels` | `int` | |
| `creditsCumules` | `int` | |
| `detteTotale` | `int` | En crédits |
| `moyenne` | `BigDecimal` | Moyenne des UE pondérée par leurs crédits |
| `dateCalcul` | `LocalDateTime` | |
| `/resultatRetenu` | dérivé | `getResultatRetenu()` : la décision du jury si elle existe, sinon la proposition |

Méthodes : `versContexte() : ContexteEvaluation`, `nouvelleVersion() : SituationAcademique`, `getResultatRetenu() : EtatDecision`.

Associations :
- examinée par une `Deliberation` (composition) ;
- résulte d'une `InscriptionAnnuelle` ;
- une `ReglePassage` s'applique à elle ;
- « remplacer » (version précédente, 0..1) ;
- détaille des `ResultatUE` (composition) ;
- constate des `Dette` (composition) ;
- aboutit à une `PropositionDecision` (composition, 1).

Le verrouillage optimiste (`verrou`) la protège en séance.

### ResultatUE

`moyenne : BigDecimal`, `valide : boolean`, `creditsAcquis : int`, `calculer(ParametreDeliberation p)`. Il porte sur une `MaquetteUE` et agrège des `ResultatEC` (composition, 1..*).

### ResultatEC

| Attribut | Type |
| --- | --- |
| `noteEC` | `BigDecimal` : note calculée ; la note d'origine n'est jamais modifiée |
| `valide` | `boolean` |
| `eliminatoire` | `boolean` |
| `creditsAcquis` | `int` |

Méthodes : `noteRetenue() : BigDecimal` (la note relevée par le jury s'il y en a une, sinon `noteEC`), `calculer(ParametreDeliberation p)`.

Il concerne une `InscriptionPedagogique`. Il peut être visé par des `OptionRelevement` et modifié par des `RelevementNote` (M7).

### Dette

`credits : int`, `estSoldee() : boolean`. Elle porte sur un EC précis, provient d'un niveau d'origine, et est rattrapée par une `InscriptionPedagogique` de motif `DETTE` l'année suivante.

### PropositionDecision

`etatPropose : EtatDecision`, `dateCalcul : LocalDateTime`, `explication : String`. Exemple d'explication : « crédits annuels 42 < 45 ; seuil limite 42 ; règle ING-N1-N2 version 1 ». Elle suggère des `OptionRelevement` et recommande des `UEPrioritaire` (compositions).

### OptionRelevement

`noteActuelle : BigDecimal`, `noteCible : BigDecimal`, `pointsAjoutes : BigDecimal`, `creditsObtenus : int`, `rang : int`. Elle vise un `ResultatEC`.

### UEPrioritaire

`creditsApportes : int`, `rang : int`. Elle désigne une `MaquetteUE`.

### Énumération

| Énumération | Valeurs |
| --- | --- |
| `EtatDecision` | `ADMIS`, `A_DELIBERER`, `AJOURNE`, `BLOQUE_POUR_DETTE`, `ADMIS_APRES_RATTRAPAGE`, `SOUS_CONDITION` (cette dernière n'est **jamais proposée** automatiquement) |

## 6. Règles de calcul

- **Nombres** : `BigDecimal`, deux décimales, `RoundingMode.HALF_UP` **une seule fois**, à la fin du calcul de chaque note d'EC (A3). Moyennes d'UE et moyenne annuelle : même règle **[Proposition]**.
- **Rattrapage** : avec `REMPLACE_SN`, la note RAT remplace la SN **dans tous les cas**, même si elle est plus basse. Avec `MEILLEURE_NOTE`, on garde la meilleure des deux.
- **Note éliminatoire** : un EC dont la note est strictement inférieure à `noteEliminatoire` empêche la validation de son UE.
- **Capitalisation** : une UE validée donne tous ses crédits ; sinon, seuls ses EC validés sont crédités, si `capitalisationEC` est vrai.
- **UE incomplète** (l'étudiant ne suit qu'une partie de ses EC, en reprise de dette) : jamais validée en bloc ; seuls ses EC validés sont crédités.
- **Un EC n'est crédité qu'une fois** (T10) : un EC déjà présent dans `AcquisEC` est ignoré.
- **Dette reprise et validée** : elle augmente les crédits cumulés et disparaît des dettes, **sans compter dans les crédits annuels** du nouveau niveau (à confirmer avec SJI et SJM).
- **Crédits cumulés** : somme des acquis du cycle, plus les nouveaux EC acquis.

### Contraintes d'intégrité

```
context SituationAcademique inv regleAppliquee:
  let r = self.reglePassage, ia = self.inscriptionAnnuelle in
  self.deliberation.reglePassage->includes(r)
  and r.niveau = ia.niveau
  and (r.filiere->notEmpty() implies r.filiere = ia.filiere)
  and (r.specialite->notEmpty() implies r.specialite = ia.specialite)

context SituationAcademique::resultatRetenu : EtatDecision
  derive: let p = self.propositionDecision in
    if p.decisionJury->notEmpty() then p.decisionJury.etat else p.etatPropose endif
```

## 7. Interfaces de service (cahier de conception, §6.1.8)

```java
public interface CalculService {
    SituationAcademique calculerSituation(InscriptionAnnuelle ia, Deliberation d, boolean avecRattrapage);
    ResultatEC calculerNoteEC(InscriptionPedagogique ip, ParametreDeliberation p, boolean avecRattrapage);
    ResultatUE calculerResultatUE(MaquetteUE m, List<ResultatEC> ecs, ParametreDeliberation p);
}
public interface MoteurReglesService {
    PropositionDecision proposer(SituationAcademique s, ReglePassage r, Deliberation d);
}
public interface CasCritiquesService {
    List<OptionRelevement> optionsRelevement(SituationAcademique s, ConditionRegle condition);
    List<UEPrioritaire> uesPrioritaires(SituationAcademique s, ConditionRegle condition);
}
```

Ces trois services **n'injectent aucun dépôt** : toutes leurs données arrivent en paramètre, déjà chargées par M7 (A1). Ils se testent donc par de simples tests unitaires.

## 8. Algorithmes

### A3 – Note d'un EC (`CalculService.calculerNoteEC`)

```text
FONCTION calculerNoteEC(ip, p, avecRattrapage) : ResultatEC
  rat <- SI avecRattrapage ALORS note RAT active de ip SINON NUL
  part <- table vide (type d'evaluation -> note)
  POUR CHAQUE type t de ip.enseignement, t.nature != RAT FAIRE
    SI t.nature = SN ET rat != NUL ET p.modeRattrapage = REMPLACE_SN ALORS
      part[t] <- valeurPourCalcul(rat, p) ; CONTINUER                    // la RAT remplace la SN dans tous les cas
    FIN SI
    valeurs <- [valeurPourCalcul(n, p) POUR n DANS les notes actives de ip pour le type t]
    SI valeurs contient INCOMPLET ALORS RETOURNER ResultatEC(incomplet)  // dossier signale incomplet
    valeurs <- valeurs sans les DISPENSE
    SI valeurs est vide ALORS CONTINUER                                  // type entierement dispense
    part[t] <- SI t.nature = CC ET p.combinaisonCC = MEILLEURE_NOTE ALORS MAX(valeurs) SINON MOYENNE(valeurs)
    SI t.nature = SN ET rat != NUL ALORS part[t] <- MAX(part[t], valeurPourCalcul(rat, p))   // mode MEILLEURE_NOTE
  FIN POUR
  note <- SOMME(part[t] * t.pourcentage) / SOMME(t.pourcentage POUR t DANS part)
  note <- ARRONDI(note, 2)
  SI le jury a releve cet EC ALORS note <- releve.nouvelleValeur         // la note d'origine reste inchangee
  RETOURNER ResultatEC(note, eliminatoire = note < p.noteEliminatoire, valide = note >= p.noteValidation)
FIN

FONCTION valeurPourCalcul(n, p)
  SELON n.statut
    PRESENT         : RETOURNER n.valeur
    ABSENT          : RETOURNER SI p.traitementAbsence = ZERO ALORS 0 SINON INCOMPLET
    ABSENT_JUSTIFIE : RETOURNER INCOMPLET                                // a confirmer avec les ecoles
    FRAUDE          : RETOURNER 0                                        // a confirmer avec les ecoles
    DISPENSE        : RETOURNER DISPENSE                                 // retiree, poids redistribue
FIN
```

### A4 – Résultats d'UE, crédits et dettes (`CalculService.calculerSituation`)

```text
FONCTION calculerSituation(ia, d, avecRattrapage) : SituationAcademique
  p <- d.parametres
  acquis <- EC deja acquis par l'etudiant (AcquisEC des annees anterieures)
  s <- nouvelle SituationAcademique(ia, d)
  POUR CHAQUE maquette m dont ia suit au moins un enseignement FAIRE
    ecs <- [calculerNoteEC(ip, p, avecRattrapage) POUR ip DE ia DONT l'enseignement est dans m]
    AJOUTER calculerResultatUE(m, ecs, p, complete = ia suit tous les enseignements de m) A s
  FIN POUR
  POUR CHAQUE rec DANS les ResultatEC de s FAIRE
    SI rec.ec DANS acquis ALORS CONTINUER                                // un EC n'est credite qu'une fois (T10)
    SI rec.creditsAcquis > 0 ALORS
      AJOUTER rec.ec A nouveaux
      SI rec.niveau = ia.niveau ALORS s.creditsAnnuels += rec.creditsAcquis   // une dette reprise ne compte qu'au cumul
    SINON
      AJOUTER Dette(rec.ec, rec.credits, niveau d'origine = rec.niveau) A s.dettes
    FIN SI
  FIN POUR
  AJOUTER A s.dettes les dettes anterieures dont l'EC n'est ni acquis ni valide cette annee
  s.creditsCumules <- SOMME(credits des acquis du cycle) + SOMME(credits de nouveaux)
  s.detteTotale <- SOMME(credits de s.dettes)
  s.moyenne <- moyenne des UE ponderee par leurs credits
  RETOURNER s
FIN

FONCTION calculerResultatUE(m, ecs, p, complete) : ResultatUE
  moyenne <- SI p.modeMoyenne = PONDEREE_PAR_CREDITS
             ALORS SOMME(ec.noteRetenue * ec.credits) / SOMME(ec.credits) SINON MOYENNE(ec.noteRetenue)
  valide <- complete ET moyenne >= p.noteValidation ET aucun ec n'est eliminatoire
  POUR CHAQUE ec DANS ecs FAIRE
    SI valide ALORS ec.creditsAcquis <- ec.credits                       // l'UE validee donne tous ses credits
    SINON SI p.capitalisationEC ET ec.valide ALORS ec.creditsAcquis <- ec.credits   // sinon, les EC valides
    SINON ec.creditsAcquis <- 0
  FIN POUR
  RETOURNER ResultatUE(moyenne, valide, SOMME(ec.creditsAcquis))
FIN
```

### A5 – Évaluation de la règle et proposition (`MoteurReglesService.proposer`)

```text
FONCTION proposer(s, r, d) : PropositionDecision
  ctx <- s.versContexte()
  res <- r.evaluer(ctx)               // chaque condition : valeur observee, operateur, valeur ; puis ET ou OU
  SI res.satisfaite ALORS
    sn <- calculerSituation(s.inscription, d, avecRattrapage = FAUX)     // en memoire, non enregistree
    RETOURNER SI r.evaluer(sn.versContexte()).satisfaite ALORS ADMIS     // T01, T05
              SINON ADMIS_APRES_RATTRAPAGE                               // seuil atteint grace au rattrapage (T04)
  FIN SI
  SI une condition c de res.echecs n'est pas derogeable ALORS
    RETOURNER SI c.variable = DETTE_NIVEAU ALORS BLOQUE_POUR_DETTE SINON AJOURNE   // T07
  FIN SI
  POUR CHAQUE c DANS res.echecs FAIRE                                    // tous les echecs sont derogeables
    SI c.variable DANS {CREDITS_ANNUELS, CREDITS_CUMULES}
       ET (c.seuilLimite = NUL OU NON c.atteintSeuilLimite(ctx)) ALORS
      RETOURNER AJOURNE                                                  // sans seuil limite ou en dessous (T02)
    FIN SI
  FIN POUR
  RETOURNER A_DELIBERER                                                  // entre seuil limite et seuil (T03, T06)
FIN
```

Le seuil limite est celui de la condition, dans la version de la règle figée par la délibération. Le second calcul, sans les notes de rattrapage, sert seulement à distinguer `ADMIS` de `ADMIS_APRES_RATTRAPAGE` : il n'est pas enregistré.

### A6 – Options de relèvement (`CasCritiquesService.optionsRelevement`)

```text
FONCTION optionsRelevement(s, c) : liste d'OptionRelevement
  // c : condition de credits derogeable qui a echoue, par exemple credits annuels >= 45
  p <- parametres figes de la deliberation de s
  options <- liste vide
  POUR CHAQUE rec DANS les ResultatEC de s non acquis et du niveau de l'inscription FAIRE
    cibles <- { p.noteValidation }                                       // valider l'EC
    SI rec.eliminatoire ALORS AJOUTER p.noteEliminatoire A cibles        // seulement lever l'elimination
    POUR CHAQUE cible DANS cibles AVEC cible > rec.noteRetenue FAIRE
      copie <- s ou la note de rec vaut cible
      recalculer l'UE de rec puis les credits de copie                   // A4 limite a une UE
      SI c est satisfaite dans copie ALORS
        AJOUTER OptionRelevement(rec, noteActuelle = rec.noteRetenue, noteCible = cible,
                pointsAjoutes = cible - rec.noteRetenue, creditsObtenus = valeur de c dans copie)
      FIN SI
    FIN POUR
  FIN POUR
  TRIER options PAR pointsAjoutes croissants, puis creditsObtenus decroissants   // critere par defaut
  numeroter les rangs 1, 2, 3...
  RETOURNER options
FIN
```

Exemple : Paul a 42 crédits pour un seuil de 45. Relever Algorithmique 2 de 8,5 à 10 (+1,5 point) lui donne 45 crédits : c'est l'option de rang 1. Si aucun EC ne suffit seul, la liste est vide et le dossier est signalé. Le jury peut alors relever plusieurs EC un par un.

### A7 – UE prioritaires (`CasCritiquesService.uesPrioritaires`)

```text
FONCTION uesPrioritaires(s, c) : liste d'UEPrioritaire
  manque <- c.valeur - c.valeurObservee(s.versContexte())                // 45 - 42 = 3
  candidates <- UE non validees du niveau, apport = credits encore non acquis de l'UE,
                TRIEES PAR ecart a la note de validation croissant      // les plus proches d'abord
  meilleure <- NUL
  PROCEDURE explorer(i, choix, somme)
    SI somme >= manque ALORS
      SI meilleure = NUL OU taille(choix) < taille(meilleure)
         OU (taille(choix) = taille(meilleure) ET ecartTotal(choix) < ecartTotal(meilleure)) ALORS
        meilleure <- choix
      FIN SI
      RETOURNER
    FIN SI
    SI i = taille(candidates) OU (meilleure != NUL ET taille(choix) >= taille(meilleure)) ALORS RETOURNER
    explorer(i + 1, choix + candidates[i], somme + candidates[i].apport)
    explorer(i + 1, choix, somme)
  FIN PROCEDURE
  explorer(0, [], 0)
  RETOURNER les UE de meilleure, rang = ordre d'ecart croissant, creditsApportes = apport
FIN
```

La recherche est exponentielle dans le pire cas, mais un étudiant dans la marge a peu d'UE non validées (moins de 15), et l'élagage limite l'exploration. Le jury s'en sert pour choisir les UE exigées d'une décision `SOUS_CONDITION`.

## 9. Performances (BNF06)

- Aucun accès à la base pendant le calcul : M7 charge en amont, par requêtes groupées, maquettes, enseignements, notes actives, acquis et dettes (A1).
- Calcul par lots de 50 étudiants, éventuellement en parallèle. Chaque fil d'exécution doit alors recevoir l'école par `ContexteEcole.executerPour`.
- Objectifs : pré-délibération de 500 étudiants en moins de 30 s ; recalcul après un relèvement en moins de 5 s. Prévoir un test de performance.

## 10. Dépendances

| Vers | Pour |
| --- | --- |
| M4 Règles | `ReglePassage.evaluer`, conditions, `ContexteEvaluation`, paramètres figés |
| M3 Référentiel | Maquettes, enseignements, crédits, types d'évaluation, niveaux |
| M5 Import | Types `InscriptionAnnuelle`, `InscriptionPedagogique`, `Note`, `AcquisEC` (données reçues en paramètre) |

Appelé par : M7 seulement.

**[Proposition]** Les entités de résultat (`SituationAcademique`, `ResultatUE`…) sont définies dans `calcul.domaine`, avec leurs dépôts dans `calcul.depot`. Ces dépôts sont utilisés par un service de persistance distinct (`SituationService`), que seul M7 appelle. Les services de calcul (`CalculService`, `MoteurReglesService`, `CasCritiquesService`) n'y accèdent jamais.

## 11. Journal d'audit

M6 ne journalise rien lui-même : M7 journalise la pré-délibération, les recalculs et les relèvements.

## 12. Tests d'acceptation

Couverture d'au moins 80 %. Tests unitaires purs, sans Spring ni base.

**Jeux de tests du Project Scope (`ScenariosAcceptationTest`)** :

| Test | Situation | Résultat attendu |
| --- | --- | --- |
| T01 | N1 = 45 crédits, seuil 45 | `ADMIS` |
| T02 | N1 = 44 crédits, condition sans seuil limite | `AJOURNE` |
| T03 | N1 = 42 crédits, seuil limite 42 | `A_DELIBERER` |
| T04 | N1 = 42 + un rattrapage de 3 crédits | `ADMIS_APRES_RATTRAPAGE` |
| T05 | N2, cumul = 120, seuil cumulé 120 | `ADMIS` |
| T06 | N2, cumul = 119 | `A_DELIBERER` si un seuil limite ≤ 119 existe, sinon `AJOURNE` |
| T07 | N4 avec une dette en N3, condition `DETTE_NIVEAU = 0` non dérogeable | `BLOQUE_POUR_DETTE` |
| T10 | Même EC acquis l'an passé et validé cette année | Crédité une seule fois |

T08 (décision du jury auditée) et T09 (règle historique identifiable) se testent dans M7 et M4.

**A3** :
- moyenne pondérée CC/TP/SN ;
- RAT qui remplace une SN meilleure (`REMPLACE_SN`) ;
- `MEILLEURE_NOTE` ;
- plusieurs CC (moyenne ou meilleure note) ;
- `ABSENT` avec `ZERO` et avec `NON_EVALUE` ;
- `DISPENSE` : poids redistribué ;
- arrondi `HALF_UP` d'une seule fois : 9,995 → 10,00 ;
- note relevée prise en compte.

**A4** :
- UE validée (tous les crédits) ;
- UE non validée avec capitalisation (crédits des EC validés seulement) ;
- EC éliminatoire qui bloque l'UE malgré une moyenne suffisante ;
- UE incomplète (dette reprise) ;
- dette reprise et validée : cumul augmenté, annuel inchangé.

**A6, A7** :
- exemple de Paul : rang 1 = Algorithmique 2, 8,5 → 10, +1,5, 45 crédits ;
- EC éliminatoire avec deux cibles ;
- aucune option : liste vide ;
- UE prioritaires : moins d'UE possible, puis plus petit écart.

**Cas réels de SJI** : à ajouter dès réception des données anonymisées.

## 13. Questions ouvertes

1. Traitement de `ABSENT_JUSTIFIE` (incomplet ?) et de `FRAUDE` (zéro ?) : à confirmer avec les écoles.
2. Une dette reprise et validée doit-elle compter dans les crédits annuels du nouveau niveau ? (Choix actuel : non, seulement au cumul.)
3. Valeurs de `modeMoyenne`, `combinaisonCC` et `traitementAbsence` pour SJI et SJM.
4. Arrondi des moyennes d'UE et de la moyenne annuelle : même règle que les notes d'EC ?
5. Emplacement des dépôts des entités de résultat (proposition de la section 10).
