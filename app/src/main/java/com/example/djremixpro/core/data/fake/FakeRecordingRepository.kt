package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime

/** In-memory recordings (D-08), seeded with App.dc.html:314; lost when the app is killed. */
class FakeRecordingRepository(private val clock: Clock = Clock.systemDefaultZone()) : RecordingRepository {

    // "Tiệc sinh nhật Linh" has no time in its name; 20:00 is assumed.
    private val _recordings = MutableStateFlow(
        listOf(
            seed(0, "Mix 25-09 21:40", 3 * 60 + 12, LocalDateTime.of(2026, 9, 25, 21, 40)),
            seed(1, "Mix 24-09 22:05", 5 * 60 + 47, LocalDateTime.of(2026, 9, 24, 22, 5)),
            seed(2, "Tiệc sinh nhật Linh", 12 * 60 + 30, LocalDateTime.of(2026, 9, 20, 20, 0)),
            seed(3, "Mix 18-09 23:10", 2 * 60 + 5, LocalDateTime.of(2026, 9, 18, 23, 10)),
        ),
    )
    override val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    private var nextIndex = 4

    override suspend fun add(name: String, durationSec: Int, format: RecordFormat): Result<Recording> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Recording name is blank"))
        val recording = synchronized(this) {
            val index = nextIndex++
            Recording(
                id = "rec-${index + 1}",
                name = trimmed,
                durationSec = durationSec.coerceAtLeast(0),
                createdAt = LocalDateTime.now(clock),
                waveSeed = index * 13 + 5,
                format = format,
            )
        }
        _recordings.update { listOf(recording) + it }
        return Result.success(recording)
    }

    override suspend fun rename(id: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Recording name is blank"))
        if (_recordings.value.none { it.id == id }) return Result.failure(NoSuchElementException(id))
        _recordings.update { list -> list.map { if (it.id == id) it.copy(name = trimmed) else it } }
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (_recordings.value.none { it.id == id }) return Result.failure(NoSuchElementException(id))
        _recordings.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    private fun seed(index: Int, name: String, durationSec: Int, createdAt: LocalDateTime) = Recording(
        id = "rec-${index + 1}",
        name = name,
        durationSec = durationSec,
        createdAt = createdAt,
        waveSeed = index * 13 + 5,
        format = RecordFormat.MP3,
    )
}
