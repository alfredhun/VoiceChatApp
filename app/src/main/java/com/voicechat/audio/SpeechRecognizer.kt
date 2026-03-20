package com.voicechat.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 语音识别管理器
 * 使用 Android 系统 ASR（支持离线）
 */
class SpeechRecognizer(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechRecognizer"
    }
    
    private var speechRecognizer: AndroidSpeechRecognizer? = null
    
    sealed class RecognitionState {
        data object Ready : RecognitionState()
        data class Listening(val amplitude: Float = 0f) : RecognitionState()
        data class PartialResult(val text: String) : RecognitionState()
        data class Result(val text: String) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
        data object Idle : RecognitionState()
    }
    
    /**
     * 开始语音识别
     */
    fun startListening(): Flow<RecognitionState> = callbackFlow {
        val recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(RecognitionState.Ready)
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                trySend(RecognitionState.Listening(rmsdB / 10f))
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }
            
            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    AndroidSpeechRecognizer.ERROR_AUDIO -> "音频错误"
                    AndroidSpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                    AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                    AndroidSpeechRecognizer.ERROR_NETWORK -> "网络错误"
                    AndroidSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                    AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                    AndroidSpeechRecognizer.ERROR_SERVER -> "服务器错误"
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                    else -> "未知错误: $error"
                }
                trySend(RecognitionState.Error(errorMsg))
                close()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                trySend(RecognitionState.Result(text))
                close()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotEmpty()) {
                    trySend(RecognitionState.PartialResult(text))
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        
        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)
        
        awaitClose {
            recognizer.stopListening()
            recognizer.cancel()
            recognizer.destroy()
            speechRecognizer = null
        }
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
    }
    
    fun cancel() {
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    fun destroy() {
        cancel()
    }
}
