package com.weatherapp.libre.alerts

import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.weatherapp.theme.BoutiqueThemeEngine
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.R

object AlertConsentDialog {

    private var activeDialog: Dialog? = null

    fun checkAndShowConsentPrompt(
        activity: AppCompatActivity,
        locationPreferences: LocationPreferences,
        onConsentCompleted: (() -> Unit)? = null
    ) {
        if (locationPreferences.getAlertConsentState() != LocationPreferences.ALERT_CONSENT_UNSET) {
            onConsentCompleted?.invoke()
            return
        }

        if (activeDialog?.isShowing == true) return
        if (activity.isFinishing || activity.isDestroyed) return

        val palette = BoutiqueThemeEngine.getThemePalette(locationPreferences.getSavedThemeMode())
        val density = activity.resources.displayMetrics.density

        val dialog = Dialog(activity)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        activeDialog = dialog

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (activeDialog == dialog) {
                    if (dialog.isShowing) {
                        try {
                            dialog.dismiss()
                        } catch (ignored: Exception) {}
                    }
                    activeDialog = null
                }
            }
        })

        dialog.setOnDismissListener {
            if (activeDialog == dialog) {
                activeDialog = null
            }
        }

        val rootCard = MaterialCardView(activity).apply {
            radius = 20 * density
            strokeWidth = (1 * density).toInt()
            strokeColor = palette.dividerColor
            setCardBackgroundColor(palette.surfaceCard)
            layoutParams = ViewGroup.LayoutParams((320 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt(), (24 * density).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleTv = TextView(activity).apply {
            text = activity.getString(R.string.alert_consent_title)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
        }

        val bodyTv = TextView(activity).apply {
            text = activity.getString(R.string.alert_consent_message)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.textSecondary)
            setPadding(0, (12 * density).toInt(), 0, (20 * density).toInt())
            gravity = Gravity.START
        }

        val btnEnable = MaterialButton(activity).apply {
            text = activity.getString(R.string.alert_consent_btn_enable)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            setBackgroundColor(palette.brandPrimary)
            setTextColor(Color.WHITE)
            cornerRadius = (12 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (48 * density).toInt())
        }

        val btnNoThanks = MaterialButton(activity, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = activity.getString(R.string.alert_consent_btn_no_thanks)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.textSecondary)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt()).apply {
                topMargin = (8 * density).toInt()
            }
        }

        btnEnable.setOnClickListener {
            locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_IN)

            AlertNotificationHelper.createNotificationChannels(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
            com.weatherapp.libre.alerts.workers.AlertSyncWorker.schedule(activity.applicationContext)

            dialog.dismiss()
            onConsentCompleted?.invoke()
        }

        btnNoThanks.setOnClickListener {
            locationPreferences.saveAlertConsentState(LocationPreferences.ALERT_CONSENT_OPT_OUT)

            com.weatherapp.libre.alerts.workers.AlertSyncWorker.cancel(activity.applicationContext)

            dialog.dismiss()
            onConsentCompleted?.invoke()
        }

        container.addView(titleTv)
        container.addView(bodyTv)
        container.addView(btnEnable)
        container.addView(btnNoThanks)
        rootCard.addView(container)

        dialog.setContentView(rootCard)
        dialog.show()
    }
}
