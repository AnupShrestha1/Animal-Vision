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
 * Eagle vision (spec §12), modeled on the Wedge-tailed eagle (Aquila audax).
 * Acuity ≈140 c/deg (Reymond 1985), two foveae. Crucially, raptor ocular media filter UV
 * (Lind et al. 2013) — we intentionally do NOT advertise UV vision.
 */
val eagleProfile = AnimalVisionProfile(
    id = "eagle",
    name = "Eagle (Wedge-tailed)",
    scientificName = "Aquila audax",
    description = "Extreme-acuity hunter: ~2× the best human acuity, foveal 'tunnel' sharpening, violet-shifted palette.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
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
        photoreceptorCount = 4,
        photoreceptorTypes = listOf("VS", "SWS", "MWS", "LWS"),
        sensitivities = listOf(
            SpectralSensitivity("VS", 415.0),
            SpectralSensitivity("S", 460.0),
            SpectralSensitivity("M", 510.0),
            SpectralSensitivity("L", 565.0),
        ),
        discriminationAxes = listOf("VS-S", "M-L", "luminance"),
        lutNotes = "Tetrachromat catch space with ocular-media-filtered shortwave ('violet' effective, per Lind et al. 2013). Deep UV is suppressed, not advertised.",
    ),
    acuity = AcuityModel(cutoffCpd = 140.0, relativeAcuity = 4.67, centralScalar = 1.6, peripheralScalar = 0.7),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 275.0,
        binocularWidthDeg = 40.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Species-specific FOV data are sparse; values are approximate, documented as such.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.4, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf("Violet-effective shortwave, not UV")),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Reymond (1985) Vision Research 25:1477-1491", doi = "10.1016/0042-6989(85)90226-3", type = SourceType.MEASURED),
            SourceRef("Lind, Mitkus, Olsson & Kelber (2013) J. Exp. Biol. 216:1819-1826", doi = "10.1242/jeb.082834", type = SourceType.MEASURED),
            SourceRef("Hart (2001) Prog. Retin. Eye Res. 20:675-703", type = SourceType.MEASURED),
            SourceRef("Tucker (2000) J. Exp. Biol. 203:3745-3754", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.WELL_ESTABLISHED,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "True 140 c/deg resolution exceeds both camera and phone display; the sharper 'foveal' zone is a bounded visualization, not real eagle acuity.",
            "Different eagle species vary; the profile explicitly models the Wedge-tailed eagle.",
            "Raptor UV sensitivity is strongly reduced by ocular media — no UV claim is made.",
        ),
    ),
)
