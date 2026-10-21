package com.weatherapp.libre

import com.weatherapp.theme.ColorPalette
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.weatherapp.libre.databinding.ItemMenuOrganizationRowBinding

class MenuOrganizationAdapter(
    private val context: Context,
    private val items: MutableList<String>,
    private val enabledFlags: MutableMap<InlineSubTab, Boolean>,
    private var defaultSubTab: InlineSubTab,
    private val palette: ColorPalette,
    private var showSevenDayInMenu: Boolean,
    private val onItemReordered: () -> Unit,
    private val onDefaultChanged: (InlineSubTab) -> Unit,
    private val onToggleDisabledActiveTab: () -> Unit
) : RecyclerView.Adapter<MenuOrganizationAdapter.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_TAB = 0
        const val VIEW_TYPE_SEPARATOR = 1
    }

    fun getItemList(): List<String> = items

    fun updateItems(newItems: List<String>) {
        val copy = newItems.toList()
        items.clear()
        items.addAll(copy)
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                java.util.Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                java.util.Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        onItemReordered()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] == "SEPARATOR") VIEW_TYPE_SEPARATOR else VIEW_TYPE_TAB
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuOrganizationRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], getItemViewType(position))
    }

    override fun getItemCount(): Int = items.size

    fun getItemLabel(subTabKey: String): String {
        return if (subTabKey == "SEPARATOR") {
            context.getString(R.string.label_separator)
        } else {
            try {
                when (InlineSubTab.valueOf(subTabKey)) {
                    InlineSubTab.CURRENT -> context.getString(R.string.subtab_current_weather)
                    InlineSubTab.HOURLY -> context.getString(R.string.subtab_hourly_dropdown)
                    InlineSubTab.SEVEN_DAY -> context.getString(R.string.title_seven_day_forecast)
                    InlineSubTab.NATURAL_LIGHT -> context.getString(R.string.subtab_natural_light_dropdown)
                    InlineSubTab.DARK_SKY -> context.getString(R.string.subtab_dark_sky_dropdown)
                    InlineSubTab.NIGHT_VISIBILITY -> context.getString(R.string.subtab_night_vis_dropdown)
                    InlineSubTab.SOLUNAR -> context.getString(R.string.subtab_solunar_dropdown)
                    InlineSubTab.LUNAR_CALENDAR -> context.getString(R.string.title_lunar_calendar)
                }
            } catch (e: Exception) {
                subTabKey
            }
        }
    }

    fun updateSevenDayPlacement(showInMenu: Boolean) {
        this.showSevenDayInMenu = showInMenu
        this.enabledFlags[InlineSubTab.SEVEN_DAY] = showInMenu
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemMenuOrganizationRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(itemKey: String, viewType: Int) {
            binding.cardOrgRow.setCardBackgroundColor(ColorStateList.valueOf(palette.surfaceCard))
            binding.cardOrgRow.strokeColor = palette.dividerColor
            binding.ivDragHandle.setColorFilter(palette.brandPrimary)

            if (viewType == VIEW_TYPE_SEPARATOR) {
                binding.tvOrgItemName.text = context.getString(R.string.label_separator)
                binding.tvOrgItemName.setTextColor(palette.textSecondary)
                binding.tvDefaultBadge.visibility = View.GONE
                binding.switchOrgItemEnabled.visibility = View.GONE
                binding.cardOrgRow.setOnClickListener(null)
                binding.cardOrgRow.setOnLongClickListener(null)
            } else {
                val subTab = try { InlineSubTab.valueOf(itemKey) } catch (e: Exception) { InlineSubTab.CURRENT }
                val isDefault = (subTab == defaultSubTab)
                val label = getItemLabel(itemKey)

                binding.tvOrgItemName.text = label
                binding.tvDefaultBadge.visibility = if (isDefault) View.VISIBLE else View.GONE
                binding.tvDefaultBadge.setTextColor(palette.brandPrimary)
                binding.switchOrgItemEnabled.visibility = View.VISIBLE
                binding.switchOrgItemEnabled.setOnCheckedChangeListener(null)

                val isSevenDayInline = (subTab == InlineSubTab.SEVEN_DAY && !showSevenDayInMenu)

                if (isSevenDayInline) {
                    binding.tvOrgItemName.setTextColor(palette.textSecondary)
                    binding.cardOrgRow.alpha = 0.5f
                    binding.switchOrgItemEnabled.isChecked = false
                    binding.switchOrgItemEnabled.isEnabled = false
                    
                    binding.cardOrgRow.setOnClickListener(null)
                    binding.cardOrgRow.setOnLongClickListener(null)
                } else {
                    binding.tvOrgItemName.setTextColor(palette.textPrimary)
                    binding.cardOrgRow.alpha = 1.0f
                    
                    if (isDefault) {
                        binding.switchOrgItemEnabled.isChecked = true
                        binding.switchOrgItemEnabled.isEnabled = false
                    } else {
                        binding.switchOrgItemEnabled.isEnabled = true
                        binding.switchOrgItemEnabled.isChecked = enabledFlags[subTab] ?: true
                        binding.switchOrgItemEnabled.setOnCheckedChangeListener { _, isChecked ->
                            enabledFlags[subTab] = isChecked
                            onItemReordered()
                            if (isChecked == false) {
                                onToggleDisabledActiveTab()
                            }
                        }
                    }

                    binding.cardOrgRow.setOnClickListener {
                        if (isDefault) {
                            Toast.makeText(context, R.string.toast_cannot_disable_default, Toast.LENGTH_SHORT).show()
                        } else {
                            binding.switchOrgItemEnabled.toggle()
                        }
                    }

                    binding.cardOrgRow.setOnLongClickListener {
                        if (!isDefault) {
                            defaultSubTab = subTab
                            enabledFlags[subTab] = true
                            notifyDataSetChanged()
                            onDefaultChanged(subTab)
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_default_set, label),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }
                }
            }
        }
    }
}
