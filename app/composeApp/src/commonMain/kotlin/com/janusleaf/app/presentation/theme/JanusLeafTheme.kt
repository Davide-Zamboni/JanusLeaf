package com.janusleaf.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * JanusLeaf 2026 Design System
 * 
 * A moody, sophisticated palette inspired by botanical themes
 * with vibrant accent colors for emotional expression.
 */

// Core Brand Colors - Deep forest greens with warm accents
val LeafGreen = Color(0xFF1A2F23)
val LeafGreenLight = Color(0xFF2D4A3A)
val MossGreen = Color(0xFF3D5A47)
val SageGreen = Color(0xFF6B8F71)
val MintCream = Color(0xFFE8F5E9)

// Accent Colors - Emotional spectrum for mood tracking
val SunriseGold = Color(0xFFFFB74D)
val SunsetOrange = Color(0xFFFF8A65)
val DuskPurple = Color(0xFF9575CD)
val MidnightBlue = Color(0xFF5C6BC0)
val DawnPink = Color(0xFFF48FB1)

// Neutral Colors
val Charcoal = Color(0xFF1C1C1E)
val DarkSlate = Color(0xFF2C2C2E)
val MediumSlate = Color(0xFF3A3A3C)
val LightSlate = Color(0xFF48484A)
val SoftGray = Color(0xFF8E8E93)
val CloudWhite = Color(0xFFF2F2F7)
val PureWhite = Color(0xFFFAFAFA)

// Semantic Colors
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFEF5350)
val WarningAmber = Color(0xFFFFC107)
val InfoBlue = Color(0xFF42A5F5)

// Dark Theme - Moody and atmospheric
private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    onPrimary = PureWhite,
    primaryContainer = LeafGreenLight,
    onPrimaryContainer = MintCream,
    secondary = SunriseGold,
    onSecondary = Charcoal,
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = SunriseGold,
    tertiary = DuskPurple,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFF4A4063),
    onTertiaryContainer = Color(0xFFE1BEE7),
    background = Charcoal,
    onBackground = CloudWhite,
    surface = DarkSlate,
    onSurface = CloudWhite,
    surfaceVariant = MediumSlate,
    onSurfaceVariant = SoftGray,
    outline = LightSlate,
    outlineVariant = MediumSlate,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Light Theme - Fresh and calming
private val LightColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = PureWhite,
    primaryContainer = MintCream,
    onPrimaryContainer = LeafGreen,
    secondary = SunsetOrange,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = MidnightBlue,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFC5CAE9),
    onTertiaryContainer = Color(0xFF1A237E),
    background = CloudWhite,
    onBackground = Charcoal,
    surface = PureWhite,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFE8E8ED),
    onSurfaceVariant = LightSlate,
    outline = SoftGray,
    outlineVariant = Color(0xFFD1D1D6),
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A)
)

@Composable
fun JanusLeafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = JanusLeafTypography,
        content = content
    )
}
