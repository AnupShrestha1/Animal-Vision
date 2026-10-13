package com.animalvision.core.color

/**
 * Display mappings: how each species' receptor-catch space is projected onto the RGB display.
 * These are documented visualizations (see docs/SCIENCE.md), not claims about subjective perception.
 *
 * Input is von-Kries normalized catches; output is LINEAR display RGB.
 */
object DisplayMappings {

    private const val EPS = 1e-6

    /**
     * Mammal dichromats (dog, cat, horse): single chromatic axis S-vs-L plus luminance.
     * Positive opponent -> blue hue, negative -> yellow hue; white at the neutral point.
     * Red/green inputs share a similar S/L ratio and therefore converge (the defining
     * dichromat behavior).
     */
    fun dichromat(c: DoubleArray): DoubleArray {
        val s = c[0]
        val l = if (c.size > 1) c[1] else c[0]
        val opp = (s - l) / (s + l + EPS)
        val sat = Math.min(1.0, Math.abs(opp) * 2.2)
        val hueR = if (opp >= 0) 0.30 else 1.00
        val hueG = if (opp >= 0) 0.50 else 0.85
        val hueB = if (opp >= 0) 1.00 else 0.35
        val lum = l.coerceIn(0.0, 1.2)
        fun mix(hue: Double) = (1.0 - sat) + hue * sat
        return doubleArrayOf(
            (lum * mix(hueR)).coerceIn(0.0, 1.0),
            (lum * mix(hueG)).coerceIn(0.0, 1.0),
            (lum * mix(hueB)).coerceIn(0.0, 1.0),
        )
    }

    /**
     * Honeybee trichromat: UV / blue / green. UV is rendered as false color (standard UV-photography
     * convention). Because phones cannot measure UV, the UV position is a documented proxy derived
     * from the camera's blue channel; the bee profile carries the required disclosure label.
     */
    fun bee(c: DoubleArray, linearBlueChannel: Double): DoubleArray {
        val uvProxy = Math.pow(linearBlueChannel.coerceIn(0.0, 1.0), 1.4)
        val g = if (c.size > 2) c[2] else c[0]
        val b = if (c.size > 1) c[1] else c[0]
        return doubleArrayOf(
            uvProxy.coerceIn(0.0, 1.0),
            g.coerceIn(0.0, 1.0),
            b.coerceIn(0.0, 1.0),
        )
    }

    /**
     * Generic trichromat projection (no UV false-color channel): receptors ordered
     * short→long wavelength map onto the blue/green/red display channels directly.
     * Not used by any current profile (bee/fly use the UV-proxy mapping; birds and
     * butterflies use [bird]), but kept so a future3-class non-UV profile has an
     * honest default instead of falling into the dichromat path.
     */
    fun trichromat(c: DoubleArray): DoubleArray {
        val b = c[0]
        val g = if (c.size > 1) c[1] else c[0]
        val r = if (c.size > 2) c[2] else c[0]
        return doubleArrayOf(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), b.coerceIn(0.0, 1.0))
    }

    /**
     * True monochromats (octopus: one ~490 nm pigment class; dolphin: single LWS cone
     * class after SWS1 pseudogenization). A single pigment carries no hue dimension —
     * the normalized catch maps straight to luminance as achromatic grayscale. This is
     * the honest projection: wavelength ratios are literally unavailable to the eye,
     * so any color left on screen would be a false claim.
     */
    fun monochromat(c: DoubleArray): DoubleArray {
        val v = c[0].coerceIn(0.0, 1.0)
        return doubleArrayOf(v, v, v)
    }

    /**
     * Bird tetrachromat (eagle, pigeon, chicken): VS / S / M / L projection with the deep-UV channel suppressed,
     * consistent with raptor ocular-media transmission measurements (Lind et al. 2013).
     * The short-wave planes contribute a violet bias; M->green, L->red.
     */
    fun bird(c: DoubleArray): DoubleArray {
        val vs = c[0]
        val s = if (c.size > 1) c[1] else vs
        val m = if (c.size > 2) c[2] else c[0]
        val lg = if (c.size > 3) c[3] else c[0]
        return doubleArrayOf(
            lg.coerceIn(0.0, 1.0),
            m.coerceIn(0.0, 1.0),
            Math.max(s, 0.7 * vs).coerceIn(0.0, 1.0),
        )
    }
}
