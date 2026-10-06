package com.animalvision.core.pipeline

import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.StageId

/**
 * Pipeline orchestration (spec §7). The engine only activates the stages declared in a profile
 * for the current quality tier; stage order follows the spec.
 */
object Pipeline {

    val defaultOrder: List<StageId> = listOf(
        StageId.EXPOSURE,
        StageId.COLOR,
        StageId.BRIGHTNESS,
        StageId.ACUITY,
        StageId.FOV,
        StageId.MOTION,
        StageId.SPECIAL,
        StageId.TONEMAP,
    )

    /** Ordered, de-duplicated stage list for a profile at a given tier. */
    fun activeStages(profile: AnimalVisionProfile, tier: QualityTier = QualityTier.BALANCED): List<StageId> {
        val requested = profile.pipeline.qualityVariants[tier] ?: profile.pipeline.stages
        val set = requested.toSet()
        if (set.size != requested.size) {
            // deterministic: prefer the spec's canonical order
            return defaultOrder.filter { it in set }
        }
        return requested
    }

    fun isActive(profile: AnimalVisionProfile, stage: StageId, tier: QualityTier = QualityTier.BALANCED): Boolean =
        stage in activeStages(profile, tier)
}
