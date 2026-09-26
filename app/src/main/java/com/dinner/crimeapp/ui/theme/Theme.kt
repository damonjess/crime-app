package com.dinner.crimeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---------- Signature palette: "NightWatch" ----------

private val Ink = Color(0xFF0A0F1E)
private val InkSurface = Color(0xFF111730)
private val InkSurfaceVariant = Color(0xFF1C2444)
private val Periwinkle = Color(0xFF8FA6FF)
private val Mint = Color(0xFF6EE0C4)
private val Amber = Color(0xFFFFB454)
private val Coral = Color(0xFFFF6B81)

private val Paper = Color(0xFFF5F6FB)
private val PaperSurface = Color(0xFFFFFFFF)
private val PaperSurfaceVariant = Color(0xFFE3E6F4)
private val RoyalBlue = Color(0xFF3B54C4)
private val Teal = Color(0xFF0E7C67)
private val BurntOrange = Color(0xFF9A4D00)

private val DarkColors = darkColorScheme(
    primary = Periwinkle,
    onPrimary = Color(0xFF0B1540),
    primaryContainer = Color(0xFF2A3B7A),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Mint,
    onSecondary = Color(0xFF06352C),
    secondaryContainer = Color(0xFF145748),
    onSecondaryContainer = Color(0xFFBDF3E4),
    tertiary = Amber,
    onTertiary = Color(0xFF3F2600),
    tertiaryContainer = Color(0xFF6E4400),
    onTertiaryContainer = Color(0xFFFFE3C2),
    error = Coral,
    onError = Color(0xFF4A0010),
    errorContainer = Color(0xFF6E1226),
    onErrorContainer = Color(0xFFFFD9DD),
    background = Ink,
    onBackground = Color(0xFFE4E8F5),
    surface = InkSurface,
    onSurface = Color(0xFFE4E8F5),
    surfaceVariant = InkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA9B2CE),
    outline = Color(0xFF3A4468),
    outlineVariant = Color(0xFF262E4E)
)

private val LightColors = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE3FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5F1E0),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = BurntOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF331200),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Paper,
    onBackground = Color(0xFF191C2A),
    surface = PaperSurface,
    onSurface = Color(0xFF191C2A),
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = Color(0xFF454B61),
    outline = Color(0xFF757B92),
    outlineVariant = Color(0xFFC4C9DE)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.SansSerif)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CrimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
