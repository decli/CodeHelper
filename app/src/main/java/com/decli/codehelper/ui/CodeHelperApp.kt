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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.decli.codehelper.ui.theme.AccentBlueContainer
import com.decli.codehelper.ui.theme.AccentGreen
import com.decli.codehelper.ui.theme.AccentGreenContainer
import com.decli.codehelper.ui.theme.AccentTerracotta
import com.decli.codehelper.ui.theme.AppBackground
import com.decli.codehelper.ui.theme.AppBackgroundShade
import com.decli.codehelper.ui.theme.CardSurface
import com.decli.codehelper.ui.theme.DividerTint
import com.decli.codehelper.ui.theme.DrawerAction
import com.decli.codehelper.ui.theme.HeroSurface
import com.decli.codehelper.ui.theme.HeroWarm
import com.decli.codehelper.ui.theme.Ink
import com.decli.codehelper.ui.theme.InkMuted
import com.decli.codehelper.ui.theme.PendingCardBorder
import com.decli.codehelper.ui.theme.PendingCardSurface
import com.decli.codehelper.ui.theme.PickedCardBorder
import com.decli.codehelper.ui.theme.PickedCardSurface
import com.decli.codehelper.util.PickupCodeExtractor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val PanelBorder = Color(0xFF8C8377)
private val ForceShowAllButtonColor = Color(0xFFE0D7EF)
private val SettingsButtonColor = Color(0xFFE2EDFF)
private val PendingSummaryColor = Color(0xFFFFD8D3)

@OptIn(ExperimentalMaterial3Api::class)
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
            onRestoreDefaults = {
                viewModel.resetRulesToDefault()
                showRuleEditor = false
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
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .imePadding(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    HomeDashboard(
                        uiState = uiState,
                        onFilterSelected = viewModel::selectFilter,
                        onForceRefreshAll = viewModel::forceRefreshAll,
                        onEditRules = { showRuleEditor = true },
                    )
                }

                when {
                    !uiState.hasSmsPermission -> {
                        item {
                            StateCard(
                                title = "需要短信权限才能读取取件码",
                                subtitle = "应用不会联网，只会在本机读取短信。授权后会默认展示最近 12 小时内的未取件码。",
                                actionText = "授权读取短信",
                                onAction = {
                                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                                },
                            )
                        }
                    }

                    uiState.isLoading && uiState.items.isEmpty() -> {
                        item {
                            StateCard(
                                title = "正在读取短信",
                                subtitle = "页面会按照当前时间范围和规则立即刷新内容。",
                            )
                        }
                    }

                    uiState.items.isEmpty() -> {
                        item {
                            StateCard(
                                title = if (uiState.showAllItems) {
                                    "当前时间范围没有取件码"
                                } else {
                                    "当前时间范围没有未取件码"
                                },
                                subtitle = if (uiState.showAllItems) {
                                    "可以切换时间范围，或者进入设置调整提取规则。"
                                } else {
                                    "如果想查看已取件记录，请点击“强制展示所有”。"
                                },
                            )
                        }
                    }

                    else -> {
                        items(uiState.items, key = { it.uniqueKey }) { item ->
                            PickupCodeCard(
                                item = item,
                                onMarkPickedUp = { viewModel.markPickedUp(item) },
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

@Composable
private fun HomeDashboard(
    uiState: HomeUiState,
    onFilterSelected: (CodeFilterWindow) -> Unit,
    onForceRefreshAll: () -> Unit,
    onEditRules: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "取件码助手",
            modifier = Modifier.fillMaxWidth(),
            style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
                fontSize = 30.sp,
                lineHeight = 34.sp,
            ),
            color = Ink,
            textAlign = TextAlign.Center,
        )

        TimeFilterPanel(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = onFilterSelected,
        )

        ActionPanel(
            hasSmsPermission = uiState.hasSmsPermission,
            onForceRefreshAll = onForceRefreshAll,
            onEditRules = onEditRules,
        )

        PendingSummaryPanel(
            pendingCount = uiState.pendingCount,
            isLoading = uiState.isLoading,
        )
    }
}

@Composable
private fun TimeFilterPanel(
    selectedFilter: CodeFilterWindow,
    onFilterSelected: (CodeFilterWindow) -> Unit,
) {
    var showOtherFilters by rememberSaveable { mutableStateOf(false) }
    val otherFilters = remember {
        CodeFilterWindow.entries.filterNot { it == CodeFilterWindow.Last12Hours }
    }
    val otherTimeLabel = if (selectedFilter == CodeFilterWindow.Last12Hours) {
        "选择其它时间"
    } else {
        selectedFilter.label
    }

    PanelCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "时间筛选",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = Ink,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectedFilter == CodeFilterWindow.Last12Hours) {
                    FilledPanelButton(
                        modifier = Modifier.weight(1f),
                        text = "12小时",
                        containerColor = HeroWarm,
                        onClick = { onFilterSelected(CodeFilterWindow.Last12Hours) },
                    )
                } else {
                    OutlinedPanelButton(
                        modifier = Modifier.weight(1f),
                        text = "12小时",
                        onClick = { onFilterSelected(CodeFilterWindow.Last12Hours) },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedFilter == CodeFilterWindow.Last12Hours) {
                        OutlinedPanelButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = otherTimeLabel,
                            onClick = { showOtherFilters = true },
                        )
                    } else {
                        FilledPanelButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = otherTimeLabel,
                            containerColor = HeroWarm,
                            onClick = { showOtherFilters = true },
                        )
                    }

                    DropdownMenu(
                        expanded = showOtherFilters,
                        onDismissRequest = { showOtherFilters = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        otherFilters.forEach { filter ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = filter.label,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    showOtherFilters = false
                                    onFilterSelected(filter)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPanel(
    hasSmsPermission: Boolean,
    onForceRefreshAll: () -> Unit,
    onEditRules: () -> Unit,
) {
    PanelCard(containerColor = AccentBlueContainer.copy(alpha = 0.68f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledPanelButton(
                modifier = Modifier.weight(1f),
                text = "强制展示所有",
                containerColor = ForceShowAllButtonColor,
                enabled = hasSmsPermission,
                onClick = onForceRefreshAll,
            )

            FilledPanelButton(
                text = "设置",
                containerColor = SettingsButtonColor,
                onClick = onEditRules,
            )
        }
    }
}

@Composable
private fun PendingSummaryPanel(
    pendingCount: Int,
    isLoading: Boolean,
) {
    PanelCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PendingSummaryColor),
                border = BorderStroke(1.dp, PendingCardBorder),
            ) {
                Text(
                    text = "还未取件码共：$pendingCount 个",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
            }

            if (isLoading) {
                Text(
                    text = "正在刷新当前列表…",
                    modifier = Modifier.fillMaxWidth(),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PanelCard(
    containerColor: Color = HeroSurface.copy(alpha = 0.94f),
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, PanelBorder),
    ) {
        content()
    }
}

@Composable
private fun FilledPanelButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Ink,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = Ink.copy(alpha = 0.5f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
        )
    }
}

@Composable
private fun OutlinedPanelButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PanelBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CardSurface,
            contentColor = Ink,
            disabledContentColor = Ink.copy(alpha = 0.5f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
        )
    }
}

