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
 * Japanese yellow swallowtail vision — the first proven invertebrate tetrachromat
 * (Koshitaka et al. 2008): eight receptor varieties in six classes; behavior shows the
 * UV, blue, green and red receptors carry tetrachromatic colour vision with 1–2 nm
 * wavelength discrimination. Green (515 nm) and red (575 nm) visual-pigment peaks are
 * measured (Arikawa, Scholten, Kinoshita & Stavenga 1999); UV/blue peaks are the
 * species-standard literature values (flagged INFERRED). Polarization-sensitive
 * photoreceptors are recorded (Chen, Belušič & Arikawa 2020).
 */
val butterflyProfile = AnimalVisionProfile(
    id = "butterfly",
    name = "Butterfly (Swallowtail)",
    scientificName = "Papilio xuthus",
    description = "First proven invertebrate tetrachromat: six receptor classes, 1–2 nm wavelength discrimination, polarization-sensitive eyes.",
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
        photoreceptorTypes = listOf("UV", "B", "G", "R"),
        sensitivities = listOf(
            SpectralSensitivity("UV", 340.0),
            SpectralSensitivity("B", 460.0),
            SpectralSensitivity("G", 515.0),
            SpectralSensitivity("R", 575.0),
        ),
        discriminationAxes = listOf("UV-vs-G", "B-vs-G", "R-vs-G", "luminance"),
        lutNotes = "Insect tetrachromat (UV/B/G/R) with the documented UV false-color convention: UV is a blue-channel proxy (phones have no UV sensor), green/red from measured Papilio pigment peaks. Behaviorally the four channels are used as tetrachromacy (Koshitaka et al. 2008).",
    ),
    acuity = AcuityModel(cutoffCpd = 1.0, relativeAcuity = 0.033, centralScalar = 1.0, peripheralScalar = 0.6),
    fieldOfView = FieldOfViewModel(
        totalHorizontalDeg = 250.0,
        binocularWidthDeg = 40.0,
        blindSpotLabels = listOf("behind the head (approx)"),
        notes = "Approximate compound-eye coverage — same spherical-coverage convention as the honeybee entry, not a direct field measurement for this species.",
    ),
    lowLight = LowLightModel(enabled = false, sensitivity = 1.0, shadowLift = 0.0, maxDesaturation = 0.0, noiseSigma = 0.0),
    motion = MotionModel(temporalResolutionHz = null, motionSensitivity = 1.2, confidence = Confidence.REASONABLE),
    specialVision = SpecialVisionModel(
        ultraviolet = UltravioletModel(
            isSimulation = true,
            falseColorLabel = "UV proxy — swallowtails have true UV receptors; a phone camera cannot",
        ),
        infrared = null,
        polarizationSimulation = true,
        notes = listOf(
            "Papilio photoreceptors are polarization-sensitive (Chen, Belušič & Arikawa 2020); the app's polarization overlay is a simulation, not a measurement.",
            "The retina holds six receptor classes (incl. violet, wide-band blue, double-peaked green); the app models the four channels shown to carry tetrachromatic vision.",
        ),
    ),
    scientificData = ScientificData(
        sources = listOf(
            SourceRef("Koshitaka, Kinoshita, Vorobyev & Arikawa (2008) Proc. R. Soc. B 275:947-954", doi = "10.1098/rspb.2007.1614", type = SourceType.MEASURED),
            SourceRef("Arikawa (2017) J. Physiol. 595:5457-5464 (review: receptor classes, tetrachromacy, 1–2 nm discrimination)", doi = "10.1113/JP273917", type = SourceType.MEASURED),
            SourceRef("Arikawa, Scholten, Kinoshita & Stavenga (1999) Zool. Sci. 16:17-26 (green/red pigment λmax 515/575 nm)", doi = "10.2108/zsj.16.17", type = SourceType.MEASURED),
            SourceRef("Chen, Belušič & Arikawa (2020) J. Comp. Physiol. A 206:199-216 (polarization sensitivity)", doi = "10.1007/s00359-019-01390-w", type = SourceType.MEASURED),
            SourceRef("UV ≈340 nm and blue ≈460 nm peaks: species-standard published receptor spectra (Arikawa laboratory literature)", type = SourceType.INFERRED),
        ),
        confidence = ConfidenceLevels(
            color = Confidence.STRONG,
            acuity = Confidence.UNCERTAIN,
            fov = Confidence.UNCERTAIN,
            lowLight = Confidence.WELL_ESTABLISHED,
            overall = Confidence.STRONG,
        ),
        limitations = listOf(
            "UV/blue peaks are literature-standard for P. xuthus; green/red pigment peaks (515/575 nm) were measured directly. On a phone the UV channel is a blue-proxy simulation.",
            "Violet, wide-band-blue and double-peaked-green receptor classes are omitted — the app models only the four tetrachromat channels demonstrated by behavior.",
            "Compound-eye acuity and total field are approximate: behavioral acuity varies across retinal regions and the field figure is a coverage estimate.",
            "Subjective experience is unobservable; this is a visualization of measurable properties.",
        ),
    ),
)