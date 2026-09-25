package com.example.djremixpro.feature.mixer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.MixSessionRepository
import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.data.SettingsRepository
import com.example.djremixpro.core.data.TrackRepository
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.EqBand
import com.example.djremixpro.core.model.MixerArgs
import com.example.djremixpro.core.model.MixerEntry
import com.example.djremixpro.core.model.PadTab
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.Track
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.BpmFormat
import com.example.djremixpro.core.util.MixMath
import com.example.djremixpro.core.util.TextUtils2
import com.example.djremixpro.core.util.TimeFormat
import com.example.djremixpro.core.util.resultOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Simulated two-deck mixer (Mixer.dc.html, D-07, D-11…D-22). There is no audio: a 1 s ticker advances the play
 * position of each playing deck (speed 1 + pitch/100) and the REC counter. The ticker only runs while a deck plays
 * or REC is on, so `advanceUntilIdle()` terminates in tests.
 */
class MixerViewModel(
    savedStateHandle: SavedStateHandle,
    private val trackRepository: TrackRepository,
    private val recordingRepository: RecordingRepository,
    private val settingsRepository: SettingsRepository,
    private val mixSessionRepository: MixSessionRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private data class DeckModel(
        val id: DeckId,
        val track: Track? = null,
        val playing: Boolean = false,
        val sync: Boolean = false,
        val scratch: Boolean = true,
        val touching: Boolean = false,
        val mode: DeckMode = DeckMode.JOG,
        val padTab: PadTab = PadTab.CUE,
        val manualPitch: Float = 0f,
        val positionSec: Float = 0f,
        val cuePoints: List<Float?> = DEFAULT_CUE_POINTS,
        val loopPad: Int? = null,
        val loopStartSec: Float = 0f,
        val fxOn: Set<Int> = emptySet(),
        val samplerOn: Set<Int> = emptySet(),
        val eq: Map<EqBand, Float> = EqBand.entries.associateWith { EQ_CENTER },
        val kills: Set<EqBand> = emptySet(),
        val volume: Float,
    ) {
        val loaded: Boolean get() = track != null
        val durationSec: Int get() = track?.durationSec ?: 0
    }

    private data class FxModel(val deck: DeckId, val index: Int)

    private data class SaveModel(val durationSec: Int, val name: String, val format: RecordFormat, val qualityKbps: Int)

    private data class Model(
        val a: DeckModel = DeckModel(DeckId.A, volume = 0.86f),
        val b: DeckModel = DeckModel(DeckId.B, volume = 0.72f),
        val crossfader: Float = 0.5f,
        val recording: Boolean = false,
        val recElapsedSec: Int = 0,
        val libraryTarget: DeckId? = null,
        val fx: FxModel? = null,
        val fxX: Float = FX_DEFAULT_X,
        val fxY: Float = FX_DEFAULT_Y,
        val fxBeatLength: Int = FX_DEFAULT_BEAT_LENGTH,
        val save: SaveModel? = null,
        val coach: Boolean = false,
        val settings: AppSettings = AppSettings(),
    ) {
        fun deck(id: DeckId) = if (id == DeckId.A) a else b
        fun other(id: DeckId) = if (id == DeckId.A) b else a
        fun withDeck(id: DeckId, transform: (DeckModel) -> DeckModel) =
            if (id == DeckId.A) copy(a = transform(a)) else copy(b = transform(b))
    }

    private var model = Model()
    private val _uiState = MutableStateFlow(render(model))
    val uiState: StateFlow<MixerUiState> = _uiState.asStateFlow()

    private val _events = Channel<MixerEvent>(Channel.BUFFERED)
    val events: Flow<MixerEvent> = _events.receiveAsFlow()

    private var ticker: Job? = null
    private var settingsApplied = false

    init {
        val entry = savedStateHandle.get<String>(MixerArgs.ENTRY)
            ?.let { name -> MixerEntry.entries.firstOrNull { it.name == name } } ?: MixerEntry.DEFAULT
        val lessonId = savedStateHandle.get<Int>(MixerArgs.LESSON_ID) ?: -1
        startWith(entry, lessonId)

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val first = !settingsApplied
                settingsApplied = true
                update { m ->
                    val range = settings.pitchRangePct.toFloat()
                    fun adjust(d: DeckModel) = d.copy(
                        mode = if (first) settings.defaultJogMode else d.mode,
                        manualPitch = d.manualPitch.coerceIn(-range, range),
                    )
                    m.copy(settings = settings, a = adjust(m.a), b = adjust(m.b))
                }
            }
        }
    }

    private fun startWith(entry: MixerEntry, lessonId: Int) {
        val session = mixSessionRepository.session.value
        val (sampleA, sampleB) = trackRepository.samples
        when {
            entry == MixerEntry.SAMPLES -> {
                load(DeckId.A, sampleA)
                load(DeckId.B, sampleB)
            }
            entry == MixerEntry.LESSON && lessonId == SYNC_LESSON_ID -> {
                // Lesson "Dùng Sync": both decks loaded, A playing, B not synced, coach on (D-18).
                load(DeckId.A, session?.deckA ?: sampleA)
                load(DeckId.B, session?.deckB ?: sampleB)
                update { m -> m.copy(a = m.a.copy(playing = true), b = m.b.copy(sync = false), coach = true) }
            }
            else -> update { m ->
                m.copy(a = m.a.copy(track = session?.deckA), b = m.b.copy(track = session?.deckB))
            }
        }
    }

    // region deck

    fun onPlayToggle(deck: DeckId) = updateLoadedDeck(deck) { d ->
        val atEnd = d.positionSec >= d.durationSec
        d.copy(playing = !d.playing, positionSec = if (!d.playing && atEnd) 0f else d.positionSec)
    }

    /** D-19: stop and return to the cue point 0:00. */
    fun onCue(deck: DeckId) = updateLoadedDeck(deck) { it.copy(playing = false, positionSec = 0f, loopPad = null) }

    fun onSyncToggle(deck: DeckId) = updateLoadedDeck(deck) { it.copy(sync = !it.sync) }

    fun onScratchToggle(deck: DeckId) = updateDeck(deck) { it.copy(scratch = !it.scratch) }

    fun onModeChange(deck: DeckId, mode: DeckMode) = updateDeck(deck) { it.copy(mode = mode) }

    fun onPadTabSelected(deck: DeckId, tab: PadTab) = updateDeck(deck) { it.copy(padTab = tab) }

    /** D-20 pad behaviour for the deck's current tab. */
    fun onPadTapped(deck: DeckId, index: Int) {
        if (index !in 0 until PAD_COUNT) return
        updateDeck(deck) { d ->
            when (d.padTab) {
                PadTab.CUE -> {
                    if (!d.loaded) return@updateDeck d
                    val point = d.cuePoints[index]
                    if (point == null) {
                        d.copy(cuePoints = d.cuePoints.toMutableList().also { it[index] = d.positionSec })
                    } else {
                        d.copy(positionSec = point.coerceAtMost(d.durationSec.toFloat()), loopPad = null)
                    }
                }
                PadTab.LOOP -> {
                    if (!d.loaded) return@updateDeck d
                    if (d.loopPad == index) d.copy(loopPad = null) else d.copy(loopPad = index, loopStartSec = d.positionSec)
                }
                PadTab.FX -> d.copy(fxOn = d.fxOn.toggle(index))
                PadTab.SAMPLER -> d.copy(samplerOn = d.samplerOn.toggle(index))
            }
        }
    }

    /** On the FX tab a long press opens the FX sheet of that effect; other tabs ignore it. */
    fun onPadLongPressed(deck: DeckId, index: Int) {
        if (index !in 0 until PAD_COUNT || model.deck(deck).padTab != PadTab.FX) return
        update { it.copy(fx = FxModel(deck, index)) }
    }

    fun onJogTouch(deck: DeckId, touching: Boolean) {
        if (touching && !model.deck(deck).loaded) return
        updateDeck(deck) { it.copy(touching = touching) }
    }

    /** Tapping an empty platter opens the library panel for that deck (Mixer:452). */
    fun onJogTapped(deck: DeckId) {
        if (model.deck(deck).loaded) return
        update { it.copy(libraryTarget = deck) }
    }

    /** D-19: while touching with Scratch on, 360° moves the play position by 1.8 s. */
    fun onJogRotate(deck: DeckId, deltaDegrees: Float) = updateLoadedDeck(deck) { d ->
        if (!d.touching || !d.scratch) return@updateLoadedDeck d
        val pos = (d.positionSec + deltaDegrees / 360f * SPIN_SECONDS_PER_TURN).coerceIn(0f, d.durationSec.toFloat())
        d.copy(positionSec = pos)
    }

    /** D-19: manual pitch within ±range; moving the fader turns Sync off. */
    fun onPitchChanged(deck: DeckId, pitchPct: Float) {
        val range = model.settings.pitchRangePct.toFloat()
        updateDeck(deck) { it.copy(manualPitch = pitchPct.coerceIn(-range, range), sync = false) }
    }

    fun onOverviewSeek(deck: DeckId, fraction: Float) = updateLoadedDeck(deck) { d ->
        d.copy(positionSec = MixMath.clamp01(fraction) * d.durationSec, loopPad = null)
    }

    // endregion

    // region channels

    fun onVolumeChanged(deck: DeckId, value: Float) = updateDeck(deck) { it.copy(volume = MixMath.clamp01(value)) }

    fun onEqChanged(deck: DeckId, band: EqBand, value: Float) = updateDeck(deck) {
        it.copy(eq = it.eq + (band to MixMath.clamp01(value)), kills = it.kills - band)
    }

    fun onEqReset(deck: DeckId, band: EqBand) = updateDeck(deck) {
        it.copy(eq = it.eq + (band to EQ_CENTER), kills = it.kills - band)
    }

    fun onEqKill(deck: DeckId, band: EqBand) = updateDeck(deck) { it.copy(kills = it.kills.toggle(band)) }

    fun onCrossfaderChanged(value: Float) = update { it.copy(crossfader = MixMath.clamp01(value)) }

    fun onCrossfaderReset() = update { it.copy(crossfader = 0.5f) }

    // endregion

    // region top bar & overlays

    fun onBack() {
        _events.trySend(MixerEvent.Close)
    }

    /**
     * The prototype wires this button to `openLib` (Mixer:494, D-19): opens the library panel for the first empty deck,
     * otherwise for deck B, so a loaded deck can still be replaced.
     */
    fun onQuickSettings() = update { m -> m.copy(libraryTarget = if (m.a.loaded) DeckId.B else DeckId.A) }

    /** D-12: first tap starts counting; second tap stops, resets the counter and opens the save dialog. */
    fun onRecToggle() {
        val m = model
        if (!m.recording) {
            update { it.copy(recording = true, recElapsedSec = 0) }
            return
        }
        val name = "Mix " + TimeFormat.mixStamp(LocalDateTime.now(clock))
        update {
            it.copy(
                recording = false,
                recElapsedSec = 0,
                save = SaveModel(m.recElapsedSec, name, it.settings.recordFormat, it.settings.recordQualityKbps),
            )
        }
    }

    fun onLibraryClose() = update { it.copy(libraryTarget = null) }

    fun onLibraryLoad(trackId: String, deck: DeckId) {
        val track = trackRepository.findById(trackId)
        if (track == null) {
            toastError()
            return
        }
        load(deck, track)
        update { it.copy(libraryTarget = null) }
    }

    fun onFxChanged(x: Float, y: Float) = update { it.copy(fxX = MixMath.clamp01(x), fxY = MixMath.clamp01(y)) }

    fun onFxToggle() {
        val fx = model.fx ?: return
        updateDeck(fx.deck) { it.copy(fxOn = it.fxOn.toggle(fx.index)) }
    }

    fun onFxBeatLengthSelected(index: Int) {
        if (index !in FX_BEAT_LENGTHS.indices) return
        update { it.copy(fxBeatLength = index) }
    }

    fun onFxReset() = update { it.copy(fxX = FX_DEFAULT_X, fxY = FX_DEFAULT_Y, fxBeatLength = FX_DEFAULT_BEAT_LENGTH) }

    fun onFxClose() = update { it.copy(fx = null) }

    fun onSaveNameChanged(name: String) = update { m -> m.copy(save = m.save?.copy(name = name)) }

    fun onSaveFormatSelected(format: RecordFormat) = update { m -> m.copy(save = m.save?.copy(format = format)) }

    fun onSaveConfirmed() {
        val save = model.save ?: return
        val name = save.name.trim().ifEmpty { "Mix " + TimeFormat.mixStamp(LocalDateTime.now(clock)) }
        viewModelScope.launch {
            resultOf { recordingRepository.add(name, save.durationSec, save.format).getOrThrow() }
                .onSuccess {
                    update { it.copy(save = null) }
                    toast(UiText.Res(R.string.toast_recording_saved))
                }
                .onFailure { toastError() }
        }
    }

    fun onSaveDiscarded() = update { it.copy(save = null) }

    /** D-18: "Tiếp" and "Bỏ qua" both close the coach. */
    fun onCoachNext() = update { it.copy(coach = false) }

    fun onCoachSkip() = update { it.copy(coach = false) }

    // endregion

    private fun load(deck: DeckId, track: Track) {
        mixSessionRepository.loadTrack(deck, track)
        updateDeck(deck) {
            it.copy(track = track, playing = false, touching = false, positionSec = 0f, loopPad = null)
        }
    }

    private fun update(transform: (Model) -> Model) {
        model = transform(model)
        _uiState.value = render(model)
        ensureTicker()
    }

    private fun updateDeck(deck: DeckId, transform: (DeckModel) -> DeckModel) = update { it.withDeck(deck, transform) }

    private fun updateLoadedDeck(deck: DeckId, transform: (DeckModel) -> DeckModel) {
        if (!model.deck(deck).loaded) return
        updateDeck(deck, transform)
    }

    private fun toast(text: UiText, tone: ToastTone = ToastTone.SUCCESS) {
        _events.trySend(MixerEvent.ShowToast(ToastMessage(text, tone)))
    }

    private fun toastError() = toast(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR)

    // region timer

    private fun needsTick(m: Model) = m.recording || (m.a.playing && m.a.loaded) || (m.b.playing && m.b.loaded)

    private fun ensureTicker() {
        if (!needsTick(model) || ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (needsTick(model)) {
                delay(TICK_MS)
                update(::tick)
            }
        }
    }

    private fun tick(m: Model): Model {
        fun advance(d: DeckModel, other: DeckModel): DeckModel {
            if (!d.playing || !d.loaded || (d.touching && d.scratch)) return d
            val pitch = pitchOf(d, other)
            var pos = d.positionSec + (1f + pitch / 100f) * (TICK_MS / 1000f)
            val loopPad = d.loopPad
            if (loopPad != null) {
                val loopLen = LOOP_BEATS[loopPad] * 60f / MixMath.effectiveBpm(bpmOf(d.track), pitch)
                if (loopLen > 0f && pos >= d.loopStartSec + loopLen) {
                    pos = d.loopStartSec + (pos - d.loopStartSec) % loopLen
                }
            }
            val duration = d.durationSec.toFloat()
            return if (pos >= duration) d.copy(positionSec = duration, playing = false, loopPad = null)
            else d.copy(positionSec = pos)
        }
        return m.copy(
            a = advance(m.a, m.b),
            b = advance(m.b, m.a),
            recElapsedSec = if (m.recording) m.recElapsedSec + 1 else m.recElapsedSec,
        )
    }

    // endregion

    // region render

    private fun render(m: Model): MixerUiState = MixerUiState(
        deckA = renderDeck(m.a, m.b, m),
        deckB = renderDeck(m.b, m.a, m),
        channels = ChannelsUiState(
            crossfader = m.crossfader,
            volumeA = m.a.volume,
            volumeB = m.b.volume,
            eqA = knobs(m.a),
            eqB = knobs(m.b),
            vuActiveA = m.a.loaded && m.a.playing && m.a.volume > 0f,
            vuActiveB = m.b.loaded && m.b.playing && m.b.volume > 0f,
        ),
        rec = RecUiState(
            isRecording = m.recording,
            elapsedSec = m.recElapsedSec,
            label = if (m.recording) TimeFormat.mmss(m.recElapsedSec) else REC_LABEL,
        ),
        libraryPanel = m.libraryTarget?.let { LibraryPanelUi(it, libraryRows(m, it)) },
        fxSheet = m.fx?.let { fx ->
            val (time, amount) = MixMath.fxReadout(m.fxX, m.fxY)
            FxSheetUi(
                deck = fx.deck,
                fxName = FX_NAMES[fx.index],
                enabled = fx.index in m.deck(fx.deck).fxOn,
                x = m.fxX,
                y = m.fxY,
                readout = "$time beat · $amount%",
                wetDry = FX_WET_DRY,
                beatLengths = FX_BEAT_LENGTHS,
                selectedBeatLength = m.fxBeatLength,
            )
        },
        saveDialog = m.save?.let {
            SaveDialogUi(
                durationLabel = TimeFormat.mmss(it.durationSec),
                name = it.name,
                format = it.format,
                qualityKbps = it.qualityKbps,
                showWavNote = it.format == RecordFormat.WAV,
            )
        },
        coach = if (m.coach) {
            val done = m.b.sync
            CoachUi(
                step = COACH_STEP,
                totalSteps = COACH_TOTAL_STEPS,
                isDone = done,
                title = UiText.Res(if (done) R.string.coach_sync_done_title else R.string.coach_sync_todo_title),
                body = UiText.Res(if (done) R.string.coach_sync_done_body else R.string.coach_sync_todo_body),
                target = CoachTarget.SYNC_B,
            )
        } else null,
        hapticsEnabled = m.settings.haptic,
        keepScreenOn = m.settings.keepScreenOn,
    )

    private fun renderDeck(d: DeckModel, other: DeckModel, m: Model): DeckUiState {
        val track = d.track
        val range = m.settings.pitchRangePct
        val pitch = pitchOf(d, other)
        val waveSeed = if (d.id == DeckId.A) WAVE_SEED_A else WAVE_SEED_B
        val pads = pads(d, m.fx)
        if (track == null) {
            return DeckUiState(
                id = d.id, isSync = d.sync, isScratch = d.scratch, mode = d.mode, padTab = d.padTab, pads = pads,
                pitchPct = pitch, pitchRangePct = range, pitchLabel = MixMath.pitchLabel(pitch),
                isPitchShifted = abs(pitch) > PITCH_EPSILON, pitchPosition = MixMath.pitchPosition(pitch, range),
                spinPeriodSec = MixMath.spinPeriodSec(pitch), waveSeed = waveSeed,
            )
        }
        val bpm = MixMath.effectiveBpm(bpmOf(track), pitch)
        val duration = track.durationSec
        return DeckUiState(
            id = d.id,
            track = DeckTrackUi(track.id, track.title, track.artist, TextUtils2.initials(track.title), bpmOf(track)),
            isPlaying = d.playing,
            isSync = d.sync,
            isScratch = d.scratch,
            isTouching = d.touching,
            mode = d.mode,
            padTab = d.padTab,
            pads = pads,
            pitchPct = pitch,
            pitchRangePct = range,
            pitchLabel = MixMath.pitchLabel(pitch),
            isPitchShifted = abs(pitch) > PITCH_EPSILON,
            pitchPosition = MixMath.pitchPosition(pitch, range),
            bpmLabel = BpmFormat.precise(bpm),
            remainingLabel = TimeFormat.remaining(ceil(duration - d.positionSec).toInt().coerceAtLeast(0)),
            spinPeriodSec = MixMath.spinPeriodSec(pitch),
            isSpinning = d.playing && !(d.touching && d.scratch),
            waveSeed = waveSeed,
            waveBpm = bpm,
            progress = if (duration > 0) MixMath.clamp01(d.positionSec / duration) else 0f,
            loopBeats = d.loopPad?.let { LOOP_BEATS[it] },
            positionSec = d.positionSec,
        )
    }

    private fun pads(d: DeckModel, fx: FxModel?): List<PadUi> = when (d.padTab) {
        PadTab.CUE -> d.cuePoints.mapIndexed { i, point ->
            PadUi(
                label = UiText.Raw((i + 1).toString()),
                sub = if (point != null) UiText.Raw(TimeFormat.mss(point.toInt())) else UiText.Res(R.string.mixer_pad_empty),
                state = if (point != null) PadState.ON else PadState.OFF,
                style = PadStyle.NUMERIC,
            )
        }
        PadTab.LOOP -> LOOP_LABELS.mapIndexed { i, label ->
            val inOut = i == LOOP_LABELS.lastIndex
            PadUi(
                label = UiText.Raw(label),
                sub = if (inOut) null else UiText.Res(R.string.mixer_pad_beat),
                state = if (d.loopPad == i) PadState.ON else PadState.OFF,
                style = if (inOut) PadStyle.NUMERIC_SMALL else PadStyle.NUMERIC,
            )
        }
        PadTab.FX -> FX_NAMES.mapIndexed { i, name ->
            val state = when {
                fx != null && fx.deck == d.id && fx.index == i -> PadState.HELD
                i in d.fxOn -> PadState.ON
                else -> PadState.OFF
            }
            PadUi(UiText.Raw(name), null, state, PadStyle.TEXT)
        }
        PadTab.SAMPLER -> SAMPLER_NAMES.mapIndexed { i, name ->
            PadUi(UiText.Raw(name), null, if (i in d.samplerOn) PadState.NEUTRAL_ON else PadState.OFF, PadStyle.TEXT)
        }
    }

    private fun knobs(d: DeckModel): List<KnobUi> = EqBand.entries.map { band ->
        val kill = band in d.kills
        val value = if (kill) 0f else d.eq[band] ?: EQ_CENTER
        KnobUi(band, value, kill, MixMath.eqLabel(band, value, kill))
    }

    /** Library panel (Mixer:496-500): songs by BPM, "Hợp nhịp A" when loading deck B within 3% of deck A. */
    private fun libraryRows(m: Model, target: DeckId): List<LibraryPanelRow> {
        val reference = m.a.track?.let { MixMath.effectiveBpm(bpmOf(it), pitchOf(m.a, m.b)) }
        return trackRepository.tracks.value
            .sortedBy { it.bpm ?: Float.MAX_VALUE }
            .map { t ->
                LibraryPanelRow(
                    trackId = t.id,
                    title = t.title,
                    artist = t.artist,
                    durationLabel = TimeFormat.mmss(t.durationSec),
                    bpmLabel = BpmFormat.short(t.bpm),
                    initials = TextUtils2.initials(t.title),
                    coverColor = t.coverColor,
                    isBpmMatch = target == DeckId.B && MixMath.isBpmMatch(t.bpm, reference),
                )
            }
    }

    /** Sync uses the other deck's original BPM (Mixer:437); otherwise the manual pitch. */
    private fun pitchOf(d: DeckModel, other: DeckModel): Float {
        val own = d.track
        val ref = other.track
        return if (d.sync && own != null && ref != null) MixMath.syncPitchPct(bpmOf(own), bpmOf(ref)) else d.manualPitch
    }

    private fun bpmOf(track: Track?): Float = track?.bpm ?: DEFAULT_BPM

    // endregion

    companion object {
        const val TICK_MS = 1_000L
        const val SYNC_LESSON_ID = 2
        private const val PAD_COUNT = 8
        private const val EQ_CENTER = 0.5f
        private const val DEFAULT_BPM = 120f
        private const val PITCH_EPSILON = 0.05f
        private const val SPIN_SECONDS_PER_TURN = 1.8f
        private const val WAVE_SEED_A = 7
        private const val WAVE_SEED_B = 29
        private const val REC_LABEL = "REC"
        private const val COACH_STEP = 3
        private const val COACH_TOTAL_STEPS = 5
        private const val FX_DEFAULT_X = 0.62f
        private const val FX_DEFAULT_Y = 0.3f
        private const val FX_DEFAULT_BEAT_LENGTH = 2
        private const val FX_WET_DRY = 0.6f

        /** Cue pads 1–3 preset to 0:32 / 1:04 / 2:08 (D-20). */
        private val DEFAULT_CUE_POINTS: List<Float?> = listOf(32f, 64f, 128f, null, null, null, null, null)
        private val LOOP_LABELS = listOf("1/4", "1/2", "1", "2", "4", "8", "16", "In/Out")

        /** Loop length in beats per loop pad; "In/Out" loops 4 beats. */
        private val LOOP_BEATS = listOf(0.25f, 0.5f, 1f, 2f, 4f, 8f, 16f, 4f)
        private val FX_NAMES = listOf("Echo", "Flanger", "Bitcrush", "Gate", "Reverb", "Phaser", "Roll", "Brake")
        private val SAMPLER_NAMES = listOf("Còi hơi", "Scratch", "Drop", "Vỗ tay", "Siren", "Đám đông", "Laser", "Rewind")
        private val FX_BEAT_LENGTHS = listOf("1/4", "1/2", "1", "2", "4")

        private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as DJRemixProApp).container
                MixerViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    trackRepository = c.trackRepository,
                    recordingRepository = c.recordingRepository,
                    settingsRepository = c.settingsRepository,
                    mixSessionRepository = c.mixSessionRepository,
                    clock = Clock.systemDefaultZone(),
                )
            }
        }
    }
}
