package com.weatherapp.libre

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogThemeSettingsBinding

class ThemeManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences,
    private val repository: WeatherRepository = WeatherRepository()
) {

    var forecastAdapter: DayForecastAdapter? = null
    var metricGridAdapter: MetricGridAdapter? = null
    var inlineSolarPhaseAdapter: SolarPhaseAdapter? = null
    var hourlyForecastAdapter: HourlyForecastAdapter? = null

    fun applyTheme(themeMode: Int) {
        val palette = ColorPalette.forMode(themeMode, activity)
        applyPaletteToUI(palette)
    }

    fun applyPaletteToUI(palette: ColorPalette) {
        binding.drawerLayout.setBackgroundColor(palette.background)
        binding.main.setBackgroundColor(palette.background)
        binding.layoutStickyTabsContainer.setBackgroundColor(palette.background)

        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val density = activity.resources.displayMetrics.density

        val drawerDividers = listOf(
            binding.drawerDivider1,
            binding.drawerDivider2,
            binding.drawerDivider3
        )
        val drawerTitles = listOf(
            binding.tvSettingsTitle,
            binding.tvThemeTitle,
            binding.tvInfoTitle,
            binding.tvDonateTitle,
            binding.tvRateTitle,
            binding.tvShareTitle
        )
        val drawerSnippets = listOf(
            binding.tvSettingsSnippet,
            binding.tvThemeSnippet,
            binding.tvInfoSnippet,
            binding.tvDonateSnippet,
            binding.tvRateSnippet,
            binding.tvShareSnippet
        )

        if (isFireEngine) {
            binding.drawerScrollView.setBackgroundColor(Color.parseColor("#DC2626"))
            binding.tvDrawerTitle.setTextColor(Color.WHITE)
            for (divider in drawerDividers) divider.setBackgroundColor(Color.WHITE)
            for (tv in drawerTitles) tv.setTextColor(Color.WHITE)
            for (tv in drawerSnippets) tv.setTextColor(Color.parseColor("#FEE2E2"))
        } else {
            binding.drawerScrollView.setBackgroundColor(palette.surfaceCard)
            binding.tvDrawerTitle.setTextColor(palette.textPrimary)
            for (divider in drawerDividers) divider.setBackgroundColor(palette.dividerColor)
            for (tv in drawerTitles) tv.setTextColor(palette.textPrimary)
            for (tv in drawerSnippets) tv.setTextColor(palette.textSecondary)
        }

        if (isFireEngine) {
            binding.cardHeader.setCardBackgroundColor(Color.parseColor("#DC2626"))
            binding.cardHeader.strokeColor = Color.WHITE
            binding.cardHeader.strokeWidth = (2 * density).toInt()
            binding.btnMenu.setColorFilter(Color.WHITE)
            binding.ivPin.setColorFilter(Color.WHITE)
            binding.tvLocationName.setTextColor(Color.WHITE)
            binding.tvAttribution.setTextColor(Color.parseColor("#FEE2E2"))
        } else {
            binding.cardHeader.setCardBackgroundColor(palette.surfaceCard)
            binding.cardHeader.strokeColor = palette.dividerColor
            binding.cardHeader.strokeWidth = (1 * density).toInt()
            binding.btnMenu.setColorFilter(palette.textPrimary)
            binding.ivPin.setColorFilter(palette.brandPrimary)
            binding.tvLocationName.setTextColor(palette.textPrimary)
            binding.tvAttribution.setTextColor(palette.textSecondary)
        }

        binding.cardLocationTabs.setCardBackgroundColor(palette.surfaceCard)
        binding.cardLocationTabs.strokeColor = palette.dividerColor
        binding.tabLayoutLocations.setSelectedTabIndicatorColor(palette.brandPrimary)
        binding.tabLayoutLocations.setTabTextColors(palette.textSecondary, palette.brandPrimary)



        binding.cardTodayDetails.setCardBackgroundColor(palette.surfaceCard)
        binding.cardTodayDetails.strokeColor = palette.dividerColor

        binding.tvSelectedDateHeader.setTextColor(palette.textSecondary)
        binding.tvMainCondition.setTextColor(palette.textPrimary)
        binding.tvCurrentTemp.setTextColor(palette.textPrimary)
        binding.tvFeelsLike.setTextColor(palette.textSecondary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivMainConditionIcon)?.setColorFilter(palette.brandPrimary)

        binding.root.findViewById<android.widget.TextView>(R.id.tvForecastHeaderTitle)?.setTextColor(palette.textPrimary)

        binding.root.findViewById<android.view.View>(R.id.cardHighTemp)?.backgroundTintList = ColorStateList.valueOf(palette.tempHighBg)
        binding.root.findViewById<android.view.View>(R.id.cardLowTemp)?.backgroundTintList = ColorStateList.valueOf(palette.tempLowBg)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivHighTempIcon)?.setColorFilter(palette.tempHigh)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivLowTempIcon)?.setColorFilter(palette.tempLow)
        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyHigh)?.setTextColor(palette.tempHigh)
        binding.root.findViewById<android.widget.TextView>(R.id.tvDailyLow)?.setTextColor(palette.tempLow)

        binding.layoutHeroSection.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)

        binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.apply {
            setTextColor(palette.brandPrimary)
            iconTint = ColorStateList.valueOf(palette.brandPrimary)
            strokeColor = ColorStateList.valueOf(palette.dividerColor)
            backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        }

        binding.root.findViewById<android.widget.TextView>(R.id.tvInlineSelectedDate)?.setTextColor(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageButton>(R.id.btnInlinePrevDay)?.setColorFilter(palette.textPrimary)
        binding.root.findViewById<android.widget.ImageButton>(R.id.btnInlineNextDay)?.setColorFilter(palette.textPrimary)
        
        
        
        

        binding.root.findViewById<android.view.View>(R.id.layoutMetricWind)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricAqi)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricUv)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricVisibility)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricSunrise)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricSunset)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricDaylight)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        binding.root.findViewById<android.view.View>(R.id.layoutMetricMoon)?.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)

        binding.root.findViewById<android.widget.ImageView>(R.id.ivWindIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivAqiIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivUvIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivVisIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivSunriseIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivSunsetIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivDaylightIcon)?.setColorFilter(palette.brandPrimary)
        binding.root.findViewById<android.widget.ImageView>(R.id.ivMoonIcon)?.setColorFilter(palette.brandPrimary)

        binding.root.findViewById<android.widget.TextView>(R.id.tvWindSpeed)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvWindGusts)?.setTextColor(palette.textSecondary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvAqi)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvHumidity)?.setTextColor(palette.textSecondary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvUvIndex)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvVisibility)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunrise)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunset)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunriseAzimuth)?.setTextColor(palette.textSecondary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvSunsetAzimuth)?.setTextColor(palette.textSecondary)

        binding.root.findViewById<android.widget.TextView>(R.id.tvTotalDaylight)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonPhase)?.setTextColor(palette.textPrimary)
        binding.root.findViewById<android.widget.TextView>(R.id.tvMoonCyclePercentage)?.setTextColor(palette.textSecondary)

        metricGridAdapter?.setPalette(palette)
        forecastAdapter?.setPalette(palette)
        inlineSolarPhaseAdapter?.setPalette(palette)
        hourlyForecastAdapter?.setPalette(palette)

        (activity as? MainActivity)?.weatherDisplayManager?.updateAlertBanner()
    }

    private fun getAaaTitleColor(bgColor: Int): Int {
        val r = Color.red(bgColor) / 255.0
        val g = Color.green(bgColor) / 255.0
        val b = Color.blue(bgColor) / 255.0

        val rs = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gs = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bs = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

        val luminance = 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
        return if (luminance > 0.4) Color.BLACK else Color.WHITE
    }

    fun showThemeSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = android.app.Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogThemeSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            dialogBinding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val currentTheme = locationPreferences.getThemeMode()
        val palette = ColorPalette.forMode(currentTheme, activity)
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        dialogBinding.mainThemeSettings.setBackgroundColor(palette.background)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        dialogBinding.btnBack.setColorFilter(headerTitleColor)
        val headerRow = dialogBinding.cardHeader.getChildAt(0) as? LinearLayout
        (headerRow?.getChildAt(1) as? TextView)?.setTextColor(headerTitleColor)
        if (!isFireEngine) {
            dialogBinding.cardHeader.strokeColor = palette.dividerColor
        }
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isSystemDefault = (currentTheme == LocationPreferences.THEME_SYSTEM)
        dialogBinding.switchSystemDefault.isChecked = isSystemDefault

        dialogBinding.cardSystemDefaultSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSystemDefaultSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvSystemDefaultTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSystemDefaultDesc.setTextColor(palette.textSecondary)

        fun setChoicesEnabled(enabled: Boolean) {
            dialogBinding.layoutThemeChoicesContainer.alpha = if (enabled) 1.0f else 0.4f
            for (i in 0 until dialogBinding.layoutStandardThemes.childCount) {
                dialogBinding.layoutStandardThemes.getChildAt(i).isEnabled = enabled
            }
            for (i in 0 until dialogBinding.layoutBoutiqueThemes.childCount) {
                dialogBinding.layoutBoutiqueThemes.getChildAt(i).isEnabled = enabled
            }
        }

        setChoicesEnabled(!isSystemDefault)

        dialogBinding.switchSystemDefault.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setChoicesEnabled(false)
                locationPreferences.saveThemeMode(LocationPreferences.THEME_SYSTEM)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                applyTheme(LocationPreferences.THEME_SYSTEM)
            } else {
                setChoicesEnabled(true)
                val modeToSet = if (currentTheme == LocationPreferences.THEME_SYSTEM) LocationPreferences.THEME_DARK else currentTheme
                locationPreferences.saveThemeMode(modeToSet)
                applyTheme(modeToSet)
            }
        }

        dialogBinding.tvStandardHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvBoutiqueHeader.setTextColor(palette.textPrimary)

        val standardModes = listOf(
            LocationPreferences.THEME_LIGHT to ("Light Theme" to "Clean light background and high contrast typography."),
            LocationPreferences.THEME_DARK to ("Dark Theme" to "Modern bluish dark slate background for nighttime comfort.")
        )

        val boutiqueModes = listOf(
            LocationPreferences.THEME_AURORA to ("Aurora Theme" to "Deep midnight teal with emerald aurora glow."),
            LocationPreferences.THEME_CHARCOAL to ("Charcoal Theme" to "Deep neutral charcoal background for OLED displays."),
            LocationPreferences.THEME_COBALT to ("Cobalt Theme" to "Vibrant deep ocean blue background."),
            LocationPreferences.THEME_CREMA to ("Crema Theme" to "Warm creamy off-white background with soft brown accents."),
            LocationPreferences.THEME_FIRE_ENGINE to ("Fire Engine Theme" to "Bold deep crimson red background."),
            LocationPreferences.THEME_FROST to ("Frost Theme" to "Cool icy slate blue background."),
            LocationPreferences.THEME_JADE to ("Jade Theme" to "Rich forest dark green theme."),
            LocationPreferences.THEME_ORANGE_GROVE to ("Orange Grove Theme" to "Rich sunset orange background."),
            LocationPreferences.THEME_TERRACOTTA to ("Terracotta Theme" to "Warm earthy clay tones with sunset copper highlights.")
        )

        fun populateThemeGroup(
            container: LinearLayout,
            modes: List<Pair<Int, Pair<String, String>>>
        ) {
            container.removeAllViews()
            val density = activity.resources.displayMetrics.density

            for ((mode, info) in modes) {
                val card = MaterialCardView(activity).apply {
                    radius = 12 * density
                    strokeWidth = (1 * density).toInt()
                    strokeColor = palette.dividerColor
                    setCardBackgroundColor(palette.surfaceCard)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, 0, (12 * density).toInt())
                    layoutParams = lp
                }

                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                }

                val radioButton = RadioButton(activity).apply {
                    isChecked = (!isSystemDefault && currentTheme == mode)
                    isClickable = false
                    isFocusable = false
                }

                val textLayout = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                    lp.setMargins((12 * density).toInt(), 0, 0, 0)
                    layoutParams = lp
                }

                val titleTv = TextView(activity).apply {
                    text = info.first
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(palette.textPrimary)
                }

                val descTv = TextView(activity).apply {
                    text = info.second
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(palette.textSecondary)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, (2 * density).toInt(), 0, 0)
                    layoutParams = lp
                }

                textLayout.addView(titleTv)
                textLayout.addView(descTv)
                row.addView(radioButton)
                row.addView(textLayout)
                card.addView(row)

                card.setOnClickListener {
                    if (dialogBinding.switchSystemDefault.isChecked) return@setOnClickListener
                    locationPreferences.saveThemeMode(mode)
                    applyTheme(mode)
                    dialog.dismiss()
                    showThemeSettingsScreen()
                }

                container.addView(card)
            }
        }

        populateThemeGroup(dialogBinding.layoutStandardThemes, standardModes)
        populateThemeGroup(dialogBinding.layoutBoutiqueThemes, boutiqueModes)

        (activity as? MainActivity)?.settingsDialogManager?.trackDialog(dialog)
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }
}
