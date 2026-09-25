package com.example.djremixpro.testutil

import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.core.model.NowPlaying
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.Recording
import com.example.djremixpro.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.LocalDateTime

/** Fake viết tay cho test ViewModel: đồng bộ, không timer, điều khiển được lỗi. */

fun track(
    index: Int,
    title: String,
    artist: String = "Nghệ sĩ $index",
    durationSec: Int = 200,
    bpm: Float? = 120f,
) = Track(
    id = "t-$index", title = title, artist = artist, durationSec = durationSec,
    bpm = bpm, coverColor = 0, addedIndex = index,
)

fun recording(
    index: Int,
    name: String = "Mix $index",
    durationSec: Int = 192,
    createdAt: LocalDateTime = LocalDateTime.of(2026, 9, 25, 21, 40),
) = Recording(
    id = "r-$index", name = name, durationSec = durationSec, createdAt = createdAt,
    waveSeed = index * 13 + 5, format = RecordFormat.MP3,
)

class TestTrackRepository(
    list: List<Track>,
    override val samples: Pair<Track, Track> = track(100, "Mẫu 1 · Nhịp nhà", bpm = 124f) to
        track(101, "Mẫu 2 · Đêm hội", bpm = 126f),
) : TrackRepository {
    override val tracks = MutableStateFlow(list)
    override fun findById(id: String): Track? =
        tracks.value.firstOrNull { it.id == id } ?: samples.toList().firstOrNull { it.id == id }
}

class TestRecordingRepository(initial: List<Recording> = emptyList()) : RecordingRepository {
    override val recordings = MutableStateFlow(initial)

    /** Khi khác null, mọi thao tác ghi trả failure với lỗi này. */
    var failWith: Throwable? = null
    val added = mutableListOf<Triple<String, Int, RecordFormat>>()

    override suspend fun add(name: String, durationSec: Int, format: RecordFormat): Result<Recording> {
        failWith?.let { return Result.failure(it) }
        added += Triple(name, durationSec, format)
        val r = Recording("r-new-${added.size}", name, durationSec, LocalDateTime.of(2026, 9, 25, 21, 40), 1, format)
        recordings.update { listOf(r) + it }
        return Result.success(r)
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        failWith?.let { return Result.failure(it) }
        if (newName.isBlank()) return Result.failure(IllegalArgumentException("blank"))
        if (recordings.value.none { it.id == id }) return Result.failure(NoSuchElementException(id))
        recordings.update { l -> l.map { if (it.id == id) it.copy(name = newName.trim()) else it } }
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        failWith?.let { return Result.failure(it) }
        if (recordings.value.none { it.id == id }) return Result.failure(NoSuchElementException(id))
        recordings.update { l -> l.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}

class TestSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    var failUpdates = false
    var updateCount = 0
        private set

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        if (failUpdates) throw IOException("disk full")
        updateCount++
        settings.update(transform)
    }
}

class TestMixSessionRepository(initial: MixSession? = null) : MixSessionRepository {
    override val session = MutableStateFlow(initial)
    override fun loadTrack(deck: DeckId, track: Track) = session.update { s ->
        val base = s ?: MixSession(null, null)
        if (deck == DeckId.A) base.copy(deckA = track) else base.copy(deckB = track)
    }
    override fun setSession(session: MixSession?) {
        this.session.value = session
    }
}

/** Mini player không có timer: vị trí chỉ đổi khi test gọi [setPosition]. */
class TestPlaybackRepository : PlaybackRepository {
    override val nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override fun toggle(id: String, title: String, durationSec: Int) {
        nowPlaying.value = if (nowPlaying.value?.id == id) null else NowPlaying(id, title, durationSec, 0)
    }
    override fun stop() {
        nowPlaying.value = null
    }
    fun setPosition(sec: Int) = nowPlaying.update { it?.copy(positionSec = sec) }
}
