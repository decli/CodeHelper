package com.decli.codehelper.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.decli.codehelper.model.CodeScale
import com.decli.codehelper.model.DisplaySettings
import com.decli.codehelper.model.ThemeMode
import com.decli.codehelper.ui.components.ActionButton
import com.decli.codehelper.ui.components.AppCard
import com.decli.codehelper.ui.components.SegmentedControl
import com.decli.codehelper.ui.components.motionDuration
import com.decli.codehelper.util.PickupCodeExtractor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * 全屏设置页：即改即存。
 * 除高级规则输入框（停手 700ms 后落盘）外，所有开关 / 分段 / 芯片都立刻写入 DataStore。
 */
@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen(
    promptKeywords: List<String>,
    advancedRules: List<String>,
    badgeRefreshMinutes: Int,
    displaySettings: DisplaySettings,
    notificationPermissionGranted: Boolean,
    appVersionName: String,
    onBack: () -> Unit,
    onPromptKeywordsChange: (List<String>) -> Unit,
    onAdvancedRulesChange: (List<String>) -> Unit,
    onBadgeRefreshMinutesChange: (Int) -> Unit,
    onCodeScaleChange: (CodeScale) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGroupBySenderChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val extractor = remember { PickupCodeExtractor() }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showAddKeywordDialog by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    // 本地草稿：高级规则边输入边校验，停手后自动落盘
    val ruleDrafts = remember { mutableStateListOf<String>().apply { addAll(advancedRules) } }
    LaunchedEffect(ruleDrafts) {
        snapshotFlow { ruleDrafts.toList() }
            .drop(1)
            .debounce(700)
            .distinctUntilChanged()
            .collect { drafts ->
                onAdvancedRulesChange(drafts.filter { it.isNotBlank() })
            }
    }

    val keywordChips = remember(promptKeywords) {
        (PickupCodeExtractor.defaultPromptKeywords + promptKeywords).distinct()
    }

    if (showAboutDialog) {
        AboutDialog(
            appVersionName = appVersionName,
            onDismiss = { showAboutDialog = false },
        )
    }

    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onConfirm = { keyword ->
                showAddKeywordDialog = false
                val cleaned = keyword.trim()
                when {
                    cleaned.isEmpty() -> onMessage("提示词不能为空")
                    cleaned in promptKeywords -> onMessage("「$cleaned」已经在用了")
                    else -> {
                        onPromptKeywordsChange(promptKeywords + cleaned)
                        onMessage("已添加提示词「$cleaned」")
                    }
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            SettingsTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── 显示 ──
                SettingsGroup(title = "显示") {
                    SettingsRow(title = "取件码字号") {
                        SegmentedControl(
                            options = listOf(CodeScale.Standard, CodeScale.Large),
                            selected = displaySettings.codeScale,
                            labelOf = { it.label },
                            onSelect = onCodeScaleChange,
                            modifier = Modifier.width(190.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            containerShape = RoundedCornerShape(14.dp),
                            containerPadding = 4.dp,
                            segmentGap = 4.dp,
                            segmentHeight = 44.dp,
                            segmentShape = RoundedCornerShape(11.dp),
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            checkSize = 18.dp,
                            elevated = false,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(title = "外观") {
                        SegmentedControl(
                            options = listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark),
                            selected = displaySettings.themeMode,
                            labelOf = { if (it == ThemeMode.System) "跟随" else it.label },
                            onSelect = onThemeModeChange,
                            modifier = Modifier.width(230.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            containerShape = RoundedCornerShape(14.dp),
                            containerPadding = 4.dp,
                            segmentGap = 4.dp,
                            segmentHeight = 44.dp,
                            segmentShape = RoundedCornerShape(11.dp),
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            checkSize = 16.dp,
                            elevated = false,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "按驿站分组",
                        subtitle = "同一家店的包裹放在一起",
                        checked = displaySettings.groupBySender,
                        onCheckedChange = onGroupBySenderChange,
                    )
                }

                // ── 识别提示词 ──
                SettingsGroup(title = "识别提示词") {
                    Text(
                        text = "短信里出现这些词，后面的数字就会被当作取件码。点一下可以关闭或开启。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    KeywordChips(
                        chips = keywordChips,
                        enabledKeywords = promptKeywords,
                        onToggle = { keyword ->
                            if (keyword in promptKeywords) {
                                if (promptKeywords.size <= 1) {
                                    onMessage("至少要留一个识别提示词")
                                } else {
                                    onPromptKeywordsChange(promptKeywords - keyword)
                                }
                            } else {
                                onPromptKeywordsChange(promptKeywords + keyword)
                            }
                        },
                        onAdd = { showAddKeywordDialog = true },
                    )
                }

                // ── 桌面角标 ──
                SettingsGroup(title = "桌面角标") {
                    Text(
                        text = "桌面图标上显示待取数量，每隔一段时间自动更新。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(5, 10, 15, 30).forEach { minutes ->
                            MinuteChip(
                                minutes = minutes,
                                selected = badgeRefreshMinutes == minutes,
                                modifier = Modifier.weight(1f),
                                onClick = { onBadgeRefreshMinutesChange(minutes) },
                            )
                        }
                    }
                    if (notificationPermissionGranted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                text = "通知已开启，角标正常显示",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        ActionButton(
                            text = "开启通知，角标才能显示",
                            icon = Icons.Rounded.NotificationsActive,
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            height = 56.dp,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            iconSize = 20.dp,
                        )
                    }
                }

                // ── 高级 ──
                SettingsGroup(title = "高级", contentSpacing = 0.dp) {
                    AdvancedHeaderRow(
                        expanded = advancedExpanded,
                        onToggle = { advancedExpanded = !advancedExpanded },
                    )
                    AnimatedVisibility(visible = advancedExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "语法错误时会用红色提示，且这一条不会生效，其它设置照常可用。",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ruleDrafts.indices.forEach { index ->
                                val error = extractor.validationError(ruleDrafts[index])
                                    .takeIf { ruleDrafts[index].isNotBlank() }
                                RuleField(
                                    value = ruleDrafts[index],
                                    index = index,
                                    error = error,
                                    onValueChange = { updated -> ruleDrafts[index] = updated },
                                    onRemove = { ruleDrafts.removeAt(index) },
                                )
                            }
                            ActionButton(
                                text = "＋ 新增规则",
                                onClick = { ruleDrafts.add("") },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                height = 48.dp,
                                shape = RoundedCornerShape(14.dp),
                                textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                            )
                            BadgeMinutesField(
                                badgeRefreshMinutes = badgeRefreshMinutes,
                                onChange = onBadgeRefreshMinutesChange,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        title = "恢复默认设置",
                        onClick = {
                            ruleDrafts.clear()
                            ruleDrafts.addAll(PickupCodeExtractor.defaultAdvancedRules)
                            onRestoreDefaults()
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        title = "关于取件码助手",
                        value = appVersionName,
                        onClick = { showAboutDialog = true },
                    )
                }

                Text(
                    text = "改动会自动保存，不需要再点「保存」",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────── 顶栏 ───────────────────────────

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = "设置",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ─────────────────────────── 分组容器 ───────────────────────────

@Composable
private fun SettingsGroup(
    title: String,
    contentSpacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    AppCard(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        control()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

// ─────────────────────────── 提示词芯片 ───────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordChips(
    chips: List<String>,
    enabledKeywords: List<String>,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { keyword ->
            val enabled = keyword in enabledKeywords
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                    )
                    .then(
                        if (enabled) {
                            Modifier
                        } else {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        },
                    )
                    .clickable(role = Role.Checkbox) { onToggle(keyword) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (enabled) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(role = Role.Button, onClick = onAdd)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "新增",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MinuteChip(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
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
                enabled = !selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (selected) "$minutes 分钟，已选中" else "$minutes 分钟"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$minutes 分钟",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

// ─────────────────────────── 高级区 ───────────────────────────

@Composable
private fun AdvancedHeaderRow(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = motionDuration(200)),
        label = "advancedArrow",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "高级规则",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "给懂正则的家人用，一般不需要",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起高级规则" else "展开高级规则",
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RuleField(
    value: String,
    index: Int,
    error: String?,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "高级规则 ${index + 1}",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(14.dp),
            colors = settingsFieldColors(),
            singleLine = true,
            isError = error != null,
            placeholder = {
                Text(
                    text = "货码[：:\\s]*([A-Za-z0-9-]+)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            },
            trailingIcon = {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "删除高级规则 ${index + 1}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun BadgeMinutesField(
    badgeRefreshMinutes: Int,
    onChange: (Int) -> Unit,
) {
    var text by rememberSaveable(badgeRefreshMinutes) { mutableStateOf(badgeRefreshMinutes.toString()) }
    val parsed = text.toIntOrNull()
    val hasError = parsed == null || parsed !in 5..120
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "角标刷新间隔（5 到 120 分钟）",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            value = text,
            onValueChange = { updated ->
                text = updated.filter { it.isDigit() }.take(3)
                text.toIntOrNull()?.takeIf { it in 5..120 && it != badgeRefreshMinutes }?.let(onChange)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(14.dp),
            colors = settingsFieldColors(),
            singleLine = true,
            isError = hasError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (hasError) {
            Text(
                text = "请输入 5 到 120 之间的分钟数",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun settingsFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = MaterialTheme.colorScheme.error,
    )

// ─────────────────────────── 导航行 ───────────────────────────

@Composable
private fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─────────────────────────── 弹窗 ───────────────────────────

@Composable
private fun AboutDialog(
    appVersionName: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = "取件码助手",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "版本 $appVersionName\n不联网 · 不上传 · 只读取本机短信",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                ActionButton(
                    text = "知道了",
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    height = 56.dp,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                )
            }
        }
    }
}

@Composable
private fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "新增识别提示词",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "短信里出现这个词，后面的数字就会被当作取件码。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = text,
                    onValueChange = { text = it.take(12) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
                    shape = RoundedCornerShape(14.dp),
                    colors = settingsFieldColors(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "例如：快递码",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        height = 56.dp,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.titleMedium,
                    )
                    ActionButton(
                        text = "确定",
                        onClick = { onConfirm(text) },
                        modifier = Modifier.weight(1f),
                        height = 56.dp,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
