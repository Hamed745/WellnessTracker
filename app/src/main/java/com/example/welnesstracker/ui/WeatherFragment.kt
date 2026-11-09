package com.example.welnesstracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.welnesstracker.R
import com.example.welnesstracker.databinding.FragmentWeatherBinding
import com.example.welnesstracker.network.WeatherState
import java.util.Locale

/** Detail screen for the open weather API (Open-Meteo). */
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val b get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        vm.weather.observe(viewLifecycleOwner) { render(it) }
        vm.loadWeather(force = false)
    }

    private fun render(state: WeatherState) {
        b.tvResponse.text = ""
        when (state) {
            is WeatherState.Success -> {
                b.tvTemp.text = String.format(Locale.getDefault(), "%+d°C", state.tempC)
                b.tvPlace.text = getString(
                    R.string.weather_place, state.city, getString(weatherDescriptionRes(state.code))
                )
                b.tvResponse.text = getString(R.string.weather_api_response, state.tempC, state.code)
            }
            WeatherState.Loading -> showMessage(R.string.weather_loading)
            WeatherState.NoCity -> showMessage(R.string.weather_no_city)
            WeatherState.CityNotFound -> showMessage(R.string.weather_city_not_found)
            WeatherState.Error -> showMessage(R.string.weather_error)
        }
    }

    private fun showMessage(messageRes: Int) {
        b.tvTemp.text = "—"
        b.tvPlace.setText(messageRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
