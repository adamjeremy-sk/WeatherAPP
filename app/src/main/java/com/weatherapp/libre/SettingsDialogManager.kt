package com.weatherapp.libre

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogAlertsSettingsBinding
import com.weatherapp.libre.databinding.DialogCacheSettingsBinding
import com.weatherapp.libre.databinding.DialogModelsSettingsBinding
import com.weatherapp.libre.databinding.DialogTabsSettingsBinding

class SettingsDialogManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences
) {

    fun showWebViewSettingsPage(title: String, url: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = android.app.Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val density = activity.resources.displayMetrics.density

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(palette.backgroundLight)
        }

        // Top Header Bar
        val cardHeader = MaterialCardView(activity).apply {
            radius = 0f
            strokeWidth = 0
            setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            cardElevation = 2 * density
        }

        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
        }

        val btnBack = ImageButton(activity).apply {
            setImageResource(R.drawable.ic_back)
            contentDescription = "Back"
            val typedValue = TypedValue()
            activity.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
            setBackgroundResource(typedValue.resourceId)
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            setColorFilter(palette.textPrimary)
            setOnClickListener { dialog.dismiss() }
        }

        val tvTitle = TextView(activity).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            lp.setMargins((16 * density).toInt(), 0, 0, 0)
            layoutParams = lp
        }

        headerRow.addView(btnBack)
        headerRow.addView(tvTitle)
        cardHeader.addView(headerRow)
        root.addView(cardHeader)

        // Progress Bar
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        root.addView(progressBar)

        // In-App WebView Page
        val webView = WebView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                }
            }
            loadUrl(url)
        }
        root.addView(webView)

        dialog.setContentView(root)

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showLocationTabsSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogTabsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.cardStickyTabsSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardStickyTabsSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvStickyTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvStickyDesc.setTextColor(palette.textSecondary)

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isSticky = locationPreferences.isLocationTabsSticky()
        dialogBinding.switchStickyTabs.isChecked = isSticky
        dialogBinding.switchStickyTabs.setOnCheckedChangeListener { _, checked ->
            locationPreferences.saveLocationTabsSticky(checked)
            val msg = if (checked) "Location tabs pinned as sticky" else "Location tabs set to scroll with page"
            Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }

        val currentCount = locationPreferences.getTabCount()
        dialogBinding.rgTabOptions.removeAllViews()

        val density = activity.resources.displayMetrics.density
        val tabOptions = listOf(
            1 to ("1 Location Tab" to "Displays a single location tab on the main page."),
            2 to ("2 Location Tabs (Default)" to "Pins two location tabs side-by-side."),
            3 to ("3 Location Tabs" to "Pins three location tabs for multi-city switching."),
            4 to ("4 Location Tabs" to "Pins four location tabs for multi-city monitoring.")
        )

        for ((count, info) in tabOptions) {
            val rb = RadioButton(activity).apply {
                text = "${info.first}\n${info.second}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(palette.textPrimary)
                isChecked = (currentCount == count)
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                setOnClickListener {
                    locationPreferences.saveTabCount(count)
                    if (activity is MainActivity) {
                        activity.setupLocationTabs()
                    }
                    Toast.makeText(activity.applicationContext, "Tab count updated to $count", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    showLocationTabsSettingsScreen()
                }
            }
            dialogBinding.rgTabOptions.addView(rb)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showModelsSettingsScreen(weatherDisplayManager: WeatherDisplayManager) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogModelsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val currentModel = locationPreferences.getSavedForecastModel()

        // Populate Western Models
        dialogBinding.rgWesternModels.removeAllViews()
        val westernModels = ForecastModel.values().filter { it.isWestern }
        for (model in westernModels) {
            val rb = RadioButton(activity).apply {
                text = "${model.displayName}\n${model.description}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(palette.textPrimary)
                isChecked = (currentModel == model)
                setOnClickListener {
                    locationPreferences.saveForecastModel(model)
                    weatherDisplayManager.loadWeather(forceRefresh = true)
                    Toast.makeText(activity.applicationContext, "Model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            dialogBinding.rgWesternModels.addView(rb)
        }

        // Populate Chinese Models
        dialogBinding.rgChineseModels.removeAllViews()
        val chineseModels = ForecastModel.values().filter { !it.isWestern }
        for (model in chineseModels) {
            val rb = RadioButton(activity).apply {
                text = "${model.displayName}\n${model.description}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(palette.textPrimary)
                isChecked = (currentModel == model)
                setOnClickListener {
                    locationPreferences.saveForecastModel(model)
                    weatherDisplayManager.loadWeather(forceRefresh = true)
                    Toast.makeText(activity.applicationContext, "Model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            dialogBinding.rgChineseModels.addView(rb)
        }

        val isAutoChina = locationPreferences.isAutoCmaChinaEnabled()
        dialogBinding.switchAutoChina.isChecked = isAutoChina
        dialogBinding.switchAutoChina.setOnCheckedChangeListener { _, checked ->
            locationPreferences.saveAutoCmaChinaEnabled(checked)
            weatherDisplayManager.loadWeather(forceRefresh = true)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showWeatherAlertsSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogAlertsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isAlertsOn = locationPreferences.isWeatherAlertsEnabled()
        dialogBinding.switchAlerts.isChecked = isAlertsOn
        dialogBinding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveWeatherAlertsEnabled(isChecked)
            val msg = if (isChecked) "Environment Canada Weather Alerts Enabled" else "Weather Alerts Disabled"
            Toast.makeText(activity.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }

        dialogBinding.etSolarLeadMins.setText(locationPreferences.getSolarAlarmLeadMinutes().toString())

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showCacheSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogCacheSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isCacheOn = locationPreferences.isCacheTtlEnabled()
        dialogBinding.switchCacheEnabled.isChecked = isCacheOn
        dialogBinding.switchCacheEnabled.setOnCheckedChangeListener { _, isChecked ->
            locationPreferences.saveCacheTtlEnabled(isChecked)
        }

        val currentTtl = locationPreferences.getCacheTtlMinutes()
        dialogBinding.layoutTtlOptions.removeAllViews()

        val density = activity.resources.displayMetrics.density
        val ttlOptions = listOf(
            30 to ("30 Minutes" to "Refreshes weather data if older than 30 minutes."),
            60 to ("60 Minutes (1 Hour)" to "Refreshes weather data if older than 1 hour."),
            120 to ("120 Minutes (2 Hours - Default)" to "Standard cache TTL duration. Reuses weather data for up to 2 hours."),
            240 to ("240 Minutes (4 Hours)" to "Extended cache TTL for reduced mobile data usage."),
            360 to ("360 Minutes (6 Hours)" to "Maximum cache TTL duration for offline resilience.")
        )

        for ((mins, info) in ttlOptions) {
            val card = MaterialCardView(activity).apply {
                radius = 12 * density
                strokeWidth = (1 * density).toInt()
                strokeColor = palette.dividerColor
                setCardBackgroundColor(palette.surfaceCard)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, (10 * density).toInt())
                layoutParams = lp
            }

            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
            }

            val rb = RadioButton(activity).apply {
                isChecked = (currentTtl == mins)
                isClickable = false
                isFocusable = false
            }

            val col = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                lp.setMargins((12 * density).toInt(), 0, 0, 0)
                layoutParams = lp
            }

            val titleTv = TextView(activity).apply {
                text = info.first
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
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

            col.addView(titleTv)
            col.addView(descTv)
            row.addView(rb)
            row.addView(col)
            card.addView(row)

            card.setOnClickListener {
                locationPreferences.saveCacheTtlMinutes(mins)
                Toast.makeText(activity.applicationContext, "Cache TTL set to ${info.first}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showCacheSettingsScreen()
            }

            dialogBinding.layoutTtlOptions.addView(card)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showLicensesSettingsScreen() {
        showWebViewSettingsPage("Licenses & Sources", "https://adamjeremy-sk.github.io/WeatherAPP/licenses.html")
    }

    fun showPrivacySettingsScreen() {
        showWebViewSettingsPage("Privacy Policy", "https://adamjeremy-sk.github.io/WeatherAPP/privacypolicy.html")
    }

    fun showAboutSettingsScreen() {
        showWebViewSettingsPage("About WeatherAPP", "https://adamjeremy-sk.github.io/WeatherAPP/about.html")
    }

    fun showRoadmapSettingsScreen() {
        showWebViewSettingsPage("WeatherAPP Roadmap", "https://adamjeremy-sk.github.io/WeatherAPP/roadmap.html")
    }

    fun showDefaultLocationSettingsScreen(locationDialogManager: LocationDialogManager) {
        locationDialogManager.showRecentLocationsDialog(isSettingDefaultLocation = true) {
            Toast.makeText(activity.applicationContext, "Default startup location updated", Toast.LENGTH_SHORT).show()
        }
    }
}
