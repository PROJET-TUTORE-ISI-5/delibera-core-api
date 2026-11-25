/**
 * M7 – Délibération et jury.
 *
 * <p>Cycle de vie des délibérations, composition du jury, marge et seuil limite, décisions et
 * relèvements, rattrapages, clôture et acquis, réouverture et réclamations. Seul ce module
 * orchestre une délibération.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : Deliberation, MembreJury,
 * DecisionJury, RelevementNote, Reclamation. Cas d'utilisation couverts : CU04 à CU08, CU14,
 * CU17.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 */
package com.delibera.deliberation;
