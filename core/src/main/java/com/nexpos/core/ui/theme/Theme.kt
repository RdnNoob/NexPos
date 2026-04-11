package com.nexpos.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Blue800,
    secondary = Green600,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = GreenLight,
    tertiary = Orange600,
    tertiaryContainer = OrangeLight,
    error = Red600,
    errorContainer = RedLight,
    background = Gray50,
    onBackground = Gray900,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray300
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AccentCyan,
    onPrimaryContainer = BlueLight,
    secondary = Green600,
    secondaryContainer = Green700,
    tertiary = Orange600,
    error = Red600,
    background = SurfaceDark,
    onBackground = Gray100,
    surface = SurfaceDarkCard,
    onSurface = Gray100,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = Gray300,
    outline = Gray500
)

@Composable
fun NexPosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            controller.isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
