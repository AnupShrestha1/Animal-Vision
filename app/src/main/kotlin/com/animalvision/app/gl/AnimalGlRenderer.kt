package com.animalvision.app.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGL14.*
import android.opengl.EGLDisplay
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.animalvision.core.color.ColorTransform
import com.animalvision.core.color.LutGenerator
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.model.StageId
import com.animalvision.core.pipeline.Pipeline
import com.animalvision.core.stage.Acuity
import com.animalvision.core.stage.Fov
import javax.microedition.khronos.egl.EGLConfig as KEGLConfig

/**
 * GLSurfaceView.Renderer running the vision pipeline on the camera's external texture.
 * Flow: OES -> linear -> LUT(color) -> lowlight -> acuity blur -> FOV masks -> final
 * composite (thermal + tonemap + before/after modes) -> display. Captures read FBO_FINAL.
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

    @Volatile internal var eglDisplay: EGLDisplay? = null
    @Volatile internal var eglContext: android.opengl.EGLContext? = null
    @Volatile internal var windowSurface: android.opengl.EGLSurface? = null

    private var oesTexture = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraTexSize = intArrayOf(1280, 720)

    private var oesProg = 0
    private var lowlightProg = 0
    private var blurHProg = 0
    private var blurVProg = 0
    private var finalProg = 0

    private var tierW = 0
    private var tierH = 0
    private var texA = 0; private var fboA = 0
    private var texB = 0; private var fboB = 0
    private var texC = 0; private var fboC = 0
    private var texD = 0; private var fboD = 0
    private var texRaw = 0; private var fboRaw = 0
    private var texFinal = 0; private var fboFinal = 0
    private var lutTexture = 0
    private var maskFov = 0
    private var maskBoost = 0
    private var maskVis = 0
    private var masksForProfile: String? = null

    private var pendingLut: Pair<FloatArray, Int>? = null
    private var generatedForProfile: String? = null

    private val kernelUniform = FloatArray(17)
    private var kernelRadius = 0
    private var loggedTransform = 0
    private val lastLoggedMatrix = FloatArray(16)
    @Volatile private var displayRotationDeg = 0
    // Scratch matrices for the display-rotation re-frame (pre-allocated, GL thread only).
    private val rotR = FloatArray(16)
    private val rotT1 = FloatArray(16)
    private val rotT2 = FloatArray(16)
    private val rotBase = FloatArray(16)
    private val texMatrix = FloatArray(16).also {
        it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f
    }

    private var frameCount = 0
    private var windowStartNs = 0L
    private var lowFpsWindows = 0
    private var degraded = false

    var videoRecorder: VideoRecorder? = null

    // ---- photo capture (spec §29): read FBO_FINAL back on the GL thread ----
    @Volatile private var pendingCapture: ((android.graphics.Bitmap) -> Unit)? = null

    /** Must be called before/while the GL thread is rendering; callback runs on the GL thread. */
    fun requestCapture(callback: (android.graphics.Bitmap) -> Unit) {
        pendingCapture = callback
    }

    private fun maybeCapture() {
        val cb = pendingCapture ?: return
        pendingCapture = null
        if (fboFinal == 0 || tierW == 0 || tierH == 0) return
        val buf = java.nio.ByteBuffer.allocate(tierW * tierH * 4).order(java.nio.ByteOrder.nativeOrder())
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboFinal)
        GLES30.glReadPixels(0, 0, tierW, tierH, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
        bindDefault()
        buf.rewind()
        val src = android.graphics.Bitmap.createBitmap(tierW, tierH, android.graphics.Bitmap.Config.ARGB_8888)
        src.copyPixelsFromBuffer(buf)
        // glReadPixels origin is bottom-left; flip to top-left.
        val m = android.graphics.Matrix().apply { postScale(1f, -1f) }
        val out = android.graphics.Bitmap.createBitmap(src, 0, 0, tierW, tierH, m, false)
        if (out !== src) src.recycle()
        cb(out)
    }

    /** Start recording the processed output. Call from the GL thread (view.queueEvent). */
    fun startRecordingOnGlThread(file: java.io.File, onError: (String) -> Unit) {
        stopRecordingOnGlThread()
        if (tierW == 0) { onError("Renderer not ready"); return }
        val rec = VideoRecorder(file, tierW, tierH)
        try {
            rec.start()
            videoRecorder = rec
        } catch (e: Exception) {
            runCatching { rec.stop() }
            videoRecorder = null
            onError(e.message ?: "Recorder unavailable")
        }
    }

    /** Stop recording: signal EOS on the GL thread, tear down off-thread; [onStopped] runs after teardown. */
    fun stopRecordingOnGlThread(onStopped: (() -> Unit)? = null) {
        val rec = videoRecorder ?: return
        videoRecorder = null
        rec.signalEndOfStream()
        Thread {
            runCatching { rec.stop() }
            onStopped?.invoke()
        }.start()
    }

    // EGL state is sampled per-frame from the current context (avoids fragile custom factories).
    private fun sampleEglState() {
        eglDisplay = EGL14.eglGetCurrentDisplay()
        eglContext = EGL14.eglGetCurrentContext()
        windowSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
    }

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: KEGLConfig?) {
        oesProg = GlPipeline.compileProgram(Shaders.VERT, Shaders.FRAG_OES)
        lowlightProg = GlPipeline.compileProgram(Shaders.VERT, Shaders.FRAG_LOWLIGHT)
        blurHProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_BLUR_H)
        blurVProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_BLUR_V)
        finalProg = GlPipeline.compileProgram(Shaders.VERT, Shaders2.FRAG_FINAL)
        oesTexture = GlPipeline.createOesTexture()
        val st = SurfaceTexture(oesTexture)
        st.setDefaultBufferSize(cameraTexSize[0], cameraTexSize[1])
        surfaceTexture = st
        onSurfaceTextureReady?.invoke(st)
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        screenW = width
        screenH = height
        val (tw, th) = internalSize(width, height, tierProvider())
        if (tw != tierW || th != tierH) allocateTextures(tw, th)
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
     * portrait transform; each frame we compose T(0.5)·R(−deg)·T(−0.5) in front
     * of it — identity when upright (the proven portrait path). The corrected
     * matrix feeds both the shader and computeCrop()'s cover-crop measurement.
     */
    fun setDisplayRotationDegrees(deg: Int) {
        displayRotationDeg = ((deg % 360) + 360) % 360
    }

    companion object {
        fun internalSize(screenW: Int, screenH: Int, tier: QualityTier): Pair<Int, Int> {
            val maxDim = when (tier) {
                QualityTier.HIGH -> 1280
                QualityTier.BALANCED -> 960
                QualityTier.PERFORMANCE -> 640
            }
            val scale = Math.min(1.0, maxDim.toDouble() / Math.max(screenW, screenH))
            val w = Math.max(2, (screenW * scale).toInt() and -2)
            val h = Math.max(2, (screenH * scale).toInt() and -2)
            return w to h
        }
    }
