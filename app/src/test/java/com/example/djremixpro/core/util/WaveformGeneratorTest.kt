package com.example.djremixpro.core.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** U07–U08. Kỳ vọng do Node chạy nguyên văn `mini` (App:278) và `gen` (Mixer:399–409). */
class WaveformGeneratorTest {

    private val tol = 1e-3f

    @Test
    fun mini_has16Bars() {
        assertEquals(16, WaveformGenerator.mini(5).size)
    }

    @Test
    fun mini_seed5_matchesPrototypeHalfHeights() {
        val expected = floatArrayOf(
            3.00021f, 7.47354f, 9.21058f, 5.56751f, 8.97351f, 3.82221f, 4.84219f, 5.60488f,
            5.05768f, 5.56383f, 5.45481f, 4.17878f, 3.28083f, 3.38367f, 3.83599f, 5.84958f,
        )
        assertArrayEquals(expected, WaveformGenerator.mini(5), tol)
    }

    @Test
    fun mini_seed18_matchesPrototype() {
        val m = WaveformGenerator.mini(18)
        assertEquals(3.00076f, m[0], tol)
        assertEquals(11.16404f, m[5], tol)
        assertEquals(5.34910f, m[15], tol)
    }

    @Test
    fun mini_allSeeds_heightsWithinFormulaBounds() {
        // h = 3 + r·9·(0.6 + 0.4·sin(i/2.5)) ∈ [3, 12]
        for (seed in listOf(5, 18, 31, 44)) {
            WaveformGenerator.mini(seed).forEach { h -> assertTrue("seed $seed h=$h", h in 3f..12f) }
        }
    }

    @Test
    fun mini_isDeterministic() {
        assertArrayEquals(WaveformGenerator.mini(31), WaveformGenerator.mini(31), 0f)
    }

    @Test
    fun deck_has304BarsClampedTo1And8() {
        for (seed in listOf(7, 29)) {
            val d = WaveformGenerator.deck(seed)
            assertEquals(304, d.size)
            d.forEach { h -> assertTrue("seed $seed h=$h", h in 1f..8f) }
        }
    }

    @Test
    fun deck_seed7_matchesPrototype() {
        val d = WaveformGenerator.deck(7)
        val expectedHead = floatArrayOf(
            7.01951f, 5.73015f, 2.04346f, 5.49587f, 3.20127f, 2.94557f, 4.99063f, 4.27595f, 7.90035f, 6.37045f,
        )
        assertArrayEquals(expectedHead, d.copyOfRange(0, 10), tol)
        assertEquals(4.58239f, d[303], tol)
    }

    @Test
    fun deck_seed29_matchesPrototype_includingClampAt1() {
        val d = WaveformGenerator.deck(29)
        val expectedHead = floatArrayOf(
            2.85955f, 2.17405f, 1.0f, 1.69063f, 1.72662f, 1.0f, 1.08400f, 1.0f, 1.98999f, 1.54313f,
        )
        assertArrayEquals(expectedHead, d.copyOfRange(0, 10), tol)
        assertEquals(2.01556f, d[303], tol)
    }
}
