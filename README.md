# ScreenOCR

一个简洁的安卓屏幕文字识别工具。框选屏幕任意区域，使用 AI 视觉模型进行文字识别。

## 功能特点

- 框选屏幕任意区域进行文字识别
- 支持 OpenAI 兼容协议（GPT-4o、Claude、Gemini 等）
- Material Design 3 深色主题
- 识别结果一键复制
- 支持 Android 8.0 - 14

## 使用方法

1. 首次使用，点击「设置」配置你的 API
   - **API Base URL**：你的 API 地址（例如：`https://api.openai.com`）
   - **API Key**：你的 API 密钥
   - **模型名称**：支持视觉的模型（例如：`gpt-4o-mini`、`gpt-4o`、`claude-3-5-sonnet`）
2. 点击「开始识别」
3. 授予悬浮窗权限（首次使用需要）
4. 授予屏幕录制权限
5. 在屏幕上框选要识别的区域
6. 等待识别结果，可一键复制

## 支持的 API

本应用使用 OpenAI 兼容协议，支持以下服务：

| 服务 | Base URL 示例 | 推荐模型 |
|------|---------------|----------|
| OpenAI | `https://api.openai.com` | `gpt-4o-mini`, `gpt-4o` |
| Azure OpenAI | `https://xxx.openai.azure.com` | `gpt-4o` |
| Claude (via proxy) | 取决于代理服务 | `claude-3-5-sonnet` |
| Gemini (via proxy) | 取决于代理服务 | `gemini-1.5-flash` |
| One API / New API | 你的部署地址 | 取决于配置 |
| Done Hub | 你的部署地址 | 取决于配置 |

## API 调用格式

应用会向 `{BASE_URL}/v1/chat/completions` 发送 POST 请求：

**请求格式：**
```json
{
  "model": "gpt-4o-mini",
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "请识别图片中的所有文字..."},
        {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
      ]
    }
  ],
  "max_tokens": 4096
}
```

**请求头：**
```
Content-Type: application/json
Authorization: Bearer {API_KEY}
```

## 构建

### 环境要求

- JDK 17
- Android SDK 34
- Gradle 8.5

### 构建命令

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

## 自动构建

推送到 `main` 或 `master` 分支会自动触发 GitHub Actions 构建。

构建完成后，可在 Actions 页面下载 APK：
- `app-debug` - 调试版本（可直接安装）
- `app-release-unsigned` - 未签名的发布版本

## 权限说明

| 权限 | 用途 |
|------|------|
| `FOREGROUND_SERVICE` | 前台服务，维持屏幕截取 |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 屏幕录制服务类型 |
| `INTERNET` | 调用 OCR API |
| `SYSTEM_ALERT_WINDOW` | 显示框选遮罩层 |

## 技术栈

- Kotlin
- Material Design 3
- OkHttp
- Kotlin Coroutines
- MediaProjection API
- OpenAI Vision API

## License

MIT License
