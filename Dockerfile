# Multi-stage build for Spring Boot 3 with Java 17
# Stage 1: Build the JAR using Maven wrapper
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy packaged JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
