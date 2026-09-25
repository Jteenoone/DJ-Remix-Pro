package com.example.djremixpro.core.util

/** Park–Miller PRNG of the prototype: `s = (s * 16807) % 2147483647; return s / 2147483647`. */
object ParkMiller {
    private const val MODULUS = 2147483647L

    fun sequence(seed: Int): () -> Float {
        val next = doubles(seed)
        return { next().toFloat() }
    }

    /** Same sequence in double precision, used by the waveform generators to match the JS output. */
    fun doubles(seed: Int): () -> Double {
        var s = seed.toLong()
        return {
            s = (s * 16807L) % MODULUS
            s.toDouble() / MODULUS
        }
    }
}
