package com.animalvision.app

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * CameraX wrapper binding a preview stream to the renderer's SurfaceTexture.
 * Supports front/back switching and torch. Gracefully reports errors (spec §15, §37).
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val mainExecutor: Executor
) {
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var targetSize = Size(1920, 1080)
    private var reportedActualResolution = false

    var onCapabilitiesResolved: ((hasUltraWide: Boolean, resolution: Size) -> Unit)? = null
    var onCameraError: ((message: String) -> Unit)? = null
    var facingBack = true
        private set
    val hasFlash: Boolean get() = camera?.cameraInfo?.hasFlashUnit() == true
    val torchOn: Boolean get() = camera?.cameraInfo?.torchState?.value == 1

    fun start(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        this.surfaceTexture = surfaceTexture
        targetSize = Size(width, height)
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                provider = future.get()
                bindUseCases()
            } catch (e: Exception) {
                e.printStackTrace()
                onCameraError?.invoke("Camera initialization failed: ${e.message}")
            }
        }, mainExecutor)
    }

    fun switchCamera(): Boolean {
        facingBack = !facingBack
        reportedActualResolution = false
        if (provider != null) bindUseCases()
        return facingBack
    }

    fun setTorch(on: Boolean): Boolean {
        val cam = camera ?: return false
        if (cam.cameraInfo.hasFlashUnit() != true) return false
        cam.cameraControl.enableTorch(on)
        return true
    }

    /**
     * Focus and meter at a point of the camera buffer ([x], [y] in 0..1, top-left origin —
     * see AnimalGlRenderer.viewToCameraPoint). Continuous autofocus resumes after 5 s.
     */
    fun focusAt(x: Float, y: Float) {
        val cam = camera ?: return
        val p = preview ?: return
        val point = SurfaceOrientedMeteringPointFactory(1f, 1f, p).createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
        ).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
        if (cam.cameraInfo.isFocusMeteringSupported(action)) {
            cam.cameraControl.startFocusAndMetering(action)
        }
    }

    /** Whether the lens chosen by [selector] has optical image stabilization. */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun supportsOis(p: ProcessCameraProvider, selector: CameraSelector): Boolean =
        runCatching {
            val info = selector.filter(p.availableCameraInfos).firstOrNull() ?: return false
            val modes = Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            modes?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
        }.getOrDefault(false)

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    private fun bindUseCases() {
        val p = provider ?: return
        val st = surfaceTexture ?: return
        p.unbindAll()

        // Always bind at ROTATION_0 so the transform CameraX bakes into the
        // SurfaceTexture path is identical in every orientation (the "portrait"
        // matrix). The renderer re-frames it by the actual display rotation
        // (AnimalGlRenderer.setDisplayRotationDegrees) because CameraX does not
        // reliably re-apply live target-rotation changes to this surface path
        // (the Redmi test device kept the feed rotated either way).
        val selector = CameraSelector.Builder()
            .requireLensFacing(if (facingBack) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT)
            .build()

        val builder = Preview.Builder()
            .setTargetResolution(targetSize)
            .setTargetRotation(Surface.ROTATION_0)
        // CameraX leaves OIS off for a plain preview. In dim scenes the sensor runs long
        // exposures (1/25 s was measured on the test device), where hand shake is the main
        // source of blur, so enable it whenever the lens has it. Autofocus stays on CameraX's
        // default CONTINUOUS_PICTURE so tap-to-focus (focusAt) can still drive it.
        if (supportsOis(p, selector)) {
            Camera2Interop.Extender(builder).setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON,
            )
        }
        val preview = builder.build()
        this.preview = preview

        val surfaceProvider = Preview.SurfaceProvider { request ->
            val surface = Surface(st)
            request.provideSurface(surface, ContextCompat.getMainExecutor(context)) {
                surface.release()
            }
            reportedActualResolution = true
            onCapabilitiesResolved?.invoke(false, request.resolution)
        }
        preview.setSurfaceProvider(surfaceProvider)

        try {
            camera = p.bindToLifecycle(lifecycleOwner, selector, preview)
            // Fallback only: the surface request above reports the REAL buffer size,
            // but if it hasn't fired, report the requested size so the renderer
            // still has sane dimensions (avoids a (1,1) no-crop stretch).
            if (!reportedActualResolution) onCapabilitiesResolved?.invoke(false, targetSize)
        } catch (e: Exception) {
            e.printStackTrace()
            camera = null
            onCameraError?.invoke(if (facingBack) "Rear camera unavailable" else "Front camera unavailable")
        }
    }

    fun stop() {
        provider?.unbindAll()
        camera = null
    }
}