package com.ganeshkulfi.salelog.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("api/products")
    suspend fun getProducts(): Response<ApiWrapper<ProductsListData>>

    @POST("api/sales")
    suspend fun createSale(@Body request: CreateSaleRequest): Response<ApiWrapper<SaleResponse>>

    @GET("api/sales")
    suspend fun getSales(@Query("date") date: String): Response<ApiWrapper<DailySummaryResponse>>

    @GET("api/sales/export")
    @Streaming
    suspend fun exportSales(@Query("date") date: String): Response<ResponseBody>
}
