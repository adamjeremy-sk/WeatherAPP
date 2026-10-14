package com.weatherapp.libre.almanac

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.weatherapp.libre.LocationPreferences
import com.weatherapp.libre.R
import com.weatherapp.libre.lunar.EclipseType
import com.weatherapp.libre.lunar.LunarCalculator
import java.time.ZoneId
import kotlin.math.roundToInt

object CalendarIntentHelper {

    fun launchCalendarEvent(context: Context, day: MoonAlmanacDay) {
        val prefs = LocationPreferences(context)
        val leadMins = prefs.getLunarReminderLeadTimeMinutes()

        val leadStr = when (leadMins) {
            60 -> context.getString(R.string.lead_time_1_hour)
            720 -> context.getString(R.string.lead_time_12_hours)
            2880 -> context.getString(R.string.lead_time_48_hours)
            else -> context.getString(R.string.lead_time_24_hours)
        }

        val eventTitle = if (day.eclipseInfo != null) {
            when (day.eclipseInfo.type) {
                EclipseType.TOTAL -> context.getString(R.string.eclipse_total)
                EclipseType.PARTIAL -> context.getString(R.string.eclipse_partial)
                EclipseType.PENUMBRAL -> context.getString(R.string.eclipse_penumbral)
            }
        } else {
            val formattedPhase = LunarCalculator.formatPhaseDisplay(day.primaryPhase)
            formattedPhase.phaseNameWithEmoji
        }

        val pct = (day.primaryPhase.illuminationFraction * 100).roundToInt().coerceIn(0, 100)
        val eventDesc = context.getString(R.string.calendar_event_desc_format, pct, leadStr)

        val startMillis = if (day.eclipseInfo != null) {
            day.eclipseInfo.peakInstant.toEpochMilli()
        } else if (day.exactPhasePeakTime != null) {
            day.exactPhasePeakTime.toEpochMilli()
        } else {
            day.date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, eventTitle)
            putExtra(CalendarContract.Events.DESCRIPTION, eventDesc)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 3600000L) // 1 hour duration
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle if no calendar app is installed
        }
    }
}
