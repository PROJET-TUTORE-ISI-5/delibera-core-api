package com.delibera.commun.erreur;

import org.springframework.http.HttpStatus;

/**
 * Erreur prévue par le métier, renvoyée au client au format RFC 9457 (Problem Details).
 *
 * <p>Le {@code code} est stable et documenté : le client web s'en sert pour afficher le bon
 * message, alors que le texte du message peut évoluer.
 */
public abstract class ExceptionMetier extends RuntimeException {

  private final HttpStatus statut;
  private final String titre;
  private final String code;

  protected ExceptionMetier(HttpStatus statut, String titre, String code, String message) {
    super(message);
    this.statut = statut;
    this.titre = titre;
    this.code = code;
  }

  public HttpStatus getStatut() {
    return statut;
  }

  public String getTitre() {
    return titre;
  }

  public String getCode() {
    return code;
  }
}
