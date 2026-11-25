package com.delibera.commun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.delibera.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Vérifie que chaque route de l'API est documentée dans Swagger (AGENT.md, section 5.1).
 *
 * <p>Le test lit le contrat généré par springdoc ({@code /v3/api-docs}) et échoue si une route n'a
 * pas de résumé, si son tag n'a pas de description, si sa réponse de réussite n'est pas
 * documentée, ou si une route protégée ne documente pas les réponses 401 et 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DocumentationOpenApiTest {

  private static final Set<String> VERBES = Set.of("get", "post", "put", "patch", "delete");

  /** Routes accessibles sans jeton (voir {@code SecuriteConfig}). */
  private static final String PREFIXE_PUBLIC = "/api/v1/auth/";

  @Autowired
  private MockMvc mockMvc;

  @Test
  @SuppressWarnings("unchecked")
  void chaqueRouteDeLApiEstDocumentee() throws Exception {
    // Étant donné le contrat OpenAPI généré à partir du code
    String contrat = mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    Map<String, Object> racine = JsonPath.read(contrat, "$");
    Map<String, Object> chemins = (Map<String, Object>) racine.getOrDefault("paths", Map.of());
    List<String> tagsDecrits = tagsDecrits(racine);
    List<String> manques = new ArrayList<>();

    // Quand on parcourt chaque opération
    chemins.forEach((chemin, operations) ->
        ((Map<String, Object>) operations).forEach((verbe, valeur) -> {
          if (!VERBES.contains(verbe)) {
            return;
          }
          Map<String, Object> operation = (Map<String, Object>) valeur;
          String route = verbe.toUpperCase(Locale.ROOT) + " " + chemin;
          verifierOperation(route, chemin, operation, tagsDecrits, manques);
        }));

    // Alors aucune documentation ne manque
    assertThat(manques)
        .as("Routes dont la documentation OpenAPI est incomplète (AGENT.md, section 5.1)")
        .isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static void verifierOperation(String route, String chemin, Map<String, Object> operation,
      List<String> tagsDecrits, List<String> manques) {
    if (!(operation.get("summary") instanceof String resume) || resume.isBlank()) {
      manques.add(route + " : résumé absent (@Operation summary)");
    }
    if (!(operation.get("tags") instanceof List<?> tags) || tags.isEmpty()
        || !tagsDecrits.containsAll(tags)) {
      manques.add(route + " : tag absent ou sans description (@Tag name et description)");
    }
    Map<String, Object> reponses =
        (Map<String, Object>) operation.getOrDefault("responses", Map.of());
    if (reponses.keySet().stream().noneMatch(code -> code.startsWith("2"))) {
      manques.add(route + " : réponse de réussite non documentée (@ApiResponse 2xx)");
    }
    if (!chemin.startsWith(PREFIXE_PUBLIC)) {
      for (String code : List.of("401", "403")) {
        if (!reponses.containsKey(code)) {
          manques.add(route + " : réponse " + code + " non documentée (@ApiResponse)");
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> tagsDecrits(Map<String, Object> racine) {
    List<String> noms = new ArrayList<>();
    for (Object element : (List<Object>) racine.getOrDefault("tags", List.of())) {
      Map<String, Object> tag = (Map<String, Object>) element;
      if (tag.get("description") instanceof String description && !description.isBlank()) {
        noms.add((String) tag.get("name"));
      }
    }
    return noms;
  }
}
