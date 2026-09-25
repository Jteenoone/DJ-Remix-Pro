package com.example.djremixpro.feature.home

import com.example.djremixpro.core.model.DeckId

/** [bpmLabel] is "124" (or "…" when the BPM is unknown). */
data class DeckTrackRow(val deck: DeckId, val title: String, val bpmLabel: String)

data class RecentMixRow(val id: String, val name: String, val durationLabel: String, val isPlaying: Boolean)

data class HomeUiState(
    val isNewUser: Boolean = false,
    val samples: List<DeckTrackRow> = emptyList(),
    val session: List<DeckTrackRow> = emptyList(),
    val showAd: Boolean = true,
    val recent: List<RecentMixRow> = emptyList(),
)
