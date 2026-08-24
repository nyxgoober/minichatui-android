package com.nyx.minichat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RemoteGateScreen(onSubmit: (url: String, password: String, onError: (String) -> Unit) -> Unit) {
    var url by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("[ MinichatUI ]", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Enter your server and site password",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = androidx.compose.ui.Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://your-instance.pages.dev") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Site password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        error = null
                        loading = true
                        onSubmit(url, password) { msg ->
                            loading = false
                            error = msg
                        }
                    },
                    enabled = url.isNotBlank() && password.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Continue")
                }
            }
        }
    }
}
