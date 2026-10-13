package com.animalvision.core.stage

import com.animalvision.core.model.AnimalVisionProfile

/**
 * Specialized vision modules (spec §20). All are SIMULATIONS on an ordinary phone and the
 * renderer never claims otherwise; real hardware (thermal/UV) will plug in behind the same
 * interface later.
 */
object SpecialVision {

    /** Classic "thermal" palette keyed at t ∈ [0,1]: black→purple→red→orange→yellow→white. */
    fun thermalPalette(t: Double): DoubleArray {
        val x = t.coerceIn(0.0, 1.0)
        return when {
            x < 0.25 -> {
                val s = x / 0.25
                doubleArrayOf(0.0, 0.0, 0.5 * s)
            }
            x < 0.5 -> {
                val s = (x - 0.25) / 0.25
                doubleArrayOf(0.8 * s, 0.0, 0.5 + 0.3 * s)
            }
            x < 0.75 -> {
                val s = (x - 0.5) / 0.25
                doubleArrayOf(0.8 + 0.2 * s, 0.9 * s, 0.8 - 0.5 * s)
            }
            else -> {
                val s = (x - 0.75) / 0.25
                doubleArrayOf(1.0, 0.9 + 0.1 * s, 0.3 + 0.7 * s)
            }
        }
    }

    /**
     * Educational thermal simulation for pit-organ snakes. An RGB camera does NOT measure
     * thermal IR; the image tone is mapped to a low-resolution "heat" palette that conveys the
     * concept of a coarse thermal map, and the UI carries the mandated disclosure label.
     */
    fun applyThermalSimulation(linearRgb: DoubleArray, resolution: Int = 4): DoubleArray {
        // Coarse block-average to mimic the pit organ's low spatial resolution (generous caveat).
        val luma = 0.2126 * linearRgb[0] + 0.7152 * linearRgb[1] + 0.0722 * linearRgb[2]
        val block = (resolution.toDouble()).let { r -> Math.round(luma * r) / r } // 0,1/r..1
        val palette = thermalPalette(block)
        // retain some visible structure so the scene stays recognizable, clearly a visualization
        return doubleArrayOf(
            (palette[0] * (0.35 + 0.65 * luma)).coerceIn(0.0, 1.0),
            (palette[1] * (0.35 + 0.65 * luma)).coerceIn(0.0, 1.0),
            (palette[2] * (0.35 + 0.65 * luma)).coerceIn(0.0, 1.0),
        )
    }

    /** Disclosure shown whenever the polarization overlay is drawn. */
    const val POLARIZATION_DISCLOSURE =
        "Polarization is a labeled SIMULATION — hatching marks bright glare, where reflected " +
            "light is most strongly polarized; phones have no polarization sensor."

    /** Hatch period of the polarization overlay, in output pixels. */
    const val POLARIZATION_STRIPE_PX = 8.0

    /**
     * Strength (0..1) of the simulated polarization cue at a pixel. Specular glare off water,
     * glass and leaves is strongly polarized by reflection (Brewster), and in an RGB image it is
     * the bright, low-saturation highlights — that is what the overlay marks. It is a heuristic
     * cue, never a measurement.
     */
    fun polarizationStrength(linearRgb: DoubleArray): Double {
        val r = linearRgb[0]; val g = linearRgb[1]; val b = linearRgb[2]
        val luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val max = Math.max(r, Math.max(g, b))
        val min = Math.min(r, Math.min(g, b))
        val sat = if (max > 1e-6) (max - min) / max else 0.0
        return smoothstep(0.55, 0.85, luma) * (1.0 - smoothstep(0.15, 0.4, sat))
    }

    /**
     * Applies the polarization overlay: diagonal hatching (standing in for the e-vector
     * orientation an animal could read) tinted cyan, weighted by [polarizationStrength].
     * [x], [y] are output pixel coordinates.
     */
    fun applyPolarizationOverlay(linearRgb: DoubleArray, x: Int, y: Int): DoubleArray {
        val k = polarizationStrength(linearRgb)
        if (k <= 0.0) return linearRgb
        val phase = ((x + y) / POLARIZATION_STRIPE_PX) % 1.0
        val stripe = if (phase < 0.5) 1.0 else 0.0
        val a = 0.55 * k * stripe
        val tint = doubleArrayOf(0.15, 0.85, 1.0)
        return DoubleArray(3) { linearRgb[it] * (1.0 - a) + tint[it] * a }
    }

    private fun smoothstep(e0: Double, e1: Double, x: Double): Double {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0)
        return t * t * (3.0 - 2.0 * t)
    }

    /** Exactly which educational badge the UI must show for a profile's special systems. */
    fun disclosureBadges(profile: AnimalVisionProfile): List<String> {
        val badges = mutableListOf<String>()
        profile.specialVision.ultraviolet?.let { if (it.isSimulation) badges += it.falseColorLabel }
        profile.specialVision.infrared?.let { badges += it.label }
        if (profile.specialVision.polarizationSimulation) badges += POLARIZATION_DISCLOSURE
        Motion.disclosure(profile)?.let { badges += it }
        return badges
    }
}
