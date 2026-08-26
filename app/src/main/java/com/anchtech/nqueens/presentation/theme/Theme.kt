package com.anchtech.nqueens.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Forest40,
    onPrimary = Color.White,
    primaryContainer = Forest90,
    onPrimaryContainer = Forest10,
    inversePrimary = Forest80,

    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,

    tertiary = Bronze40,
    onTertiary = Color.White,
    tertiaryContainer = Bronze90,
    onTertiaryContainer = Bronze10,

    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,

    background = Paper,
    onBackground = Ink10,
    surface = Paper,
    onSurface = Ink10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceDim = PaperDim,
    surfaceBright = PaperBright,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F4EE),
    surfaceContainer = Color(0xFFEFEEE8),
    surfaceContainerHigh = Color(0xFFEAE9E3),
    surfaceContainerHighest = Color(0xFFE4E3DD),

    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Ink20,
    inverseOnSurface = Ink95,
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Forest80,
    onPrimary = Forest20,
    primaryContainer = Forest30,
    onPrimaryContainer = Forest90,
    inversePrimary = Forest40,

    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,

    tertiary = Bronze80,
    onTertiary = Bronze20,
    tertiaryContainer = Bronze30,
    onTertiaryContainer = Bronze90,

    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,

    background = Ink10,
    onBackground = Ink90,
    surface = Ink10,
    onSurface = Ink90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceDim = Ink10,
    surfaceBright = Color(0xFF363A34),
    surfaceContainerLowest = Color(0xFF0B0F0A),
    surfaceContainerLow = Ink20,
    surfaceContainer = Color(0xFF1E231C),
    surfaceContainerHigh = Color(0xFF282D26),
    surfaceContainerHighest = Color(0xFF333831),

    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Ink90,
    inverseOnSurface = Ink20,
    scrim = Color.Black,
)

/**
 * Dynamic colour is intentionally not supported.
 */
@Composable
fun NQueensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBoardColors provides if (darkTheme) DarkBoardColors else LightBoardColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = NQueensTypography,
            shapes = NQueensShapes,
            content = content,
        )
    }
}
