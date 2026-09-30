package com.decli.codehelper.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.decli.codehelper.model.CodeScale

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
    surfaceVariant = ControlFill,
    onSurfaceVariant = InkMuted,
    outline = OutlineStrong,
    outlineVariant = Outline,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
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
    surfaceVariant = DarkControlFill,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutlineStrong,
    outlineVariant = DarkOutline,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

// ── 控件四级：用填充区分层级，控件一律不描边 ──
// 1 主操作：实心松绿（完成类）/ 实心柿橙（前进类），一张卡或一屏最多一个
// 2 状态：浅色调底 + 深色字（未取件、已取），只表达状态
// 3 次要操作与可选项：[control] 底 + 浓墨字
// 4 选中：浓墨实心（深色模式反相），颜色只留给语义，选中不借用柿橙 / 松绿

/** 次要控件底色（= surfaceVariant） */
val ColorScheme.control: Color get() = surfaceVariant

/** 选中态底色：浅色模式浓墨、深色模式浅米（= inverseSurface） */
val ColorScheme.selected: Color get() = inverseSurface

/** 选中态文字 / 图标（= inverseOnSurface） */
val ColorScheme.onSelected: Color get() = inverseOnSurface

/** 取件码字号档位，供卡片与出示页的自适应字号读取 */
val LocalCodeScale = staticCompositionLocalOf { CodeScale.Standard }

/** 当前是否深色：深色下卡片去阴影改描边，避免暖色阴影糊成一片 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** 卡片阴影：浅色 2dp / 深色 0dp */
val cardShadowElevation: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) 0.dp else 2.dp

/** 卡片描边：仅深色模式使用 1dp outlineVariant */
val cardBorder: BorderStroke?
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

@Composable
fun CodeHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    codeScale: CodeScale = CodeScale.Standard,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCodeScale provides codeScale,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CodeHelperTypography,
            content = content,
        )
    }
}
