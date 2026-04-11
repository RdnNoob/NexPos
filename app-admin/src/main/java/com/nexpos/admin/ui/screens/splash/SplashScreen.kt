package com.nexpos.admin.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexpos.admin.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // --- Animasi logo: scale spring bounce ---
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )
    var logoVisible by remember { mutableStateOf(false) }
    val logoAnim by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoAnim"
    )

    // --- Animasi teks: slide up + fade ---
    var titleVisible by remember { mutableStateOf(false) }
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = EaseOut),
        label = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (titleVisible) 0f else 40f,
        animationSpec = tween(durationMillis = 500, easing = EaseOut),
        label = "titleOffset"
    )

    // --- Animasi tagline ---
    var taglineVisible by remember { mutableStateOf(false) }
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOut),
        label = "taglineAlpha"
    )

    // --- Animasi dots loading ---
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )

    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        delay(350)
        titleVisible = true
        delay(300)
        taglineVisible = true
        delay(1400)
        when (isLoggedIn) {
            true -> onNavigateToDashboard()
            false -> onNavigateToLogin()
            null -> {
                // Tunggu state selesai
                delay(500)
                if (isLoggedIn == true) onNavigateToDashboard() else onNavigateToLogin()
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn != null && taglineVisible) {
            delay(600)
            if (isLoggedIn == true) onNavigateToDashboard() else onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1565C0),
                        Color(0xFF1976D2),
                        Color(0xFF1E88E5)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo icon dengan circle background
            Box(
                modifier = Modifier
                    .scale(logoAnim)
                    .alpha(logoAnim)
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color.White.copy(alpha = 0.95f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color(0xFF1565C0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "NexPos",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp),
                letterSpacing = 2.sp
            )
            Text(
                text = "Admin",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp),
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Manajemen Outlet Laundry\nSerba Digital",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(taglineAlpha)
            ) {
                repeat(3) { i ->
                    val alpha = when (i) { 0 -> dot1Alpha; 1 -> dot2Alpha; else -> dot3Alpha }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alpha)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Version text at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp)
                .alpha(taglineAlpha),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "v1.0.0",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}
