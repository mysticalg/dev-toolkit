package com.devtoolkit.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue600 = Color(0xFF1E88E5)
val Blue700 = Color(0xFF1565C0)
val Blue400 = Color(0xFF42A5F5)
val Teal600 = Color(0xFF00897B)
val Orange600 = Color(0xFFFB8C00)

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = Teal600,
    tertiary = Orange600,
    surface = Color(0xFFFAFAFA),
    background = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004880),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFFFB74D),
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF121212),
)

val AmoledDarkColorScheme = DarkColorScheme.copy(
    surface = Color.Black,
    background = Color.Black,
)

@Composable
fun DevToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme && amoledBlack -> AmoledDarkColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
