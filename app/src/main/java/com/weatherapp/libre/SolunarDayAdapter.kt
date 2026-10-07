package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemSolunarDayBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SolunarDayAdapter : RecyclerView.Adapter<SolunarDayAdapter.ViewHolder>() {

    private val items = mutableListOf<SolunarDaySummary>()
    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var palette: ColorPalette? = null

    fun submitList(newItems: List<SolunarDaySummary>, zoneId: ZoneId) {
        items.clear()
        items.addAll(newItems)
        this.zoneId = zoneId
        notifyDataSetChanged()
    }

    fun setPalette(newPalette: ColorPalette) {
        palette = newPalette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSolunarDayBinding.inflate(
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
        private val binding: ItemSolunarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SolunarDaySummary) {
            val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
            val dateStr = item.date.atStartOfDay(zoneId).format(dateFmt).uppercase(Locale.getDefault())

            val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

            val illumPct = (item.illumination * 100).toInt()
            binding.tvSolunarDate.text = dateStr
            binding.tvSolunarPhase.text = "${item.phaseName} • $illumPct% ill."
            binding.tvSolunarRatingLabel.text = "${item.rating} • ${item.ratingLabel}"

            val riseStr = item.moonrise?.atZone(zoneId)?.format(timeFmt) ?: "No Rise"
            val setStr = item.moonset?.atZone(zoneId)?.format(timeFmt) ?: "No Set"
            binding.tvSolunarMoonRiseSet.text = "Moonrise: $riseStr • Moonset: $setStr"

            val transitStr = item.transit?.atZone(zoneId)?.format(timeFmt) ?: "——"
            val nadirStr = item.nadir?.atZone(zoneId)?.format(timeFmt) ?: "——"
            binding.tvSolunarTransitNadir.text = "Transit: $transitStr • Nadir: $nadirStr"

            // Format Major Periods
            val majorListStr = if (item.majorPeriods.isNotEmpty()) {
                item.majorPeriods.joinToString(", ") { p ->
                    val s = p.startUtc.atZone(zoneId).format(timeFmt)
                    val e = p.endUtc.atZone(zoneId).format(timeFmt)
                    "$s – $e (${p.centerEvent})"
                }
            } else "None"
            binding.tvSolunarMajorPeriods.text = "Major: $majorListStr"

            // Format Minor Periods
            val minorListStr = if (item.minorPeriods.isNotEmpty()) {
                item.minorPeriods.joinToString(", ") { p ->
                    val s = p.startUtc.atZone(zoneId).format(timeFmt)
                    val e = p.endUtc.atZone(zoneId).format(timeFmt)
                    "$s – $e (${p.centerEvent})"
                }
            } else "None"
            binding.tvSolunarMinorPeriods.text = "Minor: $minorListStr"

            val p = palette
            if (p != null) {
                binding.root.setCardBackgroundColor(p.surfaceCard)
                binding.root.strokeColor = p.dividerColor

                binding.tvSolunarDate.setTextColor(p.brandPrimary)
                binding.tvSolunarPhase.setTextColor(p.textPrimary)
                binding.tvSolunarMoonRiseSet.setTextColor(p.textSecondary)
                binding.tvSolunarTransitNadir.setTextColor(p.textSecondary)
                binding.tvSolunarMajorPeriods.setTextColor(p.textPrimary)
                binding.tvSolunarMinorPeriods.setTextColor(p.textPrimary)

                val ratingColor = when (item.ratingLabel) {
                    "Excellent" -> p.brandPrimary
                    "Good" -> p.brandPrimary
                    "Fair" -> p.alertModerate
                    else -> p.alertSevere
                }
                binding.tvSolunarRatingLabel.setTextColor(ratingColor)
            }
        }
    }
}
