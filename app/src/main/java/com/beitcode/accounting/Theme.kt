package com.beitcode.accounting

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1F3A5F)
val Bg = Color(0xFFF3F4F6)
val Ink = Color(0xFF1B1F24)
val Muted = Color(0xFF6B7280)
val Pos = Color(0xFF1B6B3A)
val Neg = Color(0xFFB3261E)
val Rule = Color(0xFFD9DCE1)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            onPrimary = Color.White,
            secondary = Navy,
            background = Bg,
            surface = Color.White,
            onSurface = Ink
        ),
        content = content
    )
}
