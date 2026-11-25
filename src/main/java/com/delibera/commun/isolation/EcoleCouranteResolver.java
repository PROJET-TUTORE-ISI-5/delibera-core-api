package com.delibera.commun.isolation;

import java.util.function.Supplier;

/**
 * Fournit au filtre Hibernate l'identifiant de l'école du traitement en cours.
 *
 * <p>Sans école (administrateur de la plateforme, traitement hors requête), la valeur
 * {@link ContexteEcole#AUCUNE_ECOLE} ne correspond à aucune ligne : la plateforme ne voit aucune
 * donnée académique.
 */
public class EcoleCouranteResolver implements Supplier<Long> {

  @Override
  public Long get() {
    return ContexteEcole.ecoleCourante().orElse(ContexteEcole.AUCUNE_ECOLE);
  }
}
