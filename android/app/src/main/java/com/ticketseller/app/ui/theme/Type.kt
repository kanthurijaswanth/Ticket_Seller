@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.ticketseller.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ticketseller.app.R

// Variable fonts — one file each, weights selected via FontVariation (API 26+; older
// devices fall back to the file's default weight, which is fine).
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun mono(weight: Int) = Font(
    R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))
val JetMono = FontFamily(mono(400), mono(500), mono(600), mono(700))

val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(800), fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(800), fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(800), fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(800), fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(700), fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(700), fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(500), fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(500), fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(500), fontSize = 11.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(700), fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(700), fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp),
)
