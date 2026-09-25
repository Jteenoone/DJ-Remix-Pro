package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.model.NowPlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mini player without audio (D-07): position advances 1 s per second; the item is cleared at the end. */
class SimulatedPlaybackRepository(private val scope: CoroutineScope) : PlaybackRepository {

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private var ticker: Job? = null

    @Synchronized
    override fun toggle(id: String, title: String, durationSec: Int) {
        if (_nowPlaying.value?.id == id) {
            stop()
            return
        }
        ticker?.cancel()
        _nowPlaying.value = NowPlaying(id, title, durationSec.coerceAtLeast(0), 0)
        ticker = scope.launch {
            while (true) {
                delay(TICK_MS)
                val current = _nowPlaying.value ?: break
                if (current.id != id) break
                val next = current.positionSec + 1
                if (next >= current.durationSec) {
                    _nowPlaying.value = null
                    break
                }
                _nowPlaying.value = current.copy(positionSec = next)
            }
        }
    }

    @Synchronized
    override fun stop() {
        ticker?.cancel()
        ticker = null
        _nowPlaying.value = null
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
