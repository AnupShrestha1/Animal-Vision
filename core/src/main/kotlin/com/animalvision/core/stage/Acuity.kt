package com.animalvision.core.stage

import com.animalvision.core.model.AcuityModel

/**
 * Acuity simulation: converts an animal's cycles-per-degree limit into a spatial low-pass
 * (Gaussian optical-transfer rolloff), following the compiled acuity values in docs/SCIENCE.md
 * and the approach of Caves & Johnsen (2018).
 */
object Acuity {

    /** Approximate pixels per visual degree for a typical 720p, ~65°-wide phone frame. */
    const val PX_PER_DEGREE = 19.7

    /** Below this sigma (pixels) a blur is visually indistinguishable from none. */
    const val MIN_SIGMA = 0.35

    /** Reference-blur sigma (pixels) for the bounded foveal unsharp mask. */
    const val UNSHARP_SIGMA = 0.8

    /** Strength of the foveal unsharp mask; detail is clamped to ±0.05 so it stays halo-free. */
    const val SHARPEN_AMOUNT = 0.35

    /**
     * Pixels per visual degree for an image of [width]×[height] whose long side spans the
     * nominal phone-lens field ([Fov.VIEW_FOV_DEGREES]). Blur sigmas must be derived from the
     * resolution actually being processed, or apparent acuity would change with quality tier.
     */
    fun pxPerDegree(width: Int, height: Int): Double =
        Math.max(width, height) / Fov.VIEW_FOV_DEGREES

    /**
     * Per-frame acuity plan shared by the CPU reference and the GPU renderer.
     *
     * The species' central acuity limit blurs the WHOLE frame (an animal with 0.5 c/deg does
     * not see a sharp centre); the peripheral scalar adds further blur outside the central zone.
     * A sigma of 0 means "leave sharp".
     */
    data class Plan(
        val centralSigma: Double,
        val peripheralSigma: Double,
        /** > 0 only for foveal species finer than the display (eagle): bounded unsharp mask. */
        val unsharpSigma: Double,
        val peripheralScalar: Double,
    ) {
        val isNoop: Boolean get() = centralSigma == 0.0 && peripheralSigma == 0.0 && unsharpSigma == 0.0
    }

    fun plan(model: AcuityModel, width: Int, height: Int): Plan {
        val ppd = pxPerDegree(width, height)
        fun sigma(cpd: Double): Double = blurSigmaPixels(cpd, ppd).let { if (it < MIN_SIGMA) 0.0 else it }
        val p = model.peripheralScalar.coerceIn(0.05, 1.0)
        val central = sigma(model.cutoffCpd)
        val peripheral = if (p < 1.0) sigma(model.cutoffCpd * p) else central
        val unsharp = if (model.centralScalar > 1.0 && central == 0.0) UNSHARP_SIGMA else 0.0
        return Plan(central, peripheral, unsharp, p)
    }

    /**
     * Pyramid level for a large blur: the image is halved [level] times (2×2 box each step)
     * until the remaining sigma is at most [maxSigmaPerLevel], so a fixed-size kernel can
     * realize any sigma. Returns the level and the sigma still to apply AT that level
     * (in that level's pixels), with the box prefilters' variance already subtracted.
     */
    fun pyramidLevel(sigma: Double, maxSigmaPerLevel: Double = 2.6, maxLevel: Int = 5): Pair<Int, Double> {
        var level = 0
        while (level < maxLevel && sigma / (1 shl level) > maxSigmaPerLevel) level++
        val scale = (1 shl level).toDouble()
        // Each 2× box step i adds variance 4^(i-1)/4 base px²; the sum is (4^L − 1)/12.
        val prefilterVar = (scale * scale - 1.0) / 12.0
        val remaining = Math.sqrt(Math.max(0.0, sigma * sigma - prefilterVar)) / scale
        return level to remaining
    }

    /**
     * Gaussian blur sigma (in pixels) for a species' acuity cutoff.
     * sigma = pxPerDegree / (2 * cutoffCpd): a grating at the cutoff needs ~1 cycle per
     * ~1 sigma-feature; this yields perceptible softness at low c/deg and near-sharpness at high.
     */
    fun blurSigmaPixels(cutoffCpd: Double, pxPerDegree: Double = PX_PER_DEGREE): Double {
        if (cutoffCpd <= 0.0) return 0.0
        return pxPerDegree / (2.0 * cutoffCpd)
    }

    /** Normalized 1D Gaussian kernel (odd length; length derives from sigma). */
    fun gaussianKernel(sigma: Double): FloatArray {
        val radius = Math.max(1, (sigma * 3.0).toInt())
        val size = radius * 2 + 1
        val kernel = FloatArray(size)
        var sum = 0.0
        for (i in 0 until size) {
            val x = (i - radius).toDouble()
            kernel[i] = Math.exp(-(x * x) / (2.0 * sigma * sigma + 1e-9)).toFloat()
            sum += kernel[i]
        }
        for (i in kernel.indices) kernel[i] = (kernel[i] / sum).toFloat()
        return kernel
    }

    /**
     * Separable Gaussian blur on a planar float image (per channel).
     * [src] and [dst] are length w*h. [dst] may alias [src] only for the combined pass —
     * callers pass a separate intermediate for the horizontal pass.
     */
    fun blurChannel(src: FloatArray, dst: FloatArray, width: Int, height: Int, sigma: Double) {
        if (sigma <= 0.01) {
            src.copyInto(dst, 0, 0, width * height)
            return
        }
        val kernel = gaussianKernel(sigma)
        val radius = (kernel.size - 1) / 2
        val tmp = FloatArray(width * height)

        // horizontal
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var acc = 0f
                for (k in kernel.indices) {
                    val xk = (x + k - radius).coerceIn(0, width - 1)
                    acc += src[row + xk] * kernel[k]
                }
                tmp[row + x] = acc
            }
        }
        // vertical
        for (x in 0 until width) {
            for (y in 0 until height) {
                var acc = 0f
                for (k in kernel.indices) {
                    val yk = (y + k - radius).coerceIn(0, height - 1)
                    acc += tmp[yk * width + x] * kernel[k]
                }
                dst[y * width + x] = acc
            }
        }
    }
}
