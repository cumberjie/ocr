# ScreenOCR

一个简洁的安卓屏幕文字识别工具。框选屏幕任意区域，自动截图并调用 OCR API 识别文字。

## 功能特点

- 框选屏幕任意区域进行文字识别
- 支持自定义 OCR API（用户自行配置）
- Material Design 3 深色主题
- 识别结果一键复制
- 支持 Android 8.0 - 14

## 使用方法

1. 首次使用，点击「设置」配置你的 OCR API
   - 填写 API Base URL（例如：`https://api.example.com/v1`）
   - 填写 API Key
2. 点击「开始识别」
3. 授予悬浮窗权限（首次使用需要）
4. 授予屏幕录制权限
5. 在屏幕上框选要识别的区域
6. 等待识别结果，可一键复制

## API 格式

应用会向 `{BASE_URL}/ocr` 发送 POST 请求：

**请求格式：**
```json
{
  "image": "data:image/jpeg;base64,/9j/4AAQ...",
  "type": "ocr"
}
```

**请求头：**
```
Content-Type: application/json
Authorization: Bearer {API_KEY}
```

**响应格式：**
```json
{
  "success": true,
  "data": {
    "text": "识别出的文字"
  }
}
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
- `app-debug` - 调试版本
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

## License

MIT License
