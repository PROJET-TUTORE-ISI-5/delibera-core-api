package com.delibera.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * La demande est bien formée mais viole une règle métier (422). Exemples : modifier une
 * délibération clôturée, motif absent pour une décision du jury.
 */
public class RegleMetierException extends ExceptionMetier {

  public RegleMetierException(String code, String message) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, "Règle métier non respectée", code, message);
  }
}
