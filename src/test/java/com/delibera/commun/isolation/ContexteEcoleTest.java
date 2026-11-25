package com.delibera.commun.isolation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContexteEcoleTest {

  @AfterEach
  void nettoyer() {
    ContexteEcole.effacer();
  }

  @Test
  void sansEcoleLeFiltreNeCorrespondAAucuneLigne() {
    assertThat(ContexteEcole.ecoleCourante()).isEmpty();
    assertThat(new EcoleCouranteResolver().get()).isEqualTo(ContexteEcole.AUCUNE_ECOLE);
  }

  @Test
  void leFiltreRecoitLEcoleDefinie() {
    ContexteEcole.definir(7L);
    assertThat(new EcoleCouranteResolver().get()).isEqualTo(7L);
  }

  @Test
  void executerPourRetablitLEcolePrecedente() {
    ContexteEcole.definir(1L);

    Long pendant = ContexteEcole.executerPour(2L, () -> ContexteEcole.ecoleCourante().orElseThrow());

    assertThat(pendant).isEqualTo(2L);
    assertThat(ContexteEcole.ecoleCourante()).contains(1L);
  }

  @Test
  void executerPourRetablitLEcoleMemeEnCasDErreur() {
    try {
      ContexteEcole.executerPour(2L, () -> {
        throw new IllegalStateException("échec");
      });
    } catch (IllegalStateException attendue) {
      // l'erreur est voulue
    }
    assertThat(ContexteEcole.ecoleCourante()).isEmpty();
  }
}
