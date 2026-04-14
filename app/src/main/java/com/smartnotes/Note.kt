package com.smartnotes

// This is the foundational blueprint for our internal database
data class Note(
    val id: Long = System.currentTimeMillis(), // Generates a unique ID based on the exact millisecond it is saved
    val originalText: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
