package com.weatherapp.libre

import com.weatherapp.theme.ColorPalette
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemMetricCellBinding

enum class MetricType { WIND, AIR_QUALITY, UV, VISIBILITY, HUMIDITY, CEILING }

data class MetricCell(
    val type: MetricType,
    val title: String,
    val value: String,
    val secondary: String? = null,
    val iconResId: Int
)

class MetricGridAdapter : RecyclerView.Adapter<MetricGridAdapter.MetricViewHolder>() {

    private val cells = mutableListOf<MetricCell>()
    private var palette: ColorPalette? = null
    var onMetricClickListener: ((MetricType) -> Unit)? = null

    fun submitList(newCells: List<MetricCell>) {
        cells.clear()
        cells.addAll(newCells)
        notifyDataSetChanged()
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        val binding = ItemMetricCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MetricViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        val cell = cells[position]
        val p = palette

        holder.binding.tvMetricTitle.text = cell.title
        holder.binding.tvMetricValue.text = cell.value
        holder.binding.ivMetricIcon.setImageResource(cell.iconResId)

        if (cell.secondary != null) {
            holder.binding.tvMetricSecondary.text = cell.secondary
            holder.binding.tvMetricSecondary.visibility = View.VISIBLE
        } else {
            holder.binding.tvMetricSecondary.visibility = View.GONE
        }

        if (p != null) {
            holder.binding.layoutMetricCell.backgroundTintList = ColorStateList.valueOf(p.surfaceCardSecondary)
            holder.binding.tvMetricTitle.setTextColor(p.textSecondary)
            holder.binding.tvMetricValue.setTextColor(p.textPrimary)
            holder.binding.tvMetricSecondary.setTextColor(p.textSecondary)
            holder.binding.ivMetricIcon.imageTintList = ColorStateList.valueOf(p.brandPrimary)
        }

        holder.binding.root.setOnClickListener {
            onMetricClickListener?.invoke(cell.type)
        }
    }

    override fun getItemCount(): Int = cells.size

    class MetricViewHolder(val binding: ItemMetricCellBinding) : RecyclerView.ViewHolder(binding.root)
}
