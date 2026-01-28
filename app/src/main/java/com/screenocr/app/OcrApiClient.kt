package com.screenocr.app

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OcrApiClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap, quality = 85)

            // Build OpenAI Vision API request
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", "请识别图片中的所有文字，只返回识别出的文字内容，不要添加任何解释或说明。如果图片中没有文字，请回复"未识别到文字"。")
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("max_tokens", 4096)
            }.toString()

            // Use /v1/chat/completions endpoint (OpenAI compatible)
            val endpoint = if (baseUrl.endsWith("/v1")) {
                "$baseUrl/chat/completions"
            } else if (baseUrl.endsWith("/")) {
                "${baseUrl}v1/chat/completions"
            } else {
                "$baseUrl/v1/chat/completions"
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            parseOpenAIResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseOpenAIResponse(response: Response): Result<String> {
        val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))

        if (!response.isSuccessful) {
            return try {
                val json = JSONObject(body)
                val error = json.optJSONObject("error")
                val message = error?.optString("message") ?: "HTTP ${response.code}"
                Result.failure(Exception(message))
            } catch (e: Exception) {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        }

        return try {
            val json = JSONObject(body)

            // Parse OpenAI Chat Completions response
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                val content = message?.optString("content")
                if (!content.isNullOrBlank()) {
                    return Result.success(content.trim())
                }
            }

            // Fallback: try other common response formats
            when {
                json.has("result") -> Result.success(json.getString("result"))
                json.has("text") -> Result.success(json.getString("text"))
                json.optBoolean("success", false) -> {
                    val text = json.optJSONObject("data")?.optString("text")
                        ?: return Result.failure(Exception("No text in response"))
                    Result.success(text)
                }
                else -> Result.failure(Exception("无法解析响应"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("解析响应失败: ${e.message}"))
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()

        // Compress bitmap if too large
        val scaledBitmap = if (bitmap.width > 2048 || bitmap.height > 2048) {
            val scale = minOf(2048f / bitmap.width, 2048f / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
