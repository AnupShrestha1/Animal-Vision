package com.animalvision.app.gl

import android.opengl.GLES30
import com.animalvision.core.stage.Acuity

/** Programs used by [BlurChain]; compiled once per GL context by the renderer. */
internal class BlurPrograms(val horizontal: Int, val vertical: Int, val downsample: Int)

/**
 * Gaussian blur of any sigma with a fixed 17-tap shader: large blurs run on a 2×-downsampled
 * pyramid level (see [Acuity.pyramidLevel]) and are upsampled for free by bilinear sampling
 * in the consumer pass. Without this, low-acuity species (bee ≈ 20 px sigma) would be capped
 * at the kernel width and look barely softer than a dog.
 *
 * Several blurs of the same source per frame share the downsample levels; each blur writes
 * to its own [slot] so results stay valid until the frame is composited.
 */
internal class BlurChain(private val baseW: Int, private val baseH: Int) {

    private val down = ArrayList<RenderTarget>() // down[i] is pyramid level i + 1
    private val scratch = HashMap<Int, RenderTarget>() // per level: horizontal-pass target
    private val outputs = HashMap<Int, RenderTarget>() // key = slot * 16 + level
    private var downSource = 0
    private var downValid = 0
    private val kernel = FloatArray(17)

    /** Invalidate cached downsample levels; call once per frame before any [blur]. */
    fun beginFrame() {
        downSource = 0
        downValid = 0
    }

    /**
     * Blur [srcTex] (a [baseW]×[baseH] texture) by [sigma] base pixels into [slot] and return
     * the resulting texture (possibly at a lower resolution). [maxSigmaPerLevel] trades quality
     * for taps: smaller values blur on coarser levels with fewer samples.
     */
    fun blur(progs: BlurPrograms, srcTex: Int, sigma: Double, slot: Int, maxSigmaPerLevel: Double): Int {
        val (level, levelSigma) = Acuity.pyramidLevel(sigma, maxSigmaPerLevel)
        ensureDownsampled(progs, srcTex, level)
        val input = if (level == 0) srcTex else down[level - 1].tex
        // The box prefilters alone already realize this blur.
        if (levelSigma < 0.05) return input

        val (w, h) = levelSize(level)
        val tmp = scratch.getOrPut(level) { RenderTarget(w, h) }
        val out = outputs.getOrPut(slot * 16 + level) { RenderTarget(w, h) }
        val radius = buildKernel(levelSigma)
        pass(progs.horizontal, input, tmp, radius)
        pass(progs.vertical, tmp.tex, out, radius)
        return out.tex
    }

    private fun levelSize(level: Int): Pair<Int, Int> {
        var w = baseW
        var h = baseH
        repeat(level) {
            w = Math.max(1, (w + 1) / 2)
            h = Math.max(1, (h + 1) / 2)
        }
        return w to h
    }

    private fun ensureDownsampled(progs: BlurPrograms, srcTex: Int, level: Int) {
        if (srcTex != downSource) {
            downSource = srcTex
            downValid = 0
        }
        for (i in downValid + 1..level) {
            if (down.size < i) {
                val (w, h) = levelSize(i)
                down += RenderTarget(w, h)
            }
            val target = down[i - 1]
            target.bind()
            GLES30.glUseProgram(progs.downsample)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, if (i == 1) srcTex else down[i - 2].tex)
            GLES30.glUniform1i(GlPipeline.uniform(progs.downsample, "uTex"), 0)
            GlPrimitives.drawFsQuad()
        }
        if (level > downValid) downValid = level
    }

    /** Center the (never truncated, sigma ≤ ~2.6) kernel on slot 8; returns its radius. */
    private fun buildKernel(sigma: Double): Int {
        val k = Acuity.gaussianKernel(sigma)
        val radius = Math.min((k.size - 1) / 2, 8)
        java.util.Arrays.fill(kernel, 0f)
        val kc = (k.size - 1) / 2
        var sum = 0f
        for (o in -radius..radius) {
            kernel[8 + o] = k[kc + o]
            sum += k[kc + o]
        }
        if (sum > 1e-6f) for (i in kernel.indices) kernel[i] /= sum
        return radius
    }

    private fun pass(prog: Int, input: Int, target: RenderTarget, radius: Int) {
        target.bind()
        GLES30.glUseProgram(prog)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, input)
        GLES30.glUniform1i(GlPipeline.uniform(prog, "uTex"), 0)
        GLES30.glUniform1i(GlPipeline.uniform(prog, "uRadius"), radius)
        GLES30.glUniform1fv(GlPipeline.uniform(prog, "uKernel"), 17, kernel, 0)
        GLES30.glUniform2f(GlPipeline.uniform(prog, "uTexel"), 1f / target.w, 1f / target.h)
        GlPrimitives.drawFsQuad()
    }

    fun release() {
        down.forEach { it.release() }
        scratch.values.forEach { it.release() }
        outputs.values.forEach { it.release() }
        down.clear()
        scratch.clear()
        outputs.clear()
        beginFrame()
    }
}
