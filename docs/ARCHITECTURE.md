# Animal Vision — Architecture

> Companion document: [`SCIENCE.md`](./SCIENCE.md) — the fully sourced scientific profiles.
>
> The Phase 2 version of this document proposed a TypeScript + WebGL2 PWA. Once an Android
> toolchain and a test handset were available, the project was built as a **native Android app**
> instead (better camera control, MediaCodec recording of the processed feed, direct GLES). This
> document describes the code as it exists.

---

## 1. Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0, JVM 17 |
| UI | Jetpack Compose (Material 3) |
| Camera | CameraX 1.4 (`Preview` → `SurfaceTexture`) |
| GPU | OpenGL ES 3.0 via `GLSurfaceView`, RGBA16F render targets, 3D LUT textures |
| Capture | `glReadPixels` → JPEG (MediaStore); MediaCodec input surface + MediaMuxer → MP4 |
| Tests | JUnit 4 + Truth on the pure-JVM `:core` module |
| Min / target SDK | 26 / 35 |

## 2. Modules

```
:core   pure Kotlin/JVM — no Android dependencies, fully unit-tested
  model/       AnimalVisionProfile schema + ProfileValidator
  profiles/    one data file per species + registry (ordered catalog)
  color/       spectral engine: opsin template, CIE/D65 data, receptor catches, LUT generator
  stage/       Acuity, Fov, LowLight, SpecialVision, ToneMap
  pipeline/    stage activation per profile and quality tier
  reference/   CpuRenderer — CPU implementation of the whole pipeline (test oracle)

:app    Android application
  MainActivity, AppState      wiring, permission flow, capture plumbing, single UI state
  CameraController            CameraX binding (always ROTATION_0; renderer re-frames)
  CaptureSaver                MediaStore photo/video publishing
  gl/   AnimalGlRenderer      per-frame GPU pipeline
        FrameTargets          all per-resolution GPU resources for one frame size
        BlurChain             pyramid Gaussian blur of arbitrary sigma
        Shaders, Shaders2     GLSL ES 3.0 passes
        VideoRecorder         encoder surface + muxer
  ui/   CameraScreen, AnimalSelector (+ CartoonAnimals), AnimalInfoScreen,
        SettingsScreen, Onboarding, EducationalOverlay, ConfidenceDisplay, Theme
```

The scientific layer never depends on the renderer: everything that decides *what* an animal
sees (LUTs, blur sigmas, masks, warp coefficients) is computed in `:core` and tested on the JVM.
The GPU only executes it.

## 3. Data flow

```
Compose UI ──events──▶ AppState ◀──providers── AnimalGlRenderer ──▶ GLSurfaceView
                          │                          ▲
                          │ profile id               │ SurfaceTexture (OES)
                          ▼                          │
            setProfile → LUT worker (serial)    CameraX Preview
                          │
                          ▼ newest LUT only
                    3D LUT texture
```

`AppState` is the single source of truth; the renderer reads it through lambdas each frame.
Profile switches request a LUT from a single-thread executor; a generation counter guarantees
that only the newest request is uploaded, however fast the user switches.

## 4. GPU pipeline (per frame)

All passes run on linear RGB in RGBA16F targets at the tier's internal size; sRGB encoding
happens only in the final pass.

| # | Pass | Notes |
|---|---|---|
| 1 | Camera → linear (raw) | reference for Normal / Split modes; cover-crop measured from the SurfaceTexture matrix |
| 2 | Camera → species LUT | 33³ LUT from `LutGenerator`; plain feed until the first LUT lands |
| 3 | Low light | shadow lift + rod desaturation + bounded noise (`BRIGHTNESS` stage) |
| 4 | Acuity | `Acuity.plan` → pyramid blurs (central, peripheral, foveal unsharp) → FOV combine |
| 5 | Final | FOV warp, thermal simulation, filmic tone map, sRGB, before/after mode |
| 6 | Screen | final target → window; also copied to the encoder surface while recording |

Only stages listed in the profile for the current tier run (`Pipeline.activeStages`).

### Acuity model

`Acuity.plan(model, w, h)` is shared by GPU and CPU so both render the same thing:

- **Pixels per degree** come from the resolution actually processed (long side spans the nominal
  65° phone-lens field), so apparent acuity is the same at every tier.
- **Central sigma** = `ppd / (2·cutoffCpd)` blurs the *whole* frame — a 0.5 c/deg bee does not
  see a sharp centre.
