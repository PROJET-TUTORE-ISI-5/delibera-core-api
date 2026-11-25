# Image de l'API. Construction : docker build -t delibera-core-api .
FROM maven:3.9-eclipse-temurin-21 AS construction
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src/ src/
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 delibera
WORKDIR /app
COPY --from=construction /app/target/delibera-core-api-*.jar app.jar
USER delibera
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=UTC
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
