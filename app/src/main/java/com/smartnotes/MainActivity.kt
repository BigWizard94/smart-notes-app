package com.smartnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                
                val modelName by settingsManager.modelNameFlow.collectAsState(initial = "gpt-4o")
                val apiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
                
                var currentScreen by remember { mutableStateOf("home") }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen == "home") {
                        if (apiKey.isBlank()) {
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
    
    // NEW: We tell the app to remember where the user has scrolled
    val scrollState = rememberScrollState()

    // NEW: Added .verticalScroll to the Column so the page can slide up and down
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()
        .verticalScroll(scrollState)
    ) {
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

        // NEW: We split the actions into a Row of two side-by-side buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    scope.launch {
                        aiResponse = "Organizing..."
                        try {
                            val prompt = "Organize this note into a clean list or structure:\n$note"
                            val response = generativeModel.generateContent(prompt)
                            aiResponse = response.text ?: "No response."
                        } catch (e: Exception) {
                            aiResponse = "Error: ${e.localizedMessage}"
                        }
                    }
                }
            ) {
                Text("Organize")
            }

            Button(
                onClick = {
                    scope.launch {
                        aiResponse = "Summarizing..."
                        try {
                            val prompt = "Give me a very short, direct summary of this note:\n$note"
                            val response = generativeModel.generateContent(prompt)
                            aiResponse = response.text ?: "No response."
                        } catch (e: Exception) {
                            aiResponse = "Error: ${e.localizedMessage}"
                        }
                    }
                }
            ) {
                Text("Summarize")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Assistant:", style = MaterialTheme.typography.titleMedium)
        Text(aiResponse)
        
        // Add a little padding at the very bottom so text isn't cut off by the screen edge
        Spacer(modifier = Modifier.height(32.dp))
    }
}
