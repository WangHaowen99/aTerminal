package com.aterminal.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8BE9A7),
    onPrimary = Color(0xFF062313),
    secondary = Color(0xFFFFD166),
    onSecondary = Color(0xFF2B2100),
    background = Color(0xFF08110C),
    onBackground = Color(0xFFE6F4EA),
    surface = Color(0xFF101A14),
    onSurface = Color(0xFFE6F4EA),
    surfaceVariant = Color(0xFF1E2A22),
    onSurfaceVariant = Color(0xFFB5C7B9),
)

@Composable
fun ATerminalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
