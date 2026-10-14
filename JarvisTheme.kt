package com.jarvis.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF03090E)
val DeepNavy = Color(0xFF071720)
val Panel = Color(0xFF0C222D)
val PanelSoft = Color(0xFF112F3B)
val Cyan = Color(0xFF72E5FF)
val Turquoise = Color(0xFF2AC6CE)
val Mint = Color(0xFF7FF4C8)
val Amber = Color(0xFFFFC66B)
val Rose = Color(0xFFFF8096)
val Ink = Color(0xFFE5F8FC)
val Muted = Color(0xFF92AEB7)

private val JarvisColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Void,
    primaryContainer = PanelSoft,
    secondary = Turquoise,
    tertiary = Mint,
    background = Void,
    onBackground = Ink,
    surface = Panel,
    onSurface = Ink,
    surfaceVariant = PanelSoft,
    onSurfaceVariant = Muted,
    error = Rose
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
