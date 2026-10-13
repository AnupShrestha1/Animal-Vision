package com.animalvision.core.stage

import com.animalvision.core.model.AnimalVisionProfile

/**
 * Temporal vision (MOTION stage).
 *
 * Every species with a measured flicker-fusion frequency in this app sees at or above the
 * human rate, while a phone camera delivers ~30 frames/s and the display ~60. Faster temporal
 * vision therefore cannot be rendered — inventing an effect would misrepresent the science. The
 * stage instead states the measured rate and why it cannot be shown.
 */
object Motion {

    /** Human flicker-fusion reference used by the profiles (photopic, typical). */
    const val HUMAN_CFF_HZ = 55.0

    /** Ratio of the species' flicker-fusion frequency to human, or null when unknown. */
    fun relativeTemporalResolution(profile: AnimalVisionProfile): Double? =
        profile.motion.temporalResolutionHz?.let { it / HUMAN_CFF_HZ }

    /**
     * The disclosure shown while the MOTION stage is active, or null when the species is not
     * meaningfully faster than human (nothing to explain) or its rate is unknown.
     */
    fun disclosure(profile: AnimalVisionProfile): String? {
        val hz = profile.motion.temporalResolutionHz ?: return null
        val ratio = hz / HUMAN_CFF_HZ
        if (ratio < 1.2) return null
        return "Sees flicker up to ≈${hz.toInt()} Hz (≈${"%.1f".format(java.util.Locale.US, ratio)}× human) — " +
            "faster than any phone camera or screen, so it is described, not shown."
    }
}
