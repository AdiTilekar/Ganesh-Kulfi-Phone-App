package com.ganeshkulfi.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import com.ganeshkulfi.app.data.remote.ApiService
import com.ganeshkulfi.app.data.remote.CreateSaleRequest
import com.ganeshkulfi.app.data.remote.DailySalesSummaryResponse
import com.ganeshkulfi.app.data.remote.SaleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaleRepository @Inject constructor(
    private val apiService: ApiService,
    private val sharedPreferences: SharedPreferences
) {
    private fun bearerToken(): String =
        "Bearer ${sharedPreferences.getString("auth_token", "") ?: ""}"

    suspend fun addSale(request: CreateSaleRequest): Result<SaleResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.createSale(bearerToken(), request)
                if (response.isSuccessful) {
                    response.body()?.data ?: error("Empty response body")
                } else {
                    error("Server error ${response.code()}: ${response.errorBody()?.string()}")
                }
            }
        }

    suspend fun getSalesForDate(date: String): Result<DailySalesSummaryResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.getSalesForDate(bearerToken(), date)
                if (response.isSuccessful) {
                    response.body()?.data ?: error("Empty response body")
                } else {
                    error("Server error ${response.code()}: ${response.errorBody()?.string()}")
                }
            }
        }

    /**
     * Download sales Excel file and save it to the device Downloads folder.
     * Returns the path of the saved file.
     */
    suspend fun exportSales(context: Context, date: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiService.exportSales(bearerToken(), date)
                if (!response.isSuccessful) {
                    error("Export failed ${response.code()}: ${response.errorBody()?.string()}")
                }
                val bytes = response.body()?.bytes() ?: error("Empty export response")
                val filename = "sales_${date}.xlsx"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+: use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, filename)
                        put(MediaStore.Downloads.MIME_TYPE,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                    ) ?: error("Failed to create MediaStore entry")

                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }

                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)

                    uri.toString()
                } else {
                    // Pre-Q: write directly to Downloads directory
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    downloadsDir.mkdirs()
                    val file = File(downloadsDir, filename)
                    FileOutputStream(file).use { it.write(bytes) }
                    file.absolutePath
                }
            }
        }
}
