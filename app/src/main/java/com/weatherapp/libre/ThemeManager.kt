package com.weatherapp.libre

import android.content.res.ColorStateList
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
    private val locationPreferences: LocationPreferences
) {

    var forecastAdapter: DayForecastAdapter? = null
    var inlineSolarPhaseAdapter: SolarPhaseAdapter? = null

    fun applyTheme(themeMode: Int) {
        val palette = ColorPalette.forMode(themeMode, activity)
        applyPaletteToUI(palette)
    }

    fun applyPaletteToUI(palette: ColorPalette) {
        binding.drawerLayout.setBackgroundColor(palette.background)
        binding.main.setBackgroundColor(palette.background)

        binding.cardHeader.setCardBackgroundColor(palette.surfaceCard)
        binding.btnMenu.setColorFilter(palette.textPrimary)
        binding.ivPin.setColorFilter(palette.brandPrimary)
        binding.tvLocationName.setTextColor(palette.textPrimary)
        binding.tvAttribution.setTextColor(palette.textSecondary)

        binding.cardLocationTabs.setCardBackgroundColor(palette.surfaceCard)
        binding.cardLocationTabs.strokeColor = palette.dividerColor
        binding.tabLayoutLocations.setSelectedTabIndicatorColor(palette.brandPrimary)
        binding.tabLayoutLocations.setTabTextColors(palette.textSecondary, palette.brandPrimary)

        binding.cardAlertBanner.setCardBackgroundColor(palette.surfaceCardSecondary)
        binding.cardAlertBanner.strokeColor = palette.brandPrimary
        binding.tvAlertHeaderTitle.setTextColor(palette.brandPrimary)

        binding.cardTodayDetails.setCardBackgroundColor(palette.surfaceCard)
        binding.cardTodayDetails.strokeColor = palette.dividerColor

        binding.tvSelectedDateHeader.setTextColor(palette.textSecondary)
        binding.tvMainCondition.setTextColor(palette.textPrimary)
        binding.tvCurrentTemp.setTextColor(palette.textPrimary)

        binding.cardHighTemp.setBackgroundResource(R.drawable.bg_temp_high)
        binding.cardLowTemp.setBackgroundResource(R.drawable.bg_temp_low)
        binding.tvDailyHigh.setTextColor(palette.tempHigh)
        binding.tvDailyLow.setTextColor(palette.tempLow)

        binding.layoutHeroSection.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricWind.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricAqi.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricUv.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricVisibility.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricCeiling.setBackgroundResource(R.drawable.bg_metric_card)

        binding.layoutMetricSunrise.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricSunset.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricDaylight.setBackgroundResource(R.drawable.bg_metric_card)
        binding.layoutMetricMoon.setBackgroundResource(R.drawable.bg_metric_card)

        binding.tvWindSpeed.setTextColor(palette.textPrimary)
        binding.tvWindGust.setTextColor(palette.textSecondary)
        binding.tvAirQuality.setTextColor(palette.textPrimary)
        binding.tvUvIndex.setTextColor(palette.textPrimary)
        binding.tvVisibility.setTextColor(palette.textPrimary)
        binding.tvHumidity.setTextColor(palette.textSecondary)
        binding.tvCeiling.setTextColor(palette.textPrimary)

        binding.tvSunrise.setTextColor(palette.textPrimary)
        binding.tvSunset.setTextColor(palette.textPrimary)
        binding.tvSunriseAzimuth.setTextColor(palette.textSecondary)
        binding.tvSunsetAzimuth.setTextColor(palette.textSecondary)

        binding.tvTotalDaylight.setTextColor(palette.textPrimary)
        binding.tvMoonPhase.setTextColor(palette.textPrimary)
        binding.tvMoonCyclePercentage.setTextColor(palette.textSecondary)

        forecastAdapter?.setPalette(palette)
        inlineSolarPhaseAdapter?.setPalette(palette)
    }

    fun showThemeSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogThemeSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val currentTheme = locationPreferences.getThemeMode()
        val palette = ColorPalette.forMode(currentTheme, activity)

        dialogBinding.mainThemeSettings.setBackgroundColor(palette.background)
        dialogBinding.cardHeader.setCardBackgroundColor(palette.surfaceCard)
        dialogBinding.btnBack.setColorFilter(palette.textPrimary)
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isSystemDefault = (currentTheme == LocationPreferences.THEME_SYSTEM)
        dialogBinding.switchSystemDefault.isChecked = isSystemDefault

        // Show Golden & Blue Hour on Main Page Switch Card
        dialogBinding.cardShowGoldenBlueHourSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardShowGoldenBlueHourSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvShowGoldenBlueHourTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvShowGoldenBlueHourDesc.setTextColor(palette.textSecondary)

        val isShowGoldenBlueHourOn = locationPreferences.isShowGoldenBlueHourMainPageEnabled()
        dialogBinding.switchShowGoldenBlueHour.isChecked = isShowGoldenBlueHourOn
        dialogBinding.switchShowGoldenBlueHour.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveShowGoldenBlueHourMainPageEnabled(isChecked)
            binding.layoutMetricGoldenBlueHourContainer.visibility = if (isChecked) View.VISIBLE else View.GONE

            val msg = if (isChecked) "Golden & Blue Hour enabled on Main Page" else "Golden & Blue Hour hidden from Main Page"
            Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }

        // Volume Key Date Cycling Card
        dialogBinding.cardVolumeScrollSettings.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardVolumeScrollSettings.strokeColor = palette.dividerColor
        dialogBinding.tvVolumeScrollTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvVolumeScrollDesc.setTextColor(palette.textSecondary)

        val isVolumeScrollOn = locationPreferences.isVolumeScrollEnabled()
        dialogBinding.switchVolumeScroll.isChecked = isVolumeScrollOn
        dialogBinding.layoutVolumeSpeedContainer.visibility = if (isVolumeScrollOn) View.VISIBLE else View.GONE

        dialogBinding.switchVolumeScroll.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveVolumeScrollEnabled(isChecked)
            dialogBinding.layoutVolumeSpeedContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val currentSpeedIndex = locationPreferences.getVolumeScrollSpeedIndex()
        dialogBinding.seekBarVolumeSpeed.progress = currentSpeedIndex
        dialogBinding.tvVolumeSpeedLabel.text = "Cycling Speed: ${locationPreferences.getVolumeScrollSpeedLabel(currentSpeedIndex)}"

        dialogBinding.seekBarVolumeSpeed.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                dialogBinding.tvVolumeSpeedLabel.text = "Cycling Speed: ${locationPreferences.getVolumeScrollSpeedLabel(progress)}"
                if (fromUser) {
                    locationPreferences.saveVolumeScrollSpeedIndex(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Units Card
        dialogBinding.cardUnitsSelector.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardUnitsSelector.strokeColor = palette.dividerColor
        dialogBinding.tvUnitsTitle.setTextColor(palette.textPrimary)

        val isUsUnits = (locationPreferences.getUnitSystem() == UnitSystem.US_CUSTOMARY)
        dialogBinding.btnUnitsChoice.text = if (isUsUnits) "US (°F, mph, mi)" else "Metric (°C, km/h, km)"
        dialogBinding.tvUnitsDesc.text = if (isUsUnits) "US Customary (°F, mph, miles)" else "Metric (°C, km/h, km)"

        dialogBinding.btnUnitsChoice.setOnClickListener {
            val newUnits = if (locationPreferences.getUnitSystem() == UnitSystem.METRIC) UnitSystem.US_CUSTOMARY else UnitSystem.METRIC
            locationPreferences.saveUnitSystem(newUnits)
            dialogBinding.btnUnitsChoice.text = if (newUnits == UnitSystem.US_CUSTOMARY) "US (°F, mph, mi)" else "Metric (°C, km/h, km)"
            dialogBinding.tvUnitsDesc.text = if (newUnits == UnitSystem.US_CUSTOMARY) "US Customary (°F, mph, miles)" else "Metric (°C, km/h, km)"
            Toast.makeText(activity.applicationContext, "Units updated", Toast.LENGTH_SHORT).show()
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

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }
}
