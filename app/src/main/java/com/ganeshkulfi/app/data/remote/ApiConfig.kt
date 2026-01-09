package com.ganeshkulfi.app.data.remote

import com.ganeshkulfi.app.BuildConfig

object ApiConfig {
    // Automatically selects URL based on build variant
    // Debug: Local emulator (10.0.2.2:8080)
    // Release: Production on Render.com
    val BASE_URL: String = BuildConfig.BASE_URL
}
