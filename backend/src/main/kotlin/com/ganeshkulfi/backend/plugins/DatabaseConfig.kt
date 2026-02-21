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
        
        // --- resolve database coordinates -------------------------------------------
        // Priority: DATABASE_URL  >  JDBC_DATABASE_URL  >  individual DB_* vars  >  application.conf
        val databaseUrl = System.getenv("DATABASE_URL")
            ?: System.getenv("JDBC_DATABASE_URL")

        environment.log.info("🔍 DATABASE_URL present: ${databaseUrl != null}")

        val (jdbcUrl, user, password) = if (!databaseUrl.isNullOrBlank() && databaseUrl.startsWith("postgres")) {
            // Cloud format: postgres(ql)://user:password@host:port/database[?params]
            // Strip scheme prefix for uniform parsing ("postgres://" → "postgresql://")
            val normalized = if (databaseUrl.startsWith("postgresql://")) databaseUrl
                             else databaseUrl.replaceFirst("postgres://", "postgresql://")
            val uri = java.net.URI(normalized)
            val host = uri.host ?: throw IllegalArgumentException("Host is null in DATABASE_URL")
            val port = if (uri.port > 0) uri.port else 5432
            val path = uri.path?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("DB name missing in DATABASE_URL")
            val userInfo = uri.userInfo?.split(":", limit = 2)
                ?: throw IllegalArgumentException("UserInfo (user:pass) missing in DATABASE_URL")

            val dbUser = userInfo.getOrNull(0) ?: throw IllegalArgumentException("Username not found in DATABASE_URL")
            val dbPass = userInfo.getOrNull(1) ?: ""

            // Preserve any query params (sslmode, etc.) from the original URL
            val queryString = uri.rawQuery
            val sslParam = if (queryString != null && "sslmode" in queryString) ""
                           else "sslmode=require"   // Render requires SSL by default
            val separator = if (queryString != null) "&" else "?"
            val suffix = if (sslParam.isNotEmpty()) "$separator$sslParam" else ""
            val url = "jdbc:postgresql://$host:$port$path${if (queryString != null) "?$queryString" else ""}$suffix"

            environment.log.info("✅ Parsed DATABASE_URL → host=$host port=$port db=${path.removePrefix("/")}")
            Triple(url, dbUser, dbPass)

        } else if (!System.getenv("DB_HOST").isNullOrBlank()) {
            // Individual env-var override (works on any host without DATABASE_URL)
            val host = System.getenv("DB_HOST")!!
            val port = System.getenv("DB_PORT") ?: "5432"
            val dbName = System.getenv("DB_NAME") ?: "ganeshkulfi_db"
            val dbUser = System.getenv("DB_USER") ?: "ganeshkulfi_user"
            val dbPass = System.getenv("DB_PASSWORD") ?: ""
            val url = "jdbc:postgresql://$host:$port/$dbName"

            environment.log.info("📝 Using DB_HOST env vars → host=$host port=$port db=$dbName")
            Triple(url, dbUser, dbPass)

        } else {
            // Fallback: application.conf (local development only)
            val host = config.property("database.host").getString()
            val port = config.property("database.port").getString()
            val dbName = config.property("database.name").getString()
            val dbUser = config.property("database.user").getString()
            val dbPass = config.property("database.password").getString()
            
            val url = "jdbc:postgresql://$host:$port/$dbName"
            environment.log.warn("⚠️ No DATABASE_URL or DB_HOST found — falling back to application.conf (localhost). " +
                "Set DATABASE_URL on your cloud host!")
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
            
            // Connection pool settings — generous timeouts for cold-start cloud DBs
            this.connectionTimeout = 60000  // 60 seconds (Render free-tier DB can be slow to wake)
            this.initializationFailTimeout = 120000  // 2 minutes: retry during cold-start
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
