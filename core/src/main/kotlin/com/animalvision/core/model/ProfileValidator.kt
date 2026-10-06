package com.animalvision.core.model

/** Result of validating a profile: human-readable issues, empty = valid. */
data class ValidationReport(val profileId: String, val issues: List<String>) {
    val isValid: Boolean get() = issues.isEmpty()
}

/**
 * Validates a profile against the scientific contract: no invented values, no out-of-range
 * parameters, every cataloged stage is real, sources and limitations are present.
 */
object ProfileValidator {

    fun validate(profile: AnimalVisionProfile): ValidationReport {
        val issues = mutableListOf<String>()

        // ---- identity ---------------------------------------------------
        if (profile.id.isBlank()) issues += "id must be non-empty"
        if (profile.name.isBlank()) issues += "name must be non-empty"
        if (profile.scientificName.isBlank()) issues += "scientificName must be non-empty"
        if (profile.profileVersion.isBlank()) issues += "profileVersion must be set (semver)"
        if (profile.researchUpdated.isBlank()) issues += "researchUpdated must be set (ISO date)"

        // ---- color vision ------------------------------------------------
        val cv = profile.colorVision
        if (cv.photoreceptorCount != cv.sensitivities.size) {
            issues += "colorVision.photoreceptorCount (${cv.photoreceptorCount}) != sensitivities.size (${cv.sensitivities.size})"
        }
        if (cv.photoreceptorCount !in 1..4) issues += "photoreceptorCount outside 1..4"
        cv.sensitivities.forEach { s ->
            if (s.peakNm < 300.0 || s.peakNm > 750.0) issues += "peak ${s.id}=${s.peakNm}nm outside biological range 300..750"
            if (s.template != "A1") issues += "unknown opsin template '${s.template}' (only 'A1' supported)"
        }
        if (cv.discriminationAxes.isEmpty()) issues += "colorVision.discriminationAxes must be non-empty"
        if (cv.lutSize !in 8..64) issues += "lutSize should be in 8..64"

        // ---- acuity -------------------------------------------------------
        val ac = profile.acuity
        if (ac.cutoffCpd <= 0.0 || ac.cutoffCpd > 200.0) issues += "acuity.cutoffCpd outside (0,200]"
        if (ac.relativeAcuity <= 0.0) issues += "acuity.relativeAcuity must be > 0"
        if (ac.centralScalar < 0.5 || ac.centralScalar > 2.0) issues += "acuity.centralScalar outside 0.5..2.0"

        // ---- FOV ----------------------------------------------------------
        val fov = profile.fieldOfView
        if (fov.totalHorizontalDeg <= 1.0 || fov.totalHorizontalDeg > 360.0) {
            issues += "fieldOfView.totalHorizontalDeg outside (1,360]"
        }
        if (fov.binocularWidthDeg < 0.0 || fov.binocularWidthDeg > fov.totalHorizontalDeg) {
            issues += "fieldOfView.binocularWidthDeg must be within [0,total]"
        }

        // ---- low light ----------------------------------------------------
        val ll = profile.lowLight
        if (ll.sensitivity <= 0.0) issues += "lowLight.sensitivity must be > 0"
        if (ll.shadowLift !in 0.0..1.0) issues += "lowLight.shadowLift outside 0..1"
        if (ll.maxDesaturation !in 0.0..1.0) issues += "lowLight.maxDesaturation outside 0..1"
        if (ll.noiseSigma < 0.0) issues += "lowLight.noiseSigma must be >= 0"

        // ---- motion -------------------------------------------------------
        profile.motion.temporalResolutionHz?.let { if (it <= 0) issues += "motion.temporalResolutionHz must be > 0 (or null)" }

        // ---- special vision -----------------------------------------------
        val sv = profile.specialVision
        sv.ultraviolet?.let { uv ->
            if (uv.isSimulation && uv.falseColorLabel.isBlank()) issues += "UV simulation requires a disclosure label"
        }
        sv.infrared?.let { ir ->
            if (ir.speciesHasPitOrgan && ir.needsHardware && ir.label.isBlank()) issues += "IR profile requires a hardware-disclosure label"
        }

        // ---- pipeline -----------------------------------------------------
        val known = StageId.entries.toSet()
        profile.pipeline.stages.forEach { s ->
            if (s !in known) issues += "unknown pipeline stage $s"
        }
        profile.pipeline.qualityVariants.forEach { (tier, stages) ->
            if (stages.any { it !in profile.pipeline.stages }) issues += "$tier variant references a stage not in the base pipeline"
        }

        // ---- science metadata --------------------------------------------
        if (profile.scientificData.sources.isEmpty()) issues += "scientificData.sources must be non-empty"
        if (profile.scientificData.limitations.isEmpty()) issues += "scientificData.limitations must be non-empty"
        if (profile.scientificData.confidence.overall.stars !in 1..5) issues += "overall confidence invalid"

        return ValidationReport(profile.id, issues)
    }
}
