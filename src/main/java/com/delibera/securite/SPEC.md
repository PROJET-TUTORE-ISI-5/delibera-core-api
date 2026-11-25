# M1 – Sécurité et accès : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.4 CU00, CU12, CU16 ; §3.5 ; §4.2.6 ; §5.1 à 5.3), cahier de conception (version en ligne de fin novembre 2026 : §2.2, §6.1.7, §6.1.8, algorithme A8), cahier des charges technique (BF16, BF20, BF21, BNF05, BNF13).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Authentifier les utilisateurs, gérer leurs sessions et les comptes de chaque école, gérer les rôles, les permissions et les affectations sur un périmètre, et contrôler chaque requête (session, école, permission, périmètre, jury).

Paquetage : `com.delibera.securite`.

## 2. Exigences couvertes

| Code | Exigence |
| --- | --- |
| BF16 | Créer, désactiver et réactiver les comptes d'une école ; attribuer un ou plusieurs rôles, chacun avec un périmètre (école, cycle ou niveau) et une période. Un compte appartient à une seule école. |
| BF20 | Connexion par e-mail et mot de passe ; changement imposé à la première connexion ; réinitialisation par lien à usage unique ; déconnexion ; verrouillage après plusieurs échecs ; refus si compte désactivé ou école suspendue ; double authentification activable par école. |
| BF21 | Six rôles prédéfinis ; rôles personnalisés construits en combinant des permissions élémentaires. |
| BNF05 | BCrypt ; mot de passe de 10 caractères au minimum ; verrouillage 15 min après 5 échecs consécutifs ; jeton JWT de 15 min, renouvelé tant que l'utilisateur est actif ; session fermée après 30 min d'inactivité ; HTTPS. **Toutes ces valeurs sont configurables.** |
| BNF13 | Session, école, permission et périmètre vérifiés côté serveur à chaque requête ; refus en 403 journalisé ; connexions réussies et échouées journalisées ; moindre privilège. |

## 3. Acteurs et permissions

| Cas | Acteur | Permission |
| --- | --- | --- |
| CU00 S'authentifier | Tout utilisateur | — (route publique) |
| CU18 Réinitialiser son mot de passe | Tout utilisateur | — (route publique) |
| CU12 Gérer les utilisateurs | Administrateur d'école | `UTILISATEUR_GERER` |
| CU16 Gérer les rôles et les permissions | Administrateur d'école | `ROLE_GERER` |

### Les 18 permissions (cahier d'analyse, §3.5)

`ECOLE_GERER`, `REFERENTIEL_GERER`, `REGLE_GERER`, `PARAM_GERER`, `UTILISATEUR_GERER`, `ROLE_GERER`, `JURY_CONSTITUER`, `DONNEES_IMPORTER`, `PREDELIB_LANCER`, `RATTRAPAGE_SAISIR`, `DOSSIER_CONSULTER`, `DOSSIER_STATUER`, `DELIB_VALIDER`, `PV_GENERER`, `RECLAMATION_TRAITER`, `REOUVERTURE_AUTORISER`, `STATS_CONSULTER`, `AUDIT_CONSULTER`.

### Les six rôles prédéfinis et leurs permissions

| Rôle | Permissions |
| --- | --- |
| Administrateur de la plateforme | `ECOLE_GERER` |
| Administrateur d'école | `REFERENTIEL_GERER`, `PARAM_GERER`, `UTILISATEUR_GERER`, `ROLE_GERER`, `JURY_CONSTITUER`, `STATS_CONSULTER`, `AUDIT_CONSULTER` |
| Gestionnaire académique | `REGLE_GERER`, `DONNEES_IMPORTER`, `PREDELIB_LANCER`, `RATTRAPAGE_SAISIR`, `DOSSIER_CONSULTER`, `PV_GENERER`, `RECLAMATION_TRAITER`, `STATS_CONSULTER` |
| Membre du jury | `DOSSIER_CONSULTER`, `DOSSIER_STATUER` |
| Président du jury | `DOSSIER_CONSULTER`, `DOSSIER_STATUER`, `DELIB_VALIDER`, `PV_GENERER`, `REOUVERTURE_AUTORISER`, `STATS_CONSULTER` |
| Auditeur | `DOSSIER_CONSULTER`, `STATS_CONSULTER`, `AUDIT_CONSULTER` |