// ---- internal resources ---------------------------------------------------
    private fun allocateTextures(w: Int, h: Int) {
        tierW = w; tierH = h
        texA = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboA = GlPipeline.createFbo(texA)
        texB = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboB = GlPipeline.createFbo(texB)
        texC = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboC = GlPipeline.createFbo(texC)
        texD = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboD = GlPipeline.createFbo(texD)
        texRaw = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboRaw = GlPipeline.createFbo(texRaw)
        texFinal = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F); fboFinal = GlPipeline.createFbo(texFinal)
    }

    private fun bindFbo(fbo: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, tierW, tierH)
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    }

    private fun bindDefault() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun setTex(prog: Int, name: String, unit: Int, tex: Int, target: Int = GLES30.GL_TEXTURE_2D) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(target, tex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(prog, name), unit)
    }

    /**
     * Cover-fit mapping: keeps the camera frame's aspect instead of stretching it
     * into the tierW x tierH quad.
     *
     * The sampled image's true aspect is MEASURED from the SurfaceTexture transform:
     * the quad's U/V edges mapped through [texMatrix] into buffer pixels
     * (using the real buffer size in [cameraTexSize]). This is robust to the 90°
     * sensor rotation, mirroring, and any crop CameraX bakes into the matrix.
     * The narrower axis is then center-cropped (scale <= 1, never a stretch).
     */
    private fun computeCrop(): Pair<Float, Float> {
        var sx = 1f
        var sy = 1f
        val wb = cameraTexSize[0].toDouble()
        val hb = cameraTexSize[1].toDouble()
        if (tierW > 0 && tierH > 0 && wb > 0 && hb > 0) {
            // Quad edges through the transform (column-major 4x4, rotation in
            // columns 0/1; translation does not affect edge vectors).
            val lenU = Math.hypot(texMatrix[0] * wb, texMatrix[1] * hb)
            val lenV = Math.hypot(texMatrix[4] * wb, texMatrix[5] * hb)
            if (lenU > 1e-6 && lenV > 1e-6) {
                val contentAspect = lenU / lenV
                val tierAspect = tierW.toDouble() / tierH.toDouble()
                if (contentAspect > tierAspect) {
                    // Content relatively wider: crop width.
                    sx = (tierAspect / contentAspect).toFloat()
                } else if (contentAspect < tierAspect) {
                    // Content relatively taller: crop height.
                    sy = (contentAspect / tierAspect).toFloat()
                }
                if (sx.isNaN() || sx <= 0f || sx > 1f) sx = 1f
                if (sy.isNaN() || sy <= 0f || sy > 1f) sy = 1f
            }
        }
        return sx to sy
    }

    private var lastLoggedSx = -1f
    private var lastLoggedSy = -1f
    private var cropLogCount = 0

    private fun setCropUniform(prog: Int) {
        val (sx, sy) = computeCrop()
        // Log every crop change (bounded) so device logs show the measured geometry.
        if ((sx != lastLoggedSx || sy != lastLoggedSy) && cropLogCount < 20) {
            lastLoggedSx = sx
            lastLoggedSy = sy
            cropLogCount++
            android.util.Log.i(
                "AnimalVision",
                String.format(
                    java.util.Locale.US,
                    "AVDBG crop sx=%.3f sy=%.3f tier=%dx%d cam=%dx%d",
                    sx, sy, tierW, tierH, cameraTexSize[0], cameraTexSize[1],
                ),
            )
        }
        val loc = GLES30.glGetUniformLocation(prog, "uCropScale")
        if (loc >= 0) GLES30.glUniform2f(loc, sx, sy)
    }

    /** Regenerate FOV/boost/visibility masks whenever profile or resolution changes. */
    private fun ensureMasks(profile: AnimalVisionProfile) {
        val key = profile.id + ":" + tierW
        if (key == masksForProfile) return
        // Compute the acuity mask ONCE — it is O(w*h), and reusing it keeps mask
        // generation linear. (Recomputing it per pixel here was O((w*h)^2) and hung
        // the GL thread on the first frame, leaving the viewfinder black.)
        val acuity = Fov.acuityMask(profile, tierW, tierH)
        // Free the previous generation's textures before replacing them.
        if (maskFov != 0) GLES30.glDeleteTextures(3, intArrayOf(maskFov, maskBoost, maskVis), 0)
        maskFov = GlPipeline.createR8Texture(tierW, tierH, acuity)
        maskBoost = GlPipeline.createR8Texture(tierW, tierH, Fov.sharpBoostMask(profile, tierW, tierH))
        val vis = FloatArray(tierW * tierH) { i ->
            0.05f + 0.95f * acuity[i]
        }
        maskVis = GlPipeline.createR8Texture(tierW, tierH, vis)
        masksForProfile = key
    }

    /** Kick off LUT generation off-thread; upload when ready on the GL thread. */
    fun setProfile(profile: AnimalVisionProfile) {
        if (generatedForProfile == profile.id && lutTexture != 0) return
        generatedForProfile = profile.id
        Thread {
            val lut = LutGenerator.generate(ColorTransform(profile), profile.colorVision.lutSize)
            pendingLut = lut.data to lut.size
        }.start()
    }

    private fun uploadPendingLut() {
        val p = pendingLut ?: return
        pendingLut = null
        val (data, size) = p
        if (lutTexture != 0) GLES30.glDeleteTextures(1, intArrayOf(lutTexture), 0)
        lutTexture = GlPipeline.createLutTexture3d(data, size)
    }
// ---- per-frame pipeline -----------------------------------------------------
    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        val st = surfaceTexture ?: return
        if (tierW == 0) return
        sampleEglState()
        st.updateTexImage()
        st.getTransformMatrix(texMatrix)
        uploadPendingLut()
        // Re-frame from the always-portrait camera transform to the current
        // display rotation (identity when upright — the proven portrait path).
        val rotDeg = displayRotationDeg
        if (rotDeg != 0) {
            android.opengl.Matrix.setRotateM(rotR, 0, -rotDeg.toFloat(), 0f, 0f, 1f)
            android.opengl.Matrix.setIdentityM(rotT1, 0)
            android.opengl.Matrix.translateM(rotT1, 0, -0.5f, -0.5f, 0f)
            android.opengl.Matrix.multiplyMM(rotT2, 0, rotR, 0, rotT1, 0)   // R·T(−0.5)
            android.opengl.Matrix.setIdentityM(rotR, 0)
            android.opengl.Matrix.translateM(rotR, 0, 0.5f, 0.5f, 0f)       // T(0.5)
            android.opengl.Matrix.multiplyMM(rotT1, 0, rotR, 0, rotT2, 0)    // T(0.5)·R·T(−0.5)
            System.arraycopy(texMatrix, 0, rotBase, 0, 16)
            android.opengl.Matrix.multiplyMM(texMatrix, 0, rotT1, 0, rotBase, 0)
        }
        var matrixChanged = false
        for (k in 0 until 15) {
            if (texMatrix[k] != lastLoggedMatrix[k]) { matrixChanged = true; break }
        }
        if (matrixChanged && loggedTransform < 10) {
            loggedTransform++
            System.arraycopy(texMatrix, 0, lastLoggedMatrix, 0, 16)
            val c = StringBuilder()
            val corners = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
            for (k in 0 until 4) {
                val u = texMatrix[0] * corners[k * 2] + texMatrix[4] * corners[k * 2 + 1] + texMatrix[12]
                val v = texMatrix[1] * corners[k * 2] + texMatrix[5] * corners[k * 2 + 1] + texMatrix[13]
                c.append(String.format("(%.3f,%.3f)", u, v))
            }
            android.util.Log.i(
                "AnimalVision",
                "AVDBG uv#$loggedTransform tier=${tierW}x${tierH} screen=${screenW}x${screenH} " +
                    "cam=${cameraTexSize[0]}x${cameraTexSize[1]} uv=$c",
            )
        }

        // Tier change -> reallocate the internal chain (cheap size check).
        if (screenW > 0 && screenH > 0) {
            val (nw, nh) = internalSize(screenW, screenH, tierProvider())
            if (nw != tierW || nh != tierH) allocateTextures(nw, nh)
        }

        val profile = profileProvider()
        val tier = tierProvider()
        val mode = modeProvider()
        ensureMasks(profile)
        GlPrimitives.bindQuad()

        // 1. RAW linear camera (for before/after "normal" side)
        bindFbo(fboRaw)
        GLES30.glUseProgram(oesProg)
        setTex(oesProg, "uTex", 0, oesTexture, GlPipeline.TEXTURE_EXTERNAL_OES)
        setTex(oesProg, "uLut", 1, lutTexture, GLES30.GL_TEXTURE_3D)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(oesProg, "uTexMat"), 1, false, texMatrix, 0)
        setCropUniform(oesProg)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(oesProg, "uUseLut"), 0)
        GlPrimitives.drawFsQuad()

        // 2. COLOR (species LUT)
        bindFbo(fboA)
        GLES30.glUseProgram(oesProg)
        setTex(oesProg, "uTex", 0, oesTexture, GlPipeline.TEXTURE_EXTERNAL_OES)
        setTex(oesProg, "uLut", 1, lutTexture, GLES30.GL_TEXTURE_3D)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(oesProg, "uTexMat"), 1, false, texMatrix, 0)
        setCropUniform(oesProg)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(oesProg, "uUseLut"), 1)
        GlPrimitives.drawFsQuad()

        // 3. LOWLIGHT (contrast-preserving)
        var srcTex = texA
        if (Pipeline.isActive(profile, StageId.BRIGHTNESS, tier) && profile.lowLight.enabled && lowLightGate()) {
            bindFbo(fboB)
            GLES30.glUseProgram(lowlightProg)
            setTex(lowlightProg, "uTex", 0, texA)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(lowlightProg, "uLift"), profile.lowLight.shadowLift.toFloat())
            GLES30.glUniform1f(GLES30.glGetUniformLocation(lowlightProg, "uDesat"), profile.lowLight.maxDesaturation.toFloat())
            GLES30.glUniform1f(GLES30.glGetUniformLocation(lowlightProg, "uNoiseSigma"), profile.lowLight.noiseSigma.toFloat())
            GLES30.glUniform1f(GLES30.glGetUniformLocation(lowlightProg, "uSeed"), (frameCount % 1000).toFloat())
            GlPrimitives.drawFsQuad()
            srcTex = texB
        }

        // 4. ACUITY (separable blur + FOV combination)
        val sigma = Acuity.blurSigmaPixels(profile.acuity.cutoffCpd)
        if (Pipeline.isActive(profile, StageId.ACUITY, tier) && sigma >= 0.35) {
            val maxRadius = when (tier) {
                QualityTier.PERFORMANCE -> 5
                QualityTier.BALANCED -> 9
                QualityTier.HIGH -> 17
            }
            kernelRadius = Math.min(Math.min((sigma * 3).toInt(), maxRadius / 2), 8)
            buildKernel(sigma)
            val texel = floatArrayOf(1f / tierW, 1f / tierH)

            bindFbo(fboC)
            GLES30.glUseProgram(blurHProg)
            setTex(blurHProg, "uTex", 0, srcTex)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(blurHProg, "uRadius"), kernelRadius)
            GLES30.glUniform1fv(GLES30.glGetUniformLocation(blurHProg, "uKernel"), 17, kernelUniform, 0)
            GLES30.glUniform2fv(GLES30.glGetUniformLocation(blurHProg, "uTexel"), 1, texel, 0)
            GlPrimitives.drawFsQuad()

            bindFbo(fboD)
            GLES30.glUseProgram(blurVProg)
            setTex(blurVProg, "uBlur", 0, texC)
            setTex(blurVProg, "uSharp", 1, srcTex)
            setTex(blurVProg, "uFovMask", 2, maskFov)
            setTex(blurVProg, "uBoost", 3, maskBoost)
            setTex(blurVProg, "uVis", 4, maskVis)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(blurVProg, "uRadius"), kernelRadius)
            GLES30.glUniform1fv(GLES30.glGetUniformLocation(blurVProg, "uKernel"), 17, kernelUniform, 0)
            GLES30.glUniform2fv(GLES30.glGetUniformLocation(blurVProg, "uTexel"), 1, texel, 0)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(blurVProg, "uSharpenAmount"), 0.35f)
            GlPrimitives.drawFsQuad()
            srcTex = texD
        }
