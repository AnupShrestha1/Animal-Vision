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
 * Honeybee vision (spec §13). Trichromat UV≈344 / blue≈436 / green≈556 nm (Peitsch et al. 1992).
 * The phone's RGB sensor cannot detect UV; the UV channel is an explicitly labeled proxy/simulation.
 */
val beeProfile = AnimalVisionProfile(
    id = "bee",
    name = "Honeybee",
    scientificName = "Apis mellifera",
    description = "UV-blue-green trichromat: color axes shifted shortward, coarse acuity, very fast temporal vision, polarization-sensitive.",
    profileVersion = "1.0.0",
    researchUpdated = "2025-06-01",
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
            SpectralSensitivity("U", 344.0),
            SpectralSensitivity("B", 436.0),
            SpectralSensitivity("G", 556.0),
        ),
        discriminationAxes = listOf("U-vs-G", "B-vs-G", "luminance"),
        lutNotes = "UV/B/G trichromat mapping. UV rendered as standard false-color (magenta). UV is a BLUE-CHANNEL PROXY — not measured UV.",
    ),
    acuity = AcuityModel(cutoffCpd = 0.5, relativeAcuity = 0.0167, centralScalar = 1.0, peripheralScalar = 0.5),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 300.0,
        binocularWidthDeg = 15.0,
        blindSpotLabels = listOf("behind head"),
        notes = "Panoramic compound eye; values approximate (spherical coverage). Frontal binocular overlap is small.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = 250.0, motionSensitivity = 1.6, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(
            isSimulation = true,
            falseColorLabel = "UV-inspired simulation — not actual UV sensor data",
        ),
        infrared = null,
        polarizationSimulation = true,
        notes = listOf(
            "Bees use skylight polarization for navigation (dorsal rim area). The app's polarization overlay is a simulation, not a measurement.",
            "Nectar-guide illustrations are educational and are never presented as live UV data.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Peitsch et al. (1992) J. Comp. Physiol. A 170:23-40", type = SourceType.MEASURED),
            SourceRef("Hempel de Ibarra, Vorobyev & Menzel (2014) J. Comp. Physiol. A 200:411-433", doi = "10.1007/s00359-014-0915-1", type = SourceType.MEASURED),
            SourceRef("Rigosi, Wiederman & O'Carroll (2017) Scientific Reports 7:45972", doi = "10.1038/srep45972", type = SourceType.MEASURED),
            SourceRef("Wehner (1981) Handbook of Sensory Physiology VII/6C:287-616", type = SourceType.MEASURED),
            SourceRef("Srinivasan & Lehrer (1984) J. Comp. Physiol. 154:297-305", type = SourceType.MEASURED),
            SourceRef("Wehner & Labhart (2006) Invertebrate Vision, Cambridge Univ. Press", type = SourceType.MEASURED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.WELL_ESTABLISHED,
            acuity = Confidence.WELL_ESTABLISHED,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "Phone cameras have no usable UV response; the UV channel is a proxy and is always labeled as such.",
            "Polarization is simulated from solar-position heuristics, not measured.",
            "Compound-eye neural processing (motion vision, superposition) cannot be reproduced.",
        ),
    ),
)
