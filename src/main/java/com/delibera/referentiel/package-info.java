/**
 * M3 – Référentiel et maquettes.
 *
 * <p>Années et semestres, cycles, filières et niveaux couverts, spécialités, classes, catalogue
 * des UE et des EC, maquette de chaque année.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : AnneeAcademique, Cycle, Filiere,
 * Niveau, UE, EC, MaquetteUE, Enseignement. Cas d'utilisation couverts : CU01.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.referentiel;
