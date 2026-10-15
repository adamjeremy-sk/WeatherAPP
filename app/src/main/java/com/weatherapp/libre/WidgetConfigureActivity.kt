package com.weatherapp.libre

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var widgetPreferences: WidgetPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_configure)
        widgetPreferences = WidgetPreferences(this)

        val cbShowNightVis = findViewById<CheckBox>(R.id.cb_show_night_vis)
        val cbShowSolunar = findViewById<CheckBox>(R.id.cb_show_solunar)
        val btnSave = findViewById<Button>(R.id.btn_save_widget)

        cbShowNightVis.isChecked = widgetPreferences.getShowNightVis(appWidgetId)
        cbShowSolunar.isChecked = widgetPreferences.getShowSolunar(appWidgetId)

        btnSave.setOnClickListener {
            widgetPreferences.saveShowNightVis(appWidgetId, cbShowNightVis.isChecked)
            widgetPreferences.saveShowSolunar(appWidgetId, cbShowSolunar.isChecked)

            WidgetSyncWorker.rebuildAndUpdate(applicationContext, appWidgetId)
            WidgetSyncWorker.enqueueImmediate(applicationContext)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
