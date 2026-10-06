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
 * Equine vision (spec §11). Dichromat S≈428nm / L≈539nm (Carroll et al. 2001).
 * The defining feature is geometry: ≈350° horizontal FOV, 55–65° frontal binocular,
 * blind spots behind and below the muzzle (Harman et al. 1999; Saslow 1999).
 */
val horseProfile = AnimalVisionProfile(
    id = "horse",
    name = "Horse",
    scientificName = "Equus caballus",
    description = "Wide-field dichromatic vision: near-panoramic, narrow binocular strip, pronounced blind spots.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.EXPOSURE, StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.EXPOSURE, StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.FOV, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 2,
        photoreceptorTypes = listOf("SWS1", "LWS"),
        sensitivities = listOf(
            SpectralSensitivity("S", 428.0),
            SpectralSensitivity("L", 539.0),
        ),
        discriminationAxes = listOf("S-vs-L", "luminance"),
        lutNotes = "Dichromat receptor-catch mapping (S,L). L peak 539nm is short-shifted vs humans.",
    ),
    acuity = AcuityModel(cutoffCpd = 12.0, relativeAcuity = 0.4, centralScalar = 1.0, peripheralScalar = 0.75),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 350.0,
        binocularWidthDeg = 60.0,
        blindSpotLabels = listOf("directly behind the head", "directly in front / below the muzzle"),
        notes = "Monocular ≈130°/eye; binocular 55–65° (more with head raised). A phone lens cannot capture 350° — geometry is simulated, content is not fabricated.",
    ),
    lowLight = LowLightModel(enabled = true, sensitivity = 2.5, shadowLift = 0.3, maxDesaturation = 0.3, noiseSigma = 0.008),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.5, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf()),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Carroll, Murphy, Neitz, Ver Hoeve & Neitz (2001) Journal of Vision 1:80-87", doi = "10.1167/1.2.2", type = SourceType.MEASURED),
            SourceRef("Harman et al. (1999) Equine Vet. J. 31:384-390", type = SourceType.MEASURED),
            SourceRef("Timney & Keil (1992) Vision Research 32:2289-2293", type = SourceType.MEASURED),
            SourceRef("Saslow (1999) Appl. Anim. Behav. Sci. 61:273-284", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.WELL_ESTABLISHED,
            lowLight = Confidence.STRONG,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "350° FOV is physically impossible on a phone; the monocular/binocular geometry is illustrated, not recreated.",
            "Acuity estimates vary in the literature (≈20/30–20/60); a mid-range value is used.",
            "Subjective perception is unobservable.",
        ),
    ),
)
