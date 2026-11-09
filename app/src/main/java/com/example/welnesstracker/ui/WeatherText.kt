package com.example.welnesstracker.ui

import androidx.annotation.StringRes
import com.example.welnesstracker.R

/** Maps an Open-Meteo weather code to a readable description. */
@StringRes
fun weatherDescriptionRes(code: Int): Int = when (code) {
    0 -> R.string.wx_clear
    1, 2 -> R.string.wx_partly_cloudy
    3 -> R.string.wx_cloudy
    45, 48 -> R.string.wx_fog
    in 51..57 -> R.string.wx_drizzle
    in 61..67, in 80..82 -> R.string.wx_rain
    in 71..77, 85, 86 -> R.string.wx_snow
    in 95..99 -> R.string.wx_storm
    else -> R.string.wx_unknown
}
