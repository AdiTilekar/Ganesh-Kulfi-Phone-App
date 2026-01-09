# Crash Reporting Setup Guide

## 🐛 Firebase Crashlytics Integration

### **Why Crashlytics?**
- Real-time crash reports
- Stack traces with line numbers
- User impact metrics
- Free tier available
- Easy Android integration

---

## 📦 Installation Steps

### **Step 1: Add Firebase to Project**

1. **Go to Firebase Console:** https://console.firebase.google.com/
2. **Create or select project:** "Ganesh Kulfi"
3. **Add Android app:**
   - Package name: `com.ganeshkulfi.app`
   - App nickname: "Ganesh Kulfi App"
   - Debug signing certificate (optional for testing)
4. **Download google-services.json**
5. **Place in:** `app/google-services.json`

---

### **Step 2: Update build.gradle.kts Files**

**Root `build.gradle.kts`:**
```kotlin
// Top-level build file
plugins {
    id("com.android.application") version "8.1.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
    id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false
    
    // Add Firebase
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}
```

**App `build.gradle.kts`:**
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    
    // Add Firebase plugins
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    // ... existing dependencies ...
    
    // Firebase BoM (Bill of Materials) - manages versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    
    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

---

### **Step 3: Initialize in Application Class**

Create `app/src/main/java/com/ganeshkulfi/app/CrashReportingManager.kt`:

```kotlin
package com.ganeshkulfi.app

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crash Reporting Manager
 * Centralized crash reporting and analytics
 */
object CrashReportingManager {
    
    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }
    
    /**
     * Initialize crash reporting
     * Call from Application.onCreate()
     */
    fun initialize(isDebugBuild: Boolean) {
        // Disable Crashlytics in debug builds to save quota
        crashlytics.setCrashlyticsCollectionEnabled(!isDebugBuild)
        
        if (!isDebugBuild) {
            Log.i("CrashReporting", "Crashlytics enabled for release build")
        } else {
            Log.i("CrashReporting", "Crashlytics disabled for debug build")
        }
    }
    
    /**
     * Set user identifier for crash reports
     * Call after successful login
     */
    fun setUserId(userId: Long, email: String, role: String) {
        crashlytics.setUserId(userId.toString())
        crashlytics.setCustomKey("email", email)
        crashlytics.setCustomKey("role", role)
        Log.d("CrashReporting", "User context set: $email ($role)")
    }
    
    /**
     * Clear user data
     * Call on logout
     */
    fun clearUserData() {
        crashlytics.setUserId("")
        crashlytics.setCustomKey("email", "")
        crashlytics.setCustomKey("role", "")
    }
    
    /**
     * Log non-fatal exception
     * Use for caught exceptions you want to track
     */
    fun logException(exception: Throwable, context: String? = null) {
        if (context != null) {
            crashlytics.setCustomKey("error_context", context)
        }
        crashlytics.recordException(exception)
        Log.e("CrashReporting", "Exception logged: ${exception.message}", exception)
    }
    
    /**
     * Log custom event
     */
    fun logEvent(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
        Log.d("CrashReporting", "Event logged: $key = $value")
    }
    
    /**
     * Force a crash (for testing)
     * ONLY USE IN DEBUG BUILDS
     */
    fun forceCrash() {
        throw RuntimeException("Test crash from Crashlytics")
    }
}
```

---

### **Step 4: Update GaneshKulfiApp.kt**

```kotlin
package com.ganeshkulfi.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GaneshKulfiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize crash reporting
        CrashReportingManager.initialize(BuildConfig.DEBUG)
    }
}
```

---

### **Step 5: Integrate with Authentication**

**Update `AuthRepository.kt`:**

```kotlin
suspend fun login(email: String, password: String): Result<AuthResponse> {
    return try {
        val response = apiService.login(LoginRequest(email, password))
        
        // Save token
        sharedPreferences.edit()
            .putString("auth_token", response.token)
            .putLong("user_id", response.user.id)
            .putString("user_role", response.user.role)
            .apply()
        
        // Set crash reporting context
        CrashReportingManager.setUserId(
            userId = response.user.id,
            email = response.user.email,
            role = response.user.role
        )
        
        Result.success(response)
    } catch (e: Exception) {
        // Log authentication errors
        CrashReportingManager.logException(e, "Authentication - Login")
        Result.failure(e)
    }
}

fun logout() {
    sharedPreferences.edit().clear().apply()
    CrashReportingManager.clearUserData()
}
```

---

### **Step 6: Add Error Logging to Repositories**

**Example in `RetailerRepository.kt`:**

