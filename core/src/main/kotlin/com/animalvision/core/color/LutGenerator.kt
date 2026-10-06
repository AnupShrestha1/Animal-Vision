package com.animalvision.core.color

/** A 3D color lookup table stored flattened RGBA-interleaved (order: cube index r + size*(g + size*b)). */
class Lut(val size: Int, val data: FloatArray) {
    init {
        require(data.size == size * size * size * 4) { "LUT buffer must be size^3*4 floats" }
    }

    val rDim: Int get() = size

    /** Trilinear sample of the LUT at continuous coordinates 0..1 in R,G,B (sRGB input coords). */
    fun sample(r: Double, g: Double, b: Double): DoubleArray {
        val s = size.toDouble()
        val fr = (r.coerceIn(0.0, 1.0)) * (s - 1.0)
        val fg = (g.coerceIn(0.0, 1.0)) * (s - 1.0)
        val fb = (b.coerceIn(0.0, 1.0)) * (s - 1.0)
        val r0 = fr.toInt().coerceIn(0, size - 2)
        val g0 = fg.toInt().coerceIn(0, size - 2)
        val b0 = fb.toInt().coerceIn(0, size - 2)
        val dr = fr - r0; val dg = fg - g0; val db = fb - b0
        val out = DoubleArray(4)
        for (slice in intArrayOf(0, 1)) {
            for (row in intArrayOf(0, 1)) {
                for (col in intArrayOf(0, 1)) {
                    val w = (if (col == 1) dr else 1.0 - dr) *
                        (if (row == 1) dg else 1.0 - dg) *
                        (if (slice == 1) db else 1.0 - db)
                    val idx = ((r0 + col) + size * ((g0 + row) + size * (b0 + slice))) * 4
                    for (c in 0..3) out[c] += data[idx + c] * w
                }
            }
        }
        return out
    }
}

/**
 * Builds a 3D LUT for a [ColorTransform]. The LUT is keyed on LINEARIZED sRGB input and stores
 * LINEAR display values (alpha = 1). The GPU/CPU pipeline linearizes the camera frame before
 * sampling and applies tone-map + sRGB encoding only at the very end, so intermediate stages
 * (blur, desaturation) operate in linear space.
 */
object LutGenerator {
    fun generate(transform: ColorTransform, size: Int = 33): Lut {
        val data = FloatArray(size * size * size * 4)
        var idx = 0
        for (b in 0 until size) {
            for (g in 0 until size) {
                for (r in 0 until size) {
                    val lin = doubleArrayOf(
                        r / (size - 1.0),
                        g / (size - 1.0),
                        b / (size - 1.0),
                    )
                    val out = transform.applyLinear(lin)
                    data[idx] = out[0].coerceIn(0.0, 1.0).toFloat()
                    data[idx + 1] = out[1].coerceIn(0.0, 1.0).toFloat()
                    data[idx + 2] = out[2].coerceIn(0.0, 1.0).toFloat()
                    data[idx + 3] = 1.0f
                    idx += 4
                }
            }
        }
        return Lut(size, data)
    }
}
