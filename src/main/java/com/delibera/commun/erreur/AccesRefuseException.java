package com.delibera.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * L'utilisateur n'a pas la permission, le périmètre ou la qualité de membre du jury exigés
 * (403, algorithme A8). Chaque refus est journalisé.
 */
public class AccesRefuseException extends ExceptionMetier {

  public AccesRefuseException(String message) {
    super(HttpStatus.FORBIDDEN, "Accès refusé", "ACCES_REFUSE", message);
  }
}
