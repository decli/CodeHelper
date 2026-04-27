package com.decli.codehelper.ui.home

import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.PickupCodeItem
import com.decli.codehelper.util.PickupCodeExtractor

data class HomeUiState(
    val hasSmsPermission: Boolean = false,
    val isLoading: Boolean = false,
    val selectedFilter: CodeFilterWindow = CodeFilterWindow.Last12Hours,
    val items: List<PickupCodeItem> = emptyList(),
    val activePromptKeywords: List<String> = PickupCodeExtractor.defaultPromptKeywords,
    val activeAdvancedRules: List<String> = PickupCodeExtractor.defaultAdvancedRules,
    val showAllItems: Boolean = false,
    val badgeRefreshMinutes: Int = 5,
    val lastLoadedAtMillis: Long? = null,
) {
    val pendingCount: Int
        get() = items.sumOf { item ->
            if (item.isPickedUp) 0 else item.codeCount
        }
}

