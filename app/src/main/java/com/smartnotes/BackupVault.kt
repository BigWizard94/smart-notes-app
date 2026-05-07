package com.smartnotes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun BackupVaultButton(allNotesText: String) {
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(allNotesText.toByteArray())
            }
        }
    }

    IconButton(onClick = { fileLauncher.launch("SmartNotes_Vault_Backup.md") }) {
        Text("💾")
    }
}
