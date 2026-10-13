package com.animalvision.app.gl

import android.opengl.GLES30
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.stage.Fov

/** One RGBA16F texture + framebuffer. All methods run on the GL thread. */
internal class RenderTarget(val w: Int, val h: Int) {
    val tex: Int = GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F)
    val fbo: Int = GlPipeline.createFbo(tex)

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, w, h)
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        GLES30.glDeleteTextures(1, intArrayOf(tex), 0)
    }
}

/**
 * Every per-resolution GPU resource the pipeline needs for one frame size: the pass targets,
 * the blur pyramid, and the profile's FOV masks. Owned as a unit so a resize (tier change,
 * rotation) or a one-shot capture render can allocate and [release] it without leaking.
 */
internal class FrameTargets(val w: Int, val h: Int) {
    val raw = RenderTarget(w, h)
    val color = RenderTarget(w, h)
    val lowLight = RenderTarget(w, h)
    val acuity = RenderTarget(w, h)
    val final = RenderTarget(w, h)
    val blur = BlurChain(w, h)

    var maskFov = 0
        private set
    var maskBoost = 0
        private set
    private var masksForProfile: String? = null

    /** Regenerate the FOV/boost masks when the profile changes (size is fixed per instance). */
    fun ensureMasks(profile: AnimalVisionProfile) {
        if (profile.id == masksForProfile) return
        deleteMasks()
        maskFov = GlPipeline.createR8Texture(w, h, Fov.acuityMask(profile, w, h))
        maskBoost = GlPipeline.createR8Texture(w, h, Fov.sharpBoostMask(profile, w, h))
        masksForProfile = profile.id
    }

    private fun deleteMasks() {
        if (maskFov != 0) GLES30.glDeleteTextures(2, intArrayOf(maskFov, maskBoost), 0)
        maskFov = 0
        maskBoost = 0
        masksForProfile = null
    }

    fun release() {
        listOf(raw, color, lowLight, acuity, final).forEach { it.release() }
        blur.release()
        deleteMasks()
    }
}
