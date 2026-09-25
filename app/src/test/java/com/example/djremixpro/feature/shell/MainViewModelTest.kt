package com.example.djremixpro.feature.shell

import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestPlaybackRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** V27: mini player (App:227–234). */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val playback = TestPlaybackRepository()

    @Test
    fun nothingPlaying_hidesMiniPlayer() = runTest {
        val vm = MainViewModel(playback)
        runCurrent()
        assertNull(vm.uiState.value.miniPlayer)
    }

    @Test
    fun playing_showsTimeLabelAndProgress() = runTest {
        val vm = MainViewModel(playback)
        playback.toggle("rec-1", "Mix 25-09 21:40", 192)
        playback.setPosition(77)
        runCurrent()
        val mini = vm.uiState.value.miniPlayer!!
        assertEquals("rec-1", mini.id)
        assertEquals("Mix 25-09 21:40", mini.title)
        assertEquals("01:17 / 03:12", mini.timeLabel)
        assertEquals(77f / 192f, mini.progress, 1e-4f)
    }

    @Test
    fun zeroDuration_progressIsZeroNotNaN() = runTest {
        val vm = MainViewModel(playback)
        playback.toggle("x", "X", 0)
        runCurrent()
        assertEquals(0f, vm.uiState.value.miniPlayer!!.progress, 0f)
    }

    @Test
    fun stop_clearsMiniPlayer() = runTest {
        val vm = MainViewModel(playback)
        playback.toggle("rec-1", "Mix", 192)
        runCurrent()
        vm.onMiniPlayerStop()
        runCurrent()
        assertNull(vm.uiState.value.miniPlayer)
        assertNull(playback.nowPlaying.value)
    }
}
