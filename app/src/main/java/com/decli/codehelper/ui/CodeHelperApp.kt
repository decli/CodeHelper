package com.decli.codehelper.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.home.HomeUiState
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.theme.AccentBlue
import com.decli.codehelper.ui.theme.AccentBlueContainer
import com.decli.codehelper.ui.theme.AccentGold
import com.decli.codehelper.ui.theme.AccentGreen
import com.decli.codehelper.ui.theme.AccentTerracotta
import com.decli.codehelper.ui.theme.AccentTerracottaContainer
import com.decli.codehelper.ui.theme.AppBackground
import com.decli.codehelper.ui.theme.AppBackgroundShade
import com.decli.codehelper.ui.theme.CardSurface
import com.decli.codehelper.ui.theme.DividerTint
import com.decli.codehelper.ui.theme.DrawerAction
import com.decli.codehelper.ui.theme.HeroCool
import com.decli.codehelper.ui.theme.HeroSurface
import com.decli.codehelper.ui.theme.HeroWarm
import com.decli.codehelper.ui.theme.Ink
import com.decli.codehelper.ui.theme.InkMuted
import com.decli.codehelper.ui.theme.PendingCardBorder
import com.decli.codehelper.ui.theme.PendingCardSurface
import com.decli.codehelper.ui.theme.PickedCardBorder
import com.decli.codehelper.ui.theme.PickedCardSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CodeHelperApp(
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRuleEditor by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updatePermissionStatus(granted)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
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

    if (showRuleEditor) {
        RulesEditorSheet(
            initialRules = uiState.activeRules,
            onDismissRequest = { showRuleEditor = false },
            onSave = { rules ->
                if (viewModel.saveRules(rules)) {
                    showRuleEditor = false
                }
            },
        )
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppBackground, AppBackgroundShade),
                    ),
                )
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 780.dp)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
                    .imePadding(),
                contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    HomeHeaderCard(
                        uiState = uiState,
                        onFilterSelected = viewModel::selectFilter,
                        onForceRefreshAll = viewModel::forceRefreshAll,
                        onEditRules = { showRuleEditor = true },
                    )
                }

                when {
                    !uiState.hasSmsPermission -> {
                        item {
                            PermissionCard(
                                onRequestPermission = {
                                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                                },
                            )
                        }
                    }

                    uiState.isLoading && uiState.items.isEmpty() -> {
                        item {
                            LoadingCard()
                        }
                    }

                    uiState.items.isEmpty() -> {
                        item {
                            EmptyStateCard(
                                title = if (uiState.showAllItems) {
                                    "当前时间范围没有取件码"
                                } else {
                                    "当前时间范围没有未取件码"
                                },
                                subtitle = if (uiState.showAllItems) {
                                    "可以切换时间筛选，或者修改正则规则后重新读取短信。"
                                } else {
                                    "如果想回看已取件记录，请点击“强制刷新所有”。"
                                },
                            )
                        }
                    }

                    else -> {
                        items(uiState.items, key = { it.uniqueKey }) { item ->
                            PickupCodeCard(
                                item = item,
                                showRestoreAction = uiState.showAllItems && item.isPickedUp,
                                onMarkPickedUp = { viewModel.markPickedUp(item) },
                                onRestorePending = { viewModel.restorePending(item) },
                                onOpenSms = {
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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeaderCard(
    uiState: HomeUiState,
    onFilterSelected: (CodeFilterWindow) -> Unit,
    onForceRefreshAll: () -> Unit,
    onEditRules: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = HeroSurface),
        border = BorderStroke(1.dp, DividerTint),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(HeroSurface, HeroCool),
                    ),
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "取件码助手",
                        style = MaterialTheme.typography.displayMedium,
                        color = Ink,
                    )
                    Text(
                        text = "默认读取 12 小时内未取件短信，适合老人直接查看和确认。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkMuted,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = HeroWarm.copy(alpha = 0.9f),
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(14.dp)
                            .size(30.dp),
                        imageVector = Icons.Rounded.MarkunreadMailbox,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoPill(title = "未取件码", value = "${uiState.pendingCount} 个", dotColor = AccentTerracotta)
                InfoPill(title = "当前范围", value = uiState.selectedFilter.label, dotColor = AccentBlue)
                InfoPill(title = "命中规则", value = "${uiState.activeRules.size} 条", dotColor = AccentGreen)
                InfoPill(title = "最近更新", value = formatSyncTime(uiState.lastLoadedAtMillis), dotColor = AccentGold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "时间筛选",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CodeFilterWindow.entries.forEach { filter ->
                        FilterChip(
                            selected = filter == uiState.selectedFilter,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                )
                            },
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryActionCard(
                    modifier = Modifier.widthIn(min = 180.dp),
                    title = "未取件码",
                    value = "${uiState.pendingCount} 个",
                    containerColor = AccentTerracottaContainer,
                    accent = AccentTerracotta,
                )

                FilledTonalButton(
                    onClick = onForceRefreshAll,
                    enabled = uiState.hasSmsPermission,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "强制刷新所有")
                }

                OutlinedButton(onClick = onEditRules) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "设置")
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentBlueContainer),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.42f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(AccentBlue),
                    )
                }
            }

            Text(
                text = if (uiState.showAllItems) {
                    "当前列表已包含已取件和未取件。已取件始终排在后面，未取件统计只计算未取件个数。"
                } else {
                    "当前只展示未取件码。左滑卡片展开“已取件”抽屉，点击后会立即从列表消失。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun InfoPill(
    title: String,
    value: String,
    dotColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardSurface.copy(alpha = 0.94f),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
            }
        }
    }
}

