package com.ticketseller.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Rich colour token set used across the glass UI — mirrors the design's CSS variables.
 * Screens read these via [LocalGlass]; plain Material components use the mapped ColorScheme.
 */
@Immutable
data class GlassTokens(
    val brand: Color, val brandLight: Color, val deep: Color, val deepDark: Color,
    val brandGlow: Color, val deepGlow: Color,
    val onBrand: Color, val onBrandStrong: Color, val onBrandSoft: Color, val onBrandSurface: Color,
    val accent: Color, val onDeep: Color,
    val bg1: Color, val bg2: Color, val blobA: Color, val blobB: Color,
    val glass: Color, val glassStrong: Color, val glassBorder: Color, val sheet: Color,
    val ink: Color, val ink2: Color, val ink3: Color, val onInk: Color,
    val hair: Color, val shadow: Color, val dot: Color,
    val tint: Color, val tintInk: Color,
    val dark: Boolean,
)

private class ThemeDef(
    val name: String, val sub: String,
    val brand: Long, val brandLight: Long, val deep: Long, val deepDark: Long,
    val br: Int, val bg: Int, val bb: Int,   // brand rgb
    val dr: Int, val dg: Int, val db: Int,   // deep rgb
    val onBrand: Long, val onBrandStrongLight: Long,
    val inkLight: Long, val bgLight1: Long, val bgLight2: Long, val tintInkLight: Long,
    val bgDark1: Long, val bgDark2: Long,
)

val THEME_KEYS = listOf("sunshine", "mint", "indigo", "rose", "ocean", "graphite")

private val THEMES = mapOf(
    "sunshine" to ThemeDef("Sunshine", "Your brand yellow + burnt amber",
        0xFACC15, 0xFDE047, 0xB45309, 0x7C2D12, 250, 204, 21, 180, 83, 9,
        0x3F2E00, 0x3F2E00, 0x1B1B1F, 0xFFFDF4, 0xF6F2E6, 0x8A6A00, 0x191510, 0x0C0A07),
    "mint" to ThemeDef("Mint", "Teal + deep forest",
        0x2DD4BF, 0x5EEAD4, 0x0E7C6B, 0x064E44, 45, 212, 191, 14, 124, 107,
        0x043B33, 0xFFFFFF, 0x111F1C, 0xF5FFFC, 0xEBF4F1, 0x0B6154, 0x0C1A18, 0x06100E),
    "indigo" to ThemeDef("Indigo", "Periwinkle + midnight",
        0x818CF8, 0xA5B4FC, 0x4338CA, 0x26208A, 129, 140, 248, 67, 56, 202,
        0x1B1A4A, 0xFFFFFF, 0x15162B, 0xFAFAFF, 0xEEEEF8, 0x3730A3, 0x12132A, 0x080915),
    "rose" to ThemeDef("Rose", "Coral + wine",
        0xFDA4AF, 0xFECDD3, 0x9F1239, 0x701029, 253, 164, 175, 159, 18, 57,
        0x4C0519, 0xFFFFFF, 0x241318, 0xFFFAFB, 0xF7EDEF, 0x9F1239, 0x22131A, 0x120A0E),
    "ocean" to ThemeDef("Ocean", "Sky + deep navy",
        0x7DD3FC, 0xBAE6FD, 0x0C4A6E, 0x082F49, 125, 211, 252, 12, 74, 110,
        0x082F49, 0xFFFFFF, 0x0F1D26, 0xF7FCFF, 0xEAF2F7, 0x075985, 0x0B1822, 0x050D12),
    "graphite" to ThemeDef("Graphite", "Neutral, zero colour noise",
        0xCBD5E1, 0xE2E8F0, 0x334155, 0x1E293B, 203, 213, 225, 51, 65, 85,
        0x1E293B, 0xFFFFFF, 0x15181E, 0xFBFBFC, 0xF0F1F3, 0x334155, 0x14171C, 0x0A0C0F),
)

private fun solid(hex: Long) = Color(0xFF000000 or hex)
private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).toInt())
private fun ink(a: Float) = Color(23, 24, 28, (a * 255).toInt())
private fun white(a: Float) = Color(255, 255, 255, (a * 255).toInt())
private fun black(a: Float) = Color(0, 0, 0, (a * 255).toInt())

