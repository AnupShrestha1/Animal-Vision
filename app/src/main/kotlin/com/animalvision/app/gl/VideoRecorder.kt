package com.animalvision.app.gl

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.view.Surface
import java.io.File

/**
 * Records the PROCESSED GL output as MP4: the renderer draws FBO_FINAL's texture into the
 * MediaCodec input surface through the same EGL context, then presents with a playback timestamp.
 * Audio is omitted (optional per spec §30); orientation follows the camera.
 */
class VideoRecorder(
    private val outputFile: File,
    val width: Int,
    val height: Int,
    private val bitrate: Int = 8_000_000,
    private val fps: Int = 30,
) {
    private var codec: MediaCodec? = null
    private var videoSurface: Surface? = null
    private var eglVideoSurface: EGLSurface? = null
    private var muxer: MediaMuxer? = null
    private var muxerStarted = false
    private var trackIndex = -1
    private var drainThread: Thread? = null
    @Volatile private var running = false
    @Volatile private var eosSignaled = false
    private var lastDisplay: EGLDisplay? = null
    private var copyProgram = 0

    fun start() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        }
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoSurface = c.createInputSurface()
        c.start()
        codec = c
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        running = true
        drainThread = Thread { drainLoop() }.apply { start() }
    }

    private fun drainLoop() {
        val c = codec ?: return
        val info = MediaCodec.BufferInfo()
        while (running) {
            val idx = c.dequeueOutputBuffer(info, 10_000)
            when {
                idx >= 0 -> {
                    val buffer = c.getOutputBuffer(idx) ?: continue
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0) {
                        if (!muxerStarted) {
                            runCatching { trackIndex = muxer!!.addTrack(c.outputFormat); muxer!!.start(); muxerStarted = true }
                        }
                        if (muxerStarted) {
                            runCatching { muxer!!.writeSampleData(trackIndex, buffer!!, info) }
                        }
                    }
                    c.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        runCatching { trackIndex = muxer!!.addTrack(c.outputFormat); muxer!!.start(); muxerStarted = true }
                    }
                }
            }
        }
    }

    /**
     * Renders [srcTexture] (an RGBA texture) into the encoder surface. Must be called on the GL
     * thread with the renderer's EGL display/context; returns to [windowSurface] afterwards.
     */
    fun renderFrame(
        srcTexture: Int,
        srcW: Int,
        srcH: Int,
        /** Frame timestamp in NANOSECONDS — what eglPresentationTimeANDROID expects. */
        presentationTimeNs: Long,
        display: EGLDisplay?,
        context: EGLContext?,
        windowSurface: EGLSurface?,
    ) {
        val c = codec ?: return
        val d = display ?: return
        val ctx = context ?: return
        if (eglVideoSurface == null) {
            val cfg = chooseConfig(d) ?: return
            eglVideoSurface = EGL14.eglCreateWindowSurface(d, cfg, videoSurface, intArrayOf(EGL14.EGL_NONE), 0)
            lastDisplay = d
        }
        val surf = eglVideoSurface ?: return
        if (!EGL14.eglMakeCurrent(d, surf, surf, ctx)) return

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, width, height)
        if (copyProgram == 0) copyProgram = makeCopyProgram(srcW, srcH)
        GLES30.glUseProgram(copyProgram)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, srcTexture)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(copyProgram, "uTex"), 0)
        GlPrimitives.bindQuad()
        GlPrimitives.drawFsQuad()

        android.opengl.EGLExt.eglPresentationTimeANDROID(d, surf, presentationTimeNs)
        EGL14.eglSwapBuffers(d, surf)

        if (windowSurface != null) {
            EGL14.eglMakeCurrent(d, windowSurface, windowSurface, ctx)
        }
    }

    /** Pick an ES3 window-capable config on [d] for the encoder's input surface. */
    private fun chooseConfig(d: EGLDisplay): EGLConfig? {
        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, 0x0040 /* EGL_OPENGL_ES3_BIT */,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val cfgs = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        return if (EGL14.eglChooseConfig(d, attribs, 0, cfgs, 0, 1, n, 0) && n[0] > 0) cfgs[0] else null
    }

    private fun makeCopyProgram(srcW: Int, srcH: Int): Int = GlPipeline.compileProgram(
        """
        #version 300 es
        layout(location = 0) in vec2 aPos;
        out vec2 vUv;
        void main() {
            // Same orientation as the on-screen pass: the encoder surface is not y-flipped.
            vUv = aPos * 0.5 + 0.5;
            gl_Position = vec4(aPos, 0.0, 1.0);
        }
        """.trimIndent(),
        """
        #version 300 es
        precision mediump float;
        in vec2 vUv;
        uniform sampler2D uTex;
        out vec4 outColor;
        void main() { outColor = texture(uTex, vUv); }
        """.trimIndent(),
    )

    /** Signals end-of-stream; call once from the GL thread. Teardown happens in [stop]. */
    fun signalEndOfStream() {
        if (eosSignaled) return
        eosSignaled = true
        codec?.let { runCatching { it.signalEndOfInputStream() } }
    }

    /** Safe to call from any thread after [signalEndOfStream]. */
    fun stop() {
        if (!running && codec == null) return
        running = false
        val c = codec
        if (c != null) {
            if (!eosSignaled) runCatching { c.signalEndOfInputStream() }
            eosSignaled = true
            drainThread?.join(3000)
            runCatching { c.stop() }
            runCatching { c.release() }
        }
        eglVideoSurface?.let { surf ->
            runCatching { EGL14.eglDestroySurface(lastDisplay ?: EGL14.eglGetCurrentDisplay(), surf) }
        }
        videoSurface?.release()
        runCatching { if (muxerStarted) muxer?.stop() }
        runCatching { muxer?.release() }
        codec = null
        videoSurface = null
        eglVideoSurface = null
        drainThread = null
    }
}