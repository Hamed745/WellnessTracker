package com.example.welnesstracker.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.welnesstracker.data.DateUtils
import com.example.welnesstracker.data.DayEntry
import com.example.welnesstracker.data.Note
import com.example.welnesstracker.data.NotesDb
import com.example.welnesstracker.data.UserProfile
import com.example.welnesstracker.data.WellnessRepository
import com.example.welnesstracker.network.PlaceCategory
import com.example.welnesstracker.network.PlacesService
import com.example.welnesstracker.network.PlacesState
import com.example.welnesstracker.network.WeatherService
import com.example.welnesstracker.network.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Immutable snapshot of everything the screens need. */
data class UiState(val profile: UserProfile, val entries: Map<String, DayEntry>) {
    val today: DayEntry
        get() {
            val key = DateUtils.today()
            return entries[key] ?: DayEntry(key)
        }

    /** The last [n] days ending today, oldest first; days without data are empty entries. */
    fun last(n: Int): List<DayEntry> =
        DateUtils.lastDays(n).map { entries[it] ?: DayEntry(it) }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WellnessRepository(app)
    private var profile: UserProfile = repo.loadProfile() ?: UserProfile()

    private val _state = MutableLiveData(UiState(profile, repo.loadEntries()))
    val state: LiveData<UiState> = _state

    // The step service writes entries in the background; refresh the UI when that happens.
    // Kept in a field because SharedPreferences only holds listeners weakly.
    private val entriesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == WellnessRepository.KEY_ENTRIES) publish()
    }

    init {
        repo.registerChangeListener(entriesListener)
    }

    override fun onCleared() {
        repo.unregisterChangeListener(entriesListener)
        super.onCleared()
    }

    private val notesDb = NotesDb(app)
    private val _notes = MutableLiveData(notesDb.all())
    val notes: LiveData<List<Note>> = _notes

    private val _weather = MutableLiveData<WeatherState>(WeatherState.NoCity)
    val weather: LiveData<WeatherState> = _weather

    private val _places = MutableLiveData<PlacesState>(PlacesState.NoCity)
    val places: LiveData<PlacesState> = _places
    private var placesKey: String? = null
    private var placesLoadedAt = 0L
    private var placesJob: Job? = null

    private var weatherCity: String? = null
    private var weatherLoadedAt = 0L
    private var weatherJob: Job? = null

    val hasProfile: Boolean get() = repo.hasProfile()

    /** Re-publishes state (the date may have rolled over) and refreshes stale weather. */
    fun refresh() {
        publish()
        loadWeather(force = false)
    }

    fun saveProfile(newProfile: UserProfile) {
        profile = newProfile
        repo.saveProfile(newProfile)
        publish()
        loadWeather(force = false)
    }

    fun addSteps(count: Int) {
        if (count <= 0) return
        updateToday { it.copy(manualSteps = it.manualSteps + count) }
    }

    fun addWater(ml: Int) {
        if (ml <= 0) return
        updateToday { it.copy(waterMl = it.waterMl + ml) }
    }

    fun setSleepMinutes(minutes: Int) {
        updateToday { it.copy(sleepMin = minutes.coerceIn(0, 24 * 60)) }
    }

    /** Deletes everything the app stored on the phone: profile, daily data and diary notes. */
    fun deleteAllData() {
        repo.clearAll()
        notesDb.deleteAll()
        profile = UserProfile()
        placesJob?.cancel()
        weatherJob?.cancel()
        placesKey = null
        weatherCity = null
        _places.value = PlacesState.NoCity
        _weather.value = WeatherState.NoCity
        _notes.value = notesDb.all()
        publish()
    }

    fun noteById(id: Long): Note? = _notes.value?.firstOrNull { it.id == id }

    /** Creates a note when [id] is negative, otherwise updates the existing one. */
    fun saveNote(id: Long, text: String, mood: String) {
        if (id < 0) notesDb.insert(text, mood) else notesDb.update(id, text, mood)
        _notes.value = notesDb.all()
    }

    fun deleteNote(id: Long) {
        notesDb.delete(id)
        _notes.value = notesDb.all()
    }

    /** Finds sports places near the profile city (geocoding + Overpass API). */
    fun loadPlaces(category: PlaceCategory, force: Boolean = false) {
        val city = profile.city.trim()
        if (city.isEmpty()) {
            placesJob?.cancel()
            placesKey = null
            _places.value = PlacesState.NoCity
            return
        }
        val key = city + "|" + category.name
        val current = _places.value
        val fresh = System.currentTimeMillis() - placesLoadedAt < PLACES_TTL_MS
        if (!force && key == placesKey && (current is PlacesState.Loading ||
                (current is PlacesState.Success && fresh))
        ) return

        placesKey = key
        _places.value = PlacesState.Loading
        placesJob?.cancel()
        placesJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val center = WeatherService.geocode(city)
                    if (center == null) {
                        PlacesState.CityNotFound
                    } else {
                        PlacesState.Success(center, PlacesService.fetch(center.lat, center.lon, category))
                    }
                } catch (e: Exception) {
                    PlacesState.Error
                }
            }
            placesLoadedAt = System.currentTimeMillis()
            _places.value = result
        }
    }

    fun loadWeather(force: Boolean) {
        val city = profile.city.trim()
        if (city.isEmpty()) {
            weatherJob?.cancel()
            weatherCity = null
            _weather.value = WeatherState.NoCity
            return
        }
        val fresh = System.currentTimeMillis() - weatherLoadedAt < WEATHER_TTL_MS
        val current = _weather.value
        if (!force && city == weatherCity && (current is WeatherState.Loading ||
                (current is WeatherState.Success && fresh))
        ) return

        weatherCity = city
        _weather.value = WeatherState.Loading
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { WeatherService.fetch(city) }
            weatherLoadedAt = System.currentTimeMillis()
            _weather.value = result
        }
    }

    private fun updateToday(transform: (DayEntry) -> DayEntry) {
        repo.updateToday(transform)
        publish()
    }

    private fun publish() {
        _state.value = UiState(profile, repo.loadEntries())
    }

    private companion object {
        const val WEATHER_TTL_MS = 30 * 60 * 1000L
        const val PLACES_TTL_MS = 10 * 60 * 1000L
    }
}
