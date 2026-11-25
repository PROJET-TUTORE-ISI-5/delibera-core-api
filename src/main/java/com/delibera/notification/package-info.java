/**
 * M10 – Notifications.
 *
 * <p>Envoi asynchrone des courriels, nouvelle tentative en cas d'échec.
 *
 * <p>Ce module n'a pas d'entité. Cas d'utilisation couverts : CU12, CU17, CU18.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 */
package com.delibera.notification;
