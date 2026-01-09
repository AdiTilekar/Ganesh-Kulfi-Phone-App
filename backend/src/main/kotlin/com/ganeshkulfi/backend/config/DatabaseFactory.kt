package com.ganeshkulfi.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

/**
 * Day 14: Database Factory with HikariCP Connection Pool
 * Production-ready database configuration
 */
object DatabaseFactory {
    
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    fun init() {
        // Parse DATABASE_URL from Render (postgresql://user:pass@host:port/db)
        // or use legacy DB_URL format (jdbc:postgresql://...)
        val databaseUrl = System.getenv("DATABASE_URL")
        
        logger.info("🔍 DATABASE_URL present: ${databaseUrl != null}")
        if (databaseUrl != null) {
            logger.info("🔍 DATABASE_URL starts with: ${databaseUrl.take(20)}...")
        }
        
        val (dbUrl, dbUser, dbPassword) = if (databaseUrl != null && databaseUrl.startsWith("postgres")) {
            try {
                // Parse Render format: postgresql://user:password@host:port/database
                val uri = java.net.URI(databaseUrl)
                
                val host = uri.host ?: throw IllegalArgumentException("Host is null in DATABASE_URL")
                val port = if (uri.port > 0) uri.port else 5432
                val path = uri.path ?: throw IllegalArgumentException("Path is null in DATABASE_URL")
                val userInfo = uri.userInfo?.split(":") ?: throw IllegalArgumentException("UserInfo is null in DATABASE_URL")
                
                val url = "jdbc:postgresql://$host:$port$path"
                val user = userInfo.getOrNull(0) ?: throw IllegalArgumentException("Username not found in DATABASE_URL")
                val pass = userInfo.getOrNull(1) ?: throw IllegalArgumentException("Password not found in DATABASE_URL")
                
                logger.info("✅ Parsed DATABASE_URL successfully")
                Triple(url, user, pass)
            } catch (e: Exception) {
                logger.error("❌ Failed to parse DATABASE_URL: ${e.message}")
                throw e
            }
        } else {
            // Legacy format for local development
            logger.info("📝 Using legacy DB_URL format")
            Triple(
                System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/ganeshkulfi_db",
                System.getenv("DB_USER") ?: "ganeshkulfi_user",
                System.getenv("DB_PASSWORD") ?: "Ganesh@123"
            )
        }
        
        val poolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() 
            ?: 10
        
        logger.info("🔧 Initializing database connection pool...")
        logger.info("📍 Database URL: $dbUrl")
        logger.info("👤 Database User: $dbUser")
        logger.info("🏊 Pool Size: $poolSize")
        
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            maximumPoolSize = poolSize
            driverClassName = "org.postgresql.Driver"
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            // Connection pool settings
            connectionTimeout = 30000 // 30 seconds
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes
            
            // Performance optimizations
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        
        try {
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            logger.info("✅ Database connection pool initialized successfully")
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize database connection pool", e)
            throw e
        }
    }
}
