package com.weatherapp.libre

import com.weatherapp.theme.ColorPalette
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
            val context = binding.root.context

            binding.tvPeriodCategory.text = item.getLocalizedCategory(context)
            binding.tvPeriodTimeRange.text = "$startLocal – $endLocal"
            binding.tvPeriodTwilight.text = item.getLocalizedTwilightStatus(context)

            val cloudStatus = item.getLocalizedCloudCoverStatus(context)
            if (cloudStatus != null) {
                binding.tvPeriodCloudCover.text = cloudStatus
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

            val now = java.time.Instant.now()
            val isCurrentPeriod = now.isAfter(item.startUtc) && now.isBefore(item.endUtc)
            val goldColor = ContextCompat.getColor(context, R.color.golden_hour_primary)

            val p = palette
            if (p != null) {
                binding.root.setCardBackgroundColor(if (isCurrentPeriod) p.surfaceCardSecondary else p.surfaceCard)
                binding.root.strokeColor = if (isCurrentPeriod) goldColor else p.dividerColor

                binding.tvPeriodTimeRange.setTextColor(if (isCurrentPeriod) goldColor else p.textSecondary)
                binding.tvPeriodTwilight.setTextColor(if (isCurrentPeriod) goldColor else p.brandPrimary)
                binding.tvPeriodCloudCover.setTextColor(p.textSecondary)
                binding.tvPeriodDateHeader.setTextColor(p.brandPrimary)
                binding.ivPeriodIcon.setColorFilter(p.brandPrimary)

                val categoryColor = if (isCurrentPeriod) {
                    goldColor
                } else {
                    when (item.category) {
                        "Bright Night" -> p.goldenHourPrimary
                        "Moderate Light" -> p.brandPrimary
                        "Low Light" -> p.textPrimary
                        "Minimal Light" -> p.textSecondary
                        else -> p.textSecondary
                    }
                }
                binding.tvPeriodCategory.setTextColor(categoryColor)
            } else {
                binding.root.strokeColor = if (isCurrentPeriod) goldColor else ContextCompat.getColor(context, R.color.divider_color)
            }

            val strokeWidthDp = if (isCurrentPeriod) 2.5f else 1.0f
            binding.root.strokeWidth = (strokeWidthDp * binding.root.resources.displayMetrics.density).toInt()
        }
    }
}
