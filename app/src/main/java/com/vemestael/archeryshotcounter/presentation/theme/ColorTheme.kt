package com.vemestael.archeryshotcounter.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the UI uses, named by role instead of hex, so a screen never hard-codes a shade.
 *
 * [accent] is the single bright highlight: the shot counter, the primary button, selected state.
 * [active] marks an in-progress state (detecting), [pause] marks pause and destructive actions.
 */
data class AppPalette(
    val bg: Color,
    val bgElev: Color,
    val bgElev2: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val active: Color,
    val pause: Color,
    val onPause: Color
)

/** "Латунь": warm sand and brass on coal. The app's first (and for now, only) palette. */
val BrassPalette = AppPalette(
    bg = Color(0xFF15120F),
    bgElev = Color(0xFF201B15),
    bgElev2 = Color(0xFF262019),
    line = Color(0xFF3A3125),
    text = Color(0xFFF3EAD9),
    textDim = Color(0xFFA89C8A),
    textMuted = Color(0xFF7A6F60),
    accent = Color(0xFFC9A36A),
    onAccent = Color(0xFF1A1510),
    active = Color(0xFFA3B17E),
    pause = Color(0xFFC0603F),
    onPause = Color(0xFFF3EAD9)
)

val LocalAppPalette = staticCompositionLocalOf { BrassPalette }
