/**
 * M8 – Documents et statistiques.
 *
 * <p>PV au modèle de chaque école, listes par décision, relevés, statistiques par niveau, filière
 * et année.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : ProcesVerbal. Cas d'utilisation
 * couverts : CU09, CU10.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.documents;
