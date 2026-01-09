package com.ganeshkulfi.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    // Primary - Creamy Orange (Kulfi theme)
    primary = KulfiOrange,
    onPrimary = Color.White,
    primaryContainer = KulfiOrangeContainer,
    onPrimaryContainer = DarkGray,
    
    // Secondary - Cool Mint
    secondary = KulfiMint,
    onSecondary = Color.White,
    secondaryContainer = KulfiMintContainer,
    onSecondaryContainer = DarkGray,
    
    // Tertiary - Strawberry Pink
    tertiary = KulfiPink,
    onTertiary = Color.White,
    tertiaryContainer = KulfiPinkContainer,
    onTertiaryContainer = DarkGray,
    
    // Background & Surface
    background = CreamBackground,
    onBackground = DarkGray,
    surface = SurfaceWhite,
    onSurface = DarkGray,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = NeutralGray,
    surfaceTint = KulfiOrangeLight,
    
    // Error colors
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = DarkGray,
    
    // Outline
    outline = MediumGray,
    outlineVariant = LightGray,
    
    // Inverse colors
    inverseSurface = DarkGray,
    inverseOnSurface = CreamLight,
    inversePrimary = KulfiOrangeLight,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.32f)
)

private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = KulfiOrangeLight,
    onPrimary = DarkGray,
    primaryContainer = KulfiOrangeDark,
    onPrimaryContainer = CreamLight,
    
    // Secondary
    secondary = KulfiMintLight,
    onSecondary = DarkGray,
    secondaryContainer = KulfiMintDark,
    onSecondaryContainer = CreamLight,
    
    // Tertiary
    tertiary = KulfiPinkLight,
    onTertiary = DarkGray,
    tertiaryContainer = KulfiPinkDark,
    onTertiaryContainer = CreamLight,
    
    // Background & Surface
    background = Color(0xFF1A1A1A),
    onBackground = CreamLight,
    surface = Color(0xFF1C1B1F),
    onSurface = CreamLight,
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = KulfiOrange,
    
    // Error
    error = ErrorLight,
    onError = DarkGray,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ErrorContainer,
    
    // Outline
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    // Inverse
    inverseSurface = CreamLight,
    inverseOnSurface = DarkGray,
    inversePrimary = KulfiOrange,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.5f)
)

@Composable
fun KulfiDelightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
