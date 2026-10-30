package com.example.welnesstracker.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/** Persists the profile and daily entries locally (SharedPreferences + JSON). */
class WellnessRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasProfile(): Boolean = prefs.contains(KEY_PROFILE)

    fun loadProfile(): UserProfile? {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return null
        return try {
            val o = JSONObject(raw)
            UserProfile(
                name = o.optString("name", ""),
                age = o.optInt("age", 25),
                sex = enumOrDefault(o.optString("sex"), Sex.MALE),
                heightCm = o.optInt("heightCm", 175),
                weightKg = o.optDouble("weightKg", 70.0),
                activity = enumOrDefault(o.optString("activity"), ActivityLevel.LIGHT),
                stepGoal = o.optInt("stepGoal", 8000),
                city = o.optString("city", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveProfile(p: UserProfile) {
        val o = JSONObject()
            .put("name", p.name)
            .put("age", p.age)
            .put("sex", p.sex.name)
            .put("heightCm", p.heightCm)
            .put("weightKg", p.weightKg)
            .put("activity", p.activity.name)
            .put("stepGoal", p.stepGoal)
            .put("city", p.city)
        prefs.edit().putString(KEY_PROFILE, o.toString()).apply()
    }

    fun loadEntries(): Map<String, DayEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = HashMap<String, DayEntry>()
            for (key in root.keys()) {
                val o = root.getJSONObject(key)
                result[key] = DayEntry(
                    date = key,
                    sensorSteps = o.optInt("sensorSteps", 0),
                    manualSteps = o.optInt("manualSteps", 0),
                    waterMl = o.optInt("waterMl", 0),
                    sleepMin = o.optInt("sleepMin", 0)
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveEntries(entries: Map<String, DayEntry>) {
        val root = JSONObject()
        // Keep roughly the last year; date keys sort chronologically as plain strings.
        entries.toSortedMap().entries.toList().takeLast(MAX_DAYS).forEach { (key, e) ->
            root.put(
                key,
                JSONObject()
                    .put("sensorSteps", e.sensorSteps)
                    .put("manualSteps", e.manualSteps)
                    .put("waterMl", e.waterMl)
                    .put("sleepMin", e.sleepMin)
            )
        }
        prefs.edit().putString(KEY_ENTRIES, root.toString()).apply()
    }

    /** Last raw value of the hardware step counter (steps since boot), or -1 if unknown. */
    var lastSensorTotal: Float
        get() = prefs.getFloat(KEY_LAST_SENSOR, -1f)
        set(value) = prefs.edit().putFloat(KEY_LAST_SENSOR, value).apply()

    /** Atomically applies [transform] to today's entry. Safe to call from the UI and the step service. */
    fun updateToday(transform: (DayEntry) -> DayEntry) {
        synchronized(LOCK) {
            val key = DateUtils.today()
            val map = loadEntries().toMutableMap()
            map[key] = transform(map[key] ?: DayEntry(key))
            saveEntries(map)
        }
    }

    fun todayEntry(): DayEntry {
        val key = DateUtils.today()
        return loadEntries()[key] ?: DayEntry(key)
    }

    /**
     * Receives the raw hardware step counter (steps since last reboot) and adds only
     * the difference since the previous reading to today's total.
     */
    fun recordSensorTotal(total: Float) {
        synchronized(LOCK) {
            val last = lastSensorTotal
            val delta = when {
                last < 0f -> 0f          // first reading ever: just remember it
                total >= last -> total - last
                else -> total            // counter was reset by a reboot
            }
            lastSensorTotal = total
            if (delta >= 1f) {
                updateToday { it.copy(sensorSteps = it.sensorSteps + delta.toInt()) }
            }
        }
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        private val LOCK = Any()
        private const val PREFS = "wellness_tracker"
        private const val KEY_PROFILE = "profile"
        const val KEY_ENTRIES = "entries"
        private const val KEY_LAST_SENSOR = "last_sensor_total"
        private const val MAX_DAYS = 400
    }
}
