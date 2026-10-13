package com.animalvision.core

import com.animalvision.core.color.LutGenerator
import com.animalvision.core.color.ColorTransform
import com.animalvision.core.profiles.getProfile
import com.animalvision.core.reference.CpuRenderer
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Scene-level scientific validation (spec §41): a fixed synthetic scene with red/green/blue/yellow
 * chips, a checkerboard detail zone, and a dim region, rendered through the pipeline's CPU
 * reference. Assertions verify that each animal model behaves per its documented biology —
 * not merely "looks different".
 */
class CpuRendererTest {

    private class Scene(w: Int, h: Int) {
        private val data = IntArray(w * h)
        private val w = w
        private val h = h

        fun fill(x0: Int, y0: Int, x1: Int, y1: Int, argb: Int) {
            for (y in y0 until y1) for (x in x0 until x1) data[y * w + x] = argb
        }

        val raw: IntArray get() = data
    }

    private fun render(id: String, scene: Scene, w: Int = 100, h: Int = 100): IntArray {
        val profile = getProfile(id)!!
        val lut = LutGenerator.generate(ColorTransform(profile), size = 25)
        return CpuRenderer(profile, lut).render(scene.raw, w, h)
    }

    private fun dist(a: DoubleArray, b: DoubleArray): Double {
        var s = 0.0
        for (i in 0..2) s += (a[i] - b[i]) * (a[i] - b[i])
        return Math.sqrt(s)
    }

    private fun lum(a: DoubleArray) = 0.2126 * a[0] + 0.7152 * a[1] + 0.0722 * a[2]

    private fun pxOf(img: IntArray, i: Int) = doubleArrayOf(
        ((img[i] ushr 16) and 0xff) / 255.0,
        ((img[i] ushr 8) and 0xff) / 255.0,
        (img[i] and 0xff) / 255.0,
    )

    private fun chipScene(): Scene {
        val s = Scene(100, 100)
        s.fill(0, 0, 100, 100, 0xff808080.toInt()) // mid-grey background
        s.fill(5, 5, 25, 25, 0xffff0000.toInt())   // red
        s.fill(38, 5, 58, 25, 0xff00ff00.toInt())  // green
        s.fill(71, 5, 91, 25, 0xff0000ff.toInt())  // blue
        s.fill(5, 75, 25, 95, 0xffffff00.toInt())  // yellow
        return s
    }

    @Test
    fun `dichromat render converges red and green but preserves blue-yellow`() {
        val human = render("human", chipScene())
        val dog = render("dog", chipScene())
        val humanRg = dist(pxOf(human, 12 * 100 + 15), pxOf(human, 12 * 100 + 48))
        val dogRg = dist(pxOf(dog, 12 * 100 + 15), pxOf(dog, 12 * 100 + 48))
        val dogBlueYellow = dist(pxOf(dog, 12 * 100 + 81), pxOf(dog, 80 * 100 + 15))
        assertThat(dogRg).isLessThan(humanRg * 0.45)
        assertThat(dogBlueYellow).isGreaterThan(dogRg * 1.5)
    }

    @Test
    fun `bee render makes the red chip the darkest (no red receptor)`() {
        val out = render("bee", chipScene())
        val red = lum(pxOf(out, 12 * 100 + 15))
        val blue = lum(pxOf(out, 12 * 100 + 81))
        val green = lum(pxOf(out, 12 * 100 + 48))
        assertThat(red).isLessThan(blue * 0.7)
        assertThat(red).isLessThan(green * 0.7)
    }

    @Test
    fun `horse render hides the lower-center blind-spot region`() {
        val out = render("horse", chipScene())
        val center = lum(pxOf(out, 50 * 100 + 50))
        val lowerCenter = lum(pxOf(out, 95 * 100 + 50))
        assertThat(lowerCenter).isLessThan(center * 0.5)
    }

    @Test
    fun `snake render is a colored thermal visualization on a bright neutral`() {
        // A dark mid-gray lands in the cold (<0.25) thermal bin and legitimately renders
        // near-black — correct thermal behavior. A bright neutral reads as mid/warm temperature.
        val s = Scene(40, 40)
        s.fill(0, 0, 40, 40, 0xffcccccc.toInt())
        val out = render("snake", s, 40, 40)
        val px = out[20 * 40 + 20]
        val r = (px ushr 16) and 0xff; val g = (px ushr 8) and 0xff; val b = px and 0xff
        val maxDelta = Math.max(Math.abs(r - g), Math.max(Math.abs(r - b), Math.abs(g - b)))
        assertWithMessage("thermal pixel r=$r g=$g b=$b")
            .that(maxDelta.toDouble()).isGreaterThan(15.0)
    }

    @Test
    fun `human render keeps the red chip clearly red (baseline sanity)`() {
        val out = render("human", chipScene())
        val px = out[12 * 100 + 15]
        val r = (px ushr 16) and 0xff; val g = (px ushr 8) and 0xff; val b = px and 0xff
        assertThat(r).isGreaterThan(g)
        assertThat(r).isGreaterThan(b)
        assertThat(b.toDouble()).isLessThan(40.0)
    }

    @Test
    fun `low-acuity species blur fine detail even at the centre of view`() {
        // 2px checkerboard filling the frame; measure contrast in the central binocular zone.
        val s = Scene(100, 100)
        for (y in 0 until 100) for (x in 0 until 100) {
            s.fill(x, y, x + 1, y + 1, if (((x / 2) + (y / 2)) % 2 == 0) 0xffffffff.toInt() else 0xff000000.toInt())
        }
        fun centreContrast(id: String): Double {
            val out = render(id, s)
            var lo = 1.0; var hi = 0.0
            for (y in 45 until 55) for (x in 45 until 55) {
                val l = lum(pxOf(out, y * 100 + x))
                lo = Math.min(lo, l); hi = Math.max(hi, l)
            }
            return hi - lo
        }
        assertThat(centreContrast("bee")).isLessThan(centreContrast("human") * 0.2)
    }

    @Test
    fun `polarization species hatch bright glare and others do not`() {
        val s = Scene(40, 40)
        s.fill(0, 0, 40, 40, 0xfff4f4f0.toInt()) // bright, nearly neutral glare
        fun rowSpread(id: String): Int {
            val out = render(id, s, 40, 40)
            val row = 20
            val reds = (0 until 40).map { (out[row * 40 + it] ushr 16) and 0xff }
            return reds.max() - reds.min()
        }
        assertThat(rowSpread("octopus")).isGreaterThan(10)
        assertThat(rowSpread("human")).isAtMost(2)
    }

    @Test
    fun `all renders are deterministic`() {
        val a = render("cat", chipScene())
        val b = render("cat", chipScene())
        assertThat(a.contentEquals(b)).isTrue()
    }
}
