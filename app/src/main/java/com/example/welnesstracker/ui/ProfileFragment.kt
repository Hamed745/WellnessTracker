package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.data.ActivityLevel
import com.example.welnesstracker.data.Sex
import com.example.welnesstracker.data.UserProfile
import com.example.welnesstracker.databinding.FragmentProfileBinding
import com.example.welnesstracker.domain.BmiCategory
import com.example.welnesstracker.domain.HealthCalculator

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    private lateinit var sexLabels: List<String>
    private lateinit var activityLabels: List<String>
    private var sexIndex = 0
    private var activityIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sexLabels = listOf(getString(R.string.sex_male), getString(R.string.sex_female))
        activityLabels = listOf(
            getString(R.string.activity_sedentary),
            getString(R.string.activity_light),
            getString(R.string.activity_moderate),
            getString(R.string.activity_active)
        )
        b.actSex.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sexLabels))
        b.actActivity.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, activityLabels))

        // Fill the form once from the saved profile.
        val p = vm.state.value?.profile ?: UserProfile()
        b.etName.setText(p.name)
        b.etAge.setText(p.age.toString())
        b.etHeight.setText(p.heightCm.toString())
        b.etWeight.setText(trimWeight(p.weightKg))
        b.etStepGoal.setText(p.stepGoal.toString())
        b.etCity.setText(p.city)
        sexIndex = savedInstanceState?.getInt(KEY_SEX) ?: p.sex.ordinal
        activityIndex = savedInstanceState?.getInt(KEY_ACTIVITY) ?: p.activity.ordinal
        b.actSex.setText(sexLabels[sexIndex], false)
        b.actActivity.setText(activityLabels[activityIndex], false)

        b.tvFirstRun.visibility = if (vm.hasProfile) View.GONE else View.VISIBLE

        // Live preview of calculated values while typing.
        val preview = { readForm(showErrors = false)?.let { showCalculations(it) } }
        b.etAge.doAfterTextChanged { preview() }
        b.etHeight.doAfterTextChanged { preview() }
        b.etWeight.doAfterTextChanged { preview() }
        b.actSex.setOnItemClickListener { _, _, position, _ -> sexIndex = position; preview() }
        b.actActivity.setOnItemClickListener { _, _, position, _ -> activityIndex = position; preview() }
        preview()

        b.btnSave.setOnClickListener {
            val profile = readForm(showErrors = true) ?: return@setOnClickListener
            vm.saveProfile(profile)
            b.tvFirstRun.visibility = View.GONE
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
        }
    }

    /** Reads and validates the form; returns null if any field is invalid. */
    private fun readForm(showErrors: Boolean): UserProfile? {
        var valid = true

        fun intField(text: String?, min: Int, max: Int, error: (String?) -> Unit): Int? {
            val v = text?.trim()?.toIntOrNull()
            return if (v != null && v in min..max) {
                error(null); v
            } else {
                valid = false
                error(if (showErrors) getString(R.string.error_range, min, max) else null)
                null
            }
        }

        val age = intField(b.etAge.text?.toString(), 10, 100) { b.tilAge.error = it }
        val height = intField(b.etHeight.text?.toString(), 100, 250) { b.tilHeight.error = it }
        val goal = intField(b.etStepGoal.text?.toString(), 1000, 50000) { b.tilStepGoal.error = it }

        val weight = b.etWeight.text?.toString()?.replace(',', '.')?.trim()?.toDoubleOrNull()
        if (weight != null && weight in 30.0..300.0) {
            b.tilWeight.error = null
        } else {
            valid = false
            b.tilWeight.error = if (showErrors) getString(R.string.error_range, 30, 300) else null
        }

        if (!valid || age == null || height == null || goal == null || weight == null) return null

        return UserProfile(
            name = b.etName.text?.toString()?.trim().orEmpty(),
            age = age,
            sex = Sex.values()[sexIndex],
            heightCm = height,
            weightKg = weight,
            activity = ActivityLevel.values()[activityIndex],
            stepGoal = goal,
            city = b.etCity.text?.toString()?.trim().orEmpty()
        )
    }

    private fun showCalculations(p: UserProfile) {
        val bmi = HealthCalculator.bmi(p.weightKg, p.heightCm)
        val category = when (HealthCalculator.bmiCategory(bmi)) {
            BmiCategory.UNDERWEIGHT -> R.string.bmi_underweight
            BmiCategory.NORMAL -> R.string.bmi_normal
            BmiCategory.OVERWEIGHT -> R.string.bmi_overweight
            BmiCategory.OBESE -> R.string.bmi_obese
        }
        b.tvBmi.text = getString(R.string.calc_bmi, bmi.fmt1(), getString(category))
        b.tvBmr.text = getString(R.string.calc_bmr, Math.round(HealthCalculator.bmr(p)).toInt().fmt())
        b.tvCalorieNeed.text = getString(R.string.calc_calorie_need, HealthCalculator.dailyCalorieNeed(p).fmt())
        b.tvWaterGoal.text = getString(R.string.calc_water_goal, HealthCalculator.waterGoalMl(p).fmt())
        b.tvStride.text = getString(
            R.string.calc_stride,
            (HealthCalculator.strideMeters(p.heightCm) * 100).toInt()
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SEX, sexIndex)
        outState.putInt(KEY_ACTIVITY, activityIndex)
    }

    private fun trimWeight(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()

    private companion object {
        const val KEY_SEX = "sex_index"
        const val KEY_ACTIVITY = "activity_index"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
