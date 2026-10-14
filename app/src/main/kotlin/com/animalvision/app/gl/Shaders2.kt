package com.animalvision.app.gl

import com.animalvision.core.stage.CompoundEye
import com.animalvision.core.stage.Fov
import com.animalvision.core.stage.SpecialVision

/** Remaining pipeline shaders (blur passes + final composite) — separated for readability. */
object Shaders2 {
    private const val MUZZLE_NY = Fov.MUZZLE_NY
    private const val MUZZLE_NX = Fov.MUZZLE_NX
    private const val MUZZLE_FACTOR = Fov.MUZZLE_FACTOR
    private const val POLARIZATION_STRIPE_PX = SpecialVision.POLARIZATION_STRIPE_PX
    private const val BORDER_FRACTION = CompoundEye.BORDER_FRACTION
    private const val BORDER_DARKEN = CompoundEye.BORDER_DARKEN


    /** Separable Gaussian blur, horizontal pass. */
    val FRAG_BLUR_H = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uTex;
        uniform int uRadius;
        uniform float uKernel[17];
        uniform vec2 uTexel;
        out vec4 outColor;
        void main() {
            vec4 acc = vec4(0.0);
            // Symmetric window: slots [8 - radius, 8 + radius] hold the (renormalized)
            // truncated Gaussian, centered on slot 8.
            for (int k = -8; k <= 8; k++) {
                if (abs(k) > uRadius) continue;
                vec2 off = vec2(float(k) * uTexel.x, 0.0);
                acc += texture(uTex, vUv + off) * uKernel[8 + k];
            }
            outColor = acc;
        }
    """.trimIndent()

    /** Separable Gaussian blur, vertical pass (mirror of [FRAG_BLUR_H]). */
    val FRAG_BLUR_V = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uTex;
        uniform int uRadius;
        uniform float uKernel[17];
        uniform vec2 uTexel;
        out vec4 outColor;
        void main() {
            vec4 acc = vec4(0.0);
            for (int k = -8; k <= 8; k++) {
                if (abs(k) > uRadius) continue;
                vec2 off = vec2(0.0, float(k) * uTexel.y);
                acc += texture(uTex, vUv + off) * uKernel[8 + k];
            }
            outColor = acc;
        }
    """.trimIndent()

