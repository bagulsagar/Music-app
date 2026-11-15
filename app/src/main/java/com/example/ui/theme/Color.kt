package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Nocturnal Vinyl & Sonic Bloom Palette
val ObsidianCanvas = Color(0xFF0B0D13)
val MidnightSurface = Color(0xFF141824)
val SlateCard = Color(0xFF1B2030)
val ElevatedSlate = Color(0xFF232A3F)
val SubtleOutline = Color(0xFF2F3752)

val BloomCoral = Color(0xFFFF4D8D)
val BloomRose = Color(0xFFFF7AA8)
val SonicMint = Color(0xFF38E8C6)
val AmberGlow = Color(0xFFFFB84D)
val ElectricViolet = Color(0xFF8B6EFF)

val SnowText = Color(0xFFF6F7FB)
val MistText = Color(0xFFB4BCD3)
val MutedText = Color(0xFF7D86A3)

// Light Theme Counterparts ("Petal Daylight")
val DaylightCanvas = Color(0xFFFBF9FC)
val PetalSurface = Color(0xFFF3EFF6)
val PetalCard = Color(0xFFFFFFFF)
val DeepPlumText = Color(0xFF171324)
val DuskText = Color(0xFF5A536D)

val PlaylistAccentPresets = listOf(
    "#FF4D8D", // Bloom Coral
    "#38E8C6", // Sonic Mint
    "#8B6EFF", // Electric Violet
    "#FFB84D", // Amber Glow
    "#4DA6FF", // Horizon Blue
    "#FF6B57"  // Sunset Tangerine
)

fun parseHexColor(hex: String, fallback: Color = BloomCoral): Color {
    return try {
        val clean = hex.removePrefix("#")
        if (clean.length == 6) {
            Color(("FF$clean").toLong(16))
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
