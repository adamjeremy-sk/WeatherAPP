package com.weatherapp.libre

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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
    private var allForecasts = listOf<HourlyForecast>()
    private var palette: ColorPalette? = null
    private var locationTimezone: String? = null
    private var expandedInstant: Instant? = null

    fun submitForecastData(
        forecasts: List<HourlyForecast>,
        timezone: String?
    ) {
        allForecasts = forecasts
        locationTimezone = timezone

        val zoneId = try {
            if (!timezone.isNullOrEmpty()) ZoneId.of(timezone) else ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val newItems = HourlyMapper.build36HourList(
            forecasts = allForecasts,
            now = Instant.now(),
            zoneId = zoneId
        )

        val oldItems = ArrayList(items)
        items.clear()
        items.addAll(newItems)

        if (oldItems.isEmpty()) {
            notifyDataSetChanged()
        } else {
            dispatchDiff(oldItems, newItems)
        }
    }

    private fun dispatchDiff(oldItems: List<HourlyListItem>, newItems: List<HourlyListItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldItems[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return when {
                    oldItem is HourlyListItem.DayDivider && newItem is HourlyListItem.DayDivider -> oldItem.dateLabel == newItem.dateLabel
                    oldItem is HourlyListItem.HourRow && newItem is HourlyListItem.HourRow -> oldItem.forecast.time == newItem.forecast.time
                    oldItem is HourlyListItem.HorizonFooter && newItem is HourlyListItem.HorizonFooter -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldItems[oldItemPosition]
                val newItem = newItems[newItemPosition]
                return oldItem == newItem
            }
        })
        diffResult.dispatchUpdatesTo(this)
    }

    fun setLocationTimezone(timezone: String?) {
        locationTimezone = timezone
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

            val timeText = if (item.isCurrentHour) {
                "Now"
            } else {
                val pattern = if (is24Hour) "HH:mm" else "h:mm a"
                forecast.time.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
            binding.tvHourlyTime.text = timeText

            val code = forecast.weatherCode ?: 0
            val iconRes = WeatherCodeUtils.conditionIconRes(code)
            binding.ivHourlyConditionIcon.setImageResource(iconRes)
            val conditionText = WeatherCodeUtils.getWeatherDescription(code)
            binding.tvHourlyConditionText.text = conditionText

            val tempText = forecast.temperature?.let { UnitUtils.formatTemp(it, unitSystem) } ?: "——"
            binding.tvHourlyTemp.text = tempText

            val precipProb = forecast.precipitationProbability
            if (precipProb != null && precipProb > 0) {
                binding.layoutPrecip.visibility = View.VISIBLE
                binding.tvPrecipProb.text = "$precipProb%"
            } else {
                binding.layoutPrecip.visibility = View.GONE
            }

            val isExpanded = (expandedInstant == forecast.time)
            ExpandCollapseHelper.togglePanel(binding.layoutExpandedPanel, isExpanded, binding.cardHourlyRow)

            bindStatGrid(forecast)

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
                binding.tvHourlyConditionText.setTextColor(p.textSecondary)
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

            val precipSummary = if (precipProb != null && precipProb > 0) ", precipitation chance $precipProb percent" else ""
            ExpandCollapseHelper.setupExpandableAccessibility(
                binding.cardHourlyRow,
                "$timeText, $conditionText, $tempText$precipSummary",
                isExpanded
            )

            binding.cardHourlyRow.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val newExpanded = if (expandedInstant == forecast.time) null else forecast.time
                    expandedInstant = newExpanded

                    ExpandCollapseHelper.togglePanel(binding.layoutExpandedPanel, newExpanded == forecast.time, binding.cardHourlyRow)
                    ExpandCollapseHelper.setupExpandableAccessibility(
                        binding.cardHourlyRow,
                        "$timeText, $conditionText, $tempText$precipSummary",
                        newExpanded == forecast.time
                    )
                    notifyItemChanged(pos)
                }
            }
        }

        private fun bindStatGrid(forecast: HourlyForecast) {
            val feelsLike = forecast.apparentTemperature
            if (feelsLike != null) {
                binding.boxFeelsLike.visibility = View.VISIBLE
                binding.tvStatFeelsLike.text = UnitUtils.formatTemp(feelsLike, unitSystem)
            } else {
                binding.boxFeelsLike.visibility = View.GONE
            }

            val windSpeed = forecast.windSpeed
            if (windSpeed != null) {
                binding.boxWind.visibility = View.VISIBLE
                val baseWind = UnitUtils.formatWindSpeed(windSpeed, unitSystem, directionDeg = forecast.windDirection)
                val gustVal = UnitUtils.formatWindGusts(windSpeed, forecast.windGusts, unitSystem)
                if (gustVal != null) {
                    val gustInline = binding.root.context.getString(R.string.gusts_inline_format, gustVal)
                    binding.tvStatWind.text = "$baseWind $gustInline"
                } else {
                    binding.tvStatWind.text = baseWind
                }
            } else {
                binding.boxWind.visibility = View.GONE
            }

            val hum = forecast.humidity
            if (hum != null) {
                binding.boxHumidity.visibility = View.VISIBLE
                binding.tvStatHumidity.text = "$hum%"
            } else {
                binding.boxHumidity.visibility = View.GONE
            }

            val dew = forecast.dewPoint
            if (dew != null) {
                binding.boxDewPoint.visibility = View.VISIBLE
                binding.tvStatDewPoint.text = UnitUtils.formatTemp(dew, unitSystem)
            } else {
                binding.boxDewPoint.visibility = View.GONE
            }

            val precip = forecast.precipitation
            if (precip != null) {
                binding.boxPrecipAmount.visibility = View.VISIBLE
                binding.tvStatPrecipAmount.text = UnitUtils.formatPrecipitation(precip, unitSystem)
            } else {
                binding.boxPrecipAmount.visibility = View.GONE
            }

            val cloud = forecast.cloudCover
            if (cloud != null) {
                binding.boxCloudCover.visibility = View.VISIBLE
                binding.tvStatCloudCover.text = "$cloud%"
            } else {
                binding.boxCloudCover.visibility = View.GONE
            }

            val vis = forecast.visibility
            if (vis != null) {
                binding.boxVisibility.visibility = View.VISIBLE
                binding.tvStatVisibility.text = UnitUtils.formatVisibility(vis, unitSystem)
            } else {
                binding.boxVisibility.visibility = View.GONE
            }

            val uv = forecast.uvIndex
            if (uv != null && uv > 0.0) {
                binding.boxUvIndex.visibility = View.VISIBLE
                binding.tvStatUvIndex.text = String.format(Locale.getDefault(), "%.1f", uv)
            } else {
                binding.boxUvIndex.visibility = View.GONE
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
