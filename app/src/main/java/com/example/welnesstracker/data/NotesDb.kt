package com.example.welnesstracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local SQLite database with the diary table:
 * notes(id INTEGER PK, text TEXT, mood TEXT, created_at INTEGER).
 */
class NotesDb(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL, " +
                "mood TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS notes")
        onCreate(db)
    }

    /** SELECT: all notes, newest first. */
    fun all(): List<Note> {
        val result = ArrayList<Note>()
        readableDatabase.query("notes", null, null, null, null, null, "created_at DESC, id DESC").use { c ->
            val idCol = c.getColumnIndexOrThrow("id")
            val textCol = c.getColumnIndexOrThrow("text")
            val moodCol = c.getColumnIndexOrThrow("mood")
            val timeCol = c.getColumnIndexOrThrow("created_at")
            while (c.moveToNext()) {
                result.add(Note(c.getLong(idCol), c.getString(textCol), c.getString(moodCol), c.getLong(timeCol)))
            }
        }
        return result
    }

    /** INSERT */
    fun insert(text: String, mood: String): Long {
        val values = ContentValues().apply {
            put("text", text)
            put("mood", mood)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("notes", null, values)
    }

    /** UPDATE */
    fun update(id: Long, text: String, mood: String) {
        val values = ContentValues().apply {
            put("text", text)
            put("mood", mood)
        }
        writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
    }

    /** DELETE */
    fun delete(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    private companion object {
        const val DB_NAME = "wellness.db"
        const val DB_VERSION = 1
    }
}
