package com.smartnotes

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
            CyberpunkTheme {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }
                val database = remember { NoteDatabase.getDatabase(context) }
                val noteDao = database.noteDao()
                val modelName by settingsManager.modelNameFlow.collectAsState(initial = "gemini-2.5-flash")
                val apiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
                var currentScreen by remember { mutableStateOf("home") }

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    if (currentScreen == "home") {
                        if (apiKey.isBlank()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("> SYSTEM HALTED: API KEY REQUIRED", color = Color.Red, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { currentScreen = "settings" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                ) {
                                    Text("CONFIGURE AI PROVIDER", color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                            }
                        } else {
                            val generativeModel = GenerativeModel(modelName = modelName, apiKey = apiKey)
                            SmartNotesScreen(generativeModel, noteDao) { currentScreen = "settings" }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                            Button(
                                onClick = { currentScreen = "home" }, 
                                modifier = Modifier.padding(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) { Text("<- RETURN", color = Color.Green, fontFamily = FontFamily.Monospace) }
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF111111),
            primary = Color.Green,
            onPrimary = Color.Black,
            secondary = Color(0xFF00FFCC),
            tertiary = Color(0xFFFF00FF)
        ),
        typography = Typography(
            bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green),
            bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green),
            headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green)
        ),
        content = content
    )
}

@Composable
fun SmartNotesScreen(generativeModel: GenerativeModel, noteDao: NoteDao, onOpenSettings: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("> AI Assistant standing by...") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val savedNotes by noteDao.getAllNotes().collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            note = if (note.isEmpty()) spokenText else "$note $spokenText"
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(scrollState)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("> SMART_NOTES_v2", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { 
                Text("CONFIG", color = Color.Green, fontFamily = FontFamily.Monospace) 
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("INPUT DATA STREAM...", color = Color.Green) },
            modifier = Modifier.fillMaxWidth().height(120.dp)
                .shadow(10.dp, spotColor = Color.Green)
                .border(1.dp, Color.Green, RoundedCornerShape(4.dp)),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Green, fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0A0A0A),
                unfocusedContainerColor = Color(0xFF0A0A0A),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                speechRecognizerLauncher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth().shadow(15.dp, spotColor = Color(0xFFFF00FF)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF))
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("INITIALIZE VOICE CAPTURE", color = Color.Black, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = { scope.launch { aiResponse = generativeModel.generateContent("Organize this:\n$note").text ?: "ERROR" } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { Text("ORGANIZE", color = Color.Green, fontFamily = FontFamily.Monospace) }

            Button(
                onClick = { scope.launch { aiResponse = generativeModel.generateContent("Extract To-Dos:\n$note").text ?: "ERROR" } },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { Text("EXTRACT", color = Color.Green, fontFamily = FontFamily.Monospace) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF050505)).border(1.dp, Color(0xFF00FFCC)).padding(16.dp)
        ) {
            Text(aiResponse, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                scope.launch {
                    if (note.isNotBlank()) {
                        noteDao.insertNote(Note(originalText = note, aiResponse = aiResponse))
                        note = ""
                        aiResponse = "> DATA SECURED IN LOCAL VAULT."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().shadow(10.dp, spotColor = Color(0xFF00FFCC)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
        ) { Text("ENCRYPT & SAVE TO VAULT", color = Color.Black, fontFamily = FontFamily.Monospace) }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("> SECURE_VAULT", style = MaterialTheme.typography.headlineSmall, color = Color.Green)
            
            Button(
                onClick = {
                    scope.launch {
                        Toast.makeText(context, "Initiating Murena Uplink...", Toast.LENGTH_SHORT).show()
                        try {
                            var exportText = "SMART NOTES EXPORT\n========================\n\n"
                            savedNotes.forEach { savedNote ->
                                exportText += "Original: ${savedNote.originalText}\nAI: ${savedNote.aiResponse}\n\n"
                            }
                            withContext(Dispatchers.IO) {
                                val client = OkHttpClient()
                                val credential = Credentials.basic("bigwizardmedia", "Kd57n-Tt5X4-GMYw2-wMF7d-Jqgtg")
                                val request = Request.Builder()
                                    .url("https://murena.io/remote.php/webdav/SmartNotes_Backup.txt")
                                    .put(exportText.toRequestBody("text/plain".toMediaTypeOrNull()))
                                    .header("Authorization", credential)
                                    .build()
                                client.newCall(request).execute().use { response ->
                                    if (!response.isSuccessful) throw Exception("Uplink Failed")
                                }
                            }
                            Toast.makeText(context, "Uplink Successful", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { 
                Icon(Icons.Filled.CloudUpload, contentDescription = "Sync", tint = Color.Green)
                Spacer(modifier = Modifier.width(4.dp))
                Text("UPLINK", color = Color.Green, fontFamily = FontFamily.Monospace) 
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        savedNotes.forEach { savedNote ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
                    Text("[$dateString]", color = Color.Gray, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(savedNote.originalText, color = Color.White, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(savedNote.aiResponse, color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
