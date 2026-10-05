# Animal Vision — Architecture & Technology Decision

> **Phase 2 deliverable.** Covers the fourteen pre-coding items required by the project specification.
> Companion document: [`SCIENCE.md`](./SCIENCE.md) — the fully-sourced scientific profiles.

---

## 1–2. Recommended stack and why

### Recommendation
**TypeScript + WebGL2, delivered as an installable PWA** — with the vision engine written framework-agnostic (pure TS) so it can later drive a native shell (Capacitor) or be ported to Metal/GLES directly without rewriting the science layer.

### Why (vs the alternatives)
Decision criteria are the specification's weighted list: real-time camera access, GPU image processing, performance, cross-platform support, maintainability, camera-hardware access, video processing, future extensibility.

| Criterion | **Web / PWA (WebGL2)** | Flutter | Native Android (Kotlin) | React Native + vision-camera |
|---|---|---|---|---|
| Real-time camera | `getUserMedia` — hardware-accelerated preview on Android Chrome & iOS Safari | `camera` plugin exports a texture; piping it through custom fragment shaders is awkward | CameraX — raw frames, lens selection, flash (best) | frame processors — strong, needs dev build |
| GPU processing | WebGL2 fragment shaders compile to platform GPU (Metal/Vulkan/GLES) | FragmentShader/Impeller; limited for camera pipelines | OpenGL ES / Vulkan (max control) | Skia over GPU |
| Performance | 30–60 fps at ≤720p on modern phones | good; heavier multi-pass FBO chains | best | good |
| Cross-platform | Android + iOS + desktop in one shot | Android + iOS + desktop | Android only | Android + iOS |
| Maintainability | pure-TS engine, Node-testable in this environment | Dart | high native complexity | medium |
| Hardware access | weak (no flash toggle, limited ultra-wide selection) | medium | full | medium |
| **Video of the *processed* feed** | `canvas.captureStream()` + `MediaRecorder` records exactly what the user sees | needs custom platform channels | MediaCodec input surface | poorly supported |
| Extensibility | profiles-as-data; sensor provider interfaces; Capacitor wrap later | good | good | good |

**Environment reality check:** this development machine (Windows) has Node.js but **no** Flutter SDK, JDK, Android SDK, or connected handset. Native mobile builds here would require multi-GB toolchain installs with **no on-device testing possible**. A WebGL2 PWA is buildable, testable (Node + browser), runs on any phone browser, and satisfies every vertical requirement the spec places on science and interaction (offline, local processing, capture, before/after, info panels).

**What we give up (and own honestly):** flash control, guaranteed ultra-wide lens selection, sub-30ms glass latency, direct thermal/UV hardware. These map exactly to the spec's "hardware limitations must be acknowledged" rules and are isolated behind provider interfaces for future native work.

**Upgrade path:** engine and profiles are transport-agnostic; a future `app-native` package can reuse 100% of the scientific layer.

---

## 3. Architecture overview

```
┌──────────────────────────── UI (app/ui) ────────────────────────────┐
│ CameraScreen · AnimalSelector · InfoPanels · Settings · Onboarding  │
│ EducationOverlay · ConfidenceDisplay · Before/After · Capture       │
└───────────────┬──────────────────────────────────────┬──────────────┘
                │ state/events                          │ render target
┌───────────────▼──────────────┐            ┌───────────▼───────────────┐
│  vision-engine (pure TS)     │  staged    │  app/render               │
│  Pipeline orchestrator       │─ params ──>│  WebGL2 renderer          │
│  Processors (color/acuity/   │            │  FBO chain + GLSL shaders │
│   fov/lowlight/motion/spec)  │            │  LUT textures             │
│  LUT generator (spectral)    │            │  perf monitor (fps/lat)   │
│  CPU reference (tests)       │            └───────────┬───────────────┘
└───────────────▲──────────────┘                        │ canvas stream
                │ profile                                 │
┌───────────────┴──────────────┐            ┌───────────▼───────────────┐
│  profiles (data only)        │            │  app/capture              │
│  AnimalVisionProfile JSON/TS │            │  PhotoCapture             │
│  versioned, validated        │            │  VideoRecorder (MediaRec) │
└──────────────────────────────┘            └───────────────────────────┘
```

