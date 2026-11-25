package com.delibera.commun.entite;

import com.delibera.commun.isolation.ContexteEcole;
import com.delibera.commun.isolation.EcoleCouranteResolver;
import com.delibera.commun.isolation.EntiteEcoleFiltre;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Classe de base des 36 entités qui appartiennent toujours à une école (cahier de conception,
 * section 6.1.1).
 *
 * <p>L'identifiant de l'école est pris dans le contexte de la requête à la création, et ne change
 * plus ensuite. Le filtre {@link EntiteEcoleFiltre#NOM}, actif en permanence, limite toute lecture
 * aux lignes de l'école de l'utilisateur connecté.
 */
@MappedSuperclass
@FilterDef(
    name = EntiteEcoleFiltre.NOM,
    defaultCondition = EntiteEcoleFiltre.CONDITION,
    parameters = @ParamDef(
        name = EntiteEcoleFiltre.PARAMETRE,
        type = Long.class,
        resolver = EcoleCouranteResolver.class),
    autoEnabled = true,
    applyToLoadByKey = true)
@Filter(name = EntiteEcoleFiltre.NOM)
public abstract class EntiteEcole extends EntiteBase {

  @Column(name = "ecole_id", nullable = false, updatable = false)
  protected Long ecoleId;

  public Long getEcoleId() {
    return ecoleId;
  }

  /** Indique si l'objet appartient à l'école donnée. */
  public boolean appartientA(Long ecoleId) {
    return this.ecoleId != null && this.ecoleId.equals(ecoleId);
  }

  @PrePersist
  void rattacherAEcoleCourante() {
    if (ecoleId == null) {
      ecoleId = ContexteEcole.ecoleCourante()
          .orElseThrow(() -> new IllegalStateException(
              "Aucune école dans le contexte : impossible d'enregistrer "
                  + getClass().getSimpleName()));
    }
  }
}
