package com.voicechat.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 音频录制管理器
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude
    
    private val bufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }
    
    val hasRecordPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    
    /**
     * 开始录音
     * @return 录音数据流
     */
    fun startRecording(): ByteArrayOutputStream {
        if (!hasRecordPermission) {
            throw SecurityException("Missing RECORD_AUDIO permission")
        }
        
        val outputStream = ByteArrayOutputStream()
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }
        
        audioRecord?.startRecording()
        isRecording = true
        
        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                    
                    // 计算音量幅度
                    var sum = 0L
                    for (i in 0 until read step 2) {
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        sum += sample * sample
                    }
                    val rms = kotlin.math.sqrt(sum.toDouble() / (read / 2))
                    _amplitude.value = (rms / 32767f).coerceIn(0f, 1f)
                }
            }
        }.start()
        
        return outputStream
    }
    
    /**
     * 停止录音
     */
    fun stopRecording(): ByteArray {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _amplitude.value = 0f
        
        // 这里需要返回实际录音数据，简化处理
        return ByteArray(0)
    }
    
    fun isRecording(): Boolean = isRecording
}

/**
 * 音频播放管理器
 */
class AudioPlayer(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioPlayer"
    }
    
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState
    
    enum class PlaybackState {
        IDLE, PLAYING, COMPLETED
    }
    
    /**
     * 播放音频数据 (PCM格式)
     */
    suspend fun playPcm(audioData: ByteArray, sampleRate: Int = 24000) = withContext(Dispatchers.IO) {
        try {
            _playbackState.value = PlaybackState.PLAYING
            
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(bufferSize, audioData.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            audioTrack?.write(audioData, 0, audioData.size)
            audioTrack?.play()
            
            // 等待播放完成
            val durationMs = (audioData.size / 2 * 1000) / sampleRate
            kotlinx.coroutines.delay(durationMs.toLong())
            
            _playbackState.value = PlaybackState.COMPLETED
        } catch (e: Exception) {
            Log.e(TAG, "Play error", e)
            _playbackState.value = PlaybackState.IDLE
        }
    }
    
    /**
     * 播放 MP3 音频（需要解码器）
     * 简化版：直接返回，实际需要使用 MediaPlayer 或 ExoPlayer
     */
    suspend fun playMp3(audioData: ByteArray) = withContext(Dispatchers.IO) {
        try {
            _playbackState.value = PlaybackState.PLAYING
            
            // 保存到临时文件
            val tempFile = java.io.File(context.cacheDir, "tts_temp_${System.currentTimeMillis()}.mp3")
            tempFile.writeBytes(audioData)
            
            // 使用 MediaPlayer 播放
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            
            mediaPlayer.setOnCompletionListener {
                _playbackState.value = PlaybackState.COMPLETED
                tempFile.delete()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Play MP3 error", e)
            _playbackState.value = PlaybackState.IDLE
        }
    }
    
    fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isPlaying = false
        _playbackState.value = PlaybackState.IDLE
    }
    
    fun release() {
        stop()
    }
}
