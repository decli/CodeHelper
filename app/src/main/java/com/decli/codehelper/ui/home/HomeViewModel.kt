package com.decli.codehelper.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.decli.codehelper.data.SettingsRepository
import com.decli.codehelper.data.SmsRepository
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.CodeScale
import com.decli.codehelper.model.DisplaySettings
import com.decli.codehelper.model.ExtractorSettings
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.model.ThemeMode
import com.decli.codehelper.util.BadgeNotifier
import com.decli.codehelper.util.PickupCodeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val extractor = PickupCodeExtractor()
    private val settingsRepository = SettingsRepository(application)
    private val smsRepository = SmsRepository(application.contentResolver, extractor)

    private val selectedFilter = MutableStateFlow(CodeFilterWindow.Last12Hours)
    private val showAllItems = MutableStateFlow(false)
    private val permissionGranted = MutableStateFlow(hasSmsPermission())
    private val reloadNonce = MutableStateFlow(0)
    private val uiStateFlow = MutableStateFlow(
        HomeUiState(hasSmsPermission = permissionGranted.value),
    )

    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private val pickedUpEventFlow = MutableSharedFlow<PickupCodeItem>(extraBufferCapacity = 8)

    val uiState: StateFlow<HomeUiState> = uiStateFlow.asStateFlow()
    val messages = messageFlow.asSharedFlow()

    /** 标记已取到后的可撤销事件，item 为已置为已取件状态的副本 */
    val pickedUpEvents = pickedUpEventFlow.asSharedFlow()

    /** 展示类设置（字号 / 外观 / 分组 / 引导），改动不触发短信重新读取 */
    val displaySettings: StateFlow<DisplaySettings> = settingsRepository.displaySettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DisplaySettings(),
        )

    init {
        observeSelectedFilterSetting()
        observeData()
        skipOnboardingForExistingUsers()
    }

    /** 老用户升级上来时已经授过权，不再让他们重看一遍首次引导 */
    private fun skipOnboardingForExistingUsers() {
        viewModelScope.launch {
            val settings = settingsRepository.displaySettingsFlow.first()
            if (!settings.onboardingDone && hasSmsPermission()) {
                settingsRepository.saveOnboardingDone(true)
            }
        }
    }

    fun refreshPermissionStatus() {
        val granted = hasSmsPermission()
        permissionGranted.value = granted
        if (granted) {
            reloadNonce.update { it + 1 }
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) {
            showAllItems.value = false
            reloadNonce.update { it + 1 }
        }
    }

    fun selectFilter(filterWindow: CodeFilterWindow) {
        showAllItems.value = false
        selectedFilter.value = filterWindow
        viewModelScope.launch {
            settingsRepository.saveSelectedFilter(filterWindow)
        }
    }

    fun forceRefreshAll() {
        if (!permissionGranted.value) {
            messageFlow.tryEmit("请先允许读取短信")
            return
        }
        if (showAllItems.value) return
        showAllItems.value = true
        reloadNonce.update { it + 1 }
    }

    /** 回到默认的「待取」视图 */
    fun showPendingOnly() {
        if (!showAllItems.value) return
        showAllItems.value = false
        reloadNonce.update { it + 1 }
    }

    /** 下拉刷新 / 手动重新读取 */
    fun reload() {
        reloadNonce.update { it + 1 }
    }

    fun markPickedUp(item: PickupCodeItem) {
        if (item.isPickedUp) return

        viewModelScope.launch {
            // 停留在当前标签：在「全部包裹」里标记时，卡片就地降为「已取件」摘要行
            settingsRepository.markPickedUp(item.uniqueKey)
            pickedUpEventFlow.emit(item.copy(isPickedUp = true))
        }
    }

    fun restorePending(item: PickupCodeItem) {
        if (!item.isPickedUp) return

        viewModelScope.launch {
            settingsRepository.markPending(item.uniqueKey)
            messageFlow.emit("「${item.codes.joinToString("、")}」已恢复为待取")
        }
    }

    fun saveExtractorSettings(
        candidatePromptKeywords: List<String>,
        candidateAdvancedRules: List<String>,
    ): Boolean {
        if (candidatePromptKeywords.any { it.trim().isEmpty() }) {
            messageFlow.tryEmit("存在空白提示词，请修改后再保存")
            return false
        }

        val sanitizedPromptKeywords = extractor.sanitizePromptKeywords(candidatePromptKeywords)
        if (sanitizedPromptKeywords.isEmpty()) {
            messageFlow.tryEmit("至少保留 1 个识别提示词")
            return false
        }

        val sanitizedAdvancedRules = extractor.sanitizeRules(candidateAdvancedRules)
        val invalidRule = extractor.firstInvalidRule(sanitizedAdvancedRules)
        if (invalidRule != null) {
            messageFlow.tryEmit("存在语法错误规则，请检查后再保存")
            return false
        }

        viewModelScope.launch {
            settingsRepository.saveExtractorSettings(
                promptKeywords = sanitizedPromptKeywords,
                advancedRules = sanitizedAdvancedRules,
            )
            reloadNonce.update { it + 1 }
        }
        return true
    }

    /** 恢复默认：识别规则、角标频率、显示设置一起回到出厂值 */
    fun restoreDefaults() {
        viewModelScope.launch {
            settingsRepository.restoreDefaults()
            reloadNonce.update { it + 1 }
            BadgeNotifier.scheduleNextBadgeRefresh(
                context = getApplication(),
                refreshMinutes = DEFAULT_BADGE_REFRESH_MINUTES,
            )
            messageFlow.emit("已恢复默认设置")
        }
    }

    fun saveBadgeRefreshMinutes(minutes: Int) {
        val sanitizedMinutes = minutes.coerceIn(5, 120)
        viewModelScope.launch {
            settingsRepository.saveBadgeRefreshMinutes(sanitizedMinutes)
            BadgeNotifier.scheduleNextBadgeRefresh(
                context = getApplication(),
                refreshMinutes = sanitizedMinutes,
            )
            BadgeNotifier.refreshBadgeFromSms(getApplication())
        }
    }

    fun setCodeScale(codeScale: CodeScale) {
        viewModelScope.launch { settingsRepository.saveCodeScale(codeScale) }
    }

    fun setGroupBySender(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.saveGroupBySender(enabled) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.saveThemeMode(themeMode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.saveOnboardingDone(true) }
    }

    /**
     * 统计时间面板里每一档的待取件数。
     * 只在打开面板时按最近 14 天扫一次，避免每次读取短信都多跑一遍。
     */
    fun refreshPendingCountPreview() {
        if (!permissionGranted.value) {
            uiStateFlow.update { it.copy(pendingCountByWindow = null) }
            return
        }
        viewModelScope.launch {
            val snapshot = uiStateFlow.value
            val pickedUpKeys = settingsRepository.pickedUpItemsFlow.first()
            val pendingItems = withContext(Dispatchers.IO) {
                smsRepository.loadPickupCodes(
                    filterWindow = CodeFilterWindow.Last14Days,
                    promptKeywords = snapshot.activePromptKeywords,
                    advancedRules = snapshot.activeAdvancedRules,
                    pickedUpKeys = pickedUpKeys,
                    includePickedUp = false,
                )
            }
            val counts = pendingCountByWindow(
                pendingItems = pendingItems,
                nowMillis = System.currentTimeMillis(),
            )
            uiStateFlow.update { it.copy(pendingCountByWindow = counts) }
        }
    }

    private fun observeSelectedFilterSetting() {
        viewModelScope.launch {
            settingsRepository.selectedFilterFlow.collect { savedFilter ->
                selectedFilter.value = savedFilter
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                settingsRepository.extractorSettingsFlow,
                settingsRepository.pickedUpItemsFlow,
                settingsRepository.badgeRefreshMinutesFlow,
                selectedFilter,
                showAllItems,
            ) { extractorSettings, pickedUpItems, badgeRefreshMinutes, filterWindow, showAll ->
                HomeLoadRequest(
                    extractorSettings = extractorSettings,
                    pickedUpItems = pickedUpItems,
                    badgeRefreshMinutes = badgeRefreshMinutes,
                    filterWindow = filterWindow,
                    showAll = showAll,
                    hasPermission = false,
                )
            }.combine(permissionGranted) { request, hasPermission ->
                request.copy(hasPermission = hasPermission)
            }.combine(reloadNonce) { request, _ ->
                request
            }.collectLatest { request ->
                uiStateFlow.update {
                    it.copy(
                        hasSmsPermission = request.hasPermission,
                        isLoading = request.hasPermission,
                        selectedFilter = request.filterWindow,
                        activePromptKeywords = request.extractorSettings.promptKeywords,
                        activeAdvancedRules = request.extractorSettings.advancedRules,
                        showAllItems = request.showAll,
                        badgeRefreshMinutes = request.badgeRefreshMinutes,
                    )
                }

                if (!request.hasPermission) {
                    BadgeNotifier.clearBadge(getApplication())
                    uiStateFlow.update {
                        it.copy(
                            items = emptyList(),
                            isLoading = false,
                            lastLoadedAtMillis = null,
                        )
                    }
                    return@collectLatest
                }

                val items = withContext(Dispatchers.IO) {
                    smsRepository.loadPickupCodes(
                        filterWindow = request.filterWindow,
                        promptKeywords = request.extractorSettings.promptKeywords,
                        advancedRules = request.extractorSettings.advancedRules,
                        pickedUpKeys = request.pickedUpItems,
                        includePickedUp = request.showAll,
                    )
                }

                uiStateFlow.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        lastLoadedAtMillis = System.currentTimeMillis(),
                    )
                }

                BadgeNotifier.updateBadge(
                    context = getApplication(),
                    pendingCount = items.sumOf { item -> if (item.isPickedUp) 0 else item.codeCount },
                )
                BadgeNotifier.scheduleNextBadgeRefresh(
                    context = getApplication(),
                    refreshMinutes = request.badgeRefreshMinutes,
                )
            }
        }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED

    private data class HomeLoadRequest(
        val extractorSettings: ExtractorSettings,
        val pickedUpItems: Set<String>,
        val badgeRefreshMinutes: Int,
        val filterWindow: CodeFilterWindow,
        val showAll: Boolean,
        val hasPermission: Boolean,
    )
}

private const val DEFAULT_BADGE_REFRESH_MINUTES = 5
