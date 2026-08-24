package com.nyx.minichat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyx.minichat.data.AppMode
import com.nyx.minichat.data.ChatMessage
import com.nyx.minichat.data.Role
import com.nyx.minichat.data.SettingsStore
import com.nyx.minichat.network.ProviderClient
import com.nyx.minichat.network.RemoteApi
import com.nyx.minichat.network.RemoteModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AppScreen {
    data object Onboarding : AppScreen()
    data object RemoteGate : AppScreen()
    data object RemoteLogin : AppScreen()
    data object UserSetup : AppScreen()
    data object Chat : AppScreen()
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val errorText: String? = null,
    val remoteModels: List<RemoteModel> = emptyList(),
    val selectedRemoteModelId: Int? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsStore(application)
    private val providerClient = ProviderClient()
    private var remoteApi: RemoteApi? = null

    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Onboarding)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _mode = MutableStateFlow<AppMode?>(null)
    val mode: StateFlow<AppMode?> = _mode.asStateFlow()

    private var currentChatId: String? = null

    init {
        viewModelScope.launch {
            val onboarded = settings.onboarded.first()
            if (!onboarded) {
                _screen.value = AppScreen.Onboarding
                return@launch
            }
            val savedMode = settings.currentMode()
            _mode.value = savedMode
            when (savedMode) {
                AppMode.USER -> {
                    val byok = settings.byokConfig.first()
                    _screen.value = if (byok != null) AppScreen.Chat else AppScreen.UserSetup
                }
                AppMode.REMOTE -> {
                    val url = settings.currentServerUrl()
                    if (url.isBlank()) {
                        _screen.value = AppScreen.RemoteGate
                    } else {
                        remoteApi = RemoteApi(url)
                        _screen.value = AppScreen.RemoteGate
                    }
                }
                null -> _screen.value = AppScreen.Onboarding
            }
        }
    }

    // ---------------- Onboarding ----------------

    fun chooseMode(mode: AppMode) {
        viewModelScope.launch {
            settings.setMode(mode)
            settings.setOnboarded(true)
            _mode.value = mode
            _screen.value = when (mode) {
                AppMode.USER -> AppScreen.UserSetup
                AppMode.REMOTE -> AppScreen.RemoteGate
            }
        }
    }

    // ---------------- User mode setup ----------------

    fun saveByokConfig(adapter: String, endpoint: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            settings.setByokConfig(
                SettingsStore.ByokConfig(adapter, endpoint, modelName, apiKey)
            )
            _screen.value = AppScreen.Chat
        }
    }

    // ---------------- Remote mode: gate + login ----------------

    fun submitServerUrlAndGate(url: String, gatePassword: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                settings.setServerUrl(url)
                val api = RemoteApi(url)
                api.gate(gatePassword)
                remoteApi = api
                _screen.value = AppScreen.RemoteLogin
            }.onFailure { e -> onError(e.message ?: "Could not reach server") }
        }
    }

    fun login(username: String, password: String, onError: (String) -> Unit) {
        val api = remoteApi ?: return onError("Not connected to a server yet")
        viewModelScope.launch {
            runCatching {
                api.login(username, password)
                loadRemoteModels(api)
                _screen.value = AppScreen.Chat
            }.onFailure { e -> onError(e.message ?: "Login failed") }
        }
    }

    private suspend fun loadRemoteModels(api: RemoteApi) {
        runCatching { api.models() }.onSuccess { models ->
            _uiState.value = _uiState.value.copy(
                remoteModels = models,
                selectedRemoteModelId = models.firstOrNull()?.id,
            )
        }
    }

    fun logoutRemote() {
        viewModelScope.launch {
            remoteApi?.logout()
            _screen.value = AppScreen.RemoteLogin
            _uiState.value = ChatUiState()
        }
    }

    fun selectRemoteModel(modelId: Int) {
        _uiState.value = _uiState.value.copy(selectedRemoteModelId = modelId)
    }

    // ---------------- Sending a message ----------------

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isSending) return

        val userMessage = ChatMessage(Role.USER, text)
        val newMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(messages = newMessages, isSending = true, errorText = null)

        viewModelScope.launch {
            val assistantIndex = _uiState.value.messages.size
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage(Role.ASSISTANT, "")
            )

            val builder = StringBuilder()
            val onToken: suspend (String) -> Unit = { chunk ->
                builder.append(chunk)
                val updated = _uiState.value.messages.toMutableList()
                if (assistantIndex < updated.size) {
                    updated[assistantIndex] = ChatMessage(Role.ASSISTANT, builder.toString())
                    _uiState.value = _uiState.value.copy(messages = updated)
                }
            }

            val result = runCatching {
                when (_mode.value) {
                    AppMode.USER -> {
                        val byok = settings.byokConfig.first()
                            ?: throw IllegalStateException("No provider configured")
                        providerClient.streamChat(
                            adapter = byok.adapter,
                            endpoint = byok.endpoint,
                            apiKey = byok.apiKey,
                            modelName = byok.modelName,
                            messages = newMessages,
                            onToken = onToken,
                        )
                    }
                    AppMode.REMOTE -> {
                        val api = remoteApi ?: throw IllegalStateException("Not connected")
                        val modelId = _uiState.value.selectedRemoteModelId
                            ?: throw IllegalStateException("No model selected")
                        val adapter = _uiState.value.remoteModels.firstOrNull { it.id == modelId }?.adapter ?: "openai"
                        api.streamChat(modelId, adapter, newMessages, onToken)
                    }
                    null -> throw IllegalStateException("No mode selected")
                }
            }

            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorText = e.message ?: "Something went wrong")
            }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorText = null)
    }
}
