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
 * Human reference baseline (spec §5). The phone camera is a *stand-in*, not human vision.
 * Sources: Stockman & Sharpe (2000); Land & Nilsson (2012); Caves & Johnsen (2018).
 */
val humanProfile = AnimalVisionProfile(
    id = "human",
    name = "Human",
    scientificName = "Homo sapiens",
    description = "Trichromatic reference. Near-passthrough baseline for comparison and calibration.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
    pipeline = PipelineSpec(
        stages = listOf(StageId.COLOR, StageId.TONEMAP),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 3,
        photoreceptorTypes = listOf("SWS1", "MWS", "LWS"),
        sensitivities = listOf(
            SpectralSensitivity("S", 419.0),
            SpectralSensitivity("M", 530.0),
            SpectralSensitivity("L", 559.0),
        ),
        discriminationAxes = listOf("L-M", "S-(L+M)", "luminance"),
        lutNotes = "Identity/reference transform within display gamut.",
    ),
    acuity = AcuityModel(cutoffCpd = 30.0, relativeAcuity = 1.0, centralScalar = 1.0, peripheralScalar = 1.0),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 180.0,
        binocularWidthDeg = 120.0,
        blindSpotLabels = listOf("optic disc (physiological blind spot, small)"),
        notes = "Standard reference values.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = 55.0, motionSensitivity = 1.0, confidence = Confidence.WELL_ESTABLISHED),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf()),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Stockman & Sharpe (2000) Vision Research 40:1711-1737", doi = "10.1016/S0042-6989(00)00025-9", type = SourceType.MEASURED),
            SourceRef("Land & Nilsson (2012) Animal Eyes, 2nd ed., OUP", type = SourceType.MEASURED),
            SourceRef("Caves & Johnsen (2018) Methods Ecol. Evol. 9:793-797", doi = "10.1111/2041-210X.12911", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.WELL_ESTABLISHED,
            fov = Confidence.WELL_ESTABLISHED,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.WELL_ESTABLISHED,
        ),
        limitations = listOf(
            "A phone camera is a colorimetric stand-in for human vision, not identical to a human eye.",
            "Individual variation (astigmatism, color deficiency) is not modeled.",
        ),
    ),
)
