# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy EVERYTHING (simplest + safest)
COPY . .

# Build only notification service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl suvidha-notification -am package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/suvidha-notification/target/*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
