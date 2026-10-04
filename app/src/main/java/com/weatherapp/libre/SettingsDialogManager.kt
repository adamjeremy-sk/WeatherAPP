package com.weatherapp.libre

import com.weatherapp.libre.BuildConfig
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.alerts.AlertManager
import com.weatherapp.libre.alerts.AlertTestHarness
import com.weatherapp.libre.alerts.AlertNotificationHelper
import com.weatherapp.libre.alerts.AlertPollWorker
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

    fun showWebViewSettingsPage(title: String, url: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val density = activity.resources.displayMetrics.density

        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(palette.backgroundLight)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Top Header Bar
        val cardHeader = MaterialCardView(activity).apply {
            radius = 0f
            strokeWidth = 0
            setCardBackgroundColor(ColorStateList.valueOf(headerBg))
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
            setColorFilter(headerTitleColor)
            setOnClickListener { dialog.dismiss() }
        }

        val tvTitle = TextView(activity).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(headerTitleColor)
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

        // Fullscreen In-App WebView with Pull-to-Refresh
        val swipeRefresh = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val webView = WebView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        swipeRefresh.addView(webView)
        root.addView(swipeRefresh)

        fun fetchPageContent(forceRefresh: Boolean = false) {
            val pageKey = url.substringAfterLast("/")
            val cachedHtml = locationPreferences.getCachedPageHtml(pageKey)
            val cachedTime = locationPreferences.getCachedPageTime(pageKey)
            val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
            val isCacheValid = !forceRefresh && cachedHtml != null && (System.currentTimeMillis() - cachedTime) < sevenDaysMs

            if (isCacheValid && cachedHtml != null) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                webView.loadDataWithBaseURL(url, cachedHtml, "text/html", "UTF-8", null)
            } else {
                if (!forceRefresh) progressBar.visibility = View.VISIBLE
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val client = OkHttpClient()
                        val request = Request.Builder().url(url).build()
                        client.newCall(request).execute().use { response ->
                            val html = response.body?.string()
                            if (response.isSuccessful && !html.isNullOrEmpty()) {
                                locationPreferences.saveCachedPageHtml(pageKey, html)
                                withContext(Dispatchers.Main) {
                                    if (!activity.isFinishing && !activity.isDestroyed) {
                                        progressBar.visibility = View.GONE
                                        swipeRefresh.isRefreshing = false
                                        webView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
                                    }
                                }
                            } else throw java.io.IOException("HTTP ${response.code}")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                progressBar.visibility = View.GONE
                                swipeRefresh.isRefreshing = false
                                if (cachedHtml != null) {
                                    webView.loadDataWithBaseURL(url, cachedHtml, "text/html", "UTF-8", null)
                                } else {
                                    webView.loadUrl(url)
                                }
                            }
                        }
                    }
                }
            }
        }

        swipeRefresh.setOnRefreshListener {
            fetchPageContent(forceRefresh = true)
        }

        fetchPageContent(forceRefresh = false)

        dialog.setContentView(root)

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showLocationTabsSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogTabsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            dialogBinding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        dialogBinding.btnBack.setColorFilter(headerTitleColor)
        val headerRow = dialogBinding.cardHeader.getChildAt(0) as? LinearLayout
        (headerRow?.getChildAt(1) as? TextView)?.setTextColor(headerTitleColor)
        if (!isFireEngine) {
            dialogBinding.cardHeader.strokeColor = palette.dividerColor
        }

        dialogBinding.cardStickyTabsSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardStickyTabsSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvStickyTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvStickyDesc.setTextColor(palette.textSecondary)
        dialogBinding.tvTabsCountHeader.setTextColor(palette.textPrimary)

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        val isSticky = locationPreferences.isLocationTabsSticky()
        dialogBinding.switchStickyTabs.isChecked = isSticky
        dialogBinding.switchStickyTabs.setOnCheckedChangeListener { _, checked ->
            locationPreferences.saveLocationTabsSticky(checked)
            if (activity is MainActivity) {
                activity.updateStickyTabsPosition()
            }
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
                isChecked = (currentCount == count)
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
                locationPreferences.saveTabCount(count)
                if (activity is MainActivity) {
                    activity.setupLocationTabs()
                }
                Toast.makeText(activity.applicationContext, "Tab count updated to $count", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showLocationTabsSettingsScreen()
            }

            dialogBinding.rgTabOptions.addView(card)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showModelsSettingsScreen(weatherDisplayManager: WeatherDisplayManager) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogModelsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            dialogBinding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val density = activity.resources.displayMetrics.density
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        dialogBinding.btnBack.setColorFilter(headerTitleColor)
        val headerRow = dialogBinding.cardHeader.getChildAt(0) as? LinearLayout
        (headerRow?.getChildAt(1) as? TextView)?.setTextColor(headerTitleColor)
        if (!isFireEngine) {
            dialogBinding.cardHeader.strokeColor = palette.dividerColor
        }

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialogBinding.tvWesternHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvChineseHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvAutoChinaHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvAutoChinaTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvAutoChinaDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardAutoChina.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAutoChina.strokeColor = palette.dividerColor

        val currentModel = locationPreferences.getSavedForecastModel()

        // Populate Western Models
        dialogBinding.rgWesternModels.removeAllViews()
        val westernModels = ForecastModel.values().filter { it.isWestern }
        for (model in westernModels) {
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
                isChecked = (currentModel == model)
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
                text = model.displayName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
            }

            val descTv = TextView(activity).apply {
                text = model.description
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
                locationPreferences.saveForecastModel(model)
                weatherDisplayManager.loadWeather(forceRefresh = true)
                Toast.makeText(activity.applicationContext, "Model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showModelsSettingsScreen(weatherDisplayManager)
            }

            dialogBinding.rgWesternModels.addView(card)
        }

        // Populate Chinese Models
        dialogBinding.rgChineseModels.removeAllViews()
        val chineseModels = ForecastModel.values().filter { !it.isWestern }
        for (model in chineseModels) {
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
                isChecked = (currentModel == model)
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
                text = model.displayName
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
            }

            val descTv = TextView(activity).apply {
                text = model.description
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
                locationPreferences.saveForecastModel(model)
                weatherDisplayManager.loadWeather(forceRefresh = true)
                Toast.makeText(activity.applicationContext, "Model set to ${model.displayName}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showModelsSettingsScreen(weatherDisplayManager)
            }

            dialogBinding.rgChineseModels.addView(card)
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

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogAlertsSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            dialogBinding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        dialogBinding.btnBack.setColorFilter(headerTitleColor)
        val headerRow = dialogBinding.cardHeader.getChildAt(0) as? LinearLayout
        (headerRow?.getChildAt(1) as? TextView)?.setTextColor(headerTitleColor)
        if (!isFireEngine) {
            dialogBinding.cardHeader.strokeColor = palette.dividerColor
        }

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialogBinding.tvHeaderTitle.setTextColor(headerTitleColor)
        dialogBinding.tvAlertsHeaderDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardAlertsSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardAlertsSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvAlertsSwitchTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvAlertsSwitchDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardSolarLeadTime.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSolarLeadTime.strokeColor = palette.dividerColor
        dialogBinding.tvSolarLeadTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSolarLeadDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardTestAlert.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardTestAlert.strokeColor = palette.dividerColor
        dialogBinding.tvTestAlertTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvTestAlertDesc.setTextColor(palette.textSecondary)

        dialogBinding.etSolarLeadMins.setTextColor(palette.textPrimary)
        dialogBinding.etSolarLeadMins.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)

        // Test notification system harness
        dialogBinding.cardTestAlert.alpha = 1.0f
        dialogBinding.cardTestAlert.isEnabled = true
        dialogBinding.btnSendTestAlert.isEnabled = true
        dialogBinding.btnSendTestAlert.text = "Send Test Weather Alert"
        dialogBinding.btnSendTestAlert.setOnClickListener { v ->
            val popup = PopupMenu(activity, v)
            val fixtures = AlertTestHarness.TestFixture.values()
            for (i in fixtures.indices) {
                popup.menu.add(0, i, i, fixtures[i].displayName)
            }
            popup.setOnMenuItemClickListener { menuItem ->
                val selectedFixture = fixtures[menuItem.itemId]
                val currentLoc = if (activity is MainActivity) activity.weatherDisplayManager.currentLocation else locationPreferences.getSavedLocation()
                val tz = if (activity is MainActivity) activity.weatherDisplayManager.locationTimezone else null
                val alertMgr = if (activity is MainActivity) activity.alertManager else AlertManager(activity.applicationContext, locationPreferences)

                // Clear previous test dismissals for this fixture so we can re-trigger it
                locationPreferences.clearPersistentCache()
                locationPreferences.clearTestAlertDismissals()
                alertMgr.clearTestAlerts(currentLoc)

                val injectedAlert = AlertTestHarness.injectTestFixture(
                    context = activity.applicationContext,
                    fixture = selectedFixture,
                    location = currentLoc,
                    timezoneStr = tz,
                    alertManager = alertMgr
                )

                if (injectedAlert != null) {
                    AlertNotificationHelper.postNotification(activity.applicationContext, injectedAlert, currentLoc, tz)
                    if (activity is MainActivity) {
                        activity.alertBannerManager.updateAlertBanner(currentLoc)
                    }
                    Toast.makeText(activity.applicationContext, "Injected & Dispatched: ${selectedFixture.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity.applicationContext, "Failed to inject ${selectedFixture.displayName}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            popup.show()
        }

        val thumbStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val thumbColors = intArrayOf(palette.brandPrimary, palette.dividerColor)
        dialogBinding.switchAlerts.thumbTintList = ColorStateList(thumbStates, thumbColors)

        val trackStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val trackColors = intArrayOf(
            ColorUtils.setAlphaComponent(palette.brandPrimary, 128),
            ColorUtils.setAlphaComponent(palette.dividerColor, 128)
        )
        dialogBinding.switchAlerts.trackTintList = ColorStateList(trackStates, trackColors)

        var isBindingSwitchState = true
        val isAlertsOn = locationPreferences.getAlertConsentState() == LocationPreferences.ALERT_CONSENT_OPT_IN
        dialogBinding.switchAlerts.isChecked = isAlertsOn
        isBindingSwitchState = false

        dialogBinding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            if (isBindingSwitchState) return@setOnCheckedChangeListener

            if (isChecked) {
                locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)
                AlertNotificationHelper.createNotificationChannels(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                    }
                }
                AlertPollWorker.schedule(activity.applicationContext)
            } else {
                locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_OUT)
                AlertPollWorker.cancel(activity.applicationContext)
            }
        }

        dialogBinding.etSolarLeadMins.setText(locationPreferences.getSolarAlarmLeadMinutes().toString())

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showCacheSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogCacheSettingsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(dialogBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            dialogBinding.root.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        dialogBinding.root.setBackgroundColor(palette.backgroundLight)
        dialogBinding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        dialogBinding.btnBack.setColorFilter(headerTitleColor)
        val headerRow = dialogBinding.cardHeader.getChildAt(0) as? LinearLayout
        (headerRow?.getChildAt(1) as? TextView)?.setTextColor(headerTitleColor)
        if (!isFireEngine) {
            dialogBinding.cardHeader.strokeColor = palette.dividerColor
        }

        dialogBinding.btnBack.setOnClickListener { dialog.dismiss() }

        dialogBinding.tvHeaderTitle.setTextColor(headerTitleColor)
        dialogBinding.tvCacheHeaderDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardCacheSwitch.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardCacheSwitch.strokeColor = palette.dividerColor
        dialogBinding.tvCacheSwitchTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvCacheSwitchDesc.setTextColor(palette.textSecondary)

        dialogBinding.tvTtlHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvTtlReasoningHeader.setTextColor(palette.textPrimary)
        dialogBinding.tvTtlReasoningDesc.setTextColor(palette.textSecondary)
        dialogBinding.tvRateLimitingHeader.setTextColor(palette.tempHigh)
        dialogBinding.tvRateLimitingDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardTtlReasoning.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))
        dialogBinding.cardRateLimiting.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCardSecondary))

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
        showWebViewSettingsPage("Licenses & Sources", "https://www.weatherapplibre.ca/licenses.html")
    }

    fun showPrivacySettingsScreen() {
        showWebViewSettingsPage("Privacy Policy", "https://www.weatherapplibre.ca/privacypolicy.html")
    }

    fun showAboutSettingsScreen() {
        showWebViewSettingsPage(activity.getString(R.string.about_page_title, BuildConfig.VERSION_NAME), "https://www.weatherapplibre.ca/about.html")
    }

    fun showDonateSettingsScreen() {
        showWebViewSettingsPage("Donate", "https://ko-fi.com/adamjeremy")
    }

    fun showRoadmapSettingsScreen() {
        showWebViewSettingsPage("WeatherAPP Roadmap", "https://www.weatherapplibre.ca/roadmap.html")
    }

    fun showDefaultLocationSettingsScreen(locationDialogManager: LocationDialogManager) {
        locationDialogManager.showRecentLocationsDialog(isSettingDefaultLocation = true) {
            Toast.makeText(activity.applicationContext, "Default startup location updated", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareProject() {
        val message = activity.getString(R.string.share_message_body, BETA_SUPPORT_URL)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, activity.getString(R.string.share_chooser_title))
        activity.startActivity(chooser)
    }

    companion object {
        const val BETA_SUPPORT_URL = "https://www.weatherapplibre.ca/support.html"
    }
}
