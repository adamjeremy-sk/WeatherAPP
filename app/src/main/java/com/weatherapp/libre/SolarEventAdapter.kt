package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemSolarEventBinding

data class SolarEventDisplayItem(
    val name: String,
    val timeRange: String,
    val duration: String
)

class SolarEventAdapter : RecyclerView.Adapter<SolarEventAdapter.ViewHolder>() {

    private val items = mutableListOf<SolarEventDisplayItem>()

    fun submitList(newItems: List<SolarEventDisplayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSolarEventBinding.inflate(
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
        private val binding: ItemSolarEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SolarEventDisplayItem) {
            binding.tvEventName.text = item.name
            binding.tvEventTimeRange.text = item.timeRange
            binding.tvEventDuration.text = item.duration
        }
    }
}
