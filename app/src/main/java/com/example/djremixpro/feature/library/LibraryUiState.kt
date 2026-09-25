package com.example.djremixpro.feature.library

import androidx.annotation.ColorInt
import com.example.djremixpro.R
import com.example.djremixpro.core.model.LibraryTab
import com.example.djremixpro.core.model.SortMode
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.UiText

/** [subtitle] is "Nghệ sĩ · 03:15"; [bpmLabel] is "124" or "…" (then [hasBpm] = false). */
data class SongRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val initials: String,
    @param:ColorInt val coverColor: Int,
    val bpmLabel: String,
    val hasBpm: Boolean,
)

data class SongSheetUi(
    val trackId: String,
    val title: String,
    val meta: UiText,
    val initials: String,
    @param:ColorInt val coverColor: Int,
)

data class LibraryUiState(
    val query: String = "",
    val tab: LibraryTab = LibraryTab.SONGS,
    val sort: SortMode = SortMode.BPM,
    val sortLabel: UiText = UiText.Res(R.string.library_sort, listOf(UiText.Res(R.string.sort_bpm))),
    val countLabel: UiText = UiText.Plural(R.plurals.library_count, 0),
    val songs: List<SongRow> = emptyList(),
    val sheet: SongSheetUi? = null,
)

sealed interface LibraryEvent {
    data class ShowToast(val message: ToastMessage) : LibraryEvent
}
