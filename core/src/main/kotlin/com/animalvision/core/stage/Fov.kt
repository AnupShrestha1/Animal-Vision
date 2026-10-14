package com.animalvision.core.stage

import com.animalvision.core.model.AnimalVisionProfile

/**
 * Field-of-view geometry stage.
 *
 * A phone lens (~65° typical horizontal) cannot cover 240–350° animal fields; we therefore
 * simulate GEOMETRY, never content:
 *  - a central binocular cone is kept at full acuity/color;
 *  - peripheral (monocular) regions roll off toward the animal's peripheral acuity;
 *  - horse blind spots (below muzzle, and the area directly behind the head) are masked;
 *  - species with a high-acuity foveal zone (eagle) receive a bounded central boost mask;
 *  - the final composite applies a radial WARP ([warpCoefficients]) so wide-field prey
 *    species read as panoramic wrap-around vision and narrow-foveal predators (eagle)
 *    read as a targeting tunnel — geometry, not just color.
 */
object Fov {

    /** Nominal horizontal FOV of the phone lens used for mask proportions. */
    const val VIEW_FOV_DEGREES = 65.0

    private fun smoothstep(edge: Double): Double {
        var t = edge.coerceIn(0.0, 1.0)
        t = t * t * (3.0 - 2.0 * t)
        return t
    }

    /**
     * Parameters of [acuityMask], shared with the GPU combine shader (which evaluates the same
     * formula per fragment) so the two cannot drift apart.
     */
    data class MaskParams(
        /** Half-width of the binocular band in normalized frame units (frame = -1..1). */
        val halfWidth: Double,
        /** Vertical extent of the band relative to [halfWidth]. */
        val yAspect: Double,
        /** Mask value outside the binocular band. */
        val peripheral: Double,
        /** Lower-centre blind wedge below/just in front of the muzzle (horse). */
        val muzzleBlindSpot: Boolean,
    )

    /** Muzzle wedge: frame region ny > [MUZZLE_NY] and |nx| < [MUZZLE_NX] (y down), scaled by [MUZZLE_FACTOR]. */
    const val MUZZLE_NY = 0.35
    const val MUZZLE_NX = 0.18
    const val MUZZLE_FACTOR = 0.15

    /**
     * Mask parameters for a frame spanning [viewFovDeg] horizontally (narrower when zoomed).
     * The binocular band is the animal's binocular width as a fraction of that field; when it
     * is wider than the whole frame (cat, human, or any zoomed view) the frame lies entirely
     * inside the binocular zone.
     */
    fun maskParams(profile: AnimalVisionProfile, viewFovDeg: Double = VIEW_FOV_DEGREES): MaskParams {
        val fraction = profile.fieldOfView.binocularWidthDeg / viewFovDeg
        return MaskParams(
            // Frame spans nx -1..1, so a band covering the whole frame has half-width 1;
            // beyond ~1.6 the smooth edge is off-frame as well.
            halfWidth = fraction.coerceIn(0.05, 1.6),
            yAspect = 0.55,
            peripheral = profile.acuity.peripheralScalar.coerceIn(0.0, 1.0),
            muzzleBlindSpot = profile.id == "horse",
        )
    }

    /** Mask value at normalized frame coordinates ([nx], [ny] in -1..1, y pointing down). */
    fun maskAt(p: MaskParams, nx: Double, ny: Double): Double {
        val hw = p.halfWidth
        // binocular band factor: 1 inside cone, smooth fade outside
        val xEdge = (hw - Math.abs(nx)) / (hw * 0.5 + 1e-6) + 0.5
        val yEdge = (p.yAspect * hw - Math.abs(ny)) / (p.yAspect * hw * 0.5 + 1e-6) + 0.5
        val binFactor = smoothstep(xEdge) * smoothstep(yEdge)
        var mask = p.peripheral + (1.0 - p.peripheral) * binFactor
        if (p.muzzleBlindSpot && ny > MUZZLE_NY && Math.abs(nx) < MUZZLE_NX) mask *= MUZZLE_FACTOR
        return mask
    }

    /** Returns a 0..1 per-pixel acuity/visibility mask (row-major, top row first). 1 = full acuity. */
    fun acuityMask(profile: AnimalVisionProfile, width: Int, height: Int): FloatArray {
        val p = maskParams(profile)
        val out = FloatArray(width * height)
        // Pixel centres, so the CPU and the per-fragment GPU evaluation sample the same points.
        for (y in 0 until height) {
            val ny = (y + 0.5) / height * 2.0 - 1.0
            for (x in 0 until width) {
                val nx = (x + 0.5) / width * 2.0 - 1.0
                out[y * width + x] = maskAt(p, nx, ny).toFloat()
            }
        }
        return out
    }

