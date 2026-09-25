package com.example.djremixpro.core.ui.ext

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.example.djremixpro.R

/**
 * Announces a custom toggle (Sync, Scratch, FX switch…) as a checkable ToggleButton with its current state,
 * the equivalent of aria-pressed / role=switch in the design.
 */
fun View.bindToggleSemantics(checked: Boolean, className: String = "android.widget.ToggleButton") {
    val previous = getTag(R.id.tag_toggle_checked)
    setTag(R.id.tag_toggle_checked, checked)
    if (previous == null) {
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            @Suppress("DEPRECATION") // setChecked(Boolean) is the only API below 36
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = className
                info.isCheckable = true
                info.isChecked = host.getTag(R.id.tag_toggle_checked) == true
            }
        }
    }
}
