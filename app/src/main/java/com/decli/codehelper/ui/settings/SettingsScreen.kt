package com.decli.codehelper.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decli.codehelper.util.PickupCodeExtractor

/**
 * 全屏设置页：识别提示词、高级规则、桌面角标、恢复默认与关于。
 * 校验逻辑与保存行为与旧版设置弹层完全一致。
 */
@Composable
fun SettingsScreen(
    initialPromptKeywords: List<String>,
    initialAdvancedRules: List<String>,
    initialBadgeRefreshMinutes: Int,
    notificationPermissionGranted: Boolean,
    appVersionName: String,
    onBack: () -> Unit,
    onSave: (List<String>, List<String>, Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRestoreDefaults: () -> Unit,
) {
    val extractor = remember { PickupCodeExtractor() }
    val promptKeywords = remember(initialPromptKeywords) {
        mutableStateListOf<String>().apply {
            addAll(initialPromptKeywords)
        }
    }
    val advancedRules = remember(initialAdvancedRules) {
        mutableStateListOf<String>().apply {
            addAll(initialAdvancedRules)
        }
    }
    var badgeRefreshMinutesText by rememberSaveable(initialBadgeRefreshMinutes) {
        mutableStateOf(initialBadgeRefreshMinutes.toString())
    }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    val keywordTemplates = remember { PickupCodeExtractor.defaultPromptKeywords }
    val promptKeywordErrors = extractor.draftKeywordErrors(promptKeywords.toList())
    val ruleErrors = extractor.draftValidationErrors(advancedRules.toList())
    val badgeRefreshMinutes = badgeRefreshMinutesText.toIntOrNull()
    val hasBadgeRefreshError = badgeRefreshMinutes == null || badgeRefreshMinutes !in 5..120
    val canSave =
        promptKeywords.isNotEmpty() &&
            promptKeywordErrors.none { it != null } &&
            ruleErrors.none { it != null } &&
            !hasBadgeRefreshError

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "关于取件码助手") },
            text = { Text(text = "当前版本：$appVersionName") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = "知道了")
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
                .imePadding(),
        ) {
            SettingsTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SettingsGroup {
                    Text(
                        text = "识别提示词",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "应用会在这些词后面自动寻找取件码，比如「取件码」「货码」「凭」。点选下方常用词即可添加。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    KeywordTemplateChips(
                        templates = keywordTemplates,
                        addedKeywords = promptKeywords,
                        onAdd = { template ->
                            if (template !in promptKeywords) {
                                promptKeywords += template
                            }
                        },
                    )
                    promptKeywords.indices.forEach { index ->
                        val fieldError = promptKeywordErrors.getOrNull(index)
                        OutlinedTextField(
                            value = promptKeywords[index],
                            onValueChange = { updated ->
                                promptKeywords[index] = updated
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text(text = "提示词 ${index + 1}") },
                            isError = fieldError != null,
                            supportingText = {
                                Text(
                                    text = fieldError ?: "例如：取件码、货码、凭",
                                    color = if (fieldError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            trailingIcon = {
                                if (promptKeywords.size > 1) {
                                    IconButton(onClick = { promptKeywords.removeAt(index) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "删除提示词 ${index + 1}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                        )
                    }
                    DashedAddButton(
                        text = "新增提示词",
                        icon = Icons.Rounded.Add,
                        onClick = { promptKeywords += "" },
                    )
                }

                SettingsGroup {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "高级规则",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "选填 · 特殊平台才需要",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "只有遇到个别平台的特殊写法时，再补充正则规则。规则有语法错误时不能保存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    advancedRules.indices.forEach { index ->
                        val fieldError = ruleErrors.getOrNull(index)
                        OutlinedTextField(
                            value = advancedRules[index],
                            onValueChange = { updated ->
                                advancedRules[index] = updated
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text(text = "高级规则 ${index + 1}") },
                            isError = fieldError != null,
                            supportingText = {
                                Text(
                                    text = fieldError ?: "示例：货码[：:\\s]*([A-Za-z0-9-]+)",
                                    color = if (fieldError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { advancedRules.removeAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "删除高级规则 ${index + 1}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    }
                    DashedAddButton(
                        text = "新增高级规则",
                        icon = Icons.Rounded.Rule,
                        onClick = { advancedRules += "" },
                    )
                }

                SettingsGroup {
                    Text(
                        text = "桌面角标",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "桌面图标会显示待取数量，按以下频率自动刷新。数字角标依赖系统桌面与通知权限支持。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(5, 10, 15, 30).forEach { minutes ->
                            SelectableChip(
                                text = "${minutes}分钟",
                                selected = badgeRefreshMinutesText == minutes.toString(),
                                onClick = { badgeRefreshMinutesText = minutes.toString() },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = badgeRefreshMinutesText,
                        onValueChange = { updated ->
                            badgeRefreshMinutesText = updated.filter { it.isDigit() }.take(3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        label = { Text(text = "角标刷新频率（分钟）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hasBadgeRefreshError,
                        supportingText = {
                            Text(
                                text = if (hasBadgeRefreshError) {
                                    "请输入 5 到 120 之间的分钟数"
                                } else {
                                    "默认 5 分钟，也可以直接点上方常用频率"
                                },
                                color = if (hasBadgeRefreshError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        },
                    )
                    if (!notificationPermissionGranted) {
                        Surface(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.6.dp, MaterialTheme.colorScheme.primary),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "开启通知权限，角标才能显示",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }

                SettingsGroup(contentSpacing = 0.dp) {
                    SettingsNavRow(
                        title = "恢复默认设置",
                        onClick = onRestoreDefaults,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavRow(
                        title = "关于取件码助手",
                        value = appVersionName,
                        onClick = { showAboutDialog = true },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!canSave) {
                    Text(
                        text = "存在空白提示词、高级规则语法错误或角标频率无效，修正后才能保存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        onSave(
                            promptKeywords.toList(),
                            advancedRules.toList(),
                            badgeRefreshMinutes ?: 5,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = canSave,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存并刷新",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 19.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Text(
            text = "设置",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SettingsGroup(
    contentSpacing: Dp = 10.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordTemplateChips(
    templates: List<String>,
    addedKeywords: List<String>,
    onAdd: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        templates.forEach { template ->
            SelectableChip(
                text = template,
                selected = template in addedKeywords,
                onClick = { onAdd(template) },
            )
        }
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun DashedAddButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val dashColor = MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
            },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
