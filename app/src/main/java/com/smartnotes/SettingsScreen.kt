package com.smartnotes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var offlineMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { settingsManager.apiKeyFlow.collect { apiKey = it ?: "" } }
    LaunchedEffect(Unit) { settingsManager.modelNameFlow.collect { modelName = it ?: "gemini-2.5-flash" } }
    LaunchedEffect(Unit) { settingsManager.offlineModeFlow.collect { offlineMode = it } }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("> CONFIGURATION_PROTOCOL", style = MaterialTheme.typography.headlineMedium, color = Color.Green, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("> BRING YOUR OWN KEY (BYOK)", color = Color.Green, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Supply an API key from your preferred provider to power the AI.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)).padding(12.dp)) {
            Text("> PROVIDER DIRECTORY", color = Color.White, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            ProviderLinkButton(context, "GROQ (FOSS Models)", "https://console.groq.com/keys")
            Spacer(modifier = Modifier.height(4.dp))
            ProviderLinkButton(context, "GOOGLE AI STUDIO", "https://aistudio.google.com/app/apikey")
            Spacer(modifier = Modifier.height(4.dp))
            ProviderLinkButton(context, "OPENAI (ChatGPT)", "https://platform.openai.com/api-keys")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; scope.launch { settingsManager.saveApiKey(it) } },
            label = { Text("INPUT API KEY", color = Color.Green) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Green, RoundedCornerShape(4.dp)),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Green, fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF0A0A0A), unfocusedContainerColor = Color(0xFF0A0A0A), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it; scope.launch { settingsManager.saveModelName(it) } },
            label = { Text("AI MODEL ID", color = Color.Green) },
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Green, RoundedCornerShape(4.dp)),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Green, fontFamily = FontFamily.Monospace),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF0A0A0A), unfocusedContainerColor = Color(0xFF0A0A0A), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LOCAL WHISPER AI", color = Color.White, fontFamily = FontFamily.Monospace)
                Text("Process voice offline", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Switch(checked = offlineMode, onCheckedChange = { offlineMode = it; scope.launch { settingsManager.saveOfflineMode(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color(0xFF0A0A0A)))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/BigWizard94"))) },
            modifier = Modifier.fillMaxWidth().shadow(15.dp, spotColor = Color(0xFFFF00FF)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF))
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = "Donate", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SUPPORT FOSS DEVELOPER", color = Color.Black, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ProviderLinkButton(context: android.content.Context, text: String, url: String) {
    Button(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Filled.Link, contentDescription = "Link", tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.Gray, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
    }
}