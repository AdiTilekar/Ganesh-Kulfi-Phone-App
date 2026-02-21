# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ==================== GENERAL RULES ====================
# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ==================== KOTLIN ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ==================== JETPACK COMPOSE ====================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== DATA MODELS ====================
# Keep all data classes (for JSON serialization)
-keep class com.ganeshkulfi.app.data.model.** { *; }
-keepclassmembers class com.ganeshkulfi.app.data.model.** { *; }

# Keep data class constructors
-keepclassmembers class com.ganeshkulfi.app.data.model.** {
    <init>(...);
}

# Keep API DTOs (in data.remote package)
-keep class com.ganeshkulfi.app.data.remote.** { *; }
-keepclassmembers class com.ganeshkulfi.app.data.remote.** { *; }

# ==================== RETROFIT ====================
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== OKHTTP ====================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== GSON / KOTLINX SERIALIZATION ====================
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class kotlinx.serialization.** { *; }

# ==================== HILT (DEPENDENCY INJECTION) ====================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }

# ==================== ENCRYPTED SHARED PREFERENCES ====================
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltModules$** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ==================== COIL (IMAGE LOADING) ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ==================== FIREBASE (If you enable it later) ====================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ==================== REMOVE LOGGING IN RELEASE ====================
# Remove all Log calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ==================== APP SPECIFIC ====================
# Keep ApiService interface methods
-keep interface com.ganeshkulfi.app.data.remote.ApiService { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Repository classes
-keep class com.ganeshkulfi.app.data.repository.** { *; }

# Keep sealed classes
-keep class com.ganeshkulfi.app.util.Resource { *; }
-keep class com.ganeshkulfi.app.util.Resource$* { *; }
