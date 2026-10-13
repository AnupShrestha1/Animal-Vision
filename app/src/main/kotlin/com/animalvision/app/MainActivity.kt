package com.animalvision.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.animalvision.app.gl.AnimalGlRenderer
import com.animalvision.app.ui.AnimalInfoScreen
import com.animalvision.app.ui.AnimalSelector
import com.animalvision.app.ui.AnimalVisionTheme
import com.animalvision.app.ui.AvMuted
import com.animalvision.app.ui.AvTextPrimary
import com.animalvision.app.ui.CameraScreen
import com.animalvision.app.ui.OnboardingScreen
import com.animalvision.app.ui.SettingsScreen
import com.animalvision.core.model.QualityTier
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Wires the GL viewfinder, CameraX, capture plumbing, and the Compose UI.
 * Permission flow: onboarding → camera grant → live view (spec §13, §23, §37).
 */
class MainActivity : ComponentActivity() {

    private val appState = AppState()
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: AnimalGlRenderer
    private lateinit var cameraController: CameraController
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraStarted = false

    private var cameraPermissionGranted = false
        set(value) {
            field = value
            maybeStartCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPermissionGranted = hasCameraPermission()

        renderer = AnimalGlRenderer(
            profileProvider = { appState.profile },
            tierProvider = { appState.qualityTier },
            modeProvider = { appState.effectiveMode() },
            lowLightGate = { appState.lowLightEnabled },
        )
        renderer.onSurfaceTextureReady = { st ->
            surfaceTexture = st
            maybeStartCamera()
        }
        renderer.onFps = { fps -> appState.fps = fps }
        renderer.onTierDropRequest = {
            runOnUiThread {
                val current = appState.qualityTier
                val lower = when (current) {
                    QualityTier.HIGH -> QualityTier.BALANCED
                    else -> QualityTier.PERFORMANCE
                }
                if (lower != current) {
                    appState.setTier(lower)
                    appState.announce("Reduced quality to keep the preview smooth")
                }
            }
        }

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
        }

        cameraController = CameraController(this, this, mainExecutor)
        cameraController.onCapabilitiesResolved = { _, resolution ->
            renderer.setCameraSize(resolution.width, resolution.height)
            appState.hasFlash = cameraController.hasFlash
            // Ultra-wide is not probed (phase-1 limitation, spec §15): tell the user the
            // species FOV is simulated rather than optically widened.
            appState.capabilityNote = "Species FOV simulated — standard lens in use"
        }
        cameraController.onCameraError = { message ->
            runOnUiThread { appState.announce(message) }
        }

        setContent {
            AnimalVisionTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    /**
     * The manifest handles orientation|screenSize|screenLayout|smallestScreenSize
     * itself, so the activity is NOT recreated on rotate: the GL surface, camera
     * session, and Compose state all survive. This callback is our one chance to
     * re-aim the CameraX preview at the new display rotation so the live image
     * stays upright in landscape (the renderer re-derives its cover-crop from the
     * updated SurfaceTexture transform, and onSurfaceChanged reallocates the
     * pipeline for the new viewport).
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        @Suppress("DEPRECATION")
        val rotation = windowManager.defaultDisplay.rotation
        cameraController.setTargetRotation(rotation)
        android.util.Log.i(
            "AnimalVision",
            "AVDBG rotation=$rotation landscape=${newConfig.screenWidthDp > newConfig.screenHeightDp}",
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (appState.isRecording) stopRecording(publish = false)
        cameraController.stop()
        ioExecutor.shutdown()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun maybeStartCamera() {
        val st = surfaceTexture ?: return
        if (!cameraPermissionGranted || cameraStarted) return
        cameraStarted = true
        cameraController.start(st, 1280, 720)
    }


    // ---- capture plumbing ---------------------------------------------------

    private fun capturePhoto() {
        renderer.requestCapture { bitmap ->
            ioExecutor.execute {
                val uri = CaptureSaver.saveJpeg(applicationContext, bitmap, appState.profile.name)
                bitmap.recycle()
                runOnUiThread {
                    appState.announce(
                        if (uri != null) "Photo saved to Pictures/AnimalVision"
                        else "Photo could not be saved",
                    )
                }
            }
        }
        glView.requestRender()
    }

    private var pendingVideoFile: File? = null

    private fun toggleRecording() {
        if (appState.isRecording) {
            stopRecording(publish = true)
        } else {
            val file = File(cacheDir, "av_${System.currentTimeMillis()}.mp4")
            pendingVideoFile = file
            glView.queueEvent {
                renderer.startRecordingOnGlThread(file) { error ->
                    runOnUiThread {
                        appState.isRecording = false
                        appState.announce("Recording failed: $error")
                    }
                }
            }
            appState.isRecording = true
            appState.announce("Recording started")
        }
    }

    private fun stopRecording(publish: Boolean) {
        val file = pendingVideoFile
        pendingVideoFile = null
        val animalName = appState.profile.name
        appState.isRecording = false
        glView.queueEvent {
            renderer.stopRecordingOnGlThread {
                // worker thread after teardown
                if (publish && file != null) {
                    val uri = CaptureSaver.publishVideo(applicationContext, file, animalName)
                    runOnUiThread {
                        appState.announce(
                            if (uri != null) "Video saved to Movies/AnimalVision"
                            else "Video could not be saved",
                        )
                    }
                }
            }
        }
    }

    private fun switchCamera() {
        appState.cameraFacingBack = cameraController.switchCamera()
        appState.hasFlash = cameraController.hasFlash
        appState.torchOn = false
        appState.announce(if (appState.cameraFacingBack) "Rear camera" else "Front camera")
    }

    private fun toggleTorch() {
        val want = !appState.torchOn
        if (cameraController.setTorch(want)) {
            appState.torchOn = want
        } else {
            appState.announce("No flash on this camera")
        }
    }


    // ---- Compose root -------------------------------------------------------

    @Composable
    private fun AppRoot() {
        var cameraPermission by remember { mutableStateOf(hasCameraPermission()) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            cameraPermission = granted
            cameraPermissionGranted = granted
            if (!granted) appState.announce("Camera permission is required for the live view")
        }

        // Regenerate the species LUT whenever the animal changes.
        LaunchedEffect(appState.currentProfileId) {
            renderer.setProfile(appState.profile)
        }

        // Auto-clear transient status messages.
        LaunchedEffect(appState.statusMessage) {
            if (appState.statusMessage != null) {
                delay(3000)
                appState.clearStatus()
            }
        }

        when {
            appState.needsOnboarding -> OnboardingScreen(
                cameraPermissionGranted = cameraPermission,
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onDone = {
                    appState.markOnboardingDone()
                    if (!cameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            )

            !cameraPermission -> PermissionRationale(
                onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )

            else -> Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { glView },
                    modifier = Modifier.fillMaxSize(),
                )

                CameraScreen(
                    profile = appState.profile,
                    displayMode = appState.effectiveMode(),
                    onSetMode = appState::setMode,
                    onHoldStart = { appState.setHoldToCompare(true) },
                    onHoldEnd = { appState.setHoldToCompare(false) },
                    educationalMode = appState.educationalMode,
                    capabilityNote = appState.capabilityNote,
                    statusMessage = appState.statusMessage,
                    fps = appState.fps,
                    isRecording = appState.isRecording,
                    onCapturePhoto = ::capturePhoto,
                    onToggleRecord = ::toggleRecording,
                    onOpenInfo = { appState.showInfo = true },
                )

                TopBar(
                    onInfo = { appState.showInfo = true },
                    onSettings = { appState.showSettings = true },
                    onSwitchCamera = ::switchCamera,
                    onToggleTorch = ::toggleTorch,
                    hasFlash = appState.hasFlash,
                    torchOn = appState.torchOn,
                )

                AnimalSelector(
                    profiles = appState.profiles,
                    selectedId = appState.currentProfileId,
                    onSelect = appState::selectProfile,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )

                if (appState.showInfo) {
                    AnimalInfoScreen(
                        profile = appState.profile,
                        onClose = { appState.showInfo = false },
                    )
                }
                if (appState.showSettings) {
                    SettingsScreen(
                        qualityTier = appState.qualityTier,
                        onQualityTier = appState::setTier,
                        educationalMode = appState.educationalMode,
                        onEducationalMode = { appState.educationalMode = it },
                        lowLightEnabled = appState.lowLightEnabled,
                        onLowLightEnabled = { appState.lowLightEnabled = it },
                        onClose = { appState.showSettings = false },
                    )
                }
            }
        }
    }

    @Composable
    private fun TopBar(
        onInfo: () -> Unit,
        onSettings: () -> Unit,
        onSwitchCamera: () -> Unit,
        onToggleTorch: () -> Unit,
        hasFlash: Boolean,
        torchOn: Boolean,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Scrim keeps the controls readable over bright scenes.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.55f to Color.Black.copy(alpha = 0.18f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Animal Vision",
                    style = MaterialTheme.typography.titleMedium,
                    color = AvTextPrimary,
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Animal Vision, showing ${appState.profile.name}"
                        },
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hasFlash) {
                    GlassIconButton(
                        glyph = "⚡",
                        contentDesc = if (torchOn) "Turn flash off" else "Turn flash on",
                        active = torchOn,
                        onClick = onToggleTorch,
                    )
                }
                GlassIconButton(
                    glyph = "⇄",
                    contentDesc = "Switch between rear and front camera",
                    active = false,
                    onClick = onSwitchCamera,
                )
                GlassIconButton(
                    glyph = "ⓘ",
                    contentDesc = "Open species info",
                    active = false,
                    onClick = onInfo,
                )
                GlassIconButton(
                    glyph = "⚙",
                    contentDesc = "Open settings",
                    active = false,
                    onClick = onSettings,
                )
            }
        }
    }

    /** Circular translucent control used by the top bar; active state tints the glyph. */
    @Composable
    private fun GlassIconButton(
        glyph: String,
        contentDesc: String,
        active: Boolean,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else Color.Black.copy(alpha = 0.45f),
                )
                .clickable(onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription = contentDesc
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                color = if (active) MaterialTheme.colorScheme.primary else AvTextPrimary,
            )
        }
    }
}

/** Shown when camera permission is denied (spec §37: graceful, with retry). */
@Composable
private fun PermissionRationale(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E12)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.headlineSmall,
                color = AvTextPrimary,
            )
            Text(
                text = "Animal Vision processes camera frames entirely on this device to " +
                    "simulate animal vision. Nothing is uploaded. Grant camera access to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = AvMuted,
                modifier = Modifier.padding(top = 12.dp),
            )
            TextButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant camera access", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

