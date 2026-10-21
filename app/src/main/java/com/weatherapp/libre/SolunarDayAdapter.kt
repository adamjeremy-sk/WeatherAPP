package com.weatherapp.libre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemSolunarDayBinding
import com.weatherapp.libre.lunar.getEmoji
import com.weatherapp.libre.lunar.getNameRes
import com.weatherapp.theme.ColorPalette
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SolunarDayAdapter : RecyclerView.Adapter<SolunarDayAdapter.ViewHolder>() {

    private val items = mutableListOf<SolunarDaySummary>()
    private var zoneId: ZoneId = ZoneId.systemDefault()
    private var palette: ColorPalette? = null
    private var expandedPosition: Int? = null

    fun submitList(newItems: List<SolunarDaySummary>, zoneId: ZoneId) {
        items.clear()
        items.addAll(newItems)
        this.zoneId = zoneId
        expandedPosition = null
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
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemSolunarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SolunarDaySummary, position: Int) {
            val isExpanded = position == expandedPosition

            val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
            val dateStr = item.date.atStartOfDay(zoneId).format(dateFmt).uppercase(Locale.getDefault())

            val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

            val context = binding.root.context
            val illumPct = (item.illumination * 100).toInt()
            val phaseNameLocalized = item.phaseCategory?.let { cat ->
                "${context.getString(cat.getNameRes())} ${cat.getEmoji()}"
            } ?: item.phaseName
            binding.tvSolunarDate.text = dateStr
            binding.tvSolunarPhase.text = phaseNameLocalized
            binding.tvSolunarIllumination.text = context.getString(R.string.label_percent_illuminated, illumPct)
            binding.tvSolunarRatingLabel.text = "${item.rating} • ${item.getLocalizedRatingLabel(context)}"

            if (isExpanded) {
                binding.layoutExpandedDetails.visibility = View.VISIBLE

                val riseStr = item.moonrise?.atZone(zoneId)?.format(timeFmt) ?: context.getString(R.string.position_no_rise)
                val setStr = item.moonset?.atZone(zoneId)?.format(timeFmt) ?: context.getString(R.string.position_no_set)
                binding.tvSolunarMoonRiseSet.text = context.getString(R.string.solunar_moonrise_moonset, riseStr, setStr)

                val transitStr = item.transit?.atZone(zoneId)?.format(timeFmt) ?: context.getString(R.string.position_unavailable)
                val nadirStr = item.nadir?.atZone(zoneId)?.format(timeFmt) ?: context.getString(R.string.position_unavailable)
                binding.tvSolunarTransitNadir.text = context.getString(R.string.solunar_transit_nadir, transitStr, nadirStr)

                // Format Major Periods
                val majorListStr = if (item.majorPeriods.isNotEmpty()) {
                    item.majorPeriods.joinToString(", ") { p ->
                        val s = p.startUtc.atZone(zoneId).format(timeFmt)
                        val e = p.endUtc.atZone(zoneId).format(timeFmt)
                        "$s – $e (${p.getLocalizedCenterEvent(context)})"
                    }
                } else "None"
                binding.tvSolunarMajorPeriods.text = context.getString(R.string.solunar_major_periods, majorListStr)

                // Format Minor Periods
                val minorListStr = if (item.minorPeriods.isNotEmpty()) {
                    item.minorPeriods.joinToString(", ") { p ->
                        val s = p.startUtc.atZone(zoneId).format(timeFmt)
                        val e = p.endUtc.atZone(zoneId).format(timeFmt)
                        "$s – $e (${p.getLocalizedCenterEvent(context)})"
                    }
                } else "None"
                binding.tvSolunarMinorPeriods.text = context.getString(R.string.solunar_minor_periods, minorListStr)
            } else {
                binding.layoutExpandedDetails.visibility = View.GONE
            }

            val p = palette
            if (p != null) {
                binding.root.setCardBackgroundColor(p.surfaceCard)
                binding.root.strokeColor = p.dividerColor
                binding.viewDivider.setBackgroundColor(p.dividerColor)

                binding.tvSolunarDate.setTextColor(p.brandPrimary)
                binding.tvSolunarPhase.setTextColor(p.textPrimary)
                binding.tvSolunarIllumination.setTextColor(p.textSecondary)
                binding.tvSolunarMoonRiseSet.setTextColor(p.textSecondary)
                binding.tvSolunarTransitNadir.setTextColor(p.textSecondary)
                binding.tvSolunarMajorPeriods.setTextColor(p.textPrimary)
                binding.tvSolunarMinorPeriods.setTextColor(p.textPrimary)

                if (isExpanded) {
                    binding.layoutExpandedDetails.setBackgroundColor(p.surfaceCardSecondary)
                }

                val ratingColor = when (item.ratingLabel) {
                    "Excellent" -> p.brandPrimary
                    "Good" -> p.brandPrimary
                    "Fair" -> p.alertModerate
                    else -> p.alertSevere
                }
                binding.tvSolunarRatingLabel.setTextColor(ratingColor)
            }

            binding.root.setOnClickListener {
                val previousExpanded = expandedPosition
                expandedPosition = if (previousExpanded == position) null else position
                previousExpanded?.let { notifyItemChanged(it) }
                notifyItemChanged(position)
            }
        }
    }
}
