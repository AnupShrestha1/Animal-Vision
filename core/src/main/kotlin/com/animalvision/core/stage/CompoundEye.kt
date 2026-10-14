package com.animalvision.core.stage

import com.animalvision.core.model.AcuityModel

/**
 * Optional ommatidial mosaic for compound-eyed species (bee, fly, butterfly, mantis shrimp).
 *
 * A compound eye forms ONE image, sampled by many facets (ommatidia), each looking in its own
 * direction. The popular "many little copies of the scene" picture is a myth. This stage shows
 * the honest version: the frame is tiled with hexagonal facets and each facet takes the single
 * value at its centre, with faint borders between facets.
 *
 * Facet pitch comes from the profile's acuity limit via the sampling (Nyquist) relation
 * Δφ = 1 / (2ν): a grating at ν cycles/degree needs two facets per cycle. So the mosaic is
 * consistent with the acuity blur already applied, and scales with zoom like it.
 *
 * Grid: centres on a flat-sided hex lattice with spacing [pitch] between neighbours; each cell
 * is a hexagon with inradius pitch/2. The same math runs in Shaders2.FRAG_FINAL.
 */
object CompoundEye {

    /** Strength of the darkening along facet borders (0 = invisible borders). */
    const val BORDER_DARKEN = 0.45

    /** Border band width as a fraction of the facet pitch. */
    const val BORDER_FRACTION = 0.08

    private const val SQRT3 = 1.7320508075688772

    /** Angular spacing between facets in degrees, from the acuity limit (Δφ = 1 / 2ν). */
    fun facetPitchDeg(model: AcuityModel): Double = 1.0 / (2.0 * model.cutoffCpd)

    /** Facet pitch in pixels for a [width]×[height] frame spanning [viewFovDeg]. */
    fun facetPitchPixels(model: AcuityModel, width: Int, height: Int, viewFovDeg: Double = Fov.VIEW_FOV_DEGREES): Double =
        facetPitchDeg(model) * Acuity.pxPerDegree(width, height, viewFovDeg)

    /**
     * Centre of the facet containing point ([x], [y]) for facet [pitch], as a 2-element array.
     * Two rectangular lattices (pitch × pitch·√3), offset by half a cell, together form the
     * hexagonal lattice; the nearer of the two candidate centres wins.
     */
    fun facetCenter(x: Double, y: Double, pitch: Double): DoubleArray {
        val rx = pitch
        val ry = pitch * SQRT3
        fun cell(px: Double, py: Double): DoubleArray {
            val cx = Math.floor(px / rx) * rx + rx / 2.0
            val cy = Math.floor(py / ry) * ry + ry / 2.0
            return doubleArrayOf(cx, cy)
        }
        val a = cell(x, y)
        val b = cell(x - rx / 2.0, y - ry / 2.0).let { doubleArrayOf(it[0] + rx / 2.0, it[1] + ry / 2.0) }
        val da = (x - a[0]) * (x - a[0]) + (y - a[1]) * (y - a[1])
        val db = (x - b[0]) * (x - b[0]) + (y - b[1]) * (y - b[1])
        return if (da <= db) a else b
    }

    /**
     * Brightness factor at ([x], [y]): 1 inside a facet, dipping toward the hexagon's edge.
     * Hex distance for this lattice: max(|dx|, |dx|/2 + |dy|·√3/2), which equals pitch/2 on the
     * cell border.
     */
    fun borderFactor(x: Double, y: Double, pitch: Double): Double {
        val c = facetCenter(x, y, pitch)
        val dx = Math.abs(x - c[0])
        val dy = Math.abs(y - c[1])
        val hex = Math.max(dx, dx * 0.5 + dy * SQRT3 / 2.0) / (pitch / 2.0) // 0 centre .. 1 edge
        val t = ((hex - (1.0 - 2.0 * BORDER_FRACTION)) / (2.0 * BORDER_FRACTION)).coerceIn(0.0, 1.0)
        return 1.0 - BORDER_DARKEN * t * t * (3.0 - 2.0 * t)
    }
}
