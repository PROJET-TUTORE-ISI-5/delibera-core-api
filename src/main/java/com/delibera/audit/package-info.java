/**
 * M9 – Audit.
 *
 * <p>Journal non effaçable des modifications, connexions et refus d'accès ; consultation filtrée.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : JournalAudit. Cas d'utilisation
 * couverts : CU11.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.audit;
