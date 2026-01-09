package com.ganeshkulfi.backend

import com.ganeshkulfi.backend.config.DatabaseFactory
import com.ganeshkulfi.backend.config.configureLogging
import com.ganeshkulfi.backend.data.repository.UserRepository
import com.ganeshkulfi.backend.data.repository.ProductRepository
import com.ganeshkulfi.backend.data.repository.InventoryRepository
import com.ganeshkulfi.backend.data.repository.OrderRepository
import com.ganeshkulfi.backend.data.repository.OrderTimelineRepository
import com.ganeshkulfi.backend.data.repository.PriceOverrideRepository
import com.ganeshkulfi.backend.data.repository.StatusHistoryRepository
import com.ganeshkulfi.backend.data.repository.AnalyticsRepository
import com.ganeshkulfi.backend.plugins.*
import com.ganeshkulfi.backend.routes.*
import com.ganeshkulfi.backend.services.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.io.File

/**
 * Ganesh Kulfi Backend Application
 * Day 1: Basic setup with Ktor + PostgreSQL + Flyway
 * Day 2: JWT Authentication + User Management
 * Day 3: Product Management + 13 Kulfi Flavors
 * Day 4: Inventory Ledger System (Immutable)
 * Day 5: Order Confirmation System with Status Management
 * Day 6: Inventory Management with Stock Reservation
 * Day 7: Order History, Filtering, Analytics Dashboard
 * Day 8: Idempotent Orders, Validation, Transaction Safety
 * Day 9: Advanced Pricing System (Server-Side Only, Retailer Hidden)
 * Day 10: Full Order Module Completion + Admin Order Dashboard
 * Day 11: Order Timeline + Polling-based Updates (No Firebase)
 * Day 12: Enhanced Analytics Dashboard
 * Day 13: Admin Product Management Enhancements
 * Day 14: Final Deployment Setup (Production Ready)
 */

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Print startup banner
    printBanner()
    
    // Day 14: Create uploads directory
    val uploadsDir = File(System.getenv("UPLOADS_DIR") ?: "uploads")
    if (!uploadsDir.exists()) {
        log.info("📁 Creating uploads directory: ${uploadsDir.absolutePath}")
        uploadsDir.mkdirs()
    }
    
    // Day 14: Initialize database with HikariCP connection pool
    log.info("🔧 Initializing database connection pool...")
    DatabaseFactory.init()
    
    // Run Flyway migrations
    log.info("🔧 Running database migrations...")
    DatabaseConfig.init(environment)
    
    // Initialize services
    log.info("🔧 Initializing services...")
    val jwtService = JWTService(environment.config)
    val passwordService = PasswordService()
    val userRepository = UserRepository()
    val userService = UserService(userRepository, passwordService, jwtService)
    val productRepository = ProductRepository()
    val productService = ProductService(productRepository)
    val inventoryRepository = InventoryRepository()
    val inventoryService = InventoryService(inventoryRepository, productRepository)
    
    // Day 9: Pricing Service
    val pricingService = PricingService()
    val priceOverrideRepository = PriceOverrideRepository()
    val priceOverrideService = PriceOverrideService(priceOverrideRepository, productRepository)
    
    // Day 10: Order Management with Status History
    val orderRepository = OrderRepository()
    val statusHistoryRepository = StatusHistoryRepository()
    val orderHistoryService = OrderHistoryService(orderRepository, statusHistoryRepository)
    val orderManagementService = OrderManagementService(orderRepository, statusHistoryRepository)
    val orderService = OrderService(orderRepository, inventoryService, pricingService, productRepository)
    
    // Day 11: Order Timeline and Simple Notifications (No Firebase)
    val notificationService = NotificationService()
    val orderTimelineRepository = OrderTimelineRepository()
    
    // Day 12: Enhanced Analytics Dashboard
    val analyticsRepository = AnalyticsRepository()
    val analyticsService = AnalyticsService(orderRepository, productRepository, analyticsRepository)
    
    // Configure plugins
    log.info("🔧 Configuring Ktor plugins...")
    configureSerialization()
    configureCORS()
    configureLogging() // Day 14: Production logging
    configureAuthentication(jwtService)
    
    // Configure routes
    log.info("🔧 Configuring API routes...")
    routing {
        healthRoutes()
        authRoutes(userService)
        userRoutes(userService)
        productRoutes(productService)
        orderRoutes(orderService, userService)
        retailerOrderRoutes(orderService)
        adminOrderRoutes(orderService)
        factoryRoutes(productService, inventoryService)
        adminAnalyticsRoutes(analyticsService)
        adminPriceOverrideRoutes(priceOverrideService) // Day 9
        factoryOrderStatusRoutes(orderService, orderRepository, orderTimelineRepository, userRepository, notificationService, productRepository) // Day 11: Order Status with Logging + Auto Stock Reduction
        orderPollingRoutes(orderRepository, orderTimelineRepository) // Day 11: Order polling for updates
        analyticsRoutes(analyticsService) // Day 12: Enhanced Analytics Dashboard
    }
    
    log.info("✅ Ganesh Kulfi Backend is ready!")
    log.info("📍 Server running at: http://localhost:8080")
    log.info("🏥 Health check: http://localhost:8080/api/health")
    log.info("🔐 Auth endpoints: http://localhost:8080/api/auth/*")
    log.info("👥 User endpoints: http://localhost:8080/api/users/*")
    log.info("🍦 Product endpoints: http://localhost:8080/api/products/*")
    log.info("📋 Order endpoints: http://localhost:8080/api/orders/*")
    log.info("📊 Retailer history: http://localhost:8080/api/retailer/orders/history")
    log.info("🚫 Retailer cancel: http://localhost:8080/api/retailer/orders/:id/cancel")
    log.info("🏭 Factory orders: http://localhost:8080/api/factory/orders/*")
    log.info("🏭 Factory products: http://localhost:8080/api/factory/products/*")
    log.info("📈 Admin analytics: http://localhost:8080/api/admin/analytics/*")
    log.info("⚡ Admin orders: http://localhost:8080/api/admin/orders/*")
    log.info("🛑 Admin cancel: http://localhost:8080/api/admin/orders/:id/cancel")
    log.info("💰 Admin pricing: http://localhost:8080/api/admin/price-override/*")
    log.info("📦 Order status: http://localhost:8080/api/orders/:id/confirm|pack|out-for-delivery|deliver")
    log.info("📜 Order timeline: http://localhost:8080/api/orders/:id/timeline")
    log.info("🔄 Order polling: http://localhost:8080/api/retailer/orders/updates")
}

fun Application.printBanner() {
    environment.log.info("""
        
    ╔═══════════════════════════════════════════════╗
    ║                                               ║
    ║        🍦 GANESH KULFI BACKEND 🍦             ║
    ║                                               ║
    ║   Authentic Taste of Tradition - Now Digital ║
    ║                                               ║
    ╚═══════════════════════════════════════════════╝
    
    Day 1: Ktor + PostgreSQL + Flyway + Exposed
    Day 2: JWT Authentication + User Management
    Day 3: Product Management + 13 Kulfi Flavors
    Day 4: Inventory Ledger System (Immutable)
    Day 5: Order Confirmation System
    Day 6: Inventory Management with Stock Reservation
    Day 7: Order History, Filtering, Analytics Dashboard
    Day 8: Idempotent Orders, Validation, Transaction Safety
    Day 9: Advanced Pricing System (Retailer Tiers Hidden)
    Day 10: Order Status History + Admin Cancellation
    Day 11: Order Timeline + Polling-based Updates (No Firebase)
    Version: 0.0.11-SNAPSHOT
    
    """.trimIndent())
}
