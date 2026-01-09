# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

LABEL maintainer="Ganesh Kulfi Team"
LABEL description="Ganesh Kulfi Backend - Production Ready"

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

# Copy source code
COPY src ./src

# Build the application
RUN chmod +x gradlew && ./gradlew clean shadowJar --no-daemon

# Stage 2: Runtime
FROM openjdk:17-slim
WORKDIR /app

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
