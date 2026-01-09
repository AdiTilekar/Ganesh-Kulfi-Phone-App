package com.ganeshkulfi.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==================== BRAND COLORS (Kulfi Theme) ====================

// Primary - Creamy Orange (Mango Kulfi)
val KulfiOrange = Color(0xFFFF9F43)
val KulfiOrangeLight = Color(0xFFFFBF7A)
val KulfiOrangeDark = Color(0xFFE67E22)
val KulfiOrangeContainer = Color(0xFFFFE5CC)

// Secondary - Cool Mint (Refreshing)
val KulfiMint = Color(0xFF4ECDC4)
val KulfiMintLight = Color(0xFF7EDDD6)
val KulfiMintDark = Color(0xFF26A69A)
val KulfiMintContainer = Color(0xFFE0F7F6)

// Tertiary - Strawberry Pink (Sweet)
val KulfiPink = Color(0xFFFF6B9D)
val KulfiPinkLight = Color(0xFFFF9DBD)
val KulfiPinkDark = Color(0xFFE64A7A)
val KulfiPinkContainer = Color(0xFFFFE5F0)

// ==================== STATUS COLORS ====================

// Order Status
val StatusPending = Color(0xFFFF9F43)        // Warm Orange
val StatusConfirmed = Color(0xFF26DE81)      // Fresh Green
val StatusPacked = Color(0xFF45B7D1)         // Info Blue
val StatusOutForDelivery = Color(0xFF8B5CF6) // Purple
val StatusDelivered = Color(0xFF10B981)      // Dark Green
val StatusCancelled = Color(0xFFEE5A6F)      // Cherry Red

// Stock Alerts
val StockInStock = Color(0xFF26DE81)         // Success Green
val StockLowStock = Color(0xFFFFC312)        // Golden Yellow
val StockOutOfStock = Color(0xFFEE5A6F)      // Error Red

// ==================== PRICING TIER COLORS ====================

val TierGold = Color(0xFFFFD700)            // Luxury Gold
val TierGoldLight = Color(0xFFFFE55C)
val TierGoldContainer = Color(0xFFFFF4CC)

val TierSilver = Color(0xFFC0C0C0)          // Silver Gray
val TierSilverLight = Color(0xFFD9D9D9)
val TierSilverContainer = Color(0xFFF0F0F0)

val TierBronze = Color(0xFFCD7F32)          // Bronze Brown
val TierBronzeLight = Color(0xFFDEA15C)
val TierBronzeContainer = Color(0xFFFFEBD9)

// ==================== UI COLORS ====================

// Background colors
val CreamBackground = Color(0xFFFFFBF5)     // Warm, inviting
val CreamLight = Color(0xFFFFFAF0)
val CreamDark = Color(0xFFF5F0E8)

// Surface colors
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF8F5F2)
val SurfaceElevated = Color(0xFFFFFDFA)

// Neutral colors
val NeutralGray = Color(0xFF6B6B6B)
val LightGray = Color(0xFFE8E8E8)
val DarkGray = Color(0xFF2B2B2B)
val MediumGray = Color(0xFF9E9E9E)

// ==================== SEMANTIC COLORS ====================

val Success = Color(0xFF26DE81)             // Fresh Green
val SuccessLight = Color(0xFF5FE8A0)
val SuccessContainer = Color(0xFFD5F7E8)

val Error = Color(0xFFEE5A6F)               // Cherry Red
val ErrorLight = Color(0xFFF48191)
val ErrorContainer = Color(0xFFFFE5E9)

val Warning = Color(0xFFFFC312)             // Golden Yellow
val WarningLight = Color(0xFFFFD54F)
val WarningContainer = Color(0xFFFFF5D9)

val Info = Color(0xFF45B7D1)                // Sky Blue
val InfoLight = Color(0xFF73C8DD)
val InfoContainer = Color(0xFFE0F4F9)

// ==================== GRADIENT DEFINITIONS ====================

val GradientOrangePink = Brush.horizontalGradient(
    colors = listOf(KulfiOrange, KulfiPink)
)

val GradientMintBlue = Brush.horizontalGradient(
    colors = listOf(KulfiMint, Info)
)

val GradientSuccessFresh = Brush.horizontalGradient(
    colors = listOf(Success, KulfiMint)
)

val GradientGoldLuxury = Brush.horizontalGradient(
    colors = listOf(TierGold, Color(0xFFFFE55C))
)

val GradientWarmWelcome = Brush.verticalGradient(
    colors = listOf(KulfiOrangeLight, KulfiOrangeContainer)
)

// ==================== ADDITIONAL COLORS ====================
// Note: Deprecated colors removed - use KulfiOrange, KulfiOrangeLight, 
// KulfiOrangeDark, CreamBackground, and CreamDark instead

val DeepBrown = Color(0xFF8B4513)
val DeepBrownLight = Color(0xFFA0522D)
val DeepBrownDark = Color(0xFF654321)
