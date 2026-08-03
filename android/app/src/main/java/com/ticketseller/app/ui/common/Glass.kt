package com.ticketseller.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ticketseller.app.ui.theme.LocalGlass
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** Shared Haze state so glass surfaces can blur the [AppBackground] behind them. */
val LocalHazeState = staticCompositionLocalOf { HazeState() }

/**
 * Full-screen warm gradient background with soft colour "blobs" — the base of every screen.
 * The gradient + blobs are the Haze source, so glass surfaces drawn on top truly blur them
 * (real backdrop blur on Android 12+, a translucent tint on older devices).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val g = LocalGlass.current
    val hazeState = remember { HazeState() }
    Box(modifier.fillMaxSize()) {
        // Clean, even background — a single flat theme colour (no colour patches).
        Box(
            Modifier
                .matchParentSize()
                .haze(hazeState, style = HazeDefaults.style(backgroundColor = g.bg1, tint = g.glass, blurRadius = 24.dp))
                .background(g.bg1),
        )
        // Foreground content (drawn over the Haze source).
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            Box(Modifier.matchParentSize(), content = content)
        }
    }
}

/** Frosted translucent card. Approximates the design's glass with a translucent fill + hairline border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
    elevate: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val g = LocalGlass.current
    var m = modifier
    if (elevate) m = m.shadow(10.dp, shape, ambientColor = g.shadow, spotColor = g.shadow)
    m = m.clip(shape).hazeChild(LocalHazeState.current, shape)
    if (strong) m = m.background(g.glass) // extra frost for hero cards
    m = m.border(1.dp, g.glassBorder, shape)
    if (onClick != null) m = m.clickable { onClick() }
    Column(m, content = content)
}

/** Brand→deep gradient primary action button. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Int = 54,
    onClick: () -> Unit,
) {
    val g = LocalGlass.current
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(g.brand, g.deep)))
            .clickable(enabled = enabled) { onClick() }
            .height(height.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = g.onBrandStrong, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** Ink (near-black) pill button — used for secondary confirms like "Use this look". */
@Composable
fun InkButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val g = LocalGlass.current
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(g.ink)
            .clickable(enabled = enabled) { onClick() }
            .height(52.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = g.onInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** Circular glass button, e.g. a back arrow in a screen header. */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .size(40.dp)
            .clip(shape)
            .hazeChild(LocalHazeState.current, shape)
            .border(1.dp, g.glassBorder, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = g.ink, modifier = Modifier.size(18.dp))
    }
}

/** Small tinted chip (e.g. "Bus", "Checked"). */
@Composable
fun TintChip(text: String, leading: ImageVector? = null) {
    val g = LocalGlass.current
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(g.tint)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = g.tintInk, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = g.tintInk, style = MaterialTheme.typography.labelSmall)
    }
}

/** Uppercase muted section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Text(
        text.uppercase(),
        modifier = modifier,
        color = g.ink3,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.08f, androidx.compose.ui.unit.TextUnitType.Em),
    )
}
