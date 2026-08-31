package ru.maxx52.pulseplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PulsePurple = Color(0xFF8B6CFF)
val PulseMint = Color(0xFF32F6C7)
val PulsePink = Color(0xFFFF4FA3)
val PulseBackground = Color(0xFF090A12)
val PulseSurface = Color(0xFF121420)
val PulseSurfaceHigh = Color(0xFF1A1D2B)
val PulseMuted = Color(0xFF9699AD)

private val PulseColors = darkColorScheme(
    primary = PulsePurple,
    onPrimary = Color.White,
    secondary = PulseMint,
    onSecondary = Color(0xFF002018),
    tertiary = PulsePink,
    background = PulseBackground,
    onBackground = Color(0xFFF7F4FF),
    surface = PulseSurface,
    onSurface = Color(0xFFF7F4FF),
    surfaceVariant = PulseSurfaceHigh,
    onSurfaceVariant = PulseMuted,
    outline = Color(0xFF343849),
)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.statusBarColor = PulseBackground.toArgb()
                window.navigationBarColor = PulseBackground.toArgb()
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PulseColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
