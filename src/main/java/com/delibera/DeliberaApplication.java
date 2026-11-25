package com.delibera;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée de l'API Delibera.
 *
 * <p>Le serveur est découpé en dix modules (cahier de conception, section 5.1), un paquetage par
 * module : {@code securite} (M1), {@code ecole} (M2), {@code referentiel} (M3), {@code regles}
 * (M4), {@code importation} (M5), {@code calcul} (M6), {@code deliberation} (M7),
 * {@code documents} (M8), {@code audit} (M9) et {@code notification} (M10). Le paquetage
 * {@code commun} regroupe ce qu'ils partagent.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DeliberaApplication {

  public static void main(String[] args) {
    // Toutes les dates sont enregistrées en UTC et affichées dans le fuseau de l'école.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(DeliberaApplication.class, args);
  }
}
