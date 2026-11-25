/**
 * M5 – Import.
 *
 * <p>Modèles de fichiers, correspondance des colonnes, contrôle de cohérence, rapport
 * d'anomalies, provenance de chaque ligne. Le paquetage s'appelle « importation » car « import »
 * est un mot réservé de Java.
 *
 * <p>Classes principales (cahier de conception, tableau 5) : ImportFichier, Etudiant,
 * InscriptionAnnuelle, InscriptionPedagogique, Note, AcquisEC. Cas d'utilisation couverts : CU03,
 * CU07.
 *
 * <p>Organisation interne : {@code web} (contrôleurs REST et DTO), {@code service} (transactions
 * et règles métier), {@code domaine} (entités JPA), {@code depot} (dépôts Spring Data). Les
 * entités ne sortent jamais de la couche service. Les autres modules n'utilisent que les
 * interfaces de service de ce module.
 *
 * <p>Spécifications détaillées : {@code SPEC.md} dans ce dossier.
 */
package com.delibera.importation;
