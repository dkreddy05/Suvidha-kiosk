# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl suvidha-grievance -am package -DskipTests -q

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/suvidha-grievance/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