One-way data flow: **UI → engine → renderer → display**, with capture tapping the renderer output so photos/videos contain exactly the animal-processed pixels (no UI chrome).

### Modules (spec §38, adapted to this stack)

| Spec module | Implementation |
|---|---|
| `camera/` | `app/camera`: `CameraController` (getUserMedia + capability probing), `CameraCapabilities` (lenses/resolutions/face), graceful fallback chain |
| `vision/` | `engine/src`: `VisionEngine`, `pipeline/` (Color→Acuity→FOV→LowLight→Motion→Special→ToneMap); each stage = one shader pass + a CPU reference |
| `animals/` | `profiles/src`: `AnimalVisionProfile` type + profiles (Human, Dog, Cat, Horse, Eagle, Bee, Snake) + registry + validation |
| `rendering/` | `app/render`: GLSL shaders, precomputed 3D LUTs, `GPUProcessor` (FBO chain), quality-tier uniforms |
| `capture/` | `app/capture`: `PhotoCapture`, `VideoRecorder`, orientation/aspect, metadata save |
| `education/` | `profiles/info.ts` + `app/ui/info`: scientific copy, sources, confidence |
| `ui/`, `settings/`, `onboarding` | `app/ui/*` |

---

## 4. Directory structure

```
animal-vision/
├─ docs/                     # SCIENCE.md, ARCHITECTURE.md
├─ packages/
│  ├─ profiles/              # scientific data layer (pure data)
│  │  ├─ src/types.ts        # AnimalVisionProfile schema (spec §6)
│  │  ├─ src/validate.ts     # schema + range validation
│  │  ├─ src/{human,dog,cat,horse,eagle,bee,snake}.ts
│  │  ├─ src/registry.ts     # ordered catalog
│  │  └─ test/               # schema + science-paranoia tests
│  ├─ engine/                # vision processing (pure TS, no DOM)
│  │  ├─ src/pipeline.ts     # stage orchestration, per-animal activation
│  │  ├─ src/color/          # receptor-catch math, opponent encoders, LUT gen
│  │  ├─ src/acuity|fov|brightness|lowlight|motion|special|tone/
│  │  ├─ src/perf/           # rolling FPS/latency monitor, tier policy
│  │  └─ src/reference/      # CPU reference implementation (tests)
│  │  └─ test/
│  └─ app/                   # WebGL2 PWA
│     ├─ index.html  manifest.webmanifest  sw.js
│     ├─ src/{camera,render,capture,ui,state}/  src/main.ts
│     └─ public/             # icons, LUTs
├─ package.json              # npm workspaces
└─ README.md
```

The scientific layer stays **independent of the renderer** so all science tests run under Node with zero GPU.

---

## 5. Camera pipeline design

```
Camera Sensor
   │ getUserMedia (widest available lens; 16:9; res by quality tier)
   ▼
Camera Texture ──► (resolve) ──► sRGB working space
   │
   ├─ Pass 0  Exposure/white-point normalization   [Human: none; others: fixed-pivot, GPU luminance histogram]
   ├─ Pass 1  COLOR · species 3D-LUT (receptor catch → opponent → display gamut)
   ├─ Pass 2  BRIGHTNESS/CONTRAST/LOW-LIGHT · species tone curve (+rod desat, control noise)  [Cat, Horse, Dog]
   ├─ Pass 3  ACUITY · separable Gaussian (σ from c/deg) × radial acuity mask  [all except Human]
   │          special high tier: Eagle bounded unsharp (clamped, halo-free) in foveal zone
   ├─ Pass 4  FOV/GEOMETRY · binocular/monocular masks, blind-spot overlays, mild barrel warp  [Horse, Dog, Eagle]
   ├─ Pass 5  MOTION/TEMPORAL · frame blend coefs; labeled CFF overlay only (phisically bounded by fps)
   ├─ Pass 6  SPECIAL · UV false-color (Bee) / thermal simulation (Snake) / polarization overlay (Bee)
   └─ Pass 7  TONE MAP + gamma → canvas → (a) display  (b) captureStream → photo/video
```

