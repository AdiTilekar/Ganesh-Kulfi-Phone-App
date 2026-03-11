package com.ganeshkulfi.salelog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Saffron/orange seed color: #FF6B00
private val SaffronOrange = Color(0xFFFF6B00)
private val DeepSaffron   = Color(0xFFE55A00)
private val LightSaffron  = Color(0xFFFFEDE0)

private val LightColorScheme = lightColorScheme(
    primary            = SaffronOrange,
    onPrimary          = Color.White,
    primaryContainer   = LightSaffron,
    onPrimaryContainer = DeepSaffron,
    secondary          = Color(0xFF8B5E3C),
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF4D2A0D),
    background         = Color(0xFFFFFBF8),
    onBackground       = Color(0xFF1C1B1B),
    surface            = Color(0xFFFFFBF8),
    onSurface          = Color(0xFF1C1B1B),
    error              = Color(0xFFB00020),
    onError            = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFFFBD98),
    onPrimary          = Color(0xFF5C1C00),
    primaryContainer   = Color(0xFF882E00),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary          = Color(0xFFEFBD97),
    onSecondary        = Color(0xFF4D2A0D),
    background         = Color(0xFF1C1B1B),
    onBackground       = Color(0xFFE6E1E5),
    surface            = Color(0xFF1C1B1B),
    onSurface          = Color(0xFFE6E1E5)
)

@Composable
fun SaleLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
