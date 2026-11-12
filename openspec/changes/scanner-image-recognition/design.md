# Design

## Context

See proposal.md for why. The current pipeline, all on the camera's single analysis thread:

- `CameraViewfinder` binds an `ImageAnalysis` (YUV_420_888, about 2560×1440, keep-only-latest) to a `ViewPort`, so
  `ImageProxy.cropRect` is what the user sees.
- `TextFrameAnalyzer` maps the on-screen `AimBox` into sensor coordinates (`cropRegion`) and copies only the luma plane
  (`lumaCropToNv21`). `MlKitLineRecognizer` then returns the lines it reads.
- `PokedexViewModel.onFrame` runs `TextNameSignal.analyze`, turns the candidates into a `Set<SpeciesId>` and reduces
  `ScanState.afterFrame`. That feeds `LockOnTracker` (3 of the last 5 frames).
- `SignalResult.decisions` already records, for each piece of text, every species it matched, ambiguous fuzzy ties
  included. The debug overlay that used to read it is gone.
- The model files are in `feature/pokedex/impl/src/main/assets/`, and their contract is in the ML package README:
  - `dexlens_encoder.tflite`: input `[1,224,224,3]` float RGB 0–255, output a unit `[1,256]` vector, CPU with 4 threads,
    18.9 ms on a Pixel 6, about 86 MB of memory.
  - `dexlens_references.bin`: magic `DXRF`, version 1, N=28,085 int8 vectors of D=256 with one float scale, and the
    National Dex numbers as uint16. The match takes the best dot product per species divided by the scale, 6.7 ms in ART.
- Placement follows ADR 0013: camera and recogniser code lives in `:feature:pokedex:impl`'s `scanner` package. Pure
  matching logic lives in `:core:domain`'s `scan` package.

Form factors: only `:app` (phone, tablet, foldable). The scanner area's layout doesn't change apart from the hint text. On expanded windows the scanner sits above the list as it does today, so compact and expanded
windows get the same treatment.

## Goals / Non-Goals

**Goals:**
- Run the picture signal next to text with no visible delay. The scanner keeps up with the camera on a Pixel 6-class phone.
- Keep every lock-on rule pure Kotlin in `:core:domain`, so unit tests cover the combination without a camera or LiteRT.
- Keep all score cut-offs in one place, so tuning them later is a one-line change.

**Non-Goals:**
- Running the model in Robolectric. LiteRT is native, so the encoder is exercised on a device. Every JVM test goes
  through a fake.
- Detecting where the Pokémon is in the frame.

## Decisions

### 1. Colour crop sampled straight from the YUV planes

The analyzer keeps YUV_420_888. A new `colourSquare` function takes the centred square of the existing `CropRegion`
(side = its shorter edge) and fills a reused 224×224×3 `FloatArray` directly from the Y, U and V planes:

- rotation from `rotationDegrees`, so the square is upright;
- area averaging over each output pixel's footprint on Y, and the matching U and V sample, honouring row and pixel strides;
- BT.601 full-range YUV to RGB, written as 0–255 floats.

The text path and its luma crop are unchanged.

*Alternatives:*
- `OUTPUT_IMAGE_FORMAT_RGBA_8888` shared with ML Kit converts the whole 2560×1440 frame every frame (a 14 MB buffer) when
  only about 900×900 pixels are needed, and it would rework the text path.
- `ImageProxy.toBitmap()` and then crop and scale allocates a full-frame bitmap per frame.
- Plain bilinear sampling at a 4× downscale aliases, and the training pipeline resized with antialiasing. Area averaging
  is the closest cheap match.

*Square choice:* the aiming box is at most 1.5:1. Its centred square crops a little off the sides, which matches "aimed
crops score better than the whole frame" from the benchmark. Padding the box to a square was rejected: the model never
saw letterboxed input.

### 2. LiteRT CompiledModel, bundled runtime, CPU only (ADR 0014)

- `com.google.ai.edge.litert:litert` 2.3.x (latest stable at implementation; 2.3.x needs no `android.uniquePackageNames`
  workaround on AGP 9).
- `CompiledModel.create(assets, "dexlens_encoder.tflite", CompiledModel.Options(Accelerator.CPU))`, with 4 CPU threads if
  the options expose a thread count, otherwise the default. No `litert-gpu`.
- The input and output `TensorBuffer`s are created once and reused, with one warm-up run at load.

*Alternatives:*
- The Google Play services runtime (`play-services-litert`) is Google's default. It was rejected because it ties
  recognition to Play services being present and up to date, which breaks "works offline from first launch". It would
  also exclude builds without Google Play, which matters because the app is open source.
