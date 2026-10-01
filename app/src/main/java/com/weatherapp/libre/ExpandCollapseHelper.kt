package com.weatherapp.libre

import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager

object ExpandCollapseHelper {

    const val EXPAND_COLLAPSE_DURATION_MS = 220L
    val FAST_OUT_SLOW_IN_INTERPOLATOR = PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)

    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0f
        } catch (e: Exception) {
            false
        }
    }

    fun togglePanel(panelView: View, isExpanded: Boolean, parentViewGroup: ViewGroup? = null) {
        val context = panelView.context
        if (isReducedMotionEnabled(context)) {
            panelView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        } else {
            if (parentViewGroup != null) {
                val transition = AutoTransition().apply {
                    duration = EXPAND_COLLAPSE_DURATION_MS
                    interpolator = FAST_OUT_SLOW_IN_INTERPOLATOR
                }
                TransitionManager.beginDelayedTransition(parentViewGroup, transition)
            }
            panelView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }

    fun setupExpandableAccessibility(view: View, label: String, isExpanded: Boolean) {
        val stateAction = if (isExpanded) "expanded, double tap to collapse" else "collapsed, double tap to expand"
        view.contentDescription = "$label, $stateAction"
    }
}
