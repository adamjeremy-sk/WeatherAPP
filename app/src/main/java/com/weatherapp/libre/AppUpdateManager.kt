package com.weatherapp.libre

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdateManager(
    private val activity: AppCompatActivity,
    private val locationPreferences: LocationPreferences
) {

    fun checkPlayStoreUpdate(silent: Boolean = false) {
        if (!silent) {
            Toast.makeText(activity.applicationContext, "Checking for WeatherAPP updates...", Toast.LENGTH_SHORT).show()
        }

        try {
            val playUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = playUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    showUpdateAvailablePrompt(appUpdateInfo, playUpdateManager)
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

    private fun showUpdateAvailablePrompt(
        appUpdateInfo: AppUpdateInfo,
        playUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val currentVersion = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.4.1"
        } catch (e: Exception) {
            "1.4.1"
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("🚀 Update Available!")
            .setMessage("A new version of WeatherAPP is available on the Google Play Store.\n\nYour current version: v$currentVersion\n\nUpdating gives you the latest weather features, model accuracy enhancements, and bug fixes.")
            .setPositiveButton("Update Now") { dialog, _ ->
                try {
                    val isFlexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    val isImmediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    if (isFlexibleAllowed || isImmediateAllowed) {
                        val updateType = if (isFlexibleAllowed) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
                        playUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.newBuilder(updateType).build(),
                            1002
                        )
                    } else {
                        openPlayStorePage()
                    }
                } catch (e: Exception) {
                    openPlayStorePage()
                }
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
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.4.1"
        } catch (e: Exception) {
            "1.4.1"
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

    private fun openPlayStorePage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        }
    }

    private fun openDonateUrl() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/adamhainstock"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity.applicationContext, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
