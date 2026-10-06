package com.animalvision.core.stage

/** Final tone mapping + sRGB encoding (the last pipeline stage). */
object ToneMap {
    /** Reinhard-style exposure/contrast curve: x/(1+x), compressing highlights. */
    fun filmic(x: Double): Double {
        val v = x.coerceAtLeast(0.0)
        return v / (1.0 + v * 1.0)
    }

    /** Applies [filmic] per channel, then encodes to sRGB. Input is LINEAR. */
    fun toDisplay(linearRgb: DoubleArray): DoubleArray = doubleArrayOf(
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[0])),
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[1])),
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[2])),
    )
}
