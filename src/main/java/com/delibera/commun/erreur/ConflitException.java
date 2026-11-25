package com.delibera.commun.erreur;

import org.springframework.http.HttpStatus;

/** La demande entre en conflit avec l'état actuel des données (409). */
public class ConflitException extends ExceptionMetier {

  public ConflitException(String code, String message) {
    super(HttpStatus.CONFLICT, "Conflit", code, message);
  }
}
