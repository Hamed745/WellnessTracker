package com.example.welnesstracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.LocaleList
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.welnesstracker.data.WellnessRepository
import com.example.welnesstracker.ui.fmt

/**
 * Foreground service that keeps listening to the hardware step counter while the app is closed,
 * so every step lands on the correct day. It only uses local storage — no network.
 */
class StepCounterService : Service(), SensorEventListener {

    private lateinit var repo: WellnessRepository
    private lateinit var sensorManager: SensorManager
    private var lastNotificationUpdate = 0L

    override fun onCreate() {
        super.onCreate()
        repo = WellnessRepository(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        createChannel()

        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(repo.todayEntry().steps), type)
        } catch (e: Exception) {
            // Missing permission or the system refused a background start: give up quietly.
            stopSelf()
            return
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            stopSelf()
        } else {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent) {
        repo.recordSensorTotal(event.values[0])

        // Refresh the notification text at most every 5 seconds.
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotificationUpdate >= 5_000L) {
            lastNotificationUpdate = now
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(repo.todayEntry().steps))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Context using the language chosen with the in-app EN | RU switch. */
    private fun localized(): Context {
        val chosen = AppCompatDelegate.getApplicationLocales()
        if (chosen.isEmpty) return this
        val config = Configuration(resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(chosen.toLanguageTags()))
        return createConfigurationContext(config)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                localized().getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(steps: Int): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(localized().getString(R.string.notification_title))
            .setContentText(localized().getString(R.string.notification_text, steps.fmt()))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "step_counter"
        const val NOTIFICATION_ID = 1
    }
}
