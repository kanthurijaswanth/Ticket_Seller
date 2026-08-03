package com.ticketseller.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ticketseller.app.R
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branded intro: six theme colours sweep in from the edges and additively blend into a
 * bright core, out of which the logo badge pops and the wordmark fades in.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val p = progress.value

    androidx.compose.runtime.LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1700, easing = FastOutSlowInEasing))
        delay(450)
        onDone()
    }

    val blobColors = listOf(
        Color(0xFFFACC15), Color(0xFF2DD4BF), Color(0xFF818CF8),
        Color(0xFFFDA4AF), Color(0xFF7DD3FC), Color(0xFFB45309),
    )

    val appear = ((p - 0.25f) / 0.6f).coerceIn(0f, 1f)
    val scale = 0.5f + 0.5f * easeOutBack(appear)
    val textAlpha = ((p - 0.62f) / 0.38f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1206), Color(0xFF0B0A08))))) {
        // Colour-mix glow, centred on the logo (which sits slightly above middle).
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            val startR = size.minDimension * 0.55f
            val r = startR * (1f - p) + 20f
            val blob = size.minDimension * (0.40f - 0.16f * p)
            val rot = p * 80f
            blobColors.forEachIndexed { i, c ->
                val a = Math.toRadians((i * 60f + rot).toDouble())
                val bx = cx + (cos(a) * r).toFloat()
                val by = cy + (sin(a) * r).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(c.copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(bx, by), radius = blob,
                    ),
                    radius = blob, center = Offset(bx, by),
                    blendMode = BlendMode.Plus,
                )
            }
        }

        // Logo badge — optically centred (slightly above the middle).
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .size(132.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = appear }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFB45309)))),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(132.dp))
        }

        // Wordmark + tagline, anchored near the bottom, clear of the glow so they read cleanly.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 84.dp)
                .graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "TicketResell",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Someone else's seat, your trip",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun easeOutBack(t: Float): Float {
    val s = 1.70158f
    val x = t - 1f
    return 1f + (s + 1f) * x * x * x + s * x * x
}
