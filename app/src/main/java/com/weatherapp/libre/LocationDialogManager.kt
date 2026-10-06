package com.weatherapp.libre

import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.weatherapp.libre.databinding.ActivityMainBinding
import com.weatherapp.libre.databinding.DialogRecentLocationsBinding
import com.weatherapp.libre.databinding.DialogSearchLocationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationDialogManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val locationPreferences: LocationPreferences,
    private val repository: WeatherRepository,
    private val lifecycleScope: CoroutineScope
) {

    fun showSearchLocationDialog(
        isSettingDefaultLocation: Boolean = false,
        onLocationSelected: (LocationItem) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogSearchLocationBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        dialogBinding.root.setBackgroundColor(palette.surfaceCard)

        val searchAdapter = LocationSearchAdapter { selected ->
            if (isSettingDefaultLocation) {
                locationPreferences.saveStartupMode(LocationPreferences.STARTUP_FIXED_DEFAULT)
                locationPreferences.saveFixedDefaultLocation(selected)
                Toast.makeText(activity.applicationContext, "Default startup location set to ${selected.name}", Toast.LENGTH_SHORT).show()
            } else {
                locationPreferences.saveLastUsedLocation(selected)
                onLocationSelected(selected)
            }
            dialog.dismiss()
        }

        dialogBinding.rvLocationResults.layoutManager = LinearLayoutManager(activity)
        dialogBinding.rvLocationResults.adapter = searchAdapter

        dialogBinding.btnCloseDialog.setOnClickListener {
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
                val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
            dialogBinding.etSearchLocation.requestFocus()
        }
    }

    fun showRecentLocationsDialog(
        isSettingDefaultLocation: Boolean = false,
        onLocationSelected: (LocationItem) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val recentLocations = locationPreferences.getRecentLocations()
        if (recentLocations.isEmpty()) {
            showSearchLocationDialog(isSettingDefaultLocation = isSettingDefaultLocation, onLocationSelected = onLocationSelected)
            return
        }

        val dialog = BottomSheetDialog(activity)
        val dialogBinding = DialogRecentLocationsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        dialogBinding.root.setBackgroundColor(palette.surfaceCard)
        dialogBinding.tvRecentTitle.setTextColor(palette.textPrimary)
        dialogBinding.tvRecentDesc.setTextColor(palette.textSecondary)

        dialogBinding.layoutRecentList.removeAllViews()
        val density = activity.resources.displayMetrics.density

        for (loc in recentLocations) {
            val tv = TextView(activity).apply {
                text = "${loc.name}, ${loc.regionAndCountryText}"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(palette.textPrimary)
                setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
                val typedValue = TypedValue()
                activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                background = ContextCompat.getDrawable(activity, typedValue.resourceId)

                setOnClickListener {
                    if (isSettingDefaultLocation) {
                        locationPreferences.saveStartupMode(LocationPreferences.STARTUP_FIXED_DEFAULT)
                        locationPreferences.saveFixedDefaultLocation(loc)
                        Toast.makeText(activity.applicationContext, "Default startup location set to ${loc.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        locationPreferences.saveLastUsedLocation(loc)
                        onLocationSelected(loc)
                    }
                    dialog.dismiss()
                }
            }
            dialogBinding.layoutRecentList.addView(tv)
        }

        dialogBinding.btnSearchNewCity.setOnClickListener {
            dialog.dismiss()
            showSearchLocationDialog(isSettingDefaultLocation = isSettingDefaultLocation, onLocationSelected = onLocationSelected)
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }
}
