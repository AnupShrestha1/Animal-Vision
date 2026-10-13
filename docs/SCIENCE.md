# Animal Vision — Scientific Profiles & Source Registry

> **Phase 1 deliverable.** Every biological parameter below is traceable to peer-reviewed literature or
> established review sources. No values are invented. Where the literature conflicts or is sparse, that
> uncertainty is explicitly graded — never hidden.

---

## How to read this document

**Confidence scale**

| Level | Meaning |
|---|---|
| `█████` Well established | Multiple independent studies, consistent |
| `████░` Strong evidence | One robust study, or consistent secondary sources |
| `███░░` Reasonable approximation | Modeled / inferred from adjacent, well-studied data |
| `██░░░` Significant uncertainty | Conflicting or sparse data |
| `█░░░░` Highly speculative | No direct data; extrapolation only |

**Parameter type** — `M` directly measured (photopigment, anatomy, behavior) · `I` inferred · `A` computational approximation used in this app.

**Metamerism disclaimer:** animal *subjective perception* is unobservable. What this app renders is a *scientific visualization* of measurable properties (receptor sensitivities, acuity, geometry), not a literal recreation of an animal's inner experience.

---

## Common methodology (used by every profile)

1. **Photoreceptor sensitivities** — modeled with the Govardovskii et al. (2000) A1-opsin template, parameterized by the spectral peak (λmax) reported for each species.
2. **Camera RGB → receptor catch** — linear transformation estimated from the phone sensor spectral response against the modeled receptor functions, following the calibrated-imaging method validated in Troscianko & Stevens (2015) (micaToolbox lineage). This is a *mapping*, not an arbitrary "filter".
3. **Color discrimination axes** — receptor-noise-limited opponent modeling per Vorobyev & Osorio (1998), used to decide which dimensions (e.g. L-vs-S) remain available to the species and which collapse.
4. **Acuity** — conversion of each species' cycles-per-degree (c/deg) limit into a spatial low-pass (Gaussian optical-transfer rolloff), following the approach of Caves & Johnsen (2018) and the empirically compiled acuity values below.
5. **Low light** — a tone/contrast/desaturation curve derived from rod/cone density ratios and behavioral light-threshold studies, *never* a simple brightness slider.

All transforms are re-implemented in the engine code; every profile records which stages apply to it.

---

## 1. Human — `Homo sapiens` (reference baseline)

| Parameter | Value | Source | Conf. | Type |
|---|---|---|---|---|
| Color | Trichromat; S≈419 nm, M≈530 nm, L≈559 nm | Stockman & Sharpe (2000) | █████ | M |
| Acuity | ~30 c/deg typical; up to ~60–72 c/deg best | Land & Nilsson (2012); Caves & Johnsen (2018) | █████ | M |
| FOV | ≈180° horizontal total; ≈120° binocular | standard ophthalmology | █████ | M |
| Low light | Rod monochrome scotopic vision below cone threshold | standard vision science | █████ | M |

**Role in app:** identity / before-after baseline. The phone camera is a *stand-in* for human-like viewing, not human vision — the in-app copy states this explicitly. Astigmatic/individual variation is out of scope and documented as a limitation.

---

## 2. Dog — `Canis lupus familiaris`

### Color — dichromat
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Two cone classes; spectral peaks S≈429 nm, L≈555 nm | Neitz, Geist & Jacobs (1989); Jacobs et al. (1993) | █████ | M |
| Behaviorally similar to human deuteranopia-like red/green deficiency; blue-vs-yellow & lightness largely preserved | Neitz et al. (1989); Miller & Murphy (1995) | █████ | M |

**Transformation (A):** compute S and L catches; encode an opponent axis (S vs L) plus lightness; deliberately collapse the red–green dimension so colors dogs confuse converge perceptually. Not a "blue/yellow tint" — the red/green collapse *and* the shifted peak positions (555 vs human 559, 429 vs 419) both emerge from the measured pigments.

