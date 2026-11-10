package com.example.welnesstracker.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the CRUD methods of the notes table on a device (uses an in-memory database). */
@RunWith(AndroidJUnit4::class)
class NotesDbTest {

    private lateinit var db: NotesDb

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = NotesDb(context, null) // in-memory: does not touch the real diary
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun create_thenRead() {
        val id = db.insert("Morning run", Mood.HAPPY.name)
        val note = db.get(id)
        assertNotNull(note)
        assertEquals("Morning run", note!!.text)
        assertEquals(Mood.HAPPY.name, note.mood)
        assertEquals(1, db.all().size)
    }

    @Test
    fun update_changesTextAndMood() {
        val id = db.insert("Before", Mood.NEUTRAL.name)
        assertEquals(1, db.update(id, "After", Mood.SAD.name))
        val note = db.get(id)!!
        assertEquals("After", note.text)
        assertEquals(Mood.SAD.name, note.mood)
    }

    @Test
    fun delete_removesNote() {
        val id = db.insert("Temporary", Mood.HAPPY.name)
        assertEquals(1, db.delete(id))
        assertNull(db.get(id))
        assertEquals(0, db.all().size)
    }

    @Test
    fun update_andDelete_ofMissingNote_changeNothing() {
        assertEquals(0, db.update(12345L, "x", Mood.HAPPY.name))
        assertEquals(0, db.delete(12345L))
    }

    @Test
    fun readAll_returnsNewestFirst() {
        val first = db.insert("first", Mood.HAPPY.name)
        val second = db.insert("second", Mood.HAPPY.name)
        assertEquals(listOf(second, first), db.all().map { it.id })
    }
}
