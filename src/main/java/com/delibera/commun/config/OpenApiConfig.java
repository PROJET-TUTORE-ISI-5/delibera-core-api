package com.delibera.commun.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Contrat OpenAPI 3 de l'API, généré depuis le code (Swagger UI : {@code /swagger-ui.html}).
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Delibera – API",
        version = "v1",
        description = "API REST de la plateforme de délibération des étudiants (SJI, SJM)."),
    security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_JWT))
@SecurityScheme(
    name = OpenApiConfig.SCHEMA_JWT,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {

  static final String SCHEMA_JWT = "jwt";
}
