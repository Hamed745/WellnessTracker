package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.data.DateUtils
import com.example.welnesstracker.databinding.FragmentStatsBinding
import com.example.welnesstracker.domain.HealthCalculator

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.state.observe(viewLifecycleOwner) { render(it) }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun render(state: UiState) {
        val p = state.profile
        val days = state.last(7) // oldest first

        b.chartSteps.setData(
            values = days.map { it.steps.toFloat() },
            labels = days.map { DateUtils.label(it.date, "EEE") },
            goal = p.stepGoal.toFloat()
        )
        b.chartSteps.contentDescription = getString(R.string.chart_description)

        val totalSteps = days.sumOf { it.steps }
        val totalKm = HealthCalculator.distanceKm(totalSteps, p.heightCm)
        val totalKcal = HealthCalculator.caloriesBurned(totalSteps, p)
        val goalDays = days.count { it.steps >= p.stepGoal }
        val best = days.maxByOrNull { it.steps }

        val avgSteps = HealthCalculator.averageOfRecorded(days.map { it.steps })
        val avgWater = HealthCalculator.averageOfRecorded(days.map { it.waterMl })
        val avgSleep = HealthCalculator.averageOfRecorded(days.map { it.sleepMin })

        b.tvAvgSteps.text = getString(R.string.stat_avg_steps, avgSteps.fmt())
        b.tvAvgWater.text = getString(R.string.stat_avg_water, avgWater.fmt())
        b.tvAvgSleep.text = getString(R.string.stat_avg_sleep, avgSleep / 60, avgSleep % 60)
        b.tvTotalDistance.text = getString(R.string.stat_total_distance, totalKm.fmt1())
        b.tvTotalCalories.text = getString(R.string.stat_total_calories, totalKcal.fmt())
        b.tvGoalDays.text = getString(R.string.stat_goal_days, goalDays, days.size)
        b.tvBestDay.text =
            if (best == null || best.steps == 0) getString(R.string.stat_best_day_none)
            else getString(R.string.stat_best_day, DateUtils.label(best.date), best.steps.fmt())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
