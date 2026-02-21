# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

LABEL maintainer="Ganesh Kulfi Team"
LABEL description="Ganesh Kulfi Backend - Production Ready"

WORKDIR /app

# Copy gradle wrapper + build files first (these change rarely → better layer cache)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

# Download dependencies as a separate cached layer
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Now copy source code (this layer invalidates on every code change)
COPY src ./src

# Build the application
RUN ./gradlew clean shadowJar --no-daemon

# Stage 2: Runtime (use eclipse-temurin for smaller + better maintained image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*-all.jar app.jar

# Create uploads directory with correct ownership
RUN mkdir -p uploads && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run the application
CMD ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
