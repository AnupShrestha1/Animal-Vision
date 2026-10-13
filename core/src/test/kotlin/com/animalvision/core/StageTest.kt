package com.animalvision.core

import com.animalvision.core.profiles.getProfile
import com.animalvision.core.stage.Acuity
import com.animalvision.core.stage.Fov
import com.animalvision.core.stage.LowLight
import com.animalvision.core.stage.SpecialVision
import com.animalvision.core.stage.ToneMap
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.StageId
import com.animalvision.core.pipeline.Pipeline
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Stage-level behavior tests: acuity kernels, low-light curves, FOV masks, pipeline activation. */
class StageTest {

    @Test
    fun `fov warp separates panoramic prey from foveal predator and leaves human flat`() {
        val horse = Fov.warpCoefficients(getProfile("horse")!!)
        val dog = Fov.warpCoefficients(getProfile("dog")!!)
        val human = Fov.warpCoefficients(getProfile("human")!!)
        val eagle = Fov.warpCoefficients(getProfile("eagle")!!)

        // Human is the standard-lens baseline: no warp at all.
        assertThat(human[0]).isEqualTo(0f)
        assertThat(human[1]).isEqualTo(0f)

        // Wide-field prey get a panoramic (positive) horizontal warp, stronger for wider fields.
        assertThat(horse[0]).isGreaterThan(0f)
        assertThat(dog[0]).isGreaterThan(0f)
        assertThat(horse[0]).isGreaterThan(dog[0]) // 350° > 240°
        // Panoramic warp is horizontal-dominant (wide lateral fields).
        assertThat(horse[1]).isGreaterThan(0f)
        assertThat(horse[1]).isLessThan(horse[0])

        // Foveal predator (eagle) tunnels instead: negative coefficients.
        assertThat(eagle[0]).isLessThan(0f)
        assertThat(eagle[1]).isLessThan(0f)

        // All coefficients stay inside the shader's monotonic bounds window (-4, 2).
        for (k in horse + dog + eagle) {
            assertThat(k).isGreaterThan(-4f)
            assertThat(k).isLessThan(2f)
        }
    }

    @Test
    fun `blur sigma scales inversely with acuity cutoff`() {
        val bee = Acuity.blurSigmaPixels(0.5)
        val dog = Acuity.blurSigmaPixels(8.0)
        val cat = Acuity.blurSigmaPixels(6.0)
        val human = Acuity.blurSigmaPixels(30.0)
        val eagle = Acuity.blurSigmaPixels(140.0)
        assertThat(bee).isGreaterThan(dog * 5.0)
        assertThat(cat).isGreaterThan(dog) // lower acuity (6 cpd) than dog (8 cpd) -> more blur
        assertThat(eagle).isLessThan(0.5)
        assertThat(human).isLessThan(0.5)
    }

    @Test
    fun `gaussian kernel is normalized and falls off`() {
        val kernel = Acuity.gaussianKernel(2.0)
        assertThat(kernel.sum()).isWithin(0.000001f).of(1.0f)
        assertThat(kernel.first()).isLessThan(kernel[kernel.size / 2])
    }

    @Test
    fun `blur reduces high-frequency contrast but preserves mean`() {
        val w = 64; val h = 64
        val src = FloatArray(w * h) { if (it % 2 == 0) 1f else 0f } // checkerboard
        val dst = FloatArray(w * h)
        Acuity.blurChannel(src, dst, w, h, 3.0)
        val meanIn = src.average()
        val meanOut = dst.average()
        assertThat(meanOut).isWithin(0.02).of(meanIn)
        val contrastIn = (src.max()!! - src.min()!!)
        val contrastOut = (dst.max()!! - dst.min()!!)
        assertThat(contrastOut).isLessThan(contrastIn * 0.6f)
    }

    @Test
    fun `low-light shadow lift is monotonic and lifts shadows more than highlights`() {
        val dark = LowLight.applyPixel(0.05, 0.05, 0.05, lift = 0.5, maxDesaturation = 0.0, k = 0.35)
        val mid = LowLight.applyPixel(0.3, 0.3, 0.3, lift = 0.5, maxDesaturation = 0.0, k = 0.35)
        val lit = LowLight.applyPixel(0.8, 0.8, 0.8, lift = 0.5, maxDesaturation = 0.0, k = 0.35)
        assertThat(dark[0]).isGreaterThan(0.05)
        assertThat(lit[0]).isGreaterThan(mid[0])
        assertThat(mid[0]).isGreaterThan(dark[0]) // monotonic
        // relative boost must be larger in the shadows
        assertThat((dark[0] - 0.05) / 0.05).isGreaterThan((lit[0] - 0.8) / 0.8)
    }

    @Test
    fun `rod desaturation pushes saturated colors toward gray as darkness increases`() {
        val brightGreen = LowLight.applyPixel(0.0, 0.9, 0.0, lift = 0.3, maxDesaturation = 0.7, k = 0.3)
        val darkGreen = LowLight.applyPixel(0.0, 0.1, 0.0, lift = 0.3, maxDesaturation = 0.7, k = 0.3)
        fun chroma(c: DoubleArray) = Math.abs(c[0] - c[1]) + Math.abs(c[1] - c[2])
        assertThat(chroma(darkGreen)).isLessThan(chroma(brightGreen))
    }

    @Test
    fun `horse blind-spot wedge is masked near the lower-center frame`() {
        val horse = getProfile("horse")!!
        val mask = Fov.acuityMask(horse, 100, 100)
        val center = mask[50 * 100 + 50]
        val lowerCenter = mask[95 * 100 + 50]
        assertThat(center).isGreaterThan(0.95f)
        assertThat(lowerCenter).isLessThan(0.4f)
    }

