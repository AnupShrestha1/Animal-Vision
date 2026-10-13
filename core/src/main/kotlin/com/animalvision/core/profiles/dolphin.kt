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
 * Bottlenose dolphin vision — a rod-monochromat lineage with a broken blue gene.
 * Expressed pigments: rod rhodopsin 488 nm and a single LWS cone pigment 524 nm; the
 * SWS opsin carries inactivating deletions and is not expressed (Fasick, Cronin,
 * Hunt & Robinson 1998), and genomic surveys confirm SWS1 loss across toothed and
 * baleen whales (Meredith et al. 2013). In-air/underwater acuity ≈12.6 arcmin
 * → ≈4.8 c/deg (Tomonaga et al. 2014).
 */
val dolphinProfile = AnimalVisionProfile(
    id = "dolphin",
    name = "Dolphin",
    scientificName = "Tursiops truncatus",
    description = "Monochromat of the sea: one cone pigment, a broken blue gene, and detail measured in half-degrees.",
    profileVersion = "1.0.0",
    researchUpdated = "2026-10-13",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.ACUITY, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.ACUITY, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 1,
        photoreceptorTypes = listOf("LWS (single cone class)"),
        sensitivities = listOf(
            SpectralSensitivity("L", 524.0),
        ),
        discriminationAxes = listOf("luminance"),
        lutNotes = "Rod-monochromat lineage: one expressed cone pigment (LWS 524 nm; rod rhodopsin 488 nm) with the SWS opsin broken by inactivating mutations (Fasick et al. 1998). Display projection is luminance only — no hue axis, because the blue–yellow dimension is genetically gone.",
    ),
    acuity = AcuityModel(cutoffCpd = 4.8, relativeAcuity = 0.16, centralScalar = 1.0, peripheralScalar = 0.8),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 300.0,
        binocularWidthDeg = 60.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate lateral-eye geometry; cetacean visual-field measurements are sparse. Geometry is simulated — content outside the phone lens is never fabricated.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.1, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = null,
        infrared = null,
        polarizationSimulation = false,
        notes = listOf(
            "The dolphin's broken SWS1 gene removes the mammalian blue–yellow axis entirely — this is loss of a dimension, not a colorblind 'deficiency' of two.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Fasick, Cronin, Hunt & Robinson (1998) 'The visual pigments of the bottlenose dolphin (Tursiops truncatus)' (rod 488 nm, LWS cone 524 nm, SWS1 not expressed)", type = SourceType.MEASURED),
            SourceRef("Meredith, Gatesy, Emerling, York & Springer (2013) PLoS Genetics 9(4):e1003432 (SWS1 inactivation across toothed and baleen whales)", doi = "10.1371/journal.pgen.1003432", type = SourceType.MEASURED),
            SourceRef("Tomonaga, Uwano & Saito (2014) Scientific Reports 4:3717 (dolphin acuity 12.6 arcmin)", doi = "10.1038/srep03717", type = SourceType.MEASURED),
            SourceRef("Land & Nilsson (2012) Animal Eyes, 2nd ed., OUP (field geometry convention)", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.STRONG,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.UNCERTAIN,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Underwater light attenuation (depth, turbidity, scattering) is not modeled — the low-light stage is disabled rather than guessed.",
            "Total field (~300°) is eye-placement geometry; cetacean visual-field measurements are sparse.",
            "The monochromat projection is a single-pigment luminance mapping; rod/cone adaptation switching is not separately simulated.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)