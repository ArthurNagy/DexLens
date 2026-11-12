# Tasks

## 1. Docs and decisions

- [x] 1.1 docs/DECISIONS/0014-litert-image-recognition.md: LiteRT CompiledModel, bundled runtime, CPU only. Covers the
  Play services runtime, legacy Interpreter and GPU alternatives, and the consequences (APK size, native library,
  device-only encoder tests).
- [x] 1.2 docs/MVP_SPEC.md: move "Recognising Pokémon from images" from out of scope into v1, next to camera name
  recognition. Add to the official-art line that the bundled model files hold numbers derived from pictures, not pictures.
- [x] 1.3 gradle/libs.versions.toml: add `litert` (latest stable 2.3.x, at least two weeks old) and the
  `litert` library alias.

## 2. :core:domain: picture matching and combined lock-on

- [x] 2.1 `scan` package: add `SpeciesScore`, `PictureScores` (`score(id)` for any species, plus `top(n)` best first) and
  `RecognitionThresholds` (candidate 0.90, imageOnly 0.92, leadMargin 0.015; lowered from 0.94 / 0.957 after device testing) in one place.
- [x] 2.2 `ReferenceGallery.parse(bytes)`: validate the `DXRF` header (version 1, D=256, 1 byte per value, size) and every
  species ID (1..1025), and return null on any mismatch. Add `rank(embedding): PictureScores` with the best-per-species
  loop, divided by the scale.
- [x] 2.3 `ReferenceGalleryTest` on a synthetic file:
  - parses ids, scale and vectors;
  - rejects bad magic, a bad version, a truncated file and a species ID outside 1..1025;
  - keeps the best reference per species;
  - `top(n)` ranks best first, and `score(id)` answers for a species below the top n;
  - two references of the same species (base and Mega) count as that species, tagged
    `// Scenario: Form counts as its species`.
- [x] 2.4 `combineSignals(text, picture, thresholds): FrameEvidence` with `Evidence` (`NAME`, `PICTURE`, `BOTH`): picture
  tie-break for multi-species decisions (including `AMBIGUOUS`, needing `candidate` and `leadMargin`), "name wins" using
  `score(id)`, and picture-only frames (species reaching imageOnly within leadMargin of the best, at most 3). Update the `FrameObservation` and `SignalResult` KDoc that says only the debug overlay reads
  decisions.
- [x] 2.5 `LockOnTracker` over `FrameEvidence`: stable at NAME/BOTH ≥ 3, BOTH ≥ 2 or PICTURE ≥ 4 in the 5-frame window.
  The companion rule covers name evidence only (in the latest frame and in ≥ 2 frames of the window). Picture-only
  species never ride along. Empty windows collapse to `Empty`.
