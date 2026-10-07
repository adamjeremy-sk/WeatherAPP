package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemDarkSkyWindowBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class DarkSkyWindowAdapter : RecyclerView.Adapter<DarkSkyWindowAdapter.ViewHolder>() {

    private val items = mutableListOf<DarkSkyWindow>()
    private var isMultiNight: Boolean = false
    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var palette: ColorPalette? = null

    fun submitList(newItems: List<DarkSkyWindow>, isMultiNight: Boolean, zoneId: ZoneId) {
        items.clear()
        items.addAll(newItems)
        this.isMultiNight = isMultiNight
        this.zoneId = zoneId
        notifyDataSetChanged()
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDarkSkyWindowBinding.inflate(
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
        private val binding: ItemDarkSkyWindowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DarkSkyWindow) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val startLocal = item.startUtc.atZone(zoneId).format(formatter)
            val endLocal = item.endUtc.atZone(zoneId).format(formatter)

            binding.tvWindowTimeRange.text = "$startLocal – $endLocal"
            binding.tvWindowMoonStatus.text = item.moonStatus
            binding.tvWindowQuality.text = item.qualityLabel

            val hours = item.durationMinutes / 60
            val mins = item.durationMinutes % 60
            val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
            binding.tvWindowDuration.text = "$durationText • Index ${item.interferenceIndex}/100"

            if (isMultiNight) {
                val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                binding.tvWindowDateHeader.text = item.startUtc.atZone(zoneId).format(dateFmt).uppercase(Locale.getDefault())
                binding.tvWindowDateHeader.visibility = View.VISIBLE
            } else {
                binding.tvWindowDateHeader.visibility = View.GONE
            }

            val p = palette
            if (p != null) {
                binding.root.setCardBackgroundColor(p.surfaceCard)
                binding.root.strokeColor = p.dividerColor

                binding.tvWindowTimeRange.setTextColor(p.textPrimary)
                binding.tvWindowMoonStatus.setTextColor(p.textSecondary)
                binding.tvWindowDuration.setTextColor(p.textSecondary)
                binding.tvWindowDateHeader.setTextColor(p.brandPrimary)
                binding.ivWindowIcon.setColorFilter(p.brandPrimary)

                val qualityColor = when (item.qualityLabel) {
                    "Excellent" -> p.brandPrimary
                    "Good" -> p.brandPrimary
                    "Moderate" -> p.alertModerate
                    else -> p.alertSevere
                }
                binding.tvWindowQuality.setTextColor(qualityColor)
            }
        }
    }
}
