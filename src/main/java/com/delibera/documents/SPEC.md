# M8 – Documents et statistiques : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.1 BF13 et BF14, §3.4 CU08, §4.2.5, §5.10), cahier de conception (version en ligne de fin novembre 2026 : §2.2, §2.3, §4.2.1, §5.2, §6.1.6, §6.1.8), cahier des charges technique (BF13, BF14, BNF06).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Seul module qui produit des documents. Il génère :
- le procès-verbal (PV) de délibération, au **modèle officiel de chaque école** ;
- les listes par décision ;
- les relevés de situation ;
- les statistiques par niveau, filière et année, avec leurs exports.

Paquetage : `com.delibera.documents`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF13 | Générer le PV **pendant la séance**, selon les modèles existants, avec la raison de chaque décision du jury, prêt à signer. Une régénération produit un **nouveau PV sans supprimer le précédent** ; le signataire et la date de signature sont enregistrés. Listes par décision dès la clôture. |
| BF14 | Statistiques : taux d'admission, répartition des décisions par niveau, formation et année. |
| BNF06 | Génération du PV en moins de 5 s. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU09 Générer le PV et les listes | Gestionnaire académique, président du jury | `PV_GENERER` (le président seulement pour sa délibération) |
| Enregistrer la signature du PV | Président du jury | `PV_GENERER` **[Proposition]** |
| CU10 Consulter les statistiques | Administrateur d'école, gestionnaire, président, auditeur | `STATS_CONSULTER` |

## 4. Cas d'utilisation

Le cahier d'analyse ne détaille ni CU09 ni CU10. Déroulés déduits de BF13, BF14 et de la séquence de CU08 **[Proposition]**.

### CU09 – Générer le PV et les listes

1. À la validation (CU08), M7 demande le PV. M8 le génère en PDF avec le modèle de l'école.
2. Le PV contient :
   - école, année, niveau ;
   - composition du jury (président et membres) ;
   - pour chaque étudiant, le résultat retenu et, pour chaque décision du jury, son motif ;
   - les relèvements de note ;
   - la version des règles appliquées.
3. Le PV reçoit un numéro unique dans l'école et son empreinte SHA-256.
4. Le président signe sur place. Le système enregistre le signataire et la date de signature. Un PV signé ne se modifie plus : son empreinte le prouve.
5. Listes par décision (`ADMIS`, `ADMIS_APRES_RATTRAPAGE`, `SOUS_CONDITION`, `AJOURNE`, `BLOQUE_POUR_DETTE`) en PDF et XLSX, dès la clôture.
6. Après une réouverture (CU14), un nouveau PV est généré ; **l'ancien reste conservé**.

### CU10 – Consulter les statistiques

- Taux d'admission et répartition des résultats retenus, par niveau, filière et année, avec des filtres.
- Export XLSX.
- Sur une délibération en cours, les statistiques sont **provisoires** et signalées comme telles (séquence de CU04).

## 5. Modèle de données

### ProcesVerbal (hérite de `EntiteEcole`)

| Attribut | Type | Règle |
| --- | --- | --- |
| `numero` | `String` | Unique dans l'école |
| `dateGeneration` | `LocalDateTime` | |
| `dateSignature` | `LocalDateTime` | Nulle tant que non signé |
| `cheminFichier` | `String` | Emplacement du PDF dans le stockage des documents |
| `empreinteSha256` | `String` | Empreinte du PDF : preuve qu'un PV signé n'a pas été modifié |

Méthodes : `signer(MembreJury signataire, LocalDateTime date)`, `estSigne() : boolean`.

Associations : consigne une `Deliberation` (M7, 0..* PV par délibération) ; signé par un `MembreJury` (0..1).

### Types non persistés (`record`)

`Document` (nom, type MIME, contenu ou emplacement), `Statistiques`, `FiltreStatistiques` (année, niveau, filière, délibération).

## 6. Règles techniques

