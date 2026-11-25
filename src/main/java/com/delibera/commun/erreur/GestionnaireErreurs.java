package com.delibera.commun.erreur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduit les exceptions en réponses au format RFC 9457 (Problem Details).
 *
 * <p>Les erreurs de Spring MVC (validation, JSON mal formé, méthode non permise…) sont traitées
 * par la classe mère, au même format.
 */
@RestControllerAdvice
public class GestionnaireErreurs extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GestionnaireErreurs.class);

  @ExceptionHandler(ExceptionMetier.class)
  ProblemDetail traiterExceptionMetier(ExceptionMetier exception) {
    return probleme(exception.getStatut(), exception.getTitre(), exception.getCode(),
        exception.getMessage());
  }

  /** Deux saisies simultanées sur le même dossier : le client recharge le dossier. */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail traiterModificationConcurrente(OptimisticLockingFailureException exception) {
    return probleme(HttpStatus.CONFLICT, "Conflit", "MODIFICATION_CONCURRENTE",
        "Ces données ont été modifiées par un autre utilisateur. Rechargez-les puis recommencez.");
  }

  /** Refus d'une règle {@code @PreAuthorize}. */
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail traiterAccesRefuse(AccessDeniedException exception) {
    return probleme(HttpStatus.FORBIDDEN, "Accès refusé", "ACCES_REFUSE",
        "Vous n'avez pas la permission d'effectuer cette action.");
  }

  /** Erreur imprévue : le détail reste dans le journal du serveur, jamais dans la réponse. */
  @ExceptionHandler(Exception.class)
  ProblemDetail traiterErreurImprevue(Exception exception) {
    LOG.error("Erreur imprévue", exception);
    return probleme(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "ERREUR_INTERNE",
        "Une erreur imprévue est survenue. Réessayez ou contactez l'administrateur.");
  }

  private static ProblemDetail probleme(HttpStatus statut, String titre, String code,
      String detail) {
    ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
    probleme.setTitle(titre);
    probleme.setProperty("code", code);
    return probleme;
  }
}