### Acuity
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Behavioral bright-light acuity range across breeds ≈1.9–11.5 c/deg; classic estimate ≈20/75 | Lind et al. (2017); Miller & Murphy (1995) | ████░ | M/I |
| → roughly 3–8× coarser than human typical acuity | computed | ████░ | I |

### FOV / geometry
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Total ≈240° (≈240–270° by breed); frontal binocular overlap ≈30–60° (≪ human ≈120°); horizontal visual streak, no macula-like fovea | Miller & Murphy (1995); Peichl (1992); McGreevy et al. (2004) | ███░░ | I/A |

### Low light / motion
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Tapetum lucidum, rod-rich retina → modest low-light gain | Miller & Murphy (1995) | █████ | M |
| Flicker-fusion (CFF) ≈70–80 Hz vs human ≈50–60 Hz in several studies → sharper temporal sensitivity | reviewed in Miklósi (2015) | ███░░ | I |

### Limitations
No UV perception (lens blocks <~400 nm). Breed differences (brachycephalic vs dolichocephalic) change FOV/overlap — stated in the info panel. Subjective experience unknowable.
---

## 3. Cat — `Felis catus`

### Color — dichromat (with documented caveat)
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Molecularly two cone classes; SWS≈450–459 nm, M/LWS≈555–556 nm | Jacobs (1993); Schneeweis & Schnapf (1999) | █████ | M |
| **Uncertainty:** Ringo et al. (1977) reported electrophysiological evidence of a third cone system in the cat retina; never confirmed molecularly. The mainstream model is dichromacy; this app models dichromacy **and discloses the caveat**. | Ringo et al. (1977) | ███░░ | I |

**Transformation (A):** like the dog, a receptor-based dichromat mapping (S vs L opponent axis + lightness), with the cat's actual peak positions (≈452/556 nm). Reds and greens converge; blues stay discriminable.

### Acuity
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Anatomical ≈10 c/deg; behavioral ≈4–6 c/deg (human ≈30–72) → ≈6–14× coarser than human | Wässle (1971); Blake, Cool & Crawford (1974) | █████ | M |
| Equivalent 20/100–20/200 Snellen | Cat senses review (sourced) | ████░ | I |

### FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Total ≈200° vs human 180°; binocular overlap reported ≈90–130° (sources conflict; some report *narrower* than human) | Cat senses review; Miller & Murphy (1995) | ███░░ | I/A |

### Low-light (signature feature)
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Tapetum lucidum + strongly rod-dominant retina; behaviorally needs ≈1/6 the light humans need | Gunter (1951); Steinberg, Reid & Lacy (1973) | █████ | M |
| **Transformation (A):** dark-region tone-lift with contrast preservation, progressive rod-type desaturation as luminance drops, controlled realistic noise. Explicitly *not* a brightness slider; eye-shine/tapetum shown as a labeled visualization. | — | ███░░ | A |

### Motion
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Excellent sensitivity to low-contrast motion; temporal resolution (CFF) in the human order of magnitude | Blake et al. (1974); Pasternak & Merigan (1981) | ███░░ | I |

### Limitations
Binocular *depth* (stereo) cannot be reproduced with a single camera. Slit-pupil depth-of-field effects not modeled (marked). Subjective experience unknowable.

---

## 4. Horse — `Equus caballus`

### Color — dichromat
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Two cone classes; S≈428 nm, L≈539 nm (notably short-shifted L vs humans) | Carroll, Murphy, Neitz, Ver Hoeve & Neitz (2001), *J Vis* | █████ | M |
| Red/green far less discriminable than in humans; blue-vs-yellow better preserved | Carroll et al. (2001) | ████░ | M/I |

### FOV — the defining geometry
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Total horizontal ≈350°; monocular ≈130° per eye; frontal binocular ≈55–65° (up to ≈85° with head raised); **blind spot directly behind and directly below/in front of the muzzle** | Harman et al. (1999); Saslow (1999); Timney & Keil (1992) | █████ | M |
| Eyes placed laterally → strongly *different* vision in monocular-peripheral vs binocular-central regions | consistent anatomy | █████ | M |
| Vertical FOV large and asymmetrical | Harman et al. (1999) | ████░ | M |

**Transformation (A):** a phone cannot capture 350°. We use the widest available lens, then *simulate the geometry*, not the content: a central binocular region processed for higher acuity/color, peripheral monocular regions desaturated/slightly diffused, a labeled blind-spot mask near the bottom-front edge, gentle barrel distortion to convey peripheral squeeze — and explicit copy explaining what is simulated vs the true optical field. Never fake imagery outside the sensor.

### Acuity / motion
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Behavioral estimates ≈20/30–20/60 (near-human at best by some measures), conflicting older claims of much lower acuity; motion sensitivity excellent | Timney & Keil (1992); Saslow (1999) | ███░░ | M/I |

### Low light
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Tapetum lucidum (fibrosum) + rod-rich retina; better night vision than humans, weaker than cat | Harman et al. (1999) | █████ | M |

### Limitations
350° FOV physically impossible on a phone — geometry is illustrated, not recreated. Mono/binocular switching is a visualization. Subjective experience unknowable.
---

## 5. Eagle — model species **Wedge-tailed eagle** (`Aquila audax`)

### Acuity — signature feature
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Behavioral acuity ≈140 c/deg | Reymond (1985), *Vision Res* | █████ | M |
| vs human best ≈60–72 c/deg → ≈2× human *best*, ≈4–5× human *typical* | Land & Nilsson (2012) | █████ | M |
| Two foveae (central + deep temporal), enabling high-resolution forward and sideways vision | Reymond (1985); Tucker (2000) | █████ | M |

**Transformation (A):** the phone sensor/screen can't exceed *captured* detail, so the honest rendering is bounded (halo-free) local-contrast detail enhancement concentrated in a central "foveal" region via a radial acuity mask, plus copy noting true eagle resolution exceeds camera and display. **Explicitly not** "HD sharpening = eagle eyesight."

### Color
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Birds in general: four cone classes + oil droplets (tetrachromacy); many raptors' SWS1 opsin is near-UV tuned | Hart (2001); Ödeen & Håstad (2003) | ████░ | M/I |
| **But** measured raptor ocular media cut off UV strongly (buzzard, sparrowhawk, red kite, kestrel; red kite cutoff ≥394 nm) → effective sensitivity is *violet*, not UV. Vole-urine "UV hunting" hypothesis unsupported. | Lind, Mitkus, Olsson & Kelber (2013) | █████ | M |
| → Model a violet-shifted, slightly extended shortwave palette; **do not advertise UV vision**. | computed | ███░░ | A |

### FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Wide total FOV with pronounced frontal binocular zone; species-specific values sparsely published | general raptor vision literature | ██░░░ | I/A |

### Limitations
True 140 c/deg exceeds hardware — simulation is a visualization. Species vary (wedge-tailed vs golden vs bald eagle); the model species is named in the UI. Subjective experience unknowable.

---

## 6. Honeybee — `Apis mellifera`

### Color — trichromat, shifted to UV/blue/green
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Three photoreceptor classes: UV≈344 nm, blue≈436 nm, green≈556 nm | Peitsch et al. (1992), *J Comp Physiol A* | █████ | M |
| Color-opponent processing; color constancy; UV-blue-green trichromacy differs structurally from human RGB | Hempel de Ibarra, Vorobyev & Menzel (2014) | █████ | M |
| UV flower patterns (nectar guides) documented, but require true UV imaging | Hempel de Ibarra et al. (2014) | █████ | M |

**Phone limitation (must be labeled):** ordinary RGB phone sensors have essentially no usable UV response (lens/IR-cut filter blocks <≈400 nm). Therefore the UV channel **cannot be measured**.
**Transformation (A):** compute the bee's UV/B/G receptor catches; the blue channel serves only as a *weak, explicitly-labeled proxy* for deep UV. UV is rendered as a distinct false-color hue (standard UV-photography convention) and labeled **"UV-inspired simulation — not actual UV sensor data."**

### Acuity
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Behavioral ≈0.5 c/deg; single-photoreceptor acute-zone resolves ≈0.6° objects → ≈100–140× coarser than human | Wehner (1981); Land (1997); Rigosi et al. (2017) | █████ | M |

### Temporal
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Flicker-fusion reported ≈200–300 Hz in some measures (≈3–5× human ≈50–60 Hz); methodological variance | Srinivasan & Lehrer (1984); Skorupski & Chittka (2010) | ███░░ | I |

### Polarization
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Bees detect skylight polarization (dorsal rim area) for navigation | Wehner & Labhart (2006) | █████ | M |

**Transformation (A):** polarization is **simulated** (a labeled overlay in the sky region using solar-position heuristics), never claimed as measured.

### FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Panoramic compound-eye coverage with limited frontal binocular overlap | Wehner (1981) | ███░░ | A |

### Limitations
No true UV capture; no polarization sensor; compound-eye *processing* unreproducible. Subjective experience unknowable.
---

## 7. Snake — model species: **pit viper** (thermal) / **colubrid** (color)

### Color — species-dependent
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Most snakes: a long-wave cone (LWS) + a short-wave cone (SWS1, UV/violet ≈360–400 nm) → often functionally dichromatic; some diurnal colubrids express 3–4 cone types; nocturnal forms mostly rod-driven with retained short-wave cones | Simões et al. (2016); Hauzman et al. (2017) | ████░ | M/I |
| High species variance → the app names which snake model is approximated | — | ███░░ | I |

**Transformation (A):** receptor-based mapping for a short-wave + long-wave dichromat (default), with a documented colubrid "expanded" variant reserved for future.

### Infrared sensing (pit organs) — **separate sensory system, species-specific**
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Pit organs evolved independently in pythons, boas, and pit vipers (Crotalinae) — *not* all snakes | Infrared-sensing-in-snakes review | █████ | M |
| Detect radiant heat ≈5–30 µm via TRPA1-expressing trigeminal terminals; IR and visual signals converge in the optic tectum | Gracheva et al. (2010), *Nature*; Newman & Hartline (1982) | █████ | M |
| Pit spatial resolution ≪ the eye's; a coarse thermal "map" | Newman & Hartline (1982) | ████░ | M/I |

**Phone limitation (must be labeled):** an ordinary RGB camera detects **no** thermal IR.
**Transformation (A):** default mode = **educational thermal simulation** — a coarse thermal-like palette derived from broad image cues, *clearly labeled* ("SIMULATION"), with a dedicated `ThermalSensor` provider interface reserved for real thermal hardware (FLIR-style), enabled only when such a device is detected. No false claim of thermal data.

### Limitations
Not all snakes sense IR. Phone RGB ≠ thermal camera. Subjective experience unknowable.

---

## 8. Pigeon — `Columba livia`

### Color — tetrachromat with a UV edge
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Four cone classes: VS≈393 nm, S≈448 nm, M≈503 nm, L≈559 nm (cloned & expressed pigments) | Kawamura, Blow & Yokoyama (1999), *Genetics* | █████ | M |
| SWS1 (393 nm) mediates the documented 320–380 nm UV response | Kawamura et al. (1999); Wright (1972) | ████░ | M |
| Rod RH1 ≈502 nm | Kawamura et al. (1999) | █████ | M |

**Transformation (A):** bird projection (VS/S/M/L → display); the short-wave end is rendered as a labeled blue-channel proxy — a phone never measures UV.

### Acuity / motion
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Grating bar-width thresholds 1.16–4.0 arcmin across six pigeons (humans 0.79–0.82′ in the same apparatus) → ≈7.5–26 c/deg span; 7.5 c/deg modeled (conservative end) | Blough (1971) | ███░░ | M/I |
| Flicker-fusion figure not adopted (strongly light-level dependent in the literature) | — | ██░░░ | I |

### FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| ≈340° total, ≈60° binocular — deep-lateral eye geometry | Land & Nilsson (2012) convention | ██░░░ | I/A |

### Limitations
Bar-width→c/deg conversion spans a wide range; the conservative end is shown. FOV is eye-placement geometry, not a direct measurement. UV is proxied, never measured. Subjective experience unknowable.
---

## 9. Chicken — `Gallus gallus domesticus`

### Color — tetrachromat behind oil droplets
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Four cone pigments ≈415/460/505/562 nm (multiple extraction & ERG studies) | Fager & Fager (1981); Yen & Fager (1984); Okano et al. (1989); Govardovskii & Zueva (1977); compiled by Bowmaker, Wilkie & Hunt (1999) | █████ | M |
| Colored oil droplets act as long-pass filters narrowing each cone — the A1 template models pigments only | Bowmaker et al. (1999) | ████░ | M/A |

### Acuity / FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Hen grating acuity 4–6 c/deg → 5.0 c/deg modeled | Demello, Foster & Reese (1992) | ████░ | M |
| ≈300° total, ≈40° binocular — lateral-eye geometry | Land & Nilsson (2012) convention | ██░░░ | I/A |

### Limitations
Droplet filtering is disclosed, not simulated. FOV is eye-placement geometry. Subjective experience unknowable.
---

## 10. Butterfly — `Papilio xuthus`

### Color — first proven invertebrate tetrachromat
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Eight receptor varieties in six classes (UV, violet, blue ×2, green ×2, red, broad-band) | Koshitaka et al. (2008); Arikawa (2017) | █████ | M |
| Tetrachromatic use of UV/blue/green/red demonstrated behaviorally; wavelength discrimination minima of 1–2 nm rival humans | Koshitaka et al. (2008) | █████ | M |
| Green & red visual-pigment λmax ≈515/575 nm (effective sensitivities 520/600 nm after the red/yellow filtering pigments) | Arikawa, Scholten, Kinoshita & Stavenga (1999) | █████ | M |
| UV ≈340 and blue ≈460 nm: species-standard published receptor spectra | Arikawa-laboratory literature | ████░ | I |
| Photoreceptors polarization-sensitive | Chen, Belušič & Arikawa (2020) | █████ | M |

**Transformation (A):** UV → labeled blue-channel proxy (bee convention); polarization overlay is a simulation. Violet / wide-band-blue / double-peaked-green classes are omitted — only the four behaviorally demonstrated channels are modeled.

### Acuity / FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Compound-eye detail coarse (≈1 c/deg modeled); regional variation across the eye | eye-geometry convention | ██░░░ | A |
| ≈250° coverage estimate (spherical convention, as with the honeybee) | convention | ██░░░ | A |

### Limitations
UV/blue peaks are literature-standard (green/red measured directly); UV on a phone is a proxy. Acuity and FOV approximate. Subjective experience unknowable.
---

## 11. Fruit fly — `Drosophila melanogaster`

### Color — UV/blue/green trichromat
| Fact | Source | Conf. | Type |
|---|---|---|---|
| In-vivo ERG peak estimates: UV (Rh3) ≈345 nm, blue (Rh5) ≈440 nm, green (Rh1) ≈485 nm; Rh6 ≈540 nm in vivo (insect screening pigments shift apparent peaks vs molecular values) | Sharkey et al. (2020), *Sci. Rep.* | █████ | M |
| UV channel rendered as the documented blue-channel proxy (phones have no UV sensor) | app convention | ███░░ | A |

### Motion — the signature
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Dark-adapted flicker fusion 60–100 Hz (D. hydei); 40–70% higher after light adaptation; profile displays 100 Hz as a conservative photopic figure | Miall (1978) | ████░ | M |
| Flies are "fast-eyed" (biphasic ERG) vs slow-eyed locusts/cockroaches | Miall (1978) | ████░ | M |

### Acuity / FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Compound-eye sampling modeled ≈0.4 c/deg (not a behavioral measurement of D. melanogaster) | eye-geometry convention | ██░░░ | A |
| ≈300° coverage estimate (spherical convention) | convention | ██░░░ | A |

### Limitations
Flicker figure is a conservative point within a measured range. Acuity is a geometric model. UV proxied. Subjective experience unknowable.
---

## 12. Octopus — `Octopus vulgaris`

### Color — a TRUE monochromat
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Eight coastal coleoid cephalopods monochromatic, λmax 484–505 nm; profile models the 495 nm midpoint (MODELED) | Chung & Marshall (2016), *Proc. R. Soc. B* | ████░ | M/A |
| Behavioral color blindness: no hue discrimination by an octopus | Messenger (1977), *Nature* | █████ | M |
| Proposed chromatic-aberration + pupil-shape mechanism could in principle yield color cues without color vision | Stubbs & Oakley (2016), *PNAS* | ███░░ | I |

**Transformation (A):** single normalized catch → luminance; pure grayscale on screen. Any hue would be a false claim — wavelength ratios are literally unavailable to the eye.

### Polarization
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Cephalopods sense polarized reflections; cuttlefish polarization vision is high-resolution | Chiou et al. (2007); Temple et al. (2012) | █████ | M |
| App overlay is a SIMULATION — an RGB phone sensor cannot measure polarization | hardware fact | █████ | M |

### Acuity / FOV / low light
| Fact | Source | Conf. | Type |
|---|---|---|---|
| ≈4 c/deg modeling choice (cephalopod resolution estimates are method-dependent and sparse) | modeling choice | ██░░░ | A |
| ≈270° lateral-eye geometry | convention | ██░░░ | I/A |
| Aquatic attenuation (depth, turbidity) NOT modeled — low-light stage disabled rather than guessed | — | ███░░ | A |

### Limitations
Single λmax is a midpoint model, not the species' exact measured peak. Acuity flagged uncertain. Subjective experience unknowable.
---

## 13. Mantis shrimp — `Odontodactylus scyllarus`

### Color — 16 classes (4 modeled here)
| Fact | Source | Conf. | Type |
|---|---|---|---|
| 16 functional receptor classes: UV, colour, spatial and polarization subsystems | Cronin, Bok, Marshall & Caldwell (2014) | █████ | M |
| Colour is channel-**labelled**, not opponent-compared — a different form of colour vision | Thoen, How, Chiou & Marshall (2014), *Science* | █████ | M |
| App models 4 representative peaks (340/450/500/560 nm) spanning UV→red — MODELED, not the literal class measurements | coverage choice | ███░░ | A |

### Polarization
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Linear AND circular polarization detection (birefringent receptor filters); midband rows specialized for it | Marshall (1988); Marshall, Land, King & Cronin (1991); Cronin et al. (2014) | █████ | M |
| App overlay simulates LINEAR polarization only — circular cannot be represented by a phone | hardware fact | █████ | M |

### Acuity / FOV
| Fact | Source | Conf. | Type |
|---|---|---|---|
| ≈2 c/deg modeled from mid-band angular sampling (behavioral acuity not established) | modeling choice | ██░░░ | A |
| ≈300° stalked-eye coverage estimate (spherical convention) | convention | ██░░░ | A |

### Limitations
4-of-16 channel model with MODELED peaks. A1 template omits their intrarhabdomal filtering pigments. Subjective experience unknowable.
---

## 14. Dolphin — `Tursiops truncatus`

### Color — rod-monochromat lineage with a broken blue gene
| Fact | Source | Conf. | Type |
|---|---|---|---|
| Expressed pigments: rod rhodopsin λmax 488 nm; a single LWS cone pigment λmax 524 nm (both blue-shifted vs terrestrial mammals) | Fasick, Cronin, Hunt & Robinson (1998) | █████ | M |
| SWS opsin carries inactivating deletions (frame-shift) and is not expressed — the mammalian blue–yellow axis is genetically gone | Fasick et al. (1998); Meredith et al. (2013) | █████ | M |

**Transformation (A):** single catch → grayscale. The loss is a missing dimension, not a two-color "deficiency."

### Acuity / FOV / low light
| Fact | Source | Conf. | Type |
|---|---|---|---|
| In-air/underwater acuity ≈12.6 arcmin → ≈4.8 c/deg | Tomonaga, Uwano & Saito (2014) | ████░ | M |
| ≈300° lateral-eye geometry; cetacean field measurements sparse | Land & Nilsson (2012) convention | ██░░░ | I/A |
| Underwater attenuation NOT modeled — low-light stage disabled rather than guessed | — | ██░░░ | A |

### Limitations
Monochromat projection does not separately simulate rod/cone adaptation switching. FOV approximate. Subjective experience unknowable.
---

## References (peer-reviewed / institutional)

