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
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class OcrApiClient(
    private val baseUrl: String,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun recognizeText(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap, quality = 80)
            val requestBody = JSONObject().apply {
                put("image", "data:image/jpeg;base64,$base64Image")
                put("type", "ocr")
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/ocr")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseResponse(response: Response): Result<String> {
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

            // Multi-format compatible parsing
            when {
                json.optBoolean("success", false) -> {
                    val text = json.optJSONObject("data")?.optString("text")
                        ?: return Result.failure(Exception("No text in response"))
                    Result.success(text)
                }
                json.has("result") -> Result.success(json.getString("result"))
                json.has("text") -> Result.success(json.getString("text"))
                json.has("error") -> {
                    val error = json.getJSONObject("error")
                    Result.failure(Exception(error.optString("message", "Unknown error")))
                }
                else -> Result.success(body) // Return raw response as text
            }
        } catch (e: Exception) {
            // If JSON parsing fails, return raw body
            Result.success(body)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()

        // Compress bitmap
        val scaledBitmap = if (bitmap.width > 1920 || bitmap.height > 1920) {
            val scale = minOf(1920f / bitmap.width, 1920f / bitmap.height)
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
