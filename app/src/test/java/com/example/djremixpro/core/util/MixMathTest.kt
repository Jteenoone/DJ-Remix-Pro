package com.example.djremixpro.core.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** U09–U19, M19. Công thức Mixer:437–488 và D-19, D-21, D-22. */
class MixMathTest {

    private val minus = '−'
    private lateinit var savedLocale: Locale

    @Before
    fun saveLocale() {
        savedLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(savedLocale)
    }

    // U09
    @Test
    fun syncPitch_deckBFollowsA_isNegative() {
        assertEquals(-3.125f, MixMath.syncPitchPct(ownBpm = 128f, otherBpm = 124f), 1e-4f)
    }

    @Test
    fun syncPitch_deckAFollowsB_isPositive() {
        assertEquals(3.2258f, MixMath.syncPitchPct(ownBpm = 124f, otherBpm = 128f), 1e-3f)
    }

    @Test
    fun syncPitch_equalBpm_isZero() {
        assertEquals(0f, MixMath.syncPitchPct(126f, 126f), 1e-6f)
    }

    // U10
    @Test
    fun effectiveBpm_afterSync_matchesOtherDeck() {
        assertEquals(124f, MixMath.effectiveBpm(128f, -3.125f), 1e-3f)
        assertEquals(128f, MixMath.effectiveBpm(124f, MixMath.syncPitchPct(124f, 128f)), 1e-3f)
        assertEquals(124f, MixMath.effectiveBpm(124f, 0f), 1e-6f)
    }

    // U11
    @Test
    fun pitchLabel_zeroAndDeadZone_showPlusZero() {
        assertEquals("+0.0%", MixMath.pitchLabel(0f))
        assertEquals("+0.0%", MixMath.pitchLabel(0.04f))
        assertEquals("+0.0%", MixMath.pitchLabel(-0.04f))
    }

    @Test
    fun pitchLabel_signsAndOneDecimal() {
        assertEquals("+3.2%", MixMath.pitchLabel(3.2258f))
        assertEquals("${minus}3.1%", MixMath.pitchLabel(-3.125f))
        assertEquals("+8.0%", MixMath.pitchLabel(8f))
        assertEquals("${minus}8.0%", MixMath.pitchLabel(-8f))
    }

    @Test
    fun pitchLabel_negative_usesUnicodeMinusNotHyphen() {
        val label = MixMath.pitchLabel(-3.125f)
        assertFalse(label.contains('-'))
        assertEquals(minus, label.first())
    }

    // M19 / D-15
    @Test
    fun pitchLabel_usesDotDecimal_underCommaLocales() {
        for (tag in listOf("vi-VN", "de-DE", "fr-FR", "ar")) {
            Locale.setDefault(Locale.forLanguageTag(tag))
            assertEquals("default=$tag", "+3.2%", MixMath.pitchLabel(3.2258f))
            assertEquals("default=$tag", "${minus}3.1%", MixMath.pitchLabel(-3.125f))
        }
    }

    // U12
    @Test
    fun pitchPosition_centerTopBottom() {
        assertEquals(0.5f, MixMath.pitchPosition(0f, 8), 1e-6f)
        assertEquals(0f, MixMath.pitchPosition(8f, 8), 1e-6f)
        assertEquals(1f, MixMath.pitchPosition(-8f, 8), 1e-6f)
    }

    @Test
    fun pitchPosition_syncExample_matchesDesign() {
        // Mixer:447: 50 − (−3.125)/8·50 = 69.53125 %
        assertEquals(0.6953125f, MixMath.pitchPosition(-3.125f, 8), 1e-5f)
    }

    @Test
    fun pitchPosition_outOfRange_isClamped() {
        assertEquals(0f, MixMath.pitchPosition(20f, 8), 1e-6f)
        assertEquals(1f, MixMath.pitchPosition(-20f, 8), 1e-6f)
    }

    @Test
    fun pitchPosition_widerRange_scales() {
        assertEquals(0.25f, MixMath.pitchPosition(8f, 16), 1e-6f)
        assertEquals(0.46875f, MixMath.pitchPosition(3.125f, 50), 1e-5f)
    }

    // U13
    @Test
    fun spinPeriod_followsPitch() {
        assertEquals(1.8f, MixMath.spinPeriodSec(0f), 1e-6f)
        assertEquals(1.8580645f, MixMath.spinPeriodSec(-3.125f), 1e-4f)
        assertEquals(1.74375f, MixMath.spinPeriodSec(3.2258f), 1e-4f) // 1.8 / 1.032258
        assertEquals(1.745f, MixMath.spinPeriodSec(3.125f), 1e-3f) // Mixer:448 toFixed(3)
    }

    // U14
    @Test
    fun waveScroll_32dpPerBeat() {
        assertEquals(66.13333f, MixMath.waveScrollDpPerSec(124f), 1e-3f)
        assertEquals(64f, MixMath.waveScrollDpPerSec(120f), 1e-4f)
    }

    // U15
    @Test
    fun bpmMatch_withinThreePercent() {
        assertTrue(MixMath.isBpmMatch(126f, 124f))
        assertTrue(MixMath.isBpmMatch(122f, 124f))
        assertTrue(MixMath.isBpmMatch(125f, 124f))
        assertTrue(MixMath.isBpmMatch(124f, 124f))
    }

