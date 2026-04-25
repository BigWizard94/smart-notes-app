package com.smartnotes

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
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
                            SmartNotesScreen(apiKey, modelName, noteDao) { currentScreen = "settings" }
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

suspend fun fetchAiResponse(prompt: String, apiKey: String, modelName: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val isGoogle = apiKey.startsWith("AIza") || modelName.contains("gemini")
    val isGroq = apiKey.startsWith("gsk_")

    val url = when {
        isGoogle -> "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        isGroq -> "https://api.groq.com/openai/v1/chat/completions"
        else -> "https://api.openai.com/v1/chat/completions"
    }

    val jsonBody = if (isGoogle) {
        JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
    } else {
        JSONObject().put("model", modelName).put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
    }

    val requestBuilder = Request.Builder().url(url).post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
    if (!isGoogle) requestBuilder.addHeader("Authorization", "Bearer $apiKey")

    try {
        val response = client.newCall(requestBuilder.build()).execute()
        val responseData = response.body?.string() ?: return@withContext "ERROR: Empty response"
        if (!response.isSuccessful) return@withContext "ERROR: Code ${response.code}\n$responseData"

        val json = JSONObject(responseData)
        if (isGoogle) {
            return@withContext json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } else {
            return@withContext json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    } catch (e: Exception) {
        return@withContext "NETWORK ERROR: ${e.message}"
    }
}

fun formatMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        var lastIndex = 0
        boldRegex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append(matchResult.groupValues[1]) }
            lastIndex = matchResult.range.last + 1
        }
        append(text.substring(lastIndex))
    }
}

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = Color.Black, surface = Color(0xFF111111), primary = Color.Green, onPrimary = Color.Black, secondary = Color(0xFF00FFCC), tertiary = Color(0xFFFF00FF)),
        typography = Typography(bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green), bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green), headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Color.Green)),
        content = content
    )
}

@Composable
fun SmartNotesScreen(apiKey: String, modelName: String, noteDao: NoteDao, onOpenSettings: () -> Unit) {
    var note by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("> AI Assistant standing by...") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val savedNotes by noteDao.getAllNotes().collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()
    
    val whisperEngine = remember { WhisperEngine(context).apply { initializeBrain() } }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            note = if (note.isEmpty()) spokenText else "$note $spokenText"
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(scrollState)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("> SMART_NOTES_v2", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("CONFIG", color = Color.Green, fontFamily = FontFamily.Monospace) }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("INPUT DATA STREAM / QUERY...", color = Color.Green) },
            modifier = Modifier.fillMaxWidth().height(120.dp).shadow(10.dp, spotColor = Color.Green).border(1.dp, Color.Green, RoundedCornerShape(4.dp)),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Green, fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF0A0A0A), unfocusedContainerColor = Color(0xFF0A0A0A), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { speechRecognizerLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) }) },
            modifier = Modifier.fillMaxWidth().shadow(15.dp, spotColor = Color(0xFFFF00FF)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF))
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("INITIALIZE VOICE CAPTURE", color = Color.Black, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { scope.launch { aiResponse = fetchAiResponse("Organize this:\n$note", apiKey, modelName) } }, border = BorderStroke(1.dp, Color.Green), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Green)) { Text("ORGANIZE", fontFamily = FontFamily.Monospace) }
            OutlinedButton(onClick = { scope.launch { aiResponse = fetchAiResponse("Extract To-Dos:\n$note", apiKey, modelName) } }, border = BorderStroke(1.dp, Color.Green), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Green)) { Text("EXTRACT", fontFamily = FontFamily.Monospace) }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = { scope.launch { aiResponse = fetchAiResponse("Act as an AI assistant. Answer this query:\n$note", apiKey, modelName) } }, 
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.Green), 
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Green)
        ) { 
            Icon(Icons.Filled.Search, contentDescription = "Query", tint = Color.Green)
            Spacer(modifier = Modifier.width(8.dp))
            Text("QUERY AI ARCHIVES", fontFamily = FontFamily.Monospace) 
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF050505)).border(1.dp, Color(0xFF00FFCC)).padding(16.dp)) {
            Text(formatMarkdown(aiResponse), color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { scope.launch { if (note.isNotBlank()) { noteDao.insertNote(Note(originalText = note, aiResponse = aiResponse)); note = ""; aiResponse = "> DATA SECURED IN LOCAL VAULT." } } },
            modifier = Modifier.fillMaxWidth().shadow(10.dp, spotColor = Color(0xFF00FFCC)), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
        ) { Text("ENCRYPT & SAVE TO VAULT", color = Color.Black, fontFamily = FontFamily.Monospace) }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("> SECURE_VAULT", style = MaterialTheme.typography.headlineSmall, color = Color.Green)
            
            Button(
                onClick = {
                    var exportText = "SMART NOTES EXPORT\n========================\n\n"
                    savedNotes.forEach { savedNote -> exportText += "Original: ${savedNote.originalText}\nAI: ${savedNote.aiResponse}\n\n" }
                    val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, exportText); type = "text/plain" }
                    context.startActivity(Intent.createChooser(sendIntent, "Export Vault to..."))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) { 
                Icon(Icons.Filled.Share, contentDescription = "Export", tint = Color.Green)
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXPORT", color = Color.Green, fontFamily = FontFamily.Monospace) 
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        savedNotes.forEach { savedNote -> NoteCard(savedNote) }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NoteCard(savedNote: Note) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)).clickable { expanded = !expanded }.animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(savedNote.timestamp))
            Text("[$dateString]", color = Color.Gray, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(savedNote.originalText, color = Color.White, fontFamily = FontFamily.Monospace, maxLines = if (expanded) Int.MAX_VALUE else 1)
            Spacer(modifier = Modifier.height(4.dp))
            if (expanded) {
                Text(formatMarkdown(savedNote.aiResponse), color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("> TAP TO EXPAND DATABLOCK...", color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
