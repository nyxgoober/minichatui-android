package com.nyx.minichat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nyx.minichat.data.AppMode

@Composable
fun OnboardingScreen(onChooseMode: (AppMode) -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "[ MinichatUI ]",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Choose how you want to chat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ModeCard(
                    title = "User mode",
                    description = "Chat directly with your own OpenAI or Anthropic key. No server, no admin — just you.",
                    buttonLabel = "Use my own key",
                    onClick = { onChooseMode(AppMode.USER) },
                )
                ModeCard(
                    title = "Remote mode",
                    description = "Connect to an existing MinichatUI instance — same login as the web app.",
                    buttonLabel = "Connect to a server",
                    onClick = { onChooseMode(AppMode.REMOTE) },
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1f), fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(buttonLabel)
            }
        }
    }
}
