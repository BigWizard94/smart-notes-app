package com.smartnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keeping the hardcoded key for one more step until DataStore is wired up!
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AIzaSyCiPRk0N1JpNVAYkexKWd68_1XXrjm3o9k"
        )

        setContent {
            MaterialTheme {
                // This variable tracks which screen is currently visible
                var currentScreen by remember { mutableStateOf("home") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen == "home") {
                        // Show the notes UI and pass a command to open settings
                        SmartNotesScreen(
                            generativeModel = generativeModel,
                            onOpenSettings = { currentScreen = "settings" }
                        )
                    } else {
                        // Show the Settings screen with a quick Back button wrapper
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
        // NEW: A Row that places the Title and Settings button on the same line
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
