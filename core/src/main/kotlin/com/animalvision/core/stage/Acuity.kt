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

    /**
     * Bounded, halo-free unsharp mask for high-acuity species (eagle). Returns an image where
     * local contrast in the feature region is gently boosted. [amount] in (0, ~0.5]; clamp keeps
     * overshoot tiny so halos are avoided (spec §12).
     */
    fun sharpenChannel(src: FloatArray, dst: FloatArray, width: Int, height: Int, sigma: Double, amount: Double) {
        val blurred = FloatArray(width * height)
        blurChannel(src, blurred, width, height, sigma)
        for (i in src.indices) {
            val detail = src[i] - blurred[i]
            val clamped = detail.coerceIn(-0.05f, 0.05f)
            dst[i] = (src[i] + amount.toFloat() * clamped).coerceIn(0f, 1f)
        }
    }

    /** Convenience: effective sigma for a model (0 if the cutoff implies finer than display). */
    fun effectiveSigma(model: AcuityModel): Double {
        val sigma = blurSigmaPixels(model.cutoffCpd)
        return if (sigma < 0.35) 0.0 else sigma
    }
}
