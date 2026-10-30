package com.example.welnesstracker.ui

import android.content.ActivityNotFoundException
import android.content.Intent
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

/** Opens the user's installed maps app (or the browser) with a search for nearby fitness places. */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.etCity.setText(vm.state.value?.profile?.city.orEmpty())
        b.btnGyms.setOnClickListener { search(R.string.query_gyms) }
        b.btnParks.setOnClickListener { search(R.string.query_parks) }
        b.btnTracks.setOnClickListener { search(R.string.query_tracks) }
    }

    private fun search(@StringRes queryRes: Int) {
        val city = b.etCity.text?.toString()?.trim().orEmpty()
        val query = (getString(queryRes) + " " + city).trim()

        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
        try {
            startActivity(geo)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
            )
            try {
                startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), R.string.error_no_maps_app, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
