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
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            val iconRes = WeatherCodeUtils.conditionIconRes(item.weatherCode)
            binding.ivWeatherIcon.setImageResource(iconRes)
            binding.tvWeatherDesc.text = item.weatherDescription

            binding.tvTempHigh.text = UnitUtils.formatTemp(item.tempMax, unitSystem)
            binding.tvTempMin.text = UnitUtils.formatTemp(item.tempMin, unitSystem)

            val p = palette
            if (p != null) {
                binding.cardDay.setCardBackgroundColor(p.surfaceCard)
                binding.cardDay.strokeColor = p.dividerColor
                binding.tvDayName.setTextColor(p.textPrimary)
                binding.tvDayDate.setTextColor(p.textSecondary)
                binding.ivWeatherIcon.setColorFilter(p.brandPrimary)
                binding.tvWeatherDesc.setTextColor(p.textSecondary)
                binding.tvTempHigh.setTextColor(p.tempHigh)
                binding.tvTempMin.setTextColor(p.tempLow)
                binding.tvTempHigh.backgroundTintList = android.content.res.ColorStateList.valueOf(p.tempHighBg)
                binding.tvTempMin.backgroundTintList = android.content.res.ColorStateList.valueOf(p.tempLowBg)
                binding.layoutExpandedDetails.setBackgroundColor(p.surfaceCardSecondary)
                binding.tvExpandedMoonPhase.setTextColor(p.textPrimary)
                binding.tvExpandedDaylight.setTextColor(p.textPrimary)
                binding.tvExpandedSunriseSunset.setTextColor(p.textPrimary)
                binding.tvExpandedWindUv.setTextColor(p.textPrimary)
                binding.tvExpandedSunAzimuth.setTextColor(p.textSecondary)
                binding.tvExpandedMoonAzimuth.setTextColor(p.textSecondary)
            }

            val zoneId = try {
                if (!locationTimezone.isNullOrEmpty()) ZoneId.of(locationTimezone) else ZoneId.systemDefault()
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }

            val is24Hour = DateFormat.is24HourFormat(binding.root.context)

            // Populate Expanded Section Data via SolarDaySummary
            val solar = item.solarSummary

            val durationMins = solar?.daylightDurationMinutes
            if (durationMins != null) {
                val h = durationMins / 60
                val m = durationMins % 60
                binding.tvExpandedDaylight.text = "${h}h ${String.format(Locale.getDefault(), "%02dm", m)}"
            } else {
                binding.tvExpandedDaylight.text = "——"
            }

            fun formatInst(inst: java.time.Instant?): String {
                if (inst == null) return "——"
                val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                return inst.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }

            val srStr = formatInst(solar?.sunrise)
            val ssStr = formatInst(solar?.sunset)
            binding.tvExpandedSunriseSunset.text = "$srStr / $ssStr"

            val windStr = UnitUtils.formatWindSpeed(item.windSpeedMaxKmH, unitSystem, gustKmh = item.windGustsMaxKmH)
            binding.tvExpandedWindUv.text = "Wind: $windStr"

            val srAz = solar?.sunriseAzimuthDegrees
            val ssAz = solar?.sunsetAzimuthDegrees
            val srAzText = if (srAz != null) "$srAz° (${UnitUtils.getDegreesToCompass(srAz.toDouble())})" else "—"
            val ssAzText = if (ssAz != null) "$ssAz° (${UnitUtils.getDegreesToCompass(ssAz.toDouble())})" else "—"
            binding.tvExpandedSunAzimuth.text = "Rise $srAzText • Set $ssAzText"

            // Calculate Lunar Phase & Moonrise/Set via LunarCalculator
            val loc = currentLocation
            if (loc != null) {
                val observer = Observer(
                    latitudeDegrees = loc.latitude,
                    longitudeDegrees = loc.longitude,
                    elevationMeters = loc.elevationMeters
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

                val timeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())

                val riseText = if (riseEvent != null) {
                    val timeStr = riseEvent.instantUtc.atZone(zoneId).format(timeFormatter)
                    "Rise $timeStr (${riseEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(riseEvent.azimuthDegrees)})"
                } else {
                    "No Rise"
                }

                val setText = if (setEvent != null) {
                    val timeStr = setEvent.instantUtc.atZone(zoneId).format(timeFormatter)
                    "Set $timeStr (${setEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(setEvent.azimuthDegrees)})"
                } else {
                    "No Set"
                }

                binding.tvExpandedMoonAzimuth.text = "$riseText • $setText"
            } else {
                binding.tvExpandedMoonPhase.text = "——"
                binding.tvExpandedMoonAzimuth.text = "——"
            }

            ExpandCollapseHelper.togglePanel(binding.layoutExpandedDetails, item.isExpanded, binding.cardDay)
            ExpandCollapseHelper.setupExpandableAccessibility(
                binding.cardDay,
                "${item.dayOfWeek}, ${item.formattedDate}, ${item.weatherDescription}",
                item.isExpanded
            )

            binding.cardDay.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    item.isExpanded = !item.isExpanded
                    ExpandCollapseHelper.togglePanel(binding.layoutExpandedDetails, item.isExpanded, binding.cardDay)
                    ExpandCollapseHelper.setupExpandableAccessibility(
                        binding.cardDay,
                        "${item.dayOfWeek}, ${item.formattedDate}, ${item.weatherDescription}",
                        item.isExpanded
                    )
                    notifyItemChanged(pos)
                }
            }
        }
    }
}