1. Stockman & Sharpe (2000) *Vision Research* 40, 1711–1737.
2. Govardovskii et al. (2000) *Visual Neuroscience* 17, 509–528.
3. Troscianko & Stevens (2015) *Methods Ecol. Evol.* 6, 1320–1331.
4. Vorobyev & Osorio (1998) *Proc. R. Soc. B* 265, 351–358.
5. Caves & Johnsen (2018) *Methods Ecol. Evol.* 9, 793–797.
6. Land & Nilsson (2012) *Animal Eyes*, 2nd ed., OUP.
7. Neitz, Geist & Jacobs (1989) *Visual Neuroscience* 3, 119–125.
8. Jacobs, Deegan, Crognale & Fenwick (1993) *Visual Neuroscience* 10, 173–180.
9. Miller & Murphy (1995) *JAVMA* 207, 1623–1634.
10. Peichl (1992) *J. Comp. Neurol.* 324, 603–620.
11. Lind, Milton, Andersson, Jensen & Roth (2017) *PLoS ONE* 12(12), e0188557.
12. Jacobs (1993) *Biological Reviews* 68, 413–471.
13. Schneeweis & Schnapf (1999) *J. Neuroscience* 19, 10732–10741.
14. Ringo, Wolbarsht, Wagner, Crocker & Amthor (1977) *Science* 198, 753–754.
15. Wässle (1971) *Vision Research* 11, 995–1006.
16. Blake, Cool & Crawford (1974) *Vision Research* 14, 1211–1217.
17. Gunter (1951) *J. Physiology* 114, 8–15.
18. Steinberg, Reid & Lacy (1973) *J. Comp. Neurol.* 148, 229–248.
19. Carroll, Murphy, Neitz, Ver Hoeve & Neitz (2001) *Journal of Vision* 1, 80–87.
20. Harman, Moore, Hoskins & Keller (1999) *Equine Vet. J.* 31, 384–390.
21. Timney & Keil (1992) *Vision Research* 32, 2289–2293.
22. Saslow (1999) *Applied Animal Behaviour Science* 61, 273–284.
23. Reymond (1985) *Vision Research* 25, 1477–1491.
24. Tucker (2000) *J. Exp. Biol.* 203, 3745–3754.
25. Hart (2001) *Prog. Retin. Eye Res.* 20, 675–703.
26. Ödeen & Håstad (2003) *Proc. R. Soc. B* 270, 2411–2415.
27. Lind, Mitkus, Olsson & Kelber (2013) *J. Exp. Biol.* 216, 1819–1826.
28. Peitsch et al. (1992) *J. Comp. Physiol. A* 170, 23–40.
29. Hempel de Ibarra, Vorobyev & Menzel (2014) *J. Comp. Physiol. A* 200, 411–433.
30. Wehner (1981) *Handbook of Sensory Physiology* VII/6C, 287–616.
31. Rigosi, Wiederman & O'Carroll (2017) *Scientific Reports* 7, 45972.
32. Srinivasan & Lehrer (1984) *J. Comp. Physiol.* 154, 297–305.
33. Skorupski & Chittka (2010) *J. Exp. Biol.* 213, 1686–1690.
34. Wehner & Labhart (2006) *Invertebrate Vision*, Cambridge Univ. Press.
35. Simões et al. (2016) *Mol. Biol. Evol.* 33, 2483–2495.
36. Hauzman et al. (2017) *BMC Evol. Biol.* 17, 249.
37. Gracheva et al. (2010) *Nature* 464, 1006–1011.
38. Newman & Hartline (1982) *Scientific American* 246(3), 116–127.
39. Wikipedia vision reviews used as secondary indexes only (*Cat senses*, *Equine vision*, *Dog anatomy*, *Bird vision*, *Infrared sensing in snakes*); all values traced to the primary literature above.
40. Kawamura, Blow & Yokoyama (1999) *Genetics* 153, 1839–1850.
41. Blough (1971) *J. Exp. Anal. Behav.* 15, 57.
42. Wright (1972) *Vision Research* 12, 1447–1464.
43. Bowmaker, Wilkie & Hunt (1999) *Proc. 22 Int. Ornithol. Congr.* 2729–2742.
44. Fager & Fager (1981); Yen & Fager (1984); Okano et al. (1989); Govardovskii & Zueva (1977) — chicken visual pigments, as compiled in ref. 43.
45. Demello, Foster & Reese (1992) — hen grating acuity, 4–6 cycles/degree.
46. Koshitaka, Kinoshita, Vorobyev & Arikawa (2008) *Proc. R. Soc. B* 275, 947–954.
47. Arikawa (2017) *J. Physiol.* 595, 5457–5464.
48. Arikawa, Scholten, Kinoshita & Stavenga (1999) *Zool. Sci.* 16, 17–26.
49. Chen, Belušič & Arikawa (2020) *J. Comp. Physiol. A* 206, 199–216.
50. Sharkey, Blanco, Leibowitz, Pinto-Benito & Wardill (2020) *Scientific Reports* 10, 18242.
51. Miall (1978) *Physiological Entomology* — flicker fusion frequencies of six laboratory insects.
52. Chung & Marshall (2016) *Proc. R. Soc. B* 283, 20161346.
53. Messenger (1977) *Nature* 267, 804–805.
54. Stubbs & Oakley (2016) *PNAS* 113, 8206–8211.
55. Chiou, Mäthger, Hanlon & Cronin (2007) *J. Exp. Biol.* 210, 3624–3635.
56. Temple et al. (2012) *Curr. Biol.* 22, R121–R122.
57. Cronin, Bok, Marshall & Caldwell (2014) *Phil. Trans. R. Soc. B* 369, 20130032.
58. Thoen, How, Chiou & Marshall (2014) *Science* 343, 411–413.
59. Marshall (1988) *Nature* 333, 557–560.
60. Marshall, Land, King & Cronin (1991) *Phil. Trans. R. Soc. B* 334, 33–56.
61. Fasick, Cronin, Hunt & Robinson (1998) — the visual pigments of the bottlenose dolphin (*Tursiops truncatus*).
62. Meredith, Gatesy, Emerling, York & Springer (2013) *PLoS Genetics* 9, e1003432.
63. Tomonaga, Uwano & Saito (2014) *Scientific Reports* 4, 3717.

> **Versioning:** profiles carry `profileVersion` + `researchUpdated` so they can be revised independently of engine code as literature evolves (spec §42).