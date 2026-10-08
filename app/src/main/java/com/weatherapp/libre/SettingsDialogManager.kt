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
import android.view.KeyEvent
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.alerts.AlertManager
import com.weatherapp.libre.alerts.AlertTestHarness
import com.weatherapp.libre.alerts.AlertNotificationHelper
import com.weatherapp.libre.alerts.AlertPollWorker
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogAboutSettingsBinding
import com.weatherapp.libre.databinding.DialogAlertsSettingsBinding
import com.weatherapp.libre.databinding.DialogBehaviourScreenBinding
import com.weatherapp.libre.databinding.DialogCacheSettingsBinding
import com.weatherapp.libre.databinding.DialogInfoScreenBinding
import com.weatherapp.libre.databinding.DialogLicensesSettingsBinding
import com.weatherapp.libre.databinding.DialogMenuOrganizationScreenBinding
import com.weatherapp.libre.databinding.DialogModelsSettingsBinding
import com.weatherapp.libre.databinding.DialogPrivacySettingsBinding
import com.weatherapp.libre.databinding.DialogSystemScreenBinding
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

    private val menuStack = mutableListOf<Dialog>()
    private var wasPopped = false

    fun trackDialog(dialog: Dialog) {
        menuStack.add(dialog)
        dialog.setOnDismissListener {
            menuStack.remove(dialog)
            if (menuStack.isEmpty() && wasPopped) {
                openDrawer()
            }
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) {
                    popTopMenu()
                }
                true
            } else {
                false
            }
        }
    }

    fun popTopMenu() {
        if (menuStack.isNotEmpty()) {
            wasPopped = true
            menuStack.last().dismiss()
            wasPopped = false
        }
    }

    fun getMenuStackDepth() = menuStack.size

    fun openDrawer() {
        (activity as? MainActivity)?.binding?.drawerLayout?.openDrawer(GravityCompat.START)
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

            if (isCacheValid) {
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
        trackDialog(dialog)

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

        trackDialog(dialog)
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

        trackDialog(dialog)
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

        trackDialog(dialog)
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

        val alertLogCard = dialogBinding.root.findViewById<com.google.android.material.card.MaterialCardView>(com.weatherapp.libre.R.id.cardAlertLog)
        val tvAlertLogTitle = dialogBinding.root.findViewById<TextView>(com.weatherapp.libre.R.id.tvAlertLogTitle)
        val tvAlertLogDesc = dialogBinding.root.findViewById<TextView>(com.weatherapp.libre.R.id.tvAlertLogDesc)
        
        alertLogCard?.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        alertLogCard?.strokeColor = palette.dividerColor
        tvAlertLogTitle?.setTextColor(palette.textPrimary)
        tvAlertLogDesc?.setTextColor(palette.textSecondary)
        alertLogCard?.setOnClickListener {
            dialog.dismiss()
            showAlertLogScreen()
        }

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

        trackDialog(dialog)
        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }

    fun showAlertLogScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val view = activity.layoutInflater.inflate(com.weatherapp.libre.R.layout.dialog_alert_log, null)
        dialog.setContentView(view)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val isFireEngine = (locationPreferences.getThemeMode() == LocationPreferences.THEME_FIRE_ENGINE)
        val headerBg = if (isFireEngine) Color.parseColor("#DC2626") else palette.surfaceCard
        val headerTitleColor = getAaaTitleColor(headerBg)

        view.setBackgroundColor(palette.backgroundLight)
        val cardHeader = view.findViewById<com.google.android.material.card.MaterialCardView>(com.weatherapp.libre.R.id.cardHeader)
        cardHeader.setCardBackgroundColor(ColorStateList.valueOf(headerBg))
        
        val btnBack = view.findViewById<ImageButton>(com.weatherapp.libre.R.id.btnBack)
        btnBack.setColorFilter(headerTitleColor)
        
        val tvHeaderTitle = view.findViewById<TextView>(com.weatherapp.libre.R.id.tvHeaderTitle)
        tvHeaderTitle.setTextColor(headerTitleColor)
        
        if (!isFireEngine) {
            cardHeader.strokeColor = palette.dividerColor
        }

        btnBack.setOnClickListener { dialog.dismiss() }

        val btnSort = view.findViewById<android.widget.Button>(com.weatherapp.libre.R.id.btnSort)
        val btnFilter = view.findViewById<android.widget.Button>(com.weatherapp.libre.R.id.btnFilter)
        val btnClear = view.findViewById<android.widget.Button>(com.weatherapp.libre.R.id.btnClear)
        val layoutEntries = view.findViewById<LinearLayout>(com.weatherapp.libre.R.id.layoutAlertLogEntries)

        // Palette wiring
        btnSort.setTextColor(palette.textPrimary)
        btnFilter.setTextColor(palette.textPrimary)
        btnClear.setTextColor(palette.textPrimary)
        
        btnSort.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        btnFilter.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)
        btnClear.backgroundTintList = ColorStateList.valueOf(palette.surfaceCardSecondary)

        var sortMode = "newest"
        var filterMode = "show_all"
        var entries = com.weatherapp.libre.alerts.AlertLogRepository.getAllEntries(activity)

        fun renderEntries() {
            layoutEntries.removeAllViews()
            
            var displayList = entries.toList()
            if (filterMode == "hide_tests") {
                displayList = displayList.filter { !it.isTest }
            }
            
            displayList = when (sortMode) {
                "severity" -> {
                    val severityRank = mapOf("extreme" to 4, "severe" to 3, "moderate" to 2, "minor" to 1)
                    displayList.sortedByDescending { severityRank[it.severity.lowercase()] ?: 0 }
                }
                "oldest" -> {
                    displayList.sortedBy { it.timestampUtc }
                }
                else -> {
                    displayList.sortedByDescending { it.timestampUtc }
                }
            }

            val density = activity.resources.displayMetrics.density
            val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a", java.util.Locale.getDefault())
            val zoneId = java.time.ZoneId.systemDefault()

            for (entry in displayList) {
                val card = com.google.android.material.card.MaterialCardView(activity).apply {
                    radius = 8 * density
                    strokeWidth = 0
                    setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, 0, (12 * density).toInt())
                    layoutParams = lp
                }

                val rowCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((16 * density).toInt(), (14 * density).toInt(), (16 * density).toInt(), (14 * density).toInt())
                }
                
                val row1 = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val locTv = TextView(activity).apply {
                    text = entry.locationName
                    setTextColor(palette.textPrimary)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                    layoutParams = lp
                }
                row1.addView(locTv)
                
                if (entry.isTest) {
                    val testBadge = TextView(activity).apply {
                        text = activity.getString(com.weatherapp.libre.R.string.alert_log_test_badge)
                        setTextColor(Color.WHITE)
                        setBackgroundColor(palette.tempHigh)
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                        setPadding((4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
                        val bg = android.graphics.drawable.GradientDrawable()
                        bg.setColor(palette.tempHigh)
                        bg.cornerRadius = 4 * density
                        background = bg
                    }
                    row1.addView(testBadge)
                }

                val eventTv = TextView(activity).apply {
                    text = entry.alertEvent
                    setTextColor(palette.textPrimary)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, (4 * density).toInt(), 0, 0)
                    layoutParams = lp
                }

                val row3 = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, (4 * density).toInt(), 0, 0)
                    layoutParams = lp
                }
                val timeStr = java.time.Instant.ofEpochMilli(entry.timestampUtc).atZone(zoneId).format(formatter)
                val timeTv = TextView(activity).apply {
                    text = timeStr
                    setTextColor(palette.textSecondary)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                    layoutParams = lp
                }
                val sevTv = TextView(activity).apply {
                    text = entry.severity
                    setTextColor(palette.textSecondary)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                row3.addView(timeTv)
                row3.addView(sevTv)

                rowCol.addView(row1)
                rowCol.addView(eventTv)
                rowCol.addView(row3)
                card.addView(rowCol)
                layoutEntries.addView(card)
            }
        }

        btnSort.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(activity, view)
            popup.menu.add(0, 1, 0, activity.getString(com.weatherapp.libre.R.string.alert_log_sort_newest))
            popup.menu.add(0, 2, 0, activity.getString(com.weatherapp.libre.R.string.alert_log_sort_oldest))
            popup.menu.add(0, 3, 0, activity.getString(com.weatherapp.libre.R.string.alert_log_sort_severity))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        sortMode = "newest"
                        btnSort.text = activity.getString(com.weatherapp.libre.R.string.alert_log_sort_newest)
                    }
                    2 -> {
                        sortMode = "oldest"
                        btnSort.text = activity.getString(com.weatherapp.libre.R.string.alert_log_sort_oldest)
                    }
                    3 -> {
                        sortMode = "severity"
                        btnSort.text = activity.getString(com.weatherapp.libre.R.string.alert_log_sort_severity)
                    }
                }
                renderEntries()
                true
            }
            popup.show()
        }

        btnFilter.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(activity, view)
            popup.menu.add(0, 1, 0, activity.getString(com.weatherapp.libre.R.string.alert_log_filter_show_all))
            popup.menu.add(0, 2, 0, activity.getString(com.weatherapp.libre.R.string.alert_log_filter_hide_tests))
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        filterMode = "show_all"
                        btnFilter.text = activity.getString(com.weatherapp.libre.R.string.alert_log_filter_show_all)
                    }
                    2 -> {
                        filterMode = "hide_tests"
                        btnFilter.text = activity.getString(com.weatherapp.libre.R.string.alert_log_filter_hide_tests)
                    }
                }
                renderEntries()
                true
            }
            popup.show()
        }

        btnClear.setOnClickListener {
            android.app.AlertDialog.Builder(activity)
                .setTitle(activity.getString(com.weatherapp.libre.R.string.alert_log_clear))
                .setMessage(activity.getString(com.weatherapp.libre.R.string.alert_log_clear_confirm))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    com.weatherapp.libre.alerts.AlertLogRepository.clearAll(activity)
                    entries = emptyList()
                    Toast.makeText(activity, activity.getString(com.weatherapp.libre.R.string.alert_log_cleared), Toast.LENGTH_SHORT).show()
                    renderEntries()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        renderEntries()
        trackDialog(dialog)
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

    fun showSystemSettingsScreen(weatherDisplayManager: WeatherDisplayManager, locationDialogManager: LocationDialogManager) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogSystemScreenBinding.inflate(activity.layoutInflater)
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
        dialogBinding.tvHeaderDesc.setTextColor(palette.textSecondary)

        val cards = listOf(
            dialogBinding.cardBehaviour to (dialogBinding.tvBehaviourTitle to dialogBinding.tvBehaviourDesc),
            dialogBinding.cardDataCache to (dialogBinding.tvDataCacheTitle to dialogBinding.tvDataCacheDesc),
            dialogBinding.cardDefaultLocation to (dialogBinding.tvDefaultLocationTitle to dialogBinding.tvDefaultLocationDesc),
            dialogBinding.cardLocationTabs to (dialogBinding.tvLocationTabsTitle to dialogBinding.tvLocationTabsDesc),
            dialogBinding.cardNotifications to (dialogBinding.tvNotificationsTitle to dialogBinding.tvNotificationsDesc),
            dialogBinding.cardWeatherModels to (dialogBinding.tvWeatherModelsTitle to dialogBinding.tvWeatherModelsDesc)
        )

        for ((card, textPair) in cards) {
            card.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            card.strokeColor = palette.dividerColor
            textPair.first.setTextColor(palette.textPrimary)
            textPair.second.setTextColor(palette.textSecondary)
        }

        dialogBinding.cardBehaviour.setOnClickListener { showBehaviourSettingsScreen() }
        dialogBinding.cardDataCache.setOnClickListener { showCacheSettingsScreen() }
        dialogBinding.cardDefaultLocation.setOnClickListener { showDefaultLocationSettingsScreen(locationDialogManager) }
        dialogBinding.cardLocationTabs.setOnClickListener { showLocationTabsSettingsScreen() }
        dialogBinding.cardNotifications.setOnClickListener { showWeatherAlertsSettingsScreen() }
        dialogBinding.cardWeatherModels.setOnClickListener { showModelsSettingsScreen(weatherDisplayManager) }

        trackDialog(dialog)
        dialog.show()
    }

    fun showBehaviourSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogBehaviourScreenBinding.inflate(activity.layoutInflater)
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
        dialogBinding.tvHeaderDesc.setTextColor(palette.textSecondary)

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
        dialogBinding.tvVolumeSpeedLabel.setTextColor(palette.textPrimary)

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

            // Clear weather cache and reload weather for active tab immediately
            if (activity is MainActivity) {
                activity.repository.clearMemoryAndDiskCache(locationPreferences)
                activity.weatherDisplayManager.loadWeather(forceRefresh = true)
            }

            Toast.makeText(activity.applicationContext, "Units updated", Toast.LENGTH_SHORT).show()
        }

        // Menu Organization Card
        dialogBinding.cardOrganizeMenu.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardOrganizeMenu.strokeColor = palette.dividerColor
        dialogBinding.tvOrganizeMenuTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvOrganizeMenuDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardOrganizeMenu.setOnClickListener {
            showMenuOrganizationScreen()
        }

        trackDialog(dialog)
        dialog.show()
    }

    private fun checkActiveTabValidity() {
        if (activity is MainActivity) {
            val activeTab = activity.astronomyManager.activeInlineSubTab
            val showSevenDay = locationPreferences.getSevenDayAsMenuItem()
            val enabledFlags = locationPreferences.getMenuEnabledFlags()
            val isTabEnabled = enabledFlags[activeTab] ?: true
            val isSevenDayInvalid = (activeTab == InlineSubTab.SEVEN_DAY && !showSevenDay)

            if (!isTabEnabled || isSevenDayInvalid) {
                val defaultTab = locationPreferences.getDefaultLaunchSubTab()
                activity.astronomyManager.switchInlineSubTab(
                    defaultTab,
                    activity.weatherDisplayManager.currentLocation,
                    activity.weatherDisplayManager.locationTimezone
                )
            }
        }
    }

    fun showMenuOrganizationScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogMenuOrganizationScreenBinding.inflate(activity.layoutInflater)
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
        dialogBinding.tvHeaderDesc.setTextColor(palette.textSecondary)

        // Palette theming for cards
        dialogBinding.cardSevenDayPlacement.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSevenDayPlacement.strokeColor = palette.dividerColor
        dialogBinding.tvSevenDayPlacementTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSevenDayPlacementDesc.setTextColor(palette.textSecondary)

        dialogBinding.cardSeparatorStepper.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        dialogBinding.cardSeparatorStepper.strokeColor = palette.dividerColor
        dialogBinding.tvNumSeparatorsTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvSeparatorCount.setTextColor(palette.textPrimary)
        dialogBinding.btnMinusSeparators.setColorFilter(palette.brandPrimary)
        dialogBinding.btnPlusSeparators.setColorFilter(palette.brandPrimary)

        // Saved state
        val orgItems = locationPreferences.getMenuOrder().toMutableList()
        val showSevenDay = locationPreferences.getSevenDayAsMenuItem()

        val enabledFlags = locationPreferences.getMenuEnabledFlags().toMutableMap()
        var defaultLaunchSubTab = locationPreferences.getDefaultLaunchSubTab()

        // Adapter - create first so it can be updated
        lateinit var adapter: MenuOrganizationAdapter

        // 7-Day Placement Toggle Card
        dialogBinding.switchSevenDayPlacement.isChecked = showSevenDay
        dialogBinding.tvSevenDayPlacementDesc.text = activity.getString(
            if (showSevenDay) R.string.desc_seven_day_placement_menu else R.string.desc_seven_day_placement_inline
        )

        dialogBinding.switchSevenDayPlacement.setOnCheckedChangeListener { _, isChecked: Boolean ->
            locationPreferences.saveSevenDayAsMenuItem(isChecked)
            dialogBinding.tvSevenDayPlacementDesc.text = activity.getString(
                if (isChecked) R.string.desc_seven_day_placement_menu else R.string.desc_seven_day_placement_inline
            )
            val flags = locationPreferences.getMenuEnabledFlags().toMutableMap()
            flags[InlineSubTab.SEVEN_DAY] = isChecked
            locationPreferences.saveMenuEnabledFlags(flags)

            adapter.updateSevenDayPlacement(isChecked)
            checkActiveTabValidity()
            if (activity is MainActivity) {
                activity.astronomyManager.updateSevenDayForecastVisibility()
            }
        }

        // Stepper
        val initialSepCount = orgItems.count { it == "SEPARATOR" }
        dialogBinding.tvSeparatorCount.text = initialSepCount.toString()

        adapter = MenuOrganizationAdapter(
            context = activity,
            items = orgItems,
            enabledFlags = enabledFlags,
            defaultSubTab = defaultLaunchSubTab,
            palette = palette,
            showSevenDayInMenu = showSevenDay,
            onItemReordered = {
                locationPreferences.saveMenuEnabledFlags(enabledFlags)
                locationPreferences.saveMenuOrder(orgItems)
            },
            onDefaultChanged = { newDefault ->
                defaultLaunchSubTab = newDefault
                locationPreferences.saveDefaultLaunchSubTab(newDefault)
                locationPreferences.saveMenuEnabledFlags(enabledFlags)
            },
            onToggleDisabledActiveTab = {
                checkActiveTabValidity()
            }
        )

        dialogBinding.btnMinusSeparators.setOnClickListener {
            val currentOrder = locationPreferences.getMenuOrder().toMutableList()
            val count = currentOrder.count { it == "SEPARATOR" }
            if (count > 0) {
                currentOrder.removeAt(currentOrder.lastIndexOf("SEPARATOR"))
                locationPreferences.saveMenuOrder(currentOrder)
                adapter.updateItems(locationPreferences.getMenuOrder())
                dialogBinding.tvSeparatorCount.text = locationPreferences.getMenuOrder().count { it == "SEPARATOR" }.toString()
            }
        }

        dialogBinding.btnPlusSeparators.setOnClickListener {
            val currentOrder = locationPreferences.getMenuOrder().toMutableList()
            val count = currentOrder.count { it == "SEPARATOR" }
            if (count < 6) {
                currentOrder.add("SEPARATOR")
                locationPreferences.saveMenuOrder(currentOrder)
                adapter.updateItems(locationPreferences.getMenuOrder())
                dialogBinding.tvSeparatorCount.text = locationPreferences.getMenuOrder().count { it == "SEPARATOR" }.toString()
            }
        }

        dialogBinding.rvMenuOrgItems.layoutManager = LinearLayoutManager(activity)
        dialogBinding.rvMenuOrgItems.adapter = adapter
        dialogBinding.rvMenuOrgItems.isNestedScrollingEnabled = false
        dialogBinding.rvMenuOrgItems.setHasFixedSize(false)

        // Reset to Default button
        dialogBinding.btnResetDefaults.setOnClickListener {
            locationPreferences.saveMenuOrder(LocationPreferences.DEFAULT_MENU_ORDER)
            locationPreferences.saveMenuEnabledFlags(InlineSubTab.values().associateWith { true })
            locationPreferences.saveDefaultLaunchSubTab(InlineSubTab.CURRENT)
            locationPreferences.saveSevenDayAsMenuItem(false)

            checkActiveTabValidity()
            if (activity is MainActivity) {
                activity.astronomyManager.updateSevenDayForecastVisibility()
            }

            Toast.makeText(activity.applicationContext, "Reset to default settings", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            showMenuOrganizationScreen()
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    adapter.moveItem(fromPos, toPos)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })

        touchHelper.attachToRecyclerView(dialogBinding.rvMenuOrgItems)

        trackDialog(dialog)
        dialog.show()
    }

    fun showInfoSettingsScreen() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar)
        val dialogBinding = DialogInfoScreenBinding.inflate(activity.layoutInflater)
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
        dialogBinding.tvHeaderDesc.setTextColor(palette.textSecondary)

        val cards = listOf(
            dialogBinding.cardAbout to (dialogBinding.tvAboutTitle to dialogBinding.tvAboutDesc),
            dialogBinding.cardLicenses to (dialogBinding.tvLicensesTitle to dialogBinding.tvLicensesDesc),
            dialogBinding.cardPrivacy to (dialogBinding.tvPrivacyTitle to dialogBinding.tvPrivacyDesc),
            dialogBinding.cardRoadmap to (dialogBinding.tvRoadmapTitle to dialogBinding.tvRoadmapDesc)
        )

        for ((card, textPair) in cards) {
            card.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            card.strokeColor = palette.dividerColor
            textPair.first.setTextColor(palette.textPrimary)
            textPair.second.setTextColor(palette.textSecondary)
        }

        dialogBinding.cardAbout.setOnClickListener { showAboutSettingsScreen() }
        dialogBinding.cardLicenses.setOnClickListener { showLicensesSettingsScreen() }
        dialogBinding.cardPrivacy.setOnClickListener { showPrivacySettingsScreen() }
        dialogBinding.cardRoadmap.setOnClickListener { showRoadmapSettingsScreen() }

        trackDialog(dialog)
        dialog.show()
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
