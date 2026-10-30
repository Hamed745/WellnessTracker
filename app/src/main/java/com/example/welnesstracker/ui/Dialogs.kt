package com.example.welnesstracker.ui

import android.content.Context
import android.text.InputType
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.welnesstracker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object Dialogs {

    /** Asks for a positive number and calls [onValue] with it; shows an error for invalid input. */
    fun askNumber(
        context: Context,
        @StringRes title: Int,
        @StringRes hintRes: Int,
        decimal: Boolean = false,
        onValue: (Double) -> Unit
    ) {
        val input = TextInputEditText(context).apply {
            inputType = if (decimal) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_NUMBER
            }
        }
        val layout = TextInputLayout(context).apply {
            hint = context.getString(hintRes)
            addView(input)
        }
        val pad = (20 * context.resources.displayMetrics.density).toInt()
        val container = FrameLayout(context).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(
                layout,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = input.text?.toString()?.replace(',', '.')?.toDoubleOrNull()
                if (value != null && value > 0) {
                    onValue(value)
                } else {
                    Toast.makeText(context, R.string.error_invalid_number, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
