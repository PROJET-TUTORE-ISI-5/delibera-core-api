package com.delibera.commun.isolation;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * École de l'utilisateur connecté, pour le traitement en cours.
 *
 * <p>L'école vient toujours de la session (jeton JWT), jamais d'un paramètre envoyé par le client
 * (cahier de conception, section 4.2.2). Le filtre de sécurité la définit au début de chaque
 * requête et l'efface à la fin. Le filtre Hibernate {@link EntiteEcoleFiltre} la lit pour limiter
 * toute requête aux données de cette école.
 *
 * <p>Un traitement lancé hors d'une requête (tâche planifiée, test) doit passer par
 * {@link #executerPour(Long, Supplier)}. Un traitement parallèle (pré-délibération par lots) doit
 * transmettre l'école à ses fils d'exécution de la même manière.
 */
public final class ContexteEcole {

  /** Valeur utilisée quand aucune école n'est définie : aucune ligne ne lui appartient. */
  public static final long AUCUNE_ECOLE = -1L;

  private static final ThreadLocal<Long> ECOLE_COURANTE = new ThreadLocal<>();

  private ContexteEcole() {}

  /** Définit l'école du traitement en cours. */
  public static void definir(Long ecoleId) {
    if (ecoleId == null) {
      ECOLE_COURANTE.remove();
    } else {
      ECOLE_COURANTE.set(ecoleId);
    }
  }

  /** École du traitement en cours ; vide pour l'administrateur de la plateforme ou hors requête. */
  public static Optional<Long> ecoleCourante() {
    return Optional.ofNullable(ECOLE_COURANTE.get());
  }

  /** Efface l'école du traitement en cours. */
  public static void effacer() {
    ECOLE_COURANTE.remove();
  }

  /** Exécute une action pour une école donnée, puis rétablit le contexte précédent. */
  public static <T> T executerPour(Long ecoleId, Supplier<T> action) {
    Long precedente = ECOLE_COURANTE.get();
    definir(ecoleId);
    try {
      return action.get();
    } finally {
      definir(precedente);
    }
  }
}
