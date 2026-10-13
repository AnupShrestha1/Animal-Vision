package com.animalvision.app.gl

import android.opengl.GLES30

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
 * Every per-resolution GPU resource the pipeline needs for one frame size: the pass targets
 * and the blur pyramid (FOV masks are evaluated analytically in the combine shader). Owned as
 * a unit so a resize (tier change, rotation) or a one-shot capture render can allocate and
 * [release] it without leaking.
 */
internal class FrameTargets(val w: Int, val h: Int) {
    val raw = RenderTarget(w, h)
    val color = RenderTarget(w, h)
    val lowLight = RenderTarget(w, h)
    val acuity = RenderTarget(w, h)
    /** sRGB-encoded output: shown on screen, recorded, and read back for photos. */
    val final = RenderTarget(w, h, eightBit = true)
    val blur = BlurChain(w, h)

    fun release() {
        listOf(raw, color, lowLight, acuity, final).forEach { it.release() }
        blur.release()
    }
}
