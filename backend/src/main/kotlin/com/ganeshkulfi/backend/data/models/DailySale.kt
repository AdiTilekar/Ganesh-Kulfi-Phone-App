package com.ganeshkulfi.backend.data.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Payment methods supported at point of sale
 */
enum class PaymentMethod {
    CASH,
    UPI,
    CREDIT
}

/**
 * DailySales Table - Exposed ORM
 * Maps to the daily_sales PostgreSQL table (V21 migration)
 *
 * Note: The `profit` column is defined as GENERATED ALWAYS AS STORED in PostgreSQL.
 * We include it here as a read-only column; it must never be set in INSERT blocks.
 */
object DailySales : UUIDTable("daily_sales") {
    val userId          = reference("user_id", Users)
    val productId       = varchar("product_id", 36)
    val productName     = varchar("product_name", 100)
    val quantity        = integer("quantity")
    val costPrice       = decimal("cost_price", 10, 2)
    val sellPrice       = decimal("sell_price", 10, 2)
    // DB-generated column — read only; never set in INSERT
    val profit          = decimal("profit", 10, 2)
    val paymentMethod   = varchar("payment_method", 20).default("CASH")
    val note            = text("note").nullable()
    val soldAt          = timestamp("sold_at").default(Instant.now())
    val idempotencyKey  = varchar("idempotency_key", 64).nullable().uniqueIndex()
}

/**
 * DailySale Domain Model
 */
data class DailySale(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val profit: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String? = null,
    val soldAt: Instant = Instant.now(),
    val idempotencyKey: String? = null
)
