package com.animalvision.core.model

/**
 * Scientific confidence ratings — see docs/SCIENCE.md for the five-level scale.
 */
enum class Confidence(val stars: Int) {
    WELL_ESTABLISHED(5),
    STRONG(4),
    REASONABLE(3),
    UNCERTAIN(2),
    SPECULATIVE(1);

    companion object {
        fun of(stars: Int): Confidence = entries.first { it.stars == stars }
    }
}

/** Whether a scientific parameter is directly measured, inferred, or a modeling approximation. */
enum class SourceType { MEASURED, INFERRED, MODELED }

/**
 * Stages of the GPU pipeline (spec §7). Only those listed in a profile are activated.
 * Exposure/white-point normalization is not a stage of its own: the camera's auto-exposure
 * and the color LUT's von-Kries white normalization already perform it.
 */
enum class StageId {
    COLOR,
    BRIGHTNESS,
    ACUITY,
    FOV,
    MOTION,
    SPECIAL,
    TONEMAP
}

enum class QualityTier(val label: String) {
    HIGH("High"),
    BALANCED("Balanced"),
    PERFORMANCE("Performance")
}

data class SourceRef(
    val cite: String,
    val doi: String? = null,
    val type: SourceType,
)

data class ConfidenceLevels(
    val color: Confidence,
    val acuity: Confidence,
    val fov: Confidence,
    val lowLight: Confidence,
    val overall: Confidence,
)

/** One modeled photoreceptor class. [template] is the opsin template used ("A1" = Govardovskii 2000). */
data class SpectralSensitivity(
    val id: String,
    val peakNm: Double,
    val template: String = "A1",
)

data class ColorVisionModel(
    val photoreceptorCount: Int,
    val photoreceptorTypes: List<String>,
    val sensitivities: List<SpectralSensitivity>,
    /** The discrimination axes available to the species (e.g. ["S-vs-L", "luminance"]). */
    val discriminationAxes: List<String>,
    val lutSize: Int = 33,
    val lutNotes: String,
)

data class AcuityModel(
    /** Behavioral acuity limit in cycles/degree (source values in SCIENCE.md). */
    val cutoffCpd: Double,
    /** Multiplier vs a nominal human reference of ~30 c/deg. */
    val relativeAcuity: Double,
    /** Central (foveal/area-centralis) scalar; > 1 means a high-acuity region exists. */
    val centralScalar: Double,
    /** Peripheral scalar (< 1 means peripheral falloff). */
    val peripheralScalar: Double,
)

data class FieldOfViewModel(
    val totalHorizontalDeg: Double,
    /** Angular width of the forward binocular cone (degrees). */
    val binocularWidthDeg: Double,
    val blindSpotLabels: List<String>,
    val notes: String,
)

data class LowLightModel(
    val enabled: Boolean,
    /** Relative light sensitivity vs human (=1). */
    val sensitivity: Double,
    /** Shadow tone lift in [0,1]; contrast-preserving (never a plain brightness boost). */
    val shadowLift: Double,
    /** Max desaturation toward rod-vision in [0,1], applied as luminance drops. */
    val maxDesaturation: Double,
    val noiseSigma: Double,
)

data class MotionModel(
    /** Flicker-fusion frequency (Hz); null if unknown. See SCIENCE.md. */
    val temporalResolutionHz: Double?,
    val motionSensitivity: Double,
    val confidence: Confidence,
)

data class UltravioletModel(
    /** True when the phone cannot measure UV and a labeled proxy/simulation is used. */
    val isSimulation: Boolean,
    val falseColorLabel: String,
)

data class InfraredModel(
    /** True for species with functional pit organs (pythons, boas, pit vipers). */
    val speciesHasPitOrgan: Boolean,
    val needsHardware: Boolean,
    val label: String,
)

data class SpecialVisionModel(
    val ultraviolet: UltravioletModel?,
    val infrared: InfraredModel?,
    val polarizationSimulation: Boolean,
    val notes: List<String>,
)

data class PipelineSpec(
    val stages: List<StageId>,
    val qualityVariants: Map<QualityTier, List<StageId>>,
)

data class ScientificData(
    val sources: List<SourceRef>,
    val confidence: ConfidenceLevels,
    val limitations: List<String>,
)

/**
 * The complete scientific model of one animal's vision (spec §6 + §42).
 * Profiles are data only — the engine consumes them.
 */
data class AnimalVisionProfile(
    val id: String,
    val name: String,
    val scientificName: String,
    val description: String,
    val profileVersion: String,
    val researchUpdated: String,
    val pipeline: PipelineSpec,
    val colorVision: ColorVisionModel,
    val acuity: AcuityModel,
    val fieldOfView: FieldOfViewModel,
    val lowLight: LowLightModel,
    val motion: MotionModel,
    val specialVision: SpecialVisionModel,
    val scientificData: ScientificData,
)
