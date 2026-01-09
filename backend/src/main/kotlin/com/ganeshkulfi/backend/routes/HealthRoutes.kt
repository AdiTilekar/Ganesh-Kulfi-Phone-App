package com.ganeshkulfi.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.Table
import com.ganeshkulfi.backend.utils.requireAdmin

/**
 * Health Check Routes
 * Used to verify backend is running and database is connected
 */

@Serializable
data class HealthResponse(
    val status: String,
    val message: String,
    val timestamp: Long,
    val database: String
)

fun Route.healthRoutes() {
    /**
     * GET /
     * Welcome page - API documentation
     */
    get("/") {
        call.respondText("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Ganesh Kulfi API</title>
                <style>
                    body { 
                        font-family: 'Segoe UI', Arial, sans-serif; 
                        max-width: 1200px; 
                        margin: 50px auto; 
                        padding: 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: #333;
                    }
                    .container {
                        background: white;
                        border-radius: 15px;
                        padding: 40px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.3);
                    }
                    h1 { 
                        color: #667eea; 
                        border-bottom: 3px solid #667eea; 
                        padding-bottom: 10px;
                        margin-bottom: 30px;
                    }
                    h2 { color: #764ba2; margin-top: 30px; }
                    .endpoint { 
                        background: #f8f9fa; 
                        padding: 15px; 
                        margin: 10px 0; 
                        border-left: 4px solid #667eea;
                        border-radius: 5px;
                    }
                    .method { 
                        display: inline-block;
                        background: #28a745; 
                        color: white; 
                        padding: 5px 10px; 
                        border-radius: 5px;
                        font-weight: bold;
                        min-width: 60px;
                        text-align: center;
                    }
                    .method.post { background: #007bff; }
                    .method.patch { background: #ffc107; }
                    .method.delete { background: #dc3545; }
                    .path { 
                        color: #495057; 
                        font-family: monospace; 
                        margin-left: 10px;
                    }
                    .description { 
                        color: #6c757d; 
                        margin-top: 5px;
                        font-size: 14px;
                    }
                    .status { 
                        background: #d4edda; 
                        color: #155724; 
                        padding: 15px; 
                        border-radius: 5px;
                        margin: 20px 0;
                        border: 1px solid #c3e6cb;
                    }
                    .flavor-count {
                        font-size: 24px;
                        font-weight: bold;
                        color: #667eea;
                    }
                    .protected {
                        display: inline-block;
                        background: #ffc107;
                        color: #333;
                        padding: 2px 8px;
                        border-radius: 3px;
                        font-size: 12px;
                        margin-left: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🍦 Ganesh Kulfi Backend API</h1>
                    
                    <div class="status">
                        ✅ <strong>Status:</strong> Running on http://localhost:8080<br>
                        📦 <strong>Version:</strong> 0.0.3-SNAPSHOT (Day 3)<br>
                        🍦 <strong>Kulfi Flavors:</strong> <span class="flavor-count">13</span> authentic flavors loaded!
                    </div>
                    
                    <h2>📋 Available Endpoints</h2>
                    
                    <h3>🏥 Health & Status</h3>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/health</span>
                        <div class="description">Check backend health and database connection</div>
                    </div>
                    
                    <h3>🔐 Authentication</h3>
                    <div class="endpoint">
                        <span class="method post">POST</span>
                        <span class="path">/api/auth/login</span>
                        <div class="description">Login with email & password → Get JWT token</div>
                    </div>
                    
                    <h3>🍦 Products (Public)</h3>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/products</span>
                        <div class="description">Get all available kulfi flavors (for Android app)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/products/{id}</span>
                        <div class="description">Get product by ID (e.g., /api/products/mango)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/products/category/{category}</span>
                        <div class="description">Filter by category: FRUIT, PREMIUM, CLASSIC, FUSION</div>
                    </div>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/products/seasonal</span>
                        <div class="description">Get seasonal kulfi flavors</div>
                    </div>
                    
                    <h3>🔒 Products (Admin Only)</h3>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/products/all</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Get all products including unavailable (Admin only)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method post">POST</span>
                        <span class="path">/api/products</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Create new product (Admin only)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method patch">PATCH</span>
                        <span class="path">/api/products/{id}</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Update product (Admin only)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method patch">PATCH</span>
                        <span class="path">/api/products/{id}/stock</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Update stock quantity (Admin only)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method delete">DELETE</span>
                        <span class="path">/api/products/{id}</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Delete product (Admin only)</div>
                    </div>
                    
                    <h3>👥 Users (Admin Only)</h3>
                    <div class="endpoint">
                        <span class="method">GET</span>
                        <span class="path">/api/users</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">List all users (Admin only)</div>
                    </div>
                    <div class="endpoint">
                        <span class="method post">POST</span>
                        <span class="path">/api/users</span>
                        <span class="protected">🔐 JWT Required</span>
                        <div class="description">Create new user (Admin only)</div>
                    </div>
                    
                    <h2>🧪 Quick Test</h2>
                    <div class="endpoint">
                        <strong>Try it now:</strong> 
                        <a href="/api/products" target="_blank">View All Kulfi Flavors →</a>
                    </div>
                    <div class="endpoint">
                        <strong>Health Check:</strong> 
                        <a href="/api/health" target="_blank">Check Backend Status →</a>
                    </div>
                    
                    <h2>📱 13 Kulfi Flavors</h2>
                    <p>Mango • Rabadi • Strawberry • Chocolate • Paan • Gulkand • Dry Fruit • Pineapple • Chikoo • Guava • Jamun • Sitafal • Fig</p>
                    
                    <p style="text-align: center; margin-top: 40px; color: #6c757d;">
                        <strong>Shree Ganesh Kulfi</strong> - Kopargaon<br>
                        <em>Preserving tradition, embracing technology</em>
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent(), ContentType.Text.Html)
    }
    
    route("/api") {
        /**
         * GET/HEAD /api/health
         * Simple health check endpoint (supports both GET and HEAD for monitoring)
         */
        route("/health") {
            get {
                try {
                    // Test database connection
                    val dbStatus = transaction {
                        // Simple query to verify DB connection
                        val result = exec("SELECT 1") { rs ->
                            rs.next()
                            rs.getInt(1)
                        }
                        if (result == 1) "connected" else "error"
                    }
                    
                    call.respond(
                        HttpStatusCode.OK,
                        HealthResponse(
                            status = "healthy",
                            message = "Ganesh Kulfi Backend is running!",
                            timestamp = System.currentTimeMillis(),
                            database = dbStatus ?: "error"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        HealthResponse(
                            status = "unhealthy",
                            message = "Database connection failed: ${e.message}",
                            timestamp = System.currentTimeMillis(),
                            database = "disconnected"
                        )
                    )
                }
            }
            
            // HEAD method support for UptimeRobot free tier
            head {
                try {
                    // Quick DB check
                    transaction {
                        exec("SELECT 1") { rs -> rs.next() }
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }
            }
        }
        
        /**
         * GET /api/health/db
         * Detailed database health check
         */
        get("/health/db") {
            try {
                val userCount = transaction {
                    // Count users in database
                    exec("SELECT COUNT(*) FROM app_user") { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
                
                call.respondText(
                    "✅ Database connected!\n" +
                    "Total users in database: $userCount\n" +
                    "Connection: PostgreSQL\n" +
                    "Status: Healthy",
                    ContentType.Text.Plain,
                    HttpStatusCode.OK
                )
            } catch (e: Exception) {
                call.respondText(
                    "❌ Database error: ${e.message}",
                    ContentType.Text.Plain,
                    HttpStatusCode.ServiceUnavailable
                )
            }
        }
    }
}
