package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.MainActivity
import com.example.welnesstracker.data.Mood
import com.example.welnesstracker.data.Note
import com.example.welnesstracker.databinding.FragmentDiaryBinding
import com.example.welnesstracker.databinding.ItemNoteBinding
import java.text.DateFormat
import java.util.Date

class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.fabAdd.setOnClickListener { openEditor(-1L) }
        vm.notes.observe(viewLifecycleOwner) { render(it) }
    }

    private fun render(notes: List<Note>) {
        b.tvEmpty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
        b.notesContainer.removeAllViews()
        val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        for (note in notes) {
            val row = ItemNoteBinding.inflate(layoutInflater, b.notesContainer, false)
            row.tvMood.text = Mood.from(note.mood).emoji
            row.tvText.text = note.text
            row.tvDate.text = dateFormat.format(Date(note.createdAt))
            row.root.setOnClickListener { openEditor(note.id) }
            b.notesContainer.addView(row.root)
        }
    }

    private fun openEditor(id: Long) {
        (requireActivity() as MainActivity).showDetail(NoteEditorFragment.newInstance(id))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
