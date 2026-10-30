package com.example.welnesstracker.domain

import com.example.welnesstracker.data.ActivityLevel
import com.example.welnesstracker.data.Sex
import com.example.welnesstracker.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthCalculatorTest {

    private val male = UserProfile(
        age = 30, sex = Sex.MALE, heightCm = 180, weightKg = 80.0, activity = ActivityLevel.MODERATE
    )
    private val female = male.copy(sex = Sex.FEMALE)

    @Test
    fun bmi_isWeightOverHeightSquared() {
        assertEquals(22.857, HealthCalculator.bmi(70.0, 175), 0.001)
    }

    @Test
    fun bmi_zeroHeightIsSafe() {
        assertEquals(0.0, HealthCalculator.bmi(70.0, 0), 0.0)
    }

    @Test
    fun bmiCategory_boundaries() {
        assertEquals(BmiCategory.UNDERWEIGHT, HealthCalculator.bmiCategory(18.4))
        assertEquals(BmiCategory.NORMAL, HealthCalculator.bmiCategory(18.5))
        assertEquals(BmiCategory.OVERWEIGHT, HealthCalculator.bmiCategory(25.0))
        assertEquals(BmiCategory.OBESE, HealthCalculator.bmiCategory(30.0))
    }

    @Test
    fun bmr_mifflinStJeor() {
        assertEquals(1780.0, HealthCalculator.bmr(male), 0.001)
        assertEquals(1614.0, HealthCalculator.bmr(female), 0.001)
    }

    @Test
    fun dailyCalorieNeed_appliesActivityFactor() {
        // 1780 * 1.55 = 2759
        assertEquals(2759, HealthCalculator.dailyCalorieNeed(male))
    }

    @Test
    fun distance_usesStrideFromHeight() {
        // stride = 175 * 0.415 = 72.625 cm -> 10 000 steps = 7.2625 km
        assertEquals(7.2625, HealthCalculator.distanceKm(10_000, 175), 0.0001)
    }

    @Test
    fun caloriesBurned_scalesWithDistanceAndWeight() {
        // 180 cm: stride 74.7 cm -> 10 000 steps = 7.47 km; 7.47 * 80 * 0.5 = 298.8
        assertEquals(299, HealthCalculator.caloriesBurned(10_000, male))
        assertEquals(0, HealthCalculator.caloriesBurned(0, male))
    }

    @Test
    fun waterGoal_is35mlPerKgRoundedTo50() {
        assertEquals(2450, HealthCalculator.waterGoalMl(male.copy(weightKg = 70.0)))
        assertEquals(2800, HealthCalculator.waterGoalMl(male))
    }

    @Test
    fun progress_isClampedToZeroOne() {
        assertEquals(0.5f, HealthCalculator.progress(50, 100), 0.0001f)
        assertEquals(1f, HealthCalculator.progress(500, 100), 0f)
        assertEquals(0f, HealthCalculator.progress(10, 0), 0f)
    }

    @Test
    fun wellnessScore_isAverageOfThreeGoals() {
        assertEquals(100, HealthCalculator.wellnessScore(8000, 8000, 2000, 2000, 480, 480))
        assertEquals(50, HealthCalculator.wellnessScore(4000, 8000, 1000, 2000, 240, 480))
        assertEquals(0, HealthCalculator.wellnessScore(0, 8000, 0, 2000, 0, 480))
    }

    @Test
    fun averageOfRecorded_ignoresEmptyDays() {
        assertEquals(6000, HealthCalculator.averageOfRecorded(listOf(0, 4000, 8000, 0)))
        assertEquals(0, HealthCalculator.averageOfRecorded(listOf(0, 0)))
    }
}
