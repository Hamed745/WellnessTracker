package com.example.welnesstracker.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local SQLite database with the diary table:
 * notes(id INTEGER PK, text TEXT, mood TEXT, created_at INTEGER).
 *
 * CRUD methods: [insert] (Create), [all] / [get] (Read), [update] (Update), [delete] (Delete).
 * Pass `name = null` to get an in-memory database (used by the tests).
 */
class NotesDb(context: Context, name: String? = DEFAULT_NAME) :
    SQLiteOpenHelper(context.applicationContext, name, null, DB_VERSION) {

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

    /** READ one note by id, or null if it does not exist. */
    fun get(id: Long): Note? {
        return readableDatabase.query("notes", null, "id = ?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) {
                Note(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("text")),
                    c.getString(c.getColumnIndexOrThrow("mood")),
                    c.getLong(c.getColumnIndexOrThrow("created_at"))
                )
            } else {
                null
            }
        }
    }

    /** READ: all notes, newest first (SELECT). */
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

    /** CREATE (INSERT): returns the id of the new row. */
    fun insert(text: String, mood: String): Long {
        val values = ContentValues().apply {
            put("text", text)
            put("mood", mood)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("notes", null, values)
    }

    /** UPDATE: returns the number of changed rows (1 if the note exists). */
    fun update(id: Long, text: String, mood: String): Int {
        val values = ContentValues().apply {
            put("text", text)
            put("mood", mood)
        }
        return writableDatabase.update("notes", values, "id = ?", arrayOf(id.toString()))
    }

    /** DELETE: returns the number of removed rows (1 if the note existed). */
    fun delete(id: Long): Int {
        return writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }

    /** DELETE all notes (used by "Delete all my data"). */
    fun deleteAll(): Int = writableDatabase.delete("notes", null, null)

    companion object {
        const val DEFAULT_NAME = "wellness.db"
        private const val DB_VERSION = 1
    }
}
