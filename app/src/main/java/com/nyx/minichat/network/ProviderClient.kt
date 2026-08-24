package com.nyx.minichat.network

import com.nyx.minichat.data.ChatMessage
import com.nyx.minichat.data.toApiString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * User mode: calls the model provider directly from the device, mirroring
 * streamDirectChat() + ADAPTERS in the web app's lib/adapters.js. The API
 * key never leaves the device — no server involved, same BYOK contract as
 * the web frontend's localStorage-only BYOK entries.
 */
class ProviderClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    suspend fun streamChat(
        adapter: String,
        endpoint: String,
        apiKey: String,
        modelName: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit,
    ) {
        val (url, headers, bodyJson) = when (adapter) {
            "anthropic" -> Triple(
                endpoint.ifBlank { "https://api.anthropic.com/v1/messages" },
                mapOf(
                    "x-api-key" to apiKey,
                    "anthropic-version" to "2023-06-01",
                    "anthropic-dangerous-direct-browser-access" to "true",
                    "Content-Type" to "application/json",
                ),
                buildJsonObject {
                    put("model", modelName)
                    put("max_tokens", 4096)
                    put("stream", true)
                    put("messages", messages.toAnthropicJson())
                },
            )
            else -> Triple(
                endpoint.ifBlank { "https://api.openai.com/v1/chat/completions" },
                mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json",
                ),
                buildJsonObject {
                    put("model", modelName)
                    put("stream", true)
                    put("messages", messages.toOpenAiJson())
                },
            )
        }

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val request = requestBuilder.post(bodyJson.toString().toRequestBody(jsonMedia)).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body?.string()?.take(200) ?: ""
                throw IOException("Provider request failed (${response.code}): $text")
            }
            val body = response.body ?: throw IOException("Empty response body")
            SseReader.read(body, adapter, onToken)
        }
    }

    private fun List<ChatMessage>.toOpenAiJson(): JsonArray = buildJsonArray {
        this@toOpenAiJson.forEach { m ->
            add(buildJsonObject {
                put("role", m.role.toApiString())
                put("content", m.content)
            })
        }
    }

    private fun List<ChatMessage>.toAnthropicJson(): JsonArray = buildJsonArray {
        this@toAnthropicJson.forEach { m ->
            add(buildJsonObject {
                put("role", m.role.toApiString())
                put("content", m.content)
            })
        }
    }
}
