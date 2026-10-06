package com.animalvision.app.gl

/**
 * GLSL ES 3.0 shaders implementing the vision pipeline (mirrors the CPU reference in :core,
 * which is the tested oracle). All passes operate on LINEAR RGB; encoding happens in the final pass.
 */
object Shaders {

    val VERT = """
        #version 300 es
        layout(location = 0) in vec2 aPos;
        out vec2 vUv;
        void main() {
            vUv = aPos * 0.5 + 0.5;
            gl_Position = vec4(aPos, 0.0, 1.0);
        }
    """.trimIndent()

    /** OES camera -> linear RGB, optionally through the species 3D LUT. */
    val FRAG_OES = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision highp float;
        in vec2 vUv;
        uniform samplerExternalOES uTex;
        uniform sampler3D uLut;
        uniform int uUseLut;
        uniform mat4 uTexMat;
        out vec4 outColor;
        vec3 srgbToLinear(vec3 c) {
            return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)),
                       step(vec3(0.04045), c));
        }
        void main() {
            vec2 uv = (uTexMat * vec4(vUv, 0.0, 1.0)).xy;
            vec3 rgb = texture(uTex, uv).rgb;
            vec3 lin = srgbToLinear(rgb);
            vec3 outRgb = (uUseLut == 1) ? texture(uLut, lin).rgb : lin;
            outColor = vec4(outRgb, 1.0);
        }
    """.trimIndent()

    /** Low-light: contrast-preserving shadow lift + rod desaturation + bounded noise. */
    val FRAG_LOWLIGHT = """
        #version 300 es
        precision highp float;
        in vec2 vUv;
        uniform sampler2D uTex;
        uniform float uLift;
        uniform float uDesat;
        uniform float uNoiseSigma;
        uniform float uSeed;
        out vec4 outColor;
        float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
        void main() {
            vec3 c = texture(uTex, vUv).rgb;
            float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
            float liftAmount = clamp(uLift, 0.0, 0.66) * 2.0 * (1.0 - luma);
            vec3 t = c * (1.0 + liftAmount);
            t = t / (1.0 + t * 0.35);
            float desat = clamp(uDesat, 0.0, 1.0) * (1.0 - luma) * (1.0 - luma);
            vec3 grey = vec3(dot(t, vec3(0.2126, 0.7152, 0.0722)));
            vec3 outRgb = mix(t, grey, desat);
            if (uNoiseSigma > 0.0) {
                float n = (hash(vUv + uSeed) + hash(vUv * 1.7 + uSeed) + hash(vUv * 2.3 + uSeed) - 1.5) * uNoiseSigma;
                outRgb += vec3(n);
            }
            outColor = vec4(clamp(outRgb, 0.0, 1.0), 1.0);
        }
    """.trimIndent()
}