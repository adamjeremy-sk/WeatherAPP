package com.weatherapp.libre

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherapp.libre.databinding.ActivityGoldenHourBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class GoldenHourActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoldenHourBinding
    private val adapter = SolarPhaseAdapter()

    private var selectedDate: LocalDate = LocalDate.now()
    private var latitude: Double = 45.4215
    private var longitude: Double = -75.6972

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

    // Volume key rapid cycling handler
    private var isVolumeKeyHeld = false
    private var volumeKeyDirection = 0 // +1 for Up, -1 for Down
    private val handler = Handler(Looper.getMainLooper())
    private val rapidCycleRunnable = object : Runnable {
        override fun run() {
            if (isVolumeKeyHeld) {
                selectedDate = if (volumeKeyDirection > 0) selectedDate.plusDays(1) else selectedDate.minusDays(1)
                updateSolarDisplay()
                handler.postDelayed(this, 120L)
            }
        }
    }

    companion object {
        const val EXTRA_NAME = "extra_location_name"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityGoldenHourBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainGoldenHour) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val locationPreferences = LocationPreferences(this)
        val savedTheme = locationPreferences.getSavedThemeMode()
        val palette = BoutiqueThemeEngine.getThemePalette(savedTheme)

        binding.root.setBackgroundColor(palette.backgroundLight)
        binding.mainGoldenHour.setBackgroundColor(palette.backgroundLight)
        binding.cardHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        binding.cardHeader.strokeColor = palette.dividerColor
        binding.tvGoldenHourTitle.setTextColor(palette.textPrimary)
        binding.tvLocationName.setTextColor(palette.textSecondary)
        binding.btnBack.imageTintList = ColorStateList.valueOf(palette.textPrimary)

        binding.cardDateHeader.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
        binding.cardDateHeader.strokeColor = palette.dividerColor
        binding.tvSelectedDate.setTextColor(palette.primary)

        adapter.setPalette(palette)

        val locationName = intent.getStringExtra(EXTRA_NAME) ?: "Selected Location"
        latitude = intent.getDoubleExtra(EXTRA_LAT, 45.4215)
        longitude = intent.getDoubleExtra(EXTRA_LON, -75.6972)

        binding.tvLocationName.text = locationName
        binding.rvSolarPhases.layoutManager = LinearLayoutManager(this)
        binding.rvSolarPhases.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        binding.btnPrevDay.setOnClickListener {
            selectedDate = selectedDate.minusDays(1)
            updateSolarDisplay()
        }

        binding.btnNextDay.setOnClickListener {
            selectedDate = selectedDate.plusDays(1)
            updateSolarDisplay()
        }

        binding.layoutDateClickable.setOnClickListener {
            showDatePicker()
        }

        updateSolarDisplay()
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                updateSolarDisplay()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        datePicker.show()
    }

    private fun updateSolarDisplay() {
        binding.tvSelectedDate.text = if (selectedDate.isEqual(LocalDate.now())) {
            "TODAY • ${selectedDate.format(dateFormatter).uppercase(Locale.getDefault())}"
        } else {
            selectedDate.format(dateFormatter).uppercase(Locale.getDefault())
        }

        val observer = com.weatherapp.libre.solar.Observer(latitude, longitude)
        val zoneId = java.time.ZoneId.systemDefault()
        val phases = com.weatherapp.libre.solar.SolarLightingEngine.calculateChronologicalSolarPhases(
            selectedDate,
            zoneId,
            observer
        )

        adapter.submitList(phases)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (!isVolumeKeyHeld) {
                    isVolumeKeyHeld = true
                    volumeKeyDirection = 1
                    selectedDate = selectedDate.plusDays(1)
                    updateSolarDisplay()
                    handler.postDelayed(rapidCycleRunnable, 350L)
                }
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (!isVolumeKeyHeld) {
                    isVolumeKeyHeld = true
                    volumeKeyDirection = -1
                    selectedDate = selectedDate.minusDays(1)
                    updateSolarDisplay()
                    handler.postDelayed(rapidCycleRunnable, 350L)
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            isVolumeKeyHeld = false
            handler.removeCallbacks(rapidCycleRunnable)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
