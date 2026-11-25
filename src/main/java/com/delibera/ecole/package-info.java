/**
 * M2 – Écoles clientes.
 *
 * <p>Création, suspension et réactivation des écoles, premier administrateur, contexte d'école de
 * chaque requête.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : Ecole. Cas d'utilisation couverts :
 * CU15.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.ecole;
