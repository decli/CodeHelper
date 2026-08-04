package com.decli.codehelper.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.home.HomeUiState
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.settings.SettingsScreen
import com.decli.codehelper.util.BadgeNotifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CodeHelperApp(
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showTimeSheet by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionGranted by remember {
        mutableStateOf(BadgeNotifier.hasNotificationPermission(context))
    }
    val appVersionName = remember(context) { context.appVersionName() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updatePermissionStatus(granted)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        notificationPermissionGranted = BadgeNotifier.hasNotificationPermission(context)
        if (notificationPermissionGranted) {
            BadgeNotifier.updateBadge(context, uiState.pendingCount)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
                notificationPermissionGranted = BadgeNotifier.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.pickedUpEvents.collect { item ->
            val result = snackbarHostState.showSnackbar(
                message = "已把「${item.codes.joinToString("、")}」标记为已取到",
                actionLabel = "撤销",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePending(item)
            }
        }
    }

    if (showTimeSheet) {
        TimeFilterSheet(
            selectedFilter = uiState.selectedFilter,
            options = remember {
                CodeFilterWindow.entries.filterNot { it == CodeFilterWindow.Last12Hours }
            },
            onDismissRequest = { showTimeSheet = false },
            onSelect = { filter ->
                showTimeSheet = false
                viewModel.selectFilter(filter)
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (showSettings) {
                BackHandler { showSettings = false }
                SettingsScreen(
                    initialPromptKeywords = uiState.activePromptKeywords,
                    initialAdvancedRules = uiState.activeAdvancedRules,
                    initialBadgeRefreshMinutes = uiState.badgeRefreshMinutes,
                    notificationPermissionGranted = notificationPermissionGranted,
                    appVersionName = appVersionName,
                    onBack = { showSettings = false },
                    onSave = { promptKeywords, advancedRules, badgeRefreshMinutes ->
                        if (
                            viewModel.saveExtractorSettings(
                                candidatePromptKeywords = promptKeywords,
                                candidateAdvancedRules = advancedRules,
                            )
                        ) {
                            viewModel.saveBadgeRefreshMinutes(badgeRefreshMinutes)
                            showSettings = false
                        }
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationPermissionGranted = true
                        }
                    },
                    onRestoreDefaults = {
                        viewModel.resetRulesToDefault()
                        showSettings = false
                    },
                )
            } else {
                HomeContent(
                    uiState = uiState,
                    onSelect12Hours = { viewModel.selectFilter(CodeFilterWindow.Last12Hours) },
                    onOpenTimeSheet = { showTimeSheet = true },
                    onShowAll = viewModel::forceRefreshAll,
                    onShowPendingOnly = { viewModel.selectFilter(uiState.selectedFilter) },
                    onOpenSettings = { showSettings = true },
                    onGrantPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_SMS)
                    },
                    onMarkPickedUp = viewModel::markPickedUp,
                    onRestorePending = viewModel::restorePending,
                    onOpenSms = { item ->
                        openSmsOrConversation(
                            context = context,
                            item = item,
                            onMessage = { snackbarHostState.showSnackbar(it) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSelect12Hours: () -> Unit,
    onOpenTimeSheet: () -> Unit,
    onShowAll: () -> Unit,
    onShowPendingOnly: () -> Unit,
    onOpenSettings: () -> Unit,
    onGrantPermission: () -> Unit,
    onMarkPickedUp: (PickupCodeItem) -> Unit,
    onRestorePending: (PickupCodeItem) -> Unit,
    onOpenSms: suspend (PickupCodeItem) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .imePadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                HomeHeader(onOpenSettings = onOpenSettings)
            }

            item(key = "hero") {
                HeroCard(
                    pendingCount = uiState.pendingCount,
                    rangeLabel = rangeLabel(uiState.selectedFilter),
                    isLoading = uiState.isLoading,
                    hasSmsPermission = uiState.hasSmsPermission,
                )
            }

            item(key = "time-filter") {
                TimeSegmentedControl(
                    selectedFilter = uiState.selectedFilter,
                    onSelect12Hours = onSelect12Hours,
                    onOpenTimeSheet = onOpenTimeSheet,
                )
            }

            item(key = "list-mode") {
                ListModeTabs(
                    showAll = uiState.showAllItems && uiState.hasSmsPermission,
                    allTabEnabled = uiState.hasSmsPermission,
                    onSelectPending = onShowPendingOnly,
                    onSelectAll = onShowAll,
                )
            }

            when {
                !uiState.hasSmsPermission -> {
                    item(key = "state-permission") {
                        StateCard(
                            icon = Icons.Rounded.VerifiedUser,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.primaryContainer,
                            title = "需要允许读取短信\n才能帮您找取件码",
                            subtitle = "应用不联网、不上传、不发送短信，只在这台手机上帮您查找取件码。",
                            actionText = "允许读取短信",
                            onAction = onGrantPermission,
                        )
                    }
                }

                uiState.isLoading && uiState.items.isEmpty() -> {
                    item(key = "state-loading") {
                        StateCard(
                            showProgress = true,
                            title = "正在读取短信",
                            subtitle = "马上就好，页面会按当前时间范围自动刷新。",
                        )
                    }
                }

                uiState.items.isEmpty() -> {
                    item(key = "state-empty") {
                        if (uiState.showAllItems) {
                            StateCard(
                                icon = Icons.Rounded.Inventory2,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                                title = "当前时间范围没有取件码",
                                subtitle = "可以换个时间范围，或到「设置」调整识别提示词。",
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
                                subtitle = "${rangeLabel(uiState.selectedFilter)}内没有待取的取件码。想查看更早的短信，可以换个时间范围。",
                                actionText = "看更早的短信",
                                actionIsPrimary = false,
                                onAction = onOpenTimeSheet,
                            )
                        }
                    }
                }

                else -> {
                    items(uiState.items, key = { it.uniqueKey }) { item ->
                        PickupCodeCard(
                            item = item,
                            promptKeywords = uiState.activePromptKeywords,
                            onMarkPickedUp = { onMarkPickedUp(item) },
                            onRestorePending = { onRestorePending(item) },
                            onOpenSms = { onOpenSms(item) },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────── 首页头部 ───────────────────────────

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
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Text(
                text = "取件码助手",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Surface(
            onClick = onOpenSettings,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ─────────────────────────── 英雄区 ───────────────────────────

@Composable
private fun HeroCard(
    pendingCount: Int,
    rangeLabel: String,
    isLoading: Boolean,
    hasSmsPermission: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                Text(
                    text = "$pendingCount",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (pendingCount == 0) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "个包裹待取",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = when {
                    !hasSmsPermission -> "等待授权后开始读取"
                    isLoading -> "正在读取短信…"
                    else -> "已自动读取本机短信 · 全程离线"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────── 分段控件（全应用统一的选中语言：柿橙实心底 + 对勾） ───────────────

@Composable
private fun SegmentedCard(
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = content,
        )
    }
}

@Composable
private fun Segment(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = false,
    clickableWhenSelected: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            )
            .clickable(
                enabled = enabled && (!selected || clickableWhenSelected),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = contentColor,
            )
            if (showArrow) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = contentColor,
                )
            }
        }
    }
}

// ─────────────────────────── 时间筛选 ───────────────────────────

@Composable
private fun TimeSegmentedControl(
    selectedFilter: CodeFilterWindow,
    onSelect12Hours: () -> Unit,
    onOpenTimeSheet: () -> Unit,
) {
    val is12Hours = selectedFilter == CodeFilterWindow.Last12Hours
    SegmentedCard {
        Segment(
            modifier = Modifier.weight(1f),
            selected = is12Hours,
            label = "12小时",
            onClick = onSelect12Hours,
        )
        Segment(
            modifier = Modifier.weight(1f),
            selected = !is12Hours,
            label = if (is12Hours) "其它时间" else selectedFilter.label,
            showArrow = true,
            // 选中「3天内」等档位后仍可再点开面板换档
            clickableWhenSelected = true,
            onClick = onOpenTimeSheet,
        )
    }
}

// ─────────────────────────── 待取 / 全部 双标签 ───────────────────────────

@Composable
private fun ListModeTabs(
    showAll: Boolean,
    allTabEnabled: Boolean,
    onSelectPending: () -> Unit,
    onSelectAll: () -> Unit,
) {
    SegmentedCard {
        Segment(
            modifier = Modifier.weight(1f),
            selected = !showAll,
            label = "待取包裹",
            onClick = onSelectPending,
        )
        Segment(
            modifier = Modifier.weight(1f),
            selected = showAll,
            label = "全部包裹",
            enabled = allTabEnabled,
            onClick = onSelectAll,
        )
    }
}

// ─────────────────────────── 取件码卡片 ───────────────────────────

@Composable
private fun PickupCodeCard(
    item: PickupCodeItem,
    promptKeywords: List<String>,
    onMarkPickedUp: () -> Unit,
    onRestorePending: () -> Unit,
    onOpenSms: suspend () -> Unit,
) {
    if (item.isPickedUp) {
        SwipeActionContainer(
            actionLabel = "恢复未取",
            actionIcon = Icons.Rounded.Undo,
            actionBackground = MaterialTheme.colorScheme.primary,
            actionContentColor = MaterialTheme.colorScheme.onPrimary,
            onActionClick = onRestorePending,
        ) {
            PickupCodeCardBody(
                item = item,
                promptKeywords = promptKeywords,
                onOpenSms = onOpenSms,
                onMarkPickedUp = null,
                onRestorePending = onRestorePending,
            )
        }
    } else {
        SwipeActionContainer(
            actionLabel = "我已取到",
            actionIcon = Icons.Rounded.Check,
            actionBackground = MaterialTheme.colorScheme.tertiary,
            actionContentColor = MaterialTheme.colorScheme.onTertiary,
            onActionClick = onMarkPickedUp,
        ) {
            PickupCodeCardBody(
                item = item,
                promptKeywords = promptKeywords,
                onOpenSms = onOpenSms,
                onMarkPickedUp = onMarkPickedUp,
                onRestorePending = null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickupCodeCardBody(
    item: PickupCodeItem,
    promptKeywords: List<String>,
    onOpenSms: suspend () -> Unit,
    onMarkPickedUp: (() -> Unit)?,
    onRestorePending: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onDoubleClick = {
                    scope.launch {
                        onOpenSms()
                    }
                },
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(isPickedUp = item.isPickedUp, codeCount = item.codeCount)
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TicketDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "取件码",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 自适应字号：在不折行的前提下让取件码尽可能大；
                // maxLines = 码数，任何一行放不下即触发整体缩小，多个码保持同一字号。
                BasicText(
                    text = item.codeDisplay,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        lineHeight = 1.3.em,
                        textAlign = TextAlign.Center,
                        color = if (item.isPickedUp) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                    maxLines = item.codeCount,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 20.sp,
                        maxFontSize = 72.sp,
                        stepSize = 1.sp,
                    ),
                )
            }

            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.matchedRules.isNotEmpty()) {
                Text(
                    text = "识别依据：${matchedRuleLabel(item.matchedRules, promptKeywords)}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CardActionButton(
                    modifier = Modifier.weight(1f),
                    text = "查看短信",
                    icon = Icons.Rounded.ChatBubbleOutline,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        scope.launch {
                            onOpenSms()
                        }
                    },
                )
                if (onMarkPickedUp != null) {
                    CardActionButton(
                        modifier = Modifier.weight(1.35f),
                        text = "我已取到",
                        icon = Icons.Rounded.Check,
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        onClick = onMarkPickedUp,
                    )
                }
                if (onRestorePending != null) {
                    // 老人不一定会左滑手势，已取件卡片同时保留可见的恢复按钮
                    CardActionButton(
                        modifier = Modifier.weight(1.35f),
                        text = "恢复未取",
                        icon = Icons.Rounded.Undo,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onRestorePending,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    isPickedUp: Boolean,
    codeCount: Int,
) {
    val backgroundColor: Color
    val contentColor: Color
    if (isPickedUp) {
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = CircleShape,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isPickedUp) Icons.Rounded.CheckCircle else Icons.Rounded.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = contentColor,
            )
            Text(
                text = when {
                    isPickedUp -> "已取件"
                    codeCount > 1 -> "未取件 · ${codeCount} 个码"
                    else -> "未取件"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
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
            .height(18.dp),
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = dashColor,
            start = Offset(14.dp.toPx(), centerY),
            end = Offset(size.width - 14.dp.toPx(), centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
        drawCircle(color = notchColor, radius = 9.dp.toPx(), center = Offset(0f, centerY))
        drawCircle(color = notchColor, radius = 9.dp.toPx(), center = Offset(size.width, centerY))
    }
}

@Composable
private fun CardActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

// ─────────────────────────── 左滑抽屉（未取件=标已取，已取件=恢复未取） ───────────────────────────

@Composable
private fun SwipeActionContainer(
    actionLabel: String,
    actionIcon: ImageVector,
    actionBackground: Color,
    actionContentColor: Color,
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
            .clip(RoundedCornerShape(24.dp)),
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

// ─────────────────────────── 状态卡片 ───────────────────────────

@Composable
private fun StateCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    showProgress: Boolean = false,
    actionText: String? = null,
    actionIsPrimary: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 5.dp,
                )
            } else if (icon != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(84.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = iconTint,
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (actionText != null && onAction != null) {
                if (actionIsPrimary) {
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 19.sp),
                        )
                    }
                } else {
                    Surface(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────── 时间范围弹层 ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFilterSheet(
    selectedFilter: CodeFilterWindow,
    options: List<CodeFilterWindow>,
    onDismissRequest: () -> Unit,
    onSelect: (CodeFilterWindow) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.imePadding(),
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
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Text(
                text = "选择时间范围",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "选好后立刻重新读取短信",
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            options.forEach { filter ->
                TimeOptionRow(
                    filter = filter,
                    selected = filter == selectedFilter,
                    onClick = { onSelect(filter) },
                )
            }

            Surface(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeOptionRow(
    filter: CodeFilterWindow,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
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
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(2.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }
            Text(
                text = sheetOptionLabel(filter),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ─────────────────────────── 工具函数 ───────────────────────────

private fun rangeLabel(filter: CodeFilterWindow): String =
    if (filter == CodeFilterWindow.Last12Hours) {
        "最近 12 小时"
    } else {
        "最近 ${filter.hours / 24} 天"
    }

private fun sheetOptionLabel(filter: CodeFilterWindow): String = "最近 ${filter.hours / 24} 天"

/**
 * 把提取器的技术标签（命中规则N）翻译成老人能看懂的识别依据：
 * 规则号落在提示词范围内时直接显示命中的提示词，超出部分归为高级规则。
 */
private fun matchedRuleLabel(
    matchedRules: List<String>,
    promptKeywords: List<String>,
): String =
    matchedRules.joinToString(separator = "、") { rule ->
        val ruleNumber = rule.removePrefix("命中规则").toIntOrNull()
        when {
            ruleNumber == null || ruleNumber < 1 -> rule
            ruleNumber <= promptKeywords.size -> "「${promptKeywords[ruleNumber - 1]}」"
            else -> "高级规则${ruleNumber - promptKeywords.size}"
        }
    }

private suspend fun openSmsOrConversation(
    context: Context,
    item: PickupCodeItem,
    onMessage: suspend (String) -> Unit,
) {
    val intents = buildList {
        val directUri = item.messageUri?.let(Uri::parse)
            ?: ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, item.smsId)
        add(Intent(Intent.ACTION_VIEW, directUri))
        if (item.sender.isNotBlank()) {
            add(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("sms:${Uri.encode(item.sender)}"),
                ),
            )
        }
    }.map { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val resolvedIntent = intents.firstOrNull { intent ->
        intent.resolveActivity(context.packageManager) != null
    }

    if (resolvedIntent == null) {
        onMessage("当前系统短信应用不支持直接打开短信")
        return
    }

    runCatching {
        context.startActivity(resolvedIntent)
    }.onFailure {
        onMessage("打开短信失败，请确认系统短信应用可用")
        return
    }

    if (resolvedIntent.data?.scheme == "sms") {
        onMessage("当前系统未定位到单条短信，已打开对应短信会话")
    }
}

@Suppress("DEPRECATION")
private fun Context.appVersionName(): String =
    runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    }.getOrDefault("").ifBlank { "未知版本" }

private fun formatTime(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val timeText = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (dateTime.toLocalDate()) {
        today -> "今天 $timeText"
        today.minusDays(1) -> "昨天 $timeText"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
}
