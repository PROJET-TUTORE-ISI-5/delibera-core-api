# syntax=docker/dockerfile:1
# Image de l'API Delibera : la même en local (compose.yaml, service « api ») et en production
# (deploy/compose.prod.yaml sur l'EC2). Construite et testée par GitHub Actions.
#   docker build -t delibera-core-api .
#
# Profils Spring par défaut : prod,docker. En local, compose.yaml passe dev,docker.

# --- 1. Compilation : wrapper Maven du dépôt, tests exclus (ils tournent avant, dans le pipeline)
FROM eclipse-temurin:21-jdk-noble AS construction
WORKDIR /construction
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/main/ src/main/
RUN ./mvnw -B -q -Dmaven.test.skip=true package \
 && cp target/delibera-core-api-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# --- 2. Exécution : JRE seul, utilisateur sans privilèges, une couche par type de contenu
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system --gid 10001 delibera \
 && useradd --system --uid 10001 --gid delibera --no-create-home --shell /usr/sbin/nologin delibera
WORKDIR /application
# Des dépendances (changent rarement) au code de l'application (change à chaque version)
COPY --from=construction /construction/extracted/dependencies/ ./
COPY --from=construction /construction/extracted/spring-boot-loader/ ./
COPY --from=construction /construction/extracted/snapshot-dependencies/ ./
COPY --from=construction /construction/extracted/application/ ./
USER delibera
ENV SPRING_PROFILES_ACTIVE=prod,docker \
    TZ=UTC
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=120s --retries=4 \
  CMD curl -fsS -o /dev/null http://127.0.0.1:8080/actuator/health || exit 1
# Mémoire : 75 % de la limite du conteneur, sauf -Xmx explicite dans JAVA_TOOL_OPTIONS (VPS)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "application.jar"]
