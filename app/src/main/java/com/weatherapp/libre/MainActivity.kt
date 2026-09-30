package com.weatherapp.libre

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherapp.libre.databinding.ActivityMainBinding
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPreferences: LocationPreferences
    private val repository = WeatherRepository()

    // Decomposed Manager instances
    lateinit var themeManager: ThemeManager
    lateinit var settingsDialogManager: SettingsDialogManager
    lateinit var locationDialogManager: LocationDialogManager
    lateinit var astronomyManager: AstronomyDisplayManager
    lateinit var weatherDisplayManager: WeatherDisplayManager
    lateinit var appUpdateManager: AppUpdateManager
    lateinit var easterEggManager: EasterEggManager

    companion object {
        const val EXTRA_REOPEN_THEME_SETTINGS = "extra_reopen_theme_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        locationPreferences = LocationPreferences(this)
        val savedTheme = locationPreferences.getThemeMode()

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
            binding.main.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

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

        // Initialize Managers
        themeManager = ThemeManager(this, binding, locationPreferences)
        settingsDialogManager = SettingsDialogManager(this, binding, locationPreferences)
        locationDialogManager = LocationDialogManager(this, binding, locationPreferences, repository, lifecycleScope)
        astronomyManager = AstronomyDisplayManager(this, binding, locationPreferences)
        weatherDisplayManager = WeatherDisplayManager(this, binding, locationPreferences, repository, astronomyManager, lifecycleScope)
        appUpdateManager = AppUpdateManager(this, locationPreferences)
        easterEggManager = EasterEggManager(this, binding)

        setupUI()
        setupDrawerNavigation()
        setupLocationTabs()

        themeManager.applyTheme(savedTheme)

        val activeIndex = locationPreferences.getActiveTabIndex()
        val currentLoc = locationPreferences.getTabLocation(activeIndex)
        weatherDisplayManager.currentLocation = currentLoc
        weatherDisplayManager.updateLocationHeader()
        weatherDisplayManager.loadWeather()

        if (locationPreferences.consumePendingReopenThemeSettings() || intent.getBooleanExtra(EXTRA_REOPEN_THEME_SETTINGS, false)) {
            themeManager.showThemeSettingsScreen()
        }

        val launchCount = locationPreferences.incrementAppLaunchCount()
        if (launchCount >= 500) {
            appUpdateManager.show500LaunchMilestoneDialog()
        } else if (launchCount % 5 == 0) {
            appUpdateManager.checkPlayStoreUpdate(silent = true)
        }

        if (locationPreferences.isFirstLaunch()) {
            locationDialogManager.showFirstLaunchLocationPrompt { newLoc ->
                weatherDisplayManager.currentLocation = newLoc
                weatherDisplayManager.updateLocationHeader()
                weatherDisplayManager.loadWeather()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        weatherDisplayManager.updateAlertBanner()
        themeManager.applyTheme(locationPreferences.getThemeMode())
    }

    override fun onDestroy() {
        astronomyManager.cleanup()
        super.onDestroy()
    }

    private fun setupUI() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.layoutLocationClickable.setOnClickListener {
            locationDialogManager.showRecentLocationsDialog { selectedLoc ->
                weatherDisplayManager.currentLocation = selectedLoc
                updateActiveTabCity(selectedLoc)
                weatherDisplayManager.updateLocationHeader()
                weatherDisplayManager.loadWeather()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            weatherDisplayManager.loadWeather(forceRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            weatherDisplayManager.loadWeather()
        }

        // Easter Egg Clicks
        binding.cardHighTemp.setOnClickListener { easterEggManager.handleDailyHighTap() }
        binding.cardLowTemp.setOnClickListener { easterEggManager.handleDailyLowTap() }
        binding.btnDismissHighEffect.setOnClickListener { easterEggManager.stopShimmerEffect() }
        binding.btnDismissLowEffect.setOnClickListener { easterEggManager.stopSnowEffect() }

        // Sub-Tab Navigation Button (Weather ▾ / Golden Hour ▾)
        binding.btnInlineMenu.setOnClickListener { view ->
            showInlineMenu(view)
        }

        // Date Controls for Inline Golden Hour View
        binding.btnInlinePrevDay.setOnClickListener {
            astronomyManager.inlineGoldenDate = astronomyManager.inlineGoldenDate.minusDays(1)
            astronomyManager.updateInlineGoldenHourDisplay()
        }

        binding.btnInlineNextDay.setOnClickListener {
            astronomyManager.inlineGoldenDate = astronomyManager.inlineGoldenDate.plusDays(1)
            astronomyManager.updateInlineGoldenHourDisplay()
        }

        binding.layoutInlineDateClickable.setOnClickListener {
            val currDate = astronomyManager.inlineGoldenDate
            DatePickerDialog(
                this,
                { _, yr, monthIdx, dayOfMonth ->
                    astronomyManager.inlineGoldenDate = LocalDate.of(yr, monthIdx + 1, dayOfMonth)
                    astronomyManager.updateInlineGoldenHourDisplay()
                },
                currDate.year,
                currDate.monthValue - 1,
                currDate.dayOfMonth
            ).show()
        }

        // Adapters
        val unitSystem = locationPreferences.getUnitSystem()
        val forecastAdapter = DayForecastAdapter(unitSystem)
        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        binding.rvForecast.adapter = forecastAdapter
        weatherDisplayManager.forecastAdapter = forecastAdapter
        themeManager.forecastAdapter = forecastAdapter

        val solarAdapter = SolarPhaseAdapter { item ->
            astronomyManager.promptSetSolarAlarm(item)
        }
        binding.rvInlineSolarPhases.layoutManager = LinearLayoutManager(this)
        binding.rvInlineSolarPhases.adapter = solarAdapter
        astronomyManager.inlineSolarPhaseAdapter = solarAdapter
        themeManager.inlineSolarPhaseAdapter = solarAdapter
        
        // Pass initial palette to adapters
        val currentPalette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        forecastAdapter.setPalette(currentPalette)
        solarAdapter.setPalette(currentPalette)
    }

    private fun showInlineMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, "Weather Details")
        popup.menu.add(0, 2, 1, "Golden Hour")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> astronomyManager.switchInlineSubTab(InlineSubTab.WEATHER_DETAILS, weatherDisplayManager.currentLocation, weatherDisplayManager.locationTimezone)
                2 -> astronomyManager.switchInlineSubTab(InlineSubTab.GOLDEN_HOUR, weatherDisplayManager.currentLocation, weatherDisplayManager.locationTimezone)
            }
            true
        }
        popup.show()
    }

    fun setupLocationTabs() {
        val tabCount = locationPreferences.getTabCount()
        binding.tabLayoutLocations.clearOnTabSelectedListeners()
        binding.tabLayoutLocations.removeAllTabs()

        for (i in 0 until tabCount) {
            val loc = locationPreferences.getTabLocation(i)
            val tab = binding.tabLayoutLocations.newTab().setText(loc.name)
            binding.tabLayoutLocations.addTab(tab)
        }

        val activeIndex = locationPreferences.getActiveTabIndex().coerceIn(0, tabCount - 1)
        binding.tabLayoutLocations.getTabAt(activeIndex)?.select()

        binding.tabLayoutLocations.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                val pos = tab.position
                locationPreferences.saveActiveTabIndex(pos)
                val selectedLoc = locationPreferences.getTabLocation(pos)
                weatherDisplayManager.currentLocation = selectedLoc
                weatherDisplayManager.updateLocationHeader()
                weatherDisplayManager.loadWeather()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    fun updateActiveTabCity(location: LocationItem) {
        val activeIndex = locationPreferences.getActiveTabIndex()
        locationPreferences.saveTabLocation(activeIndex, location)
        setupLocationTabs()
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.menu_item_theme -> themeManager.showThemeSettingsScreen()
                R.id.menu_item_tabs -> settingsDialogManager.showLocationTabsSettingsScreen()
                R.id.menu_item_models -> settingsDialogManager.showModelsSettingsScreen(weatherDisplayManager)
                R.id.menu_item_alerts -> settingsDialogManager.showWeatherAlertsSettingsScreen()
                R.id.menu_item_cache -> settingsDialogManager.showCacheSettingsScreen()
                R.id.menu_item_default_location -> settingsDialogManager.showDefaultLocationSettingsScreen(locationDialogManager)
                R.id.menu_item_licenses -> settingsDialogManager.showLicensesSettingsScreen()
                R.id.menu_item_privacy -> settingsDialogManager.showPrivacySettingsScreen()
                R.id.menu_item_about -> settingsDialogManager.showAboutSettingsScreen()
                R.id.menu_item_roadmap -> settingsDialogManager.showRoadmapSettingsScreen()
                R.id.menu_item_donate -> openUrl("https://ko-fi.com/adamjeremy")
                R.id.menu_item_rate -> openUrl("https://play.google.com/store/apps/details?id=$packageName")
            }
            true
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (astronomyManager.onKeyDown(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (astronomyManager.onKeyUp(keyCode, event)) return true
        return super.onKeyUp(keyCode, event)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else if (astronomyManager.activeInlineSubTab != InlineSubTab.WEATHER_DETAILS) {
            astronomyManager.switchInlineSubTab(InlineSubTab.WEATHER_DETAILS, weatherDisplayManager.currentLocation, weatherDisplayManager.locationTimezone)
        } else {
            super.onBackPressed()
        }
    }
}
