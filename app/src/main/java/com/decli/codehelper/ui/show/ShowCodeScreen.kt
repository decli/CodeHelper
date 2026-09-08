package com.decli.codehelper.ui.show

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.components.ActionButton
import com.decli.codehelper.ui.components.AutoSizeCodeLines
import com.decli.codehelper.ui.components.animationsEnabled
import com.decli.codehelper.ui.formatSmsTime
import com.decli.codehelper.util.CodeSpeech

/** 出示页取件码字号上限：比卡片再放大一档，柜台上一米外也能看清 */
private const val SHOW_CODE_MAX_FONT_SP = 150

/**
 * 全屏出示页：把屏幕临时调到最亮、保持常亮，只做一件事——把取件码给驿站看清楚。
 * 系统返回键 = 关闭。
 */
@Composable
fun ShowCodeScreen(
    item: PickupCodeItem,
    isSpeaking: Boolean,
    speechAvailable: Boolean,
    onClose: () -> Unit,
    onCopyCode: () -> Unit,
    onSpeakCode: () -> Unit,
    onMarkPickedUp: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // 进入时临时提亮 + 常亮，退出时恢复原值
    DisposableEffect(activity) {
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness
        if (window != null) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (window != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = originalBrightness
                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    BackHandler(onBack = onClose)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val enterDuration = if (animationsEnabled()) 250 else 0
    val slideOffsetPx = with(LocalDensity.current) { 24.dp.roundToPx() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = enterDuration),
                initialOffsetY = { slideOffsetPx },
            ) + fadeIn(animationSpec = tween(durationMillis = enterDuration)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "出示给驿站工作人员",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.senderShort,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${formatSmsTime(item.receivedAtMillis)} 到店",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "取件码",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AutoSizeCodeLines(
                        codes = item.codes,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = CodeSpeech.codeContentDescription(item.codes)
                            },
                        maxFontSizeSp = SHOW_CODE_MAX_FONT_SP,
                        scaleWithSetting = false,
                        lineGap = 8.dp,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrightnessAuto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "已临时调到最亮",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ActionButton(
                            text = "复制",
                            icon = Icons.Rounded.ContentCopy,
                            onClick = onCopyCode,
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            height = 56.dp,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            iconSize = 22.dp,
                        )
                        ActionButton(
                            text = if (isSpeaking) "正在读…" else "读给我听",
                            icon = Icons.Rounded.VolumeUp,
                            onClick = onSpeakCode,
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            height = 56.dp,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            iconSize = 22.dp,
                            enabled = speechAvailable,
                        )
                    }
                    ActionButton(
                        text = "我已取到",
                        icon = Icons.Rounded.Check,
                        onClick = onMarkPickedUp,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        height = 68.dp,
                        shape = RoundedCornerShape(20.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                        iconSize = 26.dp,
                    )
                }
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
