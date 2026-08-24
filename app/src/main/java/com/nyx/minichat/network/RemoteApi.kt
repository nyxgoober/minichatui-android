package com.nyx.minichat.network

import com.nyx.minichat.data.ChatMessage
import com.nyx.minichat.data.ChatSummary
import com.nyx.minichat.data.Role
import com.nyx.minichat.data.roleFromApiString
import com.nyx.minichat.data.toApiString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val status: Int) : IOException(message)

data class MeResponse(
    val username: String,
    val role: String,
)

data class RemoteModel(
    val id: Int,
    val displayName: String,
    val adapter: String, // "openai" | "anthropic" — determines SSE parsing for /api/chat
)

/**
 * Remote mode client — talks to an existing MinichatUI Worker instance
 * using the same contract as static/js/api.js. Auth is cookie-based (gate
 * + session), same as the browser; SimpleCookieJar plays the role the
 * browser normally plays automatically.
 *
 * Scope note: only the site gate + login flow is implemented here per the
 * prototype plan. Signup/invites/recovery/admin stay web-only — if login
 * fails because there's no account yet, the caller should point the user
 * to the web app to sign up.
 */
class RemoteApi(private val baseUrl: String) {

    private val cookieJar = SimpleCookieJar()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun url(path: String) = "${baseUrl.trimEnd('/')}$path"

    private suspend fun call(
        path: String,
        method: String = "GET",
        bodyJson: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(url(path))
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((bodyJson ?: "{}").toRequestBody(jsonMedia))
            "PATCH" -> builder.patch((bodyJson ?: "{}").toRequestBody(jsonMedia))
            "DELETE" -> builder.delete((bodyJson ?: "{}").toRequestBody(jsonMedia))
        }
        val response = client.newCall(builder.build()).execute()
        val text = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            val errMsg = runCatching {
                json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content
            }.getOrNull() ?: "Request failed (${response.code})"
            throw ApiException(errMsg, response.code)
        }
        text
    }

    // ---------------- Gate ----------------

    suspend fun gate(password: String) {
        call("/api/gate", "POST", buildJsonObject { put("password", password) }.toString())
    }

    // ---------------- Login ----------------

    suspend fun login(username: String, password: String) {
        call(
            "/api/login", "POST",
            buildJsonObject {
                put("username", username)
                put("password", password)
            }.toString()
        )
    }

    suspend fun logout() {
        runCatching { call("/api/logout", "POST") }
        cookieJar.clear()
    }

    // ---------------- Me ----------------

    suspend fun me(): MeResponse {
        val text = call("/api/me")
        val obj = json.parseToJsonElement(text).jsonObject
        return MeResponse(
            username = obj["username"]?.jsonPrimitive?.content ?: "",
            role = obj["role"]?.jsonPrimitive?.content ?: "user",
        )
    }

    // ---------------- Models ----------------

    suspend fun models(): List<RemoteModel> {
        val text = call("/api/models")
        val arr = json.parseToJsonElement(text).jsonObject["models"]?.jsonArray ?: JsonArray(emptyList())
        return arr.map { el ->
            val o = el.jsonObject
            RemoteModel(
                id = o["id"]?.jsonPrimitive?.int ?: 0,
                displayName = o["display_name"]?.jsonPrimitive?.content ?: "Model",
                adapter = o["adapter"]?.jsonPrimitive?.content ?: "openai",
            )
        }
    }

    // ---------------- Chats ----------------

    suspend fun chats(): List<ChatSummary> {
        val text = call("/api/chats")
        val arr = json.parseToJsonElement(text).jsonObject["chats"]?.jsonArray ?: JsonArray(emptyList())
        return arr.map { el ->
            val o = el.jsonObject
            ChatSummary(
                id = (o["id"]?.jsonPrimitive?.int ?: 0).toString(),
                title = o["title"]?.jsonPrimitive?.content ?: "New chat",
            )
        }
    }

    suspend fun createChat(title: String = "New chat"): String {
        val text = call(
            "/api/chats", "POST",
            buildJsonObject {
                put("action", "create")
                put("title", title)
            }.toString()
        )
        val id = json.parseToJsonElement(text).jsonObject["chatId"]?.jsonPrimitive?.int ?: 0
        return id.toString()
    }

    suspend fun loadChat(chatId: String): List<ChatMessage> {
        val text = call("/api/chats?chatId=$chatId")
        val arr = json.parseToJsonElement(text).jsonObject["messages"]?.jsonArray ?: JsonArray(emptyList())
        return arr.map { el ->
            val o = el.jsonObject
            ChatMessage(
                role = roleFromApiString(o["role"]?.jsonPrimitive?.content ?: "user"),
                content = o["content"]?.jsonPrimitive?.content ?: "",
            )
        }
    }

    suspend fun addMessage(chatId: String, role: Role, content: String) {
        call(
            "/api/chats", "POST",
            buildJsonObject {
                put("action", "addMessage")
                put("chatId", chatId.toInt())
                put("role", role.toApiString())
                put("content", content)
            }.toString()
        )
    }

    suspend fun renameChat(chatId: String, title: String) {
        call(
            "/api/chats", "POST",
            buildJsonObject {
                put("action", "rename")
                put("chatId", chatId.toInt())
                put("title", title)
            }.toString()
        )
    }

    suspend fun generateChatTitle(chatId: String): String {
        val text = call(
            "/api/chats", "POST",
            buildJsonObject {
                put("action", "title")
                put("chatId", chatId.toInt())
            }.toString()
        )
        return json.parseToJsonElement(text).jsonObject["title"]?.jsonPrimitive?.content ?: "New chat"
    }

    suspend fun deleteChat(chatId: String) {
        call("/api/chats", "DELETE", buildJsonObject { put("chatId", chatId.toInt()) }.toString())
    }

    // ---------------- Streaming chat (server-routed / admin-provisioned) ----------------

    suspend fun streamChat(
        modelId: Int,
        adapter: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit,
    ) {
        val bodyJson = buildJsonObject {
            put("modelId", modelId)
            put("messages", buildJsonArray {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", m.role.toApiString())
                        put("content", m.content)
                    })
                }
            })
        }.toString()

        val request = Request.Builder()
            .url(url("/api/chat"))
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()

        // Only the blocking .execute() call needs to move off the main
        // thread; SseReader.read below calls onToken(), which updates
        // Compose state and should run on the caller's original dispatcher
        // (StateFlow updates from a background thread are safe, but
        // keeping this off Dispatchers.IO avoids surprises for callers
        // that assume Main).
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        response.use {
            if (!it.isSuccessful) {
                val text = withContext(Dispatchers.IO) { it.body?.string()?.take(200) ?: "" }
                throw ApiException("Chat request failed: $text", it.code)
            }
            val body = it.body ?: throw IOException("Empty response body")
            SseReader.read(body, adapter = adapter, onToken = onToken)
        }
    }
}
