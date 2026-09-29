package com.weatherapp.libre

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemDayForecastBinding
import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.MoonEventType
import com.weatherapp.libre.lunar.MoonPhaseName
import com.weatherapp.libre.lunar.Observer
import java.time.ZoneId

class DayForecastAdapter(
    private var unitSystem: UnitSystem = UnitSystem.METRIC
) : RecyclerView.Adapter<DayForecastAdapter.ViewHolder>() {

    private val items = mutableListOf<DayForecast>()
    private var palette: ColorPalette? = null
    private var currentLocation: LocationItem? = null
    private var locationTimezone: String? = null

    fun submitList(newList: List<DayForecast>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun setLocation(location: LocationItem, timezone: String? = null) {
        currentLocation = location
        locationTimezone = timezone
        notifyDataSetChanged()
    }

    fun setUnitSystem(newUnitSystem: UnitSystem) {
        if (unitSystem != newUnitSystem) {
            unitSystem = newUnitSystem
            notifyDataSetChanged()
        }
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayForecastBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemDayForecastBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DayForecast) {
            binding.tvDayName.text = item.dayOfWeek
            binding.tvDayDate.text = item.formattedDate
            binding.tvWeatherEmoji.text = item.iconEmoji
            binding.tvWeatherDesc.text = item.weatherDescription

            binding.tvTempHigh.text = UnitUtils.formatTemp(item.tempMax, unitSystem)
            binding.tvTempMin.text = UnitUtils.formatTemp(item.tempMin, unitSystem)

            val p = palette
            if (p != null) {
                binding.cardDay.setCardBackgroundColor(p.surfaceCard)
                binding.cardDay.strokeColor = p.dividerColor
                binding.tvDayName.setTextColor(p.textPrimary)
                binding.tvDayDate.setTextColor(p.textSecondary)
                binding.tvWeatherDesc.setTextColor(p.textSecondary)
                binding.tvTempHigh.setTextColor(p.tempHigh)
                binding.tvTempMin.setTextColor(p.tempLow)
                binding.layoutExpandedDetails.setBackgroundColor(p.surfaceCardSecondary)
                binding.tvExpandedMoonPhase.setTextColor(p.textPrimary)
                binding.tvExpandedDaylight.setTextColor(p.textPrimary)
                binding.tvExpandedSunriseSunset.setTextColor(p.textPrimary)
                binding.tvExpandedWindUv.setTextColor(p.textPrimary)
                binding.tvExpandedSunAzimuth.setTextColor(p.textSecondary)
                binding.tvExpandedMoonAzimuth.setTextColor(p.textSecondary)
            }

            // Populate Expanded Section Data
            val solar = item.solarTimes
            binding.tvExpandedDaylight.text = solar.daylightDurationText

            val sunriseStr = SolarCalculator.formatTime(solar.sunrise)
            val sunsetStr = SolarCalculator.formatTime(solar.sunset)
            binding.tvExpandedSunriseSunset.text = "$sunriseStr / $sunsetStr"

            val windStr = UnitUtils.formatWindSpeed(item.windSpeedMaxKmH, unitSystem, gustKmh = item.windGustsMaxKmH)
            binding.tvExpandedWindUv.text = "Wind: $windStr"

            binding.tvExpandedSunAzimuth.text = "Rise ${solar.sunriseAzimuthDegrees}° (${solar.sunriseCompassDirection}) • Set ${solar.sunsetAzimuthDegrees}° (${solar.sunsetCompassDirection})"

            // Calculate Lunar Phase, Moonrise/Set & Topocentric Azimuths using LunarCalculator
            val loc = currentLocation
            val zoneId = try {
                if (!locationTimezone.isNullOrEmpty()) ZoneId.of(locationTimezone) else ZoneId.systemDefault()
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }

            if (loc != null) {
                val observer = Observer(
                    latitudeDegrees = loc.latitude,
                    longitudeDegrees = loc.longitude
                )
                val targetInstant = item.date.atStartOfDay(zoneId).toInstant()
                val phaseInfo = LunarCalculator.phaseAt(targetInstant, observer)

                val phaseNameText = when (phaseInfo.phaseName) {
                    MoonPhaseName.NEW_MOON -> "New Moon 🌑"
                    MoonPhaseName.FIRST_QUARTER -> "First Quarter 🌓"
                    MoonPhaseName.FULL_MOON -> "Full Moon 🌕"
                    MoonPhaseName.LAST_QUARTER -> "Last Quarter 🌗"
                }

                binding.tvExpandedMoonPhase.text = "$phaseNameText (${(phaseInfo.illuminationFraction * 100).toInt()}%)"

                val moonEvents = LunarCalculator.moonEventsForLocalDate(item.date, zoneId, observer)
                val riseEvent = moonEvents.find { it.type == MoonEventType.MOONRISE }
                val setEvent = moonEvents.find { it.type == MoonEventType.MOONSET }

                val is24Hour = DateFormat.is24HourFormat(binding.root.context)

                val riseText = if (riseEvent != null) {
                    val timeStr = SolarCalculator.formatTime(riseEvent.instantUtc.atZone(zoneId).toLocalTime(), is24Hour)
                    "Rise $timeStr (${riseEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(riseEvent.azimuthDegrees)})"
                } else {
                    "No Rise"
                }

                val setText = if (setEvent != null) {
                    val timeStr = SolarCalculator.formatTime(setEvent.instantUtc.atZone(zoneId).toLocalTime(), is24Hour)
                    "Set $timeStr (${setEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(setEvent.azimuthDegrees)})"
                } else {
                    "No Set"
                }

                binding.tvExpandedMoonAzimuth.text = "$riseText • $setText"
            } else {
                binding.tvExpandedMoonPhase.text = "--"
                binding.tvExpandedMoonAzimuth.text = "--"
            }

            binding.layoutExpandedDetails.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

            binding.cardDay.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    item.isExpanded = !item.isExpanded
                    notifyItemChanged(pos)
                }
            }
        }
    }
}
