package com.weatherapp.libre.almanac

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.weatherapp.libre.BoutiqueThemeEngine
import com.weatherapp.libre.R
import com.weatherapp.libre.lunar.EclipseType
import com.weatherapp.libre.lunar.LunarCalculator
import com.weatherapp.libre.lunar.LunarPhaseCategory
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.asin
import kotlin.math.roundToInt

class LunarDayAdapter(
    private var days: List<MoonAlmanacDay> = emptyList(),
    private var themeMode: Int = 1,
    private val onDayClick: ((MoonAlmanacDay) -> Unit)? = null,
) : RecyclerView.Adapter<LunarDayAdapter.DayViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    fun submitData(newDays: List<MoonAlmanacDay>, newThemeMode: Int) {
        days = newDays
        themeMode = newThemeMode
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    fun refreshTheme(newThemeMode: Int) {
        themeMode = newThemeMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lunar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val isExpanded = expandedPositions.contains(position)
        val isSpecialEventsList = days.size > 7
        holder.bind(day, position, isExpanded, themeMode, isSpecialEventsList) { pos ->
            if (expandedPositions.contains(pos)) {
                expandedPositions.remove(pos)
            } else {
                expandedPositions.add(pos)
            }
            notifyItemChanged(pos)
            onDayClick?.invoke(day)
        }
    }

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardLunarDay: MaterialCardView = itemView.findViewById(R.id.cardLunarDay)
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val tvDayDate: TextView = itemView.findViewById(R.id.tvDayDate)

        private val tvBadgePenumbral: TextView = itemView.findViewById(R.id.tvBadgePenumbral)
        private val tvBadgePartial: TextView = itemView.findViewById(R.id.tvBadgePartial)
        private val tvBadgeTotal: TextView = itemView.findViewById(R.id.tvBadgeTotal)

        private val tvBadgeBlood: TextView = itemView.findViewById(R.id.tvBadgeBloodMoon)
        private val tvBadgeSuper: TextView = itemView.findViewById(R.id.tvBadgeSupermoon)
        private val tvBadgeBlue: TextView = itemView.findViewById(R.id.tvBadgeBlueMoon)
        private val tvBadgeHarvest: TextView = itemView.findViewById(R.id.tvBadgeHarvestMoon)
        private val tvBadgeMicromoon: TextView = itemView.findViewById(R.id.tvBadgeMicromoon)
        private val tvBadgeSyzygy: TextView = itemView.findViewById(R.id.tvBadgeSyzygy)

        private val ivMoonPhaseIcon: ImageView = itemView.findViewById(R.id.ivMoonPhaseIcon)
        private val tvMoonStageName: TextView = itemView.findViewById(R.id.tvMoonStageName)
        private val tvIlluminationText: TextView = itemView.findViewById(R.id.tvIlluminationText)
        private val tvApparentDiameter: TextView = itemView.findViewById(R.id.tvApparentDiameter)

        // Expanded Details Container
        private val layoutExpandedDetails: LinearLayout = itemView.findViewById(R.id.layoutExpandedDetails)
        private val viewDivider: View = itemView.findViewById(R.id.viewDivider)

        private val tvTimingsHeader: TextView = itemView.findViewById(R.id.tvTimingsHeader)
        private val tvMoonriseSet: TextView = itemView.findViewById(R.id.tvMoonriseSet)
        private val tvTransitInfo: TextView = itemView.findViewById(R.id.tvTransitInfo)

        private val tvGeometryHeader: TextView = itemView.findViewById(R.id.tvGeometryHeader)
        private val tvMoonDistance: TextView = itemView.findViewById(R.id.tvMoonDistance)
        private val tvMoonAgeNextPhase: TextView = itemView.findViewById(R.id.tvMoonAgeNextPhase)

        private val layoutEclipseContainer: LinearLayout = itemView.findViewById(R.id.layoutEclipseContainer)
        private val tvEclipseTitle: TextView = itemView.findViewById(R.id.tvEclipseTitle)
        val tvEclipseDetails: TextView = itemView.findViewById(R.id.tvEclipseDetails)
        private val tvEclipseContacts: TextView = itemView.findViewById(R.id.tvEclipseContacts)

        private val layoutScoreContainer: LinearLayout = itemView.findViewById(R.id.layoutScoreContainer)
        private val tvViewingScoreTitle: TextView = itemView.findViewById(R.id.tvViewingScoreTitle)
        private val progressScore: ProgressBar = itemView.findViewById(R.id.progressScore)
        private val tvViewingScoreDesc: TextView = itemView.findViewById(R.id.tvViewingScoreDesc)

        fun bind(
            day: MoonAlmanacDay,
            position: Int,
            isExpanded: Boolean,
            themeMode: Int,
            isSpecialEventsList: Boolean = false,
            onToggleExpand: (Int) -> Unit
        ) {
            val palette = BoutiqueThemeEngine.getThemePalette(themeMode)
            val density = itemView.context.resources.displayMetrics.density

            val today = LocalDate.now()
            val isToday = day.date.year == today.year && day.date.monthValue == today.monthValue && day.date.dayOfMonth == today.dayOfMonth
            val isSpecialEvent = (day.isBloodMoon == true) ||
                    (day.isSupermoon == true) ||
                    (day.isBlueMoon == true) ||
                    (day.isHarvestMoon == true) ||
                    (day.isMicromoon == true) ||
                    (day.eclipseInfo != null)

            // Background & Stroke Color (Opaque solid colors only, matching forecast/hourly cards)
            if (isToday) {
                cardLunarDay.setCardBackgroundColor(palette.surfaceCardSecondary)
                cardLunarDay.strokeColor = palette.brandPrimary
                cardLunarDay.strokeWidth = (2.5f * density).roundToInt()
                tvDayName.setTextColor(palette.brandPrimary)
                tvDayDate.setTextColor(palette.brandPrimary)
            } else if (isSpecialEvent) {
                cardLunarDay.setCardBackgroundColor(palette.surfaceCardSecondary)
                cardLunarDay.strokeColor = palette.brandPrimary
                cardLunarDay.strokeWidth = (2.5f * density).roundToInt()
                tvDayName.setTextColor(palette.textPrimary)
                tvDayDate.setTextColor(palette.textSecondary)
            } else {
                cardLunarDay.setCardBackgroundColor(palette.surfaceCard)
                cardLunarDay.strokeColor = palette.dividerColor
                cardLunarDay.strokeWidth = (1.0f * density).roundToInt()
                tvDayName.setTextColor(palette.textPrimary)
                tvDayDate.setTextColor(palette.textSecondary)
            }

            // Headers & Text Colors
            tvMoonStageName.setTextColor(palette.textPrimary)
            tvTimingsHeader.setTextColor(palette.textPrimary)
            tvGeometryHeader.setTextColor(palette.textPrimary)
            tvEclipseTitle.setTextColor(palette.textPrimary)
            tvViewingScoreTitle.setTextColor(palette.textPrimary)

            tvIlluminationText.setTextColor(palette.textSecondary)
            tvApparentDiameter.setTextColor(palette.textSecondary)
            tvMoonriseSet.setTextColor(palette.textSecondary)
            tvTransitInfo.setTextColor(palette.textSecondary)
            tvMoonDistance.setTextColor(palette.textSecondary)
            tvMoonAgeNextPhase.setTextColor(palette.textSecondary)
            tvEclipseDetails.setTextColor(palette.textSecondary)
            tvEclipseContacts.setTextColor(palette.textSecondary)
            tvViewingScoreDesc.setTextColor(palette.textSecondary)
            viewDivider.setBackgroundColor(palette.dividerColor)

            // Date & Day Name Column (e.g., "Sun", "Oct 11" vs "Oct 11, 2026" in Special Events view)
            val showYear = isSpecialEventsList || day.date.year != today.year
            val dayNameFmtPattern = "EEE"
            val dayDateFmtPattern = if (showYear) "MMM d, yyyy" else "MMM d"

            val dayNameFmt = DateTimeFormatter.ofPattern(dayNameFmtPattern, Locale.getDefault())
            val dayDateFmt = DateTimeFormatter.ofPattern(dayDateFmtPattern, Locale.getDefault())

            tvDayName.text = day.date.format(dayNameFmt)
            tvDayDate.text = day.date.format(dayDateFmt)

            // Eclipse Badges
            val eclipse = day.eclipseInfo
            if (eclipse != null) {
                tvBadgePenumbral.visibility = if (eclipse.type == EclipseType.PENUMBRAL) View.VISIBLE else View.GONE
                tvBadgePenumbral.setBackgroundColor(palette.alertMinor)

                tvBadgePartial.visibility = if (eclipse.type == EclipseType.PARTIAL) View.VISIBLE else View.GONE
                tvBadgePartial.setBackgroundColor(palette.alertSevere)

                tvBadgeTotal.visibility = if (eclipse.type == EclipseType.TOTAL) View.VISIBLE else View.GONE
                tvBadgeTotal.setBackgroundColor(palette.alertExtreme)
            } else {
                tvBadgePenumbral.visibility = View.GONE
                tvBadgePartial.visibility = View.GONE
                tvBadgeTotal.visibility = View.GONE
            }

            // Special Event Badges
            tvBadgeBlood.visibility = if (day.isBloodMoon) View.VISIBLE else View.GONE
            tvBadgeBlood.setBackgroundColor(palette.alertExtreme)

            tvBadgeSuper.visibility = if (day.isSupermoon) View.VISIBLE else View.GONE
            tvBadgeSuper.setBackgroundColor(palette.brandPrimary)

            tvBadgeBlue.visibility = if (day.isBlueMoon) View.VISIBLE else View.GONE
            tvBadgeBlue.setBackgroundColor(palette.brandPrimary)

            tvBadgeHarvest.visibility = if (day.isHarvestMoon) View.VISIBLE else View.GONE
            tvBadgeHarvest.setBackgroundColor(palette.goldenHourPrimary)

            tvBadgeMicromoon.visibility = if (day.isMicromoon) View.VISIBLE else View.GONE
            tvBadgeMicromoon.setBackgroundColor(palette.brandPrimary)

            tvBadgeSyzygy.visibility = if (day.isSyzygy) View.VISIBLE else View.GONE
            tvBadgeSyzygy.setBackgroundColor(palette.surfaceCardSecondary)
            tvBadgeSyzygy.setTextColor(palette.textPrimary)

            // Phase Icon & Stage Name
            val drawableRes = when (day.primaryPhase.phaseCategory) {
                LunarPhaseCategory.NEW -> R.drawable.ic_moon_new
                LunarPhaseCategory.WAXING_CRESCENT -> R.drawable.ic_moon_waxing_crescent
                LunarPhaseCategory.FIRST_QUARTER -> R.drawable.ic_moon_first_quarter
                LunarPhaseCategory.WAXING_GIBBOUS -> R.drawable.ic_moon_waxing_gibbous
                LunarPhaseCategory.FULL -> R.drawable.ic_moon_full
                LunarPhaseCategory.WANING_GIBBOUS -> R.drawable.ic_moon_waning_gibbous
                LunarPhaseCategory.LAST_QUARTER -> R.drawable.ic_moon_last_quarter
                LunarPhaseCategory.WANING_CRESCENT -> R.drawable.ic_moon_waning_crescent
            }
            ivMoonPhaseIcon.setImageResource(drawableRes)

            val formattedPhase = LunarCalculator.formatPhaseDisplay(day.primaryPhase)
            tvMoonStageName.text = formattedPhase.phaseNameWithEmoji

            // Illumination % and Peak Time
            val localTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            val pct = (day.primaryPhase.illuminationFraction * 100).roundToInt().coerceIn(0, 100)
            if (day.exactPhasePeakTime != null) {
                val peakStr = localTimeFormatter.format(day.exactPhasePeakTime)
                tvIlluminationText.text = "$pct% • ${itemView.context.getString(R.string.label_phase_peak_time, peakStr)}"
            } else {
                tvIlluminationText.text = itemView.context.getString(R.string.illumination_percentage_format, pct)
            }

            // Apparent Diameter with Abbreviation (e.g. "AD: 32.4'")
            val semidiameterArcmin = day.primaryPhase.geocentricDistanceKm.let {
                val semiDeg = Math.toDegrees(asin(1737.4 / it))
                semiDeg * 60.0
            }
            val semiStr = String.format(Locale.getDefault(), "%.1f'", semidiameterArcmin)
            tvApparentDiameter.text = "${itemView.context.getString(R.string.label_ad_short)} $semiStr"

            // Inline Expansion State with Contrasting Background
            if (isExpanded) {
                layoutExpandedDetails.visibility = View.VISIBLE
                layoutExpandedDetails.setBackgroundColor(palette.surfaceCardSecondary)

                // Bind Expanded Timings
                val riseAzStr = day.moonriseAzimuthDeg?.let { " (${it.roundToInt()}°)" } ?: ""
                val setAzStr = day.moonsetAzimuthDeg?.let { " (${it.roundToInt()}°)" } ?: ""

                val riseStr = day.moonrise?.let { localTimeFormatter.format(it) + riseAzStr } ?: itemView.context.getString(R.string.moon_no_rise)
                val setStr = day.moonset?.let { localTimeFormatter.format(it) + setAzStr } ?: itemView.context.getString(R.string.moon_no_set)
                tvMoonriseSet.text = "${itemView.context.getString(R.string.moonrise_label, riseStr)} • ${itemView.context.getString(R.string.moonset_label, setStr)}"

                tvTransitInfo.text = itemView.context.getString(R.string.transit_alt_format, day.transitAltitudeDeg) + " • " + itemView.context.getString(R.string.transit_az_format, day.transitAzimuthDeg)

                // Bind Expanded Geometry
                val distKm = day.distanceKm.roundToInt()
                tvMoonDistance.text = itemView.context.getString(R.string.label_moon_distance_km, distKm)
                tvMoonAgeNextPhase.text = itemView.context.getString(R.string.moon_age_format, day.moonAgeDays) + " • " + itemView.context.getString(R.string.next_phase_countdown_format, day.nextMajorPhaseName, day.daysUntilNextPhase)

                // Eclipse Details
                if (day.eclipseInfo != null) {
                    layoutEclipseContainer.visibility = View.VISIBLE
                    val typeRes = when (day.eclipseInfo.type) {
                        EclipseType.TOTAL -> R.string.eclipse_total
                        EclipseType.PARTIAL -> R.string.eclipse_partial
                        EclipseType.PENUMBRAL -> R.string.eclipse_penumbral
                    }
                    tvEclipseDetails.text = itemView.context.getString(typeRes) + " • " + itemView.context.getString(R.string.label_eclipse_magnitude, day.eclipseInfo.magnitude)

                    val c = day.eclipseInfo.contacts
                    val contactsText = buildString {
                        c.p1?.let { append("P1: ").append(localTimeFormatter.format(it)).append("  ") }
                        c.u1?.let { append("U1: ").append(localTimeFormatter.format(it)).append("  ") }
                        c.u2?.let { append("U2: ").append(localTimeFormatter.format(it)).append("\n") }
                        c.u3?.let { append("U3: ").append(localTimeFormatter.format(it)).append("  ") }
                        c.u4?.let { append("U4: ").append(localTimeFormatter.format(it)).append("  ") }
                        c.p4?.let { append("P4: ").append(localTimeFormatter.format(it)) }
                    }
                    tvEclipseContacts.text = contactsText
                } else {
                    layoutEclipseContainer.visibility = View.GONE
                }

                // Viewing Score Section
                if (day.cloudCoverScore != null) {
                    layoutScoreContainer.visibility = View.VISIBLE
                    val score = day.cloudCoverScore.roundToInt()
                    tvViewingScoreTitle.text = itemView.context.getString(R.string.label_viewing_score_format, score)
                    progressScore.progress = score

                    val descRes = when {
                        score >= 70 -> R.string.score_excellent_desc
                        score >= 40 -> R.string.score_good_desc
                        else -> R.string.score_poor_desc
                    }
                    tvViewingScoreDesc.text = itemView.context.getString(descRes)
                } else {
                    layoutScoreContainer.visibility = View.GONE
                }
            } else {
                layoutExpandedDetails.visibility = View.GONE
            }

            cardLunarDay.setOnClickListener {
                onToggleExpand(position)
            }

            // Long Press Launches Native Calendar Intent
            cardLunarDay.setOnLongClickListener {
                CalendarIntentHelper.launchCalendarEvent(itemView.context, day)
                true
            }
        }
    }
}