- **Peripheral sigma** uses `cutoffCpd · peripheralScalar`; the FOV mask blends it in outside the
  binocular zone (`Fov.centralWeight`).
- **Blind spots** (mask below the peripheral level, e.g. the horse's muzzle wedge) fade toward
  black (`Fov.visibility`); ordinary periphery is blurred, not darkened.
- **Foveal species** finer than the display (eagle) get only a bounded, halo-free unsharp mask.

Large sigmas (bee ≈ 15–30 px) are realized by `BlurChain`: the frame is halved with 2×2 box
filters until the remaining sigma fits a 17-tap kernel (`Acuity.pyramidLevel`, which subtracts
the box prefilters' variance), blurred there, and upsampled by bilinear sampling in the combine
pass. The Performance tier blurs one level coarser for fewer taps.

## 5. Quality tiers and resources

| Tier | Internal long side | Typical stages |
|---|---|---|
| High | 1280 | all profile stages |
| Balanced (default) | 960 | profile-defined subset |
| Performance | 640 | color + acuity + tone (no FOV warp) |

- **Auto-degradation:** a 2 s FPS window; two consecutive windows under 20 fps request one
  tier step down (repeatable until Performance). The user can raise it again in Settings.
- **Resources:** `FrameTargets` owns every per-size texture/FBO/mask and is released as a
  unit on resize, so tier changes and rotation never leak GPU memory. On EGL context loss
  all ids are dropped, programs recompiled, the last LUT re-uploaded, and the camera re-bound
  to the new `SurfaceTexture`.
- **Uniform locations** are cached per program (`GlPipeline.uniform`).

## 6. Capture

- **Photo:** always produced at the High-tier size. If the preview is running lower, the
  pipeline is re-run once into a temporary `FrameTargets` at full size, read back, and released.
  The ceiling is the camera stream itself (requested at 1280×720).
- **Video:** the final target is drawn into a MediaCodec input surface at a fixed size. While
  recording, tier changes are deferred; a rotation stops and saves the recording (the encoder
  size cannot change mid-stream). Unpublished temp files are deleted, including leftovers from
  a killed process on next launch.

## 7. Profiles

Each profile (`core/profiles/*.kt`) is a data object: identity, pipeline + tier variants, color
vision (receptor peaks, discrimination axes), acuity, field of view, low light, motion, special
vision, and scientific metadata (sources with DOI and type, per-axis confidence, limitations).
`ProfileValidator` enforces ranges, a known stage set, sources and limitations; `ProfilesTest`
pins key measured values. Adding an animal = one data file + a registry entry (+ a cartoon in
`CartoonAnimals.kt`).

Color display mapping is chosen structurally from receptor count (`ColorTransform.Kind`):
monochromat, mammal dichromat, UV trichromat (UV proxy from the blue channel, labeled),
generic trichromat, bird tetrachromat. See SCIENCE.md for per-species values.

## 8. What a phone can and cannot reproduce

| Reproducible | Approximation (labeled in-app) | Not possible (never faked) |
|---|---|---|
| Receptor-based color mapping (LUTs) | UV channel (bee, fly, birds) — no UV sensor | True UV detection |
| Tone / contrast / low-light transforms | Thermal palette (snake) — no IR sensor | True thermal detection |
| Acuity reduction by spatial filtering | Polarization (bee, mantis shrimp) | Polarization measurement |
| FOV *geometry* (masks, warp) | Eagle foveal detail boost | A field wider than the lens |
| | | Neural processing / inner experience |

## 9. Privacy

All frames are processed on-device; nothing is uploaded and there is no analytics. The camera
permission is requested at onboarding; recordings are video-only, so no microphone permission
exists. Photos and videos go to `Pictures/AnimalVision` and `Movies/AnimalVision` via MediaStore;
only on Android 8–9 (API 26–28) is storage write access requested, at the first save.

## 10. Testing

`./gradlew :core:test` runs the JVM suite:

- **ProfilesTest** — validation, pinned measured values, registry behavior.
- **ColorTransformTest** — receptor math, LUT generation, dichromat convergence.
- **StageTest** — kernels, acuity plan, pyramid math, FOV masks/weights, low light, pipeline gating.
- **CpuRendererTest** — scene-level checks on a synthetic chart (dichromat red/green collapse,
  bee red chip darkest, horse blind spot, low-acuity centre blur, thermal output, determinism).

The GPU path mirrors `CpuRenderer`; GPU behavior is verified on device.
