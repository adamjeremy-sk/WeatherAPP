package com.weatherapp.libre

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.weatherapp.libre.databinding.ActivityMainBinding

class EasterEggManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding
) {

    private var lowTempTapCount = 0
    private var lastLowTempTapTime = 0L
    private var highTempTapCount = 0
    private var lastHighTempTapTime = 0L

    fun handleDailyLowTap() {
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

    fun handleDailyHighTap() {
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

    fun startSnowEffect() {
        stopShimmerEffect()
        binding.snowflakeOverlay.startSnowing()
        binding.root.findViewById<android.view.View>(R.id.btnDismissLowEffect)?.visibility = View.VISIBLE
        Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_snowfall_activated), Toast.LENGTH_SHORT).show()
    }

    fun stopSnowEffect() {
        binding.snowflakeOverlay.stopSnowing()
        binding.root.findViewById<android.view.View>(R.id.btnDismissLowEffect)?.visibility = View.GONE
    }

    fun startShimmerEffect() {
        stopSnowEffect()
        binding.heatShimmerOverlay.startHeatShimmer()
        binding.root.findViewById<android.view.View>(R.id.btnDismissHighEffect)?.visibility = View.VISIBLE
        Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_heat_shimmer_activated), Toast.LENGTH_SHORT).show()
    }

    fun stopShimmerEffect() {
        binding.heatShimmerOverlay.stopHeatShimmer()
        binding.root.findViewById<android.view.View>(R.id.btnDismissHighEffect)?.visibility = View.GONE
    }
}
