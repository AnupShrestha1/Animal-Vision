package com.animalvision.core.color

/**
 * Spectral infrastructure (see docs/SCIENCE.md methodology notes):
 * CIE 1931 2° observer, illuminant D65, Gaussian reflectance basis, sRGB<->XYZ.
 */
object Spectrum {

    /** Wavelength grid 350–730 nm, 10 nm steps (includes UV for bee/snake short-wave channels). */
    val LAMBDA: DoubleArray = DoubleArray(39) { 350.0 + it * 10.0 }
    private const val NM560 = 21 // index of 560 nm

    /** CIE illuminant D65, relative SPD at [LAMBDA] (560 nm = 100). */
    val D65: DoubleArray = doubleArrayOf(
        // 350/360/370 extrapolated along the D65 rising tail (energy is negligible there).
        14.9, 22.4, 33.4, 49.98, 54.29, 82.75, 91.49, 93.43, 86.68, // 350..430
        104.86, 117.01, 117.81, 114.86, 115.92, 108.81, 109.35, 107.86, 104.79, // 440..520
        107.69, 104.41, 104.05, 100.00, 96.33, 95.79, 88.69, 90.01, 89.60, 87.70, // 530..630
        83.29, 83.70, 80.03, 80.21, 82.28, 78.28, 69.72, 71.61, // 640..710
        74.31, 61.60, 69.89, // 720,730
    )
    // D65 length must be 39 to match LAMBDA.

    /** CIE 1931 2° color matching functions xbar,ybar,zbar on [LAMBDA]. */
    val CMF_X: DoubleArray = doubleArrayOf(
        0.0, 0.0, 0.0,
        0.001368, 0.004243, 0.014310, 0.043510, 0.134380, 0.283900,
        0.348280, 0.336200, 0.290800, 0.195360, 0.095640, 0.032010,
        0.004900, 0.009300, 0.063270, 0.165500, 0.290400, 0.433450,
        0.594500, 0.762100, 0.916300, 1.026300, 1.062200, 1.002600,
        0.854450, 0.642400, 0.447900, 0.283500, 0.164900, 0.087400,
        0.046770, 0.022700, 0.011359, 0.005790, 0.002899, 0.001440,
    )
    val CMF_Y: DoubleArray = doubleArrayOf(
        0.0, 0.0, 0.0,
        0.000039, 0.000120, 0.000396, 0.001210, 0.004000, 0.011600,
        0.023000, 0.038000, 0.060000, 0.090980, 0.139020, 0.208020,
        0.323000, 0.503000, 0.710000, 0.862000, 0.954000, 0.994950,
        0.995000, 0.952000, 0.870000, 0.757000, 0.631000, 0.503000,
        0.381000, 0.265000, 0.175000, 0.107000, 0.061000, 0.032000,
        0.017000, 0.008210, 0.004102, 0.002091, 0.001047, 0.000520,
    )
    val CMF_Z: DoubleArray = doubleArrayOf(
        0.0, 0.0, 0.0,
        0.006450, 0.020050, 0.067850, 0.207400, 0.645600, 1.385600,
        1.747060, 1.772110, 1.669200, 1.287640, 0.812950, 0.465180,
        0.272000, 0.158200, 0.078250, 0.042160, 0.020300, 0.008750,
        0.003900, 0.002100, 0.001650, 0.001100, 0.000800, 0.000340,
        0.000190, 0.000050, 0.000020, 0.000000, 0.000000, 0.000000,
        0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000,
    )

    /** sRGB (D65) <-> linear XYZ matrices. */
    val SRGB_TO_XYZ: Array<DoubleArray> = arrayOf(
        doubleArrayOf(0.4124564, 0.3575761, 0.1804375),
        doubleArrayOf(0.2126729, 0.7151522, 0.0721750),
        doubleArrayOf(0.0193339, 0.1191920, 0.9503041),
    )
    val XYZ_TO_SRGB: Array<DoubleArray> = arrayOf(
        doubleArrayOf(3.2404542, -1.5371385, -0.4985314),
        doubleArrayOf(-0.9692660, 1.8760108, 0.0415560),
        doubleArrayOf(0.0556434, -0.2040259, 1.0572252),
    )

    fun linearizeSrgb(x: Double): Double =
        if (x <= 0.04045) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4)

    fun encodeSrgb(x: Double): Double {
        val v = x.coerceIn(0.0, 1.0)
        return if (v <= 0.0031308) 12.92 * v else 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055
    }

    /** Gaussian reflectance basis (documented low-rank approximation). */
    val BASIS_CENTERS: DoubleArray = doubleArrayOf(375.0, 425.0, 475.0, 525.0, 575.0, 625.0, 675.0, 725.0)
    private const val BASIS_SIGMA = 55.0
    fun basisValue(centerNm: Double, lambdaNm: Double): Double {
        val d = (lambdaNm - centerNm) / BASIS_SIGMA
        return Math.exp(-0.5 * d * d)
    }

    /**
     * Reflectance estimate from linear sRGB:
     * R = B * pinv(CMFᵀ·diag(E)·B) · XYZ(srgb).  See docs/SCIENCE.md.
     */
    fun estimateReflectance(linearRgb: DoubleArray): DoubleArray {
        val xyz = DoubleArray(3) { r ->
            var s = 0.0
            for (c in 0..2) s += SRGB_TO_XYZ[r][c] * linearRgb[c]
            s
        }
        val nW = LAMBDA.size
        val nB = BASIS_CENTERS.size
        val a = Array(3) { r ->
            val cmfRow = when (r) { 0 -> CMF_X; 1 -> CMF_Y; else -> CMF_Z }
            DoubleArray(nB) { b ->
                var s = 0.0
                for (w in 0 until nW) s += cmfRow[w] * D65[w] * basisValue(BASIS_CENTERS[b], LAMBDA[w])
                s
            }
        }
        val pinvA = Mat.pinv3xN(a)
        val coeffs = Mat.mulv(pinvA, xyz)
        return DoubleArray(nW) { w ->
            var s = 0.0
            for (b in 0 until nB) s += basisValue(BASIS_CENTERS[b], LAMBDA[w]) * coeffs[b]
            s.coerceAtLeast(0.0)
        }
    }
}
