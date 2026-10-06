package com.animalvision.core

import com.animalvision.core.color.ColorTransform
import com.animalvision.core.color.LutGenerator
import com.animalvision.core.color.Spectrum
import com.animalvision.core.profiles.getProfile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Scientific validation of the color engine (spec §41).
 * The scene-level checks live here on single chips; CpuRendererTest does full scenes.
 */
class ColorTransformTest {

    private fun transform(id: String) = ColorTransform(getProfile(id)!!)

    private fun enc(rgb: DoubleArray): DoubleArray =
        doubleArrayOf(Spectrum.encodeSrgb(rgb[0]), Spectrum.encodeSrgb(rgb[1]), Spectrum.encodeSrgb(rgb[2]))

    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in 0..2) s += (a[i] - b[i]) * (a[i] - b[i])
        return Math.sqrt(s)
    }

    private fun luminance(rgb: DoubleArray): Double =
        0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]

    @Test
    fun `white maps to white for every species (von-Kries neutral point preserved)`() {
        val white = doubleArrayOf(1.0, 1.0, 1.0)
        for (id in listOf("human", "dog", "cat", "horse", "eagle", "bee", "snake")) {
            val out = transform(id).applyLinear(white)
            for (c in out) assertThat(c).isWithin(1e-3).of(1.0)
        }
    }

    @Test
    fun `dichromats collapse red-green hue but keep blue-yellow`() {
        val red = doubleArrayOf(1.0, 0.0, 0.0)
        // Isoluminant green: same CIE luminance as pure red — the standard probe for
        // chromatic discrimination (removes the legitimate luminance contrast a dichromat retains).
        val greenIso = doubleArrayOf(0.0, 0.2126 / 0.7152, 0.0)
        val blue = doubleArrayOf(0.0, 0.0, 1.0)
        val yellow = doubleArrayOf(1.0, 1.0, 0.0)

        val human = transform("human")
        val humanRg = dist(enc(human.applyLinear(red)), enc(human.applyLinear(greenIso)))

        // Chromaticity = (R,G,B normalized by R+G+B) — removes luminance, isolating hue convergence.
        fun chroma(rgb: DoubleArray): DoubleArray {
            val sum = rgb[0] + rgb[1] + rgb[2] + 1e-9
            return doubleArrayOf(rgb[0] / sum, rgb[1] / sum, rgb[2] / sum)
        }

        for (id in listOf("dog", "cat", "horse")) {
            val t = transform(id)
            val rgIso = dist(enc(t.applyLinear(red)), enc(t.applyLinear(greenIso)))
            val by = dist(enc(t.applyLinear(blue)), enc(t.applyLinear(yellow)))

            // (1) Hue collapse at isoluminance: distances collapse vs the human reference.
            assertWithMessage("$id isoluminant red-green $rgIso vs human $humanRg")
                .that(rgIso).isLessThan(humanRg * 0.5)

            // (2) Chromaticity seriously converges while it is maximal for humans.
            val dogChroma = dist(chroma(enc(t.applyLinear(red))), chroma(enc(t.applyLinear(greenIso))))
            val humanChroma = dist(chroma(enc(human.applyLinear(red))), chroma(enc(human.applyLinear(greenIso))))
            assertWithMessage("$id chromatic convergence $dogChroma vs human $humanChroma")
                .that(dogChroma).isLessThan(humanChroma * 0.15)

            // (3) Blue-yellow remains the dominant axis (ratio clearly > 1).
            assertWithMessage("$id blue-yellow $by vs red-green $rgIso")
                .that(by).isGreaterThan(rgIso * 1.25)
        }
    }

    @Test
    fun `bees see red as dark - no red receptor (documented behavior)`() {
        val bee = transform("bee")
        val red = bee.applyLinear(doubleArrayOf(1.0, 0.0, 0.0))
        val green = bee.applyLinear(doubleArrayOf(0.0, 1.0, 0.0))
        val blue = bee.applyLinear(doubleArrayOf(0.0, 0.0, 1.0))
        assertThat(luminance(red)).isLessThan(0.5 * luminance(green))
        assertThat(luminance(red)).isLessThan(0.5 * luminance(blue))
    }

    @Test
    fun `bee UV position rises with the blue channel (proxy, documented)`() {
        val bee = transform("bee")
        val low = bee.applyLinear(doubleArrayOf(0.0, 0.0, 0.3))
        val high = bee.applyLinear(doubleArrayOf(0.0, 0.0, 1.0))
        // Red channel carries the UV false-color proxy.
        assertThat(high[0]).isGreaterThan(low[0] + 0.1)
    }

    @Test
    fun `human transform is near-identity`() {
        val human = transform("human")
        for (c in listOf(
            doubleArrayOf(0.1, 0.4, 0.9), doubleArrayOf(0.9, 0.6, 0.2), doubleArrayOf(0.5, 0.5, 0.5),
        )) {
            val out = human.applyLinear(c)
            for (i in 0..2) assertThat(out[i]).isWithin(1e-9).of(c[i])
        }
    }

    @Test
    fun `eagle rendering of a violet input is blue-dominated (violet-shift projection)`() {
        val eagle = transform("eagle")
        val violet = eagle.applyLinear(doubleArrayOf(0.5, 0.2, 0.8))
        assertThat(violet[2]).isGreaterThan(violet[0])
    }

    @Test
    fun `generated LUT agrees with the direct transform`() {
        for (id in listOf("human", "dog", "bee")) {
            val lut = LutGenerator.generate(transform(id), size = 33)
            for (c in listOf(
                doubleArrayOf(0.05, 0.05, 0.05), doubleArrayOf(0.2, 0.7, 0.3), doubleArrayOf(1.0, 0.0, 1.0),
            )) {
                val direct = transform(id).applyLinear(c)
                val sampled = lut.sample(c[0], c[1], c[2])
                com.google.common.truth.Truth.assertWithMessage("$id color ${c.joinToString()} direct=${direct.joinToString()} sampled=${sampled.joinToString()}")
                    .that(Math.abs(sampled[0] - direct[0])).isAtMost(0.02)
                com.google.common.truth.Truth.assertWithMessage("$id color ${c.joinToString()} ch1")
                    .that(Math.abs(sampled[1] - direct[1])).isAtMost(0.02)
                com.google.common.truth.Truth.assertWithMessage("$id color ${c.joinToString()} ch2")
                    .that(Math.abs(sampled[2] - direct[2])).isAtMost(0.02)
            }
        }
    }

    @Test
    fun `LUT alpha is always opaque`() {
        val lut = LutGenerator.generate(transform("horse"), size = 17)
        for (i in 3 until lut.data.size step 4) {
            assertThat(lut.data[i]).isEqualTo(1.0f)
        }
    }

    @Test
    fun `receptor sensitivity curves peak near their specified lambda max`() {
        val sCurve = com.animalvision.core.color.OpsinTemplate.sensitivityCurve(429.0)
        val lCurve = com.animalvision.core.color.OpsinTemplate.sensitivityCurve(555.0)
        val lambda = com.animalvision.core.color.Spectrum.LAMBDA
        fun argmax(a: DoubleArray): Int = a.indices.maxBy { a[it] }
        // 10 nm grid resolution is acceptable
        assertThat(lambda[argmax(sCurve)]).isAtLeast(420.0)
        assertThat(lambda[argmax(sCurve)]).isAtMost(439.0)
        assertThat(lambda[argmax(lCurve)]).isAtLeast(550.0)
        assertThat(lambda[argmax(lCurve)]).isAtMost(560.0)
        assertThat(lambda[argmax(lCurve)]).isGreaterThan(lambda[argmax(sCurve)])
    }
}
