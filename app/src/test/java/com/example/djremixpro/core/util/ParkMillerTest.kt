package com.example.djremixpro.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** U06. Kỳ vọng do Node chạy `r()` nguyên văn App:278 / Mixer:399. */
class ParkMillerTest {

    private fun take(seed: Int, n: Int): List<Float> {
        val r = ParkMiller.sequence(seed)
        return List(n) { r() }
    }

    @Test
    fun seed1_matchesPrototype() {
        val v = take(1, 3)
        assertEquals(7.826369e-6f, v[0], 1e-9f)
        assertEquals(0.13153779f, v[1], 1e-6f)
        assertEquals(0.75560532f, v[2], 1e-6f)
    }

    @Test
    fun seed5_matchesPrototype() {
        val v = take(5, 3)
        assertEquals(3.9131846e-5f, v[0], 1e-9f)
        assertEquals(0.65768894f, v[1], 1e-6f)
        assertEquals(0.77802661f, v[2], 1e-6f)
    }

    @Test
    fun seed7_thirdValue_requiresLongMultiplication() {
        // s*16807 vượt Int.MAX_VALUE từ lần thứ hai; nếu nhân Int sẽ tràn và lệch.
        val v = take(7, 3)
        assertEquals(0.92076452f, v[1], 1e-6f)
        assertEquals(0.28923726f, v[2], 1e-6f)
    }

    @Test
    fun longRun_staysInOpenUnitInterval() {
        val r = ParkMiller.sequence(44)
        repeat(10_000) {
            val x = r()
            assertTrue("value $x out of (0,1)", x > 0f && x < 1f)
        }
    }

    @Test
    fun independentSequences_sameSeed_sameValues() {
        assertEquals(take(18, 20), take(18, 20))
    }
}
