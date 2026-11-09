# Backend image (ADR-10). Build stage: the pinned Maven image; runtime: the pinned Temurin JRE image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /w
COPY pom.xml ./
RUN mvn -B -ntp -q dependency:go-offline
COPY src src
RUN mvn -B -ntp -q -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre
# SB-11: no root process
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /w/target/contact-groups-backend.jar app.jar
USER 10001
EXPOSE 8080
# ES-09: the container platform restarts a backend that reports DOWN
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
