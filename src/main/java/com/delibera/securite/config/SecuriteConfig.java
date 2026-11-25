package com.delibera.securite.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration de Spring Security (module M1).
 *
 * <p>L'API est sans état : chaque requête porte un jeton JWT dans l'en-tête
 * {@code Authorization}. Toutes les routes exigent un jeton valide, sauf la connexion, la
 * documentation de l'API et l'état de santé. Le contrôle fin (permission, périmètre, jury) se fait
 * dans les services par {@code @PreAuthorize} (algorithme A8).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecuriteConfig {

  private static final String[] ROUTES_PUBLIQUES = {
    "/api/v1/auth/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/actuator/health/**",
    "/actuator/info"
  };

  @Bean
  SecurityFilterChain chaineDeSecurite(HttpSecurity http) throws Exception {
    http
        // Pas de cookie de session : le jeton est envoyé dans l'en-tête, la protection CSRF est
        // donc inutile.
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(autorisations -> autorisations
            .requestMatchers(ROUTES_PUBLIQUES).permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(serveur -> serveur.jwt(Customizer.withDefaults()))
        .addFilterAfter(new ContexteEcoleFilter(), BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(SecuriteProprietes proprietes) {
    return NimbusJwtDecoder.withSecretKey(cleDeSignature(proprietes))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }

  @Bean
  JwtEncoder jwtEncoder(SecuriteProprietes proprietes) {
    return NimbusJwtEncoder.withSecretKey(cleDeSignature(proprietes))
        .algorithm(MacAlgorithm.HS256)
        .build();
  }

  /** Seules les empreintes BCrypt des mots de passe sont stockées. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(SecuriteProprietes proprietes) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(proprietes.originesAutorisees());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  private static SecretKey cleDeSignature(SecuriteProprietes proprietes) {
    byte[] secret = proprietes.jwtSecret().getBytes(StandardCharsets.UTF_8);
    return new SecretKeySpec(secret, "HmacSHA256");
  }
}
