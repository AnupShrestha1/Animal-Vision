package com.animalvision.core.profiles

import com.animalvision.core.model.AnimalVisionProfile

/** Ordered catalog of supported animals (spec §4). First entry is the default. */
val profileRegistry: List<AnimalVisionProfile> = listOf(
    humanProfile,
    dogProfile,
    catProfile,
    horseProfile,
    eagleProfile,
    beeProfile,
    snakeProfile,
)

private val byId: Map<String, AnimalVisionProfile> = profileRegistry.associateBy { it.id }

fun getProfile(id: String): AnimalVisionProfile? = byId[id]

fun requireProfile(id: String): AnimalVisionProfile =
    byId[id] ?: throw IllegalArgumentException("Unknown animal profile: $id")

val defaultProfile: AnimalVisionProfile get() = profileRegistry.first()
