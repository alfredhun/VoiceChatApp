package com.voicechat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicechat.data.LLMProvider
import com.voicechat.data.TTSProvider

/**
 * 配置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 错误提示
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LLM 配置区域
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "大语言模型 (LLM)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // LLM Provider 选择
                    Text(
                        text = "选择模型提供商",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.llmProvider == LLMProvider.OPENAI,
                            onClick = { viewModel.setLlmProvider(LLMProvider.OPENAI) },
                            label = { Text("OpenAI") }
                        )
                        FilterChip(
                            selected = uiState.llmProvider == LLMProvider.VOLCANO,
                            onClick = { viewModel.setLlmProvider(LLMProvider.VOLCANO) },
                            label = { Text("火山-豆包") }
                        )
                    }
                    
                    // OpenAI 配置
                    if (uiState.llmProvider == LLMProvider.OPENAI) {
                        OutlinedTextField(
                            value = uiState.openaiApiKey,
                            onValueChange = { viewModel.setOpenaiApiKey(it) },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            trailingIcon = {
                                var visible by remember { mutableStateOf(false) }
                                IconButton(onClick = { visible = !visible }) {
                                    Icon(
                                        if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                        
                        OutlinedTextField(
                            value = uiState.openaiBaseUrl,
                            onValueChange = { viewModel.setOpenaiBaseUrl(it) },
                            label = { Text("API Base URL (可选)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("https://api.openai.com/v1") }
                        )
                        
                        OutlinedTextField(
                            value = uiState.openaiModel,
                            onValueChange = { viewModel.setOpenaiModel(it) },
                            label = { Text("模型名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("gpt-4o-mini") }
                        )
                    }
                    
                    // 火山引擎配置
                    if (uiState.llmProvider == LLMProvider.VOLCANO) {
                        OutlinedTextField(
                            value = uiState.volcanoAccessKey,
                            onValueChange = { viewModel.setVolcanoAccessKey(it) },
                            label = { Text("Access Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        
                        OutlinedTextField(
                            value = uiState.volcanoSecretKey,
                            onValueChange = { viewModel.setVolcanoSecretKey(it) },
                            label = { Text("Secret Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        
                        OutlinedTextField(
                            value = uiState.volcanoEndpointId,
                            onValueChange = { viewModel.setVolcanoEndpointId(it) },
                            label = { Text("推理接入点 ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            
            // TTS 配置区域
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "语音合成 (TTS)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "选择语音服务",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.ttsProvider == TTSProvider.ELEVENLABS,
                            onClick = { viewModel.setTtsProvider(TTSProvider.ELEVENLABS) },
                            label = { Text("ElevenLabs") }
                        )
                        FilterChip(
                            selected = uiState.ttsProvider == TTSProvider.VOLCANO,
                            onClick = { viewModel.setTtsProvider(TTSProvider.VOLCANO) },
                            label = { Text("火山TTS") }
                        )
                    }
                    
                    // ElevenLabs 配置
                    if (uiState.ttsProvider == TTSProvider.ELEVENLABS) {
                        OutlinedTextField(
                            value = uiState.elevenLabsApiKey,
                            onValueChange = { viewModel.setElevenLabsApiKey(it) },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        
                        OutlinedTextField(
                            value = uiState.elevenLabsVoiceId,
                            onValueChange = { viewModel.setElevenLabsVoiceId(it) },
                            label = { Text("Voice ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("默认: Rachel (21m00Tcm4TlvDq8ikWAM)") }
                        )
                    }
                    
                    // 火山TTS 配置
                    if (uiState.ttsProvider == TTSProvider.VOLCANO) {
                        OutlinedTextField(
                            value = uiState.volcanoTtsAppId,
                            onValueChange = { viewModel.setVolcanoTtsAppId(it) },
                            label = { Text("App ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = uiState.volcanoTtsAccessToken,
                            onValueChange = { viewModel.setVolcanoTtsAccessToken(it) },
                            label = { Text("Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        
                        OutlinedTextField(
                            value = uiState.volcanoTtsVoiceType,
                            onValueChange = { viewModel.setVolcanoTtsVoiceType(it) },
                            label = { Text("音色类型") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("例如: BV700_V2_flow") }
                        )
                    }
                }
            }
            
            // 系统提示词配置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "系统提示词",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = uiState.systemPrompt,
                        onValueChange = { viewModel.setSystemPrompt(it) },
                        label = { Text("System Prompt") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            }
            
            // 保存按钮
            Button(
                onClick = {
                    viewModel.saveConfig {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存配置")
            }
            
            // 帮助信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "获取 API Key",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• OpenAI: platform.openai.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 火山引擎: console.volcengine.com",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• ElevenLabs: elevenlabs.io",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
