package com.smartnotes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }
                
                val database = remember { NoteDatabase.getDatabase(context) }
                val noteDao = database.noteDao()
                
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
                                noteDao = noteDao,
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
fun SmartNotesScreen(generativeModel: GenerativeModel, noteDao: NoteDao, onOpenSettings: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("AI Assistant is ready.") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val savedNotes by noteDao.getAllNotes().collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    // NEW: The Android File Saver Launcher!
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write("SMART NOTES VAULT EXPORT\n")
                            writer.write("========================\n\n")
                            savedNotes.forEach { savedNote ->
                                val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
                                writer.write("Date: $dateString\n")
                                writer.write("Original: ${savedNote.originalText}\n")
                                writer.write("AI: ${savedNote.aiResponse}\n")
                                writer.write("------------------------\n\n")
                            }
                        }
                    }
                    Toast.makeText(context, "Vault Exported to Phone!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
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
            }) { Text("Organize") }

            Button(onClick = {
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
            }) { Text("Summarize") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Assistant:", style = MaterialTheme.typography.titleMedium)
        Text(aiResponse)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    if (note.isNotBlank()) {
                        val newNote = Note(originalText = note, aiResponse = aiResponse)
                        noteDao.insertNote(newNote)
                        note = ""
                        aiResponse = "Saved to Vault!"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Save to Database")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // NEW: Vault Header with the Export Button side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved Vault", style = MaterialTheme.typography.headlineSmall)
            
            Button(
                onClick = { exportLauncher.launch("SmartNotes_Backup.txt") },
                enabled = savedNotes.isNotEmpty() // Button is disabled if the vault is empty!
            ) {
                Text("Export to Phone")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (savedNotes.isEmpty()) {
            Text("Your vault is currently empty.", style = MaterialTheme.typography.bodyMedium)
        } else {
            savedNotes.forEach { savedNote ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
                        Text(dateString, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("Original: ${savedNote.originalText}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("AI: ${savedNote.aiResponse}", style = MaterialTheme.typography.bodySmall)
                        
                        Button(
                            onClick = { scope.launch { noteDao.deleteNote(savedNote) } },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
