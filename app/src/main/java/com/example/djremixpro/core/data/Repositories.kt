package com.example.djremixpro.core.data

import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.GlossaryTerm
import com.example.djremixpro.core.model.Lesson
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.core.model.NowPlaying
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.Recording
import com.example.djremixpro.core.model.Tip
import com.example.djremixpro.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TrackRepository {
    /** The 10 library songs in their original order (= [Track.addedIndex]). */
    val tracks: StateFlow<List<Track>>

    /** Sample tracks for deck A and deck B. */
    val samples: Pair<Track, Track>

    /** Looks up library songs and samples. */
    fun findById(id: String): Track?
}

interface RecordingRepository {
    /** Newest first. */
    val recordings: StateFlow<List<Recording>>

    suspend fun add(name: String, durationSec: Int, format: RecordFormat): Result<Recording>

    /** Blank name → failure(IllegalArgumentException); unknown id → failure(NoSuchElementException). */
    suspend fun rename(id: String, newName: String): Result<Unit>

    /** Unknown id → failure(NoSuchElementException). */
    suspend fun delete(id: String): Result<Unit>
}

interface LearnRepository {
    val lessons: List<Lesson>
    val completedCount: StateFlow<Int>
    val glossary: List<GlossaryTerm>
    val tips: List<Tip>
}

interface SettingsRepository {
    /** Never throws: an IO error emits [AppSettings] defaults. */
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface MixSessionRepository {
    val session: StateFlow<MixSession?>

    /** Creates the session when there is none. */
    fun loadTrack(deck: DeckId, track: Track)

    fun setSession(session: MixSession?)
}

/** Simulated mini player: a single item at a time. */
interface PlaybackRepository {
    val nowPlaying: StateFlow<NowPlaying?>

    /** Same id as the current item → stop; otherwise play [id] from 0. */
    fun toggle(id: String, title: String, durationSec: Int)

    fun stop()
}
