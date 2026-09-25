package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.djremixpro.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog in the design's own style (Mixer:268 "Lưu bản mix", MixDeck:695): panel, radius 20, 1dp line border,
 * no M3 surface tint. [content] carries its own title, body and 44dp action buttons.
 */
fun designDialog(context: Context, content: View): AlertDialog =
    MaterialAlertDialogBuilder(context)
        .setView(content)
        .setBackground(ContextCompat.getDrawable(context, R.drawable.bg_panel_line_r20))
        .create()
