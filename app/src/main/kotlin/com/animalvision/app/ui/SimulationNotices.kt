package com.animalvision.app.ui

import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.stage.Fov
import com.animalvision.core.stage.Motion
import com.animalvision.core.stage.SpecialVision

/**
 * One honesty notice: a short [badge] for the viewfinder and the full [detail] for the info
 * panel and screen readers. Every simulated or not-renderable aspect of a profile produces one,
 * so no surface can show a channel without its disclosure.
 */
data class SimulationNotice(val badge: String, val detail: String)

fun simulationNotices(profile: AnimalVisionProfile): List<SimulationNotice> = buildList {
    if (profile.specialVision.ultraviolet != null) {
        add(
            SimulationNotice(
                "UV · simulated",
                "The UV channel is a labeled SIMULATION — phone cameras cannot see ultraviolet; " +
                    "it is a proxy derived from the blue channel.",
            ),
        )
    }
    if (profile.specialVision.infrared != null) {
        add(
            SimulationNotice(
                "Thermal · simulated",
                "The thermal view is a labeled SIMULATION — an RGB camera cannot detect infrared.",
            ),
        )
    }
    if (profile.specialVision.polarizationSimulation) {
        add(SimulationNotice("Polarization · simulated", SpecialVision.POLARIZATION_DISCLOSURE))
    }
    Motion.disclosure(profile)?.let { add(SimulationNotice("Fast vision · described", it)) }
    if (profile.fieldOfView.totalHorizontalDeg > WIDE_FIELD_DEG) {
        add(
            SimulationNotice(
                "Wide field · simulated",
                "This animal sees about ${profile.fieldOfView.totalHorizontalDeg.toInt()}° around; " +
                    "your phone's lens covers ~${Fov.VIEW_FOV_DEGREES.toInt()}°. The wider field is shown " +
                    "as a geometric warp, never as picture the camera did not capture.",
            ),
        )
    }
}

/** Fields wider than the human ~180° get a "wide field" disclosure. */
private const val WIDE_FIELD_DEG = 190.0

/** Name without the parenthetical qualifier, for compact labels ("Eagle (Wedge-tailed)" -> "Eagle"). */
val AnimalVisionProfile.shortName: String
    get() = name.substringBefore(" (").trim()
