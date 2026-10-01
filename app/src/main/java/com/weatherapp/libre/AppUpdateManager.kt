package com.weatherapp.libre

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdateManager(
    private val activity: AppCompatActivity,
    private val locationPreferences: LocationPreferences
) {

    fun checkPlayStoreUpdate(silent: Boolean = true) {
        if (!silent) {
            Toast.makeText(activity.applicationContext, "Checking for WeatherAPP updates...", Toast.LENGTH_SHORT).show()
        }

        try {
            val playUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = playUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    showUpdateAvailablePrompt(appUpdateInfo)
                } else {
                    if (!silent) {
                        showUpToDatePrompt()
                    }
                }
            }.addOnFailureListener {
                if (!silent) {
                    showCheckUpdateFallbackDialog()
                }
            }
        } catch (e: Exception) {
            if (!silent) {
                openPlayStorePage()
            }
        }
    }

    private fun showUpdateAvailablePrompt(appUpdateInfo: AppUpdateInfo) {
        if (activity.isFinishing || activity.isDestroyed) return

        val currentVersion = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.4.2"
        } catch (e: Exception) {
            "0.4.2"
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("🚀 Update Available!")
            .setMessage("A new version of WeatherAPP is available on the Google Play Store.\n\nYour current version: v$currentVersion\n\nWould you like to open the Play Store and update WeatherAPP now?")
            .setPositiveButton("Update Now") { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showUpToDatePrompt() {
        if (activity.isFinishing || activity.isDestroyed) return

        val currentVersion = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.4.2"
        } catch (e: Exception) {
            "0.4.2"
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("WeatherAPP is Up to Date")
            .setMessage("You are running the latest version of WeatherAPP (v$currentVersion).\n\nNo new updates are available at this time.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showCheckUpdateFallbackDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        MaterialAlertDialogBuilder(activity)
            .setTitle("Check Play Store for Updates")
            .setMessage("Would you like to open the Google Play Store page to check for WeatherAPP updates?")
            .setPositiveButton("Open Play Store") { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun show500LaunchMilestoneDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        locationPreferences.resetAppLaunchCount()
        MaterialAlertDialogBuilder(activity)
            .setTitle("🎉 500 App Launches!")
            .setMessage("Thank you for using WeatherAPP! You have opened WeatherAPP 500 times.\n\nAs a 100% free and ad-free community app, your review or donation keeps WeatherAPP growing for everyone.")
            .setPositiveButton("Leave a Review") { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNeutralButton("Donate (Ko-fi)") { dialog, _ ->
                openDonateUrl()
                dialog.dismiss()
            }
            .setNegativeButton("Maybe Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun openPlayStorePage() {
        val packageName = "com.weatherapp.libre"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }

    private fun openDonateUrl() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/adamhainstock")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity.applicationContext, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
