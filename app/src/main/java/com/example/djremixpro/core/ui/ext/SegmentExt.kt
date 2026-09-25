package com.example.djremixpro.core.ui.ext

import android.widget.TextView
import com.example.djremixpro.R

/**
 * Segmented control made of TextViews with sel_segment / sel_segment_text: the selected item is raised,
 * text colour and semibold (600), the others muted medium (500) (App:148, App:196).
 */
fun List<TextView>.selectSegment(selectedIndex: Int) {
    forEachIndexed { i, view ->
        val selected = i == selectedIndex
        view.isSelected = selected
        view.typeface = view.context.fontOf(if (selected) R.font.be_vietnam_pro_semibold else R.font.be_vietnam_pro_medium)
    }
}
