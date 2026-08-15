# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only what's needed to resolve dependencies first, so this layer is
# cached and skipped on rebuilds where only src/ changed (i.e. every normal
# code change) instead of re-downloading the entire Maven dependency tree.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Pre-download dependencies into their own layer, so later layers (which
# change on every code edit) don't invalidate this one.
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user rather than the image's default root — limits
# blast radius if the JVM process is ever compromised.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
