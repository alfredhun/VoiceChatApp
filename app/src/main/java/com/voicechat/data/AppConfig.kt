package com.voicechat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config")

/**
 * 应用配置管理
 */
data class AppConfig(
    val llmProvider: LLMProvider = LLMProvider.OPENAI,
    val ttsProvider: TTSProvider = TTSProvider.ELEVENLABS,
    val openaiApiKey: String = "",
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val openaiModel: String = "gpt-4o-mini",
    val volcanoAccessKey: String = "",
    val volcanoSecretKey: String = "",
    val volcanoEndpointId: String = "",  // 火山引擎推理接入点ID
    val volcanoModel: String = "doubao-pro-32k",
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM", // Rachel
    val volcanoTtsAppId: String = "",
    val volcanoTtsAccessToken: String = "",
    val volcanoTtsVoiceType: String = "BV700_V2_flow", // 火山TTS音色
    val systemPrompt: String = "你是一个友好、自然的AI助手。用简洁、口语化的方式回答问题。",
    val isConfigured: Boolean = false
)

enum class LLMProvider(val displayName: String) {
    OPENAI("OpenAI (GPT)"),
    VOLCANO("火山引擎 (豆包)")
}

enum class TTSProvider(val displayName: String) {
    ELEVENLABS("ElevenLabs"),
    VOLCANO("火山TTS")
}

class ConfigRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val TTS_PROVIDER = stringPreferencesKey("tts_provider")
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
        val OPENAI_MODEL = stringPreferencesKey("openai_model")
        val VOLCANO_ACCESS_KEY = stringPreferencesKey("volcano_access_key")
        val VOLCANO_SECRET_KEY = stringPreferencesKey("volcano_secret_key")
        val VOLCANO_ENDPOINT_ID = stringPreferencesKey("volcano_endpoint_id")
        val VOLCANO_MODEL = stringPreferencesKey("volcano_model")
        val ELEVENLABS_API_KEY = stringPreferencesKey("elevenlabs_api_key")
        val ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val VOLCANO_TTS_APP_ID = stringPreferencesKey("volcano_tts_app_id")
        val VOLCANO_TTS_ACCESS_TOKEN = stringPreferencesKey("volcano_tts_access_token")
        val VOLCANO_TTS_VOICE_TYPE = stringPreferencesKey("volcano_tts_voice_type")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val IS_CONFIGURED = booleanPreferencesKey("is_configured")
    }
    
    val appConfig: Flow<AppConfig> = context.dataStore.data.map { preferences ->
        AppConfig(
            llmProvider = preferences[PreferencesKeys.LLM_PROVIDER]?.let { 
                LLMProvider.valueOf(it) 
            } ?: LLMProvider.OPENAI,
            ttsProvider = preferences[PreferencesKeys.TTS_PROVIDER]?.let { 
                TTSProvider.valueOf(it) 
            } ?: TTSProvider.ELEVENLABS,
            openaiApiKey = preferences[PreferencesKeys.OPENAI_API_KEY] ?: "",
            openaiBaseUrl = preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/v1",
            openaiModel = preferences[PreferencesKeys.OPENAI_MODEL] ?: "gpt-4o-mini",
            volcanoAccessKey = preferences[PreferencesKeys.VOLCANO_ACCESS_KEY] ?: "",
            volcanoSecretKey = preferences[PreferencesKeys.VOLCANO_SECRET_KEY] ?: "",
            volcanoEndpointId = preferences[PreferencesKeys.VOLCANO_ENDPOINT_ID] ?: "",
            volcanoModel = preferences[PreferencesKeys.VOLCANO_MODEL] ?: "doubao-pro-32k",
            elevenLabsApiKey = preferences[PreferencesKeys.ELEVENLABS_API_KEY] ?: "",
            elevenLabsVoiceId = preferences[PreferencesKeys.ELEVENLABS_VOICE_ID] ?: "21m00Tcm4TlvDq8ikWAM",
            volcanoTtsAppId = preferences[PreferencesKeys.VOLCANO_TTS_APP_ID] ?: "",
            volcanoTtsAccessToken = preferences[PreferencesKeys.VOLCANO_TTS_ACCESS_TOKEN] ?: "",
            volcanoTtsVoiceType = preferences[PreferencesKeys.VOLCANO_TTS_VOICE_TYPE] ?: "BV700_V2_flow",
            systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "你是一个友好、自然的AI助手。用简洁、口语化的方式回答问题。",
            isConfigured = preferences[PreferencesKeys.IS_CONFIGURED] ?: false
        )
    }
    
    suspend fun saveConfig(config: AppConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_PROVIDER] = config.llmProvider.name
            preferences[PreferencesKeys.TTS_PROVIDER] = config.ttsProvider.name
            preferences[PreferencesKeys.OPENAI_API_KEY] = config.openaiApiKey
            preferences[PreferencesKeys.OPENAI_BASE_URL] = config.openaiBaseUrl
            preferences[PreferencesKeys.OPENAI_MODEL] = config.openaiModel
            preferences[PreferencesKeys.VOLCANO_ACCESS_KEY] = config.volcanoAccessKey
            preferences[PreferencesKeys.VOLCANO_SECRET_KEY] = config.volcanoSecretKey
            preferences[PreferencesKeys.VOLCANO_ENDPOINT_ID] = config.volcanoEndpointId
            preferences[PreferencesKeys.VOLCANO_MODEL] = config.volcanoModel
            preferences[PreferencesKeys.ELEVENLABS_API_KEY] = config.elevenLabsApiKey
            preferences[PreferencesKeys.ELEVENLABS_VOICE_ID] = config.elevenLabsVoiceId
            preferences[PreferencesKeys.VOLCANO_TTS_APP_ID] = config.volcanoTtsAppId
            preferences[PreferencesKeys.VOLCANO_TTS_ACCESS_TOKEN] = config.volcanoTtsAccessToken
            preferences[PreferencesKeys.VOLCANO_TTS_VOICE_TYPE] = config.volcanoTtsVoiceType
            preferences[PreferencesKeys.SYSTEM_PROMPT] = config.systemPrompt
            preferences[PreferencesKeys.IS_CONFIGURED] = true
        }
    }
    
    suspend fun clearConfig() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
