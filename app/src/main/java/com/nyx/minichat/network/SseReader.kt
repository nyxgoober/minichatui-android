package com.nyx.minichat.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody

/**
 * Direct Kotlin port of readSSEStream() in static/js/api.js. Reads an SSE
 * body line by line, buffering incomplete lines, and invokes onToken for
 * each incremental text delta — same two adapter shapes as the web app
 * (openai-style choices[0].delta.content, anthropic-style
 * content_block_delta.delta.text).
 */
object SseReader {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(body: ResponseBody, adapter: String, onToken: suspend (String) -> Unit) {
        val source = body.source()
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) continue
            val payload = trimmed.substring(5).trim()
            if (payload == "[DONE]" || payload.isEmpty()) continue

            val parsed = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: continue

            parsed["error"]?.let { errEl ->
                val msg = runCatching { errEl.jsonPrimitive.content }.getOrNull() ?: "Upstream error"
                throw RuntimeException(msg)
            }

            if (adapter == "anthropic") {
                val type = parsed["type"]?.jsonPrimitive?.content
                if (type == "content_block_delta") {
                    val text = parsed["delta"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                    if (!text.isNullOrEmpty()) onToken(text)
                }
            } else {
                val choice = parsed["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                val delta = choice?.get("delta")?.jsonObject
                val content = delta?.get("content")?.jsonPrimitive?.content
                if (!content.isNullOrEmpty()) onToken(content)
            }
        }
    }
}