- The legacy `org.tensorflow.lite.Interpreter` is deprecated.
- The GPU delegate fails on this weight-only 8-bit model, and the float model was barely faster on the GPU.

### 3. A long-lived picture recogniser on its own thread, running beside ML Kit

The recogniser comes in two parts, so the threading logic can be tested on the JVM:

- **`PictureRecognizer`** (scanner package, internal, `AutoCloseable`) owns a single-thread executor, which LiteRT's
  thread-confinement rule requires, and one input buffer of its own. It runs a `PictureEngine` on that thread and
  exposes `submit(input: FloatArray): PendingScore?` and `PendingScore.await(): PictureScores?`. `await` blocks the caller
  for at most `PICTURE_TIMEOUT_MS` (200 ms):
  - **Busy or still loading:** `submit` returns `null` at once and queues nothing. At most one job is ever queued, and
    the analysis thread never waits behind a model load.
  - **Free:** `submit` copies `input` into the recogniser's own buffer and queues one job. If `await` times out it returns
    `null` and leaves the job running. The busy flag clears when the job ends, so a late job can't race the next frame's
    buffer.
  - **Engine failure** (load or inference throws): it logs with Timber and returns `null` from then on.
  - **After `close()`:** `submit` returns `null` and never throws. `close()` submits the engine's close to the executor and
    then shuts it down.
- **`LiteRtPictureEngine`** (device-only) implements `PictureEngine`: `load()` and `embed(input): FloatArray`.
  - `load()` reads the encoder (CPU, 4 threads where the options allow) and the reference bytes from assets, then parses
    them with `ReferenceGallery.parse`.
  - It holds the reused tensor buffers and runs one warm-up at load.
  - Only this class lands outside JVM coverage, next to `MlKitLineRecognizer` and `CameraViewfinder`.

It is remembered in `PokedexScreen` while the scanner switch is on and the camera permission is granted, above the
viewfinder slot. Collapsing and expanding
the sheet therefore doesn't reload the 17 MB of model files. It is closed (on its own thread, then the executor shuts
down) when the screen leaves or the switch turns off.

Per frame, the analysis thread:
1. builds the colour square while the `ImageProxy` is still open;
2. starts the picture job on the recogniser's thread (the recogniser copies the input first);
3. runs ML Kit on the luma crop;
4. collects the picture result, waiting no longer than the timeout;
5. closes the frame and calls `onFrame` with both.

Text and picture run in parallel, so a frame costs about the slower of the two, not their sum. A frame without picture
scores (busy, loading, timed out, failed, closed) follows text-only rules.

Tests:
- A `FakePictureEngine` that is scripted, blocks or throws drives the real `PictureRecognizer` on the JVM.
- A `FakePictureRecognizer` with scripted scores drives the analyzer and ViewModel tests.

*Alternatives:*
- Running the model sequentially on the analysis thread adds its ~26 ms to every frame.
- Owning the model inside the viewfinder reloads it on every sheet collapse.
- A Hilt singleton injected into the ViewModel would put a platform recogniser in the ViewModel, which only reduces state.

### 4. Reference gallery in `:core:domain`

`ReferenceGallery.parse(bytes: ByteArray): ReferenceGallery?` validates the header (magic, version 1, D=256, 1 byte per
value, size) and every species ID (1..1025), so a bad or retrained file fails quietly to text-only instead of throwing
in the loop. It copies the vectors into a `ByteArray` and the IDs into a `ShortArray`.

`rank(embedding: FloatArray): PictureScores` runs the README's best-per-species loop and divides by the scale.
`PictureScores` has two parts:
- `score(id)`: a lookup for any species, backed by a copied 1026-slot array, so a name ranked below the top few still
  gets its picture score;
- `top(n)`: best first, used for the picture-only path and the debug logs.

It is pure Kotlin, so a unit test can build a tiny synthetic file. The asset bytes are read in the recogniser
(`AssetManager`, on its own thread) and handed to `parse`.

### 5. Combining the signals: per-frame evidence, then a weighted lock-on

All the cut-offs live in one place, `RecognitionThresholds` (domain):

| Cut-off | Starting value | Source |
|---|---|---|
| `candidate` | 0.90 | README default 0.94, lowered after the first real game screen scored 0.81 |
| `imageOnly` | 0.92 | README 0.957 (90% of unseen sprites, 0.5% false accepts on simulated capture), lowered on device |
| `leadMargin` | 0.015 | A guess, to be tuned from the debug logs |

