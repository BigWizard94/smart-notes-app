package com.smartnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Put your Gemini API Key here (from Google AI Studio)
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyCiPRkONlJpNVAYkexKWd68_lXXrjm3o9k" 
        )

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartNoteScreen(generativeModel)
                }
            }
        }
    }
}

@Composable
fun SmartNoteScreen(generativeModel: GenerativeModel) {
    var note by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("AI Assistant is ready.") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Smart Notes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Write a note...") },
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = {
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
        }) {
            Text("Ask AI to Organize")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Assistant:", style = MaterialTheme.typography.titleMedium)
        Text(aiResponse)
    }
}
