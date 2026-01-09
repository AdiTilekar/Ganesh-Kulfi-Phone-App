/**
 * Ganesh Kulfi Backend - Build Configuration
 * Day 1: Ktor + PostgreSQL + Flyway + Exposed
 * Day 2: JWT Authentication + User Management
 */

val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.7"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "com.ganeshkulfi"
version = "0.0.10-SNAPSHOT"

// Set Java compatibility to 17 (for Render deployment)
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.ganeshkulfi.backend.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    
    // Content Negotiation (JSON support)
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    
    // CORS (for Android client)
    implementation("io.ktor:ktor-server-cors-jvm")
    
    // Day 11: Ktor Client for FCM Push Notifications
    implementation("io.ktor:ktor-client-core-jvm")
    implementation("io.ktor:ktor-client-cio-jvm")
    implementation("io.ktor:ktor-client-content-negotiation-jvm")
    
    // Call Logging
    implementation("io.ktor:ktor-server-call-logging-jvm")
    
    // Status Pages (error handling)
    implementation("io.ktor:ktor-server-status-pages-jvm")
    
    // JWT Authentication (Day 2)
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    
    // Password Hashing (Day 2)
    implementation("at.favre.lib:bcrypt:0.10.2")
    
    // Database Drivers
    implementation("org.postgresql:postgresql:42.7.1")  // PostgreSQL
    
    // Exposed ORM Framework
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")
    
    // Flyway Database Migration (v9.x supports PostgreSQL in core)
    implementation("org.flywaydb:flyway-core:9.22.3")
    
    // HikariCP Connection Pool
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

// Configure Kotlin to use Java 17 bytecode (compatible with Docker runtime)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
