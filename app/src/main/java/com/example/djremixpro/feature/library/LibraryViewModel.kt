package com.example.djremixpro.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.LibraryTab
import com.example.djremixpro.core.model.SortMode
import com.example.djremixpro.core.model.Track
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.BpmFormat
import com.example.djremixpro.core.util.TextUtils2
import com.example.djremixpro.core.util.TimeFormat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.Collator
import java.util.Locale

/**
 * Library tab (App.dc.html:99-120, 308-313, 324-330). Search filters title/artist ignoring case and Vietnamese
 * accents; chips only change selection (D-17).
 */
class LibraryViewModel(
    private val trackRepository: TrackRepository,
    private val mixSessionRepository: MixSessionRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private data class Local(
        val query: String = "",
        val tab: LibraryTab = LibraryTab.SONGS,
        val sort: SortMode = SortMode.BPM,
        val sheetTrackId: String? = null,
    )

    private val local = MutableStateFlow(Local())
    private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("vi"))

    val uiState: StateFlow<LibraryUiState> = combine(trackRepository.tracks, local, ::buildState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildState(trackRepository.tracks.value, local.value))

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    fun onQueryChange(q: String) = local.update { it.copy(query = q) }

    fun onTabSelected(tab: LibraryTab) = local.update { it.copy(tab = tab) }

    fun onCycleSort() = local.update { it.copy(sort = SORT_CYCLE[(SORT_CYCLE.indexOf(it.sort) + 1) % SORT_CYCLE.size]) }

    fun onSongClicked(id: String) {
        if (trackRepository.findById(id) == null) return
        local.update { it.copy(sheetTrackId = id) }
    }

    fun onSheetDismissed() = local.update { it.copy(sheetTrackId = null) }

    fun onLoadToDeck(deck: DeckId) {
        val track = sheetTrack() ?: return
        mixSessionRepository.loadTrack(deck, track)
        local.update { it.copy(sheetTrackId = null) }
        val (letter, tone) = when (deck) {
            DeckId.A -> "A" to ToastTone.DECK_A
            DeckId.B -> "B" to ToastTone.DECK_B
        }
        _events.trySend(
            LibraryEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_loaded_deck, listOf(track.title, letter)), tone)),
        )
    }

    fun onPreview() {
        val track = sheetTrack() ?: return
        local.update { it.copy(sheetTrackId = null) }
        // "Nghe thử" always ends with the song playing (App.dc.html:329), so an already playing song is left alone.
        if (playbackRepository.nowPlaying.value?.id != track.id) {
            playbackRepository.toggle(track.id, track.title, track.durationSec)
        }
    }

    private fun sheetTrack(): Track? = local.value.sheetTrackId?.let(trackRepository::findById)

    private fun buildState(tracks: List<Track>, l: Local): LibraryUiState {
        val needle = TextUtils2.normalizeForSearch(l.query.trim())
        val filtered = if (needle.isEmpty()) tracks else tracks.filter {
            TextUtils2.normalizeForSearch(it.title).contains(needle) ||
                TextUtils2.normalizeForSearch(it.artist).contains(needle)
        }
        val sorted = sort(filtered, l.sort)
        return LibraryUiState(
            query = l.query,
            tab = l.tab,
            sort = l.sort,
            sortLabel = UiText.Res(R.string.library_sort, listOf(UiText.Res(sortLabelRes(l.sort)))),
            countLabel = UiText.Plural(R.plurals.library_count, sorted.size, listOf(sorted.size)),
            songs = sorted.map { it.toRow() },
            sheet = l.sheetTrackId?.let { id -> tracks.firstOrNull { it.id == id } ?: trackRepository.findById(id) }
                ?.toSheet(),
        )
    }

    private fun sort(tracks: List<Track>, mode: SortMode): List<Track> = when (mode) {
        SortMode.TITLE -> tracks.sortedWith { x, y -> collator.compare(x.title, y.title) }
        SortMode.ARTIST -> tracks.sortedWith { x, y -> collator.compare(x.artist, y.artist) }
        // Songs without a BPM go last (App.dc.html:311, `b ?? 999`).
        SortMode.BPM -> tracks.sortedBy { it.bpm ?: Float.MAX_VALUE }
        SortMode.RECENT -> tracks.sortedBy { it.addedIndex }
    }

    private fun sortLabelRes(mode: SortMode): Int = when (mode) {
        SortMode.TITLE -> R.string.sort_title
        SortMode.ARTIST -> R.string.sort_artist
        SortMode.BPM -> R.string.sort_bpm
        SortMode.RECENT -> R.string.sort_recent
    }

    private fun Track.toRow() = SongRow(
        id = id,
        title = title,
        subtitle = "$artist · ${TimeFormat.mmss(durationSec)}",
        initials = TextUtils2.initials(title),
        coverColor = coverColor,
        bpmLabel = BpmFormat.short(bpm),
        hasBpm = bpm != null,
    )

    private fun Track.toSheet() = SongSheetUi(
        trackId = id,
        title = title,
        meta = UiText.Res(
            R.string.song_sheet_meta,
            listOf(
                artist,
                TimeFormat.mmss(durationSec),
                if (bpm != null) UiText.Res(R.string.song_bpm_value, listOf(BpmFormat.short(bpm)))
                else UiText.Res(R.string.song_analyzing),
            ),
        ),
        initials = TextUtils2.initials(title),
        coverColor = coverColor,
    )

    companion object {
        /** "Sắp xếp" cycle: Tên → Nghệ sĩ → BPM → Mới thêm (App.dc.html:308, 349). */
        val SORT_CYCLE = listOf(SortMode.TITLE, SortMode.ARTIST, SortMode.BPM, SortMode.RECENT)

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as DJRemixProApp).container
                LibraryViewModel(c.trackRepository, c.mixSessionRepository, c.playbackRepository)
            }
        }
    }
}
