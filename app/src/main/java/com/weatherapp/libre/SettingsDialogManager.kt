package com.weatherapp.libre

import android.content.res.ColorStateList
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogAboutSettingsBinding
import com.weatherapp.libre.databinding.DialogAlertsSettingsBinding
import com.weatherapp.libre.databinding.DialogCacheSettingsBinding
import com.weatherapp.libre.databinding.DialogLicensesSettingsBinding
import com.weatherapp.libre.databinding.DialogModelsSettingsBinding
import com.weatherapp.libre.databinding.DialogPrivacySettingsBinding
import com.weatherapp.libre.databinding.DialogTabsSettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsDialogManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences
) {

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
        dialogBinding.rgTtlDuration.removeAllViews()

        val ttlOptions = listOf(
            30 to "30 Minutes",
            60 to "60 Minutes (1 Hour)",
            120 to "120 Minutes (2 Hours - Default)",
            240 to "240 Minutes (4 Hours)",
            360 to "360 Minutes (6 Hours)"
        )

        for ((mins, label) in ttlOptions) {
            val rb = RadioButton(activity).apply {
                text = label
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(palette.textPrimary)
                isChecked = (currentTtl == mins)
                setOnClickListener {
                    locationPreferences.saveCacheTtlMinutes(mins)
                    Toast.makeText(activity.applicationContext, "Cache TTL set to $label", Toast.LENGTH_SHORT).show()
                }
            }
            dialogBinding.rgTtlDuration.addView(rb)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showLicensesSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogLicensesSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showPrivacySettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogPrivacySettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialogBinding.tvPrivacyPolicyContent.text = "Loading Privacy Policy..."

        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("https://adamjeremy-sk.github.io/WeatherAPP/privacypolicy.html").build()
                client.newCall(request).execute().use { response ->
                    val htmlStr = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            dialogBinding.tvPrivacyPolicyContent.text = Html.fromHtml(htmlStr, Html.FROM_HTML_MODE_COMPACT)
                            dialogBinding.tvPrivacyPolicyContent.movementMethod = LinkMovementMethod.getInstance()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        dialogBinding.tvPrivacyPolicyContent.text = "WeatherAPP respects your privacy. All location queries are manual and no device GPS tracking is used."
                    }
                }
            }
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showAboutSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogAboutSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())

        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardHeader.strokeColor = palette.dividerColor
        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        dialogBinding.tvAboutAppVersion.text = "Version ${pInfo.versionName} (${pInfo.longVersionCode})"

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showDefaultLocationSettingsScreen(locationDialogManager: LocationDialogManager) {
        locationDialogManager.showRecentLocationsDialog(isSettingDefaultLocation = true) {
            Toast.makeText(activity.applicationContext, "Default startup location updated", Toast.LENGTH_SHORT).show()
        }
    }
}
