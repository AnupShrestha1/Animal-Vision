package com.animalvision.app.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Fullscreen quad + framebuffer readback helpers. */
object GlPrimitives {

    private var quadVao = -1

    fun bindQuad() {
        if (quadVao < 0) {
            val verts = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
            val bb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder())
            bb.asFloatBuffer().put(verts).flip()
            val vbo = IntArray(1)
            GLES30.glGenBuffers(1, vbo, 0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, bb, GLES30.GL_STATIC_DRAW)
            val vao = IntArray(1)
            GLES30.glGenVertexArrays(1, vao, 0)
            GLES30.glBindVertexArray(vao[0])
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0)
            quadVao = vao[0]
        }
        GLES30.glBindVertexArray(quadVao)
    }

    /** The VAO belongs to the old context after an EGL context loss; rebuild lazily. */
    fun resetContextState() {
        quadVao = -1
    }

    fun drawFsQuad() {
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    /** Read the currently bound framebuffer's color as RGBA8888 (top-left origin). */
    fun readPixels(w: Int, h: Int): IntArray {
        val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        GLES30.glPixelStorei(GLES30.GL_PACK_ALIGNMENT, 1)
        GLES30.glReadPixels(0, 0, w, h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buf)
        buf.rewind()
        val out = IntArray(w * h)
        for (i in out.indices) {
            val r = buf.get().toInt() and 0xff
            val g = buf.get().toInt() and 0xff
            val b = buf.get().toInt() and 0xff
            buf.get()
            out[i] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}