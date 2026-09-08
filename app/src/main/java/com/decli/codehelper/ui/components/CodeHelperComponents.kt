package com.decli.codehelper.ui.components

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decli.codehelper.ui.theme.ButtonLarge
import com.decli.codehelper.ui.theme.LocalCodeScale
import com.decli.codehelper.ui.theme.cardBorder
import com.decli.codehelper.ui.theme.cardShadowElevation

// ─────────────────────────── 动效开关 ───────────────────────────

/** 系统「减少动效」开启（动画时长缩放为 0）时，全应用改为即时切换 */
@Composable
fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }.getOrDefault(true)
    }
}

/** 统一的动画时长：系统关闭动效时全部归零 */
@Composable
fun motionDuration(durationMillis: Int): Int = if (animationsEnabled()) durationMillis else 0

/** 按下反馈：scale .98，80ms */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val duration = motionDuration(80)
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(durationMillis = duration),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ─────────────────────────── 按钮三级 ───────────────────────────

/**
 * 主按钮（60dp）/ 填充按钮（56dp）/ 出示页主按钮（68dp）共用实现。
 * 触控目标不小于 48dp，图标必带文字，禁用时整体 50% 透明但文字颜色不变。
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = 60.dp,
    shape: Shape = RoundedCornerShape(18.dp),
    textStyle: TextStyle = ButtonLarge,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    semanticsLabel: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .pressScale(interactionSource, enabled)
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                if (semanticsLabel != null) {
                    contentDescription = semanticsLabel
                }
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = contentColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = textStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 文字按钮（默认 48dp 行高，可点击文字一律 onPrimaryContainer） */
@Composable
fun TextActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconScale: Float = 1f,
    height: Dp = 48.dp,
    textStyle: TextStyle? = null,
    semanticsLabel: String? = null,
    stacked: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                if (semanticsLabel != null) {
                    contentDescription = semanticsLabel
                }
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val label: @Composable () -> Unit = {
            Text(
                text = text,
                style = textStyle ?: MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        val buttonIcon: @Composable () -> Unit = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = contentColor,
                )
            }
        }
        if (stacked) {
            // 图标在上、文字在下：三个按钮并排时也放得下完整文案，
            // 系统字号放大到 1.3× 仍然不会把「读给我听」截断。
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                buttonIcon()
                if (icon != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                label()
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                buttonIcon()
                if (icon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
                label()
            }
        }
    }
}

// ─────────────────────────── 分段控件（全应用唯一的选中语言） ───────────────────────────

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabledOf: (T) -> Boolean = { true },
    containerColor: Color = MaterialTheme.colorScheme.surface,
    containerShape: Shape = RoundedCornerShape(20.dp),
    containerPadding: Dp = 5.dp,
    segmentGap: Dp = 5.dp,
    segmentHeight: Dp = 56.dp,
    segmentShape: Shape = RoundedCornerShape(16.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    checkSize: Dp = 22.dp,
    elevated: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = containerShape,
        color = containerColor,
        shadowElevation = if (elevated) cardShadowElevation else 0.dp,
        border = if (elevated) cardBorder else null,
    ) {
        Row(
            modifier = Modifier.padding(containerPadding),
            horizontalArrangement = Arrangement.spacedBy(segmentGap),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val isEnabled = enabledOf(option)
                Segment(
                    modifier = Modifier.weight(1f),
                    label = labelOf(option),
                    selected = isSelected,
                    enabled = isEnabled,
                    height = segmentHeight,
                    shape = segmentShape,
                    textStyle = textStyle,
                    checkSize = checkSize,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    height: Dp,
    shape: Shape,
    textStyle: TextStyle,
    checkSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = motionDuration(200)
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = duration),
        label = "segmentBackground",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(shape)
            .background(background)
            .clickable(
                enabled = enabled && !selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { this.contentDescription = if (selected) "$label，已选中" else label },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(checkSize),
                    tint = contentColor,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = textStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────── 取件码自适应文本 ───────────────────────────

/** 取件码自适应字号上限（sp）。下限见 [ABSOLUTE_MIN_CODE_FONT_SP]，常规不会触到 */
const val MAX_CODE_FONT_SP = 104

/**
 * 极端长码时允许继续缩小到该下限：
 * 「一码一行、永不折行、不省略」优先级高于「不小于 40sp」，
 * 宁可字小一点，也不能把取件码截断成另一个码。
 */
private const val ABSOLUTE_MIN_CODE_FONT_SP = 22

/**
 * 字间距按码长分档：短码放宽到 4sp 更好数，长码收紧到 1sp。
 * 取件码是按宽度实测放大的——字间距每多 1sp，10 位的码就要少掉约 1.5sp 字号，
 * 而字号对老人的可读性远比字间距重要，所以长码优先让位给字号。
 */
private fun codeLetterSpacingSp(code: String): Int =
    when {
        code.length <= 5 -> 4
        code.length <= 7 -> 2
        else -> 1
    }

/**
 * 取件码专用自适应文本：每个码渲染成独立的单行 Text（一码一行、物理上不可能折行），
 * 所有码共用同一字号，取值为「最长的码单行放得下」的最大档位，用 TextMeasurer 二分实测。
 *
 * 不使用 TextAutoSize 的多行组合：softWrap=false + Ellipsis 时 Compose 会把
 * maxLines 强制为 1（LayoutUtils.finalMaxLines），含 \n 的多码文本会被压成一行
 * 并吞掉后续的码；实测方案没有这类隐藏分支，行为完全确定。
 */
@Composable
fun AutoSizeCodeLines(
    codes: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    maxFontSizeSp: Int = MAX_CODE_FONT_SP,
    scaleWithSetting: Boolean = true,
    lineGap: Dp = 0.dp,
) {
    val codeScale = LocalCodeScale.current
    val effectiveMax = remember(maxFontSizeSp, scaleWithSetting, codeScale) {
        if (scaleWithSetting) {
            (maxFontSizeSp * codeScale.multiplier).toInt()
        } else {
            maxFontSizeSp
        }
    }
    BoxWithConstraints(modifier = modifier) {
        val textMeasurer = rememberTextMeasurer()
        val baseStyle = MaterialTheme.typography.displayMedium
        val maxWidthPx = constraints.maxWidth
        val fontSizeSp = remember(codes, maxWidthPx, textMeasurer, baseStyle, effectiveMax) {
            var low = ABSOLUTE_MIN_CODE_FONT_SP
            var high = effectiveMax.coerceAtLeast(ABSOLUTE_MIN_CODE_FONT_SP)
            while (low < high) {
                val mid = (low + high + 1) / 2
                val fits = codes.all { code ->
                    textMeasurer.measure(
                        text = AnnotatedString(code),
                        style = baseStyle.copy(
                            fontSize = mid.sp,
                            letterSpacing = codeLetterSpacingSp(code).sp,
                        ),
                        softWrap = false,
                        maxLines = 1,
                    ).size.width <= maxWidthPx
                }
                if (fits) low = mid else high = mid - 1
            }
            low
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(lineGap),
        ) {
            codes.forEach { code ->
                Text(
                    text = code,
                    modifier = Modifier.fillMaxWidth(),
                    style = baseStyle.copy(
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.3f).sp,
                        letterSpacing = codeLetterSpacingSp(code).sp,
                    ),
                    color = color,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────── 卡片容器 ───────────────────────────

/** 统一卡片：浅色 2dp 阴影，深色去阴影改 1dp 描边（名字避开 theme 里的 CardSurface 颜色令牌） */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
        shadowElevation = cardShadowElevation,
        border = cardBorder,
        content = content,
    )
}

/** 行内小图标 + 文字组合，供状态标签复用 */
@Composable
fun RowScope.LabelWithDot(
    text: String,
    dotColor: Color,
    textColor: Color,
    textStyle: TextStyle,
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(dotColor),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = text,
        style = textStyle,
        color = textColor,
        maxLines = 1,
    )
}
