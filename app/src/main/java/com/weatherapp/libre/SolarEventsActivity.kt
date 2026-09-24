package com.weatherapp.libre

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.weatherapp.libre.databinding.ActivitySolarEventsBinding
import java.time.ZoneId

class SolarEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySolarEventsBinding
    private lateinit var adapter: AstronomicalEventAdapter

    private var pastEvents = listOf<AstronomicalEventItem>()
    private var upcomingEvents = listOf<AstronomicalEventItem>()

    companion object {
        const val EXTRA_NAME = "extra_location_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySolarEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainSolarEvents) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val locationPreferences = LocationPreferences(this)
        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        binding.root.setBackgroundColor(palette.backgroundLight)
        binding.mainSolarEvents.setBackgroundColor(palette.backgroundLight)
        binding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        binding.cardHeader.strokeColor = palette.dividerColor
        binding.tvSolarEventsTitle.setTextColor(palette.textPrimary)
        binding.tvLocationName.setTextColor(palette.textSecondary)
        binding.btnBack.imageTintList = ColorStateList.valueOf(palette.textPrimary)
        binding.tabLayoutEvents.setBackgroundColor(palette.surfaceCard)

        val locationName = intent.getStringExtra(EXTRA_NAME) ?: "Selected Location"
        binding.tvLocationName.text = locationName

        binding.btnBack.setOnClickListener { finish() }

        adapter = AstronomicalEventAdapter { event ->
            promptAddCalendarEvent(event)
        }
        adapter.setPalette(palette)

        binding.rvSolarEvents.layoutManager = LinearLayoutManager(this)
        binding.rvSolarEvents.adapter = adapter

        // Generate events covering -12 months to +12 months
        val (past, upcoming) = AstronomicalEventsRepository.generateEvents()
        pastEvents = past
        upcomingEvents = upcoming

        setupTabs()
        checkFirstLaunchGuide()
    }

    private fun promptAddCalendarEvent(event: AstronomicalEventItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Add to Calendar")
            .setMessage("Would you like to add '${event.title}' (${event.dateText}) to your device calendar?")
            .setPositiveButton("Add Event") { dialog, _ ->
                addEventToCalendar(event)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addEventToCalendar(event: AstronomicalEventItem) {
        try {
            val startMillis = event.eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = startMillis + (2 * 60 * 60 * 1000L)

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(CalendarContract.Events.DESCRIPTION, "${event.shortDescription}\n\n${event.detailedDescription}")
                putExtra(CalendarContract.Events.EVENT_LOCATION, "Outdoor Night Sky")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Calendar application.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkFirstLaunchGuide() {
        val prefs = LocationPreferences(this)
        if (prefs.isFirstLaunchSolarEvents()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Solar & Astronomical Events Guide")
                .setMessage("• Tap any event card to expand and view detailed astronomical explanations.\n\n• Long-press any event card to add it directly to your device calendar!")
                .setPositiveButton("Got It") { dialog, _ ->
                    prefs.setSolarEventsFirstLaunchCompleted()
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun setupTabs() {
        val tabLayout = binding.tabLayoutEvents

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showEventsList(pastEvents)
                    1 -> showEventsList(upcomingEvents)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Select "Upcoming Events" tab by default
        val upcomingTab = tabLayout.getTabAt(1)
        if (upcomingTab != null) {
            tabLayout.selectTab(upcomingTab)
            showEventsList(upcomingEvents)
        } else {
            showEventsList(upcomingEvents)
        }
    }

    private fun showEventsList(list: List<AstronomicalEventItem>) {
        if (list.isEmpty()) {
            binding.tvEmptyEvents.visibility = View.VISIBLE
            binding.rvSolarEvents.visibility = View.GONE
        } else {
            binding.tvEmptyEvents.visibility = View.GONE
            binding.rvSolarEvents.visibility = View.VISIBLE
            adapter.submitList(list)
        }
    }
}
