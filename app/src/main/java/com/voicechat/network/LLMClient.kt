package com.voicechat.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.voicechat.data.AppConfig
import com.voicechat.data.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * LLM API 客户端
 */
class LLMClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    data class Message(
        val role: String,
        val content: String
    )
    
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean = false,
        val max_tokens: Int = 1024,
        val temperature: Double = 0.7
    )
    
    data class ChatResponse(
        val id: String?,
        val choices: List<Choice>?,
        val error: ErrorBody?
    )
    
    data class Choice(
        val index: Int,
        val message: Message,
        val finish_reason: String?
    )
    
    data class ErrorBody(
        val message: String?,
        val type: String?
    )
    
    // 火山引擎请求格式
    data class VolcanoRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean = false
    )
    
    /**
     * 发送消息到 LLM
     */
    suspend fun chat(
        config: AppConfig,
        messages: List<Message>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (config.llmProvider) {
                LLMProvider.OPENAI -> chatOpenAI(config, messages)
                LLMProvider.VOLCANO -> chatVolcano(config, messages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun chatOpenAI(config: AppConfig, messages: List<Message>): Result<String> {
        val request = ChatRequest(
            model = config.openaiModel,
            messages = messages,
            stream = false
        )
        
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("${config.openaiBaseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.openaiApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        val response = client.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            return Result.failure(Exception("API Error: ${response.code} - $errorBody"))
        }
        
        val responseBody = response.body?.string() ?: return Result.failure(Exception("Empty response"))
        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
        
        if (chatResponse.error != null) {
            return Result.failure(Exception(chatResponse.error.message ?: "Unknown error"))
        }
        
        val content = chatResponse.choices?.firstOrNull()?.message?.content
            ?: return Result.failure(Exception("No response content"))
        
        return Result.success(content)
    }
    
    private fun chatVolcano(config: AppConfig, messages: List<Message>): Result<String> {
        val request = VolcanoRequest(
            model = config.volcanoEndpointId, // 火山引擎使用 endpoint_id 作为 model
            messages = messages,
            stream = false
        )
        
        val body = gson.toJson(request).toRequestBody("application/json".toMediaType())
        
        // 火山引擎豆包 API 地址
        val url = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
        
        val httpRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.volcanoAccessKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        val response = client.newCall(httpRequest).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            return Result.failure(Exception("Volcano API Error: ${response.code} - $errorBody"))
        }
        
        val responseBody = response.body?.string() ?: return Result.failure(Exception("Empty response"))
        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
        
        val content = chatResponse.choices?.firstOrNull()?.message?.content
            ?: return Result.failure(Exception("No response content"))
        
        return Result.success(content)
    }
}
