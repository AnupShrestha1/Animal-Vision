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
 *  - species with a high-acuity foveal zone (eagle) receive a bounded central boost mask.
 */
object Fov {

    /** Nominal horizontal FOV of the phone lens used for mask proportions. */
    const val VIEW_FOV_DEGREES = 65.0

    private fun smoothstep(edge: Double): Double {
        var t = edge.coerceIn(0.0, 1.0)
        t = t * t * (3.0 - 2.0 * t)
        return t
    }

    /** Returns a 0..1 per-pixel acuity/visibility mask. 1 = full acuity, 0 = (near) blind. */
    fun acuityMask(profile: AnimalVisionProfile, width: Int, height: Int): FloatArray {
        val binWidth = profile.fieldOfView.binocularWidthDeg.coerceAtMost(VIEW_FOV_DEGREES)
        val hw = (binWidth / VIEW_FOV_DEGREES).coerceIn(0.05, 1.0) / 2.0
        val peripheral = profile.acuity.peripheralScalar.coerceIn(0.0, 1.0)
        val yAspect = 0.55
        val out = FloatArray(width * height)
        val cx = width / 2.0
        val cy = height / 2.0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = (x - cx) / cx
                val ny = (y - cy) / cy
                // binocular band factor: 1 inside cone, smooth fade outside
                val xEdge = (hw - Math.abs(nx)) / (hw * 0.5 + 1e-6) + 0.5
                val yEdge = (yAspect * hw - Math.abs(ny)) / (yAspect * hw * 0.5 + 1e-6) + 0.5
                val binFactor = smoothstep(xEdge) * smoothstep(yEdge)
                var mask = peripheral + (1.0 - peripheral) * binFactor
                // horse muzzle blind spot: lower-center wedge (object below/just in front of the nose)
                if (profile.id == "horse") {
                    if (ny > 0.35 && Math.abs(nx) < 0.18) mask *= 0.15
                }
                out[y * width + x] = mask.toFloat()
            }
        }
        return out
    }

    /** 0..1 per-pixel boost mask for species with a high-acuity central region (eagle fovea). */
    fun sharpBoostMask(profile: AnimalVisionProfile, width: Int, height: Int, sigmaFrac: Double = 0.22): FloatArray {
        val amp = (profile.acuity.centralScalar - 1.0).coerceAtLeast(0.0)
        val out = FloatArray(width * height)
        if (amp <= 0.0) return out
        val cx = width / 2.0
        val cy = height / 2.0
        val sigma = sigmaFrac * Math.min(width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / sigma
                val dy = (y - cy) / sigma
                val g = Math.exp(-0.5 * (dx * dx + dy * dy))
                out[y * width + x] = (amp * g).coerceAtMost(1.0).toFloat()
            }
        }
        return out
    }
}
