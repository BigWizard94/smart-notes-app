package com.smartnotes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var offlineMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("> CONFIGURATION_PROTOCOL", style = MaterialTheme.typography.headlineMedium, color = Color.Green, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(32.dp))

        // Offline Whisper.cpp Toggle
        Row(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("LOCAL WHISPER AI", color = Color.White, fontFamily = FontFamily.Monospace)
                Text("Process voice 100% offline", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Switch(
                checked = offlineMode,
                onCheckedChange = { offlineMode = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color(0xFF0A0A0A))
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // FOSS Monetization Button
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/BigWizard94"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().shadow(15.dp, spotColor = Color(0xFFFF00FF)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF00FF))
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = "Donate", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SUPPORT FOSS DEVELOPER", color = Color.Black, fontFamily = FontFamily.Monospace)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("> ALL TIPS DIRECTLY FUND OPEN SOURCE DEVELOPMENT.", color = Color.Gray, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}
