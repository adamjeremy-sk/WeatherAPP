package com.weatherapp.libre

import android.content.Context
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.weatherapp.libre.databinding.ItemHourlyDividerBinding
import com.weatherapp.libre.databinding.ItemHourlyFooterBinding
import com.weatherapp.libre.databinding.ItemHourlyRowBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HourlyForecastAdapter(
    private var unitSystem: UnitSystem = UnitSystem.METRIC
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ROW = 0
        private const val TYPE_DIVIDER = 1
        private const val TYPE_FOOTER = 2
    }

    private val items = mutableListOf<HourlyListItem>()
    private var palette: ColorPalette? = null
    private var locationTimezone: String? = null
    private var expandedInstant: Instant? = null

    fun submitList(newList: List<HourlyListItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun setLocationTimezone(timezone: String?) {
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

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HourlyListItem.HourRow -> TYPE_ROW
            is HourlyListItem.DayDivider -> TYPE_DIVIDER
            is HourlyListItem.HorizonFooter -> TYPE_FOOTER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ROW -> RowViewHolder(ItemHourlyRowBinding.inflate(inflater, parent, false))
            TYPE_DIVIDER -> DividerViewHolder(ItemHourlyDividerBinding.inflate(inflater, parent, false))
            TYPE_FOOTER -> FooterViewHolder(ItemHourlyFooterBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HourlyListItem.HourRow -> (holder as RowViewHolder).bind(item)
            is HourlyListItem.DayDivider -> (holder as DividerViewHolder).bind(item)
            is HourlyListItem.HorizonFooter -> (holder as FooterViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0f
        } catch (e: Exception) {
            false
        }
    }

    inner class RowViewHolder(
        val binding: ItemHourlyRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyListItem.HourRow) {
            val forecast = item.forecast
            val context = binding.root.context
            val is24Hour = DateFormat.is24HourFormat(context)

            val zoneId = try {
                if (!locationTimezone.isNullOrEmpty()) ZoneId.of(locationTimezone) else ZoneId.systemDefault()
            } catch (e: Exception) {
                ZoneId.systemDefault()
            }

            // Column 1: Time Label ("Now" for current hour)
            val timeText = if (item.isCurrentHour) {
                "Now"
            } else {
                val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                forecast.time.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
            binding.tvHourlyTime.text = timeText

            // Column 2: Condition Icon & Text
            val code = forecast.weatherCode ?: 0
            val iconRes = WeatherCodeUtils.conditionIconRes(code)
            binding.ivHourlyConditionIcon.setImageResource(iconRes)
            val conditionText = WeatherCodeUtils.getWeatherDescription(code)
            binding.tvHourlyConditionText.text = conditionText

            // Column 3: Temp & Precip Probability
            val tempText = forecast.temperature?.let { UnitUtils.formatTemp(it, unitSystem) } ?: "——"
            binding.tvHourlyTemp.text = tempText

            val precipProb = forecast.precipitationProbability
            if (precipProb != null && precipProb > 0) {
                binding.layoutPrecip.visibility = View.VISIBLE
                binding.tvPrecipProb.text = "$precipProb%"
            } else {
                binding.layoutPrecip.visibility = View.GONE
            }

            // Accordion Expansion State
            val isExpanded = (expandedInstant == forecast.time)
            binding.layoutExpandedPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Bind Stat Grid inside Expanded Panel
            bindStatGrid(forecast, zoneId)

            // Theme & Palette Styling
            val p = palette
            if (p != null) {
                val isHighlighted = item.isCurrentHour
                binding.cardHourlyRow.setCardBackgroundColor(
                    if (isHighlighted) p.surfaceCardSecondary else p.surfaceCard
                )
                binding.cardHourlyRow.strokeColor = if (isHighlighted) p.brandPrimary else p.dividerColor
                val strokeWidthDp = if (isHighlighted) 2.5f else 1.0f
                binding.cardHourlyRow.strokeWidth = (strokeWidthDp * context.resources.displayMetrics.density).toInt()

                binding.tvHourlyTime.setTextColor(if (isHighlighted) p.brandPrimary else p.textPrimary)
                binding.tvHourlyConditionText.setTextColor(p.textPrimary)
                binding.tvHourlyTemp.setTextColor(p.textPrimary)
                binding.ivHourlyConditionIcon.setColorFilter(p.brandPrimary)
                binding.tvDroplet.setTextColor(p.brandPrimary)
                binding.tvPrecipProb.setTextColor(p.brandPrimary)

                binding.layoutExpandedPanel.setBackgroundColor(p.surfaceCardSecondary)
                binding.tvStatFeelsLike.setTextColor(p.textPrimary)
                binding.tvStatWind.setTextColor(p.textPrimary)
                binding.tvStatHumidity.setTextColor(p.textPrimary)
                binding.tvStatDewPoint.setTextColor(p.textPrimary)
                binding.tvStatPrecipAmount.setTextColor(p.textPrimary)
                binding.tvStatCloudCover.setTextColor(p.textPrimary)
                binding.tvStatVisibility.setTextColor(p.textPrimary)
                binding.tvStatUvIndex.setTextColor(p.textPrimary)
            }

            // Accessibility Spoken Summary
            val precipSummary = if (precipProb != null && precipProb > 0) ", precipitation chance $precipProb percent" else ""
            val stateSummary = if (isExpanded) ", expanded" else ", collapsed"
            binding.cardHourlyRow.contentDescription = "$timeText, $conditionText, $tempText$precipSummary$stateSummary"

            // Click listener to toggle expansion accordion
            binding.cardHourlyRow.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val newExpanded = if (expandedInstant == forecast.time) null else forecast.time
                    expandedInstant = newExpanded

                    if (!isReducedMotionEnabled(context) && itemView.parent is ViewGroup) {
                        TransitionManager.beginDelayedTransition(itemView.parent as ViewGroup)
                    }
                    notifyDataSetChanged()
                }
            }
        }

        private fun bindStatGrid(forecast: HourlyForecast, zoneId: ZoneId) {
            // Stat 1: Feels like
            val feelsLike = forecast.apparentTemperature
            if (feelsLike != null) {
                binding.tvStatFeelsLike.visibility = View.VISIBLE
                binding.tvStatFeelsLike.text = "Feels like: ${UnitUtils.formatTemp(feelsLike, unitSystem)}"
            } else {
                binding.tvStatFeelsLike.visibility = View.GONE
            }

            // Stat 2: Wind
            val windSpeed = forecast.windSpeed
            if (windSpeed != null) {
                binding.tvStatWind.visibility = View.VISIBLE
                binding.tvStatWind.text = "Wind: ${UnitUtils.formatWindSpeed(windSpeed, unitSystem, directionDeg = forecast.windDirection, gustKmh = forecast.windGusts)}"
            } else {
                binding.tvStatWind.visibility = View.GONE
            }

            // Stat 3: Humidity
            val hum = forecast.humidity
            if (hum != null) {
                binding.tvStatHumidity.visibility = View.VISIBLE
                binding.tvStatHumidity.text = "Humidity: $hum%"
            } else {
                binding.tvStatHumidity.visibility = View.GONE
            }

            // Stat 4: Dew point
            val dew = forecast.dewPoint
            if (dew != null) {
                binding.tvStatDewPoint.visibility = View.VISIBLE
                binding.tvStatDewPoint.text = "Dew point: ${UnitUtils.formatTemp(dew, unitSystem)}"
            } else {
                binding.tvStatDewPoint.visibility = View.GONE
            }

            // Stat 5: Precipitation amount
            val precip = forecast.precipitation
            if (precip != null) {
                binding.tvStatPrecipAmount.visibility = View.VISIBLE
                binding.tvStatPrecipAmount.text = "Precipitation: ${UnitUtils.formatPrecipitation(precip, unitSystem)}"
            } else {
                binding.tvStatPrecipAmount.visibility = View.GONE
            }

            // Stat 6: Cloud cover
            val cloud = forecast.cloudCover
            if (cloud != null) {
                binding.tvStatCloudCover.visibility = View.VISIBLE
                binding.tvStatCloudCover.text = "Cloud cover: $cloud%"
            } else {
                binding.tvStatCloudCover.visibility = View.GONE
            }

            // Stat 7: Visibility
            val vis = forecast.visibility
            if (vis != null) {
                binding.tvStatVisibility.visibility = View.VISIBLE
                binding.tvStatVisibility.text = "Visibility: ${UnitUtils.formatVisibility(vis, unitSystem)}"
            } else {
                binding.tvStatVisibility.visibility = View.GONE
            }

            // Stat 8: UV Index (Omit at night when UV index is null or <= 0.0)
            val uv = forecast.uvIndex
            if (uv != null && uv > 0.0) {
                binding.tvStatUvIndex.visibility = View.VISIBLE
                binding.tvStatUvIndex.text = String.format(Locale.getDefault(), "UV index: %.1f", uv)
            } else {
                binding.tvStatUvIndex.visibility = View.GONE
            }
        }
    }

    inner class DividerViewHolder(
        val binding: ItemHourlyDividerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyListItem.DayDivider) {
            binding.tvDividerLabel.text = item.dateLabel
            val p = palette
            if (p != null) {
                binding.tvDividerLabel.setTextColor(p.textSecondary)
            }
        }
    }

    inner class FooterViewHolder(
        val binding: ItemHourlyFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HourlyListItem.HorizonFooter) {
            binding.tvFooterMessage.text = item.message
            val p = palette
            if (p != null) {
                binding.tvFooterMessage.setTextColor(p.textSecondary)
            }
        }
    }
}
