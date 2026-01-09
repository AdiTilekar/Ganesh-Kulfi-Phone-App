# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

LABEL maintainer="Ganesh Kulfi Team"
LABEL description="Ganesh Kulfi Backend - Production Ready"

WORKDIR /app

# Copy gradle files from backend directory
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/gradle ./gradle
COPY backend/gradlew backend/gradlew.bat ./

# Copy source code from backend directory
COPY backend/src ./src

# Build the application
RUN chmod +x gradlew && ./gradlew clean shadowJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Create uploads directory
RUN mkdir -p uploads

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run the application
CMD ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
