package com.decli.codehelper.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.decli.codehelper.model.CodeFilterWindow
import com.decli.codehelper.model.CodeScale
import com.decli.codehelper.model.DisplaySettings
import com.decli.codehelper.model.ExtractorSettings
import com.decli.codehelper.model.ThemeMode
import com.decli.codehelper.util.PickupCodeExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "code_helper_settings")

class SettingsRepository(
    private val context: Context,
) {
    private companion object {
        val promptKeywordsKey = stringPreferencesKey("pickup_prompt_keywords")
        val advancedRulesKey = stringPreferencesKey("pickup_rules")
        val pickedUpItemsKey = stringSetPreferencesKey("picked_up_items")
        val legacyDeletedItemsKey = stringSetPreferencesKey("deleted_pickup_items")
        val selectedFilterKey = stringPreferencesKey("selected_filter")
        val badgeRefreshMinutesKey = intPreferencesKey("badge_refresh_minutes")
        val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
        val codeScaleKey = stringPreferencesKey("code_scale")
        val groupBySenderKey = booleanPreferencesKey("group_by_sender")
        val themeModeKey = stringPreferencesKey("theme_mode")
    }

    val extractorSettingsFlow: Flow<ExtractorSettings> =
        context.settingsDataStore.data.map { preferences ->
            ExtractorSettings(
                promptKeywords = preferences[promptKeywordsKey]
                    .toSettingList()
                    .ifEmpty { PickupCodeExtractor.defaultPromptKeywords },
                advancedRules = preferences[advancedRulesKey]
                    .toSettingList()
                    .ifEmpty { PickupCodeExtractor.defaultAdvancedRules },
            )
        }

    val pickedUpItemsFlow: Flow<Set<String>> =
        context.settingsDataStore.data.map { preferences ->
            preferences[pickedUpItemsKey].orEmpty() + preferences[legacyDeletedItemsKey].orEmpty()
        }

    val selectedFilterFlow: Flow<CodeFilterWindow> =
        context.settingsDataStore.data.map { preferences ->
            val storedName = preferences[selectedFilterKey]
            CodeFilterWindow.entries.firstOrNull { it.name == storedName } ?: CodeFilterWindow.Last12Hours
        }

    val badgeRefreshMinutesFlow: Flow<Int> =
        context.settingsDataStore.data.map { preferences ->
            coerceBadgeRefreshMinutes(preferences[badgeRefreshMinutesKey])
        }

    val displaySettingsFlow: Flow<DisplaySettings> =
        context.settingsDataStore.data.map { preferences ->
            DisplaySettings(
                codeScale = CodeScale.entries
                    .firstOrNull { it.name == preferences[codeScaleKey] }
                    ?: CodeScale.Standard,
                groupBySender = preferences[groupBySenderKey] ?: true,
                themeMode = ThemeMode.entries
                    .firstOrNull { it.name == preferences[themeModeKey] }
                    ?: ThemeMode.System,
                onboardingDone = preferences[onboardingDoneKey] ?: false,
                isLoaded = true,
            )
        }

    suspend fun saveExtractorSettings(
        promptKeywords: List<String>,
        advancedRules: List<String>,
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[promptKeywordsKey] = promptKeywords.joinToString(separator = "\n")
            preferences[advancedRulesKey] = advancedRules.joinToString(separator = "\n")
        }
    }

    suspend fun markPickedUp(uniqueKey: String) {
        context.settingsDataStore.edit { preferences ->
            val current = (preferences[pickedUpItemsKey].orEmpty() + preferences[legacyDeletedItemsKey].orEmpty())
                .toMutableSet()
            current += uniqueKey
            preferences[pickedUpItemsKey] = current
            preferences.remove(legacyDeletedItemsKey)
        }
    }

    suspend fun markPending(uniqueKey: String) {
        context.settingsDataStore.edit { preferences ->
            val current = (preferences[pickedUpItemsKey].orEmpty() + preferences[legacyDeletedItemsKey].orEmpty())
                .toMutableSet()
            current -= uniqueKey
            preferences[pickedUpItemsKey] = current
            preferences.remove(legacyDeletedItemsKey)
        }
    }

    suspend fun saveSelectedFilter(filterWindow: CodeFilterWindow) {
        context.settingsDataStore.edit { preferences ->
            preferences[selectedFilterKey] = filterWindow.name
        }
    }

    suspend fun saveBadgeRefreshMinutes(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[badgeRefreshMinutesKey] = coerceBadgeRefreshMinutes(minutes)
        }
    }

    suspend fun saveOnboardingDone(done: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[onboardingDoneKey] = done
        }
    }

    suspend fun saveCodeScale(codeScale: CodeScale) {
        context.settingsDataStore.edit { preferences ->
            preferences[codeScaleKey] = codeScale.name
        }
    }

    suspend fun saveGroupBySender(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[groupBySenderKey] = enabled
        }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.name
        }
    }

    /** 恢复默认：识别规则、角标频率与显示设置一起回到出厂值，不影响已取件记录与引导标记 */
    suspend fun restoreDefaults() {
        context.settingsDataStore.edit { preferences ->
            preferences[promptKeywordsKey] =
                PickupCodeExtractor.defaultPromptKeywords.joinToString(separator = "\n")
            preferences[advancedRulesKey] =
                PickupCodeExtractor.defaultAdvancedRules.joinToString(separator = "\n")
            preferences[badgeRefreshMinutesKey] = DEFAULT_BADGE_REFRESH_MINUTES
            preferences[codeScaleKey] = CodeScale.Standard.name
            preferences[groupBySenderKey] = true
            preferences[themeModeKey] = ThemeMode.System.name
        }
    }
}

private const val DEFAULT_BADGE_REFRESH_MINUTES = 5

private fun String?.toSettingList(): List<String> =
    this
        ?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toList()
        .orEmpty()

private fun coerceBadgeRefreshMinutes(minutes: Int?): Int =
    (minutes ?: DEFAULT_BADGE_REFRESH_MINUTES).coerceIn(5, 120)
