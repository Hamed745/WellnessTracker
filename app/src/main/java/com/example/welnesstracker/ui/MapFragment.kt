package com.example.welnesstracker.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.databinding.FragmentMapBinding
import com.example.welnesstracker.databinding.ItemPlaceBinding
import com.example.welnesstracker.network.Place
import com.example.welnesstracker.network.PlaceCategory
import com.example.welnesstracker.network.PlacesService
import com.example.welnesstracker.network.PlacesState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Map screen: a real OpenStreetMap map (osmdroid) with markers for nearby gyms, parks or running
 * tracks. The places come from the open Overpass API (OpenStreetMap data); the city comes from the profile.
 * Tapping a marker (or a list row) shows the place description.
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    private var category = PlaceCategory.GYMS
    private var selected = -1
    private var center: GeoPoint? = null
    private var places: List<Place> = emptyList()
    private val markers = ArrayList<Marker>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        initOsmConfiguration(requireContext())
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            category = PlaceCategory.values()[savedInstanceState.getInt(KEY_CATEGORY, 0)]
            selected = savedInstanceState.getInt(KEY_SELECTED, -1)
        }

        b.osmMap.setTileSource(TileSourceFactory.MAPNIK)
        b.osmMap.setMultiTouchControls(true)
        b.osmMap.controller.setZoom(DEFAULT_ZOOM)
        b.osmMap.controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
        // The map sits inside a ScrollView: let it handle its own drag gestures.
        b.osmMap.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> v.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        b.chipGroup.check(chipFor(category))
        b.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            category = when (ids.firstOrNull()) {
                R.id.chipParks -> PlaceCategory.PARKS
                R.id.chipTracks -> PlaceCategory.TRACKS
                else -> PlaceCategory.GYMS
            }
            selected = -1
            vm.loadPlaces(category)
        }

        b.btnRoute.setOnClickListener {
            val place = places.getOrNull(selected) ?: return@setOnClickListener
            val uri = "geo:${place.lat},${place.lon}?q=${place.lat},${place.lon}(${Uri.encode(place.name)})"
            openMaps(Intent(Intent.ACTION_VIEW, Uri.parse(uri)), place.name)
        }
        b.btnNearby.setOnClickListener {
            val city = vm.state.value?.profile?.city.orEmpty()
            openMaps(null, (getString(queryFor(category)) + " " + city).trim())
        }

        vm.places.observe(viewLifecycleOwner) { render(it) }
        vm.loadPlaces(category)
    }

    private fun render(state: PlacesState) {
        when (state) {
            PlacesState.NoCity -> showStatus(R.string.map_no_city)
            PlacesState.Loading -> showStatus(R.string.map_loading)
            PlacesState.CityNotFound -> showStatus(R.string.map_city_not_found)
            PlacesState.Error -> showStatus(R.string.map_error)
            is PlacesState.Success -> {
                center = GeoPoint(state.center.lat, state.center.lon)
                places = state.places
                if (places.isEmpty()) {
                    b.tvMapStatus.setText(R.string.map_empty)
                } else {
                    b.tvMapStatus.text = getString(R.string.map_found, places.size)
                }
                drawPlaces()
            }
        }
        if (state !is PlacesState.Success) {
            places = emptyList()
            drawPlaces()
        }
    }

    private fun showStatus(@StringRes res: Int) {
        b.tvMapStatus.setText(res)
    }

    /** Draws a marker for every place, fills the list under the map and updates the place card. */
    private fun drawPlaces() {
        b.osmMap.overlays.clear()
        markers.clear()
        b.placesContainer.removeAllViews()

        places.forEachIndexed { index, place ->
            val marker = Marker(b.osmMap).apply {
                position = GeoPoint(place.lat, place.lon)
                title = place.name
                snippet = describe(place)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { m, _ ->
                    selectPlace(index, moveMap = false)
                    m.showInfoWindow()
                    true
                }
            }
            markers.add(marker)
            b.osmMap.overlays.add(marker)

            val row = ItemPlaceBinding.inflate(layoutInflater, b.placesContainer, false)
            row.tvItemName.text = place.name
            row.tvItemDistance.text = distanceText(distanceFromCenter(place))
            row.root.setOnClickListener { selectPlace(index, moveMap = true) }
            b.placesContainer.addView(row.root)
        }
        b.tvListTitle.visibility = if (places.isEmpty()) View.GONE else View.VISIBLE

        center?.let {
            b.osmMap.controller.setZoom(DEFAULT_ZOOM)
            b.osmMap.controller.setCenter(it)
        }
        b.osmMap.invalidate()

        if (selected in places.indices) selectPlace(selected, moveMap = false) else showCard(null)
    }

    private fun selectPlace(index: Int, moveMap: Boolean) {
        selected = index
        val place = places.getOrNull(index)
        showCard(place)
        if (place != null && moveMap) {
            b.osmMap.controller.animateTo(GeoPoint(place.lat, place.lon))
            markers.getOrNull(index)?.showInfoWindow()
        }
    }

    private fun showCard(place: Place?) {
        if (place == null) {
            b.placeCard.visibility = View.GONE
            return
        }
        b.placeCard.visibility = View.VISIBLE
        b.tvPlaceName.text = place.name
        b.tvPlaceDetails.text =
            describe(place) + "\n" + getString(R.string.place_distance, distanceText(distanceFromCenter(place)))
    }

    /** Text description of a place built from the OpenStreetMap tags that are available. */
    private fun describe(place: Place): String {
        val lines = ArrayList<String>()
        if (place.address.isNotEmpty()) lines.add(getString(R.string.place_address, place.address))
        if (place.hours.isNotEmpty()) lines.add(getString(R.string.place_hours, place.hours))
        if (place.phone.isNotEmpty()) lines.add(getString(R.string.place_phone, place.phone))
        if (place.website.isNotEmpty()) lines.add(getString(R.string.place_website, place.website))
        if (lines.isEmpty()) lines.add(getString(R.string.place_no_details))
        return lines.joinToString("\n")
    }

    private fun distanceFromCenter(place: Place): Double {
        val c = center ?: return 0.0
        return PlacesService.distanceMeters(c.latitude, c.longitude, place.lat, place.lon)
    }

    private fun distanceText(meters: Double): String =
        if (meters < 1000) getString(R.string.distance_m, meters.toInt())
        else getString(R.string.distance_km, (meters / 1000.0).fmt1())

    private fun chipFor(c: PlaceCategory) = when (c) {
        PlaceCategory.GYMS -> R.id.chipGyms
        PlaceCategory.PARKS -> R.id.chipParks
        PlaceCategory.TRACKS -> R.id.chipTracks
    }

    @StringRes
    private fun queryFor(c: PlaceCategory) = when (c) {
        PlaceCategory.GYMS -> R.string.query_gyms
        PlaceCategory.PARKS -> R.string.query_parks
        PlaceCategory.TRACKS -> R.string.query_tracks
    }

    /** Opens the maps app with [geoIntent]; with no intent it searches for [query]. Falls back to the browser. */
    private fun openMaps(geoIntent: Intent?, query: String) {
        val primary = geoIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
        try {
            startActivity(primary)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.openstreetmap.org/search?query=" + Uri.encode(query))
            )
            try {
                startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.error_no_maps_app, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.osmMap?.onResume()
    }

    override fun onPause() {
        _binding?.osmMap?.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CATEGORY, category.ordinal)
        outState.putInt(KEY_SELECTED, selected)
    }

    override fun onDestroyView() {
        _binding?.osmMap?.onDetach()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val KEY_CATEGORY = "category"
        const val KEY_SELECTED = "selected"
        const val DEFAULT_ZOOM = 13.5
        const val DEFAULT_LAT = 56.0153 // Krasnoyarsk, used until a city is known
        const val DEFAULT_LON = 92.8932

        /** osmdroid needs a user agent and a cache folder; the app cache needs no storage permission. */
        fun initOsmConfiguration(context: Context) {
            val app = context.applicationContext
            val config = Configuration.getInstance()
            config.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            config.userAgentValue = app.packageName
            val base = File(app.cacheDir, "osmdroid")
            config.osmdroidBasePath = base
            config.osmdroidTileCache = File(base, "tiles")
        }
    }
}
