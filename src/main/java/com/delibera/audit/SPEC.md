# M9 – Audit : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.1 BF15, BNF03, BNF13 ; §4.2.5 ; §4.2.6), cahier de conception (version en ligne de fin novembre 2026 : §2.1, §5.2, §6.1.7, §6.1.8, algorithmes A1 et A8), cahier des charges technique (BF15, BNF03, BNF13), Project Scope (§15, T08).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Tenir un **journal non effaçable** de toutes les modifications, connexions et refus d'accès, et permettre sa consultation filtrée par l'auditeur et l'administrateur d'école.

Paquetage : `com.delibera.audit`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF15 | Historiser les règles, paramètres, notes, relèvements de notes et décisions : utilisateur, date, ancienne et nouvelle valeur. |
| BNF03 | Toute modification de note, de paramètre, de règle ou de décision, et tout relèvement, est journalisé et **non effaçable**. |
| BNF13 | Tout refus d'accès (403) est journalisé, ainsi que chaque connexion réussie ou échouée. |
| Project Scope §15, T08 | Une décision manuelle du jury conserve l'utilisateur, la date, l'ancienne proposition et la nouvelle décision. Une délibération clôturée ne se modifie que par une procédure tracée. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU11 Consulter le journal d'audit | Administrateur d'école, auditeur | `AUDIT_CONSULTER` (lecture seule) |
| Écrire dans le journal | Tous les modules, par `AuditService` | Aucune permission : appel interne seulement |

## 4. Cas d'utilisation – CU11 Consulter le journal d'audit

Le cahier d'analyse ne détaille pas CU11. Déroulé proposé **[Proposition]** :

1. L'utilisateur ouvre le journal de **son école**.
2. Il filtre par période, utilisateur, action, entité ou identifiant d'objet.
3. Il consulte chaque entrée : date, auteur, adresse IP, action, objet, ancienne et nouvelle valeur.
4. Il exporte le résultat filtré (CSV ou XLSX) **[Proposition]**.

Aucune entrée ne peut être modifiée ni supprimée, par aucun acteur, y compris par l'application.

## 5. Modèle de données

### JournalAudit

Hérite de `EntiteBase`, **et non** de `EntiteEcole` : l'école est facultative (actions de l'administrateur de la plateforme, connexions échouées sans compte identifié).

| Attribut | Type | Règle |
| --- | --- | --- |
| `action` | `String` | Code de l'action, par exemple `REFUS_ACCES` ou `PRE_DELIBERATION` |
| `entite` | `String` | Nom de l'entité visée, par exemple `Deliberation` |
| `entiteId` | `Long` | |
| `ancienneValeur` | `String` | Représentation JSON de l'état avant |
| `nouvelleValeur` | `String` | Représentation JSON de l'état après |
| `dateAction` | `LocalDateTime` | UTC |
| `adresseIP` | `String` | |
| `ecoleId` | `Long` | Nul pour les actions de la plateforme |

Association : déclenché par un `Utilisateur` (0..1, M1).

**Le filtre d'isolation ne s'applique pas automatiquement à cette entité** : toute lecture filtre explicitement `ecoleId` sur l'école de l'utilisateur. L'administrateur de la plateforme ne voit que les entrées sans école **[Proposition]**.

## 6. Règles techniques

- **Non effaçable** : aucune méthode de modification ou de suppression dans le dépôt. Au niveau de la base, le rôle PostgreSQL de l'application n'a que `INSERT` et `SELECT` sur la table `journal_audit`, ou un déclencheur refuse `UPDATE` et `DELETE` **[Proposition]**.
- **Même transaction pour les modifications** : l'entrée est écrite dans la transaction de la modification. Si celle-ci est annulée, l'entrée l'est aussi.
- **Transaction séparée (`REQUIRES_NEW`) pour les refus d'accès et les échecs de connexion**, qui doivent rester tracés même si la requête échoue **[Proposition]**.
- **Contenu** : jamais de mot de passe, de jeton, de secret TOTP ni d'empreinte de mot de passe dans `ancienneValeur` ou `nouvelleValeur`. Les notes peuvent y figurer : BNF03 l'exige.
- **Index** : `(ecole_id, date_action)`, `(entite, entite_id)`, `action`.
- **Volume** : une entrée par modification. Pas de journalisation des lectures, sauf les refus.