// 5. FINAL composite (thermal + tonemap + before/after modes)
        bindFbo(fboFinal)
        GLES30.glUseProgram(finalProg)
        setTex(finalProg, "uProc", 0, srcTex)
        setTex(finalProg, "uRaw", 1, texRaw)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(finalProg, "uMode"), mode)
        val thermal = Pipeline.isActive(profile, StageId.SPECIAL, tier) && profile.specialVision.infrared != null
        GLES30.glUniform1i(GLES30.glGetUniformLocation(finalProg, "uThermal"), if (thermal) 1 else 0)
        // FOV geometry warp: panoramic wrap for wide-field prey, foveal tunnel for raptors.
        // Honors StageId.FOV tier gating (PERFORMANCE tier drops the stage -> no warp).
        val warp = if (Pipeline.isActive(profile, StageId.FOV, tier)) {
            Fov.warpCoefficients(profile)
        } else {
            floatArrayOf(0f, 0f)
        }
        GLES30.glUniform2fv(GLES30.glGetUniformLocation(finalProg, "uWarp"), 1, warp, 0)
        GlPrimitives.drawFsQuad()

        // video: render FBO_FINAL into the MediaCodec input surface
        videoRecorder?.let { rec ->
            rec.renderFrame(
                texFinal, tierW, tierH, System.nanoTime() / 1000L,
                eglDisplay, eglContext, windowSurface,
            )
        }

        // photo: read back the final image if a capture was requested
        maybeCapture()

        // 6. display
        bindDefault()
        GLES30.glViewport(0, 0, screenW, screenH)
        GLES30.glUseProgram(screenProg())
        setTex(screenProg(), "uTex", 0, texFinal)
        GlPrimitives.drawFsQuad()

        updateFps()
    }

    private fun buildKernel(sigma: Double) {
        val k = Acuity.gaussianKernel(sigma)
        java.util.Arrays.fill(kernelUniform, 0f)
        // Center the truncated kernel on slot 8 and RENORMALIZE the kept taps, so the
        // kept energy sums to 1. The blur shaders loop k = -8..8 with offset k,
        // sampling slots [8 - radius, 8 + radius]: a symmetric 2*radius+1 window
        // around the center tap.
        val kc = k.size / 2 // center index of the full kernel
        val center = 8
        var sum = 0f
        for (slot in 0..16) {
            val ki = kc + (slot - center)
            if (ki in k.indices) {
                kernelUniform[slot] = k[ki]
                if (slot >= center - kernelRadius && slot <= center + kernelRadius) {
                    sum += k[ki]
                }
            }
        }
        if (sum > 1e-6f) {
            for (slot in 0..16) kernelUniform[slot] = kernelUniform[slot] / sum
        }
    }

    private var screenProgId = 0
    private var screenW = 0
    private var screenH = 0
    private fun screenProg(): Int {
        if (screenProgId == 0) {
            val v = """
                #version 300 es
                precision mediump float;
                in vec2 aPos;
                out vec2 vUv;
                void main() { vUv = aPos * 0.5 + 0.5; gl_Position = vec4(aPos, 0.0, 1.0); }
            """.trimIndent()
            val f = """
                #version 300 es
                precision mediump float;
                in vec2 vUv;
                uniform sampler2D uTex;
                out vec4 outColor;
                void main() { outColor = texture(uTex, vUv); }
            """.trimIndent()
            screenProgId = GlPipeline.compileProgram(v, f)
        }
        return screenProgId
    }

    private fun updateFps() {
        val now = System.nanoTime()
        frameCount++
        if (frameCount == 1) windowStartNs = now
        val elapsed = (now - windowStartNs) / 1_000_000_000.0
        if (elapsed >= 2.0) {
            val fps = frameCount / elapsed
            onFps?.invoke(fps)
            if (fps < 20.0) {
                lowFpsWindows++
                if (lowFpsWindows >= 2 && !degraded) {
                    degraded = true
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