package com.voicechat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voicechat.audio.AudioPlayer
import com.voicechat.audio.SpeechRecognizer
import com.voicechat.data.*
import com.voicechat.network.LLMClient
import com.voicechat.network.TTSClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 主聊天 ViewModel
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configRepository = ConfigRepository(application)
    private val llmClient = LLMClient()
    private val ttsClient = TTSClient()
    private val speechRecognizer = SpeechRecognizer(application)
    private val audioPlayer = AudioPlayer(application)
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState
    
    private val _config = configRepository.appConfig
        .stateIn(viewModelScope, SharingStarted.Lazily, AppConfig())
    
    val config: StateFlow<AppConfig> = _config
    
    private var conversationHistory = mutableListOf<LLMClient.Message>()
    private var recognitionJob: Job? = null
    private var ttsJob: Job? = null
    
    init {
        viewModelScope.launch {
            configRepository.appConfig.collect { appConfig ->
                _uiState.update { it.copy(isConfigured = appConfig.isConfigured) }
            }
        }
    }
    
    /**
     * 开始语音识别
     */
    fun startListening() {
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            _uiState.update { it.copy(isListening = true, status = "正在聆听...") }
            
            speechRecognizer.startListening()
                .collect { state ->
                    when (state) {
                        is SpeechRecognizer.RecognitionState.Ready -> {
                            _uiState.update { it.copy(status = "请说话...") }
                        }
                        is SpeechRecognizer.RecognitionState.Listening -> {
                            _uiState.update { it.copy(amplitude = state.amplitude) }
                        }
                        is SpeechRecognizer.RecognitionState.PartialResult -> {
                            _uiState.update { it.copy(recognizedText = state.text, status = "识别中...") }
                        }
                        is SpeechRecognizer.RecognitionState.Result -> {
                            _uiState.update { 
                                it.copy(
                                    recognizedText = state.text, 
                                    isListening = false,
                                    status = ""
                                ) 
                            }
                            if (state.text.isNotEmpty()) {
                                sendMessage(state.text)
                            }
                        }
                        is SpeechRecognizer.RecognitionState.Error -> {
                            _uiState.update { 
                                it.copy(
                                    isListening = false, 
                                    status = "错误: ${state.message}",
                                    error = state.message
                                ) 
                            }
                        }
                        is SpeechRecognizer.RecognitionState.Idle -> {
                            _uiState.update { it.copy(isListening = false) }
                        }
                    }
                }
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        speechRecognizer.stopListening()
        recognitionJob?.cancel()
        _uiState.update { it.copy(isListening = false, status = "") }
    }
    
    /**
     * 发送消息到 LLM
     */
    private fun sendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isThinking = true, status = "AI 正在思考...") }
            
            // 添加用户消息到历史
            conversationHistory.add(LLMClient.Message("user", text))
            
            // 构建完整消息（包含系统提示）
            val messages = buildList {
                add(LLMClient.Message("system", _config.value.systemPrompt))
                addAll(conversationHistory)
            }
            
            val result = llmClient.chat(_config.value, messages)
            
            result.fold(
                onSuccess = { response ->
                    // 添加助手回复到历史
                    conversationHistory.add(LLMClient.Message("assistant", response))
                    
                    _uiState.update { 
                        it.copy(
                            lastResponse = response,
                            isThinking = false,
                            status = ""
                        ) 
                    }
                    
                    // 自动播放 TTS
                    speakText(response)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isThinking = false, 
                            error = error.message,
                            status = ""
                        ) 
                    }
                }
            )
        }
    }
    
    /**
     * 语音合成
     */
    private fun speakText(text: String) {
        ttsJob?.cancel()
        ttsJob = viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true, status = "正在播放...") }
            
            val result = ttsClient.textToSpeech(_config.value, text)
            
            result.fold(
                onSuccess = { audioData ->
                    // 播放 MP3 音频
                    audioPlayer.playMp3(audioData)
                    
                    audioPlayer.playbackState.collect { state ->
                        if (state == AudioPlayer.PlaybackState.COMPLETED) {
                            _uiState.update { it.copy(isSpeaking = false, status = "") }
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isSpeaking = false, 
                            error = "TTS错误: ${error.message}",
                            status = ""
                        ) 
                    }
                }
            )
        }
    }
    
    /**
     * 停止播放
     */
    fun stopSpeaking() {
        ttsJob?.cancel()
        audioPlayer.stop()
        _uiState.update { it.copy(isSpeaking = false, status = "") }
    }
    
    /**
     * 清除对话历史
     */
    fun clearHistory() {
        conversationHistory.clear()
        _uiState.update { ChatUiState(isConfigured = _uiState.value.isConfigured) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognizer.destroy()
        audioPlayer.release()
    }
}

/**
 * 聊天 UI 状态
 */
data class ChatUiState(
    val isConfigured: Boolean = false,
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val recognizedText: String = "",
    val lastResponse: String = "",
    val status: String = "",
    val amplitude: Float = 0f,
    val error: String? = null
)

/**
 * 配置 ViewModel
 */
class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configRepository = ConfigRepository(application)
    
    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState
    
    val currentConfig = configRepository.appConfig.stateIn(
        viewModelScope, 
        SharingStarted.Lazily, 
        AppConfig()
    )
    
    init {
        viewModelScope.launch {
            configRepository.appConfig.collect { config ->
                _uiState.update { 
                    it.copy(
                        llmProvider = config.llmProvider,
                        ttsProvider = config.ttsProvider,
                        openaiApiKey = config.openaiApiKey,
                        openaiBaseUrl = config.openaiBaseUrl,
                        openaiModel = config.openaiModel,
                        volcanoAccessKey = config.volcanoAccessKey,
                        volcanoSecretKey = config.volcanoSecretKey,
                        volcanoEndpointId = config.volcanoEndpointId,
                        elevenLabsApiKey = config.elevenLabsApiKey,
                        elevenLabsVoiceId = config.elevenLabsVoiceId,
                        volcanoTtsAppId = config.volcanoTtsAppId,
                        volcanoTtsAccessToken = config.volcanoTtsAccessToken,
                        volcanoTtsVoiceType = config.volcanoTtsVoiceType,
                        systemPrompt = config.systemPrompt
                    ) 
                }
            }
        }
    }
    
    fun setLlmProvider(provider: LLMProvider) {
        _uiState.update { it.copy(llmProvider = provider) }
    }
    
    fun setTtsProvider(provider: TTSProvider) {
        _uiState.update { it.copy(ttsProvider = provider) }
    }
    
    fun setOpenaiApiKey(key: String) {
        _uiState.update { it.copy(openaiApiKey = key) }
    }
    
    fun setOpenaiBaseUrl(url: String) {
        _uiState.update { it.copy(openaiBaseUrl = url) }
    }
    
    fun setOpenaiModel(model: String) {
        _uiState.update { it.copy(openaiModel = model) }
    }
    
    fun setVolcanoAccessKey(key: String) {
        _uiState.update { it.copy(volcanoAccessKey = key) }
    }
    
    fun setVolcanoSecretKey(key: String) {
        _uiState.update { it.copy(volcanoSecretKey = key) }
    }
    
    fun setVolcanoEndpointId(id: String) {
        _uiState.update { it.copy(volcanoEndpointId = id) }
    }
    
    fun setElevenLabsApiKey(key: String) {
        _uiState.update { it.copy(elevenLabsApiKey = key) }
    }
    
    fun setElevenLabsVoiceId(id: String) {
        _uiState.update { it.copy(elevenLabsVoiceId = id) }
    }
    
    fun setVolcanoTtsAppId(id: String) {
        _uiState.update { it.copy(volcanoTtsAppId = id) }
    }
    
    fun setVolcanoTtsAccessToken(token: String) {
        _uiState.update { it.copy(volcanoTtsAccessToken = token) }
    }
    
    fun setVolcanoTtsVoiceType(type: String) {
        _uiState.update { it.copy(volcanoTtsVoiceType = type) }
    }
    
    fun setSystemPrompt(prompt: String) {
        _uiState.update { it.copy(systemPrompt = prompt) }
    }
    
    /**
     * 保存配置
     */
    fun saveConfig(onSaved: () -> Unit) {
        val state = _uiState.value
        
        // 验证必填字段
        val isValid = when (state.llmProvider) {
            LLMProvider.OPENAI -> state.openaiApiKey.isNotBlank()
            LLMProvider.VOLCANO -> state.volcanoAccessKey.isNotBlank() && state.volcanoEndpointId.isNotBlank()
        } && when (state.ttsProvider) {
            TTSProvider.ELEVENLABS -> state.elevenLabsApiKey.isNotBlank()
            TTSProvider.VOLCANO -> state.volcanoTtsAppId.isNotBlank() && state.volcanoTtsAccessToken.isNotBlank()
        }
        
        if (!isValid) {
            _uiState.update { it.copy(error = "请填写完整的API配置") }
            return
        }
        
        viewModelScope.launch {
            val config = AppConfig(
                llmProvider = state.llmProvider,
                ttsProvider = state.ttsProvider,
                openaiApiKey = state.openaiApiKey,
                openaiBaseUrl = state.openaiBaseUrl,
                openaiModel = state.openaiModel,
                volcanoAccessKey = state.volcanoAccessKey,
                volcanoSecretKey = state.volcanoSecretKey,
                volcanoEndpointId = state.volcanoEndpointId,
                elevenLabsApiKey = state.elevenLabsApiKey,
                elevenLabsVoiceId = state.elevenLabsVoiceId,
                volcanoTtsAppId = state.volcanoTtsAppId,
                volcanoTtsAccessToken = state.volcanoTtsAccessToken,
                volcanoTtsVoiceType = state.volcanoTtsVoiceType,
                systemPrompt = state.systemPrompt,
                isConfigured = true
            )
            
            configRepository.saveConfig(config)
            onSaved()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ConfigUiState(
    val llmProvider: LLMProvider = LLMProvider.OPENAI,
    val ttsProvider: TTSProvider = TTSProvider.ELEVENLABS,
    val openaiApiKey: String = "",
    val openaiBaseUrl: String = "https://api.openai.com/v1",
    val openaiModel: String = "gpt-4o-mini",
    val volcanoAccessKey: String = "",
    val volcanoSecretKey: String = "",
    val volcanoEndpointId: String = "",
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
    val volcanoTtsAppId: String = "",
    val volcanoTtsAccessToken: String = "",
    val volcanoTtsVoiceType: String = "BV700_V2_flow",
    val systemPrompt: String = "你是一个友好、自然的AI助手。用简洁、口语化的方式回答问题。",
    val error: String? = null
)
