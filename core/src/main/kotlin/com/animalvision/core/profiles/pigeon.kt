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
 * Rock pigeon vision. Tetrachromat with molecularly measured pigments: SWS1 393 nm
 * (mediates the documented 320–380 nm UV response), SWS2 448, RH2 503, LWS 559
 * (Kawamura, Blow & Yokoyama 1999). Acuity from grating bar-width thresholds
 * 1.16–4.0 arcmin (Blough 1971) — the conservative end is modeled.
 */
val pigeonProfile = AnimalVisionProfile(
    id = "pigeon",
    name = "Pigeon",
    scientificName = "Columba livia",
    description = "UV-edge tetrachromat: four measured pigments, real near-UV response, prey-eye panoramic field.",
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
            SpectralSensitivity("VS", 393.0),
            SpectralSensitivity("S", 448.0),
            SpectralSensitivity("M", 503.0),
            SpectralSensitivity("L", 559.0),
        ),
        discriminationAxes = listOf("VS-vs-L", "M-vs-L", "luminance"),
        lutNotes = "Avian VS/S/M/L tetrachromat via the bird display projection. The 393 nm SWS1 pigment sits at the UV edge (pigeons behaviorally respond to 320–380 nm); on a phone the short-wave end is a blue-channel proxy, never measured UV.",
    ),
    acuity = AcuityModel(cutoffCpd = 7.5, relativeAcuity = 0.25, centralScalar = 1.0, peripheralScalar = 0.8),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 340.0,
        binocularWidthDeg = 60.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate deep-lateral eye geometry; direct field measurements for C. livia are sparse. Geometry is simulated — content outside the phone lens is never fabricated.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.3, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(
            isSimulation = true,
            falseColorLabel = "UV proxy — pigeons respond to true 320–380 nm light; a phone camera cannot",
        ),
        infrared = null,
        polarizationSimulation = false,
        notes = listOf(
            "Pigeon near-UV sensitivity comes from the SWS1 pigment plus behavioral discrimination (Wright 1972); the app renders it as a labeled short-wave proxy.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Kawamura, Blow & Yokoyama (1999) Genetics 153:1839-1850", doi = "10.1093/genetics/153.4.1839", type = SourceType.MEASURED),
            SourceRef("Blough (1971) J. Exp. Anal. Behav. 15:57", doi = "10.1901/jeab.1971.15-57", type = SourceType.MEASURED),
            SourceRef("Wright (1972) Vision Research 12:1447-1464", type = SourceType.MEASURED),
            SourceRef("Land & Nilsson (2012) Animal Eyes, 2nd ed., OUP (field geometry convention)", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Acuity comes from bar-width thresholds of 1.16–4.0 arcmin (Blough 1971); 7.5 c/deg is the conservative end of that spread — individuals resolved finer.",
            "Total field (~340°) is eye-placement geometry, not a direct field measurement for this species.",
            "The 320–380 nm UV response cannot be measured by a phone; the short-wave channel is a labeled proxy.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)