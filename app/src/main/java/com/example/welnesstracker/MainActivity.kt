package com.example.welnesstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.welnesstracker.databinding.ActivityMainBinding
import com.example.welnesstracker.ui.ActivityFragment
import com.example.welnesstracker.ui.DashboardFragment
import com.example.welnesstracker.ui.MainViewModel
import com.example.welnesstracker.ui.MapFragment
import com.example.welnesstracker.ui.ProfileFragment
import com.example.welnesstracker.ui.StatsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var permissionsAsked = false
    private var warningShown = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startStepService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // targetSdk 35+ draws edge-to-edge, so keep content clear of the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setupLanguageSwitch()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_activity -> ActivityFragment()
                R.id.nav_stats -> StatsFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.nav_map -> MapFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            // New users start on the profile so every calculation uses their real data.
            binding.bottomNavigation.selectedItemId =
                if (viewModel.hasProfile) R.id.nav_dashboard else R.id.nav_profile
        }
    }

    /** EN | RU switch in the app bar; the activity is recreated with the chosen language. */
    private fun setupLanguageSwitch() {
        fun currentLanguage() = resources.configuration.locales[0].language

        val group = binding.languageToggle
        group.check(if (currentLanguage() == "ru") R.id.btnLangRu else R.id.btnLangEn)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val tag = if (checkedId == R.id.btnLangRu) "ru" else "en"
            if (tag != currentLanguage()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureStepCounting()
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun missingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isGranted(Manifest.permission.ACTIVITY_RECOGNITION)) {
            missing += Manifest.permission.ACTIVITY_RECOGNITION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }
        return missing
    }

    private fun ensureStepCounting() {
        val missing = missingPermissions()
        if (missing.isNotEmpty() && !permissionsAsked) {
            permissionsAsked = true
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            startStepService()
        }
    }

    /** Starts the background step counter once the activity-recognition permission is granted. */
    private fun startStepService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isGranted(Manifest.permission.ACTIVITY_RECOGNITION)) {
            warnOnce(R.string.permission_steps_denied)
            return
        }
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null) {
            warnOnce(R.string.no_step_sensor)
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, StepCounterService::class.java))
    }

    private fun warnOnce(messageRes: Int) {
        if (warningShown) return
        warningShown = true
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
    }
}
