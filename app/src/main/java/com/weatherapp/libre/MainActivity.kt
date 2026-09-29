package com.weatherapp.libre

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.tabs.TabLayout
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.Gson
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogAboutSettingsBinding
import com.weatherapp.libre.databinding.DialogAlertsSettingsBinding
import com.weatherapp.libre.databinding.DialogCacheSettingsBinding
import com.weatherapp.libre.databinding.DialogLicensesSettingsBinding
import com.weatherapp.libre.databinding.DialogModelsSettingsBinding
import com.weatherapp.libre.databinding.DialogPrivacySettingsBinding
import com.weatherapp.libre.databinding.DialogRecentLocationsBinding
import com.weatherapp.libre.databinding.DialogSearchLocationBinding
import com.weatherapp.libre.databinding.DialogSettingsFullscreenBinding
import com.weatherapp.libre.databinding.DialogTabsSettingsBinding
import com.weatherapp.libre.databinding.DialogThemeSettingsBinding
import com.weatherapp.libre.databinding.ItemRecentLocationBinding
import com.weatherapp.libre.databinding.ItemSettingsOptionBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPreferences: LocationPreferences
    private val repository = WeatherRepository()
    private val gson = Gson()

    private lateinit var currentLocation: LocationItem
    private lateinit var unitSystem: UnitSystem
    private var locationTimezone: String? = null
    private var dailyForecasts = listOf<DayForecast>()
    private var currentWeatherData: CurrentData? = null
    private var selectedDayForecast: DayForecast? = null

    private lateinit var forecastAdapter: DayForecastAdapter
    private lateinit var inlineSolarPhaseAdapter: SolarPhaseAdapter
    private lateinit var inlineSolarEventAdapter: AstronomicalEventAdapter

    private var isUpdatingTabsProgrammatically = false

    enum class InlineSubTab {
        WEATHER_DETAILS,
        GOLDEN_HOUR,
        SOLAR_EVENTS
    }

    private var activeInlineSubTab: InlineSubTab = InlineSubTab.WEATHER_DETAILS
    private var inlineGoldenDate: LocalDate = LocalDate.now()

    // Volume key rapid cycling handler for Golden Hour with customizable speed
    private var isVolumeKeyHeld = false
    private var volumeKeyDirection = 0 // +1 for Up, -1 for Down
    private val handler = Handler(Looper.getMainLooper())
    private val rapidCycleRunnable = object : Runnable {
        override fun run() {
            if (isVolumeKeyHeld && activeInlineSubTab == InlineSubTab.GOLDEN_HOUR && locationPreferences.isVolumeScrollEnabled()) {
                inlineGoldenDate = if (volumeKeyDirection > 0) inlineGoldenDate.plusDays(1) else inlineGoldenDate.minusDays(1)
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
                handler.postDelayed(this, delayMs)
            }
        }
    }

    // Easter Egg Tap Counters
    private var lowTempTapCount = 0
    private var lastLowTempTapTime = 0L
    private var highTempTapCount = 0
    private var lastHighTempTapTime = 0L

    companion object {
        const val EXTRA_REOPEN_THEME_SETTINGS = "extra_reopen_theme_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        locationPreferences = LocationPreferences(this)
        val savedTheme = locationPreferences.getSavedThemeMode()

        val nightMode = when (savedTheme) {
            LocationPreferences.THEME_LIGHT, LocationPreferences.THEME_CREMA, LocationPreferences.THEME_TERRACOTTA, LocationPreferences.THEME_FROST, LocationPreferences.THEME_FIRE_ENGINE, LocationPreferences.THEME_ORANGE_GROVE -> AppCompatDelegate.MODE_NIGHT_NO
            LocationPreferences.THEME_DARK, LocationPreferences.THEME_CHARCOAL, LocationPreferences.THEME_COBALT, LocationPreferences.THEME_JADE, LocationPreferences.THEME_AURORA -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navPaddingExtra = if (locationPreferences.isNavPaddingEnabled()) (48 * resources.displayMetrics.density).toInt() else 0

            // Pad main content view below top status bar and above bottom gesture/navigation bar
            binding.main.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom + navPaddingExtra
            )

            // Pad drawer header view cleanly below top status bar and close drawer when tapped
            if (binding.navigationView.headerCount > 0) {
                val headerView = binding.navigationView.getHeaderView(0)
                val density = resources.displayMetrics.density
                headerView.setPadding(
                    (20 * density).toInt(),
                    systemBars.top + (12 * density).toInt(),
                    (20 * density).toInt(),
                    (16 * density).toInt()
                )
                headerView.setOnClickListener {
                    if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }
                }
            }
            insets
        }

        unitSystem = locationPreferences.getSavedUnitSystem()
        forecastAdapter = DayForecastAdapter(unitSystem)

        val activeIndex = locationPreferences.getActiveTabIndex()
        currentLocation = locationPreferences.getTabLocation(activeIndex)

        setupUI()
        setupDrawerNavigation()
        setupLocationTabs()
        updateTabsStickyPosition()
        applyPaletteToUI()
        loadWeather()

        // Reopen Theme Settings Screen if requested after theme change
        if (locationPreferences.consumePendingReopenThemeSettings() || intent.getBooleanExtra(EXTRA_REOPEN_THEME_SETTINGS, false)) {
            showThemeSettingsScreen()
        }

        // Track launch count
        val launchCount = locationPreferences.incrementAppLaunchCount()

        // Check for 500th launch milestone prompt
        if (launchCount >= 500) {
            show500LaunchMilestoneDialog()
        } else if (launchCount % 5 == 0) {
            // Check for Play Store updates every 5th launch
            checkPlayStoreUpdate(silent = true)
        }

        if (locationPreferences.isFirstLaunch()) {
            showFirstLaunchLocationPrompt()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAlertBanner()
        checkTodaySolarEvents()
        updateTabsStickyPosition()
        applyPaletteToUI()
    }

    private fun updateTabsStickyPosition() {
        val tabCount = locationPreferences.getTabCount()
        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val density = resources.displayMetrics.density

        // Safely detach cardLocationTabs from current parent
        (binding.cardLocationTabs.parent as? ViewGroup)?.removeView(binding.cardLocationTabs)

        val paramsCard = binding.cardTodayDetails.layoutParams as? ViewGroup.MarginLayoutParams
        val paramsAlert = binding.cardAlertBanner.layoutParams as? ViewGroup.MarginLayoutParams

        val alertJson = locationPreferences.getActiveAlertJsonForLocation(currentLocation)
        val isAlertVisible = !alertJson.isNullOrEmpty() && !locationPreferences.isAlertDismissedForLocation(currentLocation)

        if (tabCount <= 1) {
            binding.layoutStickyTabsContainer.visibility = View.GONE
            binding.layoutScrollableTabsContainer.visibility = View.GONE

            if (isAlertVisible) {
                val alertShape = binding.cardAlertBanner.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 16f)
                    .build()
                binding.cardAlertBanner.shapeAppearanceModel = alertShape
                paramsAlert?.topMargin = (density * 16f).toInt()
                binding.cardAlertBanner.layoutParams = paramsAlert

                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = (density * 12f).toInt()
                binding.cardTodayDetails.layoutParams = paramsCard
            } else {
                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = (density * 16f).toInt()
                binding.cardTodayDetails.layoutParams = paramsCard
            }
            return
        }

        val isSticky = locationPreferences.isLocationTabsSticky()

        if (isSticky) {
            binding.layoutStickyTabsContainer.visibility = View.VISIBLE
            binding.layoutScrollableTabsContainer.visibility = View.GONE
            binding.layoutStickyTabsContainer.addView(binding.cardLocationTabs)

            // Chrome/Folder Tab Style: Top corners rounded (14dp), bottom corners square (0dp)
            val tabsShape = binding.cardLocationTabs.shapeAppearanceModel.toBuilder()
                .setTopLeftCorner(CornerFamily.ROUNDED, density * 14f)
                .setTopRightCorner(CornerFamily.ROUNDED, density * 14f)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                .build()
            binding.cardLocationTabs.shapeAppearanceModel = tabsShape
            binding.cardLocationTabs.strokeColor = palette.dividerColor
            binding.cardLocationTabs.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))

            if (isAlertVisible) {
                // Alert Banner attached flush below sticky tabs: flat top & bottom edges (0dp)
                val alertShape = binding.cardAlertBanner.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, 0f)
                    .build()
                binding.cardAlertBanner.shapeAppearanceModel = alertShape
                paramsAlert?.topMargin = 0
                binding.cardAlertBanner.layoutParams = paramsAlert

                // Main Weather Card attached flush below alert banner: flat top edge (0dp)
                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
                    .setTopRightCorner(CornerFamily.ROUNDED, 0f)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, density * 20f)
                    .setBottomRightCorner(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = 0
                binding.cardTodayDetails.layoutParams = paramsCard
            } else {
                // Main Weather Card attached flush below tabs: Flat top edge (0dp top-left & top-right)
                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
                    .setTopRightCorner(CornerFamily.ROUNDED, 0f)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, density * 20f)
                    .setBottomRightCorner(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = 0
                binding.cardTodayDetails.layoutParams = paramsCard
            }
        } else {
            binding.layoutStickyTabsContainer.visibility = View.GONE
            binding.layoutScrollableTabsContainer.visibility = View.VISIBLE
            binding.layoutScrollableTabsContainer.addView(binding.cardLocationTabs)

            // Standalone Cell Style: All 4 corners rounded (16dp)
            val tabsShape = binding.cardLocationTabs.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, density * 16f)
                .build()
            binding.cardLocationTabs.shapeAppearanceModel = tabsShape
            binding.cardLocationTabs.strokeColor = palette.dividerColor
            binding.cardLocationTabs.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))

            if (isAlertVisible) {
                // Alert Banner standalone below tabs: All 4 corners rounded (16dp)
                val alertShape = binding.cardAlertBanner.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 16f)
                    .build()
                binding.cardAlertBanner.shapeAppearanceModel = alertShape
                paramsAlert?.topMargin = (density * 12f).toInt()
                binding.cardAlertBanner.layoutParams = paramsAlert

                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = (density * 12f).toInt()
                binding.cardTodayDetails.layoutParams = paramsCard
            } else {
                val cardShape = binding.cardTodayDetails.shapeAppearanceModel.toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, density * 20f)
                    .build()
                binding.cardTodayDetails.shapeAppearanceModel = cardShape
                paramsCard?.topMargin = (density * 12f).toInt()
                binding.cardTodayDetails.layoutParams = paramsCard
            }
        }
    }

    private fun setupLocationTabs() {
        val tabCount = locationPreferences.getTabCount()
        val activeIndex = locationPreferences.getActiveTabIndex()

        isUpdatingTabsProgrammatically = true
        binding.tabLayoutLocations.removeAllTabs()

        for (i in 0 until tabCount) {
            val loc = locationPreferences.getTabLocation(i)
            val tab = binding.tabLayoutLocations.newTab()
            tab.text = loc.name
            tab.tag = i
            binding.tabLayoutLocations.addTab(tab, i == activeIndex)
        }

        isUpdatingTabsProgrammatically = false

        binding.tabLayoutLocations.clearOnTabSelectedListeners()
        binding.tabLayoutLocations.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (isUpdatingTabsProgrammatically) return
                val pos = tab?.position ?: 0
                locationPreferences.saveActiveTabIndex(pos)
                currentLocation = locationPreferences.getTabLocation(pos)
                updateLocationHeader()
                loadWeather()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        updateTabsStickyPosition()
    }

    private fun updateActiveTabCity(newLocation: LocationItem) {
        val activeIndex = locationPreferences.getActiveTabIndex()
        locationPreferences.saveTabLocation(activeIndex, newLocation)
        val tab = binding.tabLayoutLocations.getTabAt(activeIndex)
        if (tab != null) {
            tab.text = newLocation.name
        }
    }

    private fun applyPaletteToUI() {
        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        window.decorView.setBackgroundColor(palette.backgroundLight)
        binding.root.setBackgroundColor(palette.backgroundLight)
        binding.main.setBackgroundColor(palette.backgroundLight)
        binding.navigationView.setBackgroundColor(palette.surfaceCard)
        binding.navigationView.itemTextColor = ColorStateList.valueOf(palette.textPrimary)
        binding.navigationView.itemIconTintList = ColorStateList.valueOf(palette.textPrimary)

        if (binding.navigationView.headerCount > 0) {
            val headerView = binding.navigationView.getHeaderView(0)
            headerView.setBackgroundColor(palette.surfaceCardSecondary)
        }

        binding.cardLocationTabs.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        binding.cardLocationTabs.strokeColor = palette.dividerColor
        binding.tabLayoutLocations.setBackgroundColor(Color.TRANSPARENT)
        binding.tabLayoutLocations.setSelectedTabIndicatorColor(palette.primary)
        binding.tabLayoutLocations.setTabTextColors(palette.textSecondary, palette.primary)

        if (savedTheme == LocationPreferences.THEME_FIRE_ENGINE) {
            binding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.primary))
            binding.cardHeader.strokeColor = palette.primary
            binding.tvLocationName.setTextColor(Color.WHITE)
            binding.tvAttribution.setTextColor(Color.parseColor("#FFD1D1"))
            binding.ivPin.imageTintList = ColorStateList.valueOf(Color.WHITE)
            binding.btnMenu.imageTintList = ColorStateList.valueOf(Color.WHITE)
            binding.cardTodayDetails.strokeColor = palette.primary
        } else {
            binding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            binding.cardHeader.strokeColor = palette.dividerColor
            binding.tvLocationName.setTextColor(palette.textPrimary)
            binding.tvAttribution.setTextColor(palette.textSecondary)
            binding.ivPin.imageTintList = ColorStateList.valueOf(palette.primary)
            binding.btnMenu.imageTintList = ColorStateList.valueOf(palette.textPrimary)
            binding.cardTodayDetails.strokeColor = palette.dividerColor
        }

        binding.cardTodayDetails.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        binding.tvSelectedDateHeader.setTextColor(palette.textSecondary)
        binding.tvMainCondition.setTextColor(palette.textPrimary)
        binding.tvCurrentTemp.setTextColor(palette.textSecondary)

        binding.cardHighTemp.setBackgroundColor(palette.tempHighBg)
        binding.cardLowTemp.setBackgroundColor(palette.tempLowBg)
        binding.tvDailyHigh.setTextColor(palette.tempHigh)
        binding.tvDailyLow.setTextColor(palette.tempLow)

        binding.layoutMetricWind.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricAqi.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricUv.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricVisibility.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricHumidity.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricCeiling.setBackgroundColor(palette.surfaceCardSecondary)

        binding.layoutMetricSunrise.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricSunset.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricDaylight.setBackgroundColor(palette.surfaceCardSecondary)
        binding.layoutMetricMoon.setBackgroundColor(palette.surfaceCardSecondary)

        binding.tvWindSpeed.setTextColor(palette.textPrimary)
        binding.tvAirQuality.setTextColor(palette.textPrimary)
        binding.tvUvIndex.setTextColor(palette.textPrimary)
        binding.tvVisibility.setTextColor(palette.textPrimary)
        binding.tvHumidity.setTextColor(palette.textPrimary)
        binding.tvCeiling.setTextColor(palette.textPrimary)

        binding.tvSunrise.setTextColor(palette.textPrimary)
        binding.tvSunset.setTextColor(palette.textPrimary)
        binding.tvSunriseAzimuth.setTextColor(palette.textSecondary)
        binding.tvSunsetAzimuth.setTextColor(palette.textSecondary)

        binding.tvTotalDaylight.setTextColor(palette.textPrimary)
        binding.tvMoonPhase.setTextColor(palette.textPrimary)
        binding.tvMoonCyclePercentage.setTextColor(palette.textSecondary)

        // Dropdown menu button styling
        binding.btnInlineMenu.strokeColor = ColorStateList.valueOf(palette.dividerColor)

        inlineSolarPhaseAdapter.setPalette(palette)
        inlineSolarEventAdapter.setPalette(palette)
        forecastAdapter.setPalette(palette)
    }

    private fun checkTodaySolarEvents() {
        val (_, upcomingEvents) = LunarInfoRepository.generateEventsFromLunarInfo(this)
        val todayEvent = upcomingEvents.find { it.isHappeningToday }

        if (todayEvent != null) {
            val goldColor = ContextCompat.getColor(this, R.color.golden_hour_primary)
            binding.btnInlineMenu.strokeColor = ColorStateList.valueOf(goldColor)
        } else {
            val dividerColor = ContextCompat.getColor(this, R.color.divider_color)
            binding.btnInlineMenu.strokeColor = ColorStateList.valueOf(dividerColor)
        }
    }

    private fun updateAlertBanner() {
        val alertJson = locationPreferences.getActiveAlertJsonForLocation(currentLocation)
        val isDismissed = locationPreferences.isAlertDismissedForLocation(currentLocation)

        if (!alertJson.isNullOrEmpty() && !isDismissed) {
            try {
                val capAlert = gson.fromJson(alertJson, CapAlertMessage::class.java)
                if (capAlert != null) {
                    val savedTheme = locationPreferences.getSavedThemeMode()
                    val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

                    binding.cardAlertBanner.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))
                    binding.cardAlertBanner.strokeColor = palette.tempHigh
                    binding.tvAlertHeaderTitle.text = "🚨 ECCC ALERT: ${capAlert.event.uppercase(Locale.getDefault())}"
                    binding.tvAlertHeaderTitle.setTextColor(palette.tempHigh)
                    binding.tvAlertHeadline.text = capAlert.headline
                    binding.tvAlertHeadline.setTextColor(palette.textPrimary)
                    binding.tvAlertInstructions.text = capAlert.instruction
                    binding.tvAlertInstructions.setTextColor(palette.textSecondary)
                    binding.tvAlertSource.setTextColor(palette.textSecondary)
                    binding.btnDismissAlert.setTextColor(palette.primary)
                    binding.cardAlertBanner.visibility = View.VISIBLE

                    binding.btnDismissAlert.setOnClickListener {
                        locationPreferences.setAlertDismissedForLocation(currentLocation, true)
                        binding.cardAlertBanner.visibility = View.GONE
                        updateTabsStickyPosition()
                    }
                    updateTabsStickyPosition()
                    return
                }
            } catch (e: Exception) {
                // Parse error fallback
            }
        }
        binding.cardAlertBanner.visibility = View.GONE
        updateTabsStickyPosition()
    }

    private fun setupUI() {
        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        binding.rvForecast.adapter = forecastAdapter

        inlineSolarPhaseAdapter = SolarPhaseAdapter(
            onItemLongClick = { phaseDetail ->
                promptSetSolarAlarm(phaseDetail)
            }
        )
        binding.rvInlineSolarPhases.layoutManager = LinearLayoutManager(this)
        binding.rvInlineSolarPhases.adapter = inlineSolarPhaseAdapter

        inlineSolarEventAdapter = AstronomicalEventAdapter { event ->
            promptAddCalendarEvent(event)
        }
        binding.rvInlineSolarEvents.layoutManager = LinearLayoutManager(this)
        binding.rvInlineSolarEvents.adapter = inlineSolarEventAdapter

        // Hamburger button closes drawer if open, opens drawer if closed
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        binding.layoutLocationClickable.setOnClickListener {
            showRecentLocationsDialog()
        }

        // Single Dropdown Menu Button Popup for Sub-Tabs
        binding.btnInlineMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Weather")
            popup.menu.add(0, 2, 1, "Golden Hour")
            popup.menu.add(0, 3, 2, "Solar Events")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> switchInlineSubTab(InlineSubTab.WEATHER_DETAILS)
                    2 -> switchInlineSubTab(InlineSubTab.GOLDEN_HOUR)
                    3 -> switchInlineSubTab(InlineSubTab.SOLAR_EVENTS)
                }
                true
            }
            popup.show()
        }

        // Inline Golden Hour Controls
        binding.btnInlinePrevDay.setOnClickListener {
            inlineGoldenDate = inlineGoldenDate.minusDays(1)
            updateInlineGoldenHourDisplay()
        }

        binding.btnInlineNextDay.setOnClickListener {
            inlineGoldenDate = inlineGoldenDate.plusDays(1)
            updateInlineGoldenHourDisplay()
        }

        binding.layoutInlineDateClickable.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    inlineGoldenDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateInlineGoldenHourDisplay()
                },
                inlineGoldenDate.year,
                inlineGoldenDate.monthValue - 1,
                inlineGoldenDate.dayOfMonth
            )
            datePicker.show()
        }

        // Inline Solar Events Tab Setup
        binding.tabLayoutInlineEvents.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateInlineSolarEventsDisplay(tab?.position ?: 1)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Easter Egg 1: Tap Daily Low 5 Times -> Snowflakes Effect
        binding.tvDailyLow.setOnClickListener { handleDailyLowTap() }
        binding.cardLowTemp.setOnClickListener { handleDailyLowTap() }
        binding.btnDismissLowEffect.setOnClickListener { stopSnowEffect() }

        // Easter Egg 2: Tap Daily High 5 Times -> Heat Mirage Shimmer Effect
        binding.tvDailyHigh.setOnClickListener { handleDailyHighTap() }
        binding.cardHighTemp.setOnClickListener { handleDailyHighTap() }
        binding.btnDismissHighEffect.setOnClickListener { stopShimmerEffect() }

        binding.swipeRefresh.setOnRefreshListener {
            loadWeather(isSwipeRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            loadWeather()
        }

        updateLocationHeader()
    }

    private fun switchInlineSubTab(subTab: InlineSubTab) {
        activeInlineSubTab = subTab

        binding.layoutWeatherDetailsContainer.visibility = if (subTab == InlineSubTab.WEATHER_DETAILS) View.VISIBLE else View.GONE
        binding.layoutInlineGoldenHourContainer.visibility = if (subTab == InlineSubTab.GOLDEN_HOUR) View.VISIBLE else View.GONE
        binding.layoutInlineSolarEventsContainer.visibility = if (subTab == InlineSubTab.SOLAR_EVENTS) View.VISIBLE else View.GONE

        when (subTab) {
            InlineSubTab.WEATHER_DETAILS -> {
                binding.btnInlineMenu.text = "Weather ▾"
                binding.btnInlineMenu.setIconResource(R.drawable.ic_weather)
                checkTodaySolarEvents()
            }
            InlineSubTab.GOLDEN_HOUR -> {
                binding.btnInlineMenu.text = "Golden Hour ▾"
                binding.btnInlineMenu.setIconResource(R.drawable.ic_golden_hour)
                updateInlineGoldenHourDisplay()
            }
            InlineSubTab.SOLAR_EVENTS -> {
                binding.btnInlineMenu.text = "Solar Events ▾"
                binding.btnInlineMenu.setIconResource(R.drawable.ic_solar_events)
                val activeTabPos = binding.tabLayoutInlineEvents.selectedTabPosition.coerceAtLeast(0)
                updateInlineSolarEventsDisplay(if (activeTabPos in 0..1) activeTabPos else 1)
            }
        }
    }

    private fun updateInlineGoldenHourDisplay() {
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
        binding.tvInlineSelectedDate.text = if (inlineGoldenDate.isEqual(LocalDate.now())) {
            "TODAY • ${inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())}"
        } else {
            inlineGoldenDate.format(dateFormatter).uppercase(Locale.getDefault())
        }

        val zoneId = try {
            if (!locationTimezone.isNullOrEmpty()) ZoneId.of(locationTimezone) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val phases = SolarCalculator.calculate13ChronologicalPhases(
            currentLocation.latitude,
            currentLocation.longitude,
            inlineGoldenDate,
            is24Hour = true,
            zoneId = zoneId
        )

        inlineSolarPhaseAdapter.submitList(phases)
    }

    private fun updateInlineSolarEventsDisplay(tabPosition: Int) {
        val (pastEvents, upcomingEvents) = LunarInfoRepository.generateEventsFromLunarInfo(this)
        val eventsToShow = if (tabPosition == 0) pastEvents else upcomingEvents
        inlineSolarEventAdapter.submitList(eventsToShow)
    }

    private fun promptSetSolarAlarm(item: SolarPhaseDetail) {
        val leadMins = locationPreferences.getSolarAlarmLeadMinutes()
        val startTimeStr = item.timeRangeText.split("–", "-").firstOrNull()?.trim() ?: item.timeRangeText.trim()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

        val startTime = try {
            LocalTime.parse(startTimeStr, timeFormatter)
        } catch (e: Exception) {
            LocalTime.of(7, 0)
        }

        val alarmTime = startTime.minusMinutes(leadMins.toLong())
        val is24Hour = DateFormat.is24HourFormat(this)
        val alarmFormatted = SolarCalculator.formatTime(alarmTime, is24Hour)
        val phaseFormatted = SolarCalculator.formatTime(startTime, is24Hour)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Solar Alarm")
            .setMessage("Set a system alarm for $leadMins minutes before ${item.name} ($phaseFormatted)?\n\nAlarm time: $alarmFormatted")
            .setPositiveButton("Set Alarm") { dialog, _ ->
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "${item.name} (${currentLocation.displayName})")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Alarm set for $alarmFormatted", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun promptAddCalendarEvent(event: AstronomicalEventItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Add to Calendar")
            .setMessage("Would you like to add '${event.title}' (${event.dateText}) to your device calendar?")
            .setPositiveButton("Add Event") { dialog, _ ->
                try {
                    val startMillis = event.eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = startMillis + (2 * 60 * 60 * 1000L)

                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, event.title)
                        putExtra(CalendarContract.Events.DESCRIPTION, "${event.shortDescription}\n\n${event.detailedDescription}")
                        putExtra(CalendarContract.Events.EVENT_LOCATION, "Outdoor Night Sky")
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open Calendar application.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeKeyHeld = false
            handler.removeCallbacks(rapidCycleRunnable)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleDailyLowTap() {
        val now = System.currentTimeMillis()
        if (now - lastLowTempTapTime < 1500L) {
            lowTempTapCount++
        } else {
            lowTempTapCount = 1
        }
        lastLowTempTapTime = now

        if (lowTempTapCount >= 5) {
            lowTempTapCount = 0
            startSnowEffect()
        }
    }

    private fun handleDailyHighTap() {
        val now = System.currentTimeMillis()
        if (now - lastHighTempTapTime < 1500L) {
            highTempTapCount++
        } else {
            highTempTapCount = 1
        }
        lastHighTempTapTime = now

        if (highTempTapCount >= 5) {
            highTempTapCount = 0
            startShimmerEffect()
        }
    }

    private fun startSnowEffect() {
        stopShimmerEffect()
        binding.snowflakeOverlay.startSnowing()
        binding.btnDismissLowEffect.visibility = View.VISIBLE
        Toast.makeText(this, "❄️ Snowfall Effect Activated!", Toast.LENGTH_SHORT).show()
    }

    private fun stopSnowEffect() {
        binding.snowflakeOverlay.stopSnowing()
        binding.btnDismissLowEffect.visibility = View.GONE
    }

    private fun startShimmerEffect() {
        stopSnowEffect()
        binding.heatShimmerOverlay.startHeatShimmer()
        binding.btnDismissHighEffect.visibility = View.VISIBLE
        Toast.makeText(this, "🔥 Extreme Heat Mirage Shimmer Activated!", Toast.LENGTH_SHORT).show()
    }

    private fun stopShimmerEffect() {
        binding.heatShimmerOverlay.stopHeatShimmer()
        binding.btnDismissHighEffect.visibility = View.GONE
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.menu_item_theme -> showThemeSettingsScreen()
                R.id.menu_item_tabs -> showLocationTabsSettingsScreen()
                R.id.menu_item_models -> showModelsSettingsScreen()
                R.id.menu_item_alerts -> showWeatherAlertsSettingsScreen()
                R.id.menu_item_cache -> showCacheSettingsScreen()
                R.id.menu_item_default_location -> showDefaultLocationSettingsScreen()
                R.id.menu_item_donate -> openDonateUrl()
                R.id.menu_item_licenses -> showLicensesSettingsScreen()
                R.id.menu_item_privacy -> showPrivacySettingsScreen()
                R.id.menu_item_about -> showAboutSettingsScreen()
                R.id.menu_item_update -> checkPlayStoreUpdate(silent = false)
                R.id.menu_item_rate -> openPlayStorePage()
            }
            true
        }
    }

    private fun openDonateUrl() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/adamjeremy"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open browser link.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStorePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.weatherapp.libre"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Play Store.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPlayStoreUpdate(silent: Boolean = false) {
        if (!silent) {
            Toast.makeText(this, "Checking for WeatherAPP updates...", Toast.LENGTH_SHORT).show()
        }

        try {
            val appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    showUpdateAvailablePrompt(appUpdateInfo, appUpdateManager)
                } else {
                    if (!silent) {
                        showUpToDatePrompt()
                    }
                }
            }.addOnFailureListener {
                if (!silent) {
                    showCheckUpdateFallbackDialog()
                }
            }
        } catch (e: Exception) {
            if (!silent) {
                openPlayStorePage()
            }
        }
    }

    private fun showUpdateAvailablePrompt(
        appUpdateInfo: AppUpdateInfo,
        appUpdateManager: AppUpdateManager
    ) {
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.2.0"
        } catch (e: Exception) {
            "1.2.0"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🚀 Update Available!")
            .setMessage("A new version of WeatherAPP is available on the Google Play Store.\n\nYour current version: v$currentVersion\n\nUpdating gives you the latest weather features, model accuracy enhancements, and bug fixes.")
            .setPositiveButton("Update Now") { dialog, _ ->
                try {
                    val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (isFlexibleAllowed || isImmediateAllowed) {
                        val updateType = if (isFlexibleAllowed) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(updateType).build(),
                            1002
                        )
                    } else {
                        openPlayStorePage()
                    }
                } catch (e: Exception) {
                    openPlayStorePage()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showUpToDatePrompt() {
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.2.0"
        } catch (e: Exception) {
            "1.2.0"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("WeatherAPP is Up to Date")
            .setMessage("You are running the latest version of WeatherAPP (v$currentVersion).\n\nNo new updates are available at this time.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showCheckUpdateFallbackDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Check Play Store for Updates")
            .setMessage("Would you like to open the Google Play Store page to check for WeatherAPP updates?")
            .setPositiveButton("Open Play Store") { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun show500LaunchMilestoneDialog() {
        locationPreferences.resetAppLaunchCount()
        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 500 App Launches!")
            .setMessage("Thank you for using WeatherAPP! You have opened WeatherAPP 500 times.\n\nAs a 100% free and ad-free community app, your review or donation keeps WeatherAPP growing for everyone.")
            .setPositiveButton("Leave a Review") { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNeutralButton("Donate (Ko-fi)") { dialog, _ ->
                openDonateUrl()
                dialog.dismiss()
            }
            .setNegativeButton("Maybe Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private data class SettingsOption(
        val title: String,
        val description: String,
        val isSelected: Boolean,
        val onClick: () -> Unit
    )

    private fun showFullscreenSettingsScreen(
        title: String,
        subtitle: String,
        options: List<SettingsOption>,
        actionButtonText: String? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogSettingsFullscreenBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.imageTintList = ColorStateList.valueOf(palette.textPrimary)

        dialogBinding.tvSettingsTitle.text = title
        dialogBinding.tvSettingsTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSettingsSubtitle.text = subtitle
        dialogBinding.tvSettingsSubtitle.setTextColor(palette.textSecondary)

        dialogBinding.btnBack.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.rgOptions.removeAllViews()

        for (opt in options) {
            val optionBinding = ItemSettingsOptionBinding.inflate(
                layoutInflater,
                dialogBinding.rgOptions,
                false
            )

            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = opt.title
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = opt.description
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = opt.isSelected

            optionBinding.cardOption.setOnClickListener {
                opt.onClick()
                dialog.dismiss()
            }

            dialogBinding.rgOptions.addView(optionBinding.root)
        }

        if (actionButtonText != null && onActionClick != null) {
            dialogBinding.btnAction.visibility = View.VISIBLE
            dialogBinding.btnAction.text = actionButtonText
            dialogBinding.btnAction.setOnClickListener {
                onActionClick()
                dialog.dismiss()
            }
        } else {
            dialogBinding.btnAction.visibility = View.GONE
        }

        dialog.show()
    }

    private fun showThemeSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogThemeSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.imageTintList = ColorStateList.valueOf(palette.textPrimary)

        dialogBinding.cardSystemDefaultSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSystemDefaultSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvSystemDefaultTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSystemDefaultDesc.setTextColor(palette.textSecondary)

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        // Units Selector Card inside Theme & Display Settings
        dialogBinding.cardUnitsSelector.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardUnitsSelector.strokeColor = palette.dividerColor
        dialogBinding.tvUnitsTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvUnitsDesc.setTextColor(palette.textSecondary)

        fun updateUnitsCardDisplay() {
            if (unitSystem == UnitSystem.US_CUSTOMARY) {
                dialogBinding.tvUnitsDesc.text = "US Customary (°F, mph, mi)"
                dialogBinding.btnUnitsChoice.text = "US Customary"
            } else {
                dialogBinding.tvUnitsDesc.text = "Metric (°C, km/h, km)"
                dialogBinding.btnUnitsChoice.text = "Metric"
            }
            dialogBinding.btnUnitsChoice.setTextColor(palette.textPrimary)
            dialogBinding.btnUnitsChoice.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
            dialogBinding.btnUnitsChoice.strokeColor = ColorStateList.valueOf(palette.dividerColor)
        }
        updateUnitsCardDisplay()

        fun promptUnitsChoice() {
            val options = listOf(
                SettingsOption(
                    title = "Metric (Default)",
                    description = "Celsius (°C), km/h wind speed, kilometers visibility, meters cloud ceiling.",
                    isSelected = unitSystem == UnitSystem.METRIC,
                    onClick = {
                        applyUnitSystem(UnitSystem.METRIC)
                        updateUnitsCardDisplay()
                    }
                ),
                SettingsOption(
                    title = "US Customary",
                    description = "Fahrenheit (°F), mph wind speed, miles visibility, feet cloud ceiling.",
                    isSelected = unitSystem == UnitSystem.US_CUSTOMARY,
                    onClick = {
                        applyUnitSystem(UnitSystem.US_CUSTOMARY)
                        updateUnitsCardDisplay()
                    }
                )
            )

            showFullscreenSettingsScreen(
                title = "Units",
                subtitle = "Choose unit measurements for temperatures, wind, and visibility.",
                options = options
            )
        }

        dialogBinding.cardUnitsSelector.setOnClickListener { promptUnitsChoice() }
        dialogBinding.btnUnitsChoice.setOnClickListener { promptUnitsChoice() }

        // Volume Key Golden Hour Scroll Settings
        dialogBinding.cardVolumeScrollSettings.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardVolumeScrollSettings.strokeColor = palette.dividerColor
        dialogBinding.tvVolumeScrollTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvVolumeScrollDesc.setTextColor(palette.textSecondary)

        val isVolumeEnabled = locationPreferences.isVolumeScrollEnabled()
        dialogBinding.switchVolumeScroll.isChecked = isVolumeEnabled

        val speedNames = listOf(
            "Speed 1 - Very Slow",
            "Speed 2 - Slower",
            "Speed 3 - Slow",
            "Speed 4 - Normal (Default)",
            "Speed 5 - Fast",
            "Speed 6 - Faster",
            "Speed 7 - Very Fast"
        )

        val currentSpeedIndex = locationPreferences.getVolumeScrollSpeedIndex().coerceIn(1, 7)
        dialogBinding.seekBarVolumeSpeed.progress = currentSpeedIndex - 1
        dialogBinding.tvVolumeSpeedLabel.text = "Cycling Speed: ${speedNames[currentSpeedIndex - 1]}"

        dialogBinding.layoutVolumeSpeedContainer.alpha = if (isVolumeEnabled) 1.0f else 0.4f
        dialogBinding.seekBarVolumeSpeed.isEnabled = isVolumeEnabled

        dialogBinding.switchVolumeScroll.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveVolumeScrollEnabled(isChecked)
            dialogBinding.layoutVolumeSpeedContainer.alpha = if (isChecked) 1.0f else 0.4f
            dialogBinding.seekBarVolumeSpeed.isEnabled = isChecked
            val msg = if (isChecked) "Volume key date cycling enabled" else "Volume key date cycling disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        dialogBinding.seekBarVolumeSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val index = progress + 1
                dialogBinding.tvVolumeSpeedLabel.text = "Cycling Speed: ${speedNames[progress]}"
                if (fromUser) {
                    locationPreferences.saveVolumeScrollSpeedIndex(index)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val isSystemDefault = (savedTheme == LocationPreferences.THEME_SYSTEM)
        dialogBinding.switchSystemDefault.isChecked = isSystemDefault

        // 3-Button Touchscreen Navigation Padding Switch
        dialogBinding.cardNavPaddingSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardNavPaddingSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvNavPaddingTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvNavPaddingDesc.setTextColor(palette.textSecondary)

        val isNavPaddingOn = locationPreferences.isNavPaddingEnabled()
        dialogBinding.switchNavPadding.isChecked = isNavPaddingOn
        dialogBinding.switchNavPadding.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveNavPaddingEnabled(isChecked)
            
            // Re-trigger insets request to instantly recalculate bottom padding without requiring app restart
            ViewCompat.requestApplyInsets(binding.drawerLayout)

            val msg = if (isChecked) "3-Button Navigation Padding Enabled" else "3-Button Navigation Padding Disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        fun setChoicesEnabled(enabled: Boolean) {
            dialogBinding.layoutThemeChoicesContainer.alpha = if (enabled) 1.0f else 0.4f
            for (i in 0 until dialogBinding.layoutStandardThemes.childCount) {
                dialogBinding.layoutStandardThemes.getChildAt(i).isEnabled = enabled
            }
            for (i in 0 until dialogBinding.layoutBoutiqueThemes.childCount) {
                dialogBinding.layoutBoutiqueThemes.getChildAt(i).isEnabled = enabled
            }
        }

        dialogBinding.switchSystemDefault.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setChoicesEnabled(false)
                applyTheme(LocationPreferences.THEME_SYSTEM)
            } else {
                setChoicesEnabled(true)
            }
        }

        data class ThemeItemSpec(val themeId: Int, val title: String, val description: String)

        val standardThemes = listOf(
            ThemeItemSpec(LocationPreferences.THEME_LIGHT, "Light Theme", "Clean light background and high contrast typography."),
            ThemeItemSpec(LocationPreferences.THEME_DARK, "Dark Theme", "Modern bluish dark slate background for nighttime comfort.")
        )

        val boutiqueThemes = listOf(
            ThemeItemSpec(LocationPreferences.THEME_AURORA, "Aurora Theme", "Deep indigo, muted teal, and soft magenta with near-black surfaces."),
            ThemeItemSpec(LocationPreferences.THEME_CHARCOAL, "Charcoal Theme", "Deep matte charcoal with subtle shades of gray."),
            ThemeItemSpec(LocationPreferences.THEME_COBALT, "Cobalt Theme", "Rich oceanic cobalt navy with electric blue accents."),
            ThemeItemSpec(LocationPreferences.THEME_CREMA, "Crema Theme", "Warm cream and pastel tones for a comfortable reading experience."),
            ThemeItemSpec(LocationPreferences.THEME_FIRE_ENGINE, "Fire Engine Theme", "Bold fire engine red header, pure white surfaces, and near-black typography."),
            ThemeItemSpec(LocationPreferences.THEME_FROST, "Frost Theme", "Pale icy blue, light silver-gray, and pure white surfaces."),
            ThemeItemSpec(LocationPreferences.THEME_JADE, "Jade Theme", "Imperial jade green with sage emerald highlights."),
            ThemeItemSpec(LocationPreferences.THEME_ORANGE_GROVE, "Orange Grove Theme", "Citrus orange, deep grove green, and warm cream surfaces."),
            ThemeItemSpec(LocationPreferences.THEME_TERRACOTTA, "Terracotta Theme", "Earthy red-orange, warm sand beige, and muted olive green.")
        )

        // Populate Standard Themes
        dialogBinding.layoutStandardThemes.removeAllViews()
        for (spec in standardThemes) {
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.layoutStandardThemes, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = spec.title
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = spec.description
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (!isSystemDefault && savedTheme == spec.themeId)

            optionBinding.cardOption.setOnClickListener {
                if (!dialogBinding.switchSystemDefault.isChecked) {
                    applyTheme(spec.themeId)
                }
            }
            dialogBinding.layoutStandardThemes.addView(optionBinding.root)
        }

        // Populate Boutique Themes
        dialogBinding.layoutBoutiqueThemes.removeAllViews()
        for (spec in boutiqueThemes) {
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.layoutBoutiqueThemes, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = spec.title
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = spec.description
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (!isSystemDefault && savedTheme == spec.themeId)

            optionBinding.cardOption.setOnClickListener {
                if (!dialogBinding.switchSystemDefault.isChecked) {
                    applyTheme(spec.themeId)
                }
            }
            dialogBinding.layoutBoutiqueThemes.addView(optionBinding.root)
        }

        setChoicesEnabled(!isSystemDefault)

        dialog.show()
    }

    private fun showLocationTabsSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogTabsSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.cardStickyTabsSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardStickyTabsSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvStickyTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvStickyDesc.setTextColor(palette.textSecondary)

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        // Sticky switch toggle
        val isSticky = locationPreferences.isLocationTabsSticky()
        dialogBinding.switchStickyTabs.isChecked = isSticky
        dialogBinding.switchStickyTabs.setOnCheckedChangeListener { _, checked ->
            locationPreferences.saveLocationTabsSticky(checked)
            updateTabsStickyPosition()
            val msg = if (checked) "Location tabs pinned as sticky" else "Location tabs set to scroll with page"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        val currentCount = locationPreferences.getTabCount()
        val tabOptions = listOf(
            Pair("1 Location Tab", "Displays a single city window on the main forecast page."),
            Pair("2 Location Tabs (Default)", "Pins two location tabs side-by-side above your weather card."),
            Pair("3 Location Tabs", "Pins three location tabs for quick city switching."),
            Pair("4 Location Tabs", "Pins four location tabs for multi-city monitoring.")
        )

        dialogBinding.rgTabOptions.removeAllViews()

        for (i in 0 until 4) {
            val count = i + 1
            val (title, desc) = tabOptions[i]
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.rgTabOptions, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = title
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = desc
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (currentCount == count)

            optionBinding.cardOption.setOnClickListener {
                locationPreferences.saveTabCount(count)
                setupLocationTabs()
                Toast.makeText(this, "Set to $count Location Tabs", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            dialogBinding.rgTabOptions.addView(optionBinding.root)
        }

        dialog.show()
    }

    private fun showModelsSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogModelsSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.cardAutoChina.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAutoChina.strokeColor = palette.dividerColor

        dialogBinding.btnBack.setOnClickListener {
            dialog.dismiss()
        }

        val savedModel = locationPreferences.getSavedForecastModel()

        // Populate Western Models
        dialogBinding.rgWesternModels.removeAllViews()
        val westernModels = ForecastModel.values().filter { it.isWestern }
        for (model in westernModels) {
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.rgWesternModels, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = model.displayName
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = model.description
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (savedModel == model)

            optionBinding.cardOption.setOnClickListener {
                locationPreferences.saveForecastModel(model)
                Toast.makeText(this, "Default model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadWeather()
            }
            dialogBinding.rgWesternModels.addView(optionBinding.root)
        }

        // Populate Chinese Models
        dialogBinding.rgChineseModels.removeAllViews()
        val chineseModels = ForecastModel.values().filter { !it.isWestern }
        for (model in chineseModels) {
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.rgChineseModels, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = model.displayName
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = model.description
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (savedModel == model)

            optionBinding.cardOption.setOnClickListener {
                locationPreferences.saveForecastModel(model)
                Toast.makeText(this, "Default model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadWeather()
            }
            dialogBinding.rgChineseModels.addView(optionBinding.root)
        }

        // Auto China Switch
        dialogBinding.switchAutoChina.isChecked = locationPreferences.isAutoCmaChinaEnabled()
        dialogBinding.switchAutoChina.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveAutoCmaChinaEnabled(isChecked)
            loadWeather()
        }

        dialog.show()
    }

    private fun showWeatherAlertsSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogAlertsSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.cardAlertsSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAlertsSwitch.strokeColor = palette.dividerColor
        dialogBinding.cardSolarLeadTime.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSolarLeadTime.strokeColor = palette.dividerColor
        dialogBinding.cardTestAlert.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardTestAlert.strokeColor = palette.dividerColor
        dialogBinding.cardAlertInfo.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialogBinding.tvAlertsLocationInfo.text = "Notifications are strictly limited to your active location.\nCurrently monitoring: ${currentLocation.displayName}."

        dialogBinding.etSolarLeadMins.setText(locationPreferences.getSolarAlarmLeadMinutes().toString())
        dialogBinding.etSolarLeadMins.addTextChangedListener { editable ->
            val mins = editable?.toString()?.toIntOrNull() ?: 10
            locationPreferences.saveSolarAlarmLeadMinutes(mins)
        }

        val isEnabled = locationPreferences.isWeatherAlertsEnabled()
        dialogBinding.switchAlerts.isChecked = isEnabled

        dialogBinding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveWeatherAlertsEnabled(isChecked)
            if (isChecked) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
                WeatherNotificationHelper.createNotificationChannel(this)
                Toast.makeText(this, "Weather alerts enabled for ${currentLocation.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Weather notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnSendTestAlert.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }

            val capAlert = CapAlertMessage(
                identifier = "urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-${System.currentTimeMillis()}",
                sender = "cap-pac@ec.gc.ca",
                status = "Actual",
                msgType = "Alert",
                scope = "Public",
                urgency = "Immediate",
                severity = "Extreme",
                certainty = "Observed",
                event = "Tornado Warning & Severe Thunderstorm Watch",
                headline = "Tornado Warning in effect for ${currentLocation.displayName}",
                description = "Environment and Climate Change Canada (ECCC) Doppler radar has detected a severe thunderstorm capable of producing a tornado near ${currentLocation.displayName}. Damaging wind gusts up to 110 km/h, large nickel-sized hail, and intense local rainfall are imminent.",
                instruction = "Take cover immediately if threatening weather approaches. In the event of a tornado, go indoors to a room on the lowest floor, away from outside walls and windows. Leave mobile homes, vehicles, or outdoor structures.",
                areaDesc = "${currentLocation.displayName} and surrounding area"
            )

            WeatherNotificationHelper.sendEcccCapAlertNotification(this, capAlert)

            // Save JSON & update main screen alert banner strictly for currentLocation
            val json = gson.toJson(capAlert)
            locationPreferences.saveActiveAlertJsonForLocation(currentLocation, json)
            updateAlertBanner()

            Toast.makeText(this, "🚨 ECCC CAP-CP Weather Alert sent & displayed on main screen!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showCacheSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogCacheSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardCacheSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardCacheSwitch.strokeColor = palette.dividerColor
        dialogBinding.cardTtlReasoning.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))
        dialogBinding.cardRateLimiting.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isEnabled = locationPreferences.isCacheTtlEnabled()
        val currentTtl = locationPreferences.getCacheTtlMinutes()

        dialogBinding.switchCacheEnabled.isChecked = isEnabled

        fun setTtlOptionsEnabled(enabled: Boolean) {
            dialogBinding.layoutTtlOptions.alpha = if (enabled) 1.0f else 0.4f
            for (i in 0 until dialogBinding.rgTtlDuration.childCount) {
                val child = dialogBinding.rgTtlDuration.getChildAt(i)
                child.isEnabled = enabled
                child.isClickable = enabled
            }
        }

        val ttlOptions = listOf(
            Pair("15 Minutes", 15),
            Pair("30 Minutes", 30),
            Pair("60 Minutes", 60),
            Pair("2 Hours (120 Minutes) (Default)", 120),
            Pair("6 Hours (360 Minutes)", 360)
        )

        dialogBinding.rgTtlDuration.removeAllViews()

        for ((label, minutes) in ttlOptions) {
            val optionBinding = ItemSettingsOptionBinding.inflate(layoutInflater, dialogBinding.rgTtlDuration, false)
            optionBinding.cardOption.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            optionBinding.cardOption.strokeColor = palette.dividerColor
            optionBinding.tvOptionTitle.text = label
            optionBinding.tvOptionTitle.setTextColor(palette.textPrimary)
            optionBinding.tvOptionDesc.text = "Preserves forecast responses for $minutes minutes when navigating between cities."
            optionBinding.tvOptionDesc.setTextColor(palette.textSecondary)
            optionBinding.rbOption.isChecked = (currentTtl == minutes)

            optionBinding.cardOption.setOnClickListener {
                if (locationPreferences.isCacheTtlEnabled()) {
                    locationPreferences.saveCacheTtlMinutes(minutes)
                    Toast.makeText(this, "Cache TTL set to $label", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }

            dialogBinding.rgTtlDuration.addView(optionBinding.root)
        }

        setTtlOptionsEnabled(isEnabled)

        dialogBinding.switchCacheEnabled.setOnCheckedChangeListener { _, checked ->
            locationPreferences.saveCacheTtlEnabled(checked)
            setTtlOptionsEnabled(checked)
            val msg = if (checked) "Cache enabled ($currentTtl min TTL)" else "Cache disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showLicensesSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogLicensesSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense1.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense1.strokeColor = palette.dividerColor
        dialogBinding.cardLicense2.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense2.strokeColor = palette.dividerColor
        dialogBinding.cardLicense3.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense3.strokeColor = palette.dividerColor
        dialogBinding.cardLicense4.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense4.strokeColor = palette.dividerColor
        dialogBinding.cardLicense5.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardLicense5.strokeColor = palette.dividerColor

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showPrivacySettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogPrivacySettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardPrivacyBox.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardPrivacyBox.strokeColor = palette.dividerColor

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val mdText = try {
            assets.open("privacypolicy.md").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Privacy Policy could not be loaded."
        }

        val brandHex = String.format("#%06X", 0xFFFFFF and palette.primary)
        val primaryHex = String.format("#%06X", 0xFFFFFF and palette.textPrimary)

        dialogBinding.tvPrivacyPolicyContent.text = renderMarkdownToSpannable(mdText, primaryHex, brandHex)

        dialog.show()
    }

    private fun renderMarkdownToSpannable(mdText: String, primaryHex: String, brandHex: String): CharSequence {
        val html = mdText
            .replace(Regex("(?m)^# (.*)$"), "<font color='$brandHex'><h2><b>$1</b></h2></font>")
            .replace(Regex("(?m)^## (.*)$"), "<font color='$primaryHex'><h3><b>$1</b></h3></font>")
            .replace(Regex("(?m)^### (.*)$"), "<font color='$primaryHex'><h4><b>$1</b></h4></font>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace("\n\n", "<br/><br/>")
            .replace("\n", "<br/>")

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(html)
        }
    }

    private fun showAboutSettingsScreen() {
        val dialog = Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogAboutSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)
        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAboutBanner.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAboutBanner.strokeColor = palette.primary
        dialogBinding.cardAbout1.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAbout1.strokeColor = palette.dividerColor
        dialogBinding.cardAbout2.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAbout2.strokeColor = palette.dividerColor
        dialogBinding.cardAbout3.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAbout3.strokeColor = palette.dividerColor
        dialogBinding.cardAbout4.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAbout4.strokeColor = palette.dividerColor
        dialogBinding.cardAbout5.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAbout5.strokeColor = palette.dividerColor

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val versionName = try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "1.2.0"
        } catch (e: Exception) {
            "1.2.0"
        }

        dialogBinding.tvAboutAppVersion.text = "WeatherAPP v$versionName"

        dialog.show()
    }

    private fun showRecentLocationsDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogRecentLocationsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        dialogBinding.root.setBackgroundColor(palette.surfaceCard)

        val recentList = locationPreferences.getRecentLocations()
        val fixedDefault = locationPreferences.getFixedDefaultLocation()

        dialogBinding.layoutRecentList.removeAllViews()

        for (item in recentList) {
            val itemBinding = ItemRecentLocationBinding.inflate(layoutInflater, dialogBinding.layoutRecentList, false)

            val isDefault = item.displayName.equals(fixedDefault.displayName, ignoreCase = true)
            val isActive = item.displayName.equals(currentLocation.displayName, ignoreCase = true)

            itemBinding.cardRecentItem.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))
            itemBinding.cardRecentItem.strokeColor = palette.dividerColor
            itemBinding.tvRecentCityName.text = item.name
            itemBinding.tvRecentCityName.setTextColor(palette.textPrimary)
            itemBinding.tvRecentCityRegion.text = item.regionAndCountryText
            itemBinding.tvRecentCityRegion.setTextColor(palette.textSecondary)

            if (isDefault) {
                itemBinding.tvRecentCityName.setTypeface(null, Typeface.BOLD)
                itemBinding.tvDefaultBadge.visibility = View.VISIBLE
            } else {
                itemBinding.tvRecentCityName.setTypeface(null, Typeface.NORMAL)
                itemBinding.tvDefaultBadge.visibility = View.GONE
            }

            itemBinding.ivActiveCheck.visibility = if (isActive) View.VISIBLE else View.GONE

            itemBinding.cardRecentItem.setOnClickListener {
                currentLocation = item
                updateActiveTabCity(item)
                locationPreferences.saveLastUsedLocation(item)
                updateLocationHeader()
                loadWeather()
                dialog.dismiss()
            }

            dialogBinding.layoutRecentList.addView(itemBinding.root)
        }

        dialogBinding.btnSearchNewCity.setOnClickListener {
            dialog.dismiss()
            showSearchLocationDialog()
        }

        dialog.show()
    }

    private fun showDefaultLocationSettingsScreen() {
        val startupMode = locationPreferences.getStartupMode()
        val fixedDefault = locationPreferences.getFixedDefaultLocation()

        val options = listOf(
            SettingsOption(
                title = "Last Used Location",
                description = "Reopen WeatherAPP into whichever city was last viewed when closing the app.",
                isSelected = startupMode == LocationPreferences.STARTUP_LAST_USED,
                onClick = {
                    locationPreferences.saveStartupMode(LocationPreferences.STARTUP_LAST_USED)
                    Toast.makeText(this, "App will open into last used location", Toast.LENGTH_SHORT).show()
                }
            ),
            SettingsOption(
                title = "Fixed Default Location",
                description = "Always open WeatherAPP into '${fixedDefault.displayName}'.",
                isSelected = startupMode == LocationPreferences.STARTUP_FIXED_DEFAULT,
                onClick = {
                    locationPreferences.saveStartupMode(LocationPreferences.STARTUP_FIXED_DEFAULT)
                    Toast.makeText(this, "App will open into ${fixedDefault.name}", Toast.LENGTH_SHORT).show()
                }
            )
        )

        showFullscreenSettingsScreen(
            title = "Default Location",
            subtitle = "Choose what location loads when opening WeatherAPP.",
            options = options,
            actionButtonText = "Set Current City (${currentLocation.name}) as Default",
            onActionClick = {
                locationPreferences.saveStartupMode(LocationPreferences.STARTUP_FIXED_DEFAULT)
                locationPreferences.saveFixedDefaultLocation(currentLocation)
                Toast.makeText(this, "Default location set to ${currentLocation.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun applyTheme(themeMode: Int) {
        val currentTheme = locationPreferences.getSavedThemeMode()
        if (themeMode != currentTheme) {
            locationPreferences.saveThemeMode(themeMode)
            locationPreferences.setPendingReopenThemeSettings(true)

            val nightMode = when (themeMode) {
                LocationPreferences.THEME_LIGHT, LocationPreferences.THEME_CREMA, LocationPreferences.THEME_TERRACOTTA, LocationPreferences.THEME_FROST, LocationPreferences.THEME_FIRE_ENGINE, LocationPreferences.THEME_ORANGE_GROVE -> AppCompatDelegate.MODE_NIGHT_NO
                LocationPreferences.THEME_DARK, LocationPreferences.THEME_CHARCOAL, LocationPreferences.THEME_COBALT, LocationPreferences.THEME_JADE, LocationPreferences.THEME_AURORA -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            delegate.applyDayNight()

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_REOPEN_THEME_SETTINGS, true)
            }
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun applyUnitSystem(selectedUnits: UnitSystem) {
        if (selectedUnits != unitSystem) {
            unitSystem = selectedUnits
            locationPreferences.saveUnitSystem(selectedUnits)
            forecastAdapter.setUnitSystem(selectedUnits)
            selectedDayForecast?.let { updateDetailCard(it) }
        }
    }

    private fun updateLocationHeader() {
        binding.tvLocationName.text = "${currentLocation.displayName} ▾"
    }

    private fun loadWeather(isSwipeRefresh: Boolean = false) {
        if (!isSwipeRefresh) {
            binding.pbLoading.visibility = View.VISIBLE
            binding.layoutError.visibility = View.GONE
        }

        val activeModel = locationPreferences.getActiveModelForLocation(currentLocation)
        binding.tvAttribution.text = activeModel.sourceAttribution

        val cacheEnabled = locationPreferences.isCacheTtlEnabled()
        val ttlMinutes = locationPreferences.getCacheTtlMinutes()

        lifecycleScope.launch {
            val result = repository.getWeatherForecast(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                modelKey = activeModel.openMeteoKey,
                forceRefresh = isSwipeRefresh,
                isCacheEnabled = cacheEnabled,
                ttlMinutes = ttlMinutes,
                locationPreferences = locationPreferences
            )

            binding.pbLoading.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            result.fold(
                onSuccess = { weatherResponse ->
                    binding.layoutError.visibility = View.GONE
                    currentWeatherData = weatherResponse.current
                    locationTimezone = weatherResponse.timezone

                    val todayForecast = repository.processTodayForecast(
                        weatherResponse,
                        currentLocation.latitude,
                        currentLocation.longitude
                    )

                    dailyForecasts = repository.processDailyForecast(
                        weatherResponse,
                        currentLocation.latitude,
                        currentLocation.longitude
                    )

                    forecastAdapter.submitList(dailyForecasts)

                    if (todayForecast != null) {
                        selectedDayForecast = todayForecast
                        updateDetailCard(todayForecast)
                    }

                    applyPaletteToUI()

                    // Dispatch notification strictly for currentLocation if opt-in and severe weather code exists
                    val existingAlertJson = locationPreferences.getActiveAlertJsonForLocation(currentLocation)
                    val isDismissed = locationPreferences.isAlertDismissedForLocation(currentLocation)

                    if (locationPreferences.isWeatherAlertsEnabled()) {
                        val code = currentWeatherData?.weatherCode ?: 0
                        if (code in listOf(95, 96, 99, 65, 75, 82)) {
                            val desc = WeatherCodeUtils.getWeatherDescription(code)
                            val capAlert = CapAlertMessage(
                                identifier = "urn:oid:2.49.0.1.124.0.2026.CAP-CP-ECCC-${System.currentTimeMillis()}",
                                event = desc,
                                headline = "$desc in effect for ${currentLocation.displayName}",
                                description = "Active Environment Canada (GEM/ECCC) severe weather advisory for ${currentLocation.displayName}.",
                                instruction = "Take cover immediately if severe weather approaches. Follow local ECCC emergency weather updates.",
                                areaDesc = currentLocation.displayName
                            )
                            WeatherNotificationHelper.sendEcccCapAlertNotification(this@MainActivity, capAlert)
                            locationPreferences.saveActiveAlertJsonForLocation(currentLocation, gson.toJson(capAlert))
                        } else if (existingAlertJson.isNullOrEmpty() || isDismissed) {
                            // Only clear if no existing active alert or if user dismissed it
                            locationPreferences.saveActiveAlertJsonForLocation(currentLocation, null)
                        }
                    } else if (existingAlertJson.isNullOrEmpty() || isDismissed) {
                        locationPreferences.saveActiveAlertJsonForLocation(currentLocation, null)
                    }
                    updateAlertBanner()

                    // Refresh inline sub-tab if active
                    if (activeInlineSubTab != InlineSubTab.WEATHER_DETAILS) {
                        switchInlineSubTab(activeInlineSubTab)
                    }
                },
                onFailure = { error ->
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = "Failed to load weather data.\nPlease check connection."
                    Toast.makeText(this@MainActivity, "Error: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun updateDetailCard(day: DayForecast) {
        val isToday = day.dayOfWeek.equals("Today", ignoreCase = true)
        val baseDateStr = "${day.dayOfWeek.uppercase(Locale.getDefault())} • ${day.formattedDate.uppercase(Locale.getDefault())}"

        val dateHeader = if (isToday) {
            val is24Hour = DateFormat.is24HourFormat(this)
            val currentTime = UnitUtils.formatLocationTime(locationTimezone, is24Hour)
            "$baseDateStr • $currentTime"
        } else {
            baseDateStr
        }

        binding.tvSelectedDateHeader.text = dateHeader

        binding.tvMainEmoji.text = day.iconEmoji
        binding.tvMainCondition.text = day.weatherDescription

        val currentTemp = currentWeatherData?.temperature
        if (currentTemp != null && isToday) {
            binding.tvCurrentTemp.text = "Current: ${UnitUtils.formatTempPrecise(currentTemp, unitSystem)}"
            binding.tvCurrentTemp.visibility = View.VISIBLE
        } else {
            binding.tvCurrentTemp.visibility = View.GONE
        }

        // Daily High and Low
        binding.tvDailyHigh.text = UnitUtils.formatTemp(day.tempMax, unitSystem)
        binding.tvDailyLow.text = UnitUtils.formatTemp(day.tempMin, unitSystem)

        // 6 Weather Metrics
        val current = currentWeatherData
        val windSpeed = current?.windSpeed ?: day.windSpeedMaxKmH
        val windDir = current?.windDirection
        val windGust = current?.windGusts ?: day.windGustsMaxKmH
        binding.tvWindSpeed.text = UnitUtils.formatWindSpeed(windSpeed, unitSystem, windDir, windGust)

        val aqi = current?.airQualityIndex ?: day.airQualityIndex
        if (aqi != null) {
            binding.tvAirQuality.text = "$aqi • ${UnitUtils.getAqiDescription(aqi)}"
        } else {
            binding.tvAirQuality.text = "Good"
        }

        val uv = current?.uvIndex ?: day.uvIndexMax
        binding.tvUvIndex.text = String.format(Locale.getDefault(), "%.1f • %s", uv, UnitUtils.getUvDescription(uv))

        val visibility = current?.visibilityMeters ?: day.visibilityMeters
        binding.tvVisibility.text = UnitUtils.formatVisibility(visibility, unitSystem)

        val humidity = current?.relativeHumidity?.toInt() ?: day.humidityPct
        binding.tvHumidity.text = "$humidity%"

        val cloudCover = current?.cloudCoverPct?.toInt() ?: day.cloudCoverPct
        binding.tvCeiling.text = UnitUtils.formatCeiling(cloudCover, humidity, unitSystem)

        // Sunrise and Sunset + Azimuth
        val solar = day.solarTimes
        binding.tvSunrise.text = SolarCalculator.formatTime(solar.sunrise)
        binding.tvSunriseAzimuth.text = "Rising at ${solar.sunriseAzimuthDegrees}° (${solar.sunriseCompassDirection})"

        binding.tvSunset.text = SolarCalculator.formatTime(solar.sunset)
        binding.tvSunsetAzimuth.text = "Setting at ${solar.sunsetAzimuthDegrees}° (${solar.sunsetCompassDirection})"

        // Total Daylight and Moon Phase + Cycle Percentage
        binding.tvTotalDaylight.text = solar.daylightDurationText
        binding.tvMoonPhase.text = "--"
        binding.tvMoonCyclePercentage.text = "--"

        // Golden Hour Start and End
        val morningGolden = "${SolarCalculator.formatTime(solar.morningGoldenHourStart)} – ${SolarCalculator.formatTime(solar.morningGoldenHourEnd)}"
        val eveningGolden = "${SolarCalculator.formatTime(solar.eveningGoldenHourStart)} – ${SolarCalculator.formatTime(solar.eveningGoldenHourEnd)}"
        binding.tvMorningGolden.text = morningGolden
        binding.tvEveningGolden.text = eveningGolden

        // Blue Hour Start and End
        val morningBlue = "${SolarCalculator.formatTime(solar.morningBlueHourStart)} – ${SolarCalculator.formatTime(solar.morningBlueHourEnd)}"
        val eveningBlue = "${SolarCalculator.formatTime(solar.eveningBlueHourStart)} – ${SolarCalculator.formatTime(solar.eveningBlueHourEnd)}"
        binding.tvMorningBlue.text = morningBlue
        binding.tvEveningBlue.text = eveningBlue
    }

    private fun showFirstLaunchLocationPrompt() {
        // Weather Alerts Prompt
        MaterialAlertDialogBuilder(this)
            .setTitle("Environment Canada Weather Alerts")
            .setMessage("Would you like to receive system notifications for active severe weather warnings, watches, and advisories from Environment Canada (GEM/ECCC) for your city?\n\nThis application respects your privacy and does NOT use GPS, location services, or device tracking.")
            .setPositiveButton("Enable Alerts") { dialog, _ ->
                locationPreferences.saveWeatherAlertsEnabled(true)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
                WeatherNotificationHelper.createNotificationChannel(this)
                dialog.dismiss()
                showSearchLocationDialog(isFirstLaunchPrompt = true)
            }
            .setNegativeButton("No Thanks") { dialog, _ ->
                locationPreferences.saveWeatherAlertsEnabled(false)
                dialog.dismiss()
                showSearchLocationDialog(isFirstLaunchPrompt = true)
            }
            .setCancelable(false)
            .show()
    }

    private fun showSearchLocationDialog(
        isFirstLaunchPrompt: Boolean = false,
        isSettingDefaultLocation: Boolean = false
    ) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogSearchLocationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val searchAdapter = LocationSearchAdapter { selected ->
            if (isSettingDefaultLocation) {
                locationPreferences.saveStartupMode(LocationPreferences.STARTUP_FIXED_DEFAULT)
                locationPreferences.saveFixedDefaultLocation(selected)
                Toast.makeText(this, "Default startup location set to ${selected.name}", Toast.LENGTH_SHORT).show()
            } else {
                currentLocation = selected
                updateActiveTabCity(selected)
                locationPreferences.saveLastUsedLocation(selected)
                if (isFirstLaunchPrompt) {
                    locationPreferences.saveFixedDefaultLocation(selected)
                    locationPreferences.setFirstLaunchCompleted()
                }
                updateLocationHeader()
                loadWeather()
            }
            dialog.dismiss()
        }

        dialogBinding.rvLocationResults.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvLocationResults.adapter = searchAdapter

        dialogBinding.btnCloseDialog.setOnClickListener {
            if (isFirstLaunchPrompt) {
                locationPreferences.setFirstLaunchCompleted()
            }
            dialog.dismiss()
        }

        var searchJob: Job? = null

        fun performSearch(query: String) {
            searchJob?.cancel()
            if (query.isBlank()) {
                searchAdapter.submitList(emptyList())
                dialogBinding.pbSearchLoading.visibility = View.GONE
                dialogBinding.tvEmptySearch.visibility = View.GONE
                return
            }

            dialogBinding.pbSearchLoading.visibility = View.VISIBLE
            dialogBinding.tvEmptySearch.visibility = View.GONE

            searchJob = lifecycleScope.launch {
                delay(300)
                val result = repository.searchLocations(query)
                dialogBinding.pbSearchLoading.visibility = View.GONE

                result.fold(
                    onSuccess = { locations ->
                        searchAdapter.submitList(locations)
                        if (locations.isEmpty()) {
                            dialogBinding.tvEmptySearch.visibility = View.VISIBLE
                        } else {
                            dialogBinding.tvEmptySearch.visibility = View.GONE
                        }
                    },
                    onFailure = {
                        searchAdapter.submitList(emptyList())
                        dialogBinding.tvEmptySearch.visibility = View.VISIBLE
                        dialogBinding.tvEmptySearch.text = "Error searching location."
                    }
                )
            }
        }

        dialogBinding.etSearchLocation.addTextChangedListener { editable ->
            performSearch(editable?.toString() ?: "")
        }

        dialogBinding.etSearchLocation.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        dialog.show()

        dialogBinding.etSearchLocation.requestFocus()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (activeInlineSubTab != InlineSubTab.WEATHER_DETAILS) {
            switchInlineSubTab(InlineSubTab.WEATHER_DETAILS)
        } else {
            super.onBackPressed()
        }
    }
}