- Les droits de jury (membre, président) ne valent **que pour la délibération** où l'utilisateur est désigné (CU17, module M7). Ils ne s'obtiennent pas par une affectation permanente.
- Les rôles prédéfinis n'appartiennent à aucune école et sont en lecture seule. Ils peuvent être dupliqués pour créer une variante.
- Une école peut créer des rôles personnalisés en combinant les permissions, **sauf `ECOLE_GERER`**, qui n'est jamais proposée à une école.
- Un compte sans école est un administrateur de la plateforme : il n'accède à aucune note ni décision.
- Les permissions et les rôles prédéfinis sont créés par une migration Flyway de données.

## 4. Cas d'utilisation

### CU00 – S'authentifier

- **Préconditions** : le compte existe et est actif ; son école n'est pas suspendue.
- **Scénario principal** :
  1. L'utilisateur saisit son e-mail et son mot de passe.
  2. Le système vérifie le compte et l'empreinte du mot de passe, puis remet à zéro le compteur d'échecs.
  3. Il charge les rôles avec leurs périmètres, et les jurys où l'utilisateur est désigné.
  4. Il ouvre la session (jeton d'accès de 15 min, renouvelé tant que l'utilisateur est actif).
  5. Il journalise la connexion réussie.
  6. À la première connexion, il impose le changement du mot de passe.
- **Scénarios alternatifs** :
  - Identifiants invalides : message **neutre**, échec compté ; au 5e échec consécutif, compte verrouillé 15 min. La tentative est journalisée.
  - Compte verrouillé, désactivé ou école suspendue : connexion refusée.
  - Double authentification exigée par l'école : un code TOTP à 6 chiffres est demandé avant l'ouverture de la session.
  - Mot de passe oublié : CU18.
- **Postcondition** : session ouverte ; le client n'affiche que les cas permis ; connexion journalisée.

### CU18 – Réinitialiser son mot de passe (extension de CU00)

- L'utilisateur demande la réinitialisation avec son e-mail. Le système envoie un **lien à usage unique valable 30 min** (module M10).
- La réponse est la même que l'adresse existe ou non, pour ne pas révéler les comptes.
- L'utilisateur choisit un nouveau mot de passe (10 caractères au minimum). Le jeton est consommé, et les sessions ouvertes sont révoquées. **[Proposition]**

### CU12 – Gérer les utilisateurs

- **Scénario principal** :
  1. L'administrateur saisit le nom, le prénom et l'e-mail. Le compte est créé dans **son** école.
  2. Il attribue un ou plusieurs rôles, chacun avec un type de périmètre (`ECOLE`, `CYCLE` ou `NIVEAU`), le périmètre visé et une période de validité.
  3. Le système envoie un **lien d'activation valable 72 h**. L'utilisateur définit son mot de passe avec ce lien.
  4. Le système journalise l'opération.
- **Scénarios alternatifs** :
  - E-mail déjà utilisé : refus (`EMAIL_DEJA_UTILISE`).
  - Retrait d'un rôle ou désactivation du compte : effet **immédiat**. Les sessions ouvertes sont révoquées, et l'historique des actions du compte est conservé.
  - Le périmètre doit appartenir à l'école de l'administrateur.

### CU16 – Gérer les rôles et les permissions

- **Scénario principal** : l'administrateur consulte la matrice des rôles et des permissions, crée un rôle personnalisé (code, libellé), coche les permissions et enregistre.
- **Scénarios alternatifs** :
  - Rôles prédéfinis : en lecture seule, mais duplicables.
  - Rôle encore attribué : sa suppression est refusée.
  - Code déjà utilisé, ou permission `ECOLE_GERER` : refus.
- **Postcondition** : les comptes qui portent le rôle en bénéficient **dès leur requête suivante**. Les permissions se relisent donc à chaque requête, et non depuis le jeton.

## 5. Modèle de données

Toutes ces entités héritent de `EntiteBase` (et non de `EntiteEcole`), car leur école est facultative. **Le filtre d'isolation ne s'applique donc pas automatiquement** : chaque requête sur ces tables filtre l'école explicitement.

### Utilisateur

| Attribut | Type | Règle |
| --- | --- | --- |
| `email` | `String` | Unique sur toute la plateforme **[Proposition]**, car l'e-mail sert d'identifiant de connexion |
| `nom`, `prenom` | `String` | Obligatoires |
| `motDePasseHache` | `String` | Empreinte BCrypt ; nul tant que le compte n'est pas activé |
| `actif` | `boolean` | |
| `doitChangerMotDePasse` | `boolean` | |
| `nbEchecs` | `int` | Échecs consécutifs |
| `verrouilleJusqua` | `LocalDateTime` | Nul si non verrouillé |
| `derniereConnexion` | `LocalDateTime` | |
| `secretTotpChiffre` | `String` | Chiffré en AES-256-GCM, avec une clé hors de la base |

