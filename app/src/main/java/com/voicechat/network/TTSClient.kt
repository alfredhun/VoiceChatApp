package com.voicechat.network

import com.voicechat.data.AppConfig
import com.voicechat.data.TTSProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * TTS API 客户端
 */
class TTSClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 将文本转换为语音
     * @return 音频数据的 ByteArray (MP3/PCM 格式)
     */
    suspend fun textToSpeech(
        config: AppConfig,
        text: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            when (config.ttsProvider) {
                TTSProvider.ELEVENLABS -> ttsElevenLabs(config, text)
                TTSProvider.VOLCANO -> ttsVolcano(config, text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ElevenLabs TTS
     */
    private fun ttsElevenLabs(config: AppConfig, text: String): Result<ByteArray> {
        val url = "https://api.elevenlabs.io/v1/text-to-speech/${config.elevenLabsVoiceId}"
        
        val requestBody = """
            {
                "text": "$text",
                "model_id": "eleven_monolingual_v1",
                "voice_settings": {
                    "stability": 0.5,
                    "similarity_boost": 0.75
                }
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", config.elevenLabsApiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            return Result.failure(Exception("ElevenLabs TTS Error: ${response.code}"))
        }
        
        val audioBytes = response.body?.bytes() ?: return Result.failure(Exception("Empty audio response"))
        return Result.success(audioBytes)
    }
    
    /**
     * 火山引擎 TTS
     * 使用 WebSocket 协议，这里简化为 HTTP 方式
     */
    private fun ttsVolcano(config: AppConfig, text: String): Result<ByteArray> {
        // 火山引擎 TTS 需要 WebSocket 连接
        // 这里使用简化版本，实际项目建议使用官方 SDK
        
        val url = "https://openspeech.bytedance.com/api/v1/tts"
        
        val requestBody = """
            {
                "app": {
                    "appid": "${config.volcanoTtsAppId}",
                    "token": "${config.volcanoTtsAccessToken}",
                    "cluster": "volcano_tts"
                },
                "user": {
                    "uid": "voice_chat_user"
                },
                "audio": {
                    "voice_type": "${config.volcanoTtsVoiceType}",
                    "encoding": "mp3",
                    "speed_ratio": 1.0,
                    "volume_ratio": 1.0,
                    "pitch_ratio": 1.0
                },
                "request": {
                    "reqid": "${System.currentTimeMillis()}",
                    "text": "$text",
                    "operation": "query"
                }
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            return Result.failure(Exception("Volcano TTS Error: ${response.code}"))
        }
        
        val audioBytes = response.body?.bytes() ?: return Result.failure(Exception("Empty audio response"))
        return Result.success(audioBytes)
    }
}
