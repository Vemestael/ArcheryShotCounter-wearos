package com.vemestael.archeryshotcounter.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

@Composable
fun ArcheryShotCounterTheme(
    content: @Composable () -> Unit
) {
    val palette = BrassPalette
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = appTypography(),
            content = content
        )
    }
}

/** Wear's own type scale, with the two fonts from the design system swapped in: title/display
 * roles (dialog and section titles) get Big Shoulders, everything else — buttons, labels, body —
 * falls back to Libre Franklin via [Typography]'s own default. Sizes, weights and line-heights
 * are untouched. */
private fun appTypography(): Typography {
    val base = Typography(defaultFontFamily = LibreFranklin)
    fun TextStyle.display() = copy(fontFamily = BigShouldersDisplay)
    return Typography(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        titleLarge = base.titleLarge.display(),
        titleMedium = base.titleMedium.display(),
        titleSmall = base.titleSmall.display(),
        bodyLarge = base.bodyLarge,
        bodyMedium = base.bodyMedium,
        bodySmall = base.bodySmall,
        labelLarge = base.labelLarge,
        labelMedium = base.labelMedium,
        labelSmall = base.labelSmall
    )
}

/** Maps the palette onto Material roles so stock Wear components (ScreenScaffold's background,
 * default Button colors, etc.) pick it up automatically, without every screen reaching for
 * [LocalAppPalette] directly. */
private fun AppPalette.toColorScheme() = ColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryDim = accent,
    primaryContainer = accent,
    onPrimaryContainer = onAccent,
    secondary = textDim,
    onSecondary = bg,
    secondaryDim = textDim,
    secondaryContainer = bgElev2,
    onSecondaryContainer = text,
    surfaceContainer = bgElev,
    surfaceContainerHigh = bgElev2,
    background = bg,
    onBackground = text,
    onSurface = text,
    onSurfaceVariant = textDim,
    outline = line,
    outlineVariant = line,
    error = pause,
    onError = onPause,
    errorContainer = pause,
    onErrorContainer = onPause
)
