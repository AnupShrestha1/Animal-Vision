package com.animalvision.core.color

/**
 * Govardovskii et al. (2000) A1 rhodopsin spectral template, used for every species'
 * photoreceptor sensitivity in this project (see docs/SCIENCE.md, Common methodology #1).
 *
 * Template parameters (A1): a=69.7, b=28.0, c=-14.9, d=0.674,
 * A=0.880, B=0.924, C=1.104, D=0.655. x = lambdaMax / lambda.
 * Valid over ≈350–700 nm for peaks ≈350–620 nm; adequately accurate for the UV, blue and
 * long-wave peaks used here (344–565 nm). The small β-band term is omitted (documented).
 */
object OpsinTemplate {

    /** Slope (steepness) parameters of the A1 α-band. */
    private const val A = 69.7
    private const val B = 28.0
    private const val C = -14.9
    /** Center (position) parameters. */
    private const val aP = 0.880
    private const val bP = 0.924
    private const val cP = 1.104
    /** Tail offset. */
    private const val dP = 0.655

    /** Normalized sensitivity of a peak-[lambdaMax] A1 pigment at wavelength [lambda] nm. */
    fun sensitivity(lambdaMax: Double, lambda: Double): Double {
        if (lambda < 300.0 || lambda > 700.0) return 0.0
        val x = lambdaMax / lambda
        val denom =
            Math.exp(A * (aP - x)) +
                Math.exp(B * (bP - x)) +
                Math.exp(C * (cP - x)) +
                dP
        return 1.0 / denom
    }

    /** Precomputed discretized sensitivity on the project's wavelength grid. */
    fun sensitivityCurve(lambdaMax: Double): DoubleArray {
        val out = DoubleArray(Spectrum.LAMBDA.size)
        for (i in Spectrum.LAMBDA.indices) out[i] = sensitivity(lambdaMax, Spectrum.LAMBDA[i])
        return out
    }
}
