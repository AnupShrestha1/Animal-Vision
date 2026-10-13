package com.animalvision.core.stage

/** Final tone mapping + sRGB encoding (the last pipeline stage). */
object ToneMap {
    /** Start of the highlight shoulder (linear). Everything below passes through unchanged. */
    const val KNEE = 0.9

    /**
     * Highlight shoulder for display-referred (SDR) input: identity below [KNEE], then an
     * exponential roll-off that approaches 1 with matching slope, so the rare value above 1
     * never hard-clips. The camera feed is already a 0..1 display image — a Reinhard curve
     * (x/(1+x)) here mapped white to 0.5 and left every frame grey and flat.
     */
    fun filmic(x: Double): Double {
        val v = x.coerceAtLeast(0.0)
        if (v <= KNEE) return v
        val range = 1.0 - KNEE
        return KNEE + range * (1.0 - Math.exp(-(v - KNEE) / range))
    }

    /** Applies [filmic] per channel, then encodes to sRGB. Input is LINEAR. */
    fun toDisplay(linearRgb: DoubleArray): DoubleArray = doubleArrayOf(
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[0])),
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[1])),
        com.animalvision.core.color.Spectrum.encodeSrgb(filmic(linearRgb[2])),
    )
}
