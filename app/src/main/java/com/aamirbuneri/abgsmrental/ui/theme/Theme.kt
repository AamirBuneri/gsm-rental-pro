package com.aamirbuneri.abgsmrental.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── AB Gsm Rental brand (from the logo) ─────────────────────────────────────
val Lime = Color(0xFF96E52C)
val Green = Color(0xFF4DB82A)
val GreenDeep = Color(0xFF2E9E2A)
val Sky = Color(0xFF1AA8F0)
val Blue = Color(0xFF1A7FE0)
val BlueDeep = Color(0xFF0F55AF)
val Cyan = Color(0xFF0EE8E0)
val Night = Color(0xFF05080E)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9BE84A),
    onPrimary = Color(0xFF0A2006),
    primaryContainer = Color(0xFF1B3A12),
    onPrimaryContainer = Color(0xFFC9F7A2),
    secondary = Color(0xFF3DB8F5),
    onSecondary = Color(0xFF00243A),
    secondaryContainer = Color(0xFF0C2F4A),
    onSecondaryContainer = Color(0xFFBDE6FF),
    tertiary = Cyan,
    onTertiary = Color(0xFF00302E),
    background = Night,
    onBackground = Color(0xFFE6EDF6),
    surface = Color(0xFF0A0F17),
    onSurface = Color(0xFFE6EDF6),
    surfaceVariant = Color(0xFF141C28),
    onSurfaceVariant = Color(0xFF93A1B5),
    surfaceContainerLowest = Color(0xFF05080E),
    surfaceContainerLow = Color(0xFF0B111A),
    surfaceContainer = Color(0xFF0F1620),
    surfaceContainerHigh = Color(0xFF141C28),
    surfaceContainerHighest = Color(0xFF1A2432),
    outline = Color(0xFF2A3749),
    outlineVariant = Color(0xFF1C2635),
    error = Color(0xFFFF6B7A),
    onError = Color(0xFF3B0710),
    errorContainer = Color(0xFF3A0F18),
    onErrorContainer = Color(0xFFFFD7DC),
    inverseSurface = Color(0xFFE6EDF6),
    inverseOnSurface = Color(0xFF0A0F17),
    scrim = Color(0xCC000000),
)

private val LightScheme = lightColorScheme(
    primary = GreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF6C8),
    onPrimaryContainer = Color(0xFF0E2E08),
    secondary = Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6ECFF),
    onSecondaryContainer = Color(0xFF042845),
    tertiary = Color(0xFF0891A8),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF0D1522),
    surface = Color.White,
    onSurface = Color(0xFF0D1522),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF5B6778),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFC),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F4F8),
    surfaceContainerHighest = Color(0xFFE8EDF3),
    outline = Color(0xFFD3DAE4),
    outlineVariant = Color(0xFFE5EAF0),
    error = Color(0xFFD92D44),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E5),
    onErrorContainer = Color(0xFF4A0612),
    inverseSurface = Color(0xFF0D1522),
    inverseOnSurface = Color.White,
    scrim = Color(0x99000000),
)

/** Colours Material doesn't have: status colours and the brand gradient. */
@Immutable
data class BrandColors(
    val dark: Boolean,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val gradient: Brush,
    val gradientColors: List<Color>,
    val glowGreen: Color,
    val glowBlue: Color,
    val cardBorder: Color,
)

val LocalBrand = staticCompositionLocalOf {
    BrandColors(false, Green, Color(0xFFF5A524), Color(0xFFD92D44), Blue, Brush.linearGradient(listOf(Lime, Blue)), listOf(Lime, Blue), Green, Blue, Color.LightGray)
}

private fun brand(dark: Boolean): BrandColors {
    val colors = if (dark) listOf(Color(0xFF8BE13A), Color(0xFF1AA8F0)) else listOf(Color(0xFF6CCB1F), Color(0xFF1A7FE0))
    return BrandColors(
        dark = dark,
        success = if (dark) Color(0xFF5BE38A) else Color(0xFF16A34A),
        warning = if (dark) Color(0xFFFFC04D) else Color(0xFFD98A0B),
        danger = if (dark) Color(0xFFFF6B7A) else Color(0xFFD92D44),
        info = if (dark) Color(0xFF3DB8F5) else Blue,
        gradient = Brush.linearGradient(colors),
        gradientColors = colors,
        glowGreen = if (dark) Color(0x2E7CE02A) else Color(0x1F6CCB1F),
        glowBlue = if (dark) Color(0x331AA8F0) else Color(0x1A1A7FE0),
        cardBorder = if (dark) Color(0xFF1C2635) else Color(0xFFE6EBF1),
    )
}

private val Base = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Base, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun ABTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme: ColorScheme = if (dark) DarkScheme else LightScheme
    CompositionLocalProvider(LocalBrand provides brand(dark)) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

object AB {
    val brand: BrandColors @Composable get() = LocalBrand.current
}
