package com.nyx.minichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyx.minichat.ui.screens.ChatScreen
import com.nyx.minichat.ui.screens.OnboardingScreen
import com.nyx.minichat.ui.screens.RemoteGateScreen
import com.nyx.minichat.ui.screens.RemoteLoginScreen
import com.nyx.minichat.ui.screens.UserSetupScreen
import com.nyx.minichat.ui.theme.MinichatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinichatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MinichatRoot()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MinichatRoot(viewModel: ChatViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()

    when (screen) {
        is AppScreen.Onboarding -> OnboardingScreen(onChooseMode = viewModel::chooseMode)

        is AppScreen.UserSetup -> UserSetupScreen(onSave = viewModel::saveByokConfig)

        is AppScreen.RemoteGate -> RemoteGateScreen(
            onSubmit = { url, password, onError ->
                viewModel.submitServerUrlAndGate(url, password, onError)
            }
        )

        is AppScreen.RemoteLogin -> RemoteLoginScreen(
            onLogin = { username, password, onError ->
                viewModel.login(username, password, onError)
            }
        )

        is AppScreen.Chat -> ChatScreen(
            mode = mode,
            uiState = uiState,
            onSend = viewModel::sendMessage,
            onSelectModel = viewModel::selectRemoteModel,
            onLogout = viewModel::logoutRemote,
            onDismissError = viewModel::dismissError,
        )
    }
}
