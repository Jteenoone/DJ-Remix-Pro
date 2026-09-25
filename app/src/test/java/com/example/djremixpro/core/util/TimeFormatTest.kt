package com.example.djremixpro.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** U01–U03. Kỳ vọng theo `mm(n)` Mixer:484 và dữ liệu App:314. */
class TimeFormatTest {

    @Test
    fun mmss_padsMinutesAndSeconds() {
        assertEquals("00:00", TimeFormat.mmss(0))
        assertEquals("00:05", TimeFormat.mmss(5))
        assertEquals("03:42", TimeFormat.mmss(222))
    }

    @Test
    fun mmss_rollsOverFrom59To60Seconds() {
        assertEquals("00:59", TimeFormat.mmss(59))
        assertEquals("01:00", TimeFormat.mmss(60))
        assertEquals("01:01", TimeFormat.mmss(61))
    }

    @Test
    fun mmss_beyondOneHour_keepsCountingMinutes() {
        assertEquals("59:59", TimeFormat.mmss(3599))
        assertEquals("60:00", TimeFormat.mmss(3600))
        assertEquals("62:05", TimeFormat.mmss(3725))
    }

    @Test
    fun remaining_usesUnicodeMinusSign() {
        val label = TimeFormat.remaining(151)
        assertEquals("−02:31", label)
        assertEquals('−', label.first())
        assertEquals("−00:00", TimeFormat.remaining(0))
    }

    @Test
    fun date_formatsAsDayMonthYear() {
        assertEquals("25/09/2026", TimeFormat.date(LocalDate.of(2026, 9, 25)))
        assertEquals("05/01/2026", TimeFormat.date(LocalDate.of(2026, 1, 5)))
    }
}
