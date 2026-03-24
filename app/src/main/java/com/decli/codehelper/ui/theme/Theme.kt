package com.decli.codehelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CobaltBlue,
    onPrimary = SoftSurface,
    primaryContainer = InkBlue,
    onPrimaryContainer = SoftSurface,
    secondary = DeepGreen,
    onSecondary = SoftSurface,
    tertiary = WarmGold,
    background = SandBackground,
    surface = SoftSurface,
    surfaceVariant = MutedSurface,
    onSurface = InkBlue,
    onSurfaceVariant = MutedText,
    error = AlertRed,
    errorContainer = AlertRedContainer,
)

private val DarkColors = darkColorScheme(
    primary = WarmGold,
    onPrimary = InkBlue,
    secondary = CobaltBlue,
    tertiary = DeepGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkMutedSurface,
    onSurface = SoftSurface,
    onSurfaceVariant = WhiteSmoke,
    error = AlertRed,
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

