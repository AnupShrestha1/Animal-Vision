package com.animalvision.core.stage

import kotlin.math.pow

/**
 * Low-light / brightness transformation.
 *
 * Scientific basis (docs/SCIENCE.md): rod-dominated, tapetal eyes need ~1/6 the light of humans
 * (cat) and preserve contrast in the shadows. We model:
 *  1. a contrast-preserving shadow lift  out = p + k·p·(1-p)   (monotonic for k ≤ 0.66);
 *  2. luminance-dependent desaturation toward rod vision (chroma fades as luma falls);
 *  3. bounded sensor-like noise (seeded for deterministic tests).
 *
 * Never a plain brightness slider, and never damaging highlight detail.
 */
object LowLight {

    /** Applies the tone curve + desaturation to one LINEAR rgb pixel. Returns planar-independent triple. */
    fun applyPixel(r: Double, g: Double, b: Double, lift: Double, maxDesaturation: Double, k: Double): DoubleArray {
        val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val liftK = lift.coerceIn(0.0, 0.66)
        val dark = 1.0 - luma // 0..1, higher in shadows

        // contrast-preserving shadow lift
        val liftAmount = liftK * 2.0 * (1.0 - luma)
        fun tone(c: Double): Double {
            var v = c + liftAmount * c
            // soft clip highlights so lift never blows detail
            v = v / (1.0 + v * k.coerceIn(0.0, 0.5))
            return v
        }
        val rt = tone(r); val gt = tone(g); val bt = tone(b)

        // rod desaturation: chroma compressed as darkness increases
        val desat = maxDesaturation.coerceIn(0.0, 1.0) * dark * dark
        val grey = 0.2126 * rt + 0.7152 * gt + 0.0722 * bt
        return doubleArrayOf(
            (rt + desat * (grey - rt)).coerceIn(0.0, 1.0),
            (gt + desat * (grey - gt)).coerceIn(0.0, 1.0),
            (bt + desat * (grey - bt)).coerceIn(0.0, 1.0),
        )
    }

    /** Deterministic pseudo-random noise (xorshift64* seeded). */
    class Noise(var seed: Long = -7046029254386353131L) { // 0x9E3779B97F4A7C15 as signed Long
        private fun next(): Double {
            var x = seed
            x = x xor (x shl 13)
            x = x xor (x ushr 7)
            x = x xor (x shl 17)
            seed = x
            return ((x ushr 11) and ((1L shl 52) - 1)).toDouble() / (1L shl 52).toDouble() - 0.5
        }
        fun gaussian(sigma: Double): Double = (next() + next() + next()) * sigma // Irwin-Hall ~ Gaussian
    }
}
