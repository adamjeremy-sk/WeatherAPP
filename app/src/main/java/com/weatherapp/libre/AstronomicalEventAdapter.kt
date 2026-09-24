package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemAstronomicalEventBinding

class AstronomicalEventAdapter(
    private val onLongClickAddCalendar: ((AstronomicalEventItem) -> Unit)? = null
) : RecyclerView.Adapter<AstronomicalEventAdapter.ViewHolder>() {

    private val items = mutableListOf<AstronomicalEventItem>()
    private var palette: ColorPalette? = null

    fun submitList(newList: List<AstronomicalEventItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAstronomicalEventBinding.inflate(
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
        private val binding: ItemAstronomicalEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AstronomicalEventItem) {
            binding.tvEventTitle.text = item.title
            binding.tvEventDate.text = item.dateText
            binding.tvEventCategory.text = item.category
            binding.tvEventShortDesc.text = item.shortDescription
            binding.tvEventDetailDescription.text = item.detailedDescription

            val p = palette
            if (p != null) {
                binding.cardEvent.setCardBackgroundColor(p.surfaceCard)
                binding.tvEventTitle.setTextColor(p.textPrimary)
                binding.tvEventDate.setTextColor(p.textSecondary)
                binding.tvEventCategory.setTextColor(p.primary)
                binding.tvEventShortDesc.setTextColor(p.textSecondary)
                binding.tvEventDetailDescription.setTextColor(p.textSecondary)
            }

            // Expandable click behavior
            binding.tvEventDetailDescription.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

            binding.cardEvent.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    item.isExpanded = !item.isExpanded
                    notifyItemChanged(pos)
                }
            }

            binding.cardEvent.setOnLongClickListener {
                onLongClickAddCalendar?.invoke(item)
                true
            }

            // Highlight today's events with a Gold border
            if (item.isHappeningToday) {
                val goldColor = ContextCompat.getColor(binding.root.context, R.color.golden_hour_primary)
                binding.cardEvent.strokeColor = goldColor
                binding.cardEvent.strokeWidth = (2 * binding.root.resources.displayMetrics.density).toInt()
            } else {
                val strokeC = p?.dividerColor ?: ContextCompat.getColor(binding.root.context, R.color.divider_color)
                binding.cardEvent.strokeColor = strokeC
                binding.cardEvent.strokeWidth = (1 * binding.root.resources.displayMetrics.density).toInt()
            }
        }
    }
}
