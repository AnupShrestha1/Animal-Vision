package com.animalvision.app.gl

import android.opengl.GLES30
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.stage.Fov

/**
 * One texture + framebuffer. All methods run on the GL thread. Linear passes use RGBA16F;
 * pass [eightBit] for display-encoded output that must be read back with
 * glReadPixels(RGBA, UNSIGNED_BYTE) — that combination is invalid on float targets in
 * GLES 3.0 and silently yields an all-black image.
 */
internal class RenderTarget(val w: Int, val h: Int, eightBit: Boolean = false) {
    val tex: Int = if (eightBit) {
        GlPipeline.createTexture(w, h, GLES30.GL_RGBA8, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE)
    } else {
        GlPipeline.createTexture(w, h, GLES30.GL_RGBA16F)
    }
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
    /** sRGB-encoded output: shown on screen, recorded, and read back for photos. */
    val final = RenderTarget(w, h, eightBit = true)
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
        maskFov = GlPipeline.createR8Texture(w, h, bottomUp(Fov.acuityMask(profile, w, h)))
        maskBoost = GlPipeline.createR8Texture(w, h, bottomUp(Fov.sharpBoostMask(profile, w, h)))
        masksForProfile = profile.id
    }

    /**
     * Core masks are top-row-first (image convention, y down); GL textures and these render
     * targets are bottom-row-first. Without this flip the horse's muzzle blind spot drew at
     * the top of the view.
     */
    private fun bottomUp(mask: FloatArray): FloatArray {
        val out = FloatArray(mask.size)
        for (y in 0 until h) System.arraycopy(mask, y * w, out, (h - 1 - y) * w, w)
        return out
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
