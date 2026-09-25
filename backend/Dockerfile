# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
# Actuator listens on the management port (8081) under the prod profile and on 8080 otherwise; healthy if either answers.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s CMD wget -qO- http://localhost:8081/actuator/health/readiness || wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
# MaxRAMPercentage sizes the heap from the container memory limit instead of the host's RAM.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
