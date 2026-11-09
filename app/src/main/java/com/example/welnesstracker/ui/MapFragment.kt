package com.example.welnesstracker.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.databinding.FragmentMapBinding

/**
 * Map screen from the prototype: category chips, a static map mockup with pins and a place card.
 * "Show route" and "Search nearby" open the installed maps app.
 */
class MapFragment : Fragment() {

    private enum class Category(@StringRes val query: Int) {
        GYMS(R.string.query_gyms),
        PARKS(R.string.query_parks),
        TRACKS(R.string.query_tracks)
    }

    /** Static example data: [x] and [y] are pin positions as fractions of the map size. */
    private class Place(@StringRes val name: Int, val meters: Int, val x: Float, val y: Float)

    private val places = mapOf(
        Category.GYMS to listOf(
            Place(R.string.place_gym_1, 450, 0.34f, 0.52f),
            Place(R.string.place_gym_2, 1200, 0.62f, 0.32f),
            Place(R.string.place_gym_3, 2100, 0.74f, 0.70f)
        ),
        Category.PARKS to listOf(
            Place(R.string.place_park_1, 900, 0.30f, 0.42f),
            Place(R.string.place_park_2, 1800, 0.56f, 0.66f),
            Place(R.string.place_park_3, 2400, 0.76f, 0.30f)
        ),
        Category.TRACKS to listOf(
            Place(R.string.place_track_1, 1100, 0.40f, 0.66f),
            Place(R.string.place_track_2, 1500, 0.66f, 0.46f),
            Place(R.string.place_track_3, 2000, 0.24f, 0.30f)
        )
    )

    private var _binding: FragmentMapBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    private var category = Category.GYMS
    private var selected = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            category = Category.values()[savedInstanceState.getInt(KEY_CATEGORY, 0)]
            selected = savedInstanceState.getInt(KEY_SELECTED, -1)
        }
        b.chipGroup.check(chipFor(category))
        b.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            category = when (ids.firstOrNull()) {
                R.id.chipParks -> Category.PARKS
                R.id.chipTracks -> Category.TRACKS
                else -> Category.GYMS
            }
            selected = -1
            showCategory()
        }

        b.mapView.onPinSelected = { index ->
            selected = index
            showPlace()
        }
        b.btnRoute.setOnClickListener {
            val place = places.getValue(category).getOrNull(selected) ?: return@setOnClickListener
            openMaps(getString(place.name) + " " + city())
        }
        b.btnNearby.setOnClickListener {
            openMaps(getString(category.query) + " " + city())
        }

        showCategory()
    }

    private fun showCategory() {
        val points = places.getValue(category).map { PointF(it.x, it.y) }
        b.mapView.setPins(points, selected)
        showPlace()
    }

    private fun showPlace() {
        val place = places.getValue(category).getOrNull(selected)
        if (place == null) {
            b.placeCard.visibility = View.GONE
            return
        }
        b.placeCard.visibility = View.VISIBLE
        b.tvPlaceName.setText(place.name)
        b.tvPlaceDistance.text = getString(R.string.place_distance, distanceText(place.meters))
    }

    private fun distanceText(meters: Int): String =
        if (meters < 1000) getString(R.string.distance_m, meters)
        else getString(R.string.distance_km, (meters / 1000.0).fmt1())

    private fun chipFor(c: Category) = when (c) {
        Category.GYMS -> R.id.chipGyms
        Category.PARKS -> R.id.chipParks
        Category.TRACKS -> R.id.chipTracks
    }

    private fun city() = vm.state.value?.profile?.city.orEmpty()

    private fun openMaps(query: String) {
        val q = query.trim()
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q)))
        try {
            startActivity(geo)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(q))
            )
            try {
                startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.error_no_maps_app, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CATEGORY, category.ordinal)
        outState.putInt(KEY_SELECTED, selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val KEY_CATEGORY = "category"
        const val KEY_SELECTED = "selected"
    }
}
