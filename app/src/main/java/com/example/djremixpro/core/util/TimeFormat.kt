package com.example.djremixpro.core.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    /** U+2212, used by the design for remaining time and negative pitch. */
    const val MINUS = '−'

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
    private val mixNameFormatter = DateTimeFormatter.ofPattern("dd-MM HH:mm", Locale.US)

    /** 192 → "03:12" (Mixer.dc.html:467). Negative values count as 0. */
    fun mmss(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    /** 151 → "−02:31". */
    fun remaining(sec: Int): String = "$MINUS${mmss(sec)}"

    /** Cue pad caption without minute padding: 32 → "0:32", 128 → "2:08" (Mixer.dc.html:428). */
    fun mss(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }

    /** "25/09/2026". */
    fun date(d: LocalDate): String = dateFormatter.format(d)

    /** Default recording name part "25-09 21:40" (App.dc.html:314). */
    fun mixStamp(t: LocalDateTime): String = mixNameFormatter.format(t)
}
