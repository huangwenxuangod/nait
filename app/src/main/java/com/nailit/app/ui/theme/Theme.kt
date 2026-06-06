package com.nailit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nail-It Luxury Salon Color Palette
val Espresso = Color(0xFF1C1917)
val Gold = Color(0xFFC5A880)
val Blush = Color(0xFFE9D8D0)
val Cream = Color(0xFFFAF8F5)
val Charcoal = Color(0xFF2D2A26)
val SoftGray = Color(0xFFF3EFEA)
val AccentRose = Color(0xFF881337)

private val LightColors = lightColorScheme(
    primary = Espresso,
    secondary = Gold,
    tertiary = Blush,
    background = Cream,
    surface = Color.White,
    onPrimary = Cream,
    onSecondary = Espresso,
    onTertiary = Espresso,
    onBackground = Espresso,
    onSurface = Espresso
)

private val DarkColors = darkColorScheme(
    primary = Cream,
    secondary = Gold,
    tertiary = Charcoal,
    background = Espresso,
    surface = Charcoal,
    onPrimary = Espresso,
    onSecondary = Cream,
    onTertiary = Cream,
    onBackground = Cream,
    onSurface = Cream
)

@Composable
fun NailItTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
