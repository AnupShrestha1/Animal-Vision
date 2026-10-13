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
 * Mantis shrimp (odontodactylid) vision — the most complex receptor array known:
 * 16 functional receptor classes across UV, colour, spatial and polarization
 * subsystems (Cronin et al. 2014), with linear AND circular polarization detection
 * (Marshall 1988; Marshall et al. 1991). Colour is channel-"labelled", not
 * opponent-compared (Thoen et al. 2014). The profile schema supports ≤4 channels, so
 * four representative peaks spanning UV→red are MODELED — explicitly not the literal
 * class measurements.
 */
val mantisshrimpProfile = AnimalVisionProfile(
    id = "mantisshrimp",
    name = "Mantis Shrimp",
    scientificName = "Odontodactylus scyllarus",
    description = "Sixteen receptor classes (four modeled here), plus linear and circular polarization vision — colour by labelled channel, not comparison.",
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
        photoreceptorCount = 4,
        photoreceptorTypes = listOf("UV", "Blue", "Green", "Red"),
        sensitivities = listOf(
            SpectralSensitivity("U", 340.0),
            SpectralSensitivity("B", 450.0),
            SpectralSensitivity("G", 500.0),
            SpectralSensitivity("R", 560.0),
        ),
        discriminationAxes = listOf("UV-vs-G", "B-vs-G", "R-vs-G", "luminance"),
        lutNotes = "Four REPRESENTATIVE classes (UV/blue/green/red) stand in for the documented 16-class mosaic; peaks are MODELED representatives, not literal class measurements. Stomatopod colour is labelled-line, not opponent — this mapping is a visualization, not a reproduction of their processing.",
    ),
    acuity = AcuityModel(cutoffCpd = 2.0, relativeAcuity = 0.0667, centralScalar = 1.0, peripheralScalar = 0.6),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 300.0,
        binocularWidthDeg = 40.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Stalked compound eyes give very wide coverage — approximate spherical-coverage figure (same convention as the honeybee entry), not a direct field measurement.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.3, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(
            isSimulation = true,
            falseColorLabel = "UV proxy — stomatopods detect true UV; a phone camera cannot",
        ),
        infrared = null,
        polarizationSimulation = true,
        notes = listOf(
            "Stomatopods detect linear AND circular polarization (birefringent receptor filters); the app's overlay simulates linear polarization only — circular cannot be represented.",
            "The 16-class mosaic includes UV filtering and serial intrarhabdomal filters that the A1 pigment template does not model.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Cronin, Bok, Marshall & Caldwell (2014) Phil. Trans. R. Soc. B 369:20130032 (16 functional receptor classes)", doi = "10.1098/rstb.2013.0032", type = SourceType.MEASURED),
            SourceRef("Thoen, How, Chiou & Marshall (2014) Science 343:411-413 ('a different form of color vision' — labelled-line channels)", type = SourceType.MEASURED),
            SourceRef("Marshall (1988) Nature 333:557-560 (colour and polarization vision system)", type = SourceType.MEASURED),
            SourceRef("Marshall, Land, King & Cronin (1991) Phil. Trans. R. Soc. B 334:33-56 (polarization detection in compound eyes)", type = SourceType.MEASURED),
            SourceRef("Four representative peaks (340/450/500/560 nm) chosen to span the documented UV→red coverage of stomatopod receptors", type = SourceType.MODELED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.UNCERTAIN,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.UNCERTAIN,
        ),
        limitations = listOf(
            "The app models 4 representative channels of the documented 16 receptor classes — the peaks are MODELED, not literal class measurements.",
            "A phone cannot measure polarization: the overlay is a labeled simulation (linear only; circular polarization is not representable).",
            "Compound-eye acuity is modeled from mid-band angular sampling — behavioral acuity for O. scyllarus is not established.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)