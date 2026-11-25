package com.delibera;

import org.springframework.boot.SpringApplication;

/**
 * Lance l'API en local avec une base PostgreSQL fournie par Testcontainers, sans installer
 * PostgreSQL ni lancer compose.yaml (Docker requis).
 */
public class TestDeliberaApplication {

  public static void main(String[] args) {
    SpringApplication.from(DeliberaApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
