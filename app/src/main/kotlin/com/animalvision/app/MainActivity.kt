package com.animalvision.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.animalvision.app.gl.AnimalGlRenderer
import com.animalvision.app.ui.AnimalInfoScreen
import com.animalvision.app.ui.AnimalVisionTheme
import com.animalvision.app.ui.AvMuted
import com.animalvision.app.ui.AvTextPrimary
import com.animalvision.app.ui.CameraScreen
import com.animalvision.app.ui.OnboardingScreen
import com.animalvision.app.ui.SettingsScreen
import com.animalvision.core.model.QualityTier
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

/**
 * Wires the GL viewfinder, CameraX, capture plumbing, and the Compose UI.
 * Permission flow: onboarding → camera grant → live view (spec §13, §23, §37).
 */
class MainActivity : ComponentActivity() {

    // Activity.getMainExecutor() is API 28+; minSdk is 26.
    private val uiExecutor by lazy { ContextCompat.getMainExecutor(this) }

    // Lazy: SharedPreferences needs the Context, which isn't attached during construction.
    private val appState by lazy { AppState(getSharedPreferences("animal_vision", MODE_PRIVATE)) }
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: AnimalGlRenderer
    private lateinit var cameraController: CameraController
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraStarted = false

    // ---- storage permission (API 26–28 only; scoped storage needs none) ----
    private var pendingStorageAction: (() -> Unit)? = null
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val action = pendingStorageAction
            pendingStorageAction = null
            if (granted) action?.invoke()
            else appState.announce("Storage access is needed to save photos and videos")
        }

    private fun withStorageAccess(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
            return
        }
        pendingStorageAction = action
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private var cameraPermissionGranted = false
        set(value) {
            field = value
            maybeStartCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPermissionGranted = hasCameraPermission()
        appState.cameraPermission = cameraPermissionGranted

        renderer = AnimalGlRenderer(
            profileProvider = { appState.profile },
            tierProvider = { appState.qualityTier },
            modeProvider = { appState.effectiveMode() },
            lowLightGate = { appState.lowLightEnabled },
            zoomProvider = { appState.zoom },
            splitProvider = { appState.splitPosition },
            mosaicProvider = { appState.facetMosaic },
        )
        // Seed the current display rotation so a launch already in landscape
        // renders upright (the camera always binds at ROTATION_0; the renderer
        // re-frames by this value every frame).
        renderer.setDisplayRotationDegrees(rotationDegrees())
        renderer.onSurfaceTextureReady = { st ->
            // GL thread -> main. A second call means the EGL context was recreated with a
            // new camera texture, so the camera must be re-bound to it.
            runOnUiThread {
                surfaceTexture = st
                cameraStarted = false
                maybeStartCamera()
            }
        }
        renderer.onRecordingInterrupted = {
            runOnUiThread {
                if (appState.isRecording) {
                    stopRecording(publish = true)
                    appState.announce("Recording stopped: screen rotated")
                }
            }
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
                    appState.setTier(lower, persist = false)
                    appState.announce("Reduced quality to keep the preview smooth")
                }
            }
        }

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
        }

        cameraController = CameraController(this, this, uiExecutor)
        cameraController.onCapabilitiesResolved = { _, resolution ->
            renderer.setCameraSize(resolution.width, resolution.height)
            appState.hasFlash = cameraController.hasFlash
            appState.maxZoom = cameraController.maxZoom
            appState.zoom = 1f
        }
        // Recordings abandoned by a previous process (killed mid-recording) are never published.
        ioExecutor.execute {
            cacheDir.listFiles { f -> f.name.startsWith("av_") && f.name.endsWith(".mp4") }
                ?.forEach { it.delete() }
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
        // Permission may have been granted (or revoked) in system settings while away.
        val granted = hasCameraPermission()
        if (granted != appState.cameraPermission) {
            appState.cameraPermission = granted
            if (granted) cameraPermissionGranted = true
        }
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    /** Display rotation in degrees (0/90/180/270). */
    @Suppress("DEPRECATION")
    private fun rotationDegrees(): Int = windowManager.defaultDisplay.rotation * 90

    /**
     * The manifest handles orientation|screenSize|screenLayout|smallestScreenSize
     * itself, so the activity is NOT recreated on rotate: the GL surface, camera
     * session, and Compose state all survive. The camera always binds at
     * ROTATION_0 (see CameraController), so its transform is orientation-
     * independent; this callback hands the new rotation to the renderer, which
     * re-frames the feed upright every frame (and re-derives its cover-crop from
     * the corrected matrix).
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        renderer.setDisplayRotationDegrees(rotationDegrees())
        // Some OEMs dispatch the config change a beat before the display
        // rotation settles; re-read on the next main-loop pass so the renderer
        // always ends up with the final value.
        uiExecutor.execute {
            renderer.setDisplayRotationDegrees(rotationDegrees())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (appState.isRecording) stopRecording(publish = false)
        cameraController.stop()
        renderer.shutdown()
        ioExecutor.shutdown()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun maybeStartCamera() {
        val st = surfaceTexture ?: return
        if (!cameraPermissionGranted || cameraStarted) return
        cameraStarted = true
        cameraController.start(st, 1920, 1080)
    }


    // ---- capture plumbing ---------------------------------------------------

    private fun capturePhoto() {
        appState.captureFlashTick++
        val animalName = appState.profile.name
        renderer.requestCapture { bitmap ->
            ioExecutor.execute {
                val uri = CaptureSaver.saveJpeg(applicationContext, bitmap, animalName)
                val thumb = if (uri != null) thumbnailOf(bitmap) else null
                bitmap.recycle()
                runOnUiThread {
                    if (uri != null && thumb != null) {
                        appState.lastCapture = LastCapture(uri, thumb, isVideo = false)
                        appState.announce("Photo saved")
                    } else {
                        appState.announce("Photo could not be saved")
                    }
                }
            }
        }
        glView.requestRender()
    }

    /** Small preview for the capture thumbnail (keeps memory tiny). */
    private fun thumbnailOf(bitmap: Bitmap): ImageBitmap {
        val scale = 160f / maxOf(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(
            bitmap,
            maxOf(1, (bitmap.width * scale).toInt()),
            maxOf(1, (bitmap.height * scale).toInt()),
            true,
        ).asImageBitmap()
    }

    /** Opens the last photo/video in the user's gallery or player. */
    private fun openLastCapture() {
        val capture = appState.lastCapture ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(capture.uri, if (capture.isVideo) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            appState.announce("No app found to open it - it is in your gallery")
        }
    }

    private var pendingVideoFile: File? = null

    private fun toggleRecording() {
        if (appState.isRecording) {
            stopRecording(publish = true)
        } else {
            withStorageAccess(::startRecording)
        }
    }

    private fun startRecording() {
        if (appState.isRecording) return
        val file = File(cacheDir, "av_${System.currentTimeMillis()}.mp4")
        pendingVideoFile = file
        glView.queueEvent {
            renderer.startRecordingOnGlThread(file) { error ->
                file.delete()
                runOnUiThread {
                    appState.isRecording = false
                    appState.announce("Recording failed: $error")
                }
            }
        }
        appState.isRecording = true
        appState.recordingStartedAtMs = System.currentTimeMillis()
    }

    private fun stopRecording(publish: Boolean) {
        val file = pendingVideoFile
        pendingVideoFile = null
        val animalName = appState.profile.name
        appState.isRecording = false
        glView.queueEvent {
            renderer.stopRecordingOnGlThread {
                // worker thread after teardown
                if (!publish) {
                    file?.delete()
                } else if (file != null) {
                    val thumb = videoThumbnail(file)
                    val uri = CaptureSaver.publishVideo(applicationContext, file, animalName)
                    runOnUiThread {
                        if (uri != null) {
                            if (thumb != null) appState.lastCapture = LastCapture(uri, thumb, isVideo = true)
                            appState.announce("Video saved")
                        } else {
                            appState.announce("Video could not be saved")
                        }
                    }
                }
            }
        }
    }

    /** First frame of a finished recording, for the capture thumbnail. */
    private fun videoThumbnail(file: File): ImageBitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0)?.let { frame -> thumbnailOf(frame).also { frame.recycle() } }
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
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
        val cameraPermission = appState.cameraPermission
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            appState.cameraPermission = granted
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
                // A live viewfinder shouldn't dim or sleep. The flag belongs to this window, so it
                // only holds while the camera screen is shown and the app is in front.
                DisposableEffect(Unit) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { glView },
                    modifier = Modifier.fillMaxSize(),
                )

                CameraScreen(
                    profile = appState.profile,
                    profiles = appState.profiles,
                    onSelectProfile = appState::selectProfile,
                    displayMode = appState.effectiveMode(),
                    onSetMode = appState::setMode,
                    splitPosition = appState.splitPosition,
                    onSplitChange = { appState.splitPosition = it },
                    zoom = appState.zoom,
                    maxZoom = appState.maxZoom,
                    onZoom = { appState.zoom = cameraController.setZoom(it) },
                    onHoldStart = { appState.setHoldToCompare(true) },
                    onHoldEnd = { appState.setHoldToCompare(false) },
                    onTapFocus = { x, y ->
                        renderer.viewToCameraPoint(x, y)?.let { (u, v) -> cameraController.focusAt(u, v) }
                    },
                    educationalMode = appState.educationalMode,
                    statusMessage = appState.statusMessage,
                    showStats = appState.showStats,
                    fps = appState.fps,
                    isRecording = appState.isRecording,
                    recordingStartedAtMs = appState.recordingStartedAtMs,
                    lastCapture = appState.lastCapture,
                    captureFlashTick = appState.captureFlashTick,
                    onCapturePhoto = { withStorageAccess(::capturePhoto) },
                    onToggleRecord = ::toggleRecording,
                    onOpenLastCapture = ::openLastCapture,
                    hasFlash = appState.hasFlash,
                    torchOn = appState.torchOn,
                    onToggleTorch = ::toggleTorch,
                    onSwitchCamera = ::switchCamera,
                    onOpenInfo = { appState.showInfo = true },
                    onOpenSettings = { appState.showSettings = true },
                    facetMosaic = appState.facetMosaic,
                    onToggleFacets = {
                        appState.facetMosaic = !appState.facetMosaic
                        appState.announce(
                            if (appState.facetMosaic) "Facets: one sample per ommatidium — one image, not many copies"
                            else "Facets off",
                        )
                    },
                )

                // System back closes an open panel instead of leaving the app.
                BackHandler(enabled = appState.showInfo || appState.showSettings) {
                    appState.showInfo = false
                    appState.showSettings = false
                }

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
                        showStats = appState.showStats,
                        onShowStats = { appState.showStats = it },
                        onClose = { appState.showSettings = false },
                    )
                }
            }
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

