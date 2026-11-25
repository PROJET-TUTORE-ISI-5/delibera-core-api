# M2 – Écoles clientes : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.2.2, §3.4 CU15, §4.2.1, §4.2.7), cahier de conception (version en ligne de fin novembre 2026 : §2.1, §2.3, §6.1.1, §6.1.2), cahier des charges technique (BF19, BNF12, BNF13).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

La plateforme est un service partagé (SaaS) : chaque école (SJI, SJM, puis toute autre) est un client autonome. Ce module crée les écoles, les suspend et les réactive, et crée leur premier administrateur. Il fournit aussi le contexte d'école de chaque requête : `ContexteEcole` et le filtre Hibernate, déjà en place dans `commun.isolation`.

Paquetage : `com.delibera.ecole`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF19 | Créer une école cliente et son premier administrateur, la suspendre ou la réactiver. Réservé à l'administrateur de la plateforme, qui n'accède pas aux notes. |
| BNF12 | Les données d'une école ne sont jamais visibles ni modifiables par les utilisateurs d'une autre école. |
| BNF13 | L'administrateur de la plateforme n'accède à aucune donnée académique. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU15 Gérer les écoles clientes | Administrateur de la plateforme (compte sans école) | `ECOLE_GERER` |
| Lire son école (nom, fuseau, double authentification) | Tout utilisateur d'une école | Authentifié **[Proposition]** |

`ECOLE_GERER` n'est jamais accordée à un rôle d'école (CU16).

## 4. Cas d'utilisation – CU15 Gérer les écoles clientes

- **Préconditions** : l'acteur est authentifié comme administrateur de la plateforme.
- **Scénario principal** :
  1. Il crée l'école (code, nom), éventuellement avec son fuseau horaire et l'exigence de double authentification.
  2. Il crée le compte de son administrateur d'école, qui reçoit ses accès par un lien d'activation (M1, M10).
  3. L'école est active : son administrateur peut paramétrer le référentiel et les paramètres (CU01, CU13), et ses gestionnaires saisir les règles de passage (CU02).
- **Scénarios alternatifs** :
  - Code déjà utilisé : refus (`CODE_ECOLE_DEJA_UTILISE`).
  - École suspendue : ses utilisateurs ne peuvent plus se connecter et leurs sessions sont révoquées **[Proposition]**. Ses données sont conservées.
  - École réactivée : ses utilisateurs peuvent de nouveau se connecter.
- **Postcondition** : l'école existe avec son premier administrateur ; ses données sont isolées de celles des autres écoles.

## 5. Modèle de données

### Ecole

Hérite de `EntiteBase`, **et non** de `EntiteEcole` : c'est la racine de l'isolation.

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | **Unique sur toute la plateforme** (exemples : `SJI`, `SJM`) |
| `nom` | `String` | Obligatoire |
| `statut` | `StatutEcole` | `ACTIVE` ou `SUSPENDUE` |
| `dateAdhesion` | `LocalDate` | |
| `doubleAuthentification` | `boolean` | Exige le code TOTP à la connexion de ses utilisateurs |
| `fuseauHoraire` | `String` | Identifiant IANA, par exemple `Africa/Douala` ; sert à afficher les dates |

Méthodes :
- `activer() : void`
- `suspendre(String motif) : void`
- `estActive() : boolean`

Associations, toutes vers d'autres modules : organiser des années (`AnneeAcademique`, M3) ; offrir des cycles (`Cycle`, M3) ; définir les catalogues `UE` et `EC` (M3) ; accueillir des étudiants (`Etudiant`, M5) ; fixer des règles (`ReglePassage`, M4) ; avoir des utilisateurs (`Utilisateur`, M1) et des rôles personnalisés (`Role`, M1).

### Énumération

| Énumération | Valeurs |
| --- | --- |
| `StatutEcole` | `ACTIVE`, `SUSPENDUE` |

### Table

- `ecole` : code unique, plus les colonnes de `EntiteBase`.
- Séquence `ecole_seq` (pas de 50).
- C'est la **première migration métier** : toutes les tables d'école y font référence par `ecole_id` (clé étrangère et index).

## 6. Isolation des écoles (déjà en place dans `commun`)

