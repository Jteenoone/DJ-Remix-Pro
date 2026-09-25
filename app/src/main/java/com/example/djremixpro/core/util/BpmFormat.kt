package com.example.djremixpro.core.util

import java.util.Locale
import kotlin.math.roundToInt

object BpmFormat {
    const val UNKNOWN = "…"

    /** Badge form: 124f → "124", 124.5f → "124.5", null → "…". */
    fun short(bpm: Float?): String {
        if (bpm == null) return UNKNOWN
        val whole = bpm.roundToInt()
        return if (whole.toFloat() == bpm) whole.toString() else String.format(Locale.US, "%.1f", bpm)
    }

    /** Deck header form with one decimal: "124.0" (Mixer:444). */
    fun precise(bpm: Float): String = String.format(Locale.US, "%.1f", bpm)
}