- Only stages listed in the animal's profile are activated — the pipeline short-circuits unused passes (spec §7).
- Each pass is a single WebGL2 FBO with a fragment shader; textures are reused (double-buffered) to bound memory.
- One CPU reference implementation exists per pass for unit tests and visual validation.

---

## 6. Vision-processing strategy

**Color (the scientific core).** For each species we build a **precomputed 3D LUT (33³)** offline from spectral modeling:
1. Receptor templates via Govardovskii (2000) with species λmax from SCIENCE.md;
2. Camera RGB → receptor catches via a linear mapping fitted on a spectral surface basis (Troscianko & Stevens / micaToolbox lineage), D65 illuminant;
3. Receptor-noise-limited opponent encoding (Vorobyev & Osorio) — the *available* discrimination axes (e.g. the single S-vs-L axis for dogs/horses/cats; UV/B/G for bees; VS/S/M/L for eagles) then project each species' receptor space into a **display gamut with documented axes**:
   - dichromats → `x = Lc − Sc` (opponency), `y = lightness`: colors confusable to the animal converge because their catch vectors genuinely coincide;
   - bee → UV false-color (standard magenta convention), labeled "UV-inspired, not measured";
   - eagle → 4-channel catch space projected to a warm, violet-shifted tristimulus with the deep-UV suppressed (ocular-media evidence per Lind et al. 2013).
4. Stand-alone generator script emits `public/luts/*.png` (RGB cube) that the renderer uploads as RGBA8/16 texture.

**Acuity.** σ (Gaussian, in texels) derived from the species c/deg ÷ human reference and current display/zoom assumptions; applied decreasingly toward peripheral zones via a radial mask whose shape is defined by each animal's FOV model (central field = peak).
**FOV geometry.** Baked as UV-mask textures (binocular ROI, monocular peripheral falloff, horse blind-spot wedge). Never fabricates content beyond the sensor.
**Low light.** Contrast-preserving shadow-lift tone curve + luminance-dependent desaturation + bounded noise; a "Low-light sim" toggle (default on for Cat/Horse).
**Motion.** Stage computes 2-frame blend + optional temporal sharpening; completely disabled in Performance tier; a labeled overlay communicates the animal's CFF vs the actual device fps rather than claiming fake "fast vision."
**Special.** `UVSimulation`, `ThermalSimulation`, `PolarizationSimulation` modules behind a `SensorProvider` interface — a simulation is always labeled; real sensor data (future) is only shown when hardware is present.
---

## 7. Scientific data model (spec §6, §42)

Every profile is a versioned, validated data object:

```ts
interface AnimalVisionProfile {
  id: string;                  // "dog"
  name: string;                // "Dog"
  scientificName: string;      // "Canis lupus familiaris"
  description: string;
  profileVersion: string;      // semver — profiles may be revised independently of engine
  researchUpdated: string;     // ISO date of last literature review
  pipeline: PipelineSpec;      // ordered stage list; engine activates only these
  colorVision: {
    photoreceptorCount: 2;     // 1..4
    photoreceptorTypes: string[];           // ["SWS1","LWS"] etc.
    spectralSensitivity: { peakNm: number; template: "A1" }[];
    colorDiscrimination: { model: "RNL" | "opponent"; axes: string[] };
    colorTransformation: { lut: string; lutSize: 33; notes: string };
  };
  brightness: { sensitivity: number; dynamicRange: number; lowLight: TierProps };
  acuity: { relativeAcuity: number; centralAcuity: number; peripheralAcuity: number; cutoffCPD: number };
  fieldOfView: {
    totalHorizontalDeg: number; horizontalDeg: number; verticalDeg: number;
    binocularRegion: { yawDeg: number; widthDeg: number };
    blindSpots: { label: string; approx: string }[];
  };
  motion: { temporalResolutionHz: number | null; motionSensitivity: number; sourceConfidence: number };
  depth: { binocularDepth: number; monocularDepth: number };
  specialVision: { ultraviolet: boolean; infrared: boolean; polarization: boolean; other: string[] };
  processing: { stages: StageSpec[] };   // shader names, params, quality-tier variants
  scientificData: {
    sources: { cite: string; doi?: string; type: "measured" | "inferred" | "modeled" }[];
    confidence: ConfidenceLevels;       // per-axis + overall
    limitations: string[];
  };
}
```

