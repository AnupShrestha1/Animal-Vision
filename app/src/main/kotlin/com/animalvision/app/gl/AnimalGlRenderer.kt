package com.animalvision.app.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLDisplay
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.animalvision.core.color.ColorTransform
import com.animalvision.core.color.Lut
import com.animalvision.core.color.LutGenerator
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.StageId
import com.animalvision.core.pipeline.Pipeline
import com.animalvision.core.stage.Acuity
import com.animalvision.core.stage.Fov
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig as KEGLConfig

/**
 * GLSurfaceView.Renderer running the vision pipeline on the camera's external texture.
 * Flow: OES -> linear -> LUT(color) -> lowlight -> acuity (pyramid blur + FOV combine) ->
 * final composite (thermal + tonemap + before/after modes) -> display. Captures read the
 * final target; photos are re-rendered at the High-tier size when the preview runs lower.
 */
class AnimalGlRenderer(
    private val profileProvider: () -> AnimalVisionProfile,
    private val tierProvider: () -> QualityTier,
    private val modeProvider: () -> Int,
    private val lowLightGate: () -> Boolean = { true },
) : GLSurfaceView.Renderer {

    var onSurfaceTextureReady: ((SurfaceTexture) -> Unit)? = null
    var onFps: ((fps: Double) -> Unit)? = null
    var onTierDropRequest: (() -> Unit)? = null

    /**
     * Fired (GL thread) when the screen geometry changes during a recording. The encoder size
     * is fixed, so the caller should stop and publish the recording; the pipeline keeps its
     * current size until it does.
     */
    var onRecordingInterrupted: (() -> Unit)? = null

    @Volatile internal var eglDisplay: EGLDisplay? = null
    @Volatile internal var eglContext: android.opengl.EGLContext? = null
    @Volatile internal var windowSurface: android.opengl.EGLSurface? = null

    private var oesTexture = 0
    private var surfaceTexture: SurfaceTexture? = null
    @Volatile private var cameraTexSize = intArrayOf(1920, 1080)

    private var oesProg = 0
    private var lowlightProg = 0
    private var combineProg = 0
    private var finalProg = 0
    private var screenProg = 0
    private lateinit var blurProgs: BlurPrograms

    private var screenW = 0
    private var screenH = 0
    private var targets: FrameTargets? = null

    // ---- species LUT: generated off-thread, newest request wins ----
    private val lutExecutor = Executors.newSingleThreadExecutor()
    private val lutGeneration = AtomicInteger()
    private val pendingLut = AtomicReference<Lut?>(null)
    @Volatile private var requestedProfileId: String? = null
    private var lastLut: Lut? = null
    private var lutTexture = 0

    @Volatile private var displayRotationDeg = 0
    // Scratch matrices for the display-rotation re-frame (pre-allocated, GL thread only).
    private val rotR = FloatArray(16)
    private val rotT1 = FloatArray(16)
    private val rotT2 = FloatArray(16)
    private val rotBase = FloatArray(16)
    private val texMatrix = FloatArray(16).also {
        it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f
    }

    // Last displayed frame's view -> camera mapping, for tap-to-focus (written on the GL
    // thread, read on the UI thread under [meterLock]).
    private val meterLock = Any()
    private val meterMatrix = FloatArray(16)
    private val meterCrop = FloatArray(2)
    private val meterWarp = FloatArray(2)
    private var meterMode = 1
    private var meterValid = false

    private var frameCount = 0
    private var noiseFrame = 0
    private var windowStartNs = 0L
    private var lowFpsWindows = 0

    var videoRecorder: VideoRecorder? = null
    private var recordingScreenW = 0
    private var recordingScreenH = 0
    private var recordingInterruptSent = false

    // ---- photo capture (spec §29): read the final target back on the GL thread ----
    @Volatile private var pendingCapture: ((android.graphics.Bitmap) -> Unit)? = null

    /** Must be called before/while the GL thread is rendering; callback runs on the GL thread. */
    fun requestCapture(callback: (android.graphics.Bitmap) -> Unit) {
        pendingCapture = callback
    }

    private fun maybeCapture(preview: FrameTargets, profile: AnimalVisionProfile, tier: QualityTier, mode: Int) {
        val cb = pendingCapture ?: return
        pendingCapture = null
        // Photos are always produced at the High-tier size: when the live preview has been
        // reduced for frame rate, re-run the pipeline once at full size for this frame.
        val (cw, ch) = internalSize(screenW, screenH, QualityTier.HIGH)
        val bitmap = if (cw > preview.w || ch > preview.h) {
            val capture = FrameTargets(cw, ch)
            try {
                renderPipeline(capture, profile, tier, mode)
                readFinal(capture)
            } finally {
                capture.release()
            }
        } else {
            readFinal(preview)
        }
        cb(bitmap)
    }

    private fun readFinal(t: FrameTargets): android.graphics.Bitmap {
        val buf = java.nio.ByteBuffer.allocate(t.w * t.h * 4).order(java.nio.ByteOrder.nativeOrder())
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, t.final.fbo)
        GLES30.glReadPixels(0, 0, t.w, t.h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
        bindDefault()
        buf.rewind()
        val src = android.graphics.Bitmap.createBitmap(t.w, t.h, android.graphics.Bitmap.Config.ARGB_8888)
        src.copyPixelsFromBuffer(buf)
        // glReadPixels origin is bottom-left; flip to top-left.
        val m = android.graphics.Matrix().apply { postScale(1f, -1f) }
        val out = android.graphics.Bitmap.createBitmap(src, 0, 0, t.w, t.h, m, false)
        if (out !== src) src.recycle()
        return out
    }

    /** Start recording the processed output. Call from the GL thread (view.queueEvent). */
    fun startRecordingOnGlThread(file: java.io.File, onError: (String) -> Unit) {
        stopRecordingOnGlThread()
        val t = targets
        if (t == null) { onError("Renderer not ready"); return }
        val rec = VideoRecorder(file, t.w, t.h)
        try {
            rec.start()
            videoRecorder = rec
            recordingScreenW = screenW
            recordingScreenH = screenH
            recordingInterruptSent = false
        } catch (e: Exception) {
            runCatching { rec.stop() }
            videoRecorder = null
            onError(e.message ?: "Recorder unavailable")
        }
    }

    /** Stop recording: signal EOS on the GL thread, tear down off-thread; [onStopped] runs after teardown. */
    fun stopRecordingOnGlThread(onStopped: (() -> Unit)? = null) {
        val rec = videoRecorder
        if (rec == null) {
            onStopped?.invoke()
            return
        }
        videoRecorder = null
        rec.signalEndOfStream()
        Thread {
            runCatching { rec.stop() }
            onStopped?.invoke()
        }.start()
    }

    /** Release non-GL resources; call when the owning activity is destroyed. */
    fun shutdown() {
        lutExecutor.shutdownNow()
    }

    // EGL state is sampled per-frame from the current context (avoids fragile custom factories).
    private fun sampleEglState() {
        eglDisplay = EGL14.eglGetCurrentDisplay()
        eglContext = EGL14.eglGetCurrentContext()
        windowSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
    }

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: KEGLConfig?) {
        // A (re)created context invalidates every GL object id from a previous one: drop
        // them without deleting and rebuild. The last LUT is re-uploaded from memory.
        GlPipeline.resetContextState()
        targets = null
        lutTexture = 0
        lastLut?.let { pendingLut.compareAndSet(null, it) }

        oesProg = GlPipeline.compileProgram(Shaders.VERT, Shaders.FRAG_OES)
        lowlightProg = GlPipeline.compileProgram(Shaders.VERT, Shaders.FRAG_LOWLIGHT)
        combineProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_ACUITY_COMBINE)
        finalProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_FINAL)
        screenProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_DOWNSAMPLE)
        blurProgs = BlurPrograms(
            horizontal = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_BLUR_H),
            vertical = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_BLUR_V),
            downsample = screenProg,
        )
        oesTexture = GlPipeline.createOesTexture()
        val st = SurfaceTexture(oesTexture)
        st.setDefaultBufferSize(cameraTexSize[0], cameraTexSize[1])
        surfaceTexture = st
        onSurfaceTextureReady?.invoke(st)
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        screenW = width
        screenH = height
        GLES30.glViewport(0, 0, width, height)
    }

    fun setCameraSize(w: Int, h: Int) {
        cameraTexSize = intArrayOf(w, h)
        surfaceTexture?.setDefaultBufferSize(w, h)
    }

    /**
     * Sets the display rotation in degrees (0/90/180/270) so the preview stays
     * upright in landscape without touching the camera session. The camera
     * always binds at ROTATION_0, hence the SurfaceTexture matrix carries the
     * portrait transform; each frame we compose T(0.5)·R(deg)·T(−0.5) in front
     * of it — identity when upright (the proven portrait path). The corrected
     * matrix feeds both the shader and computeCrop()'s cover-crop measurement.
     */
    fun setDisplayRotationDegrees(deg: Int) {
        displayRotationDeg = ((deg % 360) + 360) % 360
    }

    companion object {
        fun internalSize(screenW: Int, screenH: Int, tier: QualityTier): Pair<Int, Int> {
            val maxDim = when (tier) {
                // High matches the 1920×1080 camera stream, so the viewfinder keeps every
                // pixel the sensor delivers; lower tiers trade detail for frame rate.
                QualityTier.HIGH -> 1920
                QualityTier.BALANCED -> 1440
                QualityTier.PERFORMANCE -> 960
            }
            val scale = Math.min(1.0, maxDim.toDouble() / Math.max(screenW, screenH))
            val w = Math.max(2, (screenW * scale).toInt() and -2)
            val h = Math.max(2, (screenH * scale).toInt() and -2)
            return w to h
        }

        /**
         * Largest blur sigma applied at one pyramid level. A Gaussian of sigma ≥ ~1 px leaves
         * almost nothing above half the sampling rate (≈1.5% at 0.25 cycles/px), so blurring on
         * a 2× coarser level from there is visually identical and ~4× cheaper per level. This
         * keeps moderate blurs (dog/horse ≈ 2 px at 1920) off full-resolution passes.
         */
        private fun maxSigmaPerLevel(tier: QualityTier): Double = when (tier) {
            QualityTier.HIGH, QualityTier.BALANCED -> 1.0
            QualityTier.PERFORMANCE -> 0.75
        }
    }

    // ---- internal resources ---------------------------------------------------

    private fun bindTarget(t: RenderTarget) {
        t.bind()
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    }

    private fun bindDefault() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun setTex(prog: Int, name: String, unit: Int, tex: Int, target: Int = GLES30.GL_TEXTURE_2D) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, tex)
        GLES30.glUniform1i(GlPipeline.uniform(prog, name), unit)
    }

    /**
     * Cover-fit mapping: keeps the camera frame's aspect instead of stretching it
     * into the w x h quad.
     *
     * The sampled image's true aspect is MEASURED from the SurfaceTexture transform:
     * the quad's U/V edges mapped through [texMatrix] into buffer pixels
     * (using the real buffer size in [cameraTexSize]). This is robust to the 90°
     * sensor rotation, mirroring, and any crop CameraX bakes into the matrix.
     * The narrower axis is then center-cropped (scale <= 1, never a stretch).
     */
    private fun computeCrop(w: Int, h: Int): Pair<Float, Float> {
        var sx = 1f
        var sy = 1f
        val wb = cameraTexSize[0].toDouble()
        val hb = cameraTexSize[1].toDouble()
        if (w > 0 && h > 0 && wb > 0 && hb > 0) {
            // Quad edges through the transform (column-major 4x4, rotation in
            // columns 0/1; translation does not affect edge vectors).
            val lenU = Math.hypot(texMatrix[0] * wb, texMatrix[1] * hb)
            val lenV = Math.hypot(texMatrix[4] * wb, texMatrix[5] * hb)
            if (lenU > 1e-6 && lenV > 1e-6) {
                val contentAspect = lenU / lenV
                val targetAspect = w.toDouble() / h.toDouble()
                if (contentAspect > targetAspect) {
                    // Content relatively wider: crop width.
                    sx = (targetAspect / contentAspect).toFloat()
                } else if (contentAspect < targetAspect) {
                    // Content relatively taller: crop height.
                    sy = (contentAspect / targetAspect).toFloat()
                }
                if (sx.isNaN() || sx <= 0f || sx > 1f) sx = 1f
                if (sy.isNaN() || sy <= 0f || sy > 1f) sy = 1f
            }
        }
        return sx to sy
    }

    /** Request the species LUT; generation runs off-thread and only the newest request lands. */
    fun setProfile(profile: AnimalVisionProfile) {
        if (profile.id == requestedProfileId) return
        requestedProfileId = profile.id
        val generation = lutGeneration.incrementAndGet()
        lutExecutor.execute {
            if (generation != lutGeneration.get()) return@execute // superseded before it started
            val lut = LutGenerator.generate(ColorTransform(profile), profile.colorVision.lutSize)
            // The executor is serial, so a later request always publishes after this one.
            if (generation == lutGeneration.get()) pendingLut.set(lut)
        }
    }

    private fun uploadPendingLut() {
        val lut = pendingLut.getAndSet(null) ?: return
        if (lutTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
        lutTexture = GlPipeline.createLutTexture3d(lut.data, lut.size)
        lastLut = lut
    }

    // ---- per-frame pipeline -----------------------------------------------------

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        val st = surfaceTexture ?: return
        if (screenW == 0 || screenH == 0) return
        sampleEglState()
        st.updateTexImage()
        st.getTransformMatrix(texMatrix)
        uploadPendingLut()
        applyDisplayRotation()

        val profile = profileProvider()
        val tier = tierProvider()
        val mode = modeProvider()
        val t = ensureTargets(tier)
        GlPrimitives.bindQuad()
        renderPipeline(t, profile, tier, mode)
        snapshotMetering(t, profile, tier, mode)

        // video: render the final target into the MediaCodec input surface
        videoRecorder?.let { rec ->
            rec.renderFrame(
                t.final.tex, t.w, t.h, System.nanoTime(),
                eglDisplay, eglContext, windowSurface,
            )
        }

        // photo: read back the final image if a capture was requested
        maybeCapture(t, profile, tier, mode)

        // display
        bindDefault()
        GLES30.glViewport(0, 0, screenW, screenH)
        GLES30.glUseProgram(screenProg)
        setTex(screenProg, "uTex", 0, t.final.tex)
        GlPrimitives.drawFsQuad()

        updateFps()
    }

    private fun snapshotMetering(t: FrameTargets, profile: AnimalVisionProfile, tier: QualityTier, mode: Int) {
        val (cropX, cropY) = computeCrop(t.w, t.h)
        val warp = if (Pipeline.isActive(profile, StageId.FOV, tier)) Fov.warpCoefficients(profile) else null
        synchronized(meterLock) {
            System.arraycopy(texMatrix, 0, meterMatrix, 0, 16)
            meterCrop[0] = cropX
            meterCrop[1] = cropY
            meterWarp[0] = warp?.get(0) ?: 0f
            meterWarp[1] = warp?.get(1) ?: 0f
            meterMode = mode
            meterValid = true
        }
    }

    /**
     * Maps a point on the viewfinder ([x], [y] in 0..1, top-left origin) to the camera buffer
     * (0..1, top-left origin) by replaying the last frame's sampling chain: the FOV warp (only
     * where the animal image is shown), the cover-crop, and the SurfaceTexture transform. The
     * transform's output t-axis already runs top-down in the buffer, so no flip is needed.
     * Returns null before the first frame.
     */
    fun viewToCameraPoint(x: Float, y: Float): Pair<Float, Float>? = synchronized(meterLock) {
        if (!meterValid) return null
        // Viewfinder (y down) -> final-pass UV (y up).
        var ux = x
        var uy = 1f - y
        val warped = meterMode == 1 || (meterMode == 2 && ux < 0.5f)
        if (warped) {
            // Same remap as Shaders2.FRAG_FINAL warpedUv().
            val cx = ux - 0.5f
            val cy = uy - 0.5f
            ux = 0.5f + cx * (1f + meterWarp[0] * (0.25f - cx * cx))
            uy = 0.5f + cy * (1f + meterWarp[1] * (0.25f - cy * cy))
        }
        val qx = (ux - 0.5f) * meterCrop[0] + 0.5f
        val qy = (uy - 0.5f) * meterCrop[1] + 0.5f
        val m = meterMatrix
        val s = m[0] * qx + m[4] * qy + m[12]
        val t = m[1] * qx + m[5] * qy + m[13]
        s.coerceIn(0f, 1f) to t.coerceIn(0f, 1f)
    }

    /** Re-frame from the always-portrait camera transform to the current display rotation. */
    private fun applyDisplayRotation() {
        val rotDeg = displayRotationDeg
        if (rotDeg == 0) return // identity when upright — the proven portrait path
        // R(deg): the CameraX matrix works in a v-flipped sampling frame,
        // which inverts rotation direction vs. naive screen intuition.
        android.opengl.Matrix.setRotateM(rotR, 0, rotDeg.toFloat(), 0f, 0f, 1f)
        android.opengl.Matrix.setIdentityM(rotT1, 0)
        android.opengl.Matrix.translateM(rotT1, 0, -0.5f, -0.5f, 0f)
        android.opengl.Matrix.multiplyMM(rotT2, 0, rotR, 0, rotT1, 0)   // R·T(−0.5)
        android.opengl.Matrix.setIdentityM(rotR, 0)
        android.opengl.Matrix.translateM(rotR, 0, 0.5f, 0.5f, 0f)       // T(0.5)
        android.opengl.Matrix.multiplyMM(rotT1, 0, rotR, 0, rotT2, 0)    // T(0.5)·R·T(−0.5)
        System.arraycopy(texMatrix, 0, rotBase, 0, 16)
        android.opengl.Matrix.multiplyMM(texMatrix, 0, rotT1, 0, rotBase, 0)
    }

    /**
     * Returns the frame targets for the current screen + tier, replacing (and releasing) the
     * old set on a size change. While recording, the encoder's size is fixed: a tier change is
     * deferred until recording ends, and a screen-geometry change asks the owner to stop.
     */
    private fun ensureTargets(tier: QualityTier): FrameTargets {
        val (w, h) = internalSize(screenW, screenH, tier)
        val current = targets
        if (current != null && current.w == w && current.h == h) return current
        if (current != null && videoRecorder != null) {
            val screenChanged = screenW != recordingScreenW || screenH != recordingScreenH
            if (screenChanged && !recordingInterruptSent) {
                recordingInterruptSent = true
                onRecordingInterrupted?.invoke()
            }
            return current
        }
        current?.release()
        return FrameTargets(w, h).also { targets = it }
    }

    private fun renderPipeline(t: FrameTargets, profile: AnimalVisionProfile, tier: QualityTier, mode: Int) {
        t.ensureMasks(profile)
        val (cropX, cropY) = computeCrop(t.w, t.h)

        // 1. RAW linear camera (for before/after "normal" side)
        drawCamera(t.raw, useLut = false, cropX, cropY)

        // 2. COLOR (species LUT); until the first LUT is uploaded, show the plain feed.
        drawCamera(t.color, useLut = lutTexture != 0, cropX, cropY)

        // 3. LOWLIGHT (contrast-preserving)
        var srcTex = t.color.tex
        if (Pipeline.isActive(profile, StageId.BRIGHTNESS, tier) && profile.lowLight.enabled && lowLightGate()) {
            bindTarget(t.lowLight)
            GLES30.glUseProgram(lowlightProg)
            setTex(lowlightProg, "uTex", 0, srcTex)
            GLES30.glUniform1f(GlPipeline.uniform(lowlightProg, "uLift"), profile.lowLight.shadowLift.toFloat())
            GLES30.glUniform1f(GlPipeline.uniform(lowlightProg, "uDesat"), profile.lowLight.maxDesaturation.toFloat())
            GLES30.glUniform1f(GlPipeline.uniform(lowlightProg, "uNoiseSigma"), profile.lowLight.noiseSigma.toFloat())
            GLES30.glUniform1f(GlPipeline.uniform(lowlightProg, "uSeed"), (noiseFrame % 1000).toFloat())
            GlPrimitives.drawFsQuad()
            srcTex = t.lowLight.tex
        }

        // 4. ACUITY (pyramid blur + FOV combine) — same plan as the CPU reference
        val fovOn = Pipeline.isActive(profile, StageId.FOV, tier)
        val plan = Acuity.plan(profile.acuity, t.w, t.h)
        if (Pipeline.isActive(profile, StageId.ACUITY, tier) && !(plan.isNoop && !fovOn)) {
            val maxSigma = maxSigmaPerLevel(tier)
            t.blur.beginFrame()
            val central = if (plan.centralSigma > 0.0) {
                t.blur.blur(blurProgs, srcTex, plan.centralSigma, slot = 0, maxSigma)
            } else srcTex
            val periph = when {
                plan.peripheralSigma == plan.centralSigma -> central
                plan.peripheralSigma > 0.0 -> t.blur.blur(blurProgs, srcTex, plan.peripheralSigma, slot = 1, maxSigma)
                else -> srcTex
            }
            val unsharp = if (plan.unsharpSigma > 0.0) {
                t.blur.blur(blurProgs, srcTex, plan.unsharpSigma, slot = 2, maxSigma)
            } else srcTex

            bindTarget(t.acuity)
            GLES30.glUseProgram(combineProg)
            setTex(combineProg, "uSharp", 0, srcTex)
            setTex(combineProg, "uCentral", 1, central)
            setTex(combineProg, "uPeriph", 2, periph)
            setTex(combineProg, "uUnsharp", 3, unsharp)
            setTex(combineProg, "uFovMask", 4, t.maskFov)
            setTex(combineProg, "uBoost", 5, t.maskBoost)
            GLES30.glUniform1i(GlPipeline.uniform(combineProg, "uFovOn"), if (fovOn) 1 else 0)
            GLES30.glUniform1i(GlPipeline.uniform(combineProg, "uUseUnsharp"), if (plan.unsharpSigma > 0.0) 1 else 0)
            GLES30.glUniform1f(GlPipeline.uniform(combineProg, "uPeripheral"), plan.peripheralScalar.toFloat())
            GLES30.glUniform1f(GlPipeline.uniform(combineProg, "uSharpenAmount"), Acuity.SHARPEN_AMOUNT.toFloat())
            GlPrimitives.drawFsQuad()
            srcTex = t.acuity.tex
        }

        // 5. FINAL composite (thermal + tonemap + before/after modes)
        bindTarget(t.final)
        GLES30.glUseProgram(finalProg)
        setTex(finalProg, "uProc", 0, srcTex)
        setTex(finalProg, "uRaw", 1, t.raw.tex)
        GLES30.glUniform1i(GlPipeline.uniform(finalProg, "uMode"), mode)
        val thermal = Pipeline.isActive(profile, StageId.SPECIAL, tier) && profile.specialVision.infrared != null
        GLES30.glUniform1i(GlPipeline.uniform(finalProg, "uThermal"), if (thermal) 1 else 0)
        // FOV geometry warp: panoramic wrap for wide-field prey, foveal tunnel for raptors.
        // Honors StageId.FOV tier gating (PERFORMANCE tier drops the stage -> no warp).
        val warp = if (fovOn) Fov.warpCoefficients(profile) else floatArrayOf(0f, 0f)
        GLES30.glUniform2fv(GlPipeline.uniform(finalProg, "uWarp"), 1, warp, 0)
        GlPrimitives.drawFsQuad()
    }

    private fun drawCamera(target: RenderTarget, useLut: Boolean, cropX: Float, cropY: Float) {
        bindTarget(target)
        GLES30.glUseProgram(oesProg)
        setTex(oesProg, "uTex", 0, oesTexture, GlPipeline.TEXTURE_EXTERNAL_OES)
        setTex(oesProg, "uLut", 1, lutTexture, GLES30.GL_TEXTURE_3D)
        GLES30.glUniformMatrix4fv(GlPipeline.uniform(oesProg, "uTexMat"), 1, false, texMatrix, 0)
        GLES30.glUniform2f(GlPipeline.uniform(oesProg, "uCropScale"), cropX, cropY)
        GLES30.glUniform1i(GlPipeline.uniform(oesProg, "uUseLut"), if (useLut) 1 else 0)
        GlPrimitives.drawFsQuad()
    }

    private fun updateFps() {
        val now = System.nanoTime()
        noiseFrame++
        frameCount++
        if (frameCount == 1) windowStartNs = now
        val elapsed = (now - windowStartNs) / 1_000_000_000.0
        if (elapsed >= 2.0) {
            val fps = frameCount / elapsed
            onFps?.invoke(fps)
            if (fps < 20.0) {
                lowFpsWindows++
                if (lowFpsWindows >= 2) {
                    // Ask for one tier step down; keep watching so a still-slow device can
                    // step down again after another two slow windows.
                    lowFpsWindows = 0
                    onTierDropRequest?.invoke()
                }
            } else {
                lowFpsWindows = 0
            }
            frameCount = 0
            windowStartNs = now
        }
    }
}
