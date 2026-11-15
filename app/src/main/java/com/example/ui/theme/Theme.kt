package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun buildDarkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = ObsidianCanvas,
    primaryContainer = accent.copy(alpha = 0.18f),
    onPrimaryContainer = SnowText,
    secondary = SonicMint,
    onSecondary = ObsidianCanvas,
    secondaryContainer = SonicMint.copy(alpha = 0.16f),
    onSecondaryContainer = SonicMint,
    tertiary = ElectricViolet,
    onTertiary = SnowText,
    background = ObsidianCanvas,
    onBackground = SnowText,
    surface = MidnightSurface,
    onSurface = SnowText,
    surfaceVariant = SlateCard,
    onSurfaceVariant = MistText,
    surfaceContainerHigh = ElevatedSlate,
    outline = SubtleOutline,
    outlineVariant = SubtleOutline.copy(alpha = 0.55f),
    error = Color(0xFFFF6B6B),
    onError = ObsidianCanvas
)

private fun buildLightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = DeepPlumText,
    secondary = Color(0xFF009B82),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F7EE),
    onSecondaryContainer = Color(0xFF00473B),
    tertiary = ElectricViolet,
    onTertiary = Color.White,
    background = DaylightCanvas,
    onBackground = DeepPlumText,
    surface = PetalSurface,
    onSurface = DeepPlumText,
    surfaceVariant = PetalCard,
    onSurfaceVariant = DuskText,
    surfaceContainerHigh = PetalCard,
    outline = Color(0xFFD8D0E4),
    outlineVariant = Color(0xFFE9E3F2),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

val BloomeeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BloomeeTunesTheme(
    darkTheme: Boolean = true,
    accentColor: Color = BloomCoral,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkScheme(accentColor) else buildLightScheme(accentColor)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BloomeeTypography,
        shapes = BloomeeShapes,
        content = content
    )
}
