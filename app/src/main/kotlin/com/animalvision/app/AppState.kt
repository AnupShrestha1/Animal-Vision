package com.animalvision.app

import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.profiles.getProfile
import com.animalvision.core.profiles.profileRegistry

/** A saved capture: its gallery [uri], a small [thumbnail], and whether it is a video. */
class LastCapture(val uri: Uri, val thumbnail: ImageBitmap, val isVideo: Boolean)

/** Display modes for the before/after control (spec §28). */
object DisplayMode {
    const val NORMAL = 0
    const val ANIMAL = 1
    const val SPLIT = 2
}

/**
 * Single source of truth for the camera screen; observed by Compose and read by the renderer.
 * User choices that should survive a restart (onboarding done, animal, quality, toggles) are
 * persisted to [prefs] when provided.
 */
class AppState(private val prefs: SharedPreferences? = null) {
    var currentProfileId by mutableStateOf(
        prefs?.getString(KEY_PROFILE, null)?.takeIf { getProfile(it) != null } ?: profileRegistry.first().id,
    )
        private set
    var displayMode by mutableIntStateOf(DisplayMode.ANIMAL)
        private set
    var holdingNormal by mutableStateOf(false)
        private set
    var qualityTier by mutableStateOf(
        prefs?.getString(KEY_TIER, null)?.let { name -> QualityTier.entries.firstOrNull { it.name == name } }
            ?: QualityTier.HIGH, // start at full quality; the renderer steps down if frames drop
    )
        private set

    private var educationalModeState by mutableStateOf(prefs?.getBoolean(KEY_EDUCATIONAL, true) ?: true)
    var educationalMode: Boolean
        get() = educationalModeState
        set(value) {
            educationalModeState = value
            prefs?.edit()?.putBoolean(KEY_EDUCATIONAL, value)?.apply()
        }

    private var lowLightState by mutableStateOf(prefs?.getBoolean(KEY_LOW_LIGHT, true) ?: true)
    var lowLightEnabled: Boolean
        get() = lowLightState
        set(value) {
            lowLightState = value
            prefs?.edit()?.putBoolean(KEY_LOW_LIGHT, value)?.apply()
        }

    private var showStatsState by mutableStateOf(prefs?.getBoolean(KEY_SHOW_STATS, false) ?: false)
    /** Show the fps readout (a developer aid, off by default). */
    var showStats: Boolean
        get() = showStatsState
        set(value) {
            showStatsState = value
            prefs?.edit()?.putBoolean(KEY_SHOW_STATS, value)?.apply()
        }

    private var facetMosaicState by mutableStateOf(prefs?.getBoolean(KEY_FACETS, false) ?: false)
    /** Optional ommatidial mosaic for compound-eyed species (off by default). */
    var facetMosaic: Boolean
        get() = facetMosaicState
        set(value) {
            facetMosaicState = value
            prefs?.edit()?.putBoolean(KEY_FACETS, value)?.apply()
        }

    /** Most recent saved photo/video, shown as a tappable thumbnail next to the shutter. */
    var lastCapture by mutableStateOf<LastCapture?>(null)
    /** Bumped on every shutter press; drives the capture flash. */
    var captureFlashTick by mutableIntStateOf(0)
    /** Wall-clock start of the current recording (for the REC timer), 0 when idle. */
    var recordingStartedAtMs by mutableLongStateOf(0L)

    /** Camera zoom ratio (1 = standard lens). Resets when the camera is rebound. */
    var zoom by mutableFloatStateOf(1f)
    var maxZoom by mutableFloatStateOf(1f)
    /** Split-mode divider, 0..1 from the left. */
    var splitPosition by mutableFloatStateOf(0.5f)

    var motionEnabled by mutableStateOf(true)
    var showSettings by mutableStateOf(false)
    var showInfo by mutableStateOf(false)
    var needsOnboarding by mutableStateOf(!(prefs?.getBoolean(KEY_ONBOARDED, false) ?: false))
        private set
    /** Camera permission as last observed (re-checked on resume, see MainActivity). */
    var cameraPermission by mutableStateOf(false)
    var isRecording by mutableStateOf(false)
    var fps by mutableDoubleStateOf(0.0)
    var cameraFacingBack by mutableStateOf(true)
    var torchOn by mutableStateOf(false)
    var hasFlash by mutableStateOf(false)
    /** Transient status line (photo saved, errors, ...) shown as a toast-style label. */
    var statusMessage by mutableStateOf<String?>(null)
        private set

    val profile: AnimalVisionProfile get() = getProfile(currentProfileId) ?: profileRegistry.first()
    val profiles: List<AnimalVisionProfile> get() = profileRegistry

    fun selectProfile(id: String) {
        if (getProfile(id) == null) return
        currentProfileId = id
        prefs?.edit()?.putString(KEY_PROFILE, id)?.apply()
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
        prefs?.edit()?.putBoolean(KEY_ONBOARDED, true)?.apply()
    }

    /**
     * Change the quality tier. User choices persist; automatic frame-rate step-downs pass
     * [persist] = false so a momentary slowdown doesn't lock in a lower tier for later launches.
     */
    fun setTier(tier: QualityTier, persist: Boolean = true) {
        qualityTier = tier
        if (persist) prefs?.edit()?.putString(KEY_TIER, tier.name)?.apply()
    }

    /** The effective mode for the renderer (0 = normal, 1 = animal, 2 = split). */
    fun effectiveMode(): Int = if (holdingNormal) DisplayMode.NORMAL else displayMode

    private companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_TIER = "quality_tier"
        const val KEY_EDUCATIONAL = "educational_mode"
        const val KEY_LOW_LIGHT = "low_light"
        const val KEY_ONBOARDED = "onboarding_done"
        const val KEY_SHOW_STATS = "show_stats"
        const val KEY_FACETS = "facet_mosaic"
    }
}
