# M10 – Notifications : spécifications

> **Sources** : cahier d'analyse v2.3 (§3.4 CU00, CU12, CU17 ; §5.2), cahier de conception (version en ligne de fin novembre 2026 : §2.2 « Serveur SMTP », §5.1, §5.2, §6.1.8), cahier des charges technique (BF16, BF20, BF22).
> Ce fichier est la référence pour développer le module. Les éléments marqués **[Proposition]** ne figurent pas dans les cahiers : faites-les valider par l'équipe avant de les implémenter.

## 1. Rôle

Seul module qui envoie des messages vers l'extérieur : les courriels, par SMTP. L'envoi est **asynchrone**, avec une **nouvelle tentative en cas d'échec**. Le module n'a pas d'entité.

Paquetage : `com.delibera.notification`.

## 2. Courriels à envoyer

| Courriel | Déclencheur | Contenu | Validité |
| --- | --- | --- | --- |
| Activation du compte | CU12 (création d'un compte), CU15 (premier administrateur d'une école) | Lien d'activation pour définir son mot de passe | **72 h** |
| Réinitialisation du mot de passe | CU18 | Lien de réinitialisation à usage unique | **30 min** |
| Désignation dans un jury | CU17 | Délibération (année, niveau), qualité (président ou membre) | — |

- Les jetons des liens sont créés par M1 (`JetonUtilisateur`). M10 ne fait que les transmettre dans le corps du message ; il ne les stocke ni ne les journalise.
- Langue : français.
- Contenu minimal : **aucune note, aucune décision** dans un courriel.

## 3. Interface de service (cahier de conception, §6.1.8)

```java
public interface NotificationService {
    void envoyer(Courriel courriel); // asynchrone, nouvelle tentative en cas d'échec
}
```

`Courriel` est un `record` : destinataire, objet, modèle, variables du modèle **[Proposition]**.

## 4. Règles techniques

- **SMTP avec STARTTLS (port 587)**. Paramètres : `DELIBERA_SMTP_HOST`, `DELIBERA_SMTP_PORT`, `DELIBERA_SMTP_USER`, `DELIBERA_SMTP_PASSWORD`.
- En développement, **Mailpit** (`compose.yaml`) : SMTP sur le port 1025, interface sur http://localhost:8025.
- **Asynchrone** : l'appelant n'attend pas l'envoi. Un serveur SMTP indisponible ne fait jamais échouer la transaction métier. L'indicateur de santé du courrier est d'ailleurs désactivé dans `application.yml`.
- **Envoi après la validation de la transaction** : par exemple un événement Spring traité après le commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`). Ainsi, aucun courriel ne part pour un compte dont la création a échoué **[Proposition]**.
- **Nouvelles tentatives** : par exemple 3 essais espacés de 1, 5 puis 15 minutes, puis abandon journalisé (`COURRIEL_ECHEC`) **[Proposition]**. Les valeurs sont configurables.
- **Modèles** : un modèle par type de courriel, en texte et en HTML, avec des variables. Pas de concaténation de chaînes dans le code **[Proposition]**.
- **Journaux applicatifs** : jamais le lien complet ni le jeton. Seuls le type de courriel et l'identifiant du destinataire.

## 5. API REST

Aucune route : le module est appelé par les autres modules.

## 6. Dépendances

| Vers | Pour |
| --- | --- |
| M9 Audit | Échec définitif d'un envoi **[Proposition]** |

Appelé par : M1 (activation, réinitialisation), M2 (premier administrateur, via M1), M7 (désignation dans un jury).

## 7. Tests d'acceptation

- Création d'un compte : un courriel d'activation arrive avec un lien. Test d'intégration avec un serveur SMTP de test, par exemple GreenMail, ou Mailpit dans Testcontainers.
- Création de compte annulée par une erreur : aucun courriel envoyé.
- Serveur SMTP indisponible : la création du compte réussit, et l'envoi est retenté.
- Désignation dans un jury : un courriel par personne désignée, avec la qualité et la délibération.
- Aucun jeton ni lien dans les journaux applicatifs.

## 8. Questions ouvertes

1. Fournisseur SMTP de production (compte d'école, service externe) et adresse d'expédition, par exemple `no-reply@…`.
2. Faut-il prévenir les étudiants (publication des résultats, réponse à une réclamation) ? Ce n'est pas prévu par le cahier des charges.
3. Faut-il garder une trace des courriels envoyés (table d'envoi) pour le support ?
