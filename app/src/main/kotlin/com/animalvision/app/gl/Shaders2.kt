package com.animalvision.app.gl

/** Remaining pipeline shaders (blur passes + final composite) — separated for readability. */
object Shaders2 {

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

    /**
     * Vertical blur + acuity/FOV combination:
     *  - vertical blur of the horizontal-pass texture;
     *  - blend sharp vs blurred by the binocular/peripheral mask;
     *  - bounded halo-free foveal sharpening (eagle) via the boost mask;
     *  - blind-spot visibility attenuation.
     */
    val FRAG_BLUR_V = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uBlur;
        uniform sampler2D uSharp;
        uniform sampler2D uFovMask;
        uniform sampler2D uBoost;
        uniform sampler2D uVis;
        uniform int uRadius;
        uniform float uKernel[17];
        uniform vec2 uTexel;
        uniform float uSharpenAmount;
        out vec4 outColor;
        void main() {
            vec4 acc = vec4(0.0);
            for (int k = -8; k <= 8; k++) {
                if (abs(k) > uRadius) continue;
                vec2 off = vec2(0.0, float(k) * uTexel.y);
                acc += texture(uBlur, vUv + off) * uKernel[8 + k];
            }
            vec3 sharp = texture(uSharp, vUv).rgb;
            float m = texture(uFovMask, vUv).r;
            float boost = texture(uBoost, vUv).r;
            float vis = texture(uVis, vUv).r;
            vec3 combined = mix(acc.rgb, sharp, m);
            vec3 detail = clamp(sharp - acc.rgb, vec3(-0.05), vec3(0.05));
            combined += detail * uSharpenAmount * boost;
            outColor = vec4(clamp(combined * vis, 0.0, 1.0), 1.0);
        }
    """.trimIndent()

    /**
     * Final pass: optional thermal simulation, filmic tone map, sRGB encoding, and the
     * before/after display mode (normal / animal / split).
     */
    val FRAG_FINAL = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uProc;
        uniform sampler2D uRaw;
        uniform int uMode;      // 0 normal, 1 animal, 2 split
        uniform int uThermal;   // 1 = apply thermal simulation
        out vec4 outColor;
        vec3 thermalPalette(float x) {
            if (x < 0.25) { float s = x / 0.25; return vec3(0.0, 0.0, 0.5 * s); }
            else if (x < 0.5) { float s = (x - 0.25) / 0.25; return vec3(0.8 * s, 0.0, 0.5 + 0.3 * s); }
            else if (x < 0.75) { float s = (x - 0.5) / 0.25; return vec3(0.8 + 0.2 * s, 0.9 * s, 0.8 - 0.5 * s); }
            else { float s = (x - 0.75) / 0.25; return vec3(1.0, 0.9 + 0.1 * s, 0.3 + 0.7 * s); }
        }
        vec3 filmic(vec3 x) { return x / (1.0 + x); }
        vec3 linearToSrgb(vec3 c) {
            return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055,
                       step(vec3(0.0031308), c));
        }
        void main() {
            vec3 proc = texture(uProc, vUv).rgb;
            vec3 raw = texture(uRaw, vUv).rgb;
            if (uThermal == 1) {
                float luma = dot(proc, vec3(0.2126, 0.7152, 0.0722));
                float t = round(luma * 6.0) / 6.0;
                proc = thermalPalette(t) * (0.35 + 0.65 * luma);
            }
            vec3 animal = linearToSrgb(filmic(proc));
            vec3 normal = linearToSrgb(filmic(raw));
            vec3 outRgb = (uMode == 1) ? animal : normal;
            if (uMode == 2) {
                outRgb = (vUv.x < 0.5) ? animal : normal;
            }
            outColor = vec4(outRgb, 1.0);
        }
    """.trimIndent()
}