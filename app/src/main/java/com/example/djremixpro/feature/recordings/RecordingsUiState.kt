package com.example.djremixpro.feature.recordings

import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.UiText

/** [meta] resolves to "03:12 · 25/09/2026" or "03:12 · 25/09/2026 · Đang phát". */
data class RecordingRow(val id: String, val name: String, val meta: UiText, val waveSeed: Int, val isPlaying: Boolean)

/** [meta] is "03:12 · 25/09/2026". */
data class RecordingSheetUi(val id: String, val title: String, val meta: String)

data class RenameDialogUi(val id: String, val currentName: String)

data class DeleteConfirmUi(val id: String, val name: String)

data class RecordingsUiState(
    val items: List<RecordingRow> = emptyList(),
    val isEmpty: Boolean = false,
    val sheet: RecordingSheetUi? = null,
    val renameDialog: RenameDialogUi? = null,
    val deleteConfirm: DeleteConfirmUi? = null,
)

sealed interface RecordingsEvent {
    data class ShowToast(val message: ToastMessage) : RecordingsEvent
}