Validation enforces: known set of stages (no typos can inject arbitrary processing), numeric ranges (e.g. acuity c/deg must be positive and < 200), a source exists for every measured parameter, and `profileVersion` bumps whenever scientific values change. Adding an animal = adding one data file + info copy (spec §4/§42 satisfied by construction).

---

## 8. Animal profile table (summary of SCIENCE.md)

| Animal | Color model | Acuity | FOV / geometry | Low light | Motion | Special | Overall conf. |
|---|---|---|---|---|---|---|---|
| Human | Trichromat 419/530/559 | ~30–72 c/deg | 180°/120° bin | standard | 50–60 Hz | — | █████ |
| Dog | Dichromat 429/555 | ~1.9–11.5 c/deg (breed) | ~240°; bin 30–60° | tapetum | ~70–80 Hz | — | ████░ |
| Cat | Dichromat ~452/556* | ~4–10 c/deg | ~200°; bin ~90–130° | tapetum, ~1/6 light | high | — | ████░ |
| Horse | Dichromat 428/539 | ~20/30–20/60 | ~350°; bin 55–65°; blind spots | tapetum | high | — | ████░ |
| Eagle | Tetrachromat (violet-filtered) | ~140 c/deg (2 foveae) | wide; bin ~30–50° est. | — | high | violet shift (no UV claim) | ████░ |
| Bee | Trichromat UV 344/B 436/G 556 | ~0.5 c/deg | panoramic; low bin overlap | — | ~200–300 Hz | UV-sim, polarization-sim | █████ |
| Snake | LW+SW dichromat (species-var) | low (rods dominant noct.) | side-set; modest bin | good (noct.) | low | thermal SIMULATION (pit viper) | ████░ |

*cat: mainstream dichromacy; a 1977 electrophysiological report of a third cone type is disclosed.

---

## 9. What is physically reproducible on an ordinary smartphone (spec §48 #9–10)

| Reproducible (real) | Requires approximation (labeled) | Not possible (never faked, always explained) |
|---|---|---|
| Receptor-based color catch mapping (LUTs) | False-color UV channel (Bee) — no UV sensor | True UV detection |
| Contrast/brightness/tone transforms | Thermal palette (Snake) — no IR sensor | True thermal detection |
| Acuity reduction via spatial filtering | Polarization overlay (Bee) | Polarization measurement |
| Relative FOV *geometry* (masks, binocular/peripheral) | Species-subjective appearance of any color | Actual field > lens FOV (Horse 350°, Dog ~240°) |
| Mosaic/compound-eye blur & desaturation | Tapetum "eye-shine" gloss | Animal neural processing / inner experience |
| Tone mapping, noise, sharpening (bounded) | Eagle "foveal" detail boost | Depth, binocular stereo, absolute motion feel |
---

## 10. Performance strategy (spec §22–23)

Three quality tiers, selected automatically at startup (GPU score + device hints) and **adjustable in Settings**:

| Tier | Internal res | Enabled stages | Target |
|---|---|---|---|
| `high` | 1280×720 | all (motion on) | 60 fps |
| `balanced` (default) | 960×540 | all except motion | 30 fps floor |
| `performance` | 640×360 | color + acuity + tone only; 16³ LUT | 30 fps floor |

- **Auto-degradation:** the perf monitor keeps a rolling 2s FPS/latency window; if < 20 fps for 3s, the engine drops one tier and notifies the user — the camera must never freeze.
- Texture/FBO budgets are fixed per tier (double-buffered, reused). LUTs are RGBA8/16 textures uploaded once.
- `requestVideoFrameCallback` where available; otherwise rAF; the render loop never allocates per frame.
- Motion/temporal stage can be force-disabled (spec §19).

