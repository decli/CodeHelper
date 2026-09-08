package com.decli.codehelper.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.components.ActionButton
import com.decli.codehelper.ui.components.AppCard
import com.decli.codehelper.ui.components.AutoSizeCodeLines
import com.decli.codehelper.ui.components.SegmentedControl
import com.decli.codehelper.ui.components.TextActionButton
import com.decli.codehelper.ui.components.animationsEnabled
import com.decli.codehelper.ui.components.motionDuration
import com.decli.codehelper.ui.formatSmsTime
import com.decli.codehelper.ui.rangeLabel
import com.decli.codehelper.ui.theme.HeroNumber
import com.decli.codehelper.ui.theme.cardBorder
import com.decli.codehelper.ui.theme.cardShadowElevation
import com.decli.codehelper.util.CodeSpeech
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ListMode(val label: String) {
    Pending("待取"),
    All("全部包裹"),
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    groupBySender: Boolean,
    speakingKey: String?,
    speechAvailable: Boolean,
    onOpenTimeSheet: () -> Unit,
    onSelectPending: () -> Unit,
    onSelectAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onGrantPermission: () -> Unit,
    onMarkPickedUp: (PickupCodeItem) -> Unit,
    onRestorePending: (PickupCodeItem) -> Unit,
    onOpenSms: (PickupCodeItem) -> Unit,
    onCopyCode: (PickupCodeItem) -> Unit,
    onSpeakCode: (PickupCodeItem) -> Unit,
    onShowCode: (PickupCodeItem) -> Unit,
) {
    // 读取过快时不闪加载态：超过 300ms 才显示
    var showLoadingCard by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            delay(300)
            showLoadingCard = true
        } else {
            showLoadingCard = false
        }
    }

    val rows = remember(uiState.items, groupBySender) {
        buildHomeRows(items = uiState.items, groupBySender = groupBySender)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .imePadding(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                HomeHeader(onOpenSettings = onOpenSettings)
            }

            item(key = "hero") {
                HeroSection(
                    pendingCount = uiState.pendingCount,
                    rangeLabel = rangeLabel(uiState.selectedFilter),
                    isLoading = uiState.isLoading,
                    hasSmsPermission = uiState.hasSmsPermission,
                    onOpenTimeSheet = onOpenTimeSheet,
                )
            }

            item(key = "list-mode") {
                SegmentedControl(
                    options = listOf(ListMode.Pending, ListMode.All),
                    selected = if (uiState.showAllItems && uiState.hasSmsPermission) ListMode.All else ListMode.Pending,
                    labelOf = { it.label },
                    enabledOf = { it == ListMode.Pending || uiState.hasSmsPermission },
                    onSelect = { mode ->
                        if (mode == ListMode.All) onSelectAll() else onSelectPending()
                    },
                    textStyle = MaterialTheme.typography.titleMedium,
                )
            }

            when {
                !uiState.hasSmsPermission -> {
                    item(key = "state-permission") {
                        StateCard(
                            icon = Icons.Rounded.VerifiedUser,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.primaryContainer,
                            title = "需要允许读取短信",
                            titleIsLarge = true,
                            subtitle = "取件码都在短信里。应用不联网、不上传、不发送短信，只在这台手机上帮您找。",
                            actionText = "允许读取短信",
                            onAction = onGrantPermission,
                        )
                    }
                }

                uiState.isLoading && uiState.items.isEmpty() && showLoadingCard -> {
                    item(key = "state-loading") {
                        LoadingCard()
                    }
                }

                uiState.items.isEmpty() && !uiState.isLoading -> {
                    item(key = "state-empty") {
                        if (uiState.showAllItems) {
                            StateCard(
                                icon = Icons.Rounded.Inventory2,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                                title = "这段时间没有取件码",
                                subtitle = "可以换个时间范围，或到「设置」检查识别提示词。",
                                actionText = "看更早的短信",
                                actionIsPrimary = false,
                                onAction = onOpenTimeSheet,
                            )
                        } else {
                            StateCard(
                                icon = Icons.Rounded.TaskAlt,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                                title = "包裹都取完了",
                                subtitle = "${rangeLabel(uiState.selectedFilter)}内没有待取的包裹。想看更早的，可以换个时间范围。",
                                actionText = "看更早的短信",
                                actionIsPrimary = false,
                                onAction = onOpenTimeSheet,
                            )
                        }
                    }
                }

                else -> {
                    items(
                        items = rows,
                        key = { row -> row.key },
                    ) { row ->
                        Box(modifier = Modifier.animateItem()) {
                            HomeListRow(
                                row = row,
                                speakingKey = speakingKey,
                                speechAvailable = speechAvailable,
                                onMarkPickedUp = onMarkPickedUp,
                                onRestorePending = onRestorePending,
                                onOpenSms = onOpenSms,
                                onCopyCode = onCopyCode,
                                onSpeakCode = onSpeakCode,
                                onShowCode = onShowCode,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeListRow(
    row: HomeRow,
    speakingKey: String?,
    speechAvailable: Boolean,
    onMarkPickedUp: (PickupCodeItem) -> Unit,
    onRestorePending: (PickupCodeItem) -> Unit,
    onOpenSms: (PickupCodeItem) -> Unit,
    onCopyCode: (PickupCodeItem) -> Unit,
    onSpeakCode: (PickupCodeItem) -> Unit,
    onShowCode: (PickupCodeItem) -> Unit,
) {
    when (row) {
        is HomeRow.GroupHeader -> GroupHeaderRow(
            title = row.title,
            codeCount = row.codeCount,
        )

        is HomeRow.Pending -> SwipeActionContainer(
            actionLabel = "我已取到",
            actionIcon = Icons.Rounded.Check,
            actionBackground = MaterialTheme.colorScheme.tertiary,
            actionContentColor = MaterialTheme.colorScheme.onTertiary,
            cornerRadiusDp = 24,
            onActionClick = { onMarkPickedUp(row.item) },
        ) {
            PendingCodeCard(
                item = row.item,
                isSpeaking = speakingKey == row.item.uniqueKey,
                speechAvailable = speechAvailable,
                onMarkPickedUp = { onMarkPickedUp(row.item) },
                onOpenSms = { onOpenSms(row.item) },
                onCopyCode = { onCopyCode(row.item) },
                onSpeakCode = { onSpeakCode(row.item) },
                onShowCode = { onShowCode(row.item) },
            )
        }

        is HomeRow.PickedUp -> SwipeActionContainer(
            actionLabel = "恢复未取",
            actionIcon = Icons.AutoMirrored.Rounded.Undo,
            actionBackground = MaterialTheme.colorScheme.primary,
            actionContentColor = MaterialTheme.colorScheme.onPrimary,
            cornerRadiusDp = 20,
            onActionClick = { onRestorePending(row.item) },
        ) {
            PickedUpRow(
                item = row.item,
                onRestorePending = { onRestorePending(row.item) },
            )
        }
    }
}

// ─────────────────────────── 顶栏 ───────────────────────────

@Composable
private fun HomeHeader(
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "取件码助手",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
            onClick = onOpenSettings,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = cardShadowElevation,
            border = cardBorder,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "设置",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ─────────────────────────── 英雄区（去卡片化） ───────────────────────────

@Composable
private fun HeroSection(
    pendingCount: Int,
    rangeLabel: String,
    isLoading: Boolean,
    hasSmsPermission: Boolean,
    onOpenTimeSheet: () -> Unit,
) {
    val targetColor = if (pendingCount == 0) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val numberColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = motionDuration(300)),
        label = "heroNumberColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = when {
                    !hasSmsPermission -> "等待授权"
                    isLoading -> "正在读取短信…"
                    else -> "待取包裹"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                Text(
                    text = "$pendingCount",
                    modifier = Modifier.alignByBaseline(),
                    style = HeroNumber,
                    color = numberColor,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "个",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        RangeChip(
            label = rangeLabel,
            modifier = Modifier.padding(bottom = 6.dp),
            onClick = onOpenTimeSheet,
        )
    }
}

/** 时间范围胶囊：显示当前档位，点击打开时间面板 */
@Composable
private fun RangeChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "换时间范围",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─────────────────────────── 分组标题 ───────────────────────────

@Composable
private fun GroupHeaderRow(
    title: String,
    codeCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = "$codeCount 件",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ─────────────────────────── 待取卡 ───────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PendingCodeCard(
    item: PickupCodeItem,
    isSpeaking: Boolean,
    speechAvailable: Boolean,
    onMarkPickedUp: () -> Unit,
    onOpenSms: () -> Unit,
    onCopyCode: () -> Unit,
    onSpeakCode: () -> Unit,
    onShowCode: () -> Unit,
) {
    val cardInteractionSource = remember { MutableInteractionSource() }
    AppCard(
        modifier = Modifier.combinedClickable(
            interactionSource = cardInteractionSource,
            indication = null,
            onClick = {},
            onDoubleClick = onOpenSms,
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${item.senderShort} · ${formatSmsTime(item.receivedAtMillis)}",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PendingStatusChip()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role = Role.Button,
                        onClick = onShowCode,
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = CodeSpeech.codeContentDescription(
                            codes = item.codes,
                            prefix = "放大出示取件码",
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AutoSizeCodeLines(
                    codes = item.codes,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "点一下取件码可放大出示",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            TicketDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ActionButton(
                    text = "我已取到",
                    icon = Icons.Rounded.Check,
                    onClick = onMarkPickedUp,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    height = 60.dp,
                    shape = RoundedCornerShape(18.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextActionButton(
                        text = "看短信",
                        icon = Icons.Rounded.ChatBubbleOutline,
                        onClick = onOpenSms,
                        modifier = Modifier.weight(1f),
                    )
                    TextActionButton(
                        text = "复制",
                        icon = Icons.Rounded.ContentCopy,
                        onClick = onCopyCode,
                        modifier = Modifier.weight(1f),
                    )
                    SpeakActionButton(
                        isSpeaking = isSpeaking,
                        enabled = speechAvailable,
                        onClick = onSpeakCode,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakActionButton(
    isSpeaking: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 只在朗读时才创建无限动画，避免每张卡片常驻一个每帧重组的动画
    val iconScale = if (isSpeaking && animationsEnabled()) {
        val transition = rememberInfiniteTransition(label = "speakPulse")
        val pulse by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "speakPulseScale",
        )
        pulse
    } else {
        1f
    }
    TextActionButton(
        text = if (isSpeaking) "正在读…" else "读给我听",
        icon = Icons.AutoMirrored.Rounded.VolumeUp,
        onClick = onClick,
        modifier = modifier,
        iconScale = iconScale,
        semanticsLabel = if (isSpeaking) "停止朗读" else "读给我听",
    )
}

@Composable
private fun PendingStatusChip() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = "未取件",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

/** 取件小票撕边：虚线 + 两侧打孔缺口 */
@Composable
private fun TicketDivider() {
    val dashColor = MaterialTheme.colorScheme.outlineVariant
    val notchColor = MaterialTheme.colorScheme.background
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = dashColor,
            start = Offset(16.dp.toPx(), centerY),
            end = Offset(size.width - 16.dp.toPx(), centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
        drawCircle(color = notchColor, radius = 10.dp.toPx(), center = Offset(0f, centerY))
        drawCircle(color = notchColor, radius = 10.dp.toPx(), center = Offset(size.width, centerY))
    }
}

// ─────────────────────────── 已取件摘要行 ───────────────────────────

@Composable
private fun PickedUpRow(
    item: PickupCodeItem,
    onRestorePending: () -> Unit,
) {
    AppCard(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.codes.joinToString(separator = "  "),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "已取 · ${item.senderShort} · ${formatSmsTime(item.receivedAtMillis)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextActionButton(
                text = "恢复",
                icon = Icons.AutoMirrored.Rounded.Undo,
                onClick = onRestorePending,
                semanticsLabel = "恢复为待取",
            )
        }
    }
}

// ─────────────────────────── 左滑抽屉 ───────────────────────────

@Composable
private fun SwipeActionContainer(
    actionLabel: String,
    actionIcon: ImageVector,
    actionBackground: Color,
    actionContentColor: Color,
    cornerRadiusDp: Int,
    onActionClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealWidth = 132.dp
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadiusDp.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(actionBackground),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = actionContentColor,
                    )
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = actionContentColor,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-revealWidthPx, 0f))
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            val shouldReveal = offsetX.value <= (-revealWidthPx * 0.4f) || velocity < -900f
                            offsetX.animateTo(
                                targetValue = if (shouldReveal) -revealWidthPx else 0f,
                                animationSpec = tween(durationMillis = 180),
                            )
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

// ─────────────────────────── 状态卡 ───────────────────────────

@Composable
private fun LoadingCard() {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 5.dp,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = "正在读取短信",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "马上就好，只在这台手机上查找",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    actionText: String,
    onAction: () -> Unit,
    titleIsLarge: Boolean = false,
    actionIsPrimary: Boolean = true,
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .size(96.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = iconTint,
                )
            }
            Text(
                text = title,
                style = if (titleIsLarge) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp)
                },
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            ActionButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                containerColor = if (actionIsPrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (actionIsPrimary) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                height = 60.dp,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
            )
        }
    }
}

// ─────────────────────────── 时间范围面板 ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterSheet(
    selectedFilter: CodeFilterWindow,
    pendingCountByWindow: Map<CodeFilterWindow, Int>?,
    onDismissRequest: () -> Unit,
    onSelect: (CodeFilterWindow) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.imePadding(),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Text(
                text = "选择时间范围",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "选好后立刻重新读取短信",
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CodeFilterWindow.entries.forEach { filter ->
                TimeOptionRow(
                    filter = filter,
                    selected = filter == selectedFilter,
                    pendingCount = pendingCountByWindow?.get(filter),
                    onClick = { onSelect(filter) },
                )
            }

            ActionButton(
                text = "取消",
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                height = 56.dp,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TimeOptionRow(
    filter: CodeFilterWindow,
    selected: Boolean,
    pendingCount: Int?,
    onClick: () -> Unit,
) {
    val countLabel = when {
        pendingCount == null -> "—"
        pendingCount == 0 -> "无待取"
        else -> "$pendingCount 件待取"
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = buildString {
                    append(rangeLabel(filter))
                    append("，")
                    append(countLabel)
                    if (selected) append("，已选中")
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
                )
            }
            Text(
                text = rangeLabel(filter),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
        }
    }
}
