package com.weatherapp.libre

import com.weatherapp.theme.ColorPalette
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
import com.weatherapp.libre.lunar.getEmoji
import com.weatherapp.libre.lunar.getNameRes
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
            val localizedDesc = WeatherCodeUtils.getWeatherDescription(binding.root.context, item.weatherCode)
            binding.tvWeatherDesc.text = localizedDesc

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
                binding.tvExpandedPrecipSum.setTextColor(p.textPrimary)
                binding.tvExpandedPrecipProb.setTextColor(p.textPrimary)
                binding.tvExpandedSunAzimuth.setTextColor(p.textPrimary)
                binding.tvExpandedMoonAzimuth.setTextColor(p.textPrimary)
            }

            val zoneId = try {
                if (!locationTimezone.isNullOrEmpty()) ZoneId.of(locationTimezone) else ZoneId.systemDefault()
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }

            val context = binding.root.context
            val is24Hour = DateFormat.is24HourFormat(context)

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

            val windVal = UnitUtils.formatWindSpeed(item.windSpeedMaxKmH, unitSystem, item.windDirectionDominant)
            val windLine = context.getString(R.string.wind_line_format, windVal)
            val gustVal = UnitUtils.formatWindGusts(item.windSpeedMaxKmH, item.windGustsMaxKmH, unitSystem)
            if (gustVal != null) {
                val gustsLine = context.getString(R.string.gusts_line_format, gustVal)
                binding.tvExpandedWindUv.text = joinTwoLines(windLine, gustsLine)
            } else {
                binding.tvExpandedWindUv.text = windLine
            }

            binding.tvExpandedPrecipSum.text = UnitUtils.formatPrecipitation(item.precipitationSumMm, unitSystem)
            binding.tvExpandedPrecipProb.text = UnitUtils.formatProbability(item.precipitationProbabilityMax)

            val srAz = solar?.sunriseAzimuthDegrees
            val ssAz = solar?.sunsetAzimuthDegrees
            val srAzText = if (srAz != null) "$srAz° (${UnitUtils.getDegreesToCompass(srAz.toDouble())})" else context.getString(R.string.azimuth_unavailable)
            val ssAzText = if (ssAz != null) "$ssAz° (${UnitUtils.getDegreesToCompass(ssAz.toDouble())})" else context.getString(R.string.azimuth_unavailable)
            val sunRiseLine = context.getString(R.string.position_rise_format, srAzText)
            val sunSetLine = context.getString(R.string.position_set_format, ssAzText)
            binding.tvExpandedSunAzimuth.text = joinTwoLines(sunRiseLine, sunSetLine)

            // Calculate Lunar Phase & Moonrise/Set via LunarCalculator
            val loc = currentLocation
            if (loc != null) {
                val observer = Observer(
                    latitudeDegrees = loc.latitude,
                    longitudeDegrees = loc.longitude,
                    elevationMeters = loc.elevationMeters
                )
                val targetInstant = item.date.atStartOfDay(zoneId).toInstant()
                val phaseInfo = LunarCalculator.phaseAt(targetInstant)

                val phaseNameText = "${binding.root.context.getString(phaseInfo.phaseCategory.getNameRes())} ${phaseInfo.phaseCategory.getEmoji()}"

                binding.tvExpandedMoonPhase.text = "$phaseNameText (${(phaseInfo.illuminationFraction * 100).toInt()}%)"

                val moonEvents = LunarCalculator.moonEventsForLocalDate(item.date, zoneId, observer)
                val riseEvent = moonEvents.find { it.type == MoonEventType.MOONRISE }
                val setEvent = moonEvents.find { it.type == MoonEventType.MOONSET }

                val timeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())

                val riseLine = if (riseEvent != null) {
                    val timeStr = riseEvent.instantUtc.atZone(zoneId).format(timeFormatter)
                    val segment = "$timeStr (${riseEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(riseEvent.azimuthDegrees)})"
                    context.getString(R.string.position_rise_format, segment)
                } else {
                    context.getString(R.string.position_no_rise)
                }

                val setLine = if (setEvent != null) {
                    val timeStr = setEvent.instantUtc.atZone(zoneId).format(timeFormatter)
                    val segment = "$timeStr (${setEvent.azimuthDegrees.toInt()}° ${UnitUtils.getDegreesToCompass(setEvent.azimuthDegrees)})"
                    context.getString(R.string.position_set_format, segment)
                } else {
                    context.getString(R.string.position_no_set)
                }

                binding.tvExpandedMoonAzimuth.text = joinTwoLines(riseLine, setLine)
            } else {
                binding.tvExpandedMoonPhase.text = "——"
                binding.tvExpandedMoonAzimuth.text = context.getString(R.string.position_unavailable)
            }

            ExpandCollapseHelper.togglePanel(binding.layoutExpandedDetails, item.isExpanded, binding.cardDay)
            ExpandCollapseHelper.setupExpandableAccessibility(
                binding.cardDay,
                "${item.dayOfWeek}, ${item.formattedDate}, $localizedDesc",
                item.isExpanded
            )

            binding.cardDay.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    item.isExpanded = !item.isExpanded
                    ExpandCollapseHelper.togglePanel(binding.layoutExpandedDetails, item.isExpanded, binding.cardDay)
                    ExpandCollapseHelper.setupExpandableAccessibility(
                        binding.cardDay,
                        "${item.dayOfWeek}, ${item.formattedDate}, $localizedDesc",
                        item.isExpanded
                    )
                    notifyItemChanged(pos)
                }
            }
        }
    }

    companion object {
        fun joinTwoLines(line1: String, line2: String): String {
            return "$line1\n$line2"
        }
    }
}
