package com.geecee.escapelauncher.core.theme.type

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

fun escapeType(fontFamily: FontFamily, scale: Float = 1f): Typography {
    val typography = Typography(
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 59.sp * scale,
            lineHeight = 53.sp * scale,
            letterSpacing = 0.6.sp
        ), headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 56.sp * scale,
            lineHeight = 49.sp * scale,
            letterSpacing = 0.6.sp
        ), headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 52.sp * scale,
            lineHeight = 45.sp * scale,
            letterSpacing = 0.6.sp
        ), titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 47.sp * scale,
            lineHeight = 53.sp * scale,
            letterSpacing = 0.6.sp
        ), titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 43.sp * scale,
            lineHeight = 49.sp * scale,
            letterSpacing = 0.6.sp
        ), titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 40.sp * scale,
            lineHeight = 45.sp * scale,
            letterSpacing = 0.6.sp
        ), bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 25.sp * scale,
            lineHeight = 29.sp * scale,
            letterSpacing = 0.6.sp
        ), bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp * scale,
            lineHeight = 25.sp * scale,
            letterSpacing = 0.6.sp
        ), bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp * scale,
            lineHeight = 21.sp * scale,
            letterSpacing = 0.6.sp
        )
    )

    return typography
}