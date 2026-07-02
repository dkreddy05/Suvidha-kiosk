# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy EVERYTHING (simplest + safest)
COPY . .

# Build only auth service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl suvidha-auth -am package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/suvidha-auth/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]