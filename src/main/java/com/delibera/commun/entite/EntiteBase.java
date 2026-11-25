package com.delibera.commun.entite;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Classe de base de toutes les entités (cahier de conception, section 6.1.1).
 *
 * <p>Elle porte l'identifiant technique, le compteur du verrouillage optimiste ({@code verrou},
 * distinct des versions métier des règles, des paramètres et des situations) et les dates
 * techniques, en UTC.
 *
 * <p>L'identifiant vient d'une séquence propre à chaque table ({@code <table>_seq}, pas de 50),
 * et non d'une colonne auto-incrémentée : Hibernate peut ainsi enregistrer par lots, ce que
 * demande la pré-délibération (algorithme A1). Chaque migration Flyway qui crée une table crée
 * aussi sa séquence, par exemple {@code create sequence ecole_seq start with 1 increment by 50;}.
 */
@MappedSuperclass
public abstract class EntiteBase {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  protected Long id;

  /** Deux modifications simultanées du même objet donnent une erreur 409. */
  @Version
  @Column(nullable = false)
  protected int verrou;

  @CreationTimestamp
  @Column(name = "cree_le", nullable = false, updatable = false)
  protected LocalDateTime creeLe;

  @UpdateTimestamp
  @Column(name = "modifie_le", nullable = false)
  protected LocalDateTime modifieLe;

  public Long getId() {
    return id;
  }

  public int getVerrou() {
    return verrou;
  }

  public LocalDateTime getCreeLe() {
    return creeLe;
  }

  public LocalDateTime getModifieLe() {
    return modifieLe;
  }

  /** Deux entités sont égales si elles sont de la même classe et ont le même identifiant. */
  @Override
  public boolean equals(Object autre) {
    if (this == autre) {
      return true;
    }
    if (autre == null || Hibernate.getClass(this) != Hibernate.getClass(autre)) {
      return false;
    }
    EntiteBase entite = (EntiteBase) autre;
    return id != null && id.equals(entite.id);
  }

  @Override
  public int hashCode() {
    return Hibernate.getClass(this).hashCode();
  }
}
