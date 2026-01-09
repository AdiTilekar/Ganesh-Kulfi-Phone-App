package com.ganeshkulfi.app.data.repository

import com.ganeshkulfi.app.data.model.StockTransaction
import com.ganeshkulfi.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockTransactionRepository @Inject constructor() {
    
    private val _transactions = MutableStateFlow<List<StockTransaction>>(emptyList())
    val transactionsFlow: Flow<List<StockTransaction>> = _transactions.asStateFlow()

    suspend fun getAllTransactions(): Result<List<StockTransaction>> {
        return try {
            Result.success(_transactions.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByRetailer(retailerId: String): Result<List<StockTransaction>> {
        return try {
            val transactions = _transactions.value.filter { it.retailerId == retailerId }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByFlavor(flavorId: String): Result<List<StockTransaction>> {
        return try {
            val transactions = _transactions.value.filter { it.flavorId == flavorId }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransaction(transaction: StockTransaction): Result<StockTransaction> {
        return try {
            val newTransaction = transaction.copy(
                id = "txn_${System.currentTimeMillis()}",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            _transactions.value = _transactions.value + newTransaction
            Result.success(newTransaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun giveStockToRetailer(
        retailerId: String,
        flavorId: String,
        flavorName: String,
        quantity: Int,
        unitPrice: Double,
        notes: String = ""
    ): Result<StockTransaction> {
        return try {
            val transaction = StockTransaction(
                retailerId = retailerId,
                flavorId = flavorId,
                flavorName = flavorName,
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = quantity * unitPrice,
                transactionType = TransactionType.GIVEN,
                notes = notes
            )
            addTransaction(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalStockGivenToRetailer(retailerId: String, flavorId: String): Int {
        return _transactions.value
            .filter { it.retailerId == retailerId && it.flavorId == flavorId }
            .sumOf { 
                when (it.transactionType) {
                    TransactionType.GIVEN -> it.quantity
                    TransactionType.RETURNED -> -it.quantity
                    else -> 0
                }
            }
    }

    suspend fun getRetailerOutstanding(retailerId: String): Double {
        return _transactions.value
            .filter { it.retailerId == retailerId }
            .sumOf { it.totalAmount }
    }

    fun getPendingPayments(): List<StockTransaction> {
        return _transactions.value.filter { 
            it.paymentStatus == com.ganeshkulfi.app.data.model.PaymentStatus.PENDING 
        }
    }

    suspend fun updatePaymentStatus(
        transactionId: String,
        status: com.ganeshkulfi.app.data.model.PaymentStatus
    ) {
        _transactions.value = _transactions.value.map { transaction ->
            if (transaction.id == transactionId) {
                transaction.copy(
                    paymentStatus = status,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                transaction
            }
        }
    }

    suspend fun createTransaction(
        retailerId: String,
        flavorId: String,
        quantity: Int,
        pricePerUnit: Double,
        totalAmount: Double,
        type: TransactionType
    ): Result<StockTransaction> {
        return try {
            val transaction = StockTransaction(
                id = "txn_${System.currentTimeMillis()}",
                retailerId = retailerId,
                flavorId = flavorId,
                flavorName = "", // Will be populated from flavor data
                quantity = quantity,
                unitPrice = pricePerUnit,
                totalAmount = totalAmount,
                transactionType = type,
                paymentStatus = com.ganeshkulfi.app.data.model.PaymentStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            _transactions.value = _transactions.value + transaction
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
