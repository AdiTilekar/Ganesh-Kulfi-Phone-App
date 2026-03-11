package com.ganeshkulfi.salelog.data

import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepository @Inject constructor(
    private val api: ApiService
) {

    suspend fun getProducts(): Result<List<Product>> = runCatching {
        val resp = api.getProducts()
        if (resp.isSuccessful)
            resp.body()?.data?.products ?: error("Empty products response")
        else
            error("Products error ${resp.code()}: ${resp.message()}")
    }

    suspend fun createSale(request: CreateSaleRequest): Result<SaleResponse> = runCatching {
        val resp = api.createSale(request)
        if (resp.isSuccessful)
            resp.body()?.data ?: error("Empty sale response")
        else
            error("Create sale error ${resp.code()}: ${resp.message()}")
    }

    suspend fun getSales(date: String): Result<DailySummaryResponse> = runCatching {
        val resp = api.getSales(date)
        if (resp.isSuccessful)
            resp.body()?.data ?: error("Empty summary response")
        else
            error("Get sales error ${resp.code()}: ${resp.message()}")
    }

    suspend fun exportSales(date: String): Result<ResponseBody> = runCatching {
        val resp = api.exportSales(date)
        if (resp.isSuccessful) resp.body()!!
        else error("Export error ${resp.code()}: ${resp.message()}")
    }
}
