package com.animalvision.core.profiles

import com.animalvision.core.model.AcuityModel
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.ColorVisionModel
import com.animalvision.core.model.Confidence
import com.animalvision.core.model.ConfidenceLevels
import com.animalvision.core.model.FieldOfViewModel
import com.animalvision.core.model.LowLightModel
import com.animalvision.core.model.MotionModel
import com.animalvision.core.model.PipelineSpec
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.ScientificData
import com.animalvision.core.model.SourceRef
import com.animalvision.core.model.SourceType
import com.animalvision.core.model.SpecialVisionModel
import com.animalvision.core.model.SpectralSensitivity
import com.animalvision.core.model.StageId

/**
 * Chicken (domestic fowl) vision. Tetrachromat with four measured cone pigments
 * ≈415/460/505/562 nm (multiple extraction/ERG studies compiled by Bowmaker, Wilkie
 * & Hunt 1999) plus colored oil droplets — the droplets are long-pass filters that the
 * A1 pigment template does not model, which is disclosed as a limitation. Hen grating
 * acuity 4–6 c/deg (Demello et al. 1992).
 */
val chickenProfile = AnimalVisionProfile(
    id = "chicken",
    name = "Chicken",
    scientificName = "Gallus gallus domesticus",
    description = "Farmyard tetrachromat: four measured cone pigments behind coloured oil droplets, ~4–6 c/deg detail, side-set panoramic eyes.",
    profileVersion = "1.0.0",
    researchUpdated = "2026-10-13",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.FOV, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 4,
        photoreceptorTypes = listOf("VS", "S", "M", "L"),
        sensitivities = listOf(
            SpectralSensitivity("VS", 415.0),
            SpectralSensitivity("S", 460.0),
            SpectralSensitivity("M", 505.0),
            SpectralSensitivity("L", 562.0),
        ),
        discriminationAxes = listOf("VS-vs-L", "M-vs-L", "luminance"),
        lutNotes = "Avian VS/S/M/L tetrachromat via the bird display projection. Life oil droplets act as long-pass filters narrowing each cone — the A1 template models pigments only; droplet filtering is disclosed, not simulated.",
    ),
    acuity = AcuityModel(cutoffCpd = 5.0, relativeAcuity = 0.167, centralScalar = 1.0, peripheralScalar = 0.75),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 300.0,
        binocularWidthDeg = 40.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate lateral-eye geometry (same convention as the other prey-field entries); direct field measurements vary by study and head posture.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.1, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf()),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Bowmaker, Wilkie & Hunt (1999) Proc. 22 Int. Ornithol. Congr. 2729-2742 (chicken 415/460/505/562 nm compilation)", type = SourceType.MEASURED),
            SourceRef("Demello, Foster & Reese (1992) hen grating acuity 4–6 c/deg", type = SourceType.MEASURED),
            SourceRef("Land & Nilsson (2012) Animal Eyes, 2nd ed., OUP (field geometry convention)", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.STRONG,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Colored oil droplets filter each cone class in life (long-pass); the app models pigment curves only — droplet filtering is not simulated.",
            "Total field (~300°) is eye-placement geometry, not a direct field measurement for this species.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)