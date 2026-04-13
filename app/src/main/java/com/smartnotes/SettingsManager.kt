package com.smartnotes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create the DataStore instance safely
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class SettingsManager(private val context: Context) {
    
    // Define the keys for our three settings
    companion object {
        val BASE_URL_KEY = stringPreferencesKey("base_url")
        val MODEL_NAME_KEY = stringPreferencesKey("model_name")
        val API_KEY_KEY = stringPreferencesKey("api_key")
    }

    // Read the values (Flows will automatically update the UI when changed)
    val baseUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL_KEY] ?: "https://api.openai.com/v1/chat/completions"
    }
    
    val modelNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL_NAME_KEY] ?: "gpt-4o"
    }
    
    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY_KEY] ?: ""
    }

    // Write the values to local storage
    suspend fun saveSettings(baseUrl: String, modelName: String, apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = baseUrl
            preferences[MODEL_NAME_KEY] = modelName
            preferences[API_KEY_KEY] = apiKey
        }
    }
}