    @Test
    fun bpmMatch_beyondThreePercent_isFalse() {
        assertFalse(MixMath.isBpmMatch(128f, 124f)) // +3.23 %
        assertFalse(MixMath.isBpmMatch(120f, 124f)) // −3.23 %
        assertFalse(MixMath.isBpmMatch(100f, 124f))
    }

    @Test
    fun bpmMatch_nearBoundary() {
        assertTrue(MixMath.isBpmMatch(102.9f, 100f))
        assertFalse(MixMath.isBpmMatch(103.1f, 100f))
        assertTrue(MixMath.isBpmMatch(97.1f, 100f))
        assertFalse(MixMath.isBpmMatch(96.9f, 100f))
    }

    @Test
    fun bpmMatch_nullBpm_isFalse() {
        assertFalse(MixMath.isBpmMatch(null, 124f))
        assertFalse(MixMath.isBpmMatch(124f, null))
        assertFalse(MixMath.isBpmMatch(null, null))
    }

    // U16
    @Test
    fun clamp01_bounds() {
        assertEquals(0f, MixMath.clamp01(-0.1f), 0f)
        assertEquals(0f, MixMath.clamp01(0f), 0f)
        assertEquals(0.5f, MixMath.clamp01(0.5f), 0f)
        assertEquals(1f, MixMath.clamp01(1f), 0f)
        assertEquals(1f, MixMath.clamp01(1.2f), 0f)
    }

    // U17
    @Test
    fun eqDb_mapping() {
        assertEquals(0f, MixMath.eqDb(0.5f), 1e-5f)
        assertEquals(3.51f, MixMath.eqDb(0.63f), 1e-3f)
        assertEquals(13.5f, MixMath.eqDb(1f), 1e-5f)
        assertEquals(-13.5f, MixMath.eqDb(0f), 1e-5f)
    }

    // U18 — kỳ vọng do Node chạy nguyên văn Mixer:486–488
    @Test
    fun fxReadout_designDefault() {
        assertEquals("1/2" to 70, MixMath.fxReadout(0.62f, 0.3f))
        assertEquals("1/2" to 70, MixMath.fxReadout(0.6f, 0.3f))
    }

    @Test
    fun fxReadout_corners() {
        assertEquals("1/16" to 0, MixMath.fxReadout(0f, 1f))
        assertEquals("1/2" to 50, MixMath.fxReadout(0.5f, 0.5f))
    }

    @Test
    fun fxReadout_xEqualsOne_flooredToLastBucket() {
        // floor(1·6) = 6 → phải kẹp về chỉ số 5 ("1"), không vượt mảng.
        assertEquals("1" to 100, MixMath.fxReadout(1f, 0f))
        assertEquals("1" to 70, MixMath.fxReadout(0.99f, 0.3f))
    }

    @Test
    fun fxReadout_bucketEdges() {
        assertEquals("1/16", MixMath.fxReadout(0.1666f, 0.3f).first)
        assertEquals("1/8", MixMath.fxReadout(0.17f, 0.3f).first)
        assertEquals("1/4", MixMath.fxReadout(0.34f, 0.3f).first)
        assertEquals("3/4", MixMath.fxReadout(0.67f, 0.3f).first)
    }

    @Test
    fun fxReadout_outOfRangeInput_doesNotCrash_andStaysInRange() {
        val (beat, pct) = MixMath.fxReadout(-0.1f, 1.2f)
        assertEquals("1/16", beat)
        assertTrue("pct=$pct", pct in 0..100)
        val (beat2, pct2) = MixMath.fxReadout(1.5f, -0.5f)
        assertEquals("1", beat2)
        assertTrue("pct=$pct2", pct2 in 0..100)
    }

    // U19 — Mixer:481: clamp((x − left − 18)/(width − 36))
    @Test
    fun crossfaderFromTouch_mapsTrackEndsAndCenter() {
        assertEquals(0f, MixMath.crossfaderFromTouch(18f, 0f, 236f, 36f), 1e-6f)
        assertEquals(0.5f, MixMath.crossfaderFromTouch(118f, 0f, 236f, 36f), 1e-6f)
        assertEquals(1f, MixMath.crossfaderFromTouch(218f, 0f, 236f, 36f), 1e-6f)
    }

    @Test
    fun crossfaderFromTouch_respectsLeftOffset() {
        assertEquals(0.5f, MixMath.crossfaderFromTouch(218f, 100f, 236f, 36f), 1e-6f)
    }

    @Test
    fun crossfaderFromTouch_outsideTrack_isClamped() {
        assertEquals(0f, MixMath.crossfaderFromTouch(0f, 0f, 236f, 36f), 0f)
        assertEquals(1f, MixMath.crossfaderFromTouch(500f, 0f, 236f, 36f), 0f)
    }

    @Test
    fun crossfaderFromTouch_degenerateWidth_neverNaN() {
        // View chưa layout (width == knob) không được đẩy NaN vào state.
        val v = MixMath.crossfaderFromTouch(18f, 0f, 36f, 36f)
        assertFalse("v=$v", v.isNaN())
        assertTrue("v=$v", v in 0f..1f)
    }
}