Méthodes :
- `estVerrouille(LocalDateTime maintenant) : boolean`
- `enregistrerEchec(int maxEchecs, Duration duree) : void`
- `reinitialiserEchecs() : void`
- `changerMotDePasse(String empreinte) : void`

Associations : appartenir à une `Ecole` (0..1) ; bénéficier de `AffectationRole` (composition, 0..*) ; recevoir des `JetonUtilisateur` (composition) ; ouvrir des `SessionUtilisateur` (composition) ; être désigné dans des `MembreJury` (M7) ; déclencher des `JournalAudit` (M9).

### Role

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | Unique dans l'école (ou parmi les rôles prédéfinis) |
| `libelle` | `String` | |
| `predefini` | `boolean` | Un rôle prédéfini n'a pas d'école et ne se modifie pas |

Méthodes : `accorder(Permission p)`, `retirer(Permission p)`, `dupliquer(String code) : Role`.

Associations : personnaliser (`Ecole` 0..1) ; accorder (`Permission` 1..*) ; attribuer (`AffectationRole` 0..*).

### Permission

| Attribut | Type | Règle |
| --- | --- | --- |
| `code` | `String` | Unique sur la plateforme (les 18 codes ci-dessus) |
| `libelle` | `String` | |
| `module` | `String` | Module concerné (M1 à M10) |

### AffectationRole

| Attribut | Type | Règle |
| --- | --- | --- |
| `typePerimetre` | `TypePerimetre` | `ECOLE`, `CYCLE` ou `NIVEAU` |
| `perimetreId` | `Long` | Identifiant du cycle ou du niveau ; nul pour `ECOLE` |
| `dateDebut` | `LocalDate` | |
| `dateFin` | `LocalDate` | Facultative |

Méthodes : `estValide(LocalDate jour) : boolean`, `couvre(Cible cible) : boolean` (voir A8).

### JetonUtilisateur

| Attribut | Type | Règle |
| --- | --- | --- |
| `type` | `TypeJeton` | `ACTIVATION` (72 h) ou `REINITIALISATION` (30 min) |
| `empreinte` | `String` | SHA-256 du jeton ; le jeton en clair n'est jamais stocké |
| `expireLe` | `LocalDateTime` | |
| `utilise` | `boolean` | Usage unique |

Méthodes : `estValide(LocalDateTime maintenant) : boolean`, `consommer() : void`.

### SessionUtilisateur

| Attribut | Type | Règle |
| --- | --- | --- |
| `empreinteJetonRenouvellement` | `String` | SHA-256 du jeton de renouvellement |
| `ouverteLe` | `LocalDateTime` | |
| `derniereActivite` | `LocalDateTime` | |
| `adresseIP` | `String` | |
| `revoquee` | `boolean` | |

Méthodes : `estActive(LocalDateTime maintenant, Duration inactiviteMax) : boolean`, `revoquer() : void`.

Le jeton d'accès JWT (15 min) n'est pas stocké. La session dure tant que le jeton de renouvellement n'est ni expiré ni révoqué, et que l'inactivité reste sous 30 min.

### Objet de valeur Cible

`record Cible(Long ecoleId, Long cycleId, Long niveauId, Long deliberationId)`. Il décrit l'objet visé par une requête ; il sert à A8.

### Énumérations

| Énumération | Valeurs |
| --- | --- |
| `TypePerimetre` | `ECOLE`, `CYCLE`, `NIVEAU` |
| `TypeJeton` | `ACTIVATION`, `REINITIALISATION` |

## 6. Règles techniques

- **Mots de passe** : BCrypt, 10 caractères au minimum. Message d'erreur de connexion neutre (ne pas dire si l'e-mail existe).
- **Jeton d'accès JWT** (HS256, secret `DELIBERA_JWT_SECRET`), valable 15 min. Revendications :
  - `sub` : identifiant de l'utilisateur ;
  - `ecole` : identifiant de l'école, absent pour l'administrateur de la plateforme. Déjà lue par `ContexteEcoleFilter` ;
  - `sid` : identifiant de la session, pour vérifier la révocation et l'inactivité **[Proposition]**.
- **Jeton de renouvellement** : aléatoire (256 bits), stocké sous forme d'empreinte SHA-256. Un nouveau est émis à chaque renouvellement **[Proposition : rotation]**.
- **Verrouillage** : `nbEchecs` atteint 5 → `verrouilleJusqua` = maintenant + 15 min. Valeurs lues depuis la configuration (`delibera.securite.*`).
- **Double authentification** : TOTP selon la RFC 6238, 6 chiffres, enrôlement par QR code. Exigée quand `Ecole.doubleAuthentification` est vrai.
- **Révocation immédiate** : désactiver un compte, ou retirer un rôle, révoque ses sessions.
- **Permissions relues à chaque requête** : un changement de rôle s'applique dès la requête suivante.
- **Méthodes de service** : chaque méthode exposée porte `@PreAuthorize` avec son code de permission, et `ControleAccesService.verifier` applique A8 quand un périmètre ou un jury est en jeu.

## 7. Interfaces de service (cahier de conception, §6.1.8)

```java
public interface AuthService {
    ResultatConnexion connecter(String email, String motDePasse, String adresseIP);
    JetonsSession verifierCode(Long utilisateurId, String codeTotp);
    JetonsSession renouveler(String jetonRenouvellement);
    void deconnecter(Long sessionId);
    void demanderReinitialisation(String email);
    void reinitialiser(String jeton, String nouveauMotDePasse);
}

public interface ControleAccesService {
    void verifier(Utilisateur utilisateur, String permission, Cible cible); // AccesRefuseException -> 403
}
```

`ResultatConnexion` et `JetonsSession` sont des `record`. `ResultatConnexion` indique si un code TOTP est attendu ou si le mot de passe doit être changé.

**[Proposition]** Services à ajouter pour CU12 et CU16 (signatures à valider) :
- `UtilisateurService` : `creer`, `modifier`, `desactiver`, `reactiver`, `affecterRole`, `retirerAffectation`, `activer(jeton, motDePasse)`, `changerMotDePasse` ;
- `RoleService` : `creer`, `modifier`, `dupliquer`, `supprimer`, `listerPermissions`.

## 8. Algorithme A8 – Contrôle d'accès d'une requête

```text
FONCTION controler(requete, permission, cible)
  jeton <- en-tete Authorization de la requete
  SI jeton absent, mal signe ou expire (15 min) ALORS REFUSER 401
  session <- session du jeton
  SI session revoquee OU inactivite > 30 min ALORS REFUSER 401
  u <- utilisateur de la session
  SI NON u.actif OU (u.ecole != NUL ET NON u.ecole.estActive()) ALORS REFUSER 401
  SI u.ecole = NUL ET permission != ECOLE_GERER ALORS REFUSER 403        // la plateforme ne voit aucune note
  SI u.ecole != NUL ET cible.ecole != u.ecole ALORS REFUSER 403          // isolation des ecoles (BNF12)
  activer le filtre ecole_id = u.ecole pour toute la requete
  POUR CHAQUE affectation a de u valide aujourd'hui FAIRE
    SI permission DANS a.role.permissions ET a.couvre(cible) ALORS AUTORISER
  FIN POUR
  SI cible.deliberation != NUL ET u est membre du jury de cible.deliberation ALORS
    SI permission DANS permissionsJury(qualite de u dans ce jury) ALORS AUTORISER
  FIN SI
  journaliser("REFUS_ACCES", u, permission, cible) ; REFUSER 403
FIN

FONCTION couvre(a, cible)                                                // AffectationRole.couvre
  SELON a.typePerimetre
    ECOLE  : RETOURNER VRAI
    CYCLE  : RETOURNER cible.cycle = a.perimetreId
    NIVEAU : RETOURNER cible.niveau = a.perimetreId
FIN
```

- `permissionsJury(MEMBRE)` = `DOSSIER_CONSULTER`, `DOSSIER_STATUER`.
- `permissionsJury(PRESIDENT)` = celles du membre, plus `DELIB_VALIDER`, `PV_GENERER`, `REOUVERTURE_AUTORISER`.
- Ces droits ne valent que pour la délibération du jury.
- Le refus est journalisé **même si la transaction est annulée** : journal dans une transaction séparée (`REQUIRES_NEW`) **[Proposition]**.

## 9. API REST [Proposition]

| Verbe | Route | Permission | Cas |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/connexion` | publique | CU00 |
| POST | `/api/v1/auth/code` | publique (après connexion) | CU00, double authentification |
| POST | `/api/v1/auth/renouvellement` | publique (jeton de renouvellement) | CU00 |
| POST | `/api/v1/auth/deconnexion` | authentifié | CU00 |
| POST | `/api/v1/auth/activation` | publique (jeton d'activation) | CU12 |
| POST | `/api/v1/auth/mot-de-passe-oublie` | publique | CU18 |
| POST | `/api/v1/auth/reinitialisation` | publique (jeton) | CU18 |
| PUT | `/api/v1/moi/mot-de-passe` | authentifié | CU00 (premier changement) |
| GET | `/api/v1/moi` | authentifié | Profil, permissions et jurys, pour le menu du client |
| GET, POST | `/api/v1/utilisateurs` | `UTILISATEUR_GERER` | CU12 |
| GET, PATCH | `/api/v1/utilisateurs/{id}` | `UTILISATEUR_GERER` | CU12 |
| POST | `/api/v1/utilisateurs/{id}/desactivation`, `/reactivation` | `UTILISATEUR_GERER` | CU12 |
| POST, DELETE | `/api/v1/utilisateurs/{id}/affectations[/{affectationId}]` | `UTILISATEUR_GERER` | CU12 |
| GET, POST | `/api/v1/roles` | `ROLE_GERER` | CU16 |
| PUT, DELETE | `/api/v1/roles/{id}` | `ROLE_GERER` | CU16 |
| POST | `/api/v1/roles/{id}/duplication` | `ROLE_GERER` | CU16 |
| GET | `/api/v1/permissions` | `ROLE_GERER` | CU16 |

Les routes `/api/v1/auth/**` sont déjà publiques dans `SecuriteConfig`.

## 10. Dépendances

| Vers | Pour |
| --- | --- |
| M2 Écoles | État de l'école (active ou suspendue), double authentification exigée |
| M7 Délibération | Appartenance au jury d'une délibération et qualité (A8) |
| M10 Notifications | Liens d'activation et de réinitialisation |
| M9 Audit | Connexions, refus d'accès, gestion des comptes et des rôles |

Appelé par : le filtre de sécurité (toutes les requêtes), M2 (création du premier administrateur d'une école, CU15), M7 (comptes actifs de l'école pour composer un jury).

## 11. Journal d'audit

`CONNEXION_REUSSIE`, `CONNEXION_ECHOUEE`, `COMPTE_VERROUILLE`, `DECONNEXION`, `REFUS_ACCES`, `COMPTE_CREE`, `COMPTE_MODIFIE`, `COMPTE_DESACTIVE`, `COMPTE_REACTIVE`, `ROLE_AFFECTE`, `ROLE_RETIRE`, `ROLE_CREE`, `ROLE_MODIFIE`, `ROLE_SUPPRIME`, `MOT_DE_PASSE_CHANGE`, `MOT_DE_PASSE_REINITIALISE`.

Ce catalogue d'actions est une **[Proposition]**. Ne journalisez jamais un mot de passe, un jeton ou un secret.

## 12. Tests d'acceptation

- Connexion réussie : jeton émis, `nbEchecs` remis à 0, connexion journalisée.
- Cinq échecs consécutifs : compte verrouillé 15 min. Une connexion correcte pendant le verrouillage est refusée.
- Compte désactivé, ou école suspendue : connexion refusée.
- Double authentification exigée : pas de jeton sans code TOTP valide.
- Session inactive depuis plus de 30 min, ou révoquée : 401.
- Jeton de réinitialisation expiré (plus de 30 min) ou déjà utilisé : refus.
- Un administrateur d'école ne voit ni ne modifie les comptes d'une autre école.
- Un gestionnaire limité au cycle Licence reçoit 403 sur une cible du cycle Master, et le refus est journalisé.
- Un membre du jury n'a `DOSSIER_STATUER` que sur la délibération de son jury.
- L'administrateur de la plateforme reçoit 403 sur toute route académique.
- Un rôle personnalisé ne peut pas contenir `ECOLE_GERER`. Un rôle encore attribué ne peut pas être supprimé.
- Le retrait d'un rôle s'applique dès la requête suivante.

## 13. Questions ouvertes

1. Les rôles prédéfinis « Membre du jury » et « Président du jury » servent-ils seulement de modèles de permissions pour `permissionsJury`, sans jamais être affectés ? (Interprétation retenue ci-dessus.)
2. L'e-mail est-il unique sur toute la plateforme, ou seulement dans une école ?
3. Durée de vie du jeton de renouvellement (en plus de la règle d'inactivité de 30 min).
4. Politique de mot de passe au-delà des 10 caractères (complexité, réutilisation).
