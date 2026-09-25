package com.example.djremixpro.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.core.model.NowPlaying
import com.example.djremixpro.core.model.Recording
import com.example.djremixpro.core.util.BpmFormat
import com.example.djremixpro.core.util.TimeFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    trackRepository: TrackRepository,
    private val recordingRepository: RecordingRepository,
    mixSessionRepository: MixSessionRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val samples: List<DeckTrackRow> = trackRepository.samples.let { (a, b) ->
        listOf(
            DeckTrackRow(DeckId.A, a.title, BpmFormat.short(a.bpm)),
            DeckTrackRow(DeckId.B, b.title, BpmFormat.short(b.bpm)),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        recordingRepository.recordings,
        mixSessionRepository.session,
        playbackRepository.nowPlaying,
        ::buildState,
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildState(
            recordingRepository.recordings.value,
            mixSessionRepository.session.value,
            playbackRepository.nowPlaying.value,
        ),
    )

    fun onRecentPlayToggle(id: String) {
        val recording = recordingRepository.recordings.value.firstOrNull { it.id == id } ?: return
        playbackRepository.toggle(recording.id, recording.name, recording.durationSec)
    }

    private fun buildState(recordings: List<Recording>, session: MixSession?, nowPlaying: NowPlaying?): HomeUiState {
        val sessionRows = buildList {
            session?.deckA?.let { add(DeckTrackRow(DeckId.A, it.title, BpmFormat.short(it.bpm))) }
            session?.deckB?.let { add(DeckTrackRow(DeckId.B, it.title, BpmFormat.short(it.bpm))) }
        }
        return HomeUiState(
            // D-09: new user = no recordings and no mix session.
            isNewUser = recordings.isEmpty() && session == null,
            samples = samples,
            session = sessionRows,
            showAd = true,
            recent = recordings.take(RECENT_COUNT).map {
                RecentMixRow(it.id, it.name, TimeFormat.mmss(it.durationSec), isPlaying = nowPlaying?.id == it.id)
            },
        )
    }

    companion object {
        private const val RECENT_COUNT = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as DJRemixProApp).container
                HomeViewModel(c.trackRepository, c.recordingRepository, c.mixSessionRepository, c.playbackRepository)
            }
        }
    }
}
