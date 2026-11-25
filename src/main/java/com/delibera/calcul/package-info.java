/**
 * M6 – Moteur de calcul.
 *
 * <p>Notes d'EC, résultats d'UE, crédits, dettes, évaluation des règles, propositions, options de
 * relèvement et UE prioritaires (algorithmes A3 à A7). Il calcule en mémoire ; M7 enregistre.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : SituationAcademique, ResultatUE,
 * ResultatEC, Dette, PropositionDecision, OptionRelevement, UEPrioritaire. Cas d'utilisation
 * couverts : CU04 à CU07.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 */
package com.delibera.calcul;
