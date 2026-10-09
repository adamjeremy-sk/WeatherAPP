package com.weatherapp.libre

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.solar.Observer
import com.weatherapp.libre.solar.SolarLightingEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class InlineSubTab {
    CURRENT,
    HOURLY,
    SEVEN_DAY,
    NATURAL_LIGHT,
    DARK_SKY,
    NIGHT_VISIBILITY,
    SOLUNAR
}

class AstronomyDisplayManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences
) {

    val savedSubTabIdx = locationPreferences.getSavedInlineSubTab()
    var activeInlineSubTab = InlineSubTab.values().getOrElse(savedSubTabIdx) { InlineSubTab.CURRENT }
    var inlineGoldenDate: LocalDate = LocalDate.now()
    lateinit var inlineSolarPhaseAdapter: SolarPhaseAdapter
    var hourlyDisplayManager: HourlyDisplayManager? = null

    val darkSkyManager = DarkSkyManager()
    val nightVisibilityManager = NightVisibilityManager()
    val solunarManager = SolunarManager()

    val darkSkyAdapter = DarkSkyWindowAdapter()
    val nightVisAdapter = NightVisibilityPeriodAdapter()
    val solunarAdapter = SolunarDayAdapter()

    var currentHourlyCloudCoverMap: Map<Instant, Double>? = null
    private var calculationJob: Job? = null

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

    fun setupUI() {
        activity.findViewById<RecyclerView>(R.id.rvDarkSkyWindows)?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = darkSkyAdapter
        }
        activity.findViewById<RecyclerView>(R.id.rvNightVisPeriods)?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = nightVisAdapter
        }
        activity.findViewById<RecyclerView>(R.id.rvSolunarDays)?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = solunarAdapter
        }
    }

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
        locationPreferences.saveInlineSubTab(subTab.ordinal)

        val weatherVis = if (subTab == InlineSubTab.CURRENT) View.VISIBLE else View.GONE
        val hourlyVis = if (subTab == InlineSubTab.HOURLY) View.VISIBLE else View.GONE
        val sevenDayVis = if (subTab == InlineSubTab.SEVEN_DAY) View.VISIBLE else View.GONE
        val naturalLightVis = if (subTab == InlineSubTab.NATURAL_LIGHT) View.VISIBLE else View.GONE
        val darkSkyVis = if (subTab == InlineSubTab.DARK_SKY) View.VISIBLE else View.GONE
        val nightVisVis = if (subTab == InlineSubTab.NIGHT_VISIBILITY) View.VISIBLE else View.GONE
        val solunarVis = if (subTab == InlineSubTab.SOLUNAR) View.VISIBLE else View.GONE

        activity.findViewById<View>(R.id.layoutWeatherDetailsContainer)?.visibility = weatherVis
        activity.findViewById<View>(R.id.layoutHeroSection)?.visibility = weatherVis
        activity.findViewById<View>(R.id.layoutInlineHourlyContainer)?.visibility = hourlyVis
        activity.findViewById<View>(R.id.layoutInlineSevenDayContainer)?.visibility = sevenDayVis
        activity.findViewById<View>(R.id.layoutInlineGoldenHourContainer)?.visibility = naturalLightVis
        activity.findViewById<View>(R.id.layoutInlineDarkSkyContainer)?.visibility = darkSkyVis
        activity.findViewById<View>(R.id.layoutInlineNightVisibilityContainer)?.visibility = nightVisVis
        activity.findViewById<View>(R.id.layoutInlineSolunarContainer)?.visibility = solunarVis

        val btnMenu = activity.findViewById<MaterialButton>(R.id.btnInlineMenu)
        when (subTab) {
            InlineSubTab.CURRENT -> {
                btnMenu?.text = activity.getString(R.string.subtab_current_weather)
                btnMenu?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.HOURLY -> {
                btnMenu?.text = activity.getString(R.string.subtab_hourly_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_weather)
                hourlyDisplayManager?.rebuildHourlyList()
            }
            InlineSubTab.SEVEN_DAY -> {
                btnMenu?.text = activity.getString(R.string.subtab_seven_day)
                btnMenu?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.NATURAL_LIGHT -> {
                btnMenu?.text = activity.getString(R.string.subtab_natural_light_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_golden_hour)
                updateInlineGoldenHourDisplay(location)
            }
            InlineSubTab.DARK_SKY -> {
                btnMenu?.text = activity.getString(R.string.subtab_dark_sky_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_moon_new)
                updateDarkSkyDisplay(location)
            }
            InlineSubTab.NIGHT_VISIBILITY -> {
                btnMenu?.text = activity.getString(R.string.subtab_night_vis_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_visibility)
                updateNightVisDisplay(location, currentHourlyCloudCoverMap)
            }
            InlineSubTab.SOLUNAR -> {
                btnMenu?.text = activity.getString(R.string.subtab_solunar_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_moon_full)
                updateSolunarDisplay(location)
            }
        }
        updateSevenDayForecastVisibility(subTab)
    }

    fun applyInitialLaunchState(isFreshLaunch: Boolean = false) {
        val subTab = if (isFreshLaunch) {
            locationPreferences.getDefaultLaunchSubTab()
        } else {
            activeInlineSubTab
        }
        activeInlineSubTab = subTab

        val btnMenu = activity.findViewById<MaterialButton>(R.id.btnInlineMenu)

        when (subTab) {
            InlineSubTab.CURRENT -> {
                btnMenu?.text = activity.getString(R.string.subtab_current_weather)
                btnMenu?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.HOURLY -> {
                btnMenu?.text = activity.getString(R.string.subtab_hourly_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.SEVEN_DAY -> {
                btnMenu?.text = activity.getString(R.string.subtab_seven_day)
                btnMenu?.setIconResource(R.drawable.ic_weather)
            }
            InlineSubTab.NATURAL_LIGHT -> {
                btnMenu?.text = activity.getString(R.string.subtab_natural_light_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_golden_hour)
            }
            InlineSubTab.DARK_SKY -> {
                btnMenu?.text = activity.getString(R.string.subtab_dark_sky_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_moon_new)
            }
            InlineSubTab.NIGHT_VISIBILITY -> {
                btnMenu?.text = activity.getString(R.string.subtab_night_vis_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_visibility)
            }
            InlineSubTab.SOLUNAR -> {
                btnMenu?.text = activity.getString(R.string.subtab_solunar_dropdown)
                btnMenu?.setIconResource(R.drawable.ic_moon_full)
            }
        }

        updateSevenDayForecastVisibility(subTab)

        val weatherVis = if (subTab == InlineSubTab.CURRENT) View.VISIBLE else View.GONE
        val hourlyVis = if (subTab == InlineSubTab.HOURLY) View.VISIBLE else View.GONE
        val sevenDayVis = if (subTab == InlineSubTab.SEVEN_DAY) View.VISIBLE else View.GONE
        val naturalLightVis = if (subTab == InlineSubTab.NATURAL_LIGHT) View.VISIBLE else View.GONE
        val darkSkyVis = if (subTab == InlineSubTab.DARK_SKY) View.VISIBLE else View.GONE
        val nightVisVis = if (subTab == InlineSubTab.NIGHT_VISIBILITY) View.VISIBLE else View.GONE
        val solunarVis = if (subTab == InlineSubTab.SOLUNAR) View.VISIBLE else View.GONE

        activity.findViewById<View>(R.id.layoutWeatherDetailsContainer)?.visibility = weatherVis
        activity.findViewById<View>(R.id.layoutHeroSection)?.visibility = weatherVis
        activity.findViewById<View>(R.id.layoutInlineHourlyContainer)?.visibility = hourlyVis
        activity.findViewById<View>(R.id.layoutInlineSevenDayContainer)?.visibility = sevenDayVis
        activity.findViewById<View>(R.id.layoutInlineGoldenHourContainer)?.visibility = naturalLightVis
        activity.findViewById<View>(R.id.layoutInlineDarkSkyContainer)?.visibility = darkSkyVis
        activity.findViewById<View>(R.id.layoutInlineNightVisibilityContainer)?.visibility = nightVisVis
        activity.findViewById<View>(R.id.layoutInlineSolunarContainer)?.visibility = solunarVis
    }

    fun updateSevenDayForecastVisibility(subTab: InlineSubTab = activeInlineSubTab) {
        val isInlineMode = !locationPreferences.getSevenDayAsMenuItem()
        val isVisible = isInlineMode && (subTab == InlineSubTab.CURRENT || subTab == InlineSubTab.HOURLY)
        val vis = if (isVisible) View.VISIBLE else View.GONE
        activity.findViewById<View>(R.id.tvForecastHeaderTitle)?.visibility = vis
        activity.findViewById<RecyclerView>(R.id.rvForecast)?.visibility = vis
    }

    fun updateDarkSkyDisplay(location: LocationItem = currentLocationItem) {
        currentLocationItem = location
        val zoneId = resolveZoneId(locationTimezoneStr)
        val currentDate = LocalDate.now(zoneId)

        val dates = (0..6).map { currentDate.plusDays(it.toLong()) }

        calculationJob?.cancel()
        calculationJob = activity.lifecycleScope.launch {
            val summaries = darkSkyManager.calculateDarkSkyWindows(dates, zoneId, location)
            val allWindows = summaries.flatMap { it.windows }

            darkSkyAdapter.submitList(allWindows, isMultiNight = true, zoneId = zoneId)

            val emptyTv = activity.findViewById<android.widget.TextView>(R.id.tvDarkSkyEmptyState)
            val rv = activity.findViewById<RecyclerView>(R.id.rvDarkSkyWindows)

            if (allWindows.isEmpty()) {
                val noteText = summaries.firstOrNull { !it.note.isNullOrEmpty() }?.note
                    ?: "No true astronomical darkness available for the selected night."
                emptyTv?.text = noteText
                emptyTv?.visibility = View.VISIBLE
                rv?.visibility = View.GONE
            } else {
                emptyTv?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
            }
        }
    }

    fun updateNightVisDisplay(location: LocationItem = currentLocationItem, hourlyCloudCoverMap: Map<Instant, Double>? = currentHourlyCloudCoverMap) {
        currentLocationItem = location
        if (hourlyCloudCoverMap != null) {
            currentHourlyCloudCoverMap = hourlyCloudCoverMap
        }
        val zoneId = resolveZoneId(locationTimezoneStr)
        val currentDate = LocalDate.now(zoneId)

        val dates = listOf(currentDate)

        calculationJob?.cancel()
        calculationJob = activity.lifecycleScope.launch {
            val summaries = nightVisibilityManager.calculateVisibilityPeriods(dates, zoneId, location, currentHourlyCloudCoverMap)
            val allPeriods = summaries.flatMap { it.periods }

            nightVisAdapter.submitList(allPeriods, isMultiNight = false, zoneId = zoneId)

            val emptyTv = activity.findViewById<android.widget.TextView>(R.id.tvNightVisEmptyState)
            val rv = activity.findViewById<RecyclerView>(R.id.rvNightVisPeriods)

            if (allPeriods.isEmpty()) {
                val noteText = summaries.firstOrNull { !it.note.isNullOrEmpty() }?.note
                    ?: "Night visibility data unavailable for the selected night."
                emptyTv?.text = noteText
                emptyTv?.visibility = View.VISIBLE
                rv?.visibility = View.GONE
            } else {
                emptyTv?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
            }
        }
    }

    fun updateSolunarDisplay(location: LocationItem = currentLocationItem) {
        currentLocationItem = location
        val zoneId = resolveZoneId(locationTimezoneStr)
        val currentDate = LocalDate.now(zoneId)

        val dates = (0..7).map { currentDate.plusDays(it.toLong()) }

        calculationJob?.cancel()
        calculationJob = activity.lifecycleScope.launch {
            val summaries = solunarManager.calculateSolunarForecast(dates, zoneId, location)

            solunarAdapter.submitList(summaries, zoneId = zoneId)

            val emptyTv = activity.findViewById<android.widget.TextView>(R.id.tvSolunarEmptyState)
            val rv = activity.findViewById<RecyclerView>(R.id.rvSolunarDays)

            if (summaries.isEmpty()) {
                emptyTv?.visibility = View.VISIBLE
                rv?.visibility = View.GONE
            } else {
                emptyTv?.visibility = View.GONE
                rv?.visibility = View.VISIBLE
            }
        }
    }

    fun updatePalette(palette: ColorPalette) {
        darkSkyAdapter.setPalette(palette)
        nightVisAdapter.setPalette(palette)
        solunarAdapter.setPalette(palette)
    }

    fun updateInlineGoldenHourDisplay(location: LocationItem = currentLocationItem, nowInstant: Instant = Instant.now()) {
        currentLocationItem = location
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
        val isToday = inlineGoldenDate.isEqual(LocalDate.now(resolveZoneId(locationTimezoneStr)))
        activity.findViewById<android.widget.TextView>(R.id.tvInlineSelectedDate)?.text = if (isToday) {
            "TODAY • ${inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())}"
        } else {
            inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())
        }

        val zoneId = resolveZoneId(locationTimezoneStr)
        val observer = buildObserver(location)

        val phases = SolarLightingEngine.calculateChronologicalSolarPhases(inlineGoldenDate, zoneId, observer, nowInstant = nowInstant)
        inlineSolarPhaseAdapter.submitList(phases)
    }

    fun promptSetSolarAlarm(item: SolarPhaseDetail) {
        if (activity.isFinishing || activity.isDestroyed) return

        val leadMins = locationPreferences.getSolarAlarmLeadMinutes()
        val startInstant = item.startInstant

        if (startInstant == null) {
            Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_continuous_event_alarm_error), Toast.LENGTH_SHORT).show()
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
            .setTitle(activity.getString(R.string.title_set_solar_alarm))
            .setMessage(activity.getString(R.string.msg_set_solar_alarm, leadMins, item.name, phaseFormatted, alarmFormatted))
            .setPositiveButton(activity.getString(R.string.btn_set_alarm)) { dialog, _ ->
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, alarmZdt.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, alarmZdt.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "${item.name} (${currentLocationItem.displayName})")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_alarm_set, alarmFormatted), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (activeInlineSubTab == InlineSubTab.NATURAL_LIGHT && locationPreferences.isVolumeScrollEnabled()) {
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
        calculationJob?.cancel()
        isVolumeKeyHeld = false
        handler.removeCallbacksAndMessages(null)
    }
}
