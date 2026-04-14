package com.smartnotes

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. THE INSTRUCTIONS (DAO): This tells the database what it is allowed to do
@Dao
interface NoteDao {
    // Grab all saved notes and sort them by the newest first
    @Query("SELECT * FROM notes_table ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    // Save a new note to the phone
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    // Delete a specific note
    @Delete
    suspend fun deleteNote(note: Note)
}

// 2. THE BUILDER: This safely constructs the actual database on the device
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "smart_notes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
