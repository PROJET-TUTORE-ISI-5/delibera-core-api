package com.delibera.securite;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.delibera.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Vérifie les règles d'accès générales de l'API (BNF05, BNF13). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SecuriteIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void letatDeSanteEstPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void uneRouteDeLApiSansJetonEstRefusee() throws Exception {
    mockMvc.perform(get("/api/v1/deliberations")).andExpect(status().isUnauthorized());
  }

  @Test
  void avecUnJetonLesErreursSuiventLaRfc9457() throws Exception {
    mockMvc.perform(get("/api/v1/route-inexistante").with(jwt().jwt(j -> j.claim("ecole", 1L))))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
