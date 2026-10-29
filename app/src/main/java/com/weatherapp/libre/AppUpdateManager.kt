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
            Toast.makeText(activity.applicationContext, R.string.toast_checking_updates, Toast.LENGTH_SHORT).show()
        }

        try {
            val playUpdateManager = AppUpdateManagerFactory.create(activity)
            val appUpdateInfoTask = playUpdateManager.appUpdateInfo

            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                val remoteCode = appUpdateInfo.availableVersionCode()
                val localCode = BuildConfig.VERSION_CODE
                val isAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

                if (isAvailable && remoteCode > localCode) {
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

    fun handleRemoteVersionCheck(remoteVersionTag: String?, silent: Boolean = true) {
        val remoteCode = parseVersionCode(remoteVersionTag)
        val localCode = BuildConfig.VERSION_CODE

        if (remoteCode > localCode) {
            showUpdateAvailablePrompt(null)
        } else {
            if (!silent) {
                showUpToDatePrompt()
            }
        }
    }

    fun parseVersionCode(tag: String?): Int {
        if (tag.isNullOrBlank()) return 0
        val vcMatch = Regex("""VC(\d+)""", RegexOption.IGNORE_CASE).find(tag)
        if (vcMatch != null) {
            return vcMatch.groupValues[1].toIntOrNull() ?: 0
        }
        val digitsOnly = tag.replace(Regex("""[^\d]"""), "")
        return digitsOnly.toIntOrNull() ?: 0
    }

    private fun showUpdateAvailablePrompt(appUpdateInfo: AppUpdateInfo?) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.title_update_notification))
            .setMessage(activity.getString(R.string.msg_update_available_libre))
            .setPositiveButton(activity.getString(R.string.btn_view_update)) { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.btn_not_now)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)
        dialog.show()
    }

    private fun showUpToDatePrompt() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.title_app_up_to_date))
            .setMessage(activity.getString(R.string.msg_running_latest_version))
            .setPositiveButton(activity.getString(R.string.btn_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)
        dialog.show()
    }

    private fun showCheckUpdateFallbackDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.title_check_play_store))
            .setMessage(activity.getString(R.string.msg_check_play_store))
            .setPositiveButton(activity.getString(R.string.btn_open_play_store)) { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)
        dialog.show()
    }

    fun show500LaunchMilestoneDialog() {
        if (activity.isFinishing || activity.isDestroyed) return

        locationPreferences.resetAppLaunchCount()
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.title_milestone_500_launches))
            .setMessage(activity.getString(R.string.msg_milestone_500_launches))
            .setPositiveButton(activity.getString(R.string.btn_leave_review)) { dialog, _ ->
                openPlayStorePage()
                dialog.dismiss()
            }
            .setNeutralButton(activity.getString(R.string.btn_donate_kofi)) { dialog, _ ->
                openDonateUrl()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.btn_maybe_later)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        SystemBarContrastHelper.applyDialogEdgeToEdge(dialog)
        dialog.show()
    }

    fun openPlayStorePage() {
        val packageName = activity.packageName
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
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(UrlConfig.URL_KOFI)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity.applicationContext, activity.getString(R.string.toast_could_not_open_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
