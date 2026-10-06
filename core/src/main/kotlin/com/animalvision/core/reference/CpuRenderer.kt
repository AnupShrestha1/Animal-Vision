package com.animalvision.core.reference

import com.animalvision.core.color.Lut
import com.animalvision.core.color.Spectrum
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.StageId
import com.animalvision.core.pipeline.Pipeline
import com.animalvision.core.stage.Acuity
import com.animalvision.core.stage.Fov
import com.animalvision.core.stage.LowLight
import com.animalvision.core.stage.SpecialVision
import com.animalvision.core.stage.ToneMap

/**
 * CPU reference implementation of the full vision pipeline.
 * Used for deterministic GPU-free tests, visual validation (docs/SCIENCE.md), and as a pure-software
 * render fallback on devices without WebGL. Operates on RGBA8888; internal math is linear.
 */
class CpuRenderer(
    private val profile: AnimalVisionProfile,
    private val lut: Lut,
    private val tier: QualityTier = QualityTier.BALANCED,
) {
    private val stages: List<StageId> = Pipeline.activeStages(profile, tier)
    private fun active(s: StageId): Boolean = s in stages

    fun render(input: IntArray, width: Int, height: Int): IntArray {
        require(input.size == width * height) { "input buffer size mismatch" }
        val n = width * height
        val r = FloatArray(n); val g = FloatArray(n); val b = FloatArray(n)

        // 1. linearize sRGB -> linear
        for (i in 0 until n) {
            val a = input[i]
            r[i] = Spectrum.linearizeSrgb(((a ushr 16) and 0xff) / 255.0).toFloat()
            g[i] = Spectrum.linearizeSrgb(((a ushr 8) and 0xff) / 255.0).toFloat()
            b[i] = Spectrum.linearizeSrgb((a and 0xff) / 255.0).toFloat()
        }

        // 2. color — species LUT (receptor-catch transform)
        if (active(StageId.COLOR)) {
            for (i in 0 until n) {
                val s = lut.sample(r[i].toDouble(), g[i].toDouble(), b[i].toDouble())
                r[i] = s[0].toFloat(); g[i] = s[1].toFloat(); b[i] = s[2].toFloat()
            }
        }

        // 3. brightness / low-light (contrast-preserving shadow lift + rod desaturation + noise)
        if (active(StageId.BRIGHTNESS) && profile.lowLight.enabled) {
            val noise = LowLight.Noise(0x243F6A8885A308D3L)
            for (i in 0 until n) {
                val p = LowLight.applyPixel(
                    r[i].toDouble(), g[i].toDouble(), b[i].toDouble(),
                    profile.lowLight.shadowLift, profile.lowLight.maxDesaturation, k = 0.35,
                )
                r[i] = p[0].toFloat(); g[i] = p[1].toFloat(); b[i] = p[2].toFloat()
                if (profile.lowLight.noiseSigma > 0.0) {
                    val ns = profile.lowLight.noiseSigma
                    r[i] = (r[i] + noise.gaussian(ns)).toFloat().coerceIn(0f, 1f)
                    g[i] = (g[i] + noise.gaussian(ns)).toFloat().coerceIn(0f, 1f)
                    b[i] = (b[i] + noise.gaussian(ns)).toFloat().coerceIn(0f, 1f)
                }
            }
        }

        // 4/5. acuity (spatial-frequency rolloff) blended by FOV geometry mask
        val mask = if (active(StageId.FOV)) Fov.acuityMask(profile, width, height) else FloatArray(n) { 1f }
        if (active(StageId.ACUITY)) {
            val sigma = Acuity.effectiveSigma(profile.acuity)
            val rA = FloatArray(n); val gA = FloatArray(n); val bA = FloatArray(n)
            if (sigma > 0.0) {
                Acuity.blurChannel(r, rA, width, height, sigma)
                Acuity.blurChannel(g, gA, width, height, sigma)
                Acuity.blurChannel(b, bA, width, height, sigma)
            } else {
                r.copyInto(rA); g.copyInto(gA); b.copyInto(bA)
            }
            for (i in 0 until n) {
                val m = mask[i]
                r[i] = r[i] * m + rA[i] * (1f - m)
                g[i] = g[i] * m + gA[i] * (1f - m)
                b[i] = b[i] * m + bA[i] * (1f - m)
            }

            // eagle-style foveal sharpening — bounded and halo-free
            val boost = Fov.sharpBoostMask(profile, width, height)
            if (boost.any { it > 0f }) {
                val rS = FloatArray(n); val gS = FloatArray(n); val bS = FloatArray(n)
                val sh = Math.max(0.8, sigma)
                Acuity.sharpenChannel(r, rS, width, height, sh, 0.35)
                Acuity.sharpenChannel(g, gS, width, height, sh, 0.35)
                Acuity.sharpenChannel(b, bS, width, height, sh, 0.35)
                for (i in 0 until n) {
                    val amp = boost[i]
                    r[i] = r[i] * (1f - amp) + rS[i] * amp
                    g[i] = g[i] * (1f - amp) + gS[i] * amp
                    b[i] = b[i] * (1f - amp) + bS[i] * amp
                }
            }
            // blind-spot darkening (near-invisible where the mask is ~0)
            for (i in 0 until n) {
                val vis = 0.05f + 0.95f * mask[i]
                r[i] *= vis; g[i] *= vis; b[i] *= vis
            }
        }

        // 6. special vision — thermal simulation only (UV is part of the color LUT)
        if (active(StageId.SPECIAL) && profile.specialVision.infrared != null) {
            for (i in 0 until n) {
                val t = SpecialVision.applyThermalSimulation(
                    doubleArrayOf(r[i].toDouble(), g[i].toDouble(), b[i].toDouble()),
                    resolution = 6,
                )
                r[i] = t[0].toFloat(); g[i] = t[1].toFloat(); b[i] = t[2].toFloat()
            }
        }

        // 7. tone map + sRGB encode
        val out = IntArray(n)
        for (i in 0 until n) {
            val d = ToneMap.toDisplay(doubleArrayOf(r[i].toDouble(), g[i].toDouble(), b[i].toDouble()))
            val r8 = (d[0] * 255.0 + 0.5).toInt().coerceIn(0, 255)
            val g8 = (d[1] * 255.0 + 0.5).toInt().coerceIn(0, 255)
            val b8 = (d[2] * 255.0 + 0.5).toInt().coerceIn(0, 255)
            out[i] = (0xff shl 24) or (r8 shl 16) or (g8 shl 8) or b8
        }
        return out
    }
}
