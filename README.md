# 语音助手 App

一个实时语音对话的安卓应用，支持多种 LLM 和 TTS 后端。

## 功能特性

- 🎤 **实时语音交互**：按住说话，自动识别并回复
- 🤖 **多模型支持**：
  - LLM：OpenAI (GPT-4o) / 火山引擎（豆包）
  - TTS：ElevenLabs / 火山TTS
- 💬 **对话历史**：保留上下文，多轮对话
- 🎨 **现代UI**：Material 3 设计，动画效果

## 项目结构

```
voice-chat-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/voicechat/
│   │   │   ├── data/           # 配置管理
│   │   │   ├── network/        # API 客户端
│   │   │   ├── audio/          # 音频录制/播放
│   │   │   ├── ui/             # UI 界面
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 快速开始

### 1. 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 34
- Gradle 8.5

### 2. 获取 API Key

| 服务 | 获取地址 |
|------|----------|
| OpenAI | https://platform.openai.com |
| 火山引擎 | https://console.volcengine.com |
| ElevenLabs | https://elevenlabs.io |

### 3. 编译运行

```bash
# 克隆或下载项目后
cd voice-chat-app

# 编译
./gradlew assembleDebug

# 或用 Android Studio 打开项目，点击 Run
```

## 配置说明

### OpenAI 配置

| 字段 | 说明 |
|------|------|
| API Key | OpenAI API 密钥 |
| Base URL | API 地址（默认官方，可自定义代理） |
| Model | 模型名称（gpt-4o-mini、gpt-4o 等） |

### 火山引擎（豆包）配置

| 字段 | 说明 |
|------|------|
| Access Key | 火山引擎 AK |
| Secret Key | 火山引擎 SK |
| Endpoint ID | 推理接入点 ID |

### ElevenLabs 配置

| 字段 | 说明 |
|------|------|
| API Key | ElevenLabs API 密钥 |
| Voice ID | 音色 ID（默认：Rachel） |

### 火山TTS 配置

| 字段 | 说明 |
|------|------|
| App ID | 火山TTS 应用 ID |
| Access Token | 访问令牌 |
| Voice Type | 音色类型 |

## 技术栈

- **语言**：Kotlin
- **UI框架**：Jetpack Compose + Material 3
- **架构**：MVVM
- **网络**：OkHttp
- **存储**：DataStore
- **导航**：Jetpack Navigation

## 常见问题

### Q: 语音识别无法工作？
A: 确保已授予录音权限，并安装了 Google 语音服务。

### Q: TTS 播放失败？
A: 检查 API Key 是否正确，网络是否通畅。

### Q: 火山引擎 LLM 报错？
A: 确保已在火山引擎控制台创建推理接入点，并填写正确的 Endpoint ID。

## License

MIT License
