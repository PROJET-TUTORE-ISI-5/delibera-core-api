package com.delibera.securite.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Paramètres de sécurité (BNF05), lus depuis {@code delibera.securite.*}.
 *
 * @param jwtSecret secret de signature des jetons (HS256), au moins 32 caractères, fourni par la
 *     variable d'environnement {@code DELIBERA_JWT_SECRET}
 * @param dureeJetonAcces validité du jeton d'accès JWT (15 min)
 * @param inactiviteMax durée d'inactivité qui ferme une session (30 min)
 * @param originesAutorisees origines du client web autorisées à appeler l'API (CORS)
 */
@Validated
@ConfigurationProperties(prefix = "delibera.securite")
public record SecuriteProprietes(
    @NotBlank @Size(min = 32) String jwtSecret,
    @NotNull @DefaultValue("15m") Duration dureeJetonAcces,
    @NotNull @DefaultValue("30m") Duration inactiviteMax,
    @DefaultValue List<String> originesAutorisees) {}
