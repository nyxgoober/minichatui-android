package com.nyx.minichat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "minichat_settings")

enum class AppMode { USER, REMOTE }

/**
 * Persists everything the prototype needs to remember between launches:
 * which mode you're in, remote server config, and the user-mode (BYOK)
 * provider credentials. This is a flat prototype-grade store — no
 * encryption at rest yet. Fine for a personal single-device prototype;
 * revisit before this is used with anyone else's keys.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val MODE = stringPreferencesKey("mode")

        // Remote mode
        val SERVER_URL = stringPreferencesKey("server_url")

        // User mode (BYOK) — direct-to-provider, mirrors static/js/app.js
        // getByokEntries() shape but simplified to one active provider
        // config for the prototype rather than a full list.
        val PROVIDER_ADAPTER = stringPreferencesKey("provider_adapter") // "openai" | "anthropic"
        val PROVIDER_ENDPOINT = stringPreferencesKey("provider_endpoint")
        val PROVIDER_MODEL = stringPreferencesKey("provider_model")
        val PROVIDER_API_KEY = stringPreferencesKey("provider_api_key")

        val THEME = stringPreferencesKey("theme") // "dark" | "light"
    }

    val onboarded: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    val mode: Flow<AppMode?> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.MODE]?.let { runCatching { AppMode.valueOf(it) }.getOrNull() }
        }

    val serverUrl: Flow<String> =
        context.dataStore.data.map { it[Keys.SERVER_URL] ?: "" }

    val theme: Flow<String> =
        context.dataStore.data.map { it[Keys.THEME] ?: "dark" }

    data class ByokConfig(
        val adapter: String,
        val endpoint: String,
        val modelName: String,
        val apiKey: String,
    )

    val byokConfig: Flow<ByokConfig?> =
        context.dataStore.data.map { prefs ->
            val key = prefs[Keys.PROVIDER_API_KEY]
            val model = prefs[Keys.PROVIDER_MODEL]
            if (key.isNullOrBlank() || model.isNullOrBlank()) return@map null
            ByokConfig(
                adapter = prefs[Keys.PROVIDER_ADAPTER] ?: "openai",
                endpoint = prefs[Keys.PROVIDER_ENDPOINT] ?: "",
                modelName = model,
                apiKey = key,
            )
        }

    suspend fun setMode(mode: AppMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setOnboarded(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDED] = done }
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[Keys.SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun setByokConfig(config: ByokConfig) {
        context.dataStore.edit {
            it[Keys.PROVIDER_ADAPTER] = config.adapter
            it[Keys.PROVIDER_ENDPOINT] = config.endpoint
            it[Keys.PROVIDER_MODEL] = config.modelName
            it[Keys.PROVIDER_API_KEY] = config.apiKey
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun currentMode(): AppMode? = mode.first()
    suspend fun currentServerUrl(): String = serverUrl.first()
}
