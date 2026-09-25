package com.example.djremixpro.core.util

import com.example.djremixpro.core.model.EqBand
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** Mixer formulas ported from Mixer.dc.html (see DESIGNER_REPORT §5.2) plus D-21/D-22. */
object MixMath {
    const val BPM_MATCH_TOLERANCE = 0.03f
    const val EQ_KILL_LABEL = "Kill"
    private val FX_TIME_LABELS = listOf("1/16", "1/8", "1/4", "1/2", "3/4", "1")

    /** Pitch that makes a deck at [ownBpm] match [otherBpm]: (other/own − 1)·100 (Mixer:437). */
    fun syncPitchPct(ownBpm: Float, otherBpm: Float): Float =
        if (ownBpm <= 0f) 0f else (otherBpm / ownBpm - 1f) * 100f

    fun effectiveBpm(bpm: Float, pitchPct: Float): Float = bpm * (1f + pitchPct / 100f)

    /** "+0.0%", "+2.4%", "−3.1%" (U+2212); |pitch| ≤ 0.05 counts as "+". */
    fun pitchLabel(pitchPct: Float): String {
        val sign = if (pitchPct < -0.05f) TimeFormat.MINUS else '+'
        return sign + String.format(Locale.US, "%.1f", abs(pitchPct)) + "%"
    }

    /** Knob position 0..1 from the top: 0.5 − pitch/range·0.5, clamped. Positive pitch moves up. */
    fun pitchPosition(pitchPct: Float, rangePct: Int): Float =
        if (rangePct <= 0) 0.5f else clamp01(0.5f - pitchPct / rangePct * 0.5f)

    /** Seconds per platter turn: 1.8 / (1 + pitch/100). */
    fun spinPeriodSec(pitchPct: Float): Float = 1.8f / (1f + pitchPct / 100f)

    /** Waveform scroll speed, 32 dp per beat: 32·bpm/60. */
    fun waveScrollDpPerSec(bpm: Float): Float = 32f * bpm / 60f

    /** |candidate/reference − 1| ≤ 3% (Mixer:497). False when either side is unknown. */
    fun isBpmMatch(candidateBpm: Float?, referenceBpm: Float?): Boolean {
        if (candidateBpm == null || referenceBpm == null || candidateBpm <= 0f || referenceBpm <= 0f) return false
        return abs(candidateBpm / referenceBpm - 1f) <= BPM_MATCH_TOLERANCE + 1e-6f
    }

    fun clamp01(v: Float): Float = if (v.isNaN()) 0f else v.coerceIn(0f, 1f)

    /** EQ gain in dB: (v − 0.5)·27 (D-21). */
    fun eqDb(v: Float): Float = (v - 0.5f) * 27f

    /**
     * Knob caption (D-21): EQ bands "+3.5 dB" / "−2.0 dB" / "0.0 dB"; FILTER "LPF 40%" below centre,
     * "HPF 28%" above, "0%" at centre; "Kill" when killed.
     */
    fun eqLabel(band: EqBand, v: Float, isKill: Boolean): String {
        if (isKill) return EQ_KILL_LABEL
        if (band == EqBand.FILTER) {
            val pct = (abs(v - 0.5f) / 0.5f * 100f).roundToInt()
            return when {
                pct == 0 -> "0%"
                v < 0.5f -> "LPF $pct%"
                else -> "HPF $pct%"
            }
        }
        val db = eqDb(v)
        val text = String.format(Locale.US, "%.1f", abs(db))
        return when {
            text == "0.0" -> "0.0 dB"
            db > 0f -> "+$text dB"
            else -> "${TimeFormat.MINUS}$text dB"
        }
    }

    /** XY pad readout (Mixer:486-488): time label from x (1/16…1) and amount % from y (0 = top). */
    fun fxReadout(x: Float, y: Float): Pair<String, Int> {
        val index = min(5, floor(clamp01(x) * 6f).toInt())
        return FX_TIME_LABELS[index] to ((1f - clamp01(y)) * 100f).roundToInt()
    }

    /** Crossfader value from a touch x: clamp((x − left − knob/2) / (width − knob)) (Mixer:464). */
    fun crossfaderFromTouch(x: Float, left: Float, width: Float, knobPx: Float): Float {
        val travel = width - knobPx
        if (travel <= 0f) return 0.5f
        return clamp01((x - left - knobPx / 2f) / travel)
    }
}