- Toute entité qui appartient à une école hérite de `EntiteEcole` (36 entités). Sa colonne `ecole_id` est remplie depuis `ContexteEcole` à la création, et ne change plus.
- Le filtre Hibernate `filtreEcole` (`ecole_id = :ecoleId`) est actif en permanence, y compris pour les chargements par identifiant. Sans école dans le contexte, il ne laisse passer aucune ligne.
- L'école du contexte vient de la revendication `ecole` du jeton JWT (`ContexteEcoleFilter`), jamais du client.
- **Contrainte de l'analyse** (§4.2.7) : deux objets reliés appartiennent toujours à la même école. Les services la vérifient à chaque saisie et à chaque import. Exemples :

  ```
  context InscriptionAnnuelle inv memeEcole:
    self.anneeAcademique.ecole = self.etudiant.ecole
    and self.niveau.cycle.ecole = self.etudiant.ecole

  context Enseignement inv memeEcole:
    self.ec.ecole = self.maquetteUE.ue.ecole
    and self.maquetteUE.semestre.anneeAcademique.ecole = self.ec.ecole
  ```

- Un traitement lancé hors d'une requête (tâche planifiée, calcul en parallèle) passe par `ContexteEcole.executerPour(ecoleId, action)`.

## 7. Interfaces de service [Proposition]

Le cahier de conception ne détaille pas l'interface de M2. Signatures proposées :

```java
public interface EcoleService {
    Ecole creer(String code, String nom, String fuseauHoraire, boolean doubleAuthentification,
            AdministrateurInitial administrateur);   // crée aussi le compte (M1) et envoie l'activation
    Ecole modifier(Long ecoleId, String nom, String fuseauHoraire, boolean doubleAuthentification);
    void suspendre(Long ecoleId, String motif);       // révoque les sessions de l'école
    void reactiver(Long ecoleId);
    Page<Ecole> lister(Pageable page);
    Ecole ecoleCourante();                           // école de l'utilisateur connecté
    boolean estActive(Long ecoleId);                 // utilisé par M1 (A8, CU00)
}
```

`AdministrateurInitial` est un `record` (nom, prénom, e-mail).

## 8. API REST [Proposition]

| Verbe | Route | Permission |
| --- | --- | --- |
| GET, POST | `/api/v1/ecoles` | `ECOLE_GERER` |
| GET, PATCH | `/api/v1/ecoles/{id}` | `ECOLE_GERER` |
| POST | `/api/v1/ecoles/{id}/suspension` | `ECOLE_GERER` |
| POST | `/api/v1/ecoles/{id}/reactivation` | `ECOLE_GERER` |
| GET | `/api/v1/ecoles/courante` | Authentifié (utilisateur d'une école) : nom, code, fuseau, pour l'en-tête du client |

## 9. Dépendances

| Vers | Pour |
| --- | --- |
| M1 Sécurité | Créer le premier administrateur d'école (rôle prédéfini « Administrateur d'école », périmètre `ECOLE`) ; révoquer les sessions à la suspension |
| M10 Notifications | Lien d'activation du premier administrateur (via M1) |
| M9 Audit | Création, modification, suspension, réactivation |

Appelé par : M1 (état de l'école et double authentification, à chaque connexion et dans A8), et tous les modules à travers `commun.isolation`.

## 10. Journal d'audit

`ECOLE_CREEE`, `ECOLE_MODIFIEE`, `ECOLE_SUSPENDUE`, `ECOLE_REACTIVEE` **[Proposition]**, avec le motif de suspension.

## 11. Tests d'acceptation

- Création d'une école et de son premier administrateur : l'administrateur reçoit un lien d'activation.
- Code d'école en double : refus.
- École suspendue : ses utilisateurs ne peuvent plus se connecter, et ses données restent intactes. Après réactivation, ils peuvent de nouveau se connecter.
- **Test d'isolation de référence** : avec deux écoles A et B, aucune donnée de B n'est visible depuis A, ni en recherche ni en chargement par identifiant. À reproduire pour chaque entité d'école des autres modules.
- L'administrateur de la plateforme ne lit aucune donnée académique : 403, ou résultat vide grâce au filtre.
- Un administrateur d'école reçoit 403 sur `/api/v1/ecoles`.

## 12. Questions ouvertes

1. La suspension d'une école doit-elle révoquer immédiatement les sessions en cours ? (Proposition : oui.)
2. Valeur par défaut du fuseau horaire pour SJI et SJM : `Africa/Douala` ?
3. Une école peut-elle être supprimée définitivement (fin de contrat) ? La loi n° 2024/017 peut imposer une durée de conservation ou une suppression des données.
