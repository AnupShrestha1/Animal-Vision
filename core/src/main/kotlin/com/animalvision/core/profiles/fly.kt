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
import com.animalvision.core.model.UltravioletModel

/**
 * Fruit fly vision. Trichromat UV/blue/green with in-vivo ERG peak estimates
 * Rh3 345 / Rh5 440 / Rh1 485 nm (Sharkey et al. 2020). Temporal vision is the
 * signature: dark-adapted flicker fusion 60–100 Hz in Drosophila hydei, rising
 * 40–70% when light-adapted (Miall 1978) — the profile shows the conservative
 * upper dark-adapted figure. Compound-eye detail is coarse by design.
 */
val flyProfile = AnimalVisionProfile(
    id = "fly",
    name = "Fruit Fly",
    scientificName = "Drosophila melanogaster",
    description = "UV-blue-green trichromat with fast fly-eye temporal vision and coarse compound-eye detail.",
    profileVersion = "1.0.0",
    researchUpdated = "2026-10-13",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.ACUITY, StageId.MOTION, StageId.SPECIAL, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.ACUITY, StageId.MOTION, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.ACUITY, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.ACUITY, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 3,
        photoreceptorTypes = listOf("UV", "Blue", "Green"),
        sensitivities = listOf(
            SpectralSensitivity("U", 345.0),
            SpectralSensitivity("B", 440.0),
            SpectralSensitivity("G", 485.0),
        ),
        discriminationAxes = listOf("UV-vs-G", "B-vs-G", "luminance"),
        lutNotes = "Fly trichromat (UV/B/G) with the same documented UV false-color convention as the bee: UV rendered from the camera's blue channel (no phone UV sensor). The R1–6 motion pathway is green-sensitive (Rh1 ≈485 nm in vivo).",
    ),
    acuity = AcuityModel(cutoffCpd = 0.4, relativeAcuity = 0.0133, centralScalar = 1.0, peripheralScalar = 0.5, compoundEye = true),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 300.0,
        binocularWidthDeg = 50.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate compound-eye coverage (spherical convention, as with the honeybee); direct field measurements for this species are sparse.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = 100.0, motionSensitivity = 1.7, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(
            isSimulation = true,
            falseColorLabel = "UV proxy — fruit flies detect true UV; a phone camera cannot",
        ),
        infrared = null,
        polarizationSimulation = false,
        notes = listOf(
            "Fruit flies are 'fast-eyed' (biphasic ERG); the displayed flicker figure is the upper end of the measured dark-adapted range, not a maximum.",
            "Compound-eye motion processing (elementary motion detectors) cannot be reproduced by a camera pipeline — only the temporal-rate figure is shown.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Sharkey, Blanco, Leibowitz, Pinto-Benito & Wardill (2020) Scientific Reports 10:18242", doi = "10.1038/s41598-020-74742-1", type = SourceType.MEASURED),
            SourceRef("Miall (1978) Physiological Entomology (flicker fusion of six insects incl. Drosophila hydei)", doi = "10.1111/j.1365-3032.1978.tb00139.x", type = SourceType.MEASURED),
            SourceRef("Land & Nilsson (2012) Animal Eyes, 2nd ed., OUP (compound-eye geometry convention)", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Flicker fusion measured 60–100 Hz dark-adapted (D. hydei) and 40–70% higher when light-adapted (Miall 1978); 100 Hz is displayed as a conservative photopic figure.",
            "Acuity is modeled from compound-eye angular sampling, not a behavioral measurement of D. melanogaster.",
            "The UV channel is a blue-channel proxy — a phone camera cannot measure ultraviolet.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)