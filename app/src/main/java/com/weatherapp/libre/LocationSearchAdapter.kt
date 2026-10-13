package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemLocationSearchBinding

class LocationSearchAdapter(
    private val palette: ColorPalette,
    private val onItemClick: (LocationItem) -> Unit
) : RecyclerView.Adapter<LocationSearchAdapter.ViewHolder>() {

    private val items = mutableListOf<LocationItem>()

    fun submitList(newItems: List<LocationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationSearchBinding.inflate(
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
        private val binding: ItemLocationSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationItem) {
            binding.tvLocationName.text = item.name
            binding.tvLocationSubtext.text = item.regionAndCountryText

            binding.tvLocationName.setTextColor(palette.textPrimary)
            binding.tvLocationSubtext.setTextColor(palette.textSecondary)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