- **JasperReports**, avec **un modèle (`.jrxml`) par école**, versionné dans Git (gestion de configuration). Les modèles reproduisent la mise en page officielle de chaque école (référence R7). Exports XLSX avec Apache POI. Ces dépendances sont à ajouter au `pom.xml` avec l'accord de l'équipe.
- **Stockage des PDF** hors de la base, dans un volume sauvegardé chaque nuit avec la base. Chemin relatif à l'école et à la délibération **[Proposition]**.
- **Un PV signé est immuable.** Une correction produit un nouveau PV.
- **Contenu** : aucune donnée inutile. Pas de mot de passe ni de jeton ; les documents ne contiennent que les identités et résultats nécessaires.
- **Performance** : le PV se génère en moins de 5 s, à partir des données déjà calculées. Aucun recalcul dans M8.

## 7. Interfaces de service (cahier de conception, §6.1.8)

```java
public interface DocumentService {
    ProcesVerbal genererPV(Long deliberationId);
    List<Document> genererListes(Long deliberationId);
    Statistiques statistiques(FiltreStatistiques filtre);
}
```

**[Proposition]** À ajouter : `signerPV(Long pvId, LocalDateTime date)`, `telechargerPV(Long pvId)`, `releveSituation(Long situationId)`, `exporterStatistiques(FiltreStatistiques filtre)`.

## 8. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| POST | `/api/v1/deliberations/{id}/proces-verbaux` | `PV_GENERER` |
| GET | `/api/v1/deliberations/{id}/proces-verbaux` | `PV_GENERER` ou `DOSSIER_CONSULTER` |
| GET | `/api/v1/proces-verbaux/{id}/fichier` (PDF) | Idem |
| POST | `/api/v1/proces-verbaux/{id}/signature` | `PV_GENERER` (président de la délibération) |
| GET | `/api/v1/deliberations/{id}/listes?etat=&format=pdf\|xlsx` | `PV_GENERER` |
| GET | `/api/v1/situations/{id}/releve` (PDF) | `DOSSIER_CONSULTER` |
| GET | `/api/v1/statistiques?anneeId=&niveauId=&filiereId=` | `STATS_CONSULTER` |
| GET | `/api/v1/statistiques/export` (XLSX) | `STATS_CONSULTER` |

## 9. Dépendances

| Vers | Pour |
| --- | --- |
| M7 Délibération | Données à mettre en forme : délibération, jury, situations, décisions, relèvements. **Dépendance circulaire avec M7** (question 3). |
| M3 Référentiel | Libellés des niveaux, filières, UE et EC |
| M9 Audit | Génération et signature des PV |

Appelé par : M7 (à la validation et après une réouverture).

## 10. Journal d'audit

`PV_GENERE` (numéro, empreinte), `PV_SIGNE` (signataire, date), `LISTES_GENEREES`, `STATISTIQUES_EXPORTEES` **[Proposition]**.

## 11. Tests d'acceptation

- Le PV contient la composition du jury et le motif de chaque décision du jury.
- Régénération : un nouveau numéro, et l'ancien PV reste disponible.
- Signature : signataire et date enregistrés. Un PV signé ne peut plus être modifié, et son empreinte correspond au fichier.
- Génération en moins de 5 s pour une délibération de 500 étudiants.
- Les listes par décision correspondent aux résultats retenus de la version en vigueur.
- Statistiques : taux d'admission juste sur un jeu de données connu ; filtres par niveau, filière et année.
- Isolation : PV et statistiques d'une autre école inaccessibles.

## 12. Questions ouvertes

1. **Modèles officiels de PV de SJI et SJM** (référence R7) : à obtenir auprès des scolarités. Ils sont indispensables pour les modèles JasperReports.
2. Format du numéro de PV : par exemple `SJI-2026-ING1-001` ?
3. Pour éviter la dépendance circulaire avec M7 : M7 transmet-il un `record` avec toutes les données du PV, au lieu de l'identifiant de la délibération ?
4. Liste exacte des indicateurs statistiques attendus par les écoles, au-delà du taux d'admission et de la répartition des décisions.
