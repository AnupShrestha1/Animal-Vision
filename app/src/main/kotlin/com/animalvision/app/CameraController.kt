package com.animalvision.app

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

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
    private var targetSize = Size(1280, 720)
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

    private fun bindUseCases() {
        val p = provider ?: return
        val st = surfaceTexture ?: return
        p.unbindAll()

        val preview = Preview.Builder()
            .setTargetResolution(targetSize)
            .build()
        this.preview = preview

        val selector = CameraSelector.Builder()
            .requireLensFacing(if (facingBack) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT)
            .build()

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