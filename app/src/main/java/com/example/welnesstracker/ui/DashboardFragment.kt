package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.databinding.FragmentDashboardBinding
import com.example.welnesstracker.domain.HealthCalculator
import com.example.welnesstracker.network.WeatherState

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnAddSteps.setOnClickListener {
            Dialogs.askNumber(requireContext(), R.string.dialog_add_steps, R.string.hint_steps) {
                vm.addSteps(it.toInt())
            }
        }
        b.btnWater250.setOnClickListener { vm.addWater(250) }
        b.btnWater500.setOnClickListener { vm.addWater(500) }
        b.btnSleep.setOnClickListener {
            Dialogs.askNumber(requireContext(), R.string.dialog_sleep, R.string.hint_sleep_hours, decimal = true) {
                vm.setSleepMinutes((it * 60).toInt())
            }
        }

        vm.state.observe(viewLifecycleOwner) { render(it) }
        vm.weather.observe(viewLifecycleOwner) { renderWeather(it) }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun render(state: UiState) {
        val p = state.profile
        val today = state.today

        val steps = today.steps
        val distance = HealthCalculator.distanceKm(steps, p.heightCm)
        val burned = HealthCalculator.caloriesBurned(steps, p)
        val waterGoal = HealthCalculator.waterGoalMl(p)
        val sleepGoal = HealthCalculator.SLEEP_GOAL_MIN

        b.tvGreeting.text =
            if (p.name.isBlank()) getString(R.string.greeting_anonymous) else getString(R.string.greeting, p.name)

        val score = HealthCalculator.wellnessScore(steps, p.stepGoal, today.waterMl, waterGoal, today.sleepMin, sleepGoal)
        b.tvScore.text = getString(R.string.wellness_score, score)

        b.tvSteps.text = steps.fmt()
        b.tvStepsGoal.text = getString(R.string.steps_goal, p.stepGoal.fmt())
        b.progressSteps.setProgressCompat((HealthCalculator.progress(steps, p.stepGoal) * 100).toInt(), true)
        b.tvStepsExtra.text = getString(R.string.steps_extra, distance.fmt1(), burned.fmt())

        b.tvCaloriesBurned.text = getString(R.string.kcal_value, burned.fmt())
        b.tvCalorieNeed.text = getString(R.string.calorie_need, HealthCalculator.dailyCalorieNeed(p).fmt())

        b.tvWater.text = getString(R.string.water_progress, today.waterMl.fmt(), waterGoal.fmt())
        b.progressWater.setProgressCompat((HealthCalculator.progress(today.waterMl, waterGoal) * 100).toInt(), true)

        b.tvSleep.text = getString(R.string.duration_hm, today.sleepMin / 60, today.sleepMin % 60)
        b.progressSleep.setProgressCompat((HealthCalculator.progress(today.sleepMin, sleepGoal) * 100).toInt(), true)
        b.tvSleepComment.setText(sleepComment(today.sleepMin))
    }

    @StringRes
    private fun sleepComment(minutes: Int): Int = when {
        minutes <= 0 -> R.string.sleep_none
        minutes < 6 * 60 -> R.string.sleep_poor
        minutes < 7 * 60 -> R.string.sleep_ok
        minutes <= 9 * 60 -> R.string.sleep_great
        else -> R.string.sleep_long
    }

    private fun renderWeather(state: WeatherState) {
        b.tvWeather.text = when (state) {
            WeatherState.NoCity -> getString(R.string.weather_no_city)
            WeatherState.Loading -> getString(R.string.weather_loading)
            WeatherState.CityNotFound -> getString(R.string.weather_city_not_found)
            WeatherState.Error -> getString(R.string.weather_error)
            is WeatherState.Success -> getString(
                R.string.weather_value, state.city, state.tempC, getString(weatherDescription(state.code))
            )
        }
    }

    @StringRes
    private fun weatherDescription(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1, 2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        in 51..57 -> R.string.wx_drizzle
        in 61..67, in 80..82 -> R.string.wx_rain
        in 71..77, 85, 86 -> R.string.wx_snow
        in 95..99 -> R.string.wx_storm
        else -> R.string.wx_unknown
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