private fun buildGlass(d: ThemeDef, dark: Boolean): GlassTokens = if (!dark) GlassTokens(
    brand = solid(d.brand), brandLight = solid(d.brandLight), deep = solid(d.deep), deepDark = solid(d.deepDark),
    brandGlow = rgba(d.br, d.bg, d.bb, 0.42f), deepGlow = rgba(d.dr, d.dg, d.db, 0.32f),
    onBrand = solid(d.onBrand), onBrandStrong = solid(d.onBrandStrongLight), onBrandSoft = black(0.55f), onBrandSurface = Color.White,
    accent = solid(d.deep), onDeep = Color.White,
    bg1 = solid(d.bgLight1), bg2 = solid(d.bgLight2),
    blobA = rgba(d.br, d.bg, d.bb, 0.55f), blobB = rgba(d.dr, d.dg, d.db, 0.26f),
    glass = white(0.62f), glassStrong = white(0.82f), glassBorder = white(0.92f), sheet = white(0.94f),
    ink = solid(d.inkLight), ink2 = ink(0.66f), ink3 = ink(0.46f), onInk = Color.White,
    hair = ink(0.12f), shadow = ink(0.09f), dot = Color.White,
    tint = rgba(d.br, d.bg, d.bb, 0.24f), tintInk = solid(d.tintInkLight),
    dark = false,
) else GlassTokens(
    brand = solid(d.brand), brandLight = solid(d.brandLight), deep = solid(d.deep), deepDark = solid(d.deepDark),
    brandGlow = rgba(d.br, d.bg, d.bb, 0.30f), deepGlow = rgba(d.dr, d.dg, d.db, 0.40f),
    onBrand = solid(d.onBrand), onBrandStrong = solid(d.onBrand), onBrandSoft = black(0.5f), onBrandSurface = Color.White,
    accent = solid(d.brandLight), onDeep = Color.White,
    bg1 = solid(d.bgDark1), bg2 = solid(d.bgDark2),
    blobA = rgba(d.br, d.bg, d.bb, 0.28f), blobB = rgba(d.dr, d.dg, d.db, 0.42f),
    glass = white(0.08f), glassStrong = white(0.13f), glassBorder = white(0.16f), sheet = Color(28, 30, 36, 230),
    ink = Color(0xFFF4F5F7), ink2 = white(0.70f), ink3 = white(0.46f), onInk = Color(0xFF14161B),
    hair = white(0.14f), shadow = black(0.45f), dot = Color(0xFF14161B),
    tint = rgba(d.br, d.bg, d.bb, 0.18f), tintInk = solid(d.brandLight),
    dark = true,
)

val LocalGlass = staticCompositionLocalOf { buildGlass(THEMES.getValue("sunshine"), false) }

/** Display info for the theme switcher swatches. */
data class ThemeMeta(val key: String, val name: String, val sub: String, val brand: Color, val deep: Color, val ink: Color)

fun themeMetas(dark: Boolean): List<ThemeMeta> = THEME_KEYS.map { k ->
    val d = THEMES.getValue(k)
    ThemeMeta(k, d.name, d.sub, solid(d.brand), solid(d.deep), if (dark) Color(0xFFF4F5F7) else solid(d.inkLight))
}

@Composable
fun AppTheme(
    themeKey: String,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val def = THEMES[themeKey] ?: THEMES.getValue("sunshine")
    val g = remember(themeKey, dark) { buildGlass(def, dark) }
    val scheme = remember(themeKey, dark) {
        if (dark) darkColorScheme(
            primary = g.brand, onPrimary = g.onBrand,
            primaryContainer = solid(def.deep), onPrimaryContainer = Color.White,
            secondary = g.deep, onSecondary = g.onDeep,
            background = g.bg1, onBackground = g.ink,
            surface = g.bg1, onSurface = g.ink,
            surfaceVariant = Color(0xFF23252B), onSurfaceVariant = g.ink3,
            outline = g.hair,
        ) else lightColorScheme(
            primary = g.brand, onPrimary = g.onBrand,
            primaryContainer = g.tint, onPrimaryContainer = g.tintInk,
            secondary = g.deep, onSecondary = g.onDeep,
            background = g.bg1, onBackground = g.ink,
            surface = g.bg1, onSurface = g.ink,
            surfaceVariant = Color(0xFFF0ECE0), onSurfaceVariant = g.ink3,
            outline = g.hair,
        )
    }
    CompositionLocalProvider(LocalGlass provides g) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
