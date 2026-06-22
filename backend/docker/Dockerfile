# ───────────────────────────────────────────────────────────────────────────────
# Stage 1 – Build JAR with Maven Wrapper
# ───────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Wrapper + POM zuerst, damit Dependency-Layer gecached wird
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src src
COPY checkstyle.xml ./
RUN ./mvnw -B -q clean package -DskipTests

# ───────────────────────────────────────────────────────────────────────────────
# Stage 2 – Slim Runtime Image
# ───────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-jar", "/app/app.jar"]
