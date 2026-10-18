package com.veltra.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * VELTRA Design System — "Midnight Premium"
 * Bukan RGB alay. Tenang, mahal, dipercaya.
 */
object VeltraColors {
    val Obsidian = Color(0xFF0A0E14)      // background utama — jangan pure black
    val Graphite = Color(0xFF161C28)      // card / panel overlay
    val Cobalt = Color(0xFF2A64F6)        // primary CTA — STABILIZE
    val IonCyan = Color(0xFF00D1FF)       // active state / FPS OK
    val Titanium = Color(0xFFE6E8EC)      // teks utama
    val Amber = Color(0xFFFFB800)         // warning: panas / ping tinggi
    val NeoMint = Color(0xFF00E6A0)       // success / stabil
    val Muted = Color(0xFF9AA3B3)         // teks sekunder
    val Border = Color(0xFF232A38)
}

private val DarkScheme = darkColorScheme(
    primary = VeltraColors.Cobalt,
    onPrimary = Color.White,
    secondary = VeltraColors.IonCyan,
    tertiary = VeltraColors.NeoMint,
    background = VeltraColors.Obsidian,
    surface = VeltraColors.Graphite,
    onBackground = VeltraColors.Titanium,
    onSurface = VeltraColors.Titanium,
    error = VeltraColors.Amber
)

@Composable
fun VeltraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme, // VELTRA dark-only: identitas brand
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
