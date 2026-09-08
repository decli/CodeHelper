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
    /** 时间面板的件数预览；尚未统计出来时为 null，界面显示「—」 */
    val pendingCountByWindow: Map<CodeFilterWindow, Int>? = null,
) {
    val pendingCount: Int
        get() = items.sumOf { item ->
            if (item.isPickedUp) 0 else item.codeCount
        }
}
