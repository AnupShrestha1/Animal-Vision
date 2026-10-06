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

    /** Exactly which educational badge the UI must show for a profile's special systems. */
    fun disclosureBadges(profile: AnimalVisionProfile): List<String> {
        val badges = mutableListOf<String>()
        profile.specialVision.ultraviolet?.let { if (it.isSimulation) badges += it.falseColorLabel }
        profile.specialVision.infrared?.let { badges += it.label }
        if (profile.specialVision.polarizationSimulation) {
            badges += "Polarization overlay is simulated (no polarization sensor)."
        }
        return badges
    }
}
