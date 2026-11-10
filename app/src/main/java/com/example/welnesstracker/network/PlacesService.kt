package com.example.welnesstracker.network

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class PlaceCategory { GYMS, PARKS, TRACKS }

/** A place found in OpenStreetMap data. Empty strings mean "no information". */
data class Place(
    val name: String,
    val lat: Double,
    val lon: Double,
    val address: String,
    val hours: String,
    val phone: String,
    val website: String
)

sealed class PlacesState {
    object NoCity : PlacesState()
    object Loading : PlacesState()
    object CityNotFound : PlacesState()
    object Error : PlacesState()
    data class Success(val center: Coordinates, val places: List<Place>) : PlacesState()
}

/**
 * Finds sports places near a point with the open Overpass API (OpenStreetMap data, no API key).
 * Must be called off the main thread; throws [IOException] on network errors.
 */
object PlacesService {

    private const val RADIUS_M = 3000
    private const val MAX_PLACES = 15

    fun fetch(lat: Double, lon: Double, category: PlaceCategory): List<Place> {
        val around = "(around:$RADIUS_M,$lat,$lon)"
        val filters = when (category) {
            PlaceCategory.GYMS -> listOf("[\"leisure\"~\"^(fitness_centre|sports_centre)$\"][\"name\"]")
            PlaceCategory.PARKS -> listOf("[\"leisure\"=\"park\"][\"name\"]")
            PlaceCategory.TRACKS -> listOf(
                "[\"leisure\"=\"track\"][\"name\"]",
                "[\"sport\"=\"athletics\"][\"name\"]",
                "[\"leisure\"=\"stadium\"][\"name\"]"
            )
        }
        val body = filters.joinToString("") { "nwr$it$around;" }
        val query = "[out:json][timeout:20];($body);out center 60;"
        val url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8")
        return parse(httpGet(url), lat, lon)
    }

    private fun parse(text: String, centerLat: Double, centerLon: Double): List<Place> {
        val elements = JSONObject(text).optJSONArray("elements") ?: return emptyList()
        val found = LinkedHashMap<String, Place>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name").trim()
            if (name.isEmpty()) continue

            val lat: Double
            val lon: Double
            if (el.has("lat") && el.has("lon")) {
                lat = el.getDouble("lat")
                lon = el.getDouble("lon")
            } else {
                val c = el.optJSONObject("center") ?: continue
                lat = c.getDouble("lat")
                lon = c.getDouble("lon")
            }

            val address = listOf(tags.optString("addr:street"), tags.optString("addr:housenumber"))
                .filter { it.isNotBlank() }.joinToString(" ")
            val place = Place(
                name = name,
                lat = lat,
                lon = lon,
                address = address,
                hours = tags.optString("opening_hours"),
                phone = tags.optString("phone", tags.optString("contact:phone")),
                website = tags.optString("website", tags.optString("contact:website"))
            )
            found.putIfAbsent("$name|${"%.3f".format(lat)}|${"%.3f".format(lon)}", place)
        }
        return found.values
            .sortedBy { distanceMeters(centerLat, centerLon, it.lat, it.lon) }
            .take(MAX_PLACES)
    }

    /** Great-circle distance between two points in metres (haversine formula). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = Math.toRadians(lat2 - lat1)
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "WellnessTracker/1.5 (student project)")
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
