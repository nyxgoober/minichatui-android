package com.nyx.minichat.data

// Mirrors the {role, content} shape used throughout the web app
// (static/js/app.js state.messages, functions/api/chats.js messages table)
// so the same model works whether it came from the local BYOK path or the
// remote server's /api/chats.

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val role: Role,
    val content: String,
)

data class ChatSummary(
    val id: String,
    val title: String,
)

fun Role.toApiString(): String = when (this) {
    Role.USER -> "user"
    Role.ASSISTANT -> "assistant"
}

fun roleFromApiString(value: String): Role =
    if (value == "assistant") Role.ASSISTANT else Role.USER