## 7. Interfaces de service (cahier de conception, §6.1.8)

```java
public interface AuditService {
    void journaliser(String action, String entite, Long entiteId, Object avant, Object apres);
}
```

- Les modules l'appellent depuis leurs services : l'interface `IJournal` du diagramme de composants.
- L'utilisateur, l'école et l'adresse IP sont pris dans le contexte de la requête (Spring Security et `ContexteEcole`), jamais passés par l'appelant.

**[Proposition]** À ajouter :
- `journaliserHorsTransaction(...)` : pour A8 et les connexions ;
- `rechercher(FiltreAudit filtre, Pageable page) : Page<EntreeAudit>` : pour CU11.

## 8. Catalogue des actions

Les noms `PRE_DELIBERATION` et `REFUS_ACCES` viennent du cahier de conception. Les autres sont une **[Proposition]**, détaillée dans la section « Journal d'audit » du `SPEC.md` de chaque module :

| Module | Actions |
| --- | --- |
| M1 | `CONNEXION_REUSSIE`, `CONNEXION_ECHOUEE`, `COMPTE_VERROUILLE`, `DECONNEXION`, `REFUS_ACCES`, `COMPTE_*`, `ROLE_*`, `MOT_DE_PASSE_*` |
| M2 | `ECOLE_CREEE`, `ECOLE_MODIFIEE`, `ECOLE_SUSPENDUE`, `ECOLE_REACTIVEE` |
| M3 | `REFERENTIEL_<ENTITE>_<ACTION>` |
| M4 | `REGLE_*`, `PARAMETRES_*` |
| M5 | `IMPORT_VALIDE`, `NOTE_REMPLACEE` |
| M7 | `PRE_DELIBERATION`, `DECISION_JURY`, `RELEVEMENT_NOTE`, `DELIBERATION_CLOTUREE`, `REOUVERTURE`, `RECLAMATION_*` |
| M8 | `PV_GENERE`, `PV_SIGNE` |

Un catalogue centralisé (une énumération `ActionAudit` dans ce module) évite les fautes de frappe **[Proposition]**.

## 9. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| GET | `/api/v1/journal-audit?du=&au=&utilisateurId=&action=&entite=&entiteId=` (paginé) | `AUDIT_CONSULTER` |
| GET | `/api/v1/journal-audit/export` | `AUDIT_CONSULTER` |

Aucune route d'écriture, de modification ou de suppression.

## 10. Dépendances

| Vers | Pour |
| --- | --- |
| M1 Sécurité | Utilisateur courant |

Appelé par : tous les modules.

## 11. Tests d'acceptation

- Chaque action listée produit une entrée avec l'auteur, la date, l'adresse IP, l'avant et l'après.
- Un refus 403 est journalisé, même si la requête échoue.
- Une connexion échouée est journalisée.
- **T08** : une décision du jury différente de la proposition laisse une entrée avec l'ancienne proposition et la nouvelle décision.
- Une modification annulée (exception) ne laisse pas d'entrée de modification.
- Tentative de mise à jour ou de suppression d'une entrée : impossible, au niveau du dépôt comme de la base.
- Isolation : l'auditeur de SJI ne voit aucune entrée de SJM.
- Aucun mot de passe ni jeton dans les entrées.

## 12. Questions ouvertes

1. Durée de conservation du journal, au regard de la loi n° 2024/017.
2. Faut-il un export du journal pour l'auditeur externe (tutelle, inspection) ?
3. Mise en œuvre de la non-effaçabilité en base (droits du rôle PostgreSQL ou déclencheur).
