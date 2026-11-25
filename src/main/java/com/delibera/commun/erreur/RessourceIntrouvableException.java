package com.delibera.commun.erreur;

import org.springframework.http.HttpStatus;

/** La ressource demandée n'existe pas, ou n'appartient pas à l'école de l'utilisateur (404). */
public class RessourceIntrouvableException extends ExceptionMetier {

  public RessourceIntrouvableException(String ressource, Object identifiant) {
    super(HttpStatus.NOT_FOUND, "Ressource introuvable", "RESSOURCE_INTROUVABLE",
        ressource + " " + identifiant + " introuvable.");
  }
}
