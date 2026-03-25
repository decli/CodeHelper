package com.decli.codehelper.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.decli.codehelper.util.PickupCodeExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "code_helper_settings")

class SettingsRepository(
    private val context: Context,
) {
    private companion object {
        val rulesKey = stringPreferencesKey("pickup_rules")
        val pickedUpItemsKey = stringSetPreferencesKey("picked_up_items")
        val legacyDeletedItemsKey = stringSetPreferencesKey("deleted_pickup_items")
    }

    val rulesFlow: Flow<List<String>> =
        context.settingsDataStore.data.map { preferences ->
            preferences[rulesKey]
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toList()
                ?.ifEmpty { null }
                ?: PickupCodeExtractor.defaultRules
        }

    val pickedUpItemsFlow: Flow<Set<String>> =
        context.settingsDataStore.data.map { preferences ->
            preferences[pickedUpItemsKey].orEmpty() + preferences[legacyDeletedItemsKey].orEmpty()
        }

    suspend fun saveRules(rules: List<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[rulesKey] = rules.joinToString(separator = "\n")
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
}

