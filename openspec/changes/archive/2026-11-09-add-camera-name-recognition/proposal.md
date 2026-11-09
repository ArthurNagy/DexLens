Issue: none

## Why

The main reason to open DexLens is that a Pokémon RPG is on a screen in front of you. Reading the name off that screen
with the camera is faster than typing it, and it has to work offline from the first launch.

## What Changes

- A camera screen with a CameraX preview, an aiming box, a hint, a torch toggle and permission handling.
- On-device Latin text recognition, using the bundled ML Kit model, on high-resolution frames cropped to the aiming box.
- A name matcher: normalized exact matches first, then a bounded edit distance for common misreads, strict on short names.
- Temporal lock-on: the same species across several recent frames, then haptic feedback and a result sheet that opens
  the entry. Several names become choices. Unknown text keeps scanning quietly.
- A debug-only overlay of recognised text and match decisions.
- Matching behind an interface so an image model can later be added as a second signal.

## Capabilities

### New Capabilities
- `camera-name-recognition`: reading species names from a camera view and opening their entries.

### Modified Capabilities

## Non-goals

- LiteRT or any image-recognition model.
- Non-Latin text recognition.
- Google Play Services' unbundled recognizer.

## Impact

New `:feature:scanner` module. New dependencies: CameraX and bundled ML Kit Text Recognition, which grow the APK by the
Latin model and its native libraries. Adds the `CAMERA` permission.