- [x] 2.6 `CombineSignalsTest`, with one test per scenario, each tagged `// Scenario:`:
  - Picture picks the gender;
  - Picture settles a misread;
  - Close picture scores keep both names;
  - No picture keeps both names;
  - Name decides between close pictures (including a named species ranked below the top 5);
  - Slow picture frame keeps names working (null picture scores give today's candidates).
- [x] 2.7 `LockOnTrackerTest`: update the existing tests (Lock after repeated frames, Single frame does not lock, Several
  names offered as choices) to evidence frames. Add tests tagged `// Scenario:`:
  - Sprite without a name locks;
  - Three picture frames are not enough;
  - Weak picture score does not lock;
  - Close picture scores offered as choices;
  - At most three picture choices;
  - Picture-only species does not ride along;
  - Agreement locks after two frames;
  - Name alone still needs three frames;
  - Name and picture disagree.
  Also convert the existing "Nickname keeps scanning" test to evidence frames.
- [x] 2.8 `ObserveRecognitionScoresShownUseCase` and `SetRecognitionScoresShownUseCase`, plus the two methods on
  `UserPreferencesRepository`, with tests next to `ScannerEnabledUseCasesTest`.

## 3. :core:datastore, :core:data, :core:testing: scores preference

- [x] 3.1 `UserPreferencesDataSource`: `recognitionScoresShown` flow (key `recognition_scores_shown`, default false) and a
  setter, with tests in `UserPreferencesDataSourceTest` (the default is false, and the value survives a new instance).
- [x] 3.2 `OfflineUserPreferencesRepository` passes both through, with tests. `FakeUserPreferencesRepository` gains the
  value.

## 4. :feature:pokedex:impl: colour crop, LiteRT recogniser, analyzer

- [x] 4.1 Add the `litert` dependency to `feature/pokedex/impl/build.gradle.kts`. Confirm the build needs no
  `android.uniquePackageNames` flag and that `.tflite` stays uncompressed.
- [x] 4.2 `colourSquare(...)` in the scanner package:
  - takes the centred square of the `CropRegion`;
  - area-averages the Y, U and V planes, honouring strides;
  - rotates the result upright;
  - converts BT.601 full-range to RGB;
  - writes 224×224×3 floats from 0 to 255 into a reused array.
- [x] 4.3 `ColourSquareTest` on synthetic YUV_420_888 buffers:
  - solid red, green and blue convert within tolerance;
  - a quadrant pattern lands upright for 0, 90, 180 and 270 degrees;
  - the square is centred in a wide crop;
  - pixel stride 2 (interleaved UV) works.
  Tag the orientation test `// Scenario: Crop follows the aiming box`.
- [x] 4.4 `PictureEngine` interface (`load()`, `embed(input)`, `close()`) and `PictureRecognizer`:
  - its own single-thread executor and input buffer;
  - `submit(input): PendingScore?`, which returns null while busy, loading, failed or closed, and copies the input;
  - `PendingScore.await()`, bounded by `PICTURE_TIMEOUT_MS` (200 ms), returning null on timeout;
  - Timber logging and permanent null after an engine failure;
  - `close()` closes the engine on its thread, then shuts the executor down, and is safe while a job is in flight.
- [x] 4.5 `PictureRecognizerTest` on the JVM with a `FakePictureEngine` that is scripted, blocks or throws:
  - returns scores;
  - null while loading;
  - null while busy, with only one job queued;
  - null on timeout, and the next frame is not submitted until the late job ends;
  - null for good after a failure;
  - `submit` after `close` returns null without throwing.
  Tag the timeout test `// Scenario: Slow picture frame keeps names working` and the failure test
  `// Scenario: Model missing keeps names working`.
- [x] 4.6 `LiteRtPictureEngine` (device-only):
  - the encoder from assets on the CPU, with 4 threads where the options allow;
  - parses `dexlens_references.bin` with `ReferenceGallery.parse`;
  - reused tensor buffers, with one warm-up run.
- [x] 4.7 `PictureRecognitionDeviceTest` (androidTest, next to `TextRecognitionDeviceTest`): the real engine and gallery on
  a synthetic mid-grey square and a noise square, asserting that no species reaches the candidate threshold. Tag it
  `// Scenario: Everyday scene scores nothing`. Also assert that a `top(3)` result arrives, for
  `// Scenario: Picture recognition in airplane mode`. The real-sprite scenario "Clear sprite scores its species" is
  covered by device QA in 6.3 only, because the repo holds no official art to test with.
- [x] 4.8 `FakePictureRecognizer` in the pokedex test sources, returning scripted scores or null.
- [x] 4.9 `TextFrameAnalyzer` becomes `FrameAnalyzer`:
  - builds the colour square while the frame is open and submits it;
  - runs ML Kit on the luma crop;
  - waits for the picture result with a bounded timeout;
  - emits `AnalyzedFrame(lines, pictureScores)`.
  Update its tests to use the fake recogniser.
- [x] 4.10 `PokedexScreen`: remember a `PictureRecognizer` over `LiteRtPictureEngine` while the scanner switch is on and the
  camera permission is granted, above the viewfinder slot. Close it when the screen leaves, the switch turns off or the
  permission is missing. Pass it to the analyzer.
- [x] 4.11 `PokedexViewModel` and `ScanState`:
  - `onFrame` runs `combineSignals` and `afterFrame(FrameEvidence)`;
  - observes the scores preference into `PokedexState.scoresShown`;
  - stores the top 3 in `ScanState.pictureScores` only while it is on;
  - updates `PokedexPreviews` and `PokedexState.Initial`;
  - converts `ScanStateTest` (its 5 tagged scenarios) to `afterFrame(FrameEvidence)`.
- [x] 4.12 `PokedexViewModelTest`, with tests tagged `// Scenario:`:
  - Sprite without a name locks (end to end through `onFrame` with picture scores);
  - Agreement locks after two frames;
  - Readout off by default (no picture scores in state while the preference is off);
  - Readout follows the frames;
  - Model missing keeps names working (3 frames of "PIKACHU" with null picture scores lock it, and `scan.error` stays
    null).
  Keep the existing scan tests passing with null picture scores.
- [x] 4.13 `ScannerArea`: the scores readout (`scanner:scores`, `labelSmall`, scrim with tokens only, bottom start, names
  from `speciesById`, best first). `pokedex_scanner_hint` becomes "Point at a Pokémon or its name".
- [x] 4.14 `PokedexScreenTest`, with tests tagged `// Scenario:`:
  - Hint and aiming box shown (new text);
  - Readout shows the top three;
  - Readout off by default.
- [x] 4.15 Re-record the pokedex goldens for the new hint (phone and tablet). Add phone and tablet goldens of the scanner
  with the readout on.

## 5. :feature:settings:impl: debug-only scores switch

- [x] 5.1 `SettingsState`, `SettingsViewModel`: observe and set the scores preference.
- [x] 5.2 `SettingsScreen` decides `showDeveloperOptions` from `FLAG_DEBUGGABLE`. `SettingsContent` takes it and shows a
  "Developer" heading with the "Show recognition scores" switch. Strings use the `settings_` prefix.
- [x] 5.3 Tests tagged `// Scenario:`:
  - Scores switch in debug builds (`SettingsViewModelTest` persists the choice; `SettingsScreenTest` shows the switch, off
    by default);
  - No scores switch in release builds (`SettingsScreenTest` with `showDeveloperOptions = false`).
- [x] 5.4 Settings goldens: existing goldens with developer options off stay unchanged. Add phone and tablet goldens with
  them on.

## 6. Verification

- [x] 6.1 Local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [x] 6.2 Record the APK size before and after in the PR body.
- [x] 6.3 On a device (emulator first, then a phone the user approves), with the readout on, scan gameplay images on a
  monitor:
  - check that clear sprites reach the candidate threshold and that a sprite with no name locks;
  - check that a name still locks on its own, and that airplane mode changes nothing (Picture recognition in airplane
    mode, Recognition in airplane mode);
  - note the frame time and the scores seen, to tune the thresholds.
- [x] 6.4 Run the `android-reviewer` agent on the diff and fix its findings.
