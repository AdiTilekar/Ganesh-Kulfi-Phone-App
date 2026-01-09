package com.ganeshkulfi.backend.routes

import com.ganeshkulfi.backend.data.dto.CreatePriceOverrideRequest
import com.ganeshkulfi.backend.data.dto.UpdatePriceOverrideRequest
import com.ganeshkulfi.backend.data.models.UserRole
import com.ganeshkulfi.backend.services.PriceOverrideService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Day 9: Admin Price Override Routes
 * Admin-only price override management
 * NEVER exposed to retailers
 */
fun Route.adminPriceOverrideRoutes(priceOverrideService: PriceOverrideService) {
    
    authenticate("auth-jwt") {
        route("/api/admin/price-override") {
            
            // Create price override
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val request = call.receive<CreatePriceOverrideRequest>()

                try {
                    val response = priceOverrideService.createPriceOverride(request)
                    call.respond(HttpStatusCode.Created, response)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create price override"))
                }
            }

            // Get all price overrides
            get {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                try {
                    val response = priceOverrideService.getAllPriceOverrides()
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch price overrides"))
                }
            }

            // Get price override by ID
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val id = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                try {
                    val response = priceOverrideService.getPriceOverride(id)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch price override"))
                }
            }

            // Get price overrides by product ID
            get("/product/{productId}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val productId = call.parameters["productId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid product ID"))

                try {
                    val response = priceOverrideService.getPriceOverridesByProduct(productId)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch price overrides"))
                }
            }

            // Update price override
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val id = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                val request = call.receive<UpdatePriceOverrideRequest>()

                try {
                    val response = priceOverrideService.updatePriceOverride(id, request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update price override"))
                }
            }

            // Delete price override (hard delete)
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val id = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                try {
                    val deleted = priceOverrideService.deletePriceOverride(id)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Price override deleted successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Price override not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to delete price override"))
                }
            }

            // Deactivate price override (soft delete)
            patch("/{id}/deactivate") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()

                if (role != UserRole.ADMIN.name) {
                    return@patch call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                }

                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                try {
                    val deactivated = priceOverrideService.deactivatePriceOverride(id)
                    if (deactivated) {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Price override deactivated successfully"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Price override not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to deactivate price override"))
                }
            }
        }
    }
}
