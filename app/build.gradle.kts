plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    // id("com.google.gms.google-services")  // Firebase - Uncomment when ready to connect
}

android {
    namespace = "com.ganeshkulfi.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ganeshkulfi.app"
        minSdk = 24
        targetSdk = 34
        
        // Version management - increment before each release
        versionCode = getVersionCode()
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false  // Disabled for easier debugging
            isShrinkResources = false
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = false  // Disabled due to launcher icon issues
            buildConfigField("String", "BASE_URL", "\"https://ganesh-kulfi-backend.onrender.com\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for BASE_URL
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Room Database — currently unused (no @Entity/@Dao defined); uncomment when needed
    // implementation("androidx.room:room-runtime:2.6.1")
    // implementation("androidx.room:room-ktx:2.6.1")
    // ksp("androidx.room:room-compiler:2.6.1")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // EncryptedSharedPreferences for secure token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Accompanist Permissions — currently unused; uncomment when permission handling is added
    // implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Firebase & Google Sign-In (Commented out until Firebase is connected)
    // Uncomment these when you're ready to enable OAuth:
    // implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    // implementation("com.google.firebase:firebase-auth-ktx")
    // implementation("com.google.android.gms:play-services-auth:20.7.0")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

/**
 * Version Management System
 * Automatically increments version based on Git commits
 * Format: MAJOR.MINOR.PATCH (versionCode = MAJOR * 10000 + MINOR * 100 + PATCH)
 */
fun getVersionCode(): Int {
    // Read from version.properties or calculate from git
    val major = 1
    val minor = 0
    val patch = 0
    return major * 10000 + minor * 100 + patch  // 10000
}

fun getVersionName(): String {
    val major = 1
    val minor = 0
    val patch = 0
    return "$major.$minor.$patch"  // "1.0.0"
}