    /** Foveal boost amplitude (0 = none) for [sharpBoostMask]. */
    fun boostAmplitude(profile: AnimalVisionProfile): Double =
        (profile.acuity.centralScalar - 1.0).coerceAtLeast(0.0)

    /** Gaussian width of the foveal boost as a fraction of the frame's shorter side. */
    const val BOOST_SIGMA_FRAC = 0.22

    /**
     * Weight of the central-acuity image (vs the extra-blurred peripheral one) for an
     * [acuityMask] value: 1 inside the binocular zone, 0 at or below the peripheral level.
     */
    fun centralWeight(mask: Float, peripheralScalar: Double): Float {
        if (peripheralScalar >= 1.0) return 1f
        return ((mask - peripheralScalar) / (1.0 - peripheralScalar)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Brightness factor for an [acuityMask] value. Ordinary peripheral falloff is expressed as
     * blur, not darkening; only regions masked BELOW the peripheral level (blind spots such as
     * the horse's muzzle wedge) fade toward near-black.
     */
    fun visibility(mask: Float, peripheralScalar: Double): Float {
        val p = peripheralScalar.coerceIn(0.05, 1.0)
        return 0.05f + 0.95f * (mask / p).toFloat().coerceIn(0f, 1f)
    }

    /**
     * 0..1 per-pixel boost mask for species with a high-acuity central region (eagle fovea). */
    fun sharpBoostMask(profile: AnimalVisionProfile, width: Int, height: Int, sigmaFrac: Double = BOOST_SIGMA_FRAC): FloatArray {
        val amp = boostAmplitude(profile)
        val out = FloatArray(width * height)
        if (amp <= 0.0) return out
        val cx = width / 2.0
        val cy = height / 2.0
        val sigma = sigmaFrac * Math.min(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x + 0.5 - cx) / sigma
                val dy = (y + 0.5 - cy) / sigma
                val g = Math.exp(-0.5 * (dx * dx + dy * dy))
                out[y * width + x] = (amp * g).coerceAtMost(1.0).toFloat()
            }
        }
        return out
    }

    /**
     * Per-axis radial warp coefficients [kx, ky] consumed by the final composite shader:
     *
     * ```
     * src = 0.5 + c * (1 + k * (0.25 - c*c))    where c = uv - 0.5
     * ```
     *
     * - **k > 0 (prey / wide field):** mid-frame pixels sample farther from center, so the
     *   scene is compressed inward — a wide-angle, panoramic wrap-around feel. Scaled by
     *   total horizontal field (65° phone lens → 350° horse) and horizontal-dominant,
     *   because prey fields are wide laterally.
     * - **k < 0 (foveal predator, eagle):** the center is magnified toward the edges — a
     *   tunnel/scope feel that pairs with the foveal boost mask.
     * - **k = 0 (human):** the human profile is the baseline reference (standard lens) and
     *   never warps.
     *
     * The remap is monotonic and bounds-safe for k in (-4, 2); coefficients stay inside
     * (-2.2, 1.6] with margin. Gated on `StageId.FOV` being active by the caller.
     */
    fun warpCoefficients(profile: AnimalVisionProfile): FloatArray {
        // Human is the "standard lens" baseline — no geometry warp.
        if (profile.id == "human") return floatArrayOf(0f, 0f)

        val total = profile.fieldOfView.totalHorizontalDeg
        // 0 at the phone lens width (65°), 1 at the widest species field (horse 350°).
        val wide = ((total - VIEW_FOV_DEGREES) / (350.0 - VIEW_FOV_DEGREES)).coerceIn(0.0, 1.0)
        // Foveal predators (eagle centralScalar 1.6) trade panorama for a targeting tunnel.
        val foveal = (profile.acuity.centralScalar - 1.0).coerceIn(0.0, 1.0)

        val kx = 1.6 * wide * (1.0 - foveal) - 2.0 * foveal
        // Panoramic fields are horizontal-heavy; the foveal tunnel stays rounder.
        val ky = if (kx >= 0.0) kx * 0.45 else kx * 0.8
        return floatArrayOf(kx.toFloat(), ky.toFloat())
    }
}
