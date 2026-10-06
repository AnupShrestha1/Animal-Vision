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

    /** Regenerate FOV/boost/visibility masks whenever profile or resolution changes. */
    private fun ensureMasks(profile: AnimalVisionProfile) {
        val key = profile.id + ":" + tierW
        if (key == masksForProfile) return
        maskFov = GlPipeline.createR8Texture(tierW, tierH, Fov.acuityMask(profile, tierW, tierH))
        maskBoost = GlPipeline.createR8Texture(tierW, tierH, Fov.sharpBoostMask(profile, tierW, tierH))
        val vis = FloatArray(tierW * tierH) { i ->
            val m = Fov.acuityMask(profile, tierW, tierH)[i]
            0.05f + 0.95f * m
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
        GLES30.glUniform1i(GLES30.glGetUniformLocation(oesProg, "uUseLut"), 0)
        GlPrimitives.drawFsQuad()

        // 2. COLOR (species LUT)
        bindFbo(fboA)
        GLES30.glUseProgram(oesProg)
        setTex(oesProg, "uTex", 0, oesTexture, GlPipeline.TEXTURE_EXTERNAL_OES)
        setTex(oesProg, "uLut", 1, lutTexture, GLES30.GL_TEXTURE_3D)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(oesProg, "uTexMat"), 1, false, texMatrix, 0)
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
        val kc = k.size / 2
        for (i in k.indices) {
            val slot = i - kc + kernelRadius
            if (slot in 0..16) kernelUniform[slot] = k[i]
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
                void main() { vUv = vec2(aPos.x, -aPos.y) * 0.5 + 0.5; gl_Position = vec4(aPos, 0.0, 1.0); }
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