package com.decli.codehelper.ui.home

import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.util.PickupCodeExtractor

data class HomeUiState(
    val hasSmsPermission: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    val isLoading: Boolean = false,
    val selectedFilter: CodeFilterWindow = CodeFilterWindow.Last12Hours,
    val items: List<PickupCodeItem> = emptyList(),
    val activeRules: List<String> = PickupCodeExtractor.defaultRules,
    val showAllItems: Boolean = false,
    val lastLoadedAtMillis: Long? = null,
) {
    val pendingCount: Int
        get() = items.count { !it.isPickedUp }
}

