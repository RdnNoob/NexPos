package com.nexpos.admin.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
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
    val lastCrash by viewModel.lastCrash.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    var logoVisible by remember { mutableStateOf(false) }
    val logoAnim by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoAnim"
    )

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

    var taglineVisible by remember { mutableStateOf(false) }
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOut),
        label = "taglineAlpha"
    )

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
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn != null && !hasNavigated && lastCrash == null) {
            delay(1200)
            if (!hasNavigated) {
                hasNavigated = true
                try {
                    if (isLoggedIn == true) onNavigateToDashboard() else onNavigateToLogin()
                } catch (_: Exception) {
                    hasNavigated = false
                }
            }
        }
    }

    if (lastCrash != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("⚠ Crash Terdeteksi", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "App baru saja crash. Info ini untuk diagnosa:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = lastCrash ?: "",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearCrash()
                    if (!hasNavigated) {
                        hasNavigated = true
                        if (isLoggedIn == true) onNavigateToDashboard() else onNavigateToLogin()
                    }
                }) {
                    Text("OK, Lanjutkan")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1565C0),
                        Color(0xFF0D47A1),
                        Color(0xFF002171)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoAnim)
                    .alpha(logoAnim),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = "NexPos",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "NEXPOS",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .offset(y = titleOffset.dp),
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Manajemen Outlet Laundry\nSerba Digital",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(80.dp))

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
