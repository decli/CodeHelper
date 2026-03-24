package com.decli.codehelper.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MarkunreadMailbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.home.HomeUiState
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.theme.AlertRed
import com.decli.codehelper.ui.theme.AlertRedContainer
import com.decli.codehelper.ui.theme.BorderBlue
import com.decli.codehelper.ui.theme.CobaltBlue
import com.decli.codehelper.ui.theme.DeepGreen
import com.decli.codehelper.ui.theme.InkBlue
import com.decli.codehelper.ui.theme.MutedSurface
import com.decli.codehelper.ui.theme.MutedText
import com.decli.codehelper.ui.theme.SandBackground
import com.decli.codehelper.ui.theme.SoftSurface
import com.decli.codehelper.ui.theme.WarmGold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        containerColor = SandBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .fillMaxSize()
                .background(SandBackground)
                .padding(innerPadding)
                .statusBarsPadding(),
            columns = StaggeredGridCells.Adaptive(minSize = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 20.dp,
                bottom = 32.dp,
            ),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                HeroSection(
                    uiState = uiState,
                    onFilterSelected = viewModel::selectFilter,
                    onForceRefresh = viewModel::forceRefresh,
                    onEditRules = { showRuleEditor = true },
                )
            }

            if (!uiState.hasSmsPermission) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    PermissionCard(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        },
                    )
                }
            } else if (uiState.items.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    EmptyStateCard(
                        title = "这个时间段还没有取件码",
                        subtitle = "可以切换筛选时间，或者点击“强制刷新”重新扫描短信。",
                    )
                }
            } else {
                items(uiState.items, key = { it.uniqueKey }) { item ->
                    PickupCodeCard(
                        item = item,
                        onDelete = { viewModel.hideCode(item) },
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

            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(80.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroSection(
    uiState: HomeUiState,
    onFilterSelected: (CodeFilterWindow) -> Unit,
    onForceRefresh: () -> Unit,
    onEditRules: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFDF8F0), Color(0xFFE8F0FA)),
                ),
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "取件码助手",
                    style = MaterialTheme.typography.displayMedium,
                    color = InkBlue,
                )
                Text(
                    text = "打开就看取件码，大字、高对比、离线可用。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedText,
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.74f),
            ) {
                Icon(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp),
                    imageVector = Icons.Rounded.MarkunreadMailbox,
                    contentDescription = null,
                    tint = CobaltBlue,
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryChip(title = "当前数量", value = uiState.items.size.toString(), accent = CobaltBlue)
            SummaryChip(
                title = "已删除标记",
                value = uiState.items.count { it.isDeleted }.toString(),
                accent = AlertRed,
            )
            SummaryChip(
                title = "提取规则",
                value = uiState.activeRules.size.toString(),
                accent = DeepGreen,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "筛选时间",
                style = MaterialTheme.typography.titleLarge,
                color = InkBlue,
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
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
            FilledTonalButton(
                onClick = onForceRefresh,
                enabled = uiState.hasSmsPermission,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "强制刷新")
            }

            OutlinedButton(onClick = onEditRules) {
                Icon(
                    imageVector = Icons.Rounded.Rule,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "规则设置")
            }
        }

        Text(
            text = if (uiState.showDeletedOnRefresh) {
                "灰色卡片表示这条取件码之前已经左滑删除。双击卡片可打开对应短信。"
            } else {
                "左滑卡片即可隐藏；需要回看已删除内容时，点击“强制刷新”。双击卡片可打开对应短信。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )
    }
}

@Composable
private fun SummaryChip(
    title: String,
    value: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
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
                    .background(accent),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedText,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkBlue,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onRequestPermission: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = AlertRedContainer,
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(14.dp)
                            .size(30.dp),
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = AlertRed,
                    )
                }
                Column {
                    Text(
                        text = "请先授权读取短信",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkBlue,
                    )
                    Text(
                        text = "App 不联网，只会在本机读取短信内容并提取取件码。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedText,
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestPermission,
            ) {
                Text(text = "授权读取短信")
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFF3DF),
            ) {
                Icon(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(36.dp),
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = WarmGold,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = InkBlue,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PickupCodeCard(
    item: PickupCodeItem,
    onDelete: () -> Unit,
    onOpenSms: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.32f },
        confirmValueChange = { target ->
            if (!item.isDeleted && target == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !item.isDeleted,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .background(AlertRed)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
            }
        },
    ) {
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
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (item.isDeleted) MutedSurface else SoftSurface,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = BorderBlue, shape = RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = if (item.isDeleted) "已删除" else "待取件",
                        accent = if (item.isDeleted) MutedText else DeepGreen,
                    )

                    Text(
                        text = formatTime(item.receivedAtMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MutedText,
                    )
                }

                Text(
                    text = item.code,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    color = if (item.isDeleted) MutedText else InkBlue,
                )

                StatusPill(
                    text = "来源：${item.sender}",
                    accent = CobaltBlue,
                )

                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isDeleted) MutedText else InkBlue.copy(alpha = 0.78f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "规则：${item.matchedRule}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "双击打开短信",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.isDeleted) MutedText else CobaltBlue,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.74f),
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
                color = InkBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "提取规则设置",
                style = MaterialTheme.typography.headlineSmall,
                color = InkBlue,
            )
            Text(
                text = "每行填写一条完整正则。可以直接扩展成“取件码[a-zA-Z0-9-]+”或“凭[a-zA-Z0-9-]+”这样的格式。",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedText,
            )

            rules.forEachIndexed { index, rule ->
                OutlinedTextField(
                    value = rule,
                    onValueChange = { updated ->
                        rules[index] = updated
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    label = { Text(text = "规则 ${index + 1}") },
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
                    Text(text = "新增规则")
                }
                Button(onClick = { onSave(rules.toList()) }) {
                    Text(text = "保存规则")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
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
