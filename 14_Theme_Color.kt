package com.sam.stt.ui.theme

import androidx.compose.ui.graphics.Color

// Couleurs principales - Thème sombre éclair/tonnerre
val DarkBackground = Color(0xFF0A0A0F)
val SurfaceDark = Color(0xFF14141F)
val SurfaceLight = Color(0xFF1E1E2E)

// Accents
val YellowThunder = Color(0xFFFFD700)
val YellowThunderBright = Color(0xFFFFE44D)
val BlueLightning = Color(0xFF00BFFF)
val BlueLightningBright = Color(0xFF4DD2FF)

// Textes
val TextWhite = Color(0xFFF0F0F5)
val TextGray = Color(0xFF8A8A9A)
val TextMuted = Color(0xFF5A5A6A)

// États
val SuccessGreen = Color(0xFF00C853)
val ErrorRed = Color(0xFFFF1744)
val WarningOrange = Color(0xFFFF9100)
val InfoBlue = Color(0xFF2979FF)

// Dégradés (pour utilisation avec Brush)
val ThunderGradient = listOf(YellowThunder, YellowThunderBright)
val LightningGradient = listOf(BlueLightning, BlueLightningBright)
val DarkGradient = listOf(DarkBackground, SurfaceDark)
