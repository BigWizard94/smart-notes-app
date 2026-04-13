package com.smartnotes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    // Read the currently saved values so the text boxes aren't empty when opened
    val savedBaseUrl by settingsManager.baseUrlFlow.collectAsState(initial = "")
    val savedModelName by settingsManager.modelNameFlow.collectAsState(initial = "")
    val savedApiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")

    var baseUrl by remember(savedBaseUrl) { mutableStateOf(savedBaseUrl) }
    var modelName by remember(savedModelName) { mutableStateOf(savedModelName) }
    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("AI Provider Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Configure your custom AI endpoint to power Smart Notes.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("API Base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            label = { Text("Model Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                // FIRE THE DATASTORE SAVE EVENT!
                scope.launch {
                    settingsManager.saveSettings(baseUrl, modelName, apiKey)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}
