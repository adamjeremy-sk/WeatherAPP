package com.weatherapp.libre

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.solar.Observer
import com.weatherapp.libre.solar.SolarLightingEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class InlineSubTab {
    WEATHER_DETAILS,
    GOLDEN_HOUR
}

class AstronomyDisplayManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences
) {

    var activeInlineSubTab = InlineSubTab.WEATHER_DETAILS
    var inlineGoldenDate: LocalDate = LocalDate.now()
    lateinit var inlineSolarPhaseAdapter: SolarPhaseAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var isVolumeKeyHeld = false
    private var volumeKeyDirection = 1

    private val rapidCycleRunnable = object : Runnable {
        override fun run() {
            if (isVolumeKeyHeld) {
                inlineGoldenDate = if (volumeKeyDirection > 0) inlineGoldenDate.plusDays(1) else inlineGoldenDate.minusDays(1)
                updateInlineGoldenHourDisplay(currentLocationItem)
                val speedIdx = locationPreferences.getVolumeScrollSpeedIndex()
                val delayMs = when (speedIdx) {
                    1 -> 250L
                    2 -> 200L
                    3 -> 160L
                    4 -> 120L
                    5 -> 80L
                    6 -> 50L
                    7 -> 30L
                    else -> 120L
                }
                handler.postDelayed(this, delayMs)
            }
        }
    }

    private var currentLocationItem: LocationItem = LocationItem(0L, "Ottawa", "Ontario", "Canada", 45.4215, -75.6972)
    private var locationTimezoneStr: String? = null

    fun buildObserver(location: LocationItem): Observer {
        return Observer(
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude,
            elevationMeters = location.elevationMeters
        )
    }

    fun resolveZoneId(timezoneStr: String?): ZoneId {
        return try {
            if (!timezoneStr.isNullOrEmpty()) ZoneId.of(timezoneStr) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }
    }

    fun formatInstant(instant: Instant?, zoneId: ZoneId, is24Hour: Boolean = false): String {
        if (instant == null) return "——"
        val zdt = instant.atZone(zoneId)
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return zdt.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    fun setLocation(location: LocationItem, timezone: String?) {
        currentLocationItem = location
        locationTimezoneStr = timezone
    }

    fun switchInlineSubTab(subTab: InlineSubTab, location: LocationItem, timezone: String?) {
        activeInlineSubTab = subTab
        currentLocationItem = location
        locationTimezoneStr = timezone

        activity.findViewById<android.view.View>(R.id.layoutWeatherDetailsContainer)?.visibility = if (subTab == InlineSubTab.WEATHER_DETAILS) View.VISIBLE else View.GONE
        activity.findViewById<android.view.View>(R.id.layoutHeroSection)?.visibility = if (subTab == InlineSubTab.WEATHER_DETAILS) View.VISIBLE else View.GONE
        activity.findViewById<android.view.View>(R.id.layoutInlineGoldenHourContainer)?.visibility = if (subTab == InlineSubTab.GOLDEN_HOUR) View.VISIBLE else View.GONE

        when (subTab) {
            InlineSubTab.WEATHER_DETAILS -> {
                activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.text = "Weather ▾"
                activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.GOLDEN_HOUR -> {
                activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.text = "Golden Hour ▾"
                activity.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.setIconResource(R.drawable.ic_golden_hour)
                updateInlineGoldenHourDisplay(location)
            }
        }
    }

    fun updateInlineGoldenHourDisplay(location: LocationItem = currentLocationItem) {
        currentLocationItem = location
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
        activity.findViewById<android.widget.TextView>(R.id.tvInlineSelectedDate)?.text = if (inlineGoldenDate.isEqual(LocalDate.now())) {
            "TODAY • ${inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())}"
        } else {
            inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())
        }

        val zoneId = resolveZoneId(locationTimezoneStr)
        val observer = buildObserver(location)

        val phases = SolarLightingEngine.calculateChronologicalSolarPhases(inlineGoldenDate, zoneId, observer)
        inlineSolarPhaseAdapter.submitList(phases)
    }

    fun promptSetSolarAlarm(item: SolarPhaseDetail) {
        if (activity.isFinishing || activity.isDestroyed) return

        val leadMins = locationPreferences.getSolarAlarmLeadMinutes()
        val startInstant = item.startInstant

        if (startInstant == null) {
            Toast.makeText(activity.applicationContext, "Cannot set alarm for continuous event", Toast.LENGTH_SHORT).show()
            return
        }

        val zoneId = resolveZoneId(locationTimezoneStr)
        val alarmInstant = startInstant.minusSeconds(leadMins.toLong() * 60L)
        val alarmZdt = ZonedDateTime.ofInstant(alarmInstant, zoneId)
        val phaseZdt = ZonedDateTime.ofInstant(startInstant, zoneId)

        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val alarmFormatted = alarmZdt.format(formatter)
        val phaseFormatted = phaseZdt.format(formatter)

        MaterialAlertDialogBuilder(activity)
            .setTitle("Set Solar Alarm")
            .setMessage("Set a system alarm for $leadMins minutes before ${item.name} ($phaseFormatted)?\n\nAlarm time: $alarmFormatted")
            .setPositiveButton("Set Alarm") { dialog, _ ->
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, alarmZdt.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, alarmZdt.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "${item.name} (${currentLocationItem.displayName})")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity.applicationContext, "Alarm set for $alarmFormatted", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (activeInlineSubTab == InlineSubTab.GOLDEN_HOUR && locationPreferences.isVolumeScrollEnabled()) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (!isVolumeKeyHeld) {
                        isVolumeKeyHeld = true
                        volumeKeyDirection = 1
                        inlineGoldenDate = inlineGoldenDate.plusDays(1)
                        updateInlineGoldenHourDisplay()

                        val speedIdx = locationPreferences.getVolumeScrollSpeedIndex()
                        val delayMs = when (speedIdx) {
                            1 -> 250L
                            2 -> 200L
                            3 -> 160L
                            4 -> 120L
                            5 -> 80L
                            6 -> 50L
                            7 -> 30L
                            else -> 120L
                        }
                        handler.postDelayed(rapidCycleRunnable, delayMs)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (!isVolumeKeyHeld) {
                        isVolumeKeyHeld = true
                        volumeKeyDirection = -1
                        inlineGoldenDate = inlineGoldenDate.minusDays(1)
                        updateInlineGoldenHourDisplay()

                        val speedIdx = locationPreferences.getVolumeScrollSpeedIndex()
                        val delayMs = when (speedIdx) {
                            1 -> 250L
                            2 -> 200L
                            3 -> 160L
                            4 -> 120L
                            5 -> 80L
                            6 -> 50L
                            7 -> 30L
                            else -> 120L
                        }
                        handler.postDelayed(rapidCycleRunnable, delayMs)
                    }
                    return true
                }
            }
        }
        return false
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeKeyHeld = false
            handler.removeCallbacks(rapidCycleRunnable)
            return true
        }
        return false
    }

    fun cleanup() {
        isVolumeKeyHeld = false
        handler.removeCallbacksAndMessages(null)
    }
}
