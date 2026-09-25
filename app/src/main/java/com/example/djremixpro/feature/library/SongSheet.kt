package com.example.djremixpro.feature.library

import android.content.Context
import android.content.res.ColorStateList
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.ui.widget.StateBottomSheet
import com.example.djremixpro.databinding.SheetSongBinding

/** O1 song sheet (App:244-258): load into Deck A / B, preview. Shown while LibraryUiState.sheet != null. */
class SongSheet(
    context: Context,
    private val onLoad: (DeckId) -> Unit,
    private val onPreview: () -> Unit,
    onDismissed: () -> Unit,
) {
    private val sheet = StateBottomSheet(context, { SheetSongBinding.inflate(it) }, onDismissed)

    fun render(ui: SongSheetUi?) {
        if (ui == null) {
            sheet.hide()
            return
        }
        sheet.render { b ->
            b.textCover.text = ui.initials
            b.textCover.backgroundTintList = ColorStateList.valueOf(ui.coverColor)
            b.textTitle.text = ui.title
            b.textMeta.text = ui.meta.resolve(b.root.context)
            b.actionLoadA.setOnClickListener { onLoad(DeckId.A) }
            b.actionLoadB.setOnClickListener { onLoad(DeckId.B) }
            b.actionPreview.setOnClickListener { onPreview() }
        }
    }

    fun hide() = sheet.hide()
}