```kotlin
private fun startAutoRefresh() {
    repositoryScope.launch {
        var attempt = 0
        while (isActive) {
            try {
                // Refresh data
                refreshInventory()
                delay(60_000)
                attempt = 0
            } catch (e: Exception) {
                attempt++
                val backoffTime = min(60_000L * attempt, 300_000L)
                android.util.Log.e("RetailerRepository", "Auto-refresh failed (attempt $attempt), retrying in ${backoffTime/1000}s", e)
                
                // Log to Crashlytics
                CrashReportingManager.logException(e, "RetailerRepository - Auto-refresh")
                
                delay(backoffTime)
            }
        }
    }
}
```

---

### **Step 7: Add Debug Test Button**

**Create test screen for crash testing (debug only):**

```kotlin
// In AdminScreen or SettingsScreen
if (BuildConfig.DEBUG) {
    Button(
        onClick = { 
            CrashReportingManager.forceCrash()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text("Test Crash (Debug Only)")
    }
}
```

---

## 🧪 Testing Crashlytics

### **Step 1: Build Release Build**

```powershell
# Build release APK with Crashlytics enabled
.\gradlew.bat assembleRelease
```

### **Step 2: Install on Device**

```powershell
adb install app\build\outputs\apk\release\app-release.apk
```

### **Step 3: Trigger Test Crash**

Option 1: Use test button (if in debug build)  
Option 2: Force crash manually in code  
Option 3: Cause a real error (network failure, null pointer, etc.)

### **Step 4: Verify in Firebase Console**

1. Go to Firebase Console → Crashlytics
2. Wait 5-10 minutes for crash to appear
3. Check crash report with stack trace

---

## 📊 Crash Report Analysis

### **What to Look For:**

1. **Crash-free users percentage** (target: >99%)
2. **Top crashes** by occurrence
3. **Affected devices** and OS versions
4. **User impact** (how many users affected)
5. **Stack traces** with line numbers

### **Common Issues to Track:**

- **Network errors:** Timeout, connection refused
- **Null pointer exceptions:** Missing null checks
- **Array index out of bounds:** List access errors
- **Out of memory:** Image loading, large lists
- **Concurrent modification:** Threading issues

---

## 🔔 Alert Configuration

### **Set Up Crash Alerts:**

1. Firebase Console → Crashlytics → Alerts
2. Enable email alerts for:
   - New issues (first occurrence)
   - Regressed issues (returns after fix)
   - Velocity alerts (spike in crashes)
3. Add team email addresses

### **Slack Integration:**

1. Firebase Console → Project Settings → Integrations
2. Connect Slack workspace
3. Choose channel for alerts
4. Configure alert rules

---

## 📈 Best Practices

### **DO:**
- ✅ Log user context (ID, role) after login
- ✅ Log non-fatal exceptions for tracking
- ✅ Add custom keys for debugging context
- ✅ Disable in debug builds to save quota
- ✅ Review crashes weekly
- ✅ Fix critical crashes immediately
- ✅ Group similar crashes together

### **DON'T:**
- ❌ Log sensitive data (passwords, tokens)
- ❌ Log PII without user consent
- ❌ Ignore non-fatal errors
- ❌ Leave test crashes in production
- ❌ Forget to obfuscate code (ProGuard)
- ❌ Overwhelm with too many custom keys

---

## 🔐 Privacy Considerations

### **GDPR Compliance:**

Crashlytics collects:
- Device identifiers
- Crash stack traces
- App version
- Device model and OS
- User ID (if you set it)

**Required actions:**
1. Update Privacy Policy to mention crash reporting
2. Allow users to opt-out if required
3. Don't log PII in custom keys
4. Use anonymized user IDs if possible

---

## 🚀 Alternative Solutions

If you prefer not to use Firebase:

### **1. Sentry**
- Open source option available
- Self-hosted or cloud
- More features than Crashlytics
- https://sentry.io/

### **2. Bugsnag**
- Similar to Crashlytics
- Better error grouping
- https://www.bugsnag.com/

### **3. Rollbar**
- Real-time error tracking
- Source map support
- https://rollbar.com/

### **4. Instabug**
- Crash reporting + bug reporting
- In-app feedback
- https://instabug.com/

---

## 📞 Support

**Firebase Crashlytics Docs:** https://firebase.google.com/docs/crashlytics  
**Android Integration Guide:** https://firebase.google.com/docs/crashlytics/get-started?platform=android  
**Troubleshooting:** https://firebase.google.com/docs/crashlytics/test-implementation

---

**Last Updated:** January 4, 2026  
**Status:** Ready for implementation
