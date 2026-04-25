package com.smartnotes

import android.content.Context
import java.io.File
import java.io.FileOutputStream

class WhisperEngine(private val context: Context) {
    
    init {
        System.loadLibrary("whisper_jni")
    }

    fun initializeBrain() {
        val brainFile = File(context.filesDir, "ggml-tiny.en.bin")
        if (!brainFile.exists()) {
            context.assets.open("ggml-tiny.en.bin").use { input ->
                FileOutputStream(brainFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    external fun transcribeOffline(audioData: ByteArray): String
}
