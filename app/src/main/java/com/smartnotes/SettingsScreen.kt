package com.smartnotes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    var modelName by remember { mutableStateOf("gpt-4o") }
    var apiKey by remember { mutableStateOf("") }

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
                // DataStore save logic goes here
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}

