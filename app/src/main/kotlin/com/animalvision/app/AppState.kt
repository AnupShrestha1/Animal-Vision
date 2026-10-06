package com.animalvision.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.profiles.profileRegistry
import com.animalvision.core.profiles.getProfile

/** Display modes for the before/after control (spec §28). */
object DisplayMode {
    const val NORMAL = 0
    const val ANIMAL = 1
    const val SPLIT = 2
}

/** Single source of truth for the camera screen; observed by Compose and read by the renderer. */
class AppState {
    var currentProfileId by mutableStateOf(profileRegistry.first().id)
        private set
    var displayMode by mutableIntStateOf(DisplayMode.ANIMAL)
        private set
    var holdingNormal by mutableStateOf(false)
        private set
    var qualityTier by mutableStateOf(QualityTier.BALANCED)
        private set
    var educationalMode by mutableStateOf(true)
    var lowLightEnabled by mutableStateOf(true)
    var motionEnabled by mutableStateOf(true)
    var showSettings by mutableStateOf(false)
    var showInfo by mutableStateOf(false)
    var needsOnboarding by mutableStateOf(true)
    var isRecording by mutableStateOf(false)
    var fps by mutableStateOf(0.0)
    var capabilityNote by mutableStateOf<String?>(null)
    var cameraFacingBack by mutableStateOf(true)
    var torchOn by mutableStateOf(false)
    var hasFlash by mutableStateOf(false)
    /** Transient status line (photo saved, errors, ...) shown as a toast-style label. */
    var statusMessage by mutableStateOf<String?>(null)
        private set

    val profile: AnimalVisionProfile get() = getProfile(currentProfileId) ?: profileRegistry.first()
    val profiles: List<AnimalVisionProfile> get() = profileRegistry

    fun selectProfile(id: String) {
        if (getProfile(id) != null) currentProfileId = id
    }

    fun setHoldToCompare(holding: Boolean) {
        holdingNormal = holding
    }

    fun announce(message: String) {
        statusMessage = message
    }

    fun clearStatus() {
        statusMessage = null
    }

    fun setMode(mode: Int) {
        displayMode = mode
    }

    fun markOnboardingDone() {
        needsOnboarding = false
    }

    fun setTier(tier: QualityTier) {
        qualityTier = tier
    }

    /** The effective mode for the renderer (0 = normal, 1 = animal, 2 = split). */
    fun effectiveMode(): Int = if (holdingNormal) DisplayMode.NORMAL else displayMode
}