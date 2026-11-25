/**
 * M1 – Sécurité et accès.
 *
 * <p>Connexion par e-mail et mot de passe, double authentification, sessions JWT, activation et
 * réinitialisation ; comptes, rôles, permissions et affectations ; contrôle d'accès de chaque
 * requête (algorithme A8).
 *
 * <p>Classes principales (cahier de conception, tableau 5) : Utilisateur, Role, Permission,
 * AffectationRole, JetonUtilisateur, SessionUtilisateur. Cas d'utilisation couverts : CU00, CU12,
 * CU16, CU18.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.securite;
