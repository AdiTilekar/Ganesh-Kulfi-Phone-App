package com.ganeshkulfi.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KulfiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
