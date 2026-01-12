package com.example.pagewise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = UiDark,
    secondary = UiLight,
    tertiary = UiMedium
)

private val LightColorScheme = lightColorScheme(
    primary = UiDark,
    secondary = UiLight,
    tertiary = UiMedium
)

@Composable
fun PagewiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Mengacu pada variabel di Type.kt
        content = content
    )
}