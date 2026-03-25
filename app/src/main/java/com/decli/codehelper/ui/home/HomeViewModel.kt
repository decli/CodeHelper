package com.decli.codehelper.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.decli.codehelper.data.SettingsRepository
import com.decli.codehelper.data.SmsRepository
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
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
    private val defaultSmsApp = MutableStateFlow(isDefaultSmsApp())
    private val reloadNonce = MutableStateFlow(0)
    private val uiStateFlow = MutableStateFlow(
        HomeUiState(
            hasSmsPermission = permissionGranted.value,
            isDefaultSmsApp = defaultSmsApp.value,
        ),
    )

    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val uiState: StateFlow<HomeUiState> = uiStateFlow.asStateFlow()
    val messages = messageFlow.asSharedFlow()

    init {
        observeData()
    }

    fun refreshPermissionStatus() {
        val granted = hasSmsPermission()
        val isDefault = isDefaultSmsApp()
        permissionGranted.value = granted
        defaultSmsApp.value = isDefault
        if (granted) {
            reloadNonce.update { it + 1 }
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        permissionGranted.value = granted
        defaultSmsApp.value = isDefaultSmsApp()
        if (granted) {
            showAllItems.value = false
            reloadNonce.update { it + 1 }
        }
    }

    fun refreshDefaultSmsStatus() {
        defaultSmsApp.value = isDefaultSmsApp()
        if (permissionGranted.value) {
            reloadNonce.update { it + 1 }
        }
    }

    fun handleDefaultSmsRoleResult() {
        val isDefault = isDefaultSmsApp()
        defaultSmsApp.value = isDefault
        if (isDefault) {
            reloadNonce.update { it + 1 }
            messageFlow.tryEmit("已设为默认短信应用，请重新刷新取件码列表")
        } else {
            messageFlow.tryEmit("未切换为默认短信应用，平台短信可能仍受系统限制")
        }
    }

    fun selectFilter(filterWindow: CodeFilterWindow) {
        showAllItems.value = false
        selectedFilter.value = filterWindow
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
            messageFlow.emit("已将 ${item.code} 标记为已取件")
        }
    }

    fun restorePending(item: PickupCodeItem) {
        if (!item.isPickedUp) return

        viewModelScope.launch {
            settingsRepository.markPending(item.uniqueKey)
            messageFlow.emit("已将 ${item.code} 恢复为未取件")
        }
    }

    fun saveRules(candidateRules: List<String>): Boolean {
        if (candidateRules.any { it.trim().isEmpty() }) {
            messageFlow.tryEmit("存在空白规则，请修改后再保存")
            return false
        }

        val sanitizedRules = extractor.sanitizeRules(candidateRules)
        if (sanitizedRules.isEmpty()) {
            messageFlow.tryEmit("至少保留 1 条提取规则")
            return false
        }

        val invalidRule = extractor.firstInvalidRule(sanitizedRules)
        if (invalidRule != null) {
            messageFlow.tryEmit("存在语法错误规则，请检查后再保存")
            return false
        }

        viewModelScope.launch {
            settingsRepository.saveRules(sanitizedRules)
            showAllItems.value = false
            reloadNonce.update { it + 1 }
            messageFlow.emit("提取规则已保存")
        }
        return true
    }

    fun resetRulesToDefault() {
        saveRules(PickupCodeExtractor.defaultRules)
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                settingsRepository.rulesFlow,
                settingsRepository.pickedUpItemsFlow,
                selectedFilter,
                showAllItems,
                permissionGranted,
                defaultSmsApp,
            ) { rules, pickedUpItems, filterWindow, showAll, hasPermission, isDefault ->
                HomeLoadRequest(
                    rules = rules,
                    pickedUpItems = pickedUpItems,
                    filterWindow = filterWindow,
                    showAll = showAll,
                    hasPermission = hasPermission,
                    isDefaultSmsApp = isDefault,
                )
            }.combine(reloadNonce) { request, _ ->
                request
            }.collectLatest { request ->
                uiStateFlow.update {
                    it.copy(
                        hasSmsPermission = request.hasPermission,
                        isDefaultSmsApp = request.isDefaultSmsApp,
                        isLoading = request.hasPermission,
                        selectedFilter = request.filterWindow,
                        activeRules = request.rules,
                        showAllItems = request.showAll,
                    )
                }

                if (!request.hasPermission) {
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
                        rules = request.rules,
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
            }
        }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(getApplication()) == getApplication<Application>().packageName

    private data class HomeLoadRequest(
        val rules: List<String>,
        val pickedUpItems: Set<String>,
        val filterWindow: CodeFilterWindow,
        val showAll: Boolean,
        val hasPermission: Boolean,
        val isDefaultSmsApp: Boolean,
    )
}
