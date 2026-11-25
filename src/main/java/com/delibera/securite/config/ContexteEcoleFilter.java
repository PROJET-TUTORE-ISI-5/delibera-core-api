package com.delibera.securite.config;

import com.delibera.commun.isolation.ContexteEcole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Place l'école de l'utilisateur connecté dans {@link ContexteEcole} pour toute la requête, puis
 * l'efface. L'école est lue dans le jeton JWT signé par le serveur : le client ne peut pas la
 * choisir.
 *
 * <p>Ce filtre est ajouté à la chaîne de Spring Security par {@link SecuriteConfig} ; il n'est pas
 * déclaré comme bean pour ne pas être exécuté deux fois.
 */
public class ContexteEcoleFilter extends OncePerRequestFilter {

  /** Revendication du jeton JWT qui porte l'identifiant de l'école. */
  public static final String CLAIM_ECOLE = "ecole";

  @Override
  protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse,
      FilterChain chaine) throws ServletException, IOException {
    try {
      ContexteEcole.definir(ecoleDuJeton());
      chaine.doFilter(requete, reponse);
    } finally {
      ContexteEcole.effacer();
    }
  }

  private static Long ecoleDuJeton() {
    Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
    if (authentification instanceof JwtAuthenticationToken jeton
        && jeton.getToken().getClaims().get(CLAIM_ECOLE) instanceof Number ecole) {
      return ecole.longValue();
    }
    return null;
  }
}
