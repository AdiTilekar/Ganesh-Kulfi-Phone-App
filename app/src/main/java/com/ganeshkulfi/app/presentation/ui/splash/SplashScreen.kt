package com.ganeshkulfi.app.presentation.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ganeshkulfi.app.presentation.theme.*
import com.ganeshkulfi.app.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToRetailerHome: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "splash_alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1200),
        label = "splash_scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        
        if (authViewModel.isUserLoggedIn()) {
            // Route based on user role
            when {
                authViewModel.isAdmin() -> onNavigateToAdmin()
                authViewModel.isRetailer() -> onNavigateToRetailerHome()
                else -> onNavigateToHome()
            }
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KulfiOrange,
                        KulfiPink
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative circles in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = 400f,
                center = Offset(size.width * 0.2f, size.height * 0.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = 300f,
                center = Offset(size.width * 0.85f, size.height * 0.75f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = 200f,
                center = Offset(size.width * 0.5f, size.height * 0.85f)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
                .padding(horizontal = 32.dp)
        ) {
            // Ice cream emoji as logo placeholder
            Text(
                text = "🍦",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // App name
            Text(
                text = "Shree Ganesh Kulfi",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tagline
            Text(
                text = "The Authentic Taste of Tradition",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Decorative dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}