    /**
     * Pyramid downsample: drawn into a target half the source size, each fragment samples
     * the source midway between four texels, so bilinear filtering yields a 2×2 box average.
     */
    val FRAG_DOWNSAMPLE = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uTex;
        out vec4 outColor;
        void main() { outColor = texture(uTex, vUv); }
    """.trimIndent()

    /**
     * Acuity/FOV combination (mirrors CpuRenderer's acuity stage — see Acuity.plan):
     *  - uCentral: frame blurred to the species' central acuity (or the sharp frame);
     *  - uPeriph: frame blurred to the peripheral acuity, blended in outside the
     *    binocular zone by the FOV mask;
     *  - bounded halo-free foveal sharpening (eagle) against uUnsharp, weighted by the
     *    analytic foveal boost;
     *  - blind-spot attenuation for mask values below the peripheral level.
     */
    val FRAG_ACUITY_COMBINE = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uSharp;
        uniform sampler2D uCentral;
        uniform sampler2D uPeriph;
        uniform sampler2D uUnsharp;
        uniform int uFovOn;
        // FOV mask parameters (Fov.MaskParams) — evaluated per fragment, mirroring Fov.maskAt.
        uniform float uHalfWidth;
        uniform float uYAspect;
        uniform float uMaskPeripheral;
        uniform int uMuzzle;
        // Foveal boost (Fov.sharpBoostMask): amplitude and Gaussian sigma in pixels.
        uniform float uBoostAmp;
        uniform float uBoostSigmaPx;
        uniform vec2 uResolution;
        uniform int uUseUnsharp;
        uniform float uPeripheral;   // plan.peripheralScalar
        uniform float uSharpenAmount;
        out vec4 outColor;
        float ss(float t) { t = clamp(t, 0.0, 1.0); return t * t * (3.0 - 2.0 * t); }
        float fovMask(vec2 uv) {
            float nx = uv.x * 2.0 - 1.0;
            float ny = (1.0 - uv.y) * 2.0 - 1.0; // frame y points down, as in Fov.maskAt
            float hw = uHalfWidth;
            float xEdge = (hw - abs(nx)) / (hw * 0.5 + 1e-6) + 0.5;
            float yEdge = (uYAspect * hw - abs(ny)) / (uYAspect * hw * 0.5 + 1e-6) + 0.5;
            float m = uMaskPeripheral + (1.0 - uMaskPeripheral) * ss(xEdge) * ss(yEdge);
            if (uMuzzle == 1 && ny > ${MUZZLE_NY} && abs(nx) < ${MUZZLE_NX}) m *= ${MUZZLE_FACTOR};
            return m;
        }
        void main() {
            float m = (uFovOn == 1) ? fovMask(vUv) : 1.0;
            float t = (uPeripheral >= 1.0) ? 1.0 : clamp((m - uPeripheral) / (1.0 - uPeripheral), 0.0, 1.0);
            float vis = 0.05 + 0.95 * clamp(m / uPeripheral, 0.0, 1.0);
            vec3 central = texture(uCentral, vUv).rgb;
            vec3 periph = texture(uPeriph, vUv).rgb;
            vec3 combined = mix(periph, central, t);
            if (uUseUnsharp == 1) {
                vec3 sharp = texture(uSharp, vUv).rgb;
                vec3 detail = clamp(sharp - texture(uUnsharp, vUv).rgb, vec3(-0.05), vec3(0.05));
                vec2 d = (vUv - 0.5) * uResolution / uBoostSigmaPx;
                float boost = min(uBoostAmp * exp(-0.5 * dot(d, d)), 1.0);
                combined += detail * uSharpenAmount * boost;
            }
            outColor = vec4(clamp(combined * vis, 0.0, 1.0), 1.0);
        }
    """.trimIndent()

    /**
     * Final pass: optional thermal simulation, filmic tone map, sRGB encoding, and the
     * before/after display mode (normal / animal / split).
     *
     * `uWarp` is the FOV-geometry radial warp (Fov.warpCoefficients): it deforms ONLY the
     * processed/animal image, and only in full Animal mode (the renderer passes 0 in Split
     * mode so both halves meet on the same scene point at the divider).
     */
    val FRAG_FINAL = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uProc;
        uniform sampler2D uRaw;
        uniform int uMode;      // 0 normal, 1 animal, 2 split
        uniform float uSplit;   // split divider position (0..1 from the left)
        uniform int uThermal;   // 1 = apply thermal simulation
        uniform vec2 uWarp;     // (kx, ky): >0 panoramic prey wrap, <0 foveal tunnel, 0 = none
        uniform int uPolar;     // 1 = simulated polarization cue (SpecialVision.applyPolarizationOverlay)
        uniform vec2 uResolution;
        uniform int uMosaic;    // 1 = ommatidial mosaic (CompoundEye), animal side only
        uniform float uFacetPx; // facet pitch in pixels
        out vec4 outColor;
        // Mirrors CompoundEye.facetCenter: two offset rectangular lattices form the hex grid.
        vec2 facetCenter(vec2 p, float pitch) {
            vec2 r = vec2(pitch, pitch * 1.7320508);
            vec2 a = floor(p / r) * r + r * 0.5;
            vec2 b = floor((p - r * 0.5) / r) * r + r;
            vec2 da = p - a;
            vec2 db = p - b;
            return dot(da, da) <= dot(db, db) ? a : b;
        }
        // Mirrors CompoundEye.borderFactor.
        float facetBorder(vec2 p, vec2 c, float pitch) {
            vec2 d = abs(p - c);
            float hex = max(d.x, d.x * 0.5 + d.y * 0.8660254) / (pitch * 0.5);
            float t = clamp((hex - (1.0 - 2.0 * ${BORDER_FRACTION})) / (2.0 * ${BORDER_FRACTION}), 0.0, 1.0);
            return 1.0 - ${BORDER_DARKEN} * t * t * (3.0 - 2.0 * t);
        }
        vec3 thermalPalette(float x) {
            if (x < 0.25) { float s = x / 0.25; return vec3(0.0, 0.0, 0.5 * s); }
            else if (x < 0.5) { float s = (x - 0.25) / 0.25; return vec3(0.8 * s, 0.0, 0.5 + 0.3 * s); }
            else if (x < 0.75) { float s = (x - 0.5) / 0.25; return vec3(0.8 + 0.2 * s, 0.9 * s, 0.8 - 0.5 * s); }
            else { float s = (x - 0.75) / 0.25; return vec3(1.0, 0.9 + 0.1 * s, 0.3 + 0.7 * s); }
        }
        // Mirrors ToneMap.filmic: identity below the 0.9 knee, smooth shoulder above.
        vec3 filmic(vec3 x) {
            x = max(x, vec3(0.0));
            vec3 shoulder = 0.9 + 0.1 * (1.0 - exp(-(x - 0.9) / 0.1));
            return mix(x, shoulder, step(vec3(0.9), x));
        }
        vec3 linearToSrgb(vec3 c) {
            return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055,
                       step(vec3(0.0031308), c));
        }
        // Radial remap: src = 0.5 + c * (1 + k * (0.25 - c*c)). Monotonic and in-bounds
        // for k in (-4, 2) — Fov.warpCoefficients stays inside that range.
        vec2 warpedUv(vec2 uv) {
            vec2 c = uv - 0.5;
            vec2 w = 0.5 + c * (1.0 + uWarp * (0.25 - c * c));
            return clamp(w, vec2(0.001), vec2(0.999));
        }
        void main() {
            vec2 sampleUv = vUv;
            float facet = 1.0;
            bool animalSide = uMode == 1 || (uMode == 2 && vUv.x < uSplit);
            if (uMosaic == 1 && animalSide) {
                // Pixel centre with a top-left origin, like the CPU reference.
                vec2 p = vec2(gl_FragCoord.x, uResolution.y - gl_FragCoord.y);
                vec2 c = facetCenter(p, uFacetPx);
                facet = facetBorder(p, c, uFacetPx);
                sampleUv = vec2(c.x / uResolution.x, 1.0 - c.y / uResolution.y);
            }
            vec3 proc = texture(uProc, warpedUv(sampleUv)).rgb * facet;
            vec3 raw = texture(uRaw, vUv).rgb;
            if (uThermal == 1) {
                float luma = dot(proc, vec3(0.2126, 0.7152, 0.0722));
                float t = round(luma * 6.0) / 6.0;
                proc = thermalPalette(t) * (0.35 + 0.65 * luma);
            }
            if (uPolar == 1) {
                // Mirrors SpecialVision.polarizationStrength / applyPolarizationOverlay.
                float luma = dot(proc, vec3(0.2126, 0.7152, 0.0722));
                float mx = max(proc.r, max(proc.g, proc.b));
                float mn = min(proc.r, min(proc.g, proc.b));
                float sat = mx > 1e-6 ? (mx - mn) / mx : 0.0;
                float k = smoothstep(0.55, 0.85, luma) * (1.0 - smoothstep(0.15, 0.4, sat));
                // Integer pixel coordinates with a top-left origin, like the CPU reference.
                vec2 px = vec2(floor(gl_FragCoord.x), uResolution.y - 1.0 - floor(gl_FragCoord.y));
                float stripe = fract((px.x + px.y) / ${POLARIZATION_STRIPE_PX}) < 0.5 ? 1.0 : 0.0;
                proc = mix(proc, vec3(0.15, 0.85, 1.0), 0.55 * k * stripe);
            }
            vec3 animal = linearToSrgb(filmic(proc));
            vec3 normal = linearToSrgb(filmic(raw));
            vec3 outRgb = (uMode == 1) ? animal : normal;
            if (uMode == 2) {
                outRgb = (vUv.x < uSplit) ? animal : normal;
            }
            outColor = vec4(outRgb, 1.0);
        }
    """.trimIndent()
}