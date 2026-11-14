package com.datecomp.dca.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta estricta de DCA: únicamente blanco, negro y grises.
 * La variante oscura invierte superficie y contenido (negro puro de fondo).
 */
private val LightScheme: ColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF424242),
    onSecondary = Color.White,
    tertiary = Color(0xFF757575),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF424242),
    outline = Color(0xFF9E9E9E),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    tertiary = Color(0xFF9E9E9E),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF212121),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF616161),
)

/** Tema raíz de la app. Sigue aclarado/oscurecido del sistema. */
@Composable
fun DcaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}