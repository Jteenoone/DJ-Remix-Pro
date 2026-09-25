package com.example.djremixpro.feature.home

import com.example.djremixpro.core.data.fake.FakeRecordingRepository
import com.example.djremixpro.core.data.fake.FakeTrackRepository
import com.example.djremixpro.core.data.fake.InMemoryMixSessionRepository
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestMixSessionRepository
import com.example.djremixpro.testutil.TestPlaybackRepository
import com.example.djremixpro.testutil.TestRecordingRepository
import com.example.djremixpro.testutil.recording
import com.example.djremixpro.testutil.track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V04–V06: người mới / người quay lại (D-09), phát bản mix gần đây (App:315–317). */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val tracks = FakeTrackRepository()
    private val playback = TestPlaybackRepository()

    private fun seeded() = HomeViewModel(
        tracks, FakeRecordingRepository(), InMemoryMixSessionRepository(tracks), playback,
    )

    // V04
    @Test
    fun returningUser_seedData() = runTest {
        val s = seeded().uiState.value
        assertFalse(s.isNewUser)
        assertEquals(
            listOf(
                DeckTrackRow(DeckId.A, "Sài Gòn lên đèn", "124"),
                DeckTrackRow(DeckId.B, "Mưa sao băng (Club mix)", "128"),
            ),
            s.session,
        )
        assertEquals(listOf("Mix 25-09 21:40", "Mix 24-09 22:05"), s.recent.map { it.name })
        assertEquals(listOf("03:12", "05:47"), s.recent.map { it.durationLabel })
        assertTrue(s.recent.none { it.isPlaying })
        assertTrue(s.showAd)
    }

    // V05
    @Test
    fun newUser_noRecordingsAndNoSession() = runTest {
        val vm = HomeViewModel(tracks, TestRecordingRepository(), TestMixSessionRepository(null), playback)
        runCurrent()
        val s = vm.uiState.value
        assertTrue(s.isNewUser)
        assertEquals(
            listOf(DeckTrackRow(DeckId.A, "Mẫu 1 · Nhịp nhà", "124"), DeckTrackRow(DeckId.B, "Mẫu 2 · Đêm hội", "126")),
            s.samples,
        )
        assertTrue(s.session.isEmpty())
        assertTrue(s.recent.isEmpty())
    }

    @Test
    fun onlyRecordingsEmpty_isStillReturning() = runTest {
        val session = TestMixSessionRepository(MixSession(track(1, "Bài A", bpm = 124f), null))
        val vm = HomeViewModel(tracks, TestRecordingRepository(), session, playback)
        runCurrent()
        assertFalse(vm.uiState.value.isNewUser)
        assertEquals(listOf(DeckTrackRow(DeckId.A, "Bài A", "124")), vm.uiState.value.session)
    }

    @Test
    fun onlySessionEmpty_isStillReturning() = runTest {
        val vm = HomeViewModel(tracks, TestRecordingRepository(listOf(recording(1))), TestMixSessionRepository(null), playback)
        runCurrent()
        assertFalse(vm.uiState.value.isNewUser)
    }

    @Test
    fun becomesReturningAfterFirstLoad() = runTest {
        val session = TestMixSessionRepository(null)
        val vm = HomeViewModel(tracks, TestRecordingRepository(), session, playback)
        runCurrent()
        assertTrue(vm.uiState.value.isNewUser)
        session.loadTrack(DeckId.B, track(2, "Bài B", bpm = null))
        runCurrent()
        assertFalse(vm.uiState.value.isNewUser)
        assertEquals(listOf(DeckTrackRow(DeckId.B, "Bài B", "…")), vm.uiState.value.session)
    }

    // V06
    @Test
    fun recentPlayToggle_onlyOnePlaysAtATime() = runTest {
        val vm = seeded()
        runCurrent()
        val (first, second) = vm.uiState.value.recent.map { it.id }

        vm.onRecentPlayToggle(first)
        runCurrent()
        assertEquals(listOf(true, false), vm.uiState.value.recent.map { it.isPlaying })

        vm.onRecentPlayToggle(second)
        runCurrent()
        assertEquals(listOf(false, true), vm.uiState.value.recent.map { it.isPlaying })

        vm.onRecentPlayToggle(second)
        runCurrent()
        assertEquals(listOf(false, false), vm.uiState.value.recent.map { it.isPlaying })
    }

    @Test
    fun recentPlayToggle_unknownId_isIgnored() = runTest {
        val vm = seeded()
        vm.onRecentPlayToggle("khong-ton-tai")
        runCurrent()
        assertEquals(null, playback.nowPlaying.value)
    }
}
