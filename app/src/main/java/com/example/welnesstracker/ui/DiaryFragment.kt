package com.example.welnesstracker.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.welnesstracker.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** A small offline CRUD diary, matching the prototype from practical work 1. */
class DiaryFragment : Fragment() {
    private lateinit var entries: LinearLayout
    private val prefs by lazy {
        requireContext().getSharedPreferences("wellness_diary", 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        entries = view.findViewById(R.id.diaryEntries)
        view.findViewById<Button>(R.id.btnDiaryAdd).setOnClickListener { editNote(-1) }
        render()
    }

    private fun notes(): JSONArray = try {
        JSONArray(prefs.getString("notes", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }

    private fun save(data: JSONArray) {
        prefs.edit().putString("notes", data.toString()).apply()
        render()
    }

    private fun render() {
        entries.removeAllViews()
        val data = notes()
        if (data.length() == 0) {
            entries.addView(TextView(requireContext()).apply {
                setText(R.string.diary_empty)
                setPadding(8, 24, 8, 24)
            })
        }
        for (i in data.length() - 1 downTo 0) {
            val note = data.optJSONObject(i) ?: continue
            val index = i
            val mood = moodLabels()[note.optInt("mood", 1).coerceIn(0, 2)]
            val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(note.optLong("createdAt")))
            entries.addView(TextView(requireContext()).apply {
                text = "$date · $mood\n${note.optString("text")}" 
                textSize = 16f
                setPadding(16, 20, 16, 20)
                setOnClickListener { editNote(index) }
            })
        }
    }

    private fun moodLabels() = arrayOf(
        getString(R.string.diary_mood_good),
        getString(R.string.diary_mood_ok),
        getString(R.string.diary_mood_bad)
    )

    private fun editNote(index: Int) {
        val data = notes()
        val old = if (index >= 0) data.optJSONObject(index) else null
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 16, 36, 8)
        }
        val spinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, moodLabels())
            setSelection(old?.optInt("mood", 1) ?: 1)
        }
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.diary_note)
            minLines = 3
            setText(old?.optString("text").orEmpty())
        }
        form.addView(TextView(requireContext()).apply { setText(R.string.diary_mood) })
        form.addView(spinner)
        form.addView(input)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (index < 0) R.string.diary_new else R.string.diary_edit)
            .setView(form)
            .setNegativeButton(R.string.diary_cancel, null)
            .setPositiveButton(R.string.save, null)
            .apply {
                if (index >= 0) setNeutralButton(R.string.diary_delete) { _, _ ->
                    val updated = JSONArray()
                    for (j in 0 until data.length()) if (j != index) updated.put(data.get(j))
                    save(updated)
                }
            }.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().trim()
                if (value.isEmpty()) {
                    input.error = getString(R.string.diary_note)
                    return@setOnClickListener
                }
                val note = JSONObject()
                    .put("text", value)
                    .put("mood", spinner.selectedItemPosition)
                    .put("createdAt", old?.optLong("createdAt") ?: System.currentTimeMillis())
                if (index < 0) data.put(note) else data.put(index, note)
                save(data)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
