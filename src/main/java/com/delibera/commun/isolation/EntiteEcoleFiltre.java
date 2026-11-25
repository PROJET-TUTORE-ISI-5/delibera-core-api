package com.delibera.commun.isolation;

/**
 * Nom et paramètre du filtre Hibernate qui isole les données de chaque école (BNF12).
 *
 * <p>Le filtre est déclaré sur {@link com.delibera.commun.entite.EntiteEcole} : il s'applique
 * automatiquement à toutes les entités qui en héritent, aux requêtes comme aux chargements par
 * identifiant, et ajoute la condition {@code ecole_id = :ecoleId}. La valeur du paramètre est
 * fournie par {@link EcoleCouranteResolver}.
 */
public final class EntiteEcoleFiltre {

  public static final String NOM = "filtreEcole";
  public static final String PARAMETRE = "ecoleId";
  public static final String CONDITION = "ecole_id = :" + PARAMETRE;

  private EntiteEcoleFiltre() {}
}
