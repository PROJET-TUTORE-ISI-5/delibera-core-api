package com.delibera;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DeliberaApplicationTests {

  @Test
  void leContexteDemarre() {
    // Échoue si la configuration, la connexion à PostgreSQL ou les migrations Flyway échouent.
  }
}
