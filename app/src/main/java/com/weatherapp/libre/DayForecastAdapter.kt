package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemDayForecastBinding

class DayForecastAdapter(
    private var unitSystem: UnitSystem = UnitSystem.METRIC
) : RecyclerView.Adapter<DayForecastAdapter.ViewHolder>() {

    private val items = mutableListOf<DayForecast>()
    private var palette: ColorPalette? = null

    fun submitList(newList: List<DayForecast>) {
        items.clear()
        items.addAll(newList)
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
            binding.tvExpandedMoonPhase.text = "--"
            binding.tvExpandedDaylight.text = solar.daylightDurationText

            val sunriseStr = SolarCalculator.formatTime(solar.sunrise)
            val sunsetStr = SolarCalculator.formatTime(solar.sunset)
            binding.tvExpandedSunriseSunset.text = "$sunriseStr / $sunsetStr"

            val windStr = UnitUtils.formatWindSpeed(item.windSpeedMaxKmH, unitSystem, gustKmh = item.windGustsMaxKmH)
            binding.tvExpandedWindUv.text = "Wind: $windStr"

            binding.tvExpandedSunAzimuth.text = "Rise ${solar.sunriseAzimuthDegrees}° (${solar.sunriseCompassDirection}) • Set ${solar.sunsetAzimuthDegrees}° (${solar.sunsetCompassDirection})"
            binding.tvExpandedMoonAzimuth.text = "--"

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