@Composable
private fun SummaryActionCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    containerColor: Color,
    accent: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.DoneAll,
                contentDescription = null,
                tint = accent,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onRequestPermission: () -> Unit,
) {
    StateCard(
        icon = Icons.Rounded.Security,
        iconTint = AccentTerracotta,
        iconBackground = AccentTerracottaContainer,
        title = "需要短信权限才能读取取件码",
        subtitle = "应用不会联网，只会在本机读取短信并提取取件码。授权后默认展示最近 12 小时未取件短信。",
        action = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestPermission,
            ) {
                Text(text = "授权读取短信")
            }
        },
    )
}

@Composable
private fun LoadingCard() {
    StateCard(
        icon = Icons.Rounded.Sync,
        iconTint = AccentBlue,
        iconBackground = AccentBlueContainer,
        title = "正在读取短信",
        subtitle = "请稍等，取件码列表会根据当前时间筛选和规则自动刷新。",
        action = {},
    )
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
) {
    StateCard(
        icon = Icons.Rounded.AutoAwesome,
        iconTint = AccentGold,
        iconBackground = HeroWarm.copy(alpha = 0.55f),
        title = title,
        subtitle = subtitle,
        action = {},
    )
}

@Composable
private fun StateCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DividerTint),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackground,
            ) {
                Icon(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(34.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )

            action()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickupCodeCard(
    item: PickupCodeItem,
    showRestoreAction: Boolean,
    onMarkPickedUp: () -> Unit,
    onRestorePending: () -> Unit,
    onOpenSms: suspend () -> Unit,
) {
    val content: @Composable () -> Unit = {
        PickupCodeCardBody(
            item = item,
            showRestoreAction = showRestoreAction,
            onRestorePending = onRestorePending,
            onOpenSms = onOpenSms,
        )
    }

    if (item.isPickedUp) {
        content()
    } else {
        SwipeActionContainer(
            actionLabel = "已取件",
            onActionClick = onMarkPickedUp,
        ) {
            content()
        }
    }
}

@Composable
private fun PickupCodeCardBody(
    item: PickupCodeItem,
    showRestoreAction: Boolean,
    onRestorePending: () -> Unit,
    onOpenSms: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val containerColor = if (item.isPickedUp) PickedCardSurface else PendingCardSurface
    val borderColor = if (item.isPickedUp) PickedCardBorder else PendingCardBorder
    val statusColor = if (item.isPickedUp) AccentGreen else AccentTerracotta

    Card(
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
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    text = if (item.isPickedUp) "已取件" else "未取件",
                    accent = statusColor,
                )
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkMuted,
                )
            }

            Text(
                text = item.code,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                color = Ink,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "短信内容",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerTint),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "命中规则：${item.matchedRule}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FooterHint(
                        icon = Icons.Rounded.WatchLater,
                        text = "双击打开短信",
                        containerColor = CardSurface.copy(alpha = 0.72f),
                        tint = AccentBlue,
                    )

                    if (showRestoreAction) {
                        TextButton(onClick = onRestorePending) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "恢复未取件")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterHint(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    tint: Color,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                contentDescription = null,
                tint = tint,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Ink,
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSurface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Ink,
            )
        }
    }
}

@Composable
private fun SwipeActionContainer(
    actionLabel: String,
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
            .clip(RoundedCornerShape(30.dp)),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(DrawerAction),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .background(DrawerAction),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardSurface,
                        contentColor = DrawerAction,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = actionLabel)
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RulesEditorSheet(
    initialRules: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val rules = remember(initialRules) {
        mutableStateListOf<String>().apply {
            addAll(initialRules)
        }
    }
    val templates = remember {
        listOf(
            """取件码[：:\s]*([A-Za-z0-9-]+)""",
            """凭[：:\s]*([A-Za-z0-9-]+)""",
            """货码[：:\s]*([A-Za-z0-9-]+)""",
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.imePadding(),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "规则设置",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
            Text(
                text = "每行填写一条完整正则。保存后会立即重新读取当前时间范围内的短信，并在卡片左下角显示命中规则。",
                style = MaterialTheme.typography.bodyLarge,
                color = InkMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "常用模板",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    templates.forEach { template ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (template !in rules) {
                                    rules += template
                                }
                            },
                            label = {
                                Text(
                                    text = template,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            rules.indices.forEach { index ->
                OutlinedTextField(
                    value = rules[index],
                    onValueChange = { updated ->
                        rules[index] = updated
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text(text = "规则 ${index + 1}") },
                    supportingText = {
                        Text(text = "示例：取件码[：:\\s]*([A-Za-z0-9-]+)")
                    },
                    trailingIcon = {
                        if (rules.size > 1) {
                            TextButton(onClick = { rules.removeAt(index) }) {
                                Text(text = "删除")
                            }
                        }
                    },
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { rules += "" }) {
                    Icon(
                        imageVector = Icons.Rounded.Rule,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "新增规则")
                }

                Button(onClick = { onSave(rules.toList()) }) {
                    Text(text = "保存并刷新")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

private suspend fun openSmsOrConversation(
    context: Context,
    item: PickupCodeItem,
    onMessage: suspend (String) -> Unit,
) {
    val intents = buildList {
        add(
            Intent(
                Intent.ACTION_VIEW,
                ContentUris.withAppendedId(Telephony.Sms.Inbox.CONTENT_URI, item.smsId),
            ),
        )
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

private fun formatTime(millis: Long): String =
    Instant
        .ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

private fun formatSyncTime(millis: Long?): String {
    if (millis == null) return "未加载"
    return Instant
        .ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}
