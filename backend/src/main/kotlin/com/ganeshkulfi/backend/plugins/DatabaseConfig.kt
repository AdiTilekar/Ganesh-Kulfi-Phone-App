package com.ganeshkulfi.backend.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

/**
 * Database Configuration
 * Sets up database connection with HikariCP connection pool
 * and runs Flyway migrations
 * 
 * Database: PostgreSQL 14+
 */
object DatabaseConfig {
    
    fun init(environment: ApplicationEnvironment) {
        val config = environment.config
        
        // Parse DATABASE_URL from Render or use application.conf
        val databaseUrl = System.getenv("DATABASE_URL")
        val (jdbcUrl, user, password) = if (databaseUrl != null && databaseUrl.startsWith("postgres")) {
            // Parse Render format: postgresql://user:password@host:port/database
            val uri = java.net.URI(databaseUrl)
            val host = uri.host ?: throw IllegalArgumentException("Host is null in DATABASE_URL")
            val port = if (uri.port > 0) uri.port else 5432
            val path = uri.path ?: throw IllegalArgumentException("Path is null in DATABASE_URL")
            val userInfo = uri.userInfo?.split(":") ?: throw IllegalArgumentException("UserInfo is null in DATABASE_URL")
            
            val url = "jdbc:postgresql://$host:$port$path"
            val dbUser = userInfo.getOrNull(0) ?: throw IllegalArgumentException("Username not found in DATABASE_URL")
            val dbPass = userInfo.getOrNull(1) ?: throw IllegalArgumentException("Password not found in DATABASE_URL")
            
            environment.log.info("✅ Parsed DATABASE_URL for migrations")
            Triple(url, dbUser, dbPass)
        } else {
            // Read database config from application.conf (local development)
            val host = config.property("database.host").getString()
            val port = config.property("database.port").getString()
            val dbName = config.property("database.name").getString()
            val dbUser = config.property("database.user").getString()
            val dbPass = config.property("database.password").getString()
            
            val url = "jdbc:postgresql://$host:$port/$dbName"
            environment.log.info("📝 Using application.conf for database config")
            Triple(url, dbUser, dbPass)
        }
        
        val maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10
        val driverClass = "org.postgresql.Driver"
        
        // Configure HikariCP connection pool
        val dataSource = createDataSource(jdbcUrl, user, password, maxPoolSize, driverClass)
        
        // Run Flyway migrations
        environment.log.info("🔄 Running Flyway migrations...")
        runMigrations(dataSource)
        environment.log.info("✅ Flyway migrations completed")
        
        // Connect Exposed ORM
        Database.connect(dataSource)
        
        environment.log.info("✅ PostgreSQL database connected: $jdbcUrl")
    }
    
    private fun createDataSource(
        jdbcUrl: String,
        user: String,
        password: String,
        maxPoolSize: Int,
        driverClass: String
    ): HikariDataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            this.maximumPoolSize = maxPoolSize
            this.driverClassName = driverClass
            
            // Connection pool settings
            this.connectionTimeout = 30000 // 30 seconds
            this.idleTimeout = 600000 // 10 minutes
            this.maxLifetime = 1800000 // 30 minutes
            
            // Performance tuning
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            // Pool naming
            this.poolName = "GaneshKulfiHikariPool"
        }
        
        return HikariDataSource(config)
    }
    
    private fun runMigrations(dataSource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateOnMigrate(false)  // Allow modified migrations (V2 was updated to match Android app)
            .load()
        
        val migrationInfo = flyway.info()
        val pending = migrationInfo.pending().size
        
        if (pending > 0) {
            flyway.migrate()
        } else {
        }
    }
}
