package com.example.djremixpro.feature.mixer

import androidx.annotation.ColorInt
import com.example.djremixpro.core.model.DeckId
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.EqBand
import com.example.djremixpro.core.model.PadTab
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.UiText

/** OFF/ON/HELD per Mixer.dc.html:423-431; NEUTRAL_ON = sampler pad on (text colour 12%). */
enum class PadState { OFF, ON, HELD, NEUTRAL_ON }

/** NUMERIC = Chakra 18, NUMERIC_SMALL = Chakra 14 ("In/Out"), TEXT = Be Vietnam 12. */
enum class PadStyle { NUMERIC, NUMERIC_SMALL, TEXT }

data class PadUi(val label: UiText, val sub: UiText?, val state: PadState, val style: PadStyle)

/** [bpm] is the track's original BPM (120 when the track has none, Mixer.dc.html:498). */
data class DeckTrackUi(val trackId: String, val title: String, val artist: String, val initials: String, val bpm: Float)

/**
 * One deck. [track] null = empty deck. [pads] always holds 8 items (labels of the current [padTab]).
 * [pitchPosition] is the pitch knob position, 0 = top, 1 = bottom. [loopBeats] is non-null while a loop is active.
 * [positionSec] is the current play position in seconds.
 */
data class DeckUiState(
    val id: DeckId,
    val track: DeckTrackUi? = null,
    val isPlaying: Boolean = false,
    val isSync: Boolean = false,
    val isScratch: Boolean = true,
    val isTouching: Boolean = false,
    val mode: DeckMode = DeckMode.JOG,
    val padTab: PadTab = PadTab.CUE,
    val pads: List<PadUi> = emptyList(),
    val pitchPct: Float = 0f,
    val pitchRangePct: Int = 8,
    val pitchLabel: String = "+0.0%",
    val isPitchShifted: Boolean = false,
    val pitchPosition: Float = 0.5f,
    val bpmLabel: String = "—",
    val remainingLabel: String = "−−:−−",
    val spinPeriodSec: Float = 1.8f,
    val isSpinning: Boolean = false,
    val waveSeed: Int = 7,
    val waveBpm: Float = 120f,
    val progress: Float = 0f,
    val loopBeats: Float? = null,
    val positionSec: Float = 0f,
)

/** [value] is 0 while killed. [valueLabel]: "+3.5 dB", "−2.0 dB", "0.0 dB", "Kill", "LPF 40%", "HPF 28%", "0%". */
data class KnobUi(val band: EqBand, val value: Float, val isKill: Boolean, val valueLabel: String)

/** [eqA]/[eqB] are ordered HIGH, MID, LOW, FILTER. */
data class ChannelsUiState(
    val crossfader: Float = 0.5f,
    val volumeA: Float = 0.86f,
    val volumeB: Float = 0.72f,
    val eqA: List<KnobUi> = emptyList(),
    val eqB: List<KnobUi> = emptyList(),
    val vuActiveA: Boolean = false,
    val vuActiveB: Boolean = false,
)

/** [label] is "REC" when idle, otherwise the elapsed time "03:42". */
data class RecUiState(val isRecording: Boolean = false, val elapsedSec: Int = 0, val label: String = "REC")

data class LibraryPanelRow(
    val trackId: String,
    val title: String,
    val artist: String,
    val durationLabel: String,
    val bpmLabel: String,
    val initials: String,
    @param:ColorInt val coverColor: Int,
    val isBpmMatch: Boolean,
)

data class LibraryPanelUi(val target: DeckId, val rows: List<LibraryPanelRow>)

/** [readout] is "1/2 beat · 70%"; [x]/[y] in 0..1 with y = 0 at the top. */
data class FxSheetUi(
    val deck: DeckId,
    val fxName: String,
    val enabled: Boolean,
    val x: Float,
    val y: Float,
    val readout: String,
    val wetDry: Float = 0.6f,
    val beatLengths: List<String>,
    val selectedBeatLength: Int,
)

data class SaveDialogUi(
    val durationLabel: String,
    val name: String,
    val format: RecordFormat,
    val qualityKbps: Int,
    val showWavNote: Boolean,
)

enum class CoachTarget { SYNC_B }

data class CoachUi(
    val step: Int,
    val totalSteps: Int,
    val isDone: Boolean,
    val title: UiText,
    val body: UiText,
    val target: CoachTarget,
)

data class MixerUiState(
    val deckA: DeckUiState = DeckUiState(DeckId.A),
    val deckB: DeckUiState = DeckUiState(DeckId.B, waveSeed = 29),
    val channels: ChannelsUiState = ChannelsUiState(),
    val rec: RecUiState = RecUiState(),
    val libraryPanel: LibraryPanelUi? = null,
    val fxSheet: FxSheetUi? = null,
    val saveDialog: SaveDialogUi? = null,
    val coach: CoachUi? = null,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
)

sealed interface MixerEvent {
    data class ShowToast(val message: ToastMessage) : MixerEvent
    data object Close : MixerEvent
}
