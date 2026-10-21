package com.weatherapp.libre

import com.weatherapp.theme.BoutiqueThemeEngine
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherapp.libre.alerts.AlertManager
import com.weatherapp.libre.alerts.AlertBannerManager
import com.weatherapp.libre.alerts.AlertConsentDialog
import com.weatherapp.libre.BuildConfig
import com.weatherapp.libre.databinding.ActivityMainBinding
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var locationPreferences: LocationPreferences
    val repository = WeatherRepository()

    // Decomposed Manager instances
    lateinit var themeManager: ThemeManager
    lateinit var settingsDialogManager: SettingsDialogManager
    lateinit var locationDialogManager: LocationDialogManager
    lateinit var astronomyManager: AstronomyDisplayManager
    lateinit var weatherDisplayManager: WeatherDisplayManager
    lateinit var appUpdateManager: AppUpdateManager
    lateinit var easterEggManager: EasterEggManager
    lateinit var alertManager: AlertManager
    lateinit var alertBannerManager: AlertBannerManager

    companion object {
        const val EXTRA_REOPEN_THEME_SETTINGS = "extra_reopen_theme_settings"

        fun shouldInterceptBack(menuDepth: Int, drawerOpen: Boolean, subTab: InlineSubTab): Boolean =
            menuDepth > 0 || drawerOpen || subTab != InlineSubTab.CURRENT

        fun shouldReloadAfterTabEdit(pressedIndex: Int, activeIndex: Int): Boolean =
            pressedIndex == activeIndex
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        locationPreferences = LocationPreferences(this)
        val savedTheme = locationPreferences.getThemeMode()

        val nightMode = when (savedTheme) {
            LocationPreferences.THEME_LIGHT, LocationPreferences.THEME_CREMA, LocationPreferences.THEME_TERRACOTTA, LocationPreferences.THEME_FROST, LocationPreferences.THEME_ORANGE_GROVE, LocationPreferences.THEME_FRUTIGER_AERO -> AppCompatDelegate.MODE_NIGHT_NO
            LocationPreferences.THEME_DARK, LocationPreferences.THEME_CHARCOAL, LocationPreferences.THEME_COBALT, LocationPreferences.THEME_JADE, LocationPreferences.THEME_AURORA, LocationPreferences.THEME_HARDY_HERON -> AppCompatDelegate.MODE_NIGHT_YES
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
            binding.drawerContainer.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        locationPreferences.checkAndPerformVersionMigration(BuildConfig.VERSION_CODE, repository)

        alertManager = AlertManager(applicationContext, locationPreferences)
        alertBannerManager = AlertBannerManager(this, binding, locationPreferences, alertManager)

        // Initialize Managers
        themeManager = ThemeManager(this, binding, locationPreferences)
        settingsDialogManager = SettingsDialogManager(this, binding, locationPreferences)
        locationDialogManager = LocationDialogManager(this, binding, locationPreferences, repository, lifecycleScope)
        astronomyManager = AstronomyDisplayManager(this, binding, locationPreferences)
        weatherDisplayManager = WeatherDisplayManager(this, binding, locationPreferences, repository, astronomyManager, lifecycleScope)
        weatherDisplayManager.alertBannerManager = alertBannerManager
        appUpdateManager = AppUpdateManager(this, locationPreferences)
        easterEggManager = EasterEggManager(this, binding)

        WidgetSyncWorker.notifyWidgetsInvalidated(this)

        setupUI()
        val isFreshLaunch = (savedInstanceState == null)
        astronomyManager.applyInitialLaunchState(isFreshLaunch)
        setupDrawerNavigation()

        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val menuDepth = settingsDialogManager.getMenuStackDepth()
                val drawerOpen = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
                val subTab = astronomyManager.activeInlineSubTab
                if (shouldInterceptBack(menuDepth, drawerOpen, subTab)) {
                    if (menuDepth > 0) {
                        settingsDialogManager.popTopMenu()
                    } else if (drawerOpen) {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        astronomyManager.switchInlineSubTab(
                            InlineSubTab.CURRENT,
                            weatherDisplayManager.currentLocation,
                            weatherDisplayManager.locationTimezone
                        )
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        locationPreferences.migrateLegacyConsentState()
        AlertConsentDialog.checkAndShowConsentPrompt(this, locationPreferences)

        themeManager.applyTheme(savedTheme)

        if (locationPreferences.isFirstLaunch()) {
            locationPreferences.saveTabCount(3)
            locationPreferences.saveTabLocation(0, LocationPreferences.DEFAULT_EDMONTON)
            locationPreferences.saveTabLocation(1, LocationPreferences.DEFAULT_MONTREAL)
            locationPreferences.saveTabLocation(2, LocationPreferences.DEFAULT_HALIFAX)
            locationPreferences.setFirstLaunchCompleted()
        }

        setupLocationTabs()

        handleNotificationIntent(intent)
        val handledWidgetIntent = handleWidgetIntent(intent)

        if (!handledWidgetIntent) {
            val activeIndex = locationPreferences.getActiveTabIndex()
            val currentLoc = locationPreferences.getTabLocation(activeIndex)
            weatherDisplayManager.currentLocation = currentLoc
            weatherDisplayManager.updateLocationHeader()
            weatherDisplayManager.loadWeather()
        }

        if (locationPreferences.consumePendingReopenThemeSettings() || intent.getBooleanExtra(EXTRA_REOPEN_THEME_SETTINGS, false)) {
            themeManager.showThemeSettingsScreen()
        }

        val launchCount = locationPreferences.incrementAppLaunchCount()
        if (launchCount >= 500) {
            appUpdateManager.show500LaunchMilestoneDialog()
        } else {
            appUpdateManager.checkPlayStoreUpdate(silent = true)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
        handleWidgetIntent(intent)
    }

    fun handleWidgetIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        if (!intent.hasExtra(WeatherWidgetProvider.EXTRA_WIDGET_TAB_INDEX)) return false

        val tabIndex = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TAB_INDEX, -1)
        val tabCount = locationPreferences.getTabCount()
        var handled = false
        if (tabIndex in 0 until tabCount) {
            locationPreferences.saveActiveTabIndex(tabIndex)
            binding.tabLayoutLocations.getTabAt(tabIndex)?.select()
            val selectedLoc = locationPreferences.getTabLocation(tabIndex)
            weatherDisplayManager.currentLocation = selectedLoc
            weatherDisplayManager.updateLocationHeader()
            weatherDisplayManager.loadWeather()
            handled = true
        }

        val subTabName = intent.getStringExtra(WeatherWidgetProvider.EXTRA_WIDGET_SUBTAB)
        if (!subTabName.isNullOrEmpty()) {
            try {
                val subTab = InlineSubTab.valueOf(subTabName)
                astronomyManager.switchInlineSubTab(
                    subTab,
                    weatherDisplayManager.currentLocation,
                    weatherDisplayManager.locationTimezone
                )
            } catch (e: Exception) {
                // Ignore unknown subtab
            }
        }

        intent.removeExtra(WeatherWidgetProvider.EXTRA_WIDGET_TAB_INDEX)
        intent.removeExtra(WeatherWidgetProvider.EXTRA_WIDGET_SUBTAB)
        return handled
    }

    fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val locationKey = intent.getStringExtra("EXTRA_LOCATION_KEY") ?: return
        intent.removeExtra("EXTRA_LOCATION_KEY")

        val tabCount = locationPreferences.getTabCount()
        for (i in 0 until tabCount) {
            val tabLoc = locationPreferences.getTabLocation(i)
            if (com.weatherapp.libre.alerts.AlertCache.cacheKey(tabLoc).equals(locationKey, ignoreCase = true)) {
                locationPreferences.saveActiveTabIndex(i)
                binding.tabLayoutLocations.getTabAt(i)?.select()
                break
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStickyTabsPosition()
        weatherDisplayManager.updateAlertBanner()
        themeManager.applyTheme(locationPreferences.getThemeMode())
    }

    fun updateStickyTabsPosition() {
        val isSticky = locationPreferences.isLocationTabsSticky()
        val cardTabs = binding.cardLocationTabs
        val stickyContainer = binding.layoutStickyTabsContainer
        val scrollableContainer = binding.layoutScrollableTabsContainer

        val targetParent = if (isSticky) stickyContainer else scrollableContainer

        if (cardTabs.parent != targetParent) {
            (cardTabs.parent as? ViewGroup)?.removeView(cardTabs)
            targetParent.addView(cardTabs)
        }

        stickyContainer.visibility = if (isSticky) View.VISIBLE else View.GONE
        scrollableContainer.visibility = if (isSticky) View.GONE else View.VISIBLE
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

        binding.btnDismissError.setOnClickListener {
            binding.layoutError.visibility = View.GONE
        }

        // Easter Egg Clicks
        findViewById<android.view.View>(R.id.cardHighTemp)?.setOnClickListener { easterEggManager.handleDailyHighTap() }
        findViewById<android.view.View>(R.id.cardLowTemp)?.setOnClickListener { easterEggManager.handleDailyLowTap() }
        findViewById<android.view.View>(R.id.btnDismissHighEffect)?.setOnClickListener { easterEggManager.stopShimmerEffect() }
        findViewById<android.view.View>(R.id.btnDismissLowEffect)?.setOnClickListener { easterEggManager.stopSnowEffect() }

        // Sub-Tab Navigation Button (Weather ▾ / Golden Hour ▾)
        binding.btnInlineMenu.setOnClickListener { view ->
            showInlineMenu(view)
        }

        // Date Controls for Inline Golden Hour View
        findViewById<android.view.View>(R.id.btnInlinePrevDay)?.setOnClickListener {
            astronomyManager.inlineGoldenDate = astronomyManager.inlineGoldenDate.minusDays(1)
            astronomyManager.updateInlineGoldenHourDisplay()
        }

        findViewById<android.view.View>(R.id.btnInlineNextDay)?.setOnClickListener {
            astronomyManager.inlineGoldenDate = astronomyManager.inlineGoldenDate.plusDays(1)
            astronomyManager.updateInlineGoldenHourDisplay()
        }

        findViewById<android.view.View>(R.id.layoutInlineDateClickable)?.setOnClickListener {
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
        val metricGridAdapter = MetricGridAdapter()
        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        binding.rvForecast.adapter = forecastAdapter
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSevenDayMenu)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = forecastAdapter
        }
        weatherDisplayManager.forecastAdapter = forecastAdapter
        weatherDisplayManager.metricGridAdapter = metricGridAdapter
        themeManager.forecastAdapter = forecastAdapter
        themeManager.metricGridAdapter = metricGridAdapter

        val solarAdapter = SolarPhaseAdapter { item -> astronomyManager.promptSetSolarAlarm(item) }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvInlineSolarPhases)?.layoutManager = LinearLayoutManager(this)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvInlineSolarPhases)?.adapter = solarAdapter
        astronomyManager.inlineSolarPhaseAdapter = solarAdapter
        themeManager.inlineSolarPhaseAdapter = solarAdapter

        astronomyManager.setupUI()

        val hourlyDisplayManager = HourlyDisplayManager(this, locationPreferences)
        hourlyDisplayManager.setupRecyclerView()
        weatherDisplayManager.hourlyDisplayManager = hourlyDisplayManager
        astronomyManager.hourlyDisplayManager = hourlyDisplayManager
        themeManager.hourlyForecastAdapter = hourlyDisplayManager.hourlyAdapter

        // Pass initial palette to adapters
        val currentPalette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        forecastAdapter.setPalette(currentPalette)
        solarAdapter.setPalette(currentPalette)
        hourlyDisplayManager.setPalette(currentPalette)
    }

    private fun showInlineMenu(anchorView: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchorView)
        popup.menu.setGroupDividerEnabled(true)

        val showSevenDayInMenu = locationPreferences.getSevenDayAsMenuItem()
        val order = locationPreferences.getMenuOrder()
        val enabledFlags = locationPreferences.getMenuEnabledFlags()

        var currentGroupId = 0
        var orderIndex = 0

        for (itemKey in order) {
            if (itemKey == "SEPARATOR") {
                currentGroupId++
                continue
            }

            val subTab = try {
                InlineSubTab.valueOf(itemKey)
            } catch (e: Exception) {
                continue
            }

            if (subTab == InlineSubTab.SEVEN_DAY && !showSevenDayInMenu) {
                continue
            }

            if (enabledFlags[subTab] == true) {
                val label = when (subTab) {
                    InlineSubTab.CURRENT -> getString(R.string.subtab_current_weather)
                    InlineSubTab.HOURLY -> getString(R.string.subtab_hourly_dropdown)
                    InlineSubTab.SEVEN_DAY -> getString(R.string.title_seven_day_forecast)
                    InlineSubTab.NATURAL_LIGHT -> getString(R.string.subtab_natural_light_dropdown)
                    InlineSubTab.DARK_SKY -> getString(R.string.subtab_dark_sky_dropdown)
                    InlineSubTab.NIGHT_VISIBILITY -> getString(R.string.subtab_night_vis_dropdown)
                    InlineSubTab.SOLUNAR -> getString(R.string.subtab_solunar_dropdown)
                    InlineSubTab.LUNAR_CALENDAR -> getString(R.string.title_lunar_calendar)
                }
                popup.menu.add(currentGroupId, subTab.ordinal, orderIndex, label)
                orderIndex++
            }
        }

        popup.setOnMenuItemClickListener { item ->
            val selectedSubTab = InlineSubTab.entries.getOrNull(item.itemId) ?: InlineSubTab.CURRENT
            astronomyManager.switchInlineSubTab(
                selectedSubTab,
                weatherDisplayManager.currentLocation,
                weatherDisplayManager.locationTimezone
            )
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
            tab.view.setOnLongClickListener {
                locationDialogManager.showRecentLocationsDialog { selectedLoc ->
                    locationPreferences.saveTabLocation(i, selectedLoc)
                    tab.text = selectedLoc.name
                    WidgetSyncWorker.notifyWidgetsInvalidated(this@MainActivity)
                    if (shouldReloadAfterTabEdit(i, locationPreferences.getActiveTabIndex())) {
                        weatherDisplayManager.currentLocation = selectedLoc
                        weatherDisplayManager.updateLocationHeader()
                        weatherDisplayManager.loadWeather()
                    }
                }
                true
            }
        }

        val activeIndex = locationPreferences.getActiveTabIndex().coerceIn(0, tabCount - 1)
        binding.tabLayoutLocations.getTabAt(activeIndex)?.select()

        updateStickyTabsPosition()

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
        WidgetSyncWorker.notifyWidgetsInvalidated(this)
    }

    private fun setupDrawerNavigation() {
        binding.rowTheme.setOnClickListener {
            themeManager.showThemeSettingsScreen()
        }
        binding.rowSettings.setOnClickListener {
            settingsDialogManager.showSystemSettingsScreen(weatherDisplayManager, locationDialogManager)
        }
        binding.rowInfo.setOnClickListener {
            settingsDialogManager.showInfoSettingsScreen()
        }
        binding.rowDonate.setOnClickListener {
            settingsDialogManager.showDonateSettingsScreen()
        }
        binding.rowRate.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            openUrl("https://play.google.com/store/apps/details?id=$packageName")
        }
        binding.rowShare.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            settingsDialogManager.shareProject()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(applicationContext, getString(R.string.toast_could_not_open_link), Toast.LENGTH_SHORT).show()
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
}
