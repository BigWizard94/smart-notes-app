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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
                val modelName by settingsManager.modelNameFlow.collectAsState(initial = "gemini-2.5-flash")
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
                            val generativeModel = GenerativeModel(modelName = modelName, apiKey = apiKey)
                            SmartNotesScreen(
                                generativeModel = generativeModel,
                                noteDao = noteDao,
                                onOpenSettings = { currentScreen = "settings" }
                            )
                        }
                    } else {
                        Column {
                            Button(onClick = { currentScreen = "home" }, modifier = Modifier.padding(16.dp)) {
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
    var searchQuery by remember { mutableStateOf("") } 
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val savedNotes by noteDao.getAllNotes().collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    val filteredNotes = if (searchQuery.isBlank()) {
        savedNotes
    } else {
        savedNotes.filter {
            it.originalText.contains(searchQuery, ignoreCase = true) ||
            it.aiResponse.contains(searchQuery, ignoreCase = true)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write("SMART NOTES VAULT EXPORT\n========================\n\n")
                            savedNotes.forEach { savedNote ->
                                val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
                                writer.write("Date: $dateString\nOriginal: ${savedNote.originalText}\nAI: ${savedNote.aiResponse}\n------------------------\n\n")
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

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(scrollState)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Smart Notes", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Write a note...") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                scope.launch {
                    aiResponse = "Organizing..."
                    try {
                        aiResponse = generativeModel.generateContent("Organize this note into a clean list or structure:\n$note").text ?: "No response."
                    } catch (e: Exception) { aiResponse = "Error: ${e.localizedMessage}" }
                }
            }) { Text("Organize") }
9
            Button(onClick = {
                scope.launch {
                    aiResponse = "Summarizing..."
                    try {
                        aiResponse = generativeModel.generateContent("Give me a very short, direct summary of this note:\n$note").text ?: "No response."
                    } catch (e: Exception) { aiResponse = "Error: ${e.localizedMessage}" }
                }
            }) { Text("Summarize") }

            Button(onClick = {
                scope.launch {
                    aiResponse = "Extracting To-Dos..."
                    try {
                        aiResponse = generativeModel.generateContent("Extract a clear, bulleted list of actionable to-do items from this note. Only list the actions:\n$note").text ?: "No response."
                    } catch (e: Exception) { aiResponse = "Error: ${e.localizedMessage}" }
                }
            }) { Text("To-Dos") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Assistant:", style = MaterialTheme.typography.titleMedium)
        Text(aiResponse)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                scope.launch {
                    if (note.isNotBlank()) {
                        noteDao.insertNote(Note(originalText = note, aiResponse = aiResponse))
                        note = ""
                        aiResponse = "Saved to Vault!"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text("Save to Database") }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // NEW: Dual Export/Sync Buttons!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved Vault", style = MaterialTheme.typography.headlineSmall)
            
            Row {
                Button(
                    onClick = { exportLauncher.launch("SmartNotes_Backup.txt") },
                    enabled = savedNotes.isNotEmpty()
                ) { Text("Phone") }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // THE WEBDAV SYNC BUTTON
                Button(
                    onClick = {
                        scope.launch {
                            Toast.makeText(context, "Syncing to Murena Cloud...", Toast.LENGTH_SHORT).show()
                            try {
                                var exportText = "SMART NOTES VAULT EXPORT\n========================\n\n"
                                savedNotes.forEach { savedNote ->
                                    val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
                                    exportText += "Date: $dateString\nOriginal: ${savedNote.originalText}\nAI: ${savedNote.aiResponse}\n------------------------\n\n"
                                }

                                // Send the data in the background so the app doesn't freeze
                                withContext(Dispatchers.IO) {
                                    val client = OkHttpClient()
                                    val credential = Credentials.basic("bigwizardmedia", "Kd57n-Tt5X4-GMYw2-wMF7d-Jqgtg")
