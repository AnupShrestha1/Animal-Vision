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
 * Common octopus vision — a true monochromat. Eight coastal coleoid cephalopods are
 * monochromatic with λmax 484–505 nm (Chung & Marshall 2016); the profile models the
 * mid-point (495 nm), flagged MODELED. Octopus is color-blind behaviorally
 * (Messenger 1977) yet detects polarization (Chiou et al. 2007; Temple et al. 2012).
 * The display projection is achromatic grayscale — one pigment means no hue axis.
 */
val octopusProfile = AnimalVisionProfile(
    id = "octopus",
    name = "Octopus",
    scientificName = "Octopus vulgaris",
    description = "True monochromat: one pigment, no hue axis — yet polarization-aware, and camouflaged in colours it cannot see.",
    profileVersion = "1.0.0",
    researchUpdated = "2026-10-13",
    pipeline = PipelineSpec(
        stages = listOf(
            StageId.COLOR, StageId.ACUITY, StageId.SPECIAL, StageId.TONEMAP,
        ),
        qualityVariants = mapOf(
            QualityTier.HIGH to listOf(StageId.COLOR, StageId.ACUITY, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.BALANCED to listOf(StageId.COLOR, StageId.SPECIAL, StageId.TONEMAP),
            QualityTier.PERFORMANCE to listOf(StageId.COLOR, StageId.TONEMAP),
        ),
    ),
    colorVision = ColorVisionModel(
        photoreceptorCount = 1,
        photoreceptorTypes = listOf("Rh (single class)"),
        sensitivities = listOf(
            SpectralSensitivity("R", 495.0),
        ),
        discriminationAxes = listOf("luminance"),
        lutNotes = "True monochromat: one pigment class maps to luminance only — no hue dimension is rendered, because wavelength ratios are unavailable to the eye. λmax 495 nm is the mid-point of the measured coastal-coleoid range 484–505 nm (Chung & Marshall 2016), flagged as a modeled value.",
    ),
    acuity = AcuityModel(cutoffCpd = 4.0, relativeAcuity = 0.133, centralScalar = 1.0, peripheralScalar = 0.8),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 270.0,
        binocularWidthDeg = 30.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate from laterally placed camera-type eyes; direct visual-field measurements for O. vulgaris are sparse in the accessible literature.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.0, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = null,
        infrared = null,
        polarizationSimulation = true,
        notes = listOf(
            "Cephalopods detect the polarization state of reflected light; the app's polarization overlay is a simulation — an RGB phone sensor cannot measure polarization.",
            "Pupil shape (slit to figure-of-eight) adapts with light level as anatomy, not a pipeline stage — it is not simulated.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Chung & Marshall (2016) Proc. R. Soc. B 283:20161346 (coastal coleoids monochromatic, λmax 484–505 nm)", doi = "10.1098/rspb.2016.1346", type = SourceType.MEASURED),
            SourceRef("Messenger (1977) Nature 267:804-805 (behavioral color blindness in Octopus)", type = SourceType.MEASURED),
            SourceRef("Stubbs & Oakley (2016) PNAS 113:8206-8211 (chromatic-aberration color-cue hypothesis in color-blind animals)", doi = "10.1073/pnas.1524578113", type = SourceType.INFERRED),
            SourceRef("Chiou, Mäthger, Hanlon & Cronin (2007) J. Exp. Biol. 210:3624-3635 (polarized-light reflections)", type = SourceType.MEASURED),
            SourceRef("Temple et al. (2012) Curr. Biol. 22:R121-R122 (high-resolution polarization vision in cuttlefish)", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.STRONG,
            overall = Confidence.UNCERTAIN,
        ),
        limitations = listOf(
            "The single λmax (495 nm) is modeled at the mid-point of the measured coleoid range 484–505 nm; the species' exact peak was not directly adopted.",
            "Acuity is a flagged modeling choice: cephalopod resolution estimates are method-dependent (optomotor vs shape discrimination) and sparse.",
            "Aquatic light fields (depth, turbidity, scattering) are not modeled — the low-light stage is disabled rather than guessed.",
            "The polarization overlay is a labeled simulation; an RGB camera cannot measure polarization.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)