package com.example.djremixpro.feature.mixer

import androidx.lifecycle.SavedStateHandle
import com.example.djremixpro.R
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.data.fake.FakeTrackRepository
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.EqBand
import com.example.djremixpro.core.model.MixSession
import com.example.djremixpro.core.model.MixerArgs
import com.example.djremixpro.core.model.MixerEntry
import com.example.djremixpro.core.model.PadTab
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.Track
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.testutil.MainDispatcherRule
import com.example.djremixpro.testutil.TestMixSessionRepository
import com.example.djremixpro.testutil.TestRecordingRepository
import com.example.djremixpro.testutil.TestSettingsRepository
import com.example.djremixpro.testutil.TestTrackRepository
import com.example.djremixpro.testutil.track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/** M01–M19: Mixer giả lập (Mixer.dc.html, D-07, D-12, D-18…D-22). */
@OptIn(ExperimentalCoroutinesApi::class)
class MixerViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val minus = '−'
    private val tracks = FakeTrackRepository()
    private val saigon: Track = tracks.tracks.value.single { it.title == "Sài Gòn lên đèn" } // 124 BPM, 04:12
    private val muaSaoBang: Track = tracks.tracks.value.single { it.title == "Mưa sao băng (Club mix)" } // 128, 04:26
    private val recordings = TestRecordingRepository()
    private val clock: Clock =
        Clock.fixed(LocalDateTime.of(2026, 9, 25, 21, 40).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private lateinit var session: TestMixSessionRepository
    private lateinit var settings: TestSettingsRepository

    private fun TestScope.mixer(
        entry: MixerEntry = MixerEntry.DEFAULT,
        lessonId: Int = -1,
        initialSession: MixSession? = MixSession(saigon, muaSaoBang),
        appSettings: AppSettings = AppSettings(),
        trackRepository: TrackRepository = tracks,
    ): MixerViewModel {
        session = TestMixSessionRepository(initialSession)
        settings = TestSettingsRepository(appSettings)
        val vm = MixerViewModel(
            SavedStateHandle(mapOf(MixerArgs.ENTRY to entry.name, MixerArgs.LESSON_ID to lessonId)),
            trackRepository, recordings, settings, session, clock,
        ).also { created += it }
        runCurrent()
        return vm
    }

    private val created = mutableListOf<MixerViewModel>()

    /**
     * runTest chạy hết scheduler sau thân test; ticker 1 s của Mixer vô hạn khi deck đang phát mà không thể hết bài
     * (loop, giữ jog khi scratch) hoặc đang REC. Dừng mọi deck/REC ở cuối mỗi test để runTest kết thúc.
     */
    private fun mixerTest(block: suspend TestScope.() -> Unit) = runTest(timeout = 30.seconds) {
        try {
            block()
        } finally {
            created.forEach { vm ->
                if (vm.a.isPlaying) vm.onPlayToggle(DeckId.A)
                if (vm.b.isPlaying) vm.onPlayToggle(DeckId.B)
                if (vm.uiState.value.rec.isRecording) vm.onRecToggle()
            }
        }
    }

    private val MixerViewModel.a get() = uiState.value.deckA
    private val MixerViewModel.b get() = uiState.value.deckB
    private val MixerViewModel.ch get() = uiState.value.channels

    // M01 / B10
    @Test
    fun emptyDecks_showPlaceholders_andTransportIgnored() = mixerTest {
        val vm = mixer(initialSession = null)
        val a = vm.a
        assertNull(a.track)
        assertEquals("—", a.bpmLabel)
        assertEquals("${minus}${minus}:${minus}${minus}", a.remainingLabel)
        assertFalse(a.isSpinning)
        val before = vm.uiState.value
        vm.onPlayToggle(DeckId.A)
        vm.onSyncToggle(DeckId.A)
        vm.onCue(DeckId.A)
        vm.onJogTouch(DeckId.A, true)
        vm.onJogRotate(DeckId.A, 90f)
        vm.onOverviewSeek(DeckId.A, 0.5f)
        runCurrent()
        assertEquals(before, vm.uiState.value)
    }

    // M02
    @Test
    fun defaultEntry_restoresSession_bothStopped() = mixerTest {
        val vm = mixer()
        assertEquals("Sài Gòn lên đèn", vm.a.track?.title)
        assertEquals("SG", vm.a.track?.initials)
        assertEquals("Mưa sao băng (Club mix)", vm.b.track?.title)
        assertFalse(vm.a.isPlaying)
        assertFalse(vm.b.isPlaying)
        assertEquals("124.0", vm.a.bpmLabel)
        assertEquals("128.0", vm.b.bpmLabel)
        assertEquals("${minus}04:12", vm.a.remainingLabel)
        assertEquals(7, vm.a.waveSeed)
        assertEquals(29, vm.b.waveSeed)
        assertEquals(8, vm.a.pads.size)
    }

    @Test
    fun samplesEntry_loadsBothSamples_andWritesSession() = mixerTest {
        val vm = mixer(entry = MixerEntry.SAMPLES)
        assertEquals("Mẫu 1 · Nhịp nhà", vm.a.track?.title)
        assertEquals(124f, vm.a.track?.bpm)
        assertEquals("Mẫu 2 · Đêm hội", vm.b.track?.title)
        assertEquals("126.0", vm.b.bpmLabel)
        assertEquals("Mẫu 1 · Nhịp nhà", session.session.value?.deckA?.title)
        assertEquals("Mẫu 2 · Đêm hội", session.session.value?.deckB?.title)
    }

    @Test
    fun unknownEntryArgument_fallsBackToDefault() = mixerTest {
        session = TestMixSessionRepository(MixSession(saigon, null))
        settings = TestSettingsRepository()
        val vm = MixerViewModel(SavedStateHandle(mapOf(MixerArgs.ENTRY to "???")), tracks, recordings, settings, session, clock)
        runCurrent()
        assertEquals("Sài Gòn lên đèn", vm.a.track?.title)
        assertNull(vm.b.track)
        assertNull(vm.uiState.value.coach)
    }

    // M03 / B11, B12, B13
    @Test
    fun syncDeckB_matchesDeckA() = mixerTest {
        val vm = mixer()
        vm.onSyncToggle(DeckId.B)
        val b = vm.b
        assertTrue(b.isSync)
        assertEquals(-3.125f, b.pitchPct, 1e-4f)
        assertEquals("124.0", b.bpmLabel)
        assertEquals("${minus}3.1%", b.pitchLabel)
        assertTrue(b.isPitchShifted)
        assertEquals(0.6953125f, b.pitchPosition, 1e-4f)
        assertEquals(1.8580645f, b.spinPeriodSec, 1e-4f)
        assertEquals(124f, b.waveBpm, 1e-3f)
        assertEquals(128f, b.track!!.bpm) // BPM gốc giữ nguyên

        vm.onSyncToggle(DeckId.B)
        assertFalse(vm.b.isSync)
        assertEquals("128.0", vm.b.bpmLabel)
        assertEquals("+0.0%", vm.b.pitchLabel)
        assertFalse(vm.b.isPitchShifted)
        assertEquals(0.5f, vm.b.pitchPosition, 1e-6f)
    }

    // M05
    @Test
    fun syncDeckA_toFasterDeckB_isPositive() = mixerTest {
        val vm = mixer()
        vm.onSyncToggle(DeckId.A)
        assertEquals("+3.2%", vm.a.pitchLabel)
        assertEquals("128.0", vm.a.bpmLabel)
        assertTrue(vm.a.pitchPosition < 0.5f)
    }

    @Test
    fun syncUsesOtherDecksOriginalBpm_notEffective() = mixerTest {
        val vm = mixer()
        vm.onPitchChanged(DeckId.A, 2f) // A hiệu dụng 126.48
        vm.onSyncToggle(DeckId.B)
        // Mixer:437 dùng otherT.bpm (gốc 124), không dùng BPM hiệu dụng của A.
        assertEquals("124.0", vm.b.bpmLabel)
    }

    // M04
    @Test
    fun sync_whenOtherDeckEmpty_keepsPitchZero_thenFollowsWhenLoaded() = mixerTest {
        val vm = mixer(initialSession = MixSession(saigon, null))
        vm.onSyncToggle(DeckId.A)
        assertTrue(vm.a.isSync)
        assertEquals(0f, vm.a.pitchPct, 0f)
        assertEquals("124.0", vm.a.bpmLabel)
        assertEquals("+0.0%", vm.a.pitchLabel)

        vm.onJogTapped(DeckId.B)
        vm.onLibraryLoad(muaSaoBang.id, DeckId.B)
        assertEquals("128.0", vm.a.bpmLabel)
        assertEquals("+3.2%", vm.a.pitchLabel)
    }

    // M06 / D-19
    @Test
    fun manualPitch_turnsSyncOff_andClampsToRange() = mixerTest {
        val vm = mixer()
        vm.onSyncToggle(DeckId.B)
        vm.onPitchChanged(DeckId.B, 20f)
        assertFalse(vm.b.isSync)
        assertEquals(8f, vm.b.pitchPct, 0f)
        assertEquals("+8.0%", vm.b.pitchLabel)
        assertEquals(0f, vm.b.pitchPosition, 1e-6f)
        vm.onPitchChanged(DeckId.B, -20f)
        assertEquals(-8f, vm.b.pitchPct, 0f)
        assertEquals(1f, vm.b.pitchPosition, 1e-6f)
    }

    @Test
    fun pitchRange_followsSettings_andReclampsExistingPitch() = mixerTest {
        val vm = mixer(appSettings = AppSettings(pitchRangePct = 16))
        assertEquals(16, vm.a.pitchRangePct)
        vm.onPitchChanged(DeckId.A, -12f)
        assertEquals(-12f, vm.a.pitchPct, 0f)
        assertEquals(0.875f, vm.a.pitchPosition, 1e-5f)

        settings.settings.value = AppSettings(pitchRangePct = 6)
        runCurrent()
        assertEquals(-6f, vm.a.pitchPct, 0f)
        assertEquals(1f, vm.a.pitchPosition, 1e-6f)
    }

    // M07 / B13
    @Test
    fun spinning_stopsWhileTouchingWithScratch() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.A)
        assertTrue(vm.a.isSpinning)
        vm.onJogTouch(DeckId.A, true)
        assertTrue(vm.a.isTouching)
        assertFalse(vm.a.isSpinning)
        vm.onScratchToggle(DeckId.A)
        assertTrue(vm.a.isSpinning) // scratch tắt: chạm không giữ đĩa
        vm.onScratchToggle(DeckId.A)
        vm.onJogTouch(DeckId.A, false)
        assertTrue(vm.a.isSpinning)
        vm.onPlayToggle(DeckId.A)
        assertFalse(vm.a.isSpinning)
    }

    @Test
    fun jogRotate_scratchMovesPosition_18secondsPerTurn() = mixerTest {
        val vm = mixer()
        vm.onOverviewSeek(DeckId.A, 0.5f) // 126 s
        vm.onJogRotate(DeckId.A, 360f) // chưa chạm → bỏ qua
        assertEquals(0.5f, vm.a.progress, 1e-4f)
        vm.onJogTouch(DeckId.A, true)
        vm.onJogRotate(DeckId.A, 360f)
        assertEquals((126f + 1.8f) / 252f, vm.a.progress, 1e-4f)
        vm.onJogRotate(DeckId.A, -360f * 200)
        assertEquals(0f, vm.a.progress, 0f)
    }

    @Test
    fun overviewSeek_clampsFraction() = mixerTest {
        val vm = mixer()
        vm.onOverviewSeek(DeckId.B, 1.5f)
        assertEquals(1f, vm.b.progress, 0f)
        assertEquals("${minus}00:00", vm.b.remainingLabel)
        vm.onOverviewSeek(DeckId.B, -1f)
        assertEquals(0f, vm.b.progress, 0f)
    }

    // M08 / B16
    @Test
    fun crossfader_clampAndReset() = mixerTest {
        val vm = mixer()
        assertEquals(0.5f, vm.ch.crossfader, 0f)
        vm.onCrossfaderChanged(1.5f)
        assertEquals(1f, vm.ch.crossfader, 0f)
        vm.onCrossfaderChanged(-0.2f)
        assertEquals(0f, vm.ch.crossfader, 0f)
        vm.onCrossfaderChanged(0.36f)
        assertEquals(0.36f, vm.ch.crossfader, 0f)
        vm.onCrossfaderReset()
        assertEquals(0.5f, vm.ch.crossfader, 0f)
    }

    @Test
    fun volume_defaultsAndClamp() = mixerTest {
        val vm = mixer()
        assertEquals(0.86f, vm.ch.volumeA, 0f)
        assertEquals(0.72f, vm.ch.volumeB, 0f)
        vm.onVolumeChanged(DeckId.A, 1.3f)
        assertEquals(1f, vm.ch.volumeA, 0f)
        vm.onVolumeChanged(DeckId.B, -0.1f)
        assertEquals(0f, vm.ch.volumeB, 0f)
    }

    // M09 / B15 / D-21
    @Test
    fun eq_labels_killAndReset() = mixerTest {
        val vm = mixer()
        assertEquals(listOf(EqBand.HIGH, EqBand.MID, EqBand.LOW, EqBand.FILTER), vm.ch.eqA.map { it.band })
        assertTrue(vm.ch.eqA.all { it.value == 0.5f && !it.isKill })

        vm.onEqChanged(DeckId.A, EqBand.MID, 0.63f)
        assertEquals("+3.5 dB", vm.ch.eqA[1].valueLabel)
        vm.onEqChanged(DeckId.A, EqBand.HIGH, 0.4f)
        assertEquals("${minus}2.7 dB", vm.ch.eqA[0].valueLabel)

        vm.onEqKill(DeckId.B, EqBand.LOW)
        val low = vm.ch.eqB[2]
        assertTrue(low.isKill)
        assertEquals(0f, low.value, 0f)
        assertEquals("Kill", low.valueLabel)
        assertFalse(vm.ch.eqA[2].isKill) // deck kia không bị ảnh hưởng

        vm.onEqReset(DeckId.B, EqBand.LOW)
        assertFalse(vm.ch.eqB[2].isKill)
        assertEquals(0.5f, vm.ch.eqB[2].value, 0f)
        assertEquals("0.0 dB", vm.ch.eqB[2].valueLabel)
    }

    @Test
    fun eq_dragWhileKilled_clearsKill() = mixerTest {
        val vm = mixer()
        vm.onEqKill(DeckId.A, EqBand.HIGH)
        vm.onEqChanged(DeckId.A, EqBand.HIGH, 0.7f)
        assertFalse(vm.ch.eqA[0].isKill)
        assertEquals(0.7f, vm.ch.eqA[0].value, 1e-6f)
    }

    @Test
    fun filter_lpfBelowCenter_hpfAbove() = mixerTest {
        val vm = mixer()
        vm.onEqChanged(DeckId.A, EqBand.FILTER, 0.3f)
        assertEquals("LPF 40%", vm.ch.eqA[3].valueLabel)
        vm.onEqChanged(DeckId.A, EqBand.FILTER, 0.64f)
        assertEquals("HPF 28%", vm.ch.eqA[3].valueLabel)
        vm.onEqChanged(DeckId.A, EqBand.FILTER, 1.4f)
        assertEquals(1f, vm.ch.eqA[3].value, 0f)
    }

    // M10 / B18 / D-12
    @Test
    fun rec_counterRollsOverMinutes_andBeyondAnHour() = mixerTest {
        val vm = mixer(initialSession = null)
        assertEquals("REC", vm.uiState.value.rec.label)
        vm.onRecToggle()
        assertTrue(vm.uiState.value.rec.isRecording)
        assertEquals("00:00", vm.uiState.value.rec.label)

        advanceTimeBy(59_000); runCurrent()
        assertEquals("00:59", vm.uiState.value.rec.label)
        advanceTimeBy(1_000); runCurrent()
        assertEquals("01:00", vm.uiState.value.rec.label)
        advanceTimeBy(1_000); runCurrent()
        assertEquals("01:01", vm.uiState.value.rec.label)
        advanceTimeBy((3725 - 61) * 1_000L); runCurrent()
        assertEquals(3725, vm.uiState.value.rec.elapsedSec)
        assertEquals("62:05", vm.uiState.value.rec.label)

        vm.onRecToggle()
        val rec = vm.uiState.value.rec
        assertFalse(rec.isRecording)
        assertEquals(0, rec.elapsedSec)
        assertEquals("REC", rec.label)
        assertEquals("62:05", vm.uiState.value.saveDialog?.durationLabel)

        advanceUntilIdle() // không còn deck phát, không ghi → ticker phải dừng
        assertEquals(0, vm.uiState.value.rec.elapsedSec)
    }

    // M11 / O7 / B21
    @Test
    fun saveDialog_defaultsFromClockAndSettings() = mixerTest {
        val vm = mixer(appSettings = AppSettings(recordFormat = RecordFormat.WAV, recordQualityKbps = 192))
        vm.onRecToggle()
        advanceTimeBy(5_000); runCurrent()
        vm.onRecToggle()
        val d = vm.uiState.value.saveDialog!!
        assertEquals("00:05", d.durationLabel)
        assertEquals("Mix 25-09 21:40", d.name)
        assertEquals(RecordFormat.WAV, d.format)
        assertEquals(192, d.qualityKbps)
        assertTrue(d.showWavNote)
    }

    @Test
    fun saveDialog_formatToggle_andConfirmAddsRecording() = mixerTest {
        val vm = mixer()
        vm.onRecToggle()
        advanceTimeBy(5_000); runCurrent()
        vm.onRecToggle()
        assertFalse(vm.uiState.value.saveDialog!!.showWavNote)
        vm.onSaveFormatSelected(RecordFormat.WAV)
        assertTrue(vm.uiState.value.saveDialog!!.showWavNote)
        vm.onSaveFormatSelected(RecordFormat.MP3)
        assertFalse(vm.uiState.value.saveDialog!!.showWavNote)
        vm.onSaveFormatSelected(RecordFormat.WAV)
        vm.onSaveNameChanged("Bản mix đầu tiên")

        vm.onSaveConfirmed()
        assertEquals(
            MixerEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_recording_saved))),
            vm.events.first(),
        )
        assertEquals(listOf(Triple("Bản mix đầu tiên", 5, RecordFormat.WAV)), recordings.added)
        assertNull(vm.uiState.value.saveDialog)
    }

    @Test
    fun saveDialog_blankName_fallsBackToDefaultName() = mixerTest {
        val vm = mixer()
        vm.onRecToggle(); advanceTimeBy(2_000); runCurrent(); vm.onRecToggle()
        vm.onSaveNameChanged("   ")
        vm.onSaveConfirmed()
        vm.events.first()
        assertEquals("Mix 25-09 21:40", recordings.added.single().first)
    }

    @Test
    fun saveDialog_discard_addsNothing() = mixerTest {
        val vm = mixer()
        vm.onRecToggle(); advanceTimeBy(2_000); runCurrent(); vm.onRecToggle()
        vm.onSaveDiscarded()
        runCurrent()
        assertNull(vm.uiState.value.saveDialog)
        assertTrue(recordings.added.isEmpty())
    }

    @Test
    fun saveDialog_repositoryFailure_showsError_andKeepsDialog() = mixerTest {
        val vm = mixer()
        vm.onRecToggle(); advanceTimeBy(2_000); runCurrent(); vm.onRecToggle()
        recordings.failWith = IOException("disk")
        vm.onSaveConfirmed()
        assertEquals(
            MixerEvent.ShowToast(ToastMessage(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR)),
            vm.events.first(),
        )
        assertNotNull(vm.uiState.value.saveDialog)
    }

    // M12 / B14 / D-19
    @Test
    fun ticker_advancesPositionBySpeed() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.A)
        vm.onSyncToggle(DeckId.B)
        vm.onPlayToggle(DeckId.B)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(10f / 252f, vm.a.progress, 1e-4f)
        assertEquals("${minus}04:02", vm.a.remainingLabel)
        // B chạy chậm hơn 3.125 % khi sync theo A.
        assertEquals(10f * 0.96875f / 266f, vm.b.progress, 1e-4f)
        assertEquals("${minus}04:17", vm.b.remainingLabel)
        assertTrue(vm.ch.vuActiveA)
    }

    @Test
    fun pausedDeck_doesNotAdvance_andCueReturnsToStart() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.A)
        advanceTimeBy(4_000); runCurrent()
        vm.onPlayToggle(DeckId.A)
        val paused = vm.a.progress
        advanceTimeBy(4_000); runCurrent()
        assertEquals(paused, vm.a.progress, 0f)
        assertFalse(vm.ch.vuActiveA)

        vm.onPlayToggle(DeckId.A)
        vm.onCue(DeckId.A)
        assertFalse(vm.a.isPlaying)
        assertEquals(0f, vm.a.progress, 0f)
    }

    @Test
    fun touchingWithScratch_holdsPosition() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.A)
        vm.onJogTouch(DeckId.A, true)
        advanceTimeBy(3_000); runCurrent()
        assertEquals(0f, vm.a.progress, 0f)
    }

    @Test
    fun trackEnd_stopsDeck_andTickerStops() = mixerTest {
        val short = track(1, "Ngắn", durationSec = 3, bpm = 120f)
        val vm = mixer(
            initialSession = MixSession(short, null),
            trackRepository = TestTrackRepository(listOf(short)),
        )
        vm.onPlayToggle(DeckId.A)
        advanceUntilIdle()
        assertFalse(vm.a.isPlaying)
        assertEquals(1f, vm.a.progress, 0f)
        assertEquals("${minus}00:00", vm.a.remainingLabel)
        vm.onPlayToggle(DeckId.A) // phát lại từ đầu khi đã hết bài
        assertTrue(vm.a.isPlaying)
        assertEquals(0f, vm.a.progress, 0f)
    }

    // M20 — positionSec (hợp đồng v1.2)
    @Test
    fun positionSec_loadTickCueSeek() = mixerTest {
        val vm = mixer()
        assertEquals(0f, vm.a.positionSec, 0f)
        vm.onSyncToggle(DeckId.B)
        vm.onPlayToggle(DeckId.B)
        advanceTimeBy(4_000); runCurrent()
        assertEquals(4 * 0.96875f, vm.b.positionSec, 1e-4f)

        vm.onCue(DeckId.B)
        assertEquals(0f, vm.b.positionSec, 0f)

        vm.onOverviewSeek(DeckId.A, 0.25f)
        assertEquals(63f, vm.a.positionSec, 1e-4f) // 0.25 × 252

        vm.onJogTouch(DeckId.A, true)
        vm.onJogRotate(DeckId.A, 360f)
        assertEquals(64.8f, vm.a.positionSec, 1e-4f)

        vm.onPadTapped(DeckId.A, 1) // hot cue 2 = 1:04
        assertEquals(64f, vm.a.positionSec, 0f)

        vm.onLibraryLoad(tracks.tracks.value[0].id, DeckId.A)
        assertEquals(0f, vm.a.positionSec, 0f)
    }

    @Test
    fun positionSec_fourBeatLoopAt124Bpm_wrapsWithinLoop() = mixerTest {
        val vm = mixer()
        vm.onOverviewSeek(DeckId.A, 0.5f) // start = 126 s
        vm.onPadTabSelected(DeckId.A, PadTab.LOOP)
        vm.onPadTapped(DeckId.A, 4) // 4 beat × 60/124 = 1.935 s
        vm.onPlayToggle(DeckId.A)
        val start = 126f
        val loopLen = 4 * 60f / 124f
        repeat(12) {
            advanceTimeBy(1_000); runCurrent()
            val pos = vm.a.positionSec
            assertTrue("tick ${it + 1}: pos=$pos", pos >= start - 1e-3f && pos < start + loopLen + 1e-3f)
        }
        vm.onPlayToggle(DeckId.A) // loop không bao giờ hết bài: dừng để ticker kết thúc trước khi runTest dọn scheduler
    }

    @Test
    fun positionSec_atTrackEnd_equalsDuration() = mixerTest {
        val vm = mixer()
        vm.onOverviewSeek(DeckId.A, 250f / 252f)
        vm.onPlayToggle(DeckId.A)
        advanceTimeBy(3_000); runCurrent()
        assertEquals(252f, vm.a.positionSec, 0f)
        assertFalse(vm.a.isPlaying)
    }

    // M13 / O5 / B19
    @Test
    fun jogTapOnEmptyDeck_opensLibraryPanel_withBpmMatchAgainstDeckA() = mixerTest {
        val vm = mixer(initialSession = MixSession(saigon, null))
        vm.onJogTapped(DeckId.A) // A đã có bài → không mở
        assertNull(vm.uiState.value.libraryPanel)

        vm.onJogTapped(DeckId.B)
        val panel = vm.uiState.value.libraryPanel!!
        assertEquals(DeckId.B, panel.target)
        val match = panel.rows.associate { it.title to it.isBpmMatch }
        assertEquals(true, match["Chạy về phía biển"]) // 122
        assertEquals(true, match["Sài Gòn lên đèn"]) // 124
        assertEquals(true, match["Không cần lời"]) // 125
        assertEquals(true, match["Tầng thượng 102"]) // 126
        assertEquals(false, match["Mưa sao băng (Club mix)"]) // 128: +3.2 %
        assertEquals(false, match["Đi đâu cũng được"]) // 120: −3.2 %
        assertEquals(false, match["Phố đêm"]) // chưa có BPM
        assertEquals("…", panel.rows.single { it.title == "Phố đêm" }.bpmLabel)
        assertEquals("Phố đêm", panel.rows.last().title)

        vm.onLibraryClose()
        assertNull(vm.uiState.value.libraryPanel)
    }

    @Test
    fun libraryPanel_matchUsesDeckAEffectiveBpm() = mixerTest {
        val vm = mixer(initialSession = MixSession(saigon, null))
        vm.onPitchChanged(DeckId.A, 2f) // 124 × 1.02 = 126.48
        vm.onJogTapped(DeckId.B)
        val match = vm.uiState.value.libraryPanel!!.rows.associate { it.title to it.isBpmMatch }
        assertEquals(true, match["Mưa sao băng (Club mix)"]) // 128/126.48 = +1.2 %
        assertEquals(false, match["Chạy về phía biển"]) // 122/126.48 = −3.5 %
    }

    @Test
    fun libraryPanel_forDeckA_hasNoMatchBadges() = mixerTest {
        val vm = mixer(initialSession = MixSession(null, muaSaoBang))
        vm.onJogTapped(DeckId.A)
        val panel = vm.uiState.value.libraryPanel!!
        assertEquals(DeckId.A, panel.target)
        assertTrue(panel.rows.none { it.isBpmMatch })
    }

    // M14
    @Test
    fun libraryLoad_intoB_stopsDeck_closesPanel_writesSession_keepsA() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.B)
        val target = tracks.tracks.value.single { it.title == "Không cần lời" }
        vm.onLibraryLoad(target.id, DeckId.B)
        assertEquals("Không cần lời", vm.b.track?.title)
        assertFalse(vm.b.isPlaying)
        assertEquals(0f, vm.b.progress, 0f)
        assertNull(vm.uiState.value.libraryPanel)
        assertEquals("Không cần lời", session.session.value?.deckB?.title)
        assertEquals("Sài Gòn lên đèn", vm.a.track?.title)

        val second = tracks.tracks.value.single { it.title == "Vòng quay" }
        vm.onLibraryLoad(second.id, DeckId.A)
        assertEquals("Vòng quay", vm.a.track?.title)
        assertEquals("Không cần lời", vm.b.track?.title) // không ghi đè deck kia (lỗi TR['lib'] của prototype)
    }

    @Test
    fun libraryLoad_trackWithoutBpm_uses120() = mixerTest {
        val vm = mixer(initialSession = null)
        val phoDem = tracks.tracks.value.single { it.bpm == null }
        vm.onLibraryLoad(phoDem.id, DeckId.A)
        assertEquals("120.0", vm.a.bpmLabel)
    }

    @Test
    fun libraryLoad_unknownId_showsError() = mixerTest {
        val vm = mixer(initialSession = null)
        vm.onLibraryLoad("khong-ton-tai", DeckId.A)
        assertEquals(ToastTone.ERROR, (vm.events.first() as MixerEvent.ShowToast).message.tone)
        assertNull(vm.a.track)
    }

    // M15 / B17 / D-20
    @Test
    fun cuePads_defaultPoints_setAndJump() = mixerTest {
        val vm = mixer()
        val pads = vm.a.pads
        assertEquals((1..8).map { UiText.Raw(it.toString()) }, pads.map { it.label })
        assertEquals(List(3) { PadState.ON } + List(5) { PadState.OFF }, pads.map { it.state })
        assertEquals(listOf(UiText.Raw("0:32"), UiText.Raw("1:04"), UiText.Raw("2:08")), pads.take(3).map { it.sub })
        assertEquals(UiText.Res(R.string.mixer_pad_empty), pads[3].sub)

        vm.onOverviewSeek(DeckId.A, 0.5f) // 126 s
        vm.onPadTapped(DeckId.A, 3)
        assertEquals(PadState.ON, vm.a.pads[3].state)
        assertEquals(UiText.Raw("2:06"), vm.a.pads[3].sub)

        vm.onPadTapped(DeckId.A, 0)
        assertEquals(32f / 252f, vm.a.progress, 1e-4f)
    }

    @Test
    fun loopPads_toggleLengthAndLoopBeats() = mixerTest {
        val vm = mixer()
        vm.onPadTabSelected(DeckId.A, PadTab.LOOP)
        val pads = vm.a.pads
        assertEquals(
            listOf("1/4", "1/2", "1", "2", "4", "8", "16", "In/Out").map { UiText.Raw(it) },
            pads.map { it.label },
        )
        assertEquals(PadStyle.NUMERIC_SMALL, pads.last().style)
        assertNull(pads.last().sub)
        assertTrue(pads.all { it.state == PadState.OFF })

        vm.onPadTapped(DeckId.A, 4)
        assertEquals(PadState.ON, vm.a.pads[4].state)
        assertEquals(4f, vm.a.loopBeats)
        vm.onPadTapped(DeckId.A, 5)
        assertEquals(8f, vm.a.loopBeats)
        assertEquals(1, vm.a.pads.count { it.state == PadState.ON })
        vm.onPadTapped(DeckId.A, 5)
        assertNull(vm.a.loopBeats)
    }

    @Test
    fun loop_keepsPositionInsideLoopWhilePlaying() = mixerTest {
        val vm = mixer()
        vm.onPadTabSelected(DeckId.A, PadTab.LOOP)
        vm.onPlayToggle(DeckId.A)
        vm.onPadTapped(DeckId.A, 3) // 2 beat ở 124 BPM ≈ 0.968 s
        advanceTimeBy(10_000); runCurrent()
        assertTrue("progress=${vm.a.progress}", vm.a.progress * 252f < 2 * 60f / 124f + 1e-3f)
        vm.onPlayToggle(DeckId.A) // dừng ticker vô hạn của loop
    }

    @Test
    fun fxAndSamplerPads_toggle_andLongPressOpensSheet() = mixerTest {
        val vm = mixer()
        vm.onPadTabSelected(DeckId.A, PadTab.FX)
        assertEquals(
            listOf("Echo", "Flanger", "Bitcrush", "Gate", "Reverb", "Phaser", "Roll", "Brake").map { UiText.Raw(it) },
            vm.a.pads.map { it.label },
        )
        assertTrue(vm.a.pads.all { it.style == PadStyle.TEXT })
        vm.onPadTapped(DeckId.A, 0)
        assertEquals(PadState.ON, vm.a.pads[0].state)
        vm.onPadTapped(DeckId.A, 0)
        assertEquals(PadState.OFF, vm.a.pads[0].state)

        vm.onPadLongPressed(DeckId.A, 0)
        assertEquals("Echo", vm.uiState.value.fxSheet?.fxName)
        assertEquals(PadState.HELD, vm.a.pads[0].state)

        vm.onPadTabSelected(DeckId.B, PadTab.SAMPLER)
        vm.onPadTapped(DeckId.B, 3)
        assertEquals(UiText.Raw("Vỗ tay"), vm.b.pads[3].label)
        assertEquals(PadState.NEUTRAL_ON, vm.b.pads[3].state)
    }

    @Test
    fun longPressOutsideFxTab_andBadIndex_areIgnored() = mixerTest {
        val vm = mixer()
        vm.onPadLongPressed(DeckId.A, 0) // tab CUE
        assertNull(vm.uiState.value.fxSheet)
        vm.onPadTabSelected(DeckId.A, PadTab.FX)
        vm.onPadLongPressed(DeckId.A, 8)
        vm.onPadTapped(DeckId.A, -1)
        assertNull(vm.uiState.value.fxSheet)
        assertEquals(8, vm.a.pads.size)
    }

    @Test
    fun modeAndTabChanges() = mixerTest {
        val vm = mixer()
        assertEquals(DeckMode.JOG, vm.a.mode)
        vm.onModeChange(DeckId.A, DeckMode.PAD)
        assertEquals(DeckMode.PAD, vm.a.mode)
        assertEquals(DeckMode.JOG, vm.b.mode)
        vm.onPadTabSelected(DeckId.A, PadTab.SAMPLER)
        assertEquals(PadTab.SAMPLER, vm.a.padTab)
    }

    // M16 / O6 / B20 / D-22
    @Test
    fun fxSheet_readoutClampAndBeatLength() = mixerTest {
        val vm = mixer()
        vm.onPadTabSelected(DeckId.A, PadTab.FX)
        vm.onPadLongPressed(DeckId.A, 0)
        var fx = vm.uiState.value.fxSheet!!
        assertEquals(DeckId.A, fx.deck)
        assertEquals("1/2 beat · 70%", fx.readout)
        assertEquals(listOf("1/4", "1/2", "1", "2", "4"), fx.beatLengths)
        assertEquals(2, fx.selectedBeatLength)
        assertEquals(0.6f, fx.wetDry, 0f)

        vm.onFxChanged(1f, 0f)
        assertEquals("1 beat · 100%", vm.uiState.value.fxSheet!!.readout)
        vm.onFxChanged(-0.5f, 1.5f)
        fx = vm.uiState.value.fxSheet!!
        assertEquals(0f, fx.x, 0f)
        assertEquals(1f, fx.y, 0f)
        assertEquals("1/16 beat · 0%", fx.readout)

        vm.onFxBeatLengthSelected(4)
        assertEquals(4, vm.uiState.value.fxSheet!!.selectedBeatLength)
        assertEquals("1/16 beat · 0%", vm.uiState.value.fxSheet!!.readout) // độ dài beat không đổi readout
        vm.onFxBeatLengthSelected(9)
        assertEquals(4, vm.uiState.value.fxSheet!!.selectedBeatLength)

        vm.onFxReset()
        assertEquals("1/2 beat · 70%", vm.uiState.value.fxSheet!!.readout)
        assertEquals(2, vm.uiState.value.fxSheet!!.selectedBeatLength)
    }

    @Test
    fun fxSheet_toggleSwitch_andClose() = mixerTest {
        val vm = mixer()
        vm.onPadTabSelected(DeckId.B, PadTab.FX)
        vm.onPadLongPressed(DeckId.B, 4)
        assertEquals("Reverb", vm.uiState.value.fxSheet?.fxName)
        assertFalse(vm.uiState.value.fxSheet!!.enabled)
        vm.onFxToggle()
        assertTrue(vm.uiState.value.fxSheet!!.enabled)
        vm.onFxClose()
        assertNull(vm.uiState.value.fxSheet)
        assertEquals(PadState.ON, vm.b.pads[4].state)
    }

    // M17 / O8 / B22 / D-18
    @Test
    fun syncLesson_showsCoach_completesOnSyncB() = mixerTest {
        val vm = mixer(entry = MixerEntry.LESSON, lessonId = MixerViewModel.SYNC_LESSON_ID)
        assertTrue(vm.a.isPlaying)
        assertNotNull(vm.b.track)
        assertFalse(vm.b.isSync)
        val coach = vm.uiState.value.coach!!
        assertEquals(3, coach.step)
        assertEquals(5, coach.totalSteps)
        assertFalse(coach.isDone)
        assertEquals(UiText.Res(R.string.coach_sync_todo_title), coach.title)
        assertEquals(CoachTarget.SYNC_B, coach.target)

        vm.onSyncToggle(DeckId.B)
        val done = vm.uiState.value.coach!!
        assertTrue(done.isDone)
        assertEquals(UiText.Res(R.string.coach_sync_done_title), done.title)
        assertEquals(UiText.Res(R.string.coach_sync_done_body), done.body)

        vm.onCoachNext()
        assertNull(vm.uiState.value.coach)
    }

    @Test
    fun syncLesson_withoutSession_usesSamples_andSkipCloses() = mixerTest {
        val vm = mixer(entry = MixerEntry.LESSON, lessonId = 2, initialSession = null)
        assertEquals("Mẫu 1 · Nhịp nhà", vm.a.track?.title)
        assertEquals("Mẫu 2 · Đêm hội", vm.b.track?.title)
        vm.onCoachSkip()
        assertNull(vm.uiState.value.coach)
    }

    @Test
    fun otherLesson_hasNoCoach() = mixerTest {
        val vm = mixer(entry = MixerEntry.LESSON, lessonId = 3)
        assertNull(vm.uiState.value.coach)
        assertFalse(vm.a.isPlaying)
    }

    // M18 / D-19
    @Test
    fun quickSettings_bothDecksLoaded_opensLibraryForB_withoutEvent() = mixerTest {
        // D-19 (v1.3): nút tune gọi openLib như prototype (Mixer:494); libTarget mặc định 'B'.
        val vm = mixer()
        vm.onQuickSettings()
        val panel = vm.uiState.value.libraryPanel!!
        assertEquals(DeckId.B, panel.target)
        assertEquals(10, panel.rows.size)
        assertNull(withTimeoutOrNull(100) { vm.events.first() })
    }

    @Test
    fun quickSettings_targetsFirstEmptyDeck() = mixerTest {
        assertEquals(DeckId.A, mixer(initialSession = null).also { it.onQuickSettings() }.uiState.value.libraryPanel?.target)
        assertEquals(DeckId.B, mixer(initialSession = MixSession(saigon, null)).also { it.onQuickSettings() }.uiState.value.libraryPanel?.target)
        assertEquals(DeckId.A, mixer(initialSession = MixSession(null, muaSaoBang)).also { it.onQuickSettings() }.uiState.value.libraryPanel?.target)
    }

    @Test
    fun quickSettings_thenLoadIntoLoadedDeckB_replacesTrack() = mixerTest {
        val vm = mixer()
        vm.onPlayToggle(DeckId.B)
        advanceTimeBy(3_000); runCurrent()
        vm.onQuickSettings()
        val replacement = tracks.tracks.value.single { it.title == "Tầng thượng 102" }
        vm.onLibraryLoad(replacement.id, DeckId.B)
        assertEquals("Tầng thượng 102", vm.b.track?.title)
        assertEquals(0f, vm.b.positionSec, 0f)
        assertFalse(vm.b.isPlaying)
        assertNull(vm.uiState.value.libraryPanel)
        assertEquals("Tầng thượng 102", session.session.value?.deckB?.title)
    }

    @Test
    fun back_emitsClose() = mixerTest {
        val vm = mixer()
        vm.onBack()
        assertEquals(MixerEvent.Close, vm.events.first())
    }

    @Test
    fun settings_hapticsScreenOnAndDefaultJogMode() = mixerTest {
        val vm = mixer(appSettings = AppSettings(haptic = false, keepScreenOn = false, defaultJogMode = DeckMode.PAD))
        assertFalse(vm.uiState.value.hapticsEnabled)
        assertFalse(vm.uiState.value.keepScreenOn)
        assertEquals(DeckMode.PAD, vm.a.mode)
        assertEquals(DeckMode.PAD, vm.b.mode)

        vm.onModeChange(DeckId.A, DeckMode.JOG)
        settings.settings.value = AppSettings(defaultJogMode = DeckMode.PAD, haptic = true)
        runCurrent()
        assertEquals(DeckMode.JOG, vm.a.mode) // chế độ mặc định chỉ áp dụng lần đầu
        assertTrue(vm.uiState.value.hapticsEnabled)
    }

    // M19 / D-15
    @Test
    fun numbers_useDotDecimal_underCommaLocale() = mixerTest {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            val vm = mixer()
            vm.onSyncToggle(DeckId.B)
            assertEquals("124.0", vm.b.bpmLabel)
            assertEquals("${minus}3.1%", vm.b.pitchLabel)
            vm.onEqChanged(DeckId.A, EqBand.MID, 0.63f)
            assertEquals("+3.5 dB", vm.ch.eqA[1].valueLabel)
        } finally {
            Locale.setDefault(saved)
        }
    }
}
