package com.example.welnesstracker.network

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
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

    private const val RADIUS_M = 8000
    private const val MAX_PLACES = 25
    private const val TAG = "WellnessPlaces"
    // Private.coffee explicitly permits use by third-party applications. A second provider
    // is attempted only on network/5xx failure; never to evade an access denial or rate limit.
    private val ENDPOINTS = listOf(
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass-api.de/api/interpreter"
    )

    fun fetch(lat: Double, lon: Double, category: PlaceCategory): List<Place> {
        val query = buildQuery(lat, lon, category)
        var lastError: IOException? = null
        for ((i, endpoint) in ENDPOINTS.withIndex()) {
            try {
                return parse(httpPost(endpoint, query), lat, lon, category)
            } catch (e: IOException) {
                Log.w(TAG, "Overpass request failed (${endpoint.substringBefore("/api/")}): ${e.message}")
                // Never switch providers to get around a provider's 4xx/429 restrictions.
                if (e is HttpError && e.status in 400..499) throw e
                lastError = e
                if (i == ENDPOINTS.lastIndex) break
            }
        }
        throw lastError ?: IOException("Overpass unavailable")
    }

    /** Public for JVM unit tests: query contains no personal data, only chosen city coordinates. */
    internal fun buildQuery(lat: Double, lon: Double, category: PlaceCategory): String {
        require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
        val around = "(around:$RADIUS_M,$lat,$lon)"
        val filters = when (category) {
            PlaceCategory.GYMS -> listOf(
                "[\"leisure\"~\"^(fitness_centre|sports_centre)$\"]",
                "[\"sport\"=\"fitness\"]",
                "[\"amenity\"=\"gym\"]"
            )
            PlaceCategory.PARKS -> listOf("[\"leisure\"=\"park\"]")
            PlaceCategory.TRACKS -> listOf(
                "[\"leisure\"=\"track\"]",
                "[\"sport\"=\"athletics\"]",
                "[\"leisure\"=\"stadium\"]"
            )
        }
        val body = filters.joinToString("") { "nwr$it$around;" }
        return "[out:json][timeout:25];($body);out center 150;"
    }

    private fun parse(text: String, centerLat: Double, centerLon: Double, category: PlaceCategory): List<Place> {
        val elements = JSONObject(text).optJSONArray("elements") ?: return emptyList()
        val found = LinkedHashMap<String, Place>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name").trim().ifEmpty {
                // An unnamed real OSM feature still has coordinates and is useful as a pin.
                // Never invent a place: the description explicitly marks the name as missing.
                when (category) {
                    PlaceCategory.GYMS -> "Fitness facility (unnamed)"
                    PlaceCategory.PARKS -> "Park (unnamed)"
                    PlaceCategory.TRACKS -> "Running track (unnamed)"
                }
            }

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
            found.putIfAbsent("$name|${String.format(Locale.US, "%.3f", lat)}|${String.format(Locale.US, "%.3f", lon)}", place)
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

    private class HttpError(val status: Int) : IOException("HTTP $status")

    /** POST avoids long URLs, sets explicit identification, and sends only an Overpass query. */
    private fun httpPost(endpoint: String, query: String): String {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 12_000
            conn.readTimeout = 35_000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("User-Agent", NetworkIdentity.USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            val body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val status = conn.responseCode
            if (status !in 200..299) throw HttpError(status)
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
