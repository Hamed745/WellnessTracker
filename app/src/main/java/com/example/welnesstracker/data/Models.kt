package com.example.welnesstracker.data

enum class Sex { MALE, FEMALE }

/** [factor] is the multiplier applied to BMR to estimate total daily energy expenditure. */
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725)
}

data class UserProfile(
    val name: String = "",
    val age: Int = 25,
    val sex: Sex = Sex.MALE,
    val heightCm: Int = 175,
    val weightKg: Double = 70.0,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val stepGoal: Int = 8000,
    val city: String = ""
)

/** Everything recorded for one calendar day. [date] is a key in yyyy-MM-dd format. */
data class DayEntry(
    val date: String,
    val sensorSteps: Int = 0,
    val manualSteps: Int = 0,
    val waterMl: Int = 0,
    val sleepMin: Int = 0
) {
    val steps: Int get() = sensorSteps + manualSteps
}
