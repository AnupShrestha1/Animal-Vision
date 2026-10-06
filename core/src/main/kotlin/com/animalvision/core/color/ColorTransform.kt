package com.animalvision.core.color

import com.animalvision.core.model.AnimalVisionProfile

/**
 * Species-specific color transformation layer.
 *
 * Pipeline (documented in docs/SCIENCE.md, Common methodology):
 *  1. receptor curves from Govardovskii A1 templates at the species' λmax;
 *  2. linear RGB -> receptor catches via precomputed
 *     M = Q·diag(E)·B·pinv(CMFᵀ·diag(E)·B)·(sRGB→XYZ);
 *  3. von-Kries white-point normalization;
 *  4. display mapping per species family (see DisplayMappings.kt).
 *
 * Input/output are LINEAR RGB; the caller applies tone-map + sRGB encoding.
 */
class ColorTransform(val profile: AnimalVisionProfile) {

    enum class Kind { HUMAN, MAMMAL_DICHROMAT, HYMENOPTERAN_TRICHROMAT, BIRD_TETRACHROMAT }

    val kind: Kind = when (profile.id) {
        "human" -> Kind.HUMAN
        "bee" -> Kind.HYMENOPTERAN_TRICHROMAT
        "eagle" -> Kind.BIRD_TETRACHROMAT
        else -> Kind.MAMMAL_DICHROMAT
    }

    private val nW = Spectrum.LAMBDA.size
    val nR: Int = profile.colorVision.sensitivities.size

    /** nR×3 matrix: linear RGB -> receptor catches. */
    val catchMatrix: Array<DoubleArray>

    /** White (1,1,1) catches, for von-Kries normalization. */
    val whiteCatches: DoubleArray

    init {
        val nB = Spectrum.BASIS_CENTERS.size
        val a = Array(3) { r ->
            val cmf = when (r) { 0 -> Spectrum.CMF_X; 1 -> Spectrum.CMF_Y; else -> Spectrum.CMF_Z }
            DoubleArray(nB) { b ->
                var s = 0.0
                for (w in 0 until nW) s += cmf[w] * Spectrum.D65[w] * Spectrum.basisValue(Spectrum.BASIS_CENTERS[b], Spectrum.LAMBDA[w])
                s
            }
        }
        val pinvA = Mat.pinv3xN(a)
        val q = Array(nR) { i -> OpsinTemplate.sensitivityCurve(profile.colorVision.sensitivities[i].peakNm) }
        val mQB = Array(nR) { i ->
            DoubleArray(nB) { b ->
                var s = 0.0
                for (w in 0 until nW) s += q[i][w] * Spectrum.D65[w] * Spectrum.basisValue(Spectrum.BASIS_CENTERS[b], Spectrum.LAMBDA[w])
                s
            }
        }
        val mQBA = Array(nR) { i ->
            DoubleArray(3) { j ->
                var s = 0.0
                for (k in 0 until nB) s += mQB[i][k] * pinvA[k][j]
                s
            }
        }
        catchMatrix = Array(nR) { i -> Mat.mulRow(mQBA[i], Spectrum.SRGB_TO_XYZ) }
        whiteCatches = DoubleArray(nR) { i -> catchMatrix[i][0] + catchMatrix[i][1] + catchMatrix[i][2] }
    }

    fun catches(linearRgb: DoubleArray): DoubleArray {
        val out = DoubleArray(nR)
        for (i in 0 until nR) {
            var s = 0.0
            for (j in 0..2) s += catchMatrix[i][j] * linearRgb[j]
            out[i] = s
        }
        return out
    }

    /** Von-Kries normalized catches (white -> 1). */
    fun normalizedCatches(linearRgb: DoubleArray): DoubleArray {
        val c = catches(linearRgb)
        return DoubleArray(nR) { (c[it] / whiteCatches[it]).coerceAtLeast(0.0) }
    }

    /** Map a linear RGB pixel to a LINEAR display RGB pixel for this species. */
    fun applyLinear(rgb: DoubleArray): DoubleArray = when (kind) {
        Kind.HUMAN -> doubleArrayOf(rgb[0], rgb[1], rgb[2])
        Kind.MAMMAL_DICHROMAT -> DisplayMappings.dichromat(normalizedCatches(rgb))
        Kind.HYMENOPTERAN_TRICHROMAT -> DisplayMappings.bee(normalizedCatches(rgb), rgb[2])
        Kind.BIRD_TETRACHROMAT -> DisplayMappings.bird(normalizedCatches(rgb))
    }
}
