package com.example.djremixpro.core.data.fake

import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.RecordFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

/** R01–R07: dữ liệu seed và hành vi của repository in-memory (D-08, hợp đồng §6). */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeRepositoriesTest {

    private val fixedClock: Clock =
        Clock.fixed(LocalDateTime.of(2026, 9, 26, 8, 5).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    // R01
    @Test
    fun tracks_seedMatchesPrototypeOrder() {
        val repo = FakeTrackRepository()
        val tracks = repo.tracks.value
        assertEquals(
            listOf(
                "Tết về rồi", "Vòng quay", "Chạy về phía biển", "Sài Gòn lên đèn", "Không cần lời",
                "Tầng thượng 102", "Mưa sao băng (Club mix)", "Phố đêm", "Ánh đèn sân khấu", "Đi đâu cũng được",
            ),
            tracks.map { it.title },
        )
        assertEquals((0..9).toList(), tracks.map { it.addedIndex })
        assertNull(tracks.single { it.title == "Phố đêm" }.bpm)
        assertEquals(195, tracks.first().durationSec) // 03:15
    }

    @Test
    fun tracks_samplesAndLookup() {
        val repo = FakeTrackRepository()
        val (a, b) = repo.samples
        assertEquals("Mẫu 1 · Nhịp nhà", a.title)
        assertEquals(124f, a.bpm)
        assertEquals("Mẫu 2 · Đêm hội", b.title)
        assertEquals(126f, b.bpm)
        assertEquals(a, repo.findById(a.id))
        assertEquals("Vòng quay", repo.findById(repo.tracks.value[1].id)?.title)
        assertNull(repo.findById("khong-ton-tai"))
    }

    // R02
    @Test
    fun recordings_seedNewestFirstWithWaveSeeds() {
        val list = FakeRecordingRepository(fixedClock).recordings.value
        assertEquals(listOf("Mix 25-09 21:40", "Mix 24-09 22:05", "Tiệc sinh nhật Linh", "Mix 18-09 23:10"), list.map { it.name })
        assertEquals(listOf(5, 18, 31, 44), list.map { it.waveSeed })
        assertEquals(listOf(192, 347, 750, 125), list.map { it.durationSec })
    }

    // R03
    @Test
    fun recordings_addPrependsWithClockTime() = runTest {
        val repo = FakeRecordingRepository(fixedClock)
        val result = repo.add("Mix 26-09 08:05", 61, RecordFormat.WAV)
        assertTrue(result.isSuccess)
        val first = repo.recordings.value.first()
        assertEquals("Mix 26-09 08:05", first.name)
        assertEquals(61, first.durationSec)
        assertEquals(RecordFormat.WAV, first.format)
        assertEquals(LocalDateTime.of(2026, 9, 26, 8, 5), first.createdAt)
        assertEquals(5, repo.recordings.value.size)
    }

    // R04
    @Test
    fun recordings_renameBlank_failsAndKeepsList() = runTest {
        val repo = FakeRecordingRepository(fixedClock)
        val before = repo.recordings.value
        for (blank in listOf("", "   ")) {
            val r = repo.rename("rec-1", blank)
            assertTrue(r.exceptionOrNull() is IllegalArgumentException)
        }
        assertEquals(before, repo.recordings.value)
    }

    @Test
    fun recordings_renameTrimsName() = runTest {
        val repo = FakeRecordingRepository(fixedClock)
        assertTrue(repo.rename("rec-3", "  Tiệc Linh  ").isSuccess)
        assertEquals("Tiệc Linh", repo.recordings.value.single { it.id == "rec-3" }.name)
    }

    // R05
    @Test
    fun recordings_unknownId_failsWithNoSuchElement() = runTest {
        val repo = FakeRecordingRepository(fixedClock)
        val before = repo.recordings.value
        assertTrue(repo.delete("rec-99").exceptionOrNull() is NoSuchElementException)
        assertTrue(repo.rename("rec-99", "Tên").exceptionOrNull() is NoSuchElementException)
        assertEquals(before, repo.recordings.value)
    }

    @Test
    fun recordings_deleteRemovesOnlyThatItem() = runTest {
        val repo = FakeRecordingRepository(fixedClock)
        assertTrue(repo.delete("rec-2").isSuccess)
        assertEquals(listOf("rec-1", "rec-3", "rec-4"), repo.recordings.value.map { it.id })
    }

    // R06
    @Test
    fun playback_togglePlaysStopsAndSwitches() = runTest {
        val repo = SimulatedPlaybackRepository(backgroundScope)
        repo.toggle("a", "Bài A", 10)
        assertEquals(0, repo.nowPlaying.value?.positionSec)
        repo.toggle("a", "Bài A", 10)
        assertNull(repo.nowPlaying.value)

        repo.toggle("a", "Bài A", 10)
        advanceTimeBy(3_000); runCurrent()
        repo.toggle("b", "Bài B", 10)
        assertEquals("b", repo.nowPlaying.value?.id)
        assertEquals(0, repo.nowPlaying.value?.positionSec)
    }

    @Test
    fun playback_advancesOneSecondPerTick_andClearsAtEnd() = runTest {
        val repo = SimulatedPlaybackRepository(backgroundScope)
        repo.toggle("a", "Bài A", 3)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(1, repo.nowPlaying.value?.positionSec)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(2, repo.nowPlaying.value?.positionSec)
        advanceTimeBy(1_000); runCurrent()
        assertNull(repo.nowPlaying.value)
    }

    @Test
    fun playback_stop_haltsTicker() = runTest {
        val repo = SimulatedPlaybackRepository(backgroundScope)
        repo.toggle("a", "Bài A", 100)
        repo.stop()
        advanceTimeBy(5_000); runCurrent()
        assertNull(repo.nowPlaying.value)
    }

    // R07
    @Test
    fun session_seedAndLoadIntoEmptySession() {
        val tracks = FakeTrackRepository()
        val repo = InMemoryMixSessionRepository(tracks)
        assertEquals("Sài Gòn lên đèn", repo.session.value?.deckA?.title)
        assertEquals("Mưa sao băng (Club mix)", repo.session.value?.deckB?.title)

        repo.setSession(null)
        val t = tracks.tracks.value[0]
        repo.loadTrack(DeckId.B, t)
        assertNull(repo.session.value?.deckA)
        assertEquals(t, repo.session.value?.deckB)
    }
}