    @Test
    fun `eagle foveal boost peaks at frame center and falls off`() {
        val eagle = getProfile("eagle")!!
        val boost = Fov.sharpBoostMask(eagle, 100, 100)
        assertThat(boost[50 * 100 + 50]).isGreaterThan(boost[5 * 100 + 5])
        assertThat(boost[50 * 100 + 50]).isGreaterThan(0f)
    }

    @Test
    fun `pipeline activates the documented stages per animal and tier`() {
        val dog = getProfile("dog")!!
        val high = Pipeline.activeStages(dog, QualityTier.HIGH)
        assertThat(high).containsAtLeast(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.BRIGHTNESS)

        val bee = getProfile("bee")!!
        assertThat(Pipeline.isActive(bee, StageId.SPECIAL)).isTrue()
        assertThat(Pipeline.isActive(bee, StageId.MOTION, QualityTier.HIGH)).isTrue()
        assertThat(Pipeline.isActive(bee, StageId.MOTION, QualityTier.PERFORMANCE)).isFalse()

        val human = getProfile("human")!!
        assertThat(Pipeline.isActive(human, StageId.ACUITY)).isFalse()

        val snake = getProfile("snake")!!
        assertThat(Pipeline.isActive(snake, StageId.SPECIAL)).isTrue()
    }

    @Test
    fun `tier variants never add stages beyond the base pipeline`() {
        for (profile in getProfilesAll()) {
            for ((tier, stages) in profile.pipeline.qualityVariants) {
                for (s in stages) assertThat(profile.pipeline.stages).contains(s)
            }
        }
    }

    @Test
    fun `disclosure badges advertise simulations honestly`() {
        val bee = SpecialVision.disclosureBadges(getProfile("bee")!!)
        val snake = SpecialVision.disclosureBadges(getProfile("snake")!!)
        assertThat(bee.joinToString()).contains("UV")
        assertThat(snake.joinToString()).contains("SIMULATION")
    }

    @Test
    fun `thermal palette ramps from cold dark to hot white`() {
        val cold = SpecialVision.thermalPalette(0.1)
        val hot = SpecialVision.thermalPalette(0.9)
        assertThat(cold[2]).isGreaterThan(cold[0]) // cold = blue-ish
        assertThat(hot[0]).isGreaterThan(cold[0])
        assertThat(hot[2]).isGreaterThan(cold[2]) // hot = whiter
    }

    @Test
    fun `acuity plan blurs the centre of low-acuity species and adds peripheral blur`() {
        val bee = Acuity.plan(getProfile("bee")!!.acuity, 1280, 720)
        assertThat(bee.centralSigma).isGreaterThan(10.0)
        assertThat(bee.peripheralSigma).isGreaterThan(bee.centralSigma)
        assertThat(bee.unsharpSigma).isEqualTo(0.0)
    }

    @Test
    fun `acuity plan scales with processing resolution so tiers agree`() {
        val dog = getProfile("dog")!!.acuity
        val high = Acuity.plan(dog, 1280, 720)
        val perf = Acuity.plan(dog, 640, 360)
        assertThat(perf.centralSigma).isWithin(1e-9).of(high.centralSigma / 2.0)
    }

    @Test
    fun `eagle plan keeps the image sharp and enables only the foveal unsharp mask`() {
        val eagle = Acuity.plan(getProfile("eagle")!!.acuity, 1280, 720)
        assertThat(eagle.centralSigma).isEqualTo(0.0)
        assertThat(eagle.peripheralSigma).isEqualTo(0.0)
        assertThat(eagle.unsharpSigma).isGreaterThan(0.0)
        assertThat(Acuity.plan(getProfile("human")!!.acuity, 1280, 720).isNoop).isTrue()
    }

    @Test
    fun `pyramid level keeps per-level sigma small and preserves total blur`() {
        for (sigma in doubleArrayOf(0.5, 2.0, 5.0, 19.7, 49.0)) {
            val (level, remaining) = Acuity.pyramidLevel(sigma)
            assertThat(remaining).isAtMost(2.6)
            val scale = (1 shl level).toDouble()
            val total = Math.sqrt((remaining * scale) * (remaining * scale) + (scale * scale - 1.0) / 12.0)
            assertThat(total).isWithin(1e-6).of(sigma)
        }
        assertThat(Acuity.pyramidLevel(1.0).first).isEqualTo(0)
    }

    @Test
    fun `central weight and visibility separate peripheral blur from blind spots`() {
        val p = 0.6
        assertThat(Fov.centralWeight(1f, p)).isEqualTo(1f)
        assertThat(Fov.centralWeight(0.6f, p)).isWithin(1e-6f).of(0f)
        // ordinary periphery is blurred, not darkened
        assertThat(Fov.visibility(0.6f, p)).isWithin(1e-6f).of(1f)
        // a blind spot below the peripheral level fades out
        assertThat(Fov.visibility(0.09f, p)).isLessThan(0.3f)
    }

    @Test
    fun `tone map leaves SDR midtones untouched and keeps white near white`() {
        assertThat(ToneMap.filmic(0.18)).isEqualTo(0.18)
        assertThat(ToneMap.filmic(0.5)).isEqualTo(0.5)
        assertThat(ToneMap.filmic(1.0)).isGreaterThan(0.95)
        assertThat(ToneMap.filmic(5.0)).isAtMost(1.0)
        // monotonic across the knee
        assertThat(ToneMap.filmic(0.91)).isGreaterThan(ToneMap.filmic(0.9))
    }

    private fun getProfilesAll() = com.animalvision.core.profiles.profileRegistry
}
