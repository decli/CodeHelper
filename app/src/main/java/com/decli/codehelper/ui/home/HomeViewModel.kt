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
    private val includeDeletedOnRefresh = MutableStateFlow(false)
    private val permissionGranted = MutableStateFlow(hasSmsPermission())
    private val reloadNonce = MutableStateFlow(0)
    private val uiStateFlow = MutableStateFlow(
        HomeUiState(hasSmsPermission = permissionGranted.value),
    )

    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val uiState: StateFlow<HomeUiState> = uiStateFlow.asStateFlow()
    val messages = messageFlow.asSharedFlow()

    init {
        observeData()
    }

    fun refreshPermissionStatus() {
        permissionGranted.value = hasSmsPermission()
    }

    fun updatePermissionStatus(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) {
            reloadNonce.update { it + 1 }
        }
    }

    fun selectFilter(filterWindow: CodeFilterWindow) {
        includeDeletedOnRefresh.value = false
        selectedFilter.value = filterWindow
    }

    fun forceRefresh() {
        if (!permissionGranted.value) {
            messageFlow.tryEmit("请先授权短信读取权限")
            return
        }
        includeDeletedOnRefresh.value = true
        reloadNonce.update { it + 1 }
        messageFlow.tryEmit("已强制刷新，灰色卡片表示曾删除")
    }

    fun hideCode(item: PickupCodeItem) {
        if (item.isDeleted) return

        viewModelScope.launch {
            settingsRepository.markDeleted(item.uniqueKey)
            messageFlow.emit("已隐藏取件码 ${item.code}")
        }
    }

    fun saveRules(candidateRules: List<String>): Boolean {
        val sanitizedRules = extractor.sanitizeRules(candidateRules)
        if (sanitizedRules.isEmpty()) {
            messageFlow.tryEmit("至少保留 1 条提取规则")
            return false
        }

        val invalidRule = extractor.firstInvalidRule(sanitizedRules)
        if (invalidRule != null) {
            messageFlow.tryEmit("规则无效：$invalidRule")
            return false
        }

        viewModelScope.launch {
            settingsRepository.saveRules(sanitizedRules)
            includeDeletedOnRefresh.value = false
            reloadNonce.update { it + 1 }
            messageFlow.emit("提取规则已保存")
        }
        return true
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                settingsRepository.rulesFlow,
                settingsRepository.deletedItemsFlow,
                selectedFilter,
                includeDeletedOnRefresh,
                permissionGranted,
            ) { rules, deletedItems, filterWindow, showDeleted, hasPermission ->
                HomeLoadRequest(
                    rules = rules,
                    deletedItems = deletedItems,
                    filterWindow = filterWindow,
                    showDeleted = showDeleted,
                    hasPermission = hasPermission,
                )
            }.combine(reloadNonce) { request, _ ->
                request
            }.collectLatest { request ->
                uiStateFlow.update {
                    it.copy(
                        hasSmsPermission = request.hasPermission,
                        isLoading = request.hasPermission,
                        selectedFilter = request.filterWindow,
                        activeRules = request.rules,
                        showDeletedOnRefresh = request.showDeleted,
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
                        deletedKeys = request.deletedItems,
                        includeDeleted = request.showDeleted,
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

    private data class HomeLoadRequest(
        val rules: List<String>,
        val deletedItems: Set<String>,
        val filterWindow: CodeFilterWindow,
        val showDeleted: Boolean,
        val hasPermission: Boolean,
    )
}