`combineSignals(text: SignalResult, picture: PictureScores?, thresholds): FrameEvidence` (domain, pure) decides, per
frame:

1. **Picture tie-break for text.** For each `MatchDecision` with more than one species (a missing gender symbol, or an
   `AMBIGUOUS` fuzzy tie, which today yields nothing):
   - if the best picture score among those species reaches `candidate` and leads the others by `leadMargin`, keep only
     that species as a name candidate. The margin stops near-equal scores, such as Nidoran♀ and ♂, from flipping
     between frames;
   - otherwise keep today's behaviour (both Nidorans; nothing for an ambiguous tie).
2. **Name frame.** If there is any name candidate, each one gets `NAME` evidence, or `BOTH` if its picture score reaches
   `candidate`. Picture species the text doesn't name are dropped ("name wins").
3. **Picture-only frame.** If there is no name candidate and the best picture score reaches `imageOnly`, the species
   within `leadMargin` of the best that also reach `imageOnly`, at most 3, get `PICTURE` evidence. A single species with a
   clear lead gets it alone.
4. Otherwise the frame is empty.

`FrameEvidence` is `Map<SpeciesId, Evidence>`, with `Evidence` being `NAME`, `PICTURE` or `BOTH`.

`LockOnTracker` keeps its 5-frame window, now of `FrameEvidence`. A species is stable when, within the window:
- its `NAME`/`BOTH` frames ≥ 3, or
- its `BOTH` frames ≥ 2, or
- its `PICTURE` frames ≥ 4.

The companion rule applies to name evidence only, exactly as today: when something locks, species with name evidence
(`NAME` or `BOTH`) in the latest frame and in at least 2 frames of the window are offered with it. Picture-only species
never ride along; they lock only through their own 4-frame rule.

The three-choice cap then holds by counting. Each picture-only frame holds at most 3 species, so 5 frames hold at most
15 slots. Four species at 4 frames each would need 16 slots, so at most 3 picture-only species can be stable at once.
Empty windows still collapse to `Empty`.

`ScanState.afterFrame` takes `FrameEvidence` instead of `Set<SpeciesId>`. Its replace and haptic logic is unchanged.

*Alternatives:*
- Summing weighted scores into one number per species is harder to reason about and harder to state as testable
  scenarios.
- A sequential fallback (image only after N frames without text) is slower, and it can't break ties.

### 6. Debug logs instead of a readout

Tuning reads logcat in debug builds (Timber is planted only there), with no debug UI and no settings entry:
- `FrameAnalyzer` logs each frame: how long it took, the lines read, and the picture's top 3 species with scores, or
  "skipped" when the picture signal had no answer.
- `PokedexViewModel` logs a frame's evidence when there is any, and every lock that changes the listed match.

*Alternative:* an on-screen scores readout behind a debug-only settings switch was built first and dropped. It added a
preference, a settings section and goldens for a tool only developers use.

### 7. Hint and strings

`pokedex_scanner_hint` becomes "Point at a Pokémon or its name".

## Risks / Trade-offs

- **The thresholds are untested on real game screens.** A wrong image-only lock is worse than none. → The image-only lock
  is the strictest rule (4 of 5 frames, high score), the debug logs support tuning on device, and the cut-offs live in one
  constant set.
- **Frame time.** ML Kit on a large crop plus the encoder could drop the frame rate on older phones. → They run in
  parallel and keep-only-latest drops stale frames. Measure on the Pixel 6 and S24 Ultra during QA. If a frame takes more
  than about 150 ms, a follow-up can run the picture signal every other frame.
- **Memory.** The model needs about 86 MB plus the 7 MB references while the home scanner is on and the camera is
  allowed. → Both are released when the switch turns off, the permission is missing or the screen leaves.
- **APK size.** It grows by LiteRT's native library for each ABI. → Measure it in the PR. The Play bundle splits per ABI.
- **A crop that doesn't match training** (colour range, resize, centre square) silently lowers scores. → A JVM unit test
  checks the crop's colours and orientation with synthetic YUV buffers. Device QA scans gameplay images on a monitor with
  debug logs on, and checks that clear sprites score above `candidate`.
- **The model files hold no pictures.** Spec wording: the reference vectors are numbers derived from images. The MVP
  spec's art rule is updated to say so explicitly.

## Migration Plan

There is no data migration. Rollback means reverting the PR, which leaves the
assets in place, unused as before.

## Open Questions

- The final `imageOnly` and `leadMargin` values. They will be tuned on device from the debug logs, after merge if needed,
  without changing the rules.
