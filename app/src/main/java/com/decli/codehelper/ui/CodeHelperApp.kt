package com.decli.codehelper.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.ui.home.HomeScreen
import com.decli.codehelper.ui.home.HomeViewModel
import com.decli.codehelper.ui.home.TimeFilterSheet
import com.decli.codehelper.ui.onboarding.OnboardingScreen
import com.decli.codehelper.ui.onboarding.OnboardingStep
import com.decli.codehelper.ui.settings.SettingsScreen
import com.decli.codehelper.ui.show.ShowCodeScreen
import com.decli.codehelper.util.BadgeNotifier
import com.decli.codehelper.util.CodeSpeaker
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 提示条时长：带撤销 6s，纯提示 2.5s */
private const val SNACKBAR_ACTION_MILLIS = 6_000L
private const val SNACKBAR_PLAIN_MILLIS = 2_500L

@Composable
fun CodeHelperApp(
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displaySettings by viewModel.displaySettings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showTimeSheet by rememberSaveable { mutableStateOf(false) }
    var showCodeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var onboardingStep by rememberSaveable { mutableStateOf(OnboardingStep.Intro) }
    var speakingKey by remember { mutableStateOf<String?>(null) }
    var notificationPermissionGranted by remember {
        mutableStateOf(BadgeNotifier.hasNotificationPermission(context))
    }
    val appVersionName = remember(context) { context.appVersionName() }
    val speaker = rememberCodeSpeaker()

    val showCodeItem = remember(showCodeKey, uiState.items) {
        uiState.items.firstOrNull { it.uniqueKey == showCodeKey }
    }
    LaunchedEffect(showCodeKey, showCodeItem, uiState.isLoading) {
        if (showCodeKey != null && showCodeItem == null && !uiState.isLoading) {
            showCodeKey = null
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            withTimeoutOrNull(SNACKBAR_PLAIN_MILLIS) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }
    }

    fun stopSpeaking() {
        if (speakingKey != null) {
            speaker.stop()
            speakingKey = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.updatePermissionStatus(granted)
        if (!displaySettings.onboardingDone) {
            onboardingStep = OnboardingStep.Notification
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        notificationPermissionGranted = BadgeNotifier.hasNotificationPermission(context)
        if (notificationPermissionGranted) {
            BadgeNotifier.updateBadge(context, uiState.pendingCount)
            showMessage("通知已开启")
        }
        if (!displaySettings.onboardingDone) {
            viewModel.completeOnboarding()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissionStatus()
                    notificationPermissionGranted = BadgeNotifier.hasNotificationPermission(context)
                }

                Lifecycle.Event.ON_PAUSE -> {
                    speaker.stop()
                    speakingKey = null
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            withTimeoutOrNull(SNACKBAR_PLAIN_MILLIS) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite,
                )
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.pickedUpEvents.collect { item ->
            val result = withTimeoutOrNull(SNACKBAR_ACTION_MILLIS) {
                snackbarHostState.showSnackbar(
                    message = "「${item.codes.joinToString("、")}」已标记为取到",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePending(item)
            }
        }
    }

    val onMarkPickedUp: (PickupCodeItem) -> Unit = { item ->
        stopSpeaking()
        showCodeKey = null
        viewModel.markPickedUp(item)
    }
    val onCopyCode: (PickupCodeItem) -> Unit = { item ->
        val codes = item.codes.joinToString("、")
        context.copyToClipboard(codes)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        showMessage("已复制 $codes")
    }
    val onSpeakCode: (PickupCodeItem) -> Unit = { item ->
        when {
            speakingKey == item.uniqueKey -> stopSpeaking()
            !speaker.isAvailable -> showMessage("本机没有语音引擎，无法朗读")
            else -> {
                speakingKey = item.uniqueKey
                speaker.speak(item) { speakingKey = null }
            }
        }
    }
    val onOpenSms: (PickupCodeItem) -> Unit = { item ->
        scope.launch {
            openSmsOrConversation(
                context = context,
                item = item,
                onMessage = { message -> showMessage(message) },
            )
        }
    }

    if (showTimeSheet) {
        LaunchedEffect(Unit) { viewModel.refreshPendingCountPreview() }
        TimeFilterSheet(
            selectedFilter = uiState.selectedFilter,
            pendingCountByWindow = uiState.pendingCountByWindow,
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
            ) { data ->
                CodeSnackbar(data = data)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                displaySettings.isLoaded && !displaySettings.onboardingDone -> {
                    OnboardingScreen(
                        step = onboardingStep,
                        onPrimaryAction = {
                            when (onboardingStep) {
                                OnboardingStep.Intro -> onboardingStep = OnboardingStep.SmsPermission
                                OnboardingStep.SmsPermission ->
                                    permissionLauncher.launch(Manifest.permission.READ_SMS)

                                OnboardingStep.Notification -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        )
                                    } else {
                                        notificationPermissionGranted = true
                                        viewModel.completeOnboarding()
                                    }
                                }
                            }
                        },
                        onSkip = {
                            when (onboardingStep) {
                                OnboardingStep.Intro -> Unit
                                OnboardingStep.SmsPermission -> onboardingStep = OnboardingStep.Notification
                                OnboardingStep.Notification -> viewModel.completeOnboarding()
                            }
                        },
                    )
                }

                showCodeItem != null -> {
                    ShowCodeScreen(
                        item = showCodeItem,
                        isSpeaking = speakingKey == showCodeItem.uniqueKey,
                        speechAvailable = speaker.isAvailable,
                        onClose = {
                            stopSpeaking()
                            showCodeKey = null
                        },
                        onCopyCode = { onCopyCode(showCodeItem) },
                        onSpeakCode = { onSpeakCode(showCodeItem) },
                        onMarkPickedUp = { onMarkPickedUp(showCodeItem) },
                    )
                }

                showSettings -> {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        promptKeywords = uiState.activePromptKeywords,
                        advancedRules = uiState.activeAdvancedRules,
                        badgeRefreshMinutes = uiState.badgeRefreshMinutes,
                        displaySettings = displaySettings,
                        notificationPermissionGranted = notificationPermissionGranted,
                        appVersionName = appVersionName,
                        onBack = { showSettings = false },
                        onPromptKeywordsChange = { keywords ->
                            viewModel.saveExtractorSettings(
                                candidatePromptKeywords = keywords,
                                candidateAdvancedRules = uiState.activeAdvancedRules,
                            )
                        },
                        onAdvancedRulesChange = { rules ->
                            viewModel.saveExtractorSettings(
                                candidatePromptKeywords = uiState.activePromptKeywords,
                                candidateAdvancedRules = rules,
                            )
                        },
                        onBadgeRefreshMinutesChange = viewModel::saveBadgeRefreshMinutes,
                        onCodeScaleChange = viewModel::setCodeScale,
                        onThemeModeChange = viewModel::setThemeMode,
                        onGroupBySenderChange = viewModel::setGroupBySender,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationPermissionGranted = true
                            }
                        },
                        onRestoreDefaults = viewModel::restoreDefaults,
                        onMessage = { message -> showMessage(message) },
                    )
                }

                else -> {
                    HomeScreen(
                        uiState = uiState,
                        groupBySender = displaySettings.groupBySender,
                        speakingKey = speakingKey,
                        speechAvailable = speaker.isAvailable,
                        onOpenTimeSheet = { showTimeSheet = true },
                        onSelectPending = viewModel::showPendingOnly,
                        onSelectAll = viewModel::forceRefreshAll,
                        onOpenSettings = { showSettings = true },
                        onGrantPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        },
                        onMarkPickedUp = onMarkPickedUp,
                        onRestorePending = viewModel::restorePending,
                        onOpenSms = onOpenSms,
                        onCopyCode = onCopyCode,
                        onSpeakCode = onSpeakCode,
                        onShowCode = { item ->
                            stopSpeaking()
                            showCodeKey = item.uniqueKey
                        },
                    )
                }
            }
        }
    }
}

/** 提示条：17sp、圆角 18、撤销是 48dp 实心按钮而不是文字 */
@Composable
private fun CodeSnackbar(
    data: SnackbarData,
) {
    val actionLabel = data.visuals.actionLabel
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.visuals.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (actionLabel != null) {
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            role = Role.Button,
                            onClick = { data.performAction() },
                        )
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCodeSpeaker(): CodeSpeaker {
    val context = LocalContext.current
    val speaker = remember(context) { CodeSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.release() }
    }
    return speaker
}

private fun Context.copyToClipboard(text: String) {
    runCatching {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("取件码", text))
    }
}

private fun openSmsOrConversation(
    context: Context,
    item: PickupCodeItem,
    onMessage: (String) -> Unit,
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
