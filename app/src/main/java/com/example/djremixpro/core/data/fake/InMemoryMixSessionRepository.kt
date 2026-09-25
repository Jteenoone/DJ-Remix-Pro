package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared deck state between the shell and the mixer (D-11, D-17). Seeded with the session of App.dc.html:343. */
class InMemoryMixSessionRepository(trackRepository: TrackRepository) : MixSessionRepository {

    private val _session = MutableStateFlow<MixSession?>(
        trackRepository.tracks.value.let { tracks ->
            val a = tracks.firstOrNull { it.title == SEED_TITLE_A }
            val b = tracks.firstOrNull { it.title == SEED_TITLE_B }
            if (a == null && b == null) null else MixSession(a, b)
        },
    )
    override val session: StateFlow<MixSession?> = _session.asStateFlow()

    override fun loadTrack(deck: DeckId, track: Track) {
        _session.update { current ->
            val base = current ?: MixSession(null, null)
            when (deck) {
                DeckId.A -> base.copy(deckA = track)
                DeckId.B -> base.copy(deckB = track)
            }
        }
    }

    override fun setSession(session: MixSession?) {
        _session.value = session
    }

    companion object {
        const val SEED_TITLE_A = "Sài Gòn lên đèn"
        const val SEED_TITLE_B = "Mưa sao băng (Club mix)"
    }
}
