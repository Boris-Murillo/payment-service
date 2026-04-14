# ── Build ──────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

# Cache de dependencias (solo se re-ejecuta si cambia build.gradle)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# Compilar
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Run ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]