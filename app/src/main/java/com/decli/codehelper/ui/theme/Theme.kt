package com.decli.codehelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Persimmon,
    onPrimary = Color.White,
    primaryContainer = PersimmonTint,
    onPrimaryContainer = PersimmonDeep,
    secondary = PersimmonDeep,
    onSecondary = Color.White,
    secondaryContainer = PersimmonChip,
    onSecondaryContainer = PersimmonDeep,
    tertiary = Pine,
    onTertiary = Color.White,
    tertiaryContainer = PineTint,
    onTertiaryContainer = PineDeep,
    background = Paper,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkMuted,
    outline = OutlineStrong,
    outlineVariant = Outline,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedTint,
    onErrorContainer = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = PersimmonBright,
    onPrimary = PersimmonOnBright,
    primaryContainer = DarkPersimmonTint,
    onPrimaryContainer = PersimmonTintBright,
    secondary = PersimmonTintBright,
    onSecondary = PersimmonOnBright,
    secondaryContainer = DarkPersimmonTint,
    onSecondaryContainer = PersimmonTintBright,
    tertiary = PineBright,
    onTertiary = PineOnBright,
    tertiaryContainer = DarkPineTint,
    onTertiaryContainer = PineTintBright,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkCardSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutlineStrong,
    outlineVariant = DarkOutline,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun CodeHelperTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = CodeHelperTypography,
        content = content,
    )
}
