package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemNightVisibilityPeriodBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class NightVisibilityPeriodAdapter : RecyclerView.Adapter<NightVisibilityPeriodAdapter.ViewHolder>() {

    private val items = mutableListOf<VisibilityPeriod>()
    private var isMultiNight: Boolean = false
    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var palette: ColorPalette? = null

    fun submitList(newItems: List<VisibilityPeriod>, isMultiNight: Boolean, zoneId: ZoneId) {
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
        val binding = ItemNightVisibilityPeriodBinding.inflate(
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
        private val binding: ItemNightVisibilityPeriodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VisibilityPeriod) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            val startLocal = item.startUtc.atZone(zoneId).format(formatter)
            val endLocal = item.endUtc.atZone(zoneId).format(formatter)

            binding.tvPeriodCategory.text = item.category
            binding.tvPeriodTimeRange.text = "$startLocal – $endLocal"
            binding.tvPeriodTwilight.text = item.twilightStatus

            if (item.cloudCoverStatus != null) {
                binding.tvPeriodCloudCover.text = item.cloudCoverStatus
                binding.tvPeriodCloudCover.visibility = View.VISIBLE
            } else {
                binding.tvPeriodCloudCover.visibility = View.GONE
            }

            if (isMultiNight) {
                val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                binding.tvPeriodDateHeader.text = item.startUtc.atZone(zoneId).format(dateFmt).uppercase(Locale.getDefault())
                binding.tvPeriodDateHeader.visibility = View.VISIBLE
            } else {
                binding.tvPeriodDateHeader.visibility = View.GONE
            }

            val p = palette
            if (p != null) {
                binding.root.setCardBackgroundColor(p.surfaceCard)
                binding.root.strokeColor = p.dividerColor

                binding.tvPeriodTimeRange.setTextColor(p.textSecondary)
                binding.tvPeriodTwilight.setTextColor(p.brandPrimary)
                binding.tvPeriodCloudCover.setTextColor(p.textSecondary)
                binding.tvPeriodDateHeader.setTextColor(p.brandPrimary)
                binding.ivPeriodIcon.setColorFilter(p.brandPrimary)

                val categoryColor = when (item.category) {
                    "Bright Night" -> p.goldenHourPrimary
                    "Moderate Light" -> p.brandPrimary
                    "Low Light" -> p.textPrimary
                    "Minimal Light" -> p.textSecondary
                    else -> p.textSecondary
                }
                binding.tvPeriodCategory.setTextColor(categoryColor)
            }
        }
    }
}
