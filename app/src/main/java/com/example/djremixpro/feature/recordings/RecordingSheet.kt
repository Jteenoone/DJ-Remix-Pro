package com.example.djremixpro.feature.recordings

import android.content.Context
import com.example.djremixpro.core.ui.widget.StateBottomSheet
import com.example.djremixpro.databinding.SheetRecordingBinding

/** O2 recording sheet (App:331-337, D-13): rename, share, delete. Shown while RecordingsUiState.sheet != null. */
class RecordingSheet(
    context: Context,
    private val onRename: () -> Unit,
    private val onShare: () -> Unit,
    private val onDelete: () -> Unit,
    onDismissed: () -> Unit,
) {
    private val sheet = StateBottomSheet(context, { SheetRecordingBinding.inflate(it) }, onDismissed)

    fun render(ui: RecordingSheetUi?) {
        if (ui == null) {
            sheet.hide()
            return
        }
        sheet.render { b ->
            b.textTitle.text = ui.title
            b.textMeta.text = ui.meta
            b.actionRename.setOnClickListener { onRename() }
            b.actionShare.setOnClickListener { onShare() }
            b.actionDelete.setOnClickListener { onDelete() }
        }
    }

    fun hide() = sheet.hide()
}
