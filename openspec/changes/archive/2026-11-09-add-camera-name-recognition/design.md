## Context

The most common use is a phone pointed at a game on a TV or handheld. Text can be small, low contrast, in a pixel font,
or surrounded by other text. The match has to be conservative: a wrong lock-on is worse than a slow one.

## Goals / Non-Goals

**Goals:** reliable on-device reading of names, conservative matching, a design that can take an image model later.

**Non-Goals:** recognising the Pokémon from its picture, non-Latin scripts.

## Decisions

- **Form factors:** `:app` on phone, tablet and foldable. The screen is full-window, and the aiming box scales with the
  window. Goldens cover the permission states and the overlays. The live preview cannot render in Robolectric.
- **Modules:** matching, normalisation and lock-on are pure Kotlin in `:core:domain`. CameraX and ML Kit stay in
  `:feature:scanner:impl`. `SpeciesRepository.scanNames` already provides the names.
- **Interface for future signals.** `SpeciesSignal` turns one observation of a frame into candidates. `TextNameSignal`
  implements it. `LockOnTracker` only sees candidates, so an image-model signal can be added without changing it.
- **Bundled model.** `com.google.mlkit:text-recognition:16.0.1` ships the Latin model in the APK. The Play Services
  artifact is rejected because it downloads its model.
- **ML Kit telemetry is removed.** Google documents that ML Kit collects device, app and usage data and sends it over
  HTTPS. The `datatransport` components that upload it are removed from the merged manifest, because this app promises no
  network use apart from the optional download. Device traffic measurement confirms the result.
- **Resolution and crop.** Analysis asks for about 2560×1440 and the luma plane under the aiming box is copied to NV21.
  Text only needs luminance, this avoids a `Bitmap` per frame, and it keeps characters above 16×16 pixels in the crop.
- **ViewPort.** A shared `ViewPort` in a `UseCaseGroup` makes `ImageProxy.cropRect` match the preview, so the aiming box
  maps exactly to the buffer in any orientation.
- **Backpressure.** `STRATEGY_KEEP_ONLY_LATEST`. The analyzer blocks its single thread on the recognizer, so slow
  frames drop instead of queueing.
- **Thresholds.** Exact match, then confusable folding (0/O, 1/I/L, 5/S, plus 8/B, 2/Z, 6/G), then edit
  distance: names under 8 letters allow no edits beyond folding, 8 and 9 letters allow one, 10 or more allow two. A
  candidate must be the unique best. Testing against every bundled name showed looser limits read "golden" as Goldeen and
  "magma" as Magmar. Common game words, including all type names, are ignored.
- **Lock-on.** Three of the last five frames.
- **Roman-letter names only.** Names are filtered to those that contain Latin letters, and romanised Japanese readings
  are excluded because no game displays them.
- **Debug overlay.** `BuildConfig.DEBUG` only. It draws recognised lines, candidates and rejection reasons.

## Risks / Trade-offs

- Pixel fonts can defeat OCR → the high-resolution crop and fuzzy matching help. The debug overlay exists for real tuning.
- Removing telemetry components could break ML Kit → covered by on-device tests of actual recognition.
- A second similar name on screen, such as the player's Pokémon → shown as a choice instead of guessing.

## Migration Plan

None.

## Open Questions

None.
