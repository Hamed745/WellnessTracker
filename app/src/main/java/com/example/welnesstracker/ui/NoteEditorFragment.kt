package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.data.Mood
import com.example.welnesstracker.databinding.FragmentNoteEditorBinding

/** Create, edit or delete one diary note (INSERT / UPDATE / DELETE in the local database). */
class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    private var noteId = NEW_NOTE
    private var mood = Mood.HAPPY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = requireArguments().getLong(ARG_ID, NEW_NOTE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = vm.noteById(noteId)
        if (existing != null) {
            b.etNote.setText(existing.text)
            mood = Mood.from(existing.mood)
        } else {
            noteId = NEW_NOTE
        }
        savedInstanceState?.getString(KEY_MOOD)?.let { mood = Mood.from(it) }

        b.moodToggle.check(buttonFor(mood))
        b.moodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) mood = moodFor(checkedId)
        }

        b.btnDelete.visibility = if (noteId == NEW_NOTE) View.GONE else View.VISIBLE

        b.btnSave.setOnClickListener {
            val text = b.etNote.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                b.tilNote.error = getString(R.string.note_empty_error)
                return@setOnClickListener
            }
            vm.saveNote(noteId, text, mood.name)
            Toast.makeText(requireContext(), R.string.note_saved, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        b.btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        b.btnDelete.setOnClickListener {
            vm.deleteNote(noteId)
            Toast.makeText(requireContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun buttonFor(m: Mood) = when (m) {
        Mood.HAPPY -> R.id.btnMoodHappy
        Mood.NEUTRAL -> R.id.btnMoodNeutral
        Mood.SAD -> R.id.btnMoodSad
    }

    private fun moodFor(buttonId: Int) = when (buttonId) {
        R.id.btnMoodNeutral -> Mood.NEUTRAL
        R.id.btnMoodSad -> Mood.SAD
        else -> Mood.HAPPY
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_MOOD, mood.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "note_id"
        private const val KEY_MOOD = "mood"
        private const val NEW_NOTE = -1L

        fun newInstance(id: Long) = NoteEditorFragment().apply {
            arguments = bundleOf(ARG_ID to id)
        }
    }
}
