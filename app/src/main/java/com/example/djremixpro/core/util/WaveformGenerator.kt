package com.example.djremixpro.core.util

import kotlin.math.PI
import kotlin.math.sin

object WaveformGenerator {
    const val DECK_BARS = 304
    const val MINI_BARS = 16

    /**
     * Half-bar heights (1..8) of a deck lane, Mixer.dc.html:399-409. Bar i sits at x = i·4, y = 9 − h, 3 wide, 2h tall;
     * the view draws the array twice (1216 dp each) to scroll seamlessly. r() is only drawn for k ≥ 2.
     */
    fun deck(seed: Int): FloatArray {
        val r = ParkMiller.doubles(seed)
        return FloatArray(DECK_BARS) { i ->
            val k = i % 8
            val env = 0.55 + 0.35 * sin(i.toDouble() / DECK_BARS * PI * 6 + seed)
            val base = when (k) {
                0 -> 1.0
                1 -> 0.8
                else -> 0.28 + 0.5 * r()
            }
            (base * env * 9).coerceIn(1.0, 8.0).toFloat()
        }
    }

    /**
     * Half heights of the 16 bars of the recording thumbnail, App.dc.html:278 (viewBox 56×28).
     * Bar i: x = 4 + i·3, y = 14 − h, 2 wide, 2h tall.
     */
    fun mini(seed: Int): FloatArray {
        val r = ParkMiller.doubles(seed)
        return FloatArray(MINI_BARS) { i ->
            (3 + r() * 9 * (0.6 + 0.4 * sin(i / 2.5))).toFloat()
        }
    }
}
