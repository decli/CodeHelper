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
import com.decli.codehelper.model.ExtractorSettings
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.util.BadgeNotifier
import com.decli.codehelper.util.PickupCodeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
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

    val uiState: StateFlow<HomeUiState> = uiStateFlow.asStateFlow()
    val messages = messageFlow.asSharedFlow()

    init {
        observeSelectedFilterSetting()
        observeData()
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
            messageFlow.tryEmit("请先授权短信读取权限")
            return
        }
        showAllItems.value = true
        reloadNonce.update { it + 1 }
        messageFlow.tryEmit("已重新加载当前时间范围内的全部取件码")
    }

    fun markPickedUp(item: PickupCodeItem) {
        if (item.isPickedUp) return

        viewModelScope.launch {
            showAllItems.value = false
            settingsRepository.markPickedUp(item.uniqueKey)
            messageFlow.emit("已将 ${item.codes.joinToString("、")} 标记为已取件")
        }
    }

    fun restorePending(item: PickupCodeItem) {
        if (!item.isPickedUp) return

        viewModelScope.launch {
            settingsRepository.markPending(item.uniqueKey)
            messageFlow.emit("已将 ${item.codes.joinToString("、")} 恢复为未取件")
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
            showAllItems.value = false
            reloadNonce.update { it + 1 }
            messageFlow.emit("提取规则已保存")
        }
        return true
    }

    fun resetRulesToDefault() {
        saveExtractorSettings(
            candidatePromptKeywords = PickupCodeExtractor.defaultPromptKeywords,
            candidateAdvancedRules = PickupCodeExtractor.defaultAdvancedRules,
        )
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
            messageFlow.emit("角标刷新频率已保存")
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
