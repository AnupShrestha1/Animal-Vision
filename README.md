# Animal Vision

An Android camera app that shows the live scene as different animals would see it — color,
sharpness, field of view, low light and special senses — modeled from published vision science
rather than artistic filters.

Fourteen species: human (baseline), dog, cat, horse, eagle, honeybee, snake, pigeon, chicken,
butterfly, fruit fly, octopus, mantis shrimp and dolphin. Every profile carries its sources,
per-axis confidence ratings and stated limitations; simulated senses (UV, thermal, polarization)
are always labeled as simulations.

## Features

- Real-time GPU pipeline (OpenGL ES 3.0) on the CameraX preview
- Normal / Animal / Split comparison, plus hold-to-compare
- Photo and video capture of the processed image
- Species info screens with sources and confidence
- Automatic quality tiers to keep the preview smooth
- Everything runs on-device; nothing is uploaded

## Build

Requirements: JDK 17 and the Android SDK (API 35). Point `local.properties` at your SDK
(`sdk.dir=...`), then:

```bash
./gradlew :core:test
```

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:installDebug
```

Minimum Android version: 8.0 (API 26). The camera permission is required.

## Project layout

- `core/` — pure-Kotlin science engine: species profiles, spectral color model, acuity/FOV/low-light
  stages, and a CPU reference renderer used as the test oracle
- `app/` — Android app: camera, GL renderer, Compose UI, capture
- `docs/SCIENCE.md` — sourced parameters for every species
- `docs/ARCHITECTURE.md` — how the code is put together

## Caveat

An animal's subjective experience cannot be observed. The app is a scientific visualization of
measurable properties (receptor sensitivities, acuity, geometry), not a literal recreation of
what an animal perceives.
