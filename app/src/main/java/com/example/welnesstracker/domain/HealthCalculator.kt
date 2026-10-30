package com.example.welnesstracker.domain

import com.example.welnesstracker.data.Sex
import com.example.welnesstracker.data.UserProfile
import kotlin.math.roundToInt

enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE }

/** All health formulas live here as pure functions so they are easy to unit-test. */
object HealthCalculator {

    const val SLEEP_GOAL_MIN = 8 * 60

    /** Body mass index = kg / m². */
    fun bmi(weightKg: Double, heightCm: Int): Double {
        if (heightCm <= 0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiCategory(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    /** Basal metabolic rate (Mifflin–St Jeor), kcal/day. */
    fun bmr(p: UserProfile): Double {
        val base = 10.0 * p.weightKg + 6.25 * p.heightCm - 5.0 * p.age
        return if (p.sex == Sex.MALE) base + 5.0 else base - 161.0
    }

    /** Estimated total daily energy need, kcal/day. */
    fun dailyCalorieNeed(p: UserProfile): Int = (bmr(p) * p.activity.factor).roundToInt()

    /** Average walking stride length in metres (≈ 0.415 × height). */
    fun strideMeters(heightCm: Int): Double = heightCm * 0.415 / 100.0

    fun distanceKm(steps: Int, heightCm: Int): Double = steps * strideMeters(heightCm) / 1000.0

    /** Calories burned by walking ≈ 0.5 kcal per kg per km. */
    fun caloriesBurned(steps: Int, p: UserProfile): Int =
        (distanceKm(steps, p.heightCm) * p.weightKg * 0.5).roundToInt()

    /** Recommended daily water intake: 35 ml per kg, rounded to 50 ml. */
    fun waterGoalMl(p: UserProfile): Int = ((p.weightKg * 35.0 / 50.0).roundToInt() * 50)

    /** Progress in the range 0..1. */
    fun progress(value: Int, goal: Int): Float =
        if (goal <= 0) 0f else (value.toFloat() / goal).coerceIn(0f, 1f)

    /** Overall daily score 0..100: the average of steps, water and sleep progress. */
    fun wellnessScore(
        steps: Int, stepGoal: Int,
        waterMl: Int, waterGoal: Int,
        sleepMin: Int, sleepGoal: Int
    ): Int {
        val avg = (progress(steps, stepGoal) + progress(waterMl, waterGoal) + progress(sleepMin, sleepGoal)) / 3f
        return (avg * 100f).roundToInt()
    }

    /** Average of the positive values only (days with no data are ignored); 0 if none. */
    fun averageOfRecorded(values: List<Int>): Int {
        val recorded = values.filter { it > 0 }
        return if (recorded.isEmpty()) 0 else (recorded.sum().toDouble() / recorded.size).roundToInt()
    }
}
