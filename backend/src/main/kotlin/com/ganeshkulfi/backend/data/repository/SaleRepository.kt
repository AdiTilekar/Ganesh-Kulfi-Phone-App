package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.DailySale
import com.ganeshkulfi.backend.data.models.DailySales
import com.ganeshkulfi.backend.data.models.PaymentMethod
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Sale Repository
 * Handles database operations for daily_sales table
 */
class SaleRepository {

    /**
     * Map a ResultRow to a DailySale domain object.
     * The `profit` column is DB-generated (GENERATED ALWAYS AS STORED) — safe to read.
     */
    private fun rowToDailySale(row: ResultRow): DailySale = DailySale(
        id            = row[DailySales.id].toString(),
        userId        = row[DailySales.userId].value.toString(),
        productId     = row[DailySales.productId],
        productName   = row[DailySales.productName],
        quantity      = row[DailySales.quantity],
        costPrice     = row[DailySales.costPrice].toDouble(),
        sellPrice     = row[DailySales.sellPrice].toDouble(),
        profit        = row[DailySales.profit].toDouble(),
        paymentMethod = PaymentMethod.valueOf(row[DailySales.paymentMethod]),
        note          = row[DailySales.note],
        soldAt        = row[DailySales.soldAt],
        idempotencyKey = row[DailySales.idempotencyKey]
    )

    /**
     * Insert a new sale record.
     * NOTE: `profit` column is NOT set here — PostgreSQL computes it automatically.
     */
    fun create(sale: DailySale): DailySale = transaction {
        val insertedId = DailySales.insert { row ->
            row[id]             = UUID.fromString(sale.id)
            row[userId]         = UUID.fromString(sale.userId)
            row[productId]      = sale.productId
            row[productName]    = sale.productName
            row[quantity]       = sale.quantity
            row[costPrice]      = BigDecimal.valueOf(sale.costPrice)
            row[sellPrice]      = BigDecimal.valueOf(sale.sellPrice)
            // profit intentionally omitted — GENERATED ALWAYS AS STORED in DB
            row[paymentMethod]  = sale.paymentMethod.name
            row[note]           = sale.note
            row[soldAt]         = sale.soldAt
            row[idempotencyKey] = sale.idempotencyKey
        } get DailySales.id

        findById(insertedId.toString()) ?: throw IllegalStateException("Sale not found after insert")
    }

    /**
     * Find a sale by its UUID.
     */
    fun findById(id: String): DailySale? = transaction {
        DailySales.select { DailySales.id eq UUID.fromString(id) }
            .singleOrNull()
            ?.let { rowToDailySale(it) }
    }

    /**
     * Check for a duplicate idempotency key.
     */
    fun findByIdempotencyKey(key: String): DailySale? = transaction {
        DailySales.select { DailySales.idempotencyKey eq key }
            .singleOrNull()
            ?.let { rowToDailySale(it) }
    }

    /**
     * List all sales for a given UTC date, optionally scoped to a single user.
     */
    fun findByDate(date: LocalDate, userId: String? = null): List<DailySale> = transaction {
        val dayStart: Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        val dayEnd: Instant   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val query = DailySales.select {
            (DailySales.soldAt greaterEq dayStart) and (DailySales.soldAt less dayEnd)
        }

        if (userId != null) {
            query.andWhere { DailySales.userId eq UUID.fromString(userId) }
        }

        query.orderBy(DailySales.soldAt to SortOrder.DESC)
             .map { rowToDailySale(it) }
    }
}