## 11. Privacy strategy (spec §35)

- All camera frames are processed **locally on-device**; nothing is uploaded. No analytics that capture imagery. A privacy line in Settings repeats the promise.
- Optional future cloud features (none planned) would require explicit opt-in + a disclosure *before* any upload, per spec.
- Permissions requested only when needed: **camera** (core), **microphone** (only when user presses Record-video with audio), **no storage permission** (downloads use the browser's native save flow).

## 12. Error handling (spec §37)

Failure → graceful fallback chain, never a crash:

| Failure | Fallback |
|---|---|
| Camera permission denied | Onboarding retry UI + Settings deep-link; the app still renders the **reference test scene** in "Demo mode" so the experience is demonstrable |
| Camera unavailable / init failure / in use | retry ×2 → Demo mode + clear messaging |
| Unsupported lens / capabilities | use widest available; explain FOV limitation banner |
| GPU / WebGL2 unavailable | software CPU renderer at `performance` tier (reuses engine reference impl) |
| Recording failure / storage | stop recorder, keep camera alive, show error toast |
| Low-memory | drop tier → drop resolution → disable FBO doubles |
| Overheating (device-level, where exposed) | show tip + force Balanced |

## 13. Testing strategy (spec §40–41)

- **Profiles:** schema validation; every measured param has a source; range guards (science-paranoia tests assert, e.g., dog 429/555 peaks, horse 428/539, bee 344/436/556).
- **Engine (vitest, headless Node):** pipeline orchestration (correct per-animal stage activation), each processor's CPU reference is behavior-tested, switching is seamless/stateless.
- **Scientific visual validation (spec §41):** a fixed synthetic test image (red/green/blue/yellow chips · grating · fine detail · dim scene) is processed by the CPU reference; assertions like:
  - *Dog/Cat/Horse:* ΔE(red→green) collapses below blue-yellow ΔE (dichromat convergence);
  - *Bee:* UV-proxy channel separated, chip order preserved;
  - *Acuity:* MTF rolloff at species cut-off frequency;
  - *Horse:* binocular mask geometry + blind-spot wedge;
  - *Eagle:* central detail boost bounded (no overshoot halo > small epsilon).
- **App (Playwright + Chrome fake camera):** permission flow, animal switching, before/after, split mode, photo download, video recording (WebM integrity), onboarding, a11y (labels, contrast, keyboard).
- **Performance:** headless frame-rate/latency assertions at each tier; a manual device matrix in README.
- Device matrix targets: Android Chrome (Pixel, mid-range, low-end), iOS Safari, desktop Chrome/Firefox.

## 14. Development phases (spec §47, adapted)

| Phase | Deliverable | Exit criteria |
|---|---|---|
| 1 Research | `docs/SCIENCE.md` | done — profiles sourced & graded |
| 2 Architecture | `docs/ARCHITECTURE.md` | done |
| 3 Foundation | npm workspaces, TS, vitest, engine skeleton + types + registry | tests green |
| 4 Profiles | all 8 profile data files + validation | science tests green |
| 5 Color engine | receptor math, LUT generator, CPU reference, dichromat/trichromat tests | visual validation green |
| 6 Pipeline | full polled stage chain (acuity, fov, lowlight, special, tone) | MTF/mask tests green |
| 7 Camera + renderer | getUserMedia, WebGL2 FBO chain, tier switching, Demo mode | 30 fps suite passes |
| 8 UI | camera screen, selector, info, settings, before/after, onboarding | Playwright flows pass |
| 9 Capture | photo + video of processed output | capture tests pass |
| 10 Education/UX | confidence displays, labels, limitations copy, accessibility | a11y audit passes |
| 11 Optimization | perf auto-degrade, memory budget, battery notes | perf suite passes |
| 12 Release | PWA manifest, offline SW, README, validation matrix | QA checklist complete |