package com.ticketseller.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandYellow,
    onPrimary = Ink,                       // dark text on yellow buttons
    primaryContainer = BrandYellowLight,
    onPrimaryContainer = BrandYellowDeep,
    secondary = BrandYellowDark,
    onSecondary = Surface,
    background = Surface,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceAlt,
    onSurfaceVariant = InkMuted,
    outline = Outline,
)

private val DarkColors = darkColorScheme(
    primary = BrandYellow,
    onPrimary = Ink,
    primaryContainer = BrandYellowDark,
    onPrimaryContainer = BrandYellowLight,
    secondary = BrandYellow,
    onSecondary = Ink,
    background = Color(0xFF14110A),
    surface = Color(0xFF1F1B12),
    onBackground = Color(0xFFFFFBEA),
    onSurface = Color(0xFFFFFBEA),
    surfaceVariant = Color(0xFF2A241A),
    onSurfaceVariant = Color(0xFFE3D9B5),
    outline = Color(0xFF55482C),
)

@Composable
fun TicketSellerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
