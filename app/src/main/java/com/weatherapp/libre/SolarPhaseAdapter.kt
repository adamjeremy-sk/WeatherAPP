package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemSolarPhaseBinding

class SolarPhaseAdapter(
    private val onItemLongClick: ((SolarPhaseDetail) -> Unit)? = null
) : RecyclerView.Adapter<SolarPhaseAdapter.ViewHolder>() {

    private val items = mutableListOf<SolarPhaseDetail>()
    private var palette: ColorPalette? = null

    fun submitList(newItems: List<SolarPhaseDetail>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSolarPhaseBinding.inflate(
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
        private val binding: ItemSolarPhaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SolarPhaseDetail) {
            binding.tvPhaseName.text = item.getDisplayName(binding.root.context)
            binding.tvPhaseTimeRange.text = item.timeRangeText
            binding.tvPhaseDuration.text = item.durationText

            val isEvent = item.name.equals("Sunrise", ignoreCase = true) || item.name.equals("Sunset", ignoreCase = true)
            val isHighlighted = item.isCurrentActivePhase

            val goldColor = ContextCompat.getColor(binding.root.context, R.color.golden_hour_primary)

            val p = palette
            if (p != null) {
                binding.cardPhase.setCardBackgroundColor(
                    when {
                        isHighlighted -> p.surfaceCardSecondary
                        isEvent -> p.surfaceCardSecondary
                        else -> p.surfaceCard
                    }
                )
                binding.tvPhaseName.setTextColor(if (isHighlighted) goldColor else p.textPrimary)
                binding.tvPhaseTimeRange.setTextColor(p.textPrimary)
                binding.tvPhaseDuration.setTextColor(if (isHighlighted) goldColor else p.primary)
                binding.cardPhase.strokeColor = if (isHighlighted) goldColor else p.dividerColor
            } else {
                binding.cardPhase.strokeColor = if (isHighlighted) goldColor else ContextCompat.getColor(binding.root.context, R.color.divider_color)
            }

            val strokeWidthDp = if (isHighlighted) 2.5f else 1.0f
            binding.cardPhase.strokeWidth = (strokeWidthDp * binding.root.resources.displayMetrics.density).toInt()

            binding.cardPhase.setOnLongClickListener {
                onItemLongClick?.invoke(item)
                true
            }
        }
    }
}
