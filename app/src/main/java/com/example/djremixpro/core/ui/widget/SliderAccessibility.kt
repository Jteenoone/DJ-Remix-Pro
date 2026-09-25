package com.example.djremixpro.core.ui.widget

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/** Exposes a 0..1 control as a SeekBar to TalkBack (RangeInfo + scroll / set-progress actions). */
@Suppress("DEPRECATION")
internal fun AccessibilityNodeInfo.applySlider(value: Float, enabled: Boolean) {
    className = "android.widget.SeekBar"
    rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(
        AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT, 0f, 1f, value,
    )
    if (enabled) {
        if (value < 1f) addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        if (value > 0f) addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS)
    }
}

/**
 * Resolves a slider accessibility action to the new value, or null when the action is not a slider action.
 * [step] is the increment for scroll forward/backward.
 */
internal fun sliderActionValue(action: Int, arguments: Bundle?, current: Float, step: Float = 0.05f): Float? =
    when (action) {
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> (current + step).coerceIn(0f, 1f)
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> (current - step).coerceIn(0f, 1f)
        android.R.id.accessibilityActionSetProgress ->
            arguments?.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)?.coerceIn(0f, 1f)
        else -> null
    }
