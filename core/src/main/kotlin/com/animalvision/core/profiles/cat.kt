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
 * Feline vision (spec §10). Dichromat S≈452nm / M≈556nm; requires ~1/6 the light of humans
 * (tapetum + rod-dominated retina); acuity ~10 c/deg anatomical, ~4–6 behavioral.
 * Caveat: Ringo et al. (1977) reported electrophysiological evidence of a third cone system
 * that was never confirmed molecularly — disclosed, not modeled.
 */
val catProfile = AnimalVisionProfile(
    id = "cat",
    name = "Cat",
    scientificName = "Felis catus",
    description = "Dichromatic, low-light-adapted predator vision with tapetum-driven night gain.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.FOV, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.BRIGHTNESS, StageId.ACUITY, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.BRIGHTNESS, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 2,
        photoreceptorTypes = listOf("SWS1", "MWS"),
        sensitivities = listOf(
            SpectralSensitivity("S", 452.0),
            SpectralSensitivity("M", 556.0),
        ),
        discriminationAxes = listOf("S-vs-L", "luminance"),
        lutNotes = "Dichromat receptor-catch mapping (S,M). Red/green converge; blues remain discriminable.",
    ),
    acuity = AcuityModel(cutoffCpd = 6.0, relativeAcuity = 0.2, centralScalar = 1.0, peripheralScalar = 0.9),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 200.0,
        binocularWidthDeg = 100.0,
        blindSpotLabels = listOf(),
        notes = "Binocular overlap is reported variously at 90–130° in the literature; 100° is used as midpoint.",
    ),
    lowLight = LowLightModel(enabled = true, sensitivity = 6.0, shadowLift = 0.55, maxDesaturation = 0.6, noiseSigma = 0.015),
    motion = MotionModel(temporalResolutionHz = 55.0, motionSensitivity = 1.3, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(ultraviolet = null, infrared = null, polarizationSimulation = false, notes = listOf()),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Jacobs (1993) Biological Reviews 68:413-471", type = SourceType.MEASURED),
            SourceRef("Schneeweis & Schnapf (1999) J. Neuroscience 19:10732-10741", type = SourceType.MEASURED),
            SourceRef("Wässle (1971) Vision Research 11:995-1006", type = SourceType.MEASURED),
            SourceRef("Blake, Cool & Crawford (1974) Vision Research 14:1211-1217", type = SourceType.MEASURED),
            SourceRef("Gunter (1951) J. Physiology 114:8-15", type = SourceType.MEASURED),
            SourceRef("Ringo et al. (1977) Science 198:753-754", doi = "10.1126/science.910161", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.WELL_ESTABLISHED,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Ringo et al. (1977) reported possible trichromacy; mainstream molecular finding is dichromacy — modeled here.",
            "Tapetum 'eye-shine' in the UI is a labeled visualization of the reflecting layer, not retinal physiology.",
            "Binocular stereo depth cannot be reproduced by a single camera.",
        ),
    ),
)
