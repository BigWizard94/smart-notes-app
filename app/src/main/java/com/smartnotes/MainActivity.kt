package com.smartnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }
                
                // Live-observe the saved values from DataStore
                val modelName by settingsManager.modelNameFlow.collectAsState(initial = "gpt-4o")
                val apiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
                
                var currentScreen by remember { mutableStateOf("home") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen == "home") {
                        if (apiKey.isBlank()) {
                            // NO KEY FOUND: Lock the app and force them to Settings
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Welcome to Smart Notes!", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("F-Droid requires you to bring your own API key.")
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = { currentScreen = "settings" }) {
                                    Text("Configure AI Provider")
                                }
                            }
                        } else {
                            // KEY FOUND: Build the AI dynamically and load the notes!
                            val generativeModel = GenerativeModel(
                                modelName = modelName,
                                apiKey = apiKey
                            )
                            SmartNotesScreen(
                                generativeModel = generativeModel,
                                onOpenSettings = { currentScreen = "settings" }
                            )
                        }
                    } else {
                        Column {
                            Button(
                                onClick = { currentScreen = "home" }, 
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("<- Back to Notes")
                            }
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartNotesScreen(generativeModel: GenerativeModel, onOpenSettings: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("AI Assistant is ready.") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Smart Notes", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onOpenSettings) {
                Text("Settings")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Write a note...") },
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    aiResponse = "Thinking..."
                    try {
                        val prompt = "Summarize or organize this note:\n$note"
                        val response = generativeModel.generateContent(prompt)
                        aiResponse = response.text ?: "No response."
                    } catch (e: Exception) {
                        aiResponse = "Error: ${e.localizedMessage}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ask AI to Organize")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Assistant:", style = MaterialTheme.typography.titleMedium)
        Text(aiResponse)
    }
}
