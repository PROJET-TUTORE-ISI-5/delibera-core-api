/**
 * M4 – Règles et paramètres.
 *
 * <p>Saisie et versionnement des règles et des jeux de paramètres ; choix du plus précis pour un
 * étudiant (algorithme A2).
 *
 * <p>Classes principales (cahier de conception, tableau 5) : ReglePassage, ConditionRegle,
 * ParametreDeliberation. Cas d'utilisation couverts : CU02, CU13.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 */
package com.delibera.regles;
