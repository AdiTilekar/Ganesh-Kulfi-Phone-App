package com.ganeshkulfi.backend.data.repository

import com.ganeshkulfi.backend.data.models.OrderStatus
import com.ganeshkulfi.backend.data.models.Orders
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Analytics Repository
 * Handles analytics queries for admin dashboard
 */
class AnalyticsRepository {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Get daily order count for last N days
     */
    fun getDailyOrderCount(days: Int = 30): List<DailyOrderCount> = transaction {
        val startDate = Instant.now().minusSeconds((days * 24 * 60 * 60).toLong())
        
        val ordersGrouped = Orders
            .slice(Orders.createdAt, Orders.id.count())
            .select { Orders.createdAt greaterEq startDate }
            .groupBy(Orders.createdAt)
            .map { row ->
                val instant = row[Orders.createdAt]
                val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val count = row[Orders.id.count()].toInt()
                date to count
            }
        
        // Group by date string and sum counts
        val dailyCounts = ordersGrouped
            .groupBy { it.first }
            .map { (date, entries) ->
                DailyOrderCount(
                    date = date.format(dateFormatter),
                    count = entries.sumOf { it.second }
                )
            }
            .sortedBy { it.date }
        
        // Fill missing dates with 0 count
        fillMissingDates(dailyCounts, days)
    }
    
    /**
     * Get daily sales (total amount) for last N days
     */
    fun getDailySales(days: Int = 30): List<DailySales> = transaction {
        val startDate = Instant.now().minusSeconds((days * 24 * 60 * 60).toLong())
        
        val salesGrouped = Orders
            .slice(Orders.createdAt, Orders.totalAmount)
            .select { Orders.createdAt greaterEq startDate }
            .map { row ->
                val instant = row[Orders.createdAt]
                val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                val amount = row[Orders.totalAmount].toDouble()
                date to amount
            }
        
        // Group by date string and sum amounts
        val dailySales = salesGrouped
            .groupBy { it.first }
            .map { (date, entries) ->
                DailySales(
                    date = date.format(dateFormatter),
                    totalSales = entries.sumOf { it.second }
                )
            }
            .sortedBy { it.date }
        
        // Fill missing dates with 0.0 sales
        fillMissingSalesDate(dailySales, days)
    }
    
    /**
     * Fill missing dates with zero count
     */
    private fun fillMissingDates(dailyCounts: List<DailyOrderCount>, days: Int): List<DailyOrderCount> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        
        val dateMap = dailyCounts.associateBy { it.date }
        val result = mutableListOf<DailyOrderCount>()
        
        var currentDate = startDate
        while (!currentDate.isAfter(today)) {
            val dateStr = currentDate.format(dateFormatter)
            result.add(
                dateMap[dateStr] ?: DailyOrderCount(dateStr, 0)
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return result
    }
    
    /**
     * Fill missing dates with zero sales
     */
    private fun fillMissingSalesDate(dailySales: List<DailySales>, days: Int): List<DailySales> {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        
        val dateMap = dailySales.associateBy { it.date }
        val result = mutableListOf<DailySales>()
        
        var currentDate = startDate
        while (!currentDate.isAfter(today)) {
            val dateStr = currentDate.format(dateFormatter)
            result.add(
                dateMap[dateStr] ?: DailySales(dateStr, 0.0)
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return result
    }
}

/**
 * Data class for daily order count
 */
data class DailyOrderCount(
    val date: String,
    val count: Int
)

/**
 * Data class for daily sales
 */
data class DailySales(
    val date: String,
    val totalSales: Double
)
