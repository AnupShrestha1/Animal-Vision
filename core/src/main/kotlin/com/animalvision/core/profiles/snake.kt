package com.animalvision.core.profiles

import com.animalvision.core.model.AcuityModel
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.ColorVisionModel
import com.animalvision.core.model.Confidence
import com.animalvision.core.model.ConfidenceLevels
import com.animalvision.core.model.FieldOfViewModel
import com.animalvision.core.model.InfraredModel
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
import com.animalvision.core.model.UltravioletModel

/**
 * Snake vision (spec §14). Modeled on a PIT VIPER for the thermal overlay and a colubrid-style
 * short+long wave dichromat for color. Pit organs exist ONLY in pythons, boas, and pit vipers;
 * an RGB phone camera cannot detect thermal IR, so thermal is always a labeled simulation.
 */
val snakeProfile = AnimalVisionProfile(
    id = "snake",
    name = "Snake (Pit Viper model)",
    scientificName = "Crotalinae (model) / Colubridae (color)",
    description = "Short+long-wave color vision; low-light adapted; separate IR channel via pit organs (visualized as a simulation).",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.SPECIAL, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.BRIGHTNESS, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.SPECIAL, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 2,
        photoreceptorTypes = listOf("SWS1 UV/violet", "LWS"),
        sensitivities = listOf(
            SpectralSensitivity("S", 360.0),
            SpectralSensitivity("L", 540.0),
        ),
        discriminationAxes = listOf("S-vs-L", "luminance"),
        lutNotes = "Most snakes are functionally dichromatic; some diurnal colubrids have more cone types. Species variance is disclosed in the UI.",
    ),
    acuity = AcuityModel(cutoffCpd = 4.0, relativeAcuity = 0.13, centralScalar = 1.0, peripheralScalar = 0.8),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 280.0,
        binocularWidthDeg = 35.0,
        blindSpotLabels = listOf("behind the head"),
        notes = "Approximate lateral-eye geometry; species vary widely.",
    ),
    lowLight = LowLightModel(enabled = true, sensitivity = 5.0, shadowLift = 0.5, maxDesaturation = 0.7, noiseSigma = 0.018),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.0, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(isSimulation = true, falseColorLabel = "Simulated short-wave channel (species-dependent)"),
        infrared = InfraredModel(
            speciesHasPitOrgan = true,
            needsHardware = true,
            label = "Thermal SIMULATION — pit vipers detect 5–30 µm IR via pit organs; an RGB camera cannot. Real thermal mode requires a thermal sensor.",
        ),
        polarizationSimulation = false,
        notes = listOf(
            "Pit organs evolved independently in pythons, boas, and pit vipers — NOT all snakes.",
            "IR and visual signals converge in the optic tectum (Newman & Hartline 1982).",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Simões et al. (2016) Mol. Biol. Evol. 33:2483-2495", type = SourceType.MEASURED),
            SourceRef("Hauzman et al. (2017) BMC Evol. Biol. 17:249", doi = "10.1186/s12862-017-1110-0", type = SourceType.MEASURED),
            SourceRef("Gracheva et al. (2010) Nature 464:1006-1011", doi = "10.1038/nature08943", type = SourceType.MEASURED),
            SourceRef("Newman & Hartline (1982) Scientific American 246(3):116-127", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.STRONG,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Thermal is a labeled SIMULATION; true thermal requires hardware that current phones lack.",
            "Not all snakes have pit organs — the profile models pit vipers explicitly.",
            "Color vision varies hugely across snake lineages; one dichromat model is shown.",
        ),
    ),
)
