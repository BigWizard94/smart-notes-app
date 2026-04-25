package com.smartnotes

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsManager(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val modelNameFlow: Flow<String> = context.dataStore.data.map { it[MODEL_NAME] ?: "gemini-2.5-flash" }
    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { it[OFFLINE_MODE] ?: false }

    suspend fun saveApiKey(key: String) { context.dataStore.edit { it[API_KEY] = key } }
    suspend fun saveModelName(name: String) { context.dataStore.edit { it[MODEL_NAME] = name } }
    suspend fun saveOfflineMode(isOffline: Boolean) { context.dataStore.edit { it[OFFLINE_MODE] = isOffline } }
}