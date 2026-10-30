package com.example.welnesstracker.ui

import java.text.NumberFormat
import java.util.Locale

fun Int.fmt(): String = NumberFormat.getIntegerInstance().format(this)

fun Double.fmt1(): String = String.format(Locale.getDefault(), "%.1f", this)