@Composable
private fun StateCard(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DividerTint),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )

            if (actionText != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(text = actionText)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickupCodeCard(
    item: PickupCodeItem,
    onMarkPickedUp: () -> Unit,
    onOpenSms: suspend () -> Unit,
) {
    val content: @Composable () -> Unit = {
        PickupCodeCardBody(
            item = item,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickupCodeCardBody(
    item: PickupCodeItem,
    onOpenSms: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val containerColor = if (item.isPickedUp) PickedCardSurface else PendingCardSurface
    val borderColor = if (item.isPickedUp) PickedCardBorder else PendingCardBorder
    val statusColor = if (item.isPickedUp) AccentGreen else AccentTerracotta
    val codeFontSize = if (item.code.length >= 10) 48.sp else 56.sp

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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.4.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.isPickedUp) "已取件" else "未取件",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = statusColor,
                )
                Text(
                    text = formatTime(item.receivedAtMillis),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }

            Text(
                text = item.code,
                modifier = Modifier.fillMaxWidth(),
                style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
                    fontSize = codeFontSize,
                    lineHeight = codeFontSize,
                    fontWeight = FontWeight.Normal,
                ),
                color = Ink,
                textAlign = TextAlign.Center,
            )

            Text(
                text = item.body,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.matchedRule,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
                Text(
                    text = "双击打开短信",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Ink,
                )
            }
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
    val revealWidth = 126.dp
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
                .background(DrawerAction),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .background(AccentGreenContainer),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DrawerAction,
                        contentColor = CardSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RulesEditorSheet(
    initialRules: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val extractor = remember { PickupCodeExtractor() }
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
    val fieldErrors = extractor.draftValidationErrors(rules.toList())
    val canSave = rules.isNotEmpty() && fieldErrors.none { it != null }

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
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                color = Ink,
            )
            Text(
                text = "规则有语法错误时不能保存。恢复默认设置会立即覆盖当前规则并刷新列表。",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = InkMuted,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "常用模板",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = Ink,
                )
                templates.forEach { template ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (template !in rules) {
                                rules += template
                            }
                        },
                    ) {
                        Text(
                            text = template,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            rules.indices.forEach { index ->
                val fieldError = fieldErrors.getOrNull(index)
                OutlinedTextField(
                    value = rules[index],
                    onValueChange = { updated ->
                        rules[index] = updated
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    label = { Text(text = "规则 ${index + 1}") },
                    isError = fieldError != null,
                    supportingText = {
                        Text(
                            text = fieldError ?: "示例：取件码[：:\\s]*([A-Za-z0-9-]+)",
                            color = if (fieldError != null) {
                                androidx.compose.material3.MaterialTheme.colorScheme.error
                            } else {
                                InkMuted
                            },
                        )
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

            if (!canSave) {
                Text(
                    text = "存在空白规则或正则语法错误，修正后才能保存。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { rules += "" },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Rule,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "新增规则")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestoreDefaults,
                ) {
                    Text(text = "恢复默认设置")
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave,
                    onClick = { onSave(rules.toList()) },
                ) {
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
