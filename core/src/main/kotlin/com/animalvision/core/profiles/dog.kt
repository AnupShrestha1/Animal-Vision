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
 * Canine dichromatic vision (spec §9).
 * Cones S≈429nm / L≈555nm (Neitz, Geist & Jacobs 1989); acuity breed-dependent
 * (Lind et al. 2017); FOV ≈240°, binocular overlap 30–60° (Miller & Murphy 1995).
 */
val dogProfile = AnimalVisionProfile(
    id = "dog",
    name = "Dog",
    scientificName = "Canis lupus familiaris",
    description = "Dichromatic vision with reduced red/green discrimination, wide field, tapetal low-light gain.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.EXPOSURE, StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.EXPOSURE, StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.ACUITY, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 2,
        photoreceptorTypes = listOf("SWS1", "LWS"),
        sensitivities = listOf(
            SpectralSensitivity("S", 429.0),
            SpectralSensitivity("L", 555.0),
        ),
        discriminationAxes = listOf("S-vs-L", "luminance"),
        lutNotes = "Dichromat receptor-catch mapping (S,L). Red/green pairs converge because their S/L ratios collide.",
    ),
    acuity = AcuityModel(cutoffCpd = 8.0, relativeAcuity = 0.27, centralScalar = 1.0, peripheralScalar = 0.85),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 240.0,
        binocularWidthDeg = 45.0,
        blindSpotLabels = listOf("optic disc (physiological blind spot, small)"),
        notes = "Breed-dependent: dolichocephalic ≈270°, brachycephalic ≈240°, overlap 30–60°.",
    ),
    lowLight = LowLightModel(enabled = true, sensitivity = 2.5, shadowLift = 0.25, maxDesaturation = 0.25, noiseSigma = 0.008),
    motion = MotionModel(temporalResolutionHz = 75.0, motionSensitivity = 1.2, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf()),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Neitz, Geist & Jacobs (1989) Visual Neuroscience 3:119-125", doi = "10.1017/S0952523800004370", type = SourceType.MEASURED),
            SourceRef("Jacobs et al. (1993) Visual Neuroscience 10:173-180", type = SourceType.MEASURED),
            SourceRef("Lind et al. (2017) PLoS ONE 12(12):e0188557", doi = "10.1371/journal.pone.0188557", type = SourceType.MEASURED),
            SourceRef("Miller & Murphy (1995) JAVMA 207:1623-1634", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.STRONG,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.STRONG,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Canine color vision varies modestly between breeds; acuity range 1.9–11.5 c/deg (breed-dependent).",
            "Field of view and binocular overlap scale with skull shape.",
            "Subjective perception is unobservable; this is a scientific visualization.",
        ),
    ),
)
