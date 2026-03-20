package com.voicechat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_chat_config")

/**
 * 配置管理器 - 使用DataStore持久化存储
 */
class ConfigManager(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val CONFIG_KEY = stringPreferencesKey("app_config")
    }
    
    val configFlow: Flow<AppConfig> = context.dataStore.data.map { preferences ->
        val json = preferences[CONFIG_KEY] ?: return@map AppConfig()
        try {
            gson.fromJson(json, AppConfig::class.java)
        } catch (e: Exception) {
            AppConfig()
        }
    }
    
    suspend fun saveConfig(config: AppConfig) {
        context.dataStore.edit { preferences ->
            preferences[CONFIG_KEY] = gson.toJson(config)
        }
    }
    
    suspend fun clearConfig() {
        context.dataStore.edit { preferences ->
            preferences.remove(CONFIG_KEY)
        }
    }
}
