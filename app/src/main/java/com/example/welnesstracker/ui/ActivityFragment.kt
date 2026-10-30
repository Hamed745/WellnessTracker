package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.data.DateUtils
import com.example.welnesstracker.databinding.FragmentActivityBinding
import com.example.welnesstracker.databinding.ItemDayBinding
import com.example.welnesstracker.domain.HealthCalculator

class ActivityFragment : Fragment() {

    private var _binding: FragmentActivityBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnAddSteps.setOnClickListener {
            Dialogs.askNumber(requireContext(), R.string.dialog_add_steps, R.string.hint_steps) {
                vm.addSteps(it.toInt())
            }
        }
        vm.state.observe(viewLifecycleOwner) { render(it) }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun render(state: UiState) {
        val p = state.profile
        val days = state.last(7).reversed() // newest first
        b.tvEmpty.visibility = if (days.all { it.steps == 0 }) View.VISIBLE else View.GONE

        b.daysContainer.removeAllViews()
        for (day in days) {
            val row = ItemDayBinding.inflate(layoutInflater, b.daysContainer, false)
            val distance = HealthCalculator.distanceKm(day.steps, p.heightCm)
            val burned = HealthCalculator.caloriesBurned(day.steps, p)
            row.tvDayName.text =
                if (day.date == DateUtils.today()) getString(R.string.today) else DateUtils.label(day.date)
            row.tvDayInfo.text = getString(R.string.day_info, day.steps.fmt(), distance.fmt1(), burned.fmt())
            row.progressDay.setProgressCompat((HealthCalculator.progress(day.steps, p.stepGoal) * 100).toInt(), false)
            b.daysContainer.addView(row.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
