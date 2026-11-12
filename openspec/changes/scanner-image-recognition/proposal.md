Issue: none

## Why

The scanner only reads names. Most game screens show a name, but many don't: the overworld, a Pokémon walking behind
the player, the box, a sprite with its name cut off or written in a font OCR can't read. The image model and its
reference vectors have been in the app's assets since PR #2, and nothing loads them yet. Using both signals together
also helps when the scanner can see a name: the picture can decide between names the text can't tell apart, such as
"NIDORAN" without its symbol.

## What Changes

- Run the bundled image model on every analysed frame, next to text recognition, on a square colour crop of the aiming
  box. It works offline from first launch, like the text recogniser.
- Combine the two signals into one decision. Text counts for more:
  - a name alone locks as it does today, after 3 of the last 5 frames;
  - a name and the picture that agree lock after 2 frames;
  - the picture decides between names the text can't separate (a missing gender symbol, a misread that ties two names);
  - text decides between pictures that score close together;
  - the picture alone locks only with a high score and 4 of the last 5 frames, and lists up to 3 species when no single
    one clearly leads;
  - when the name and the picture disagree, the name wins.
- Change the aiming box hint from "Point at the Pokémon's name" to "Point at a Pokémon or its name".
- Add a developer setting, shown only in debug builds and off by default, that shows the image model's top 3 species
  and scores in the scanner area, so the score cut-offs can be tuned on real game screens.
- If the image model can't load, the scanner keeps working on text alone.
- Update docs/MVP_SPEC.md: recognising Pokémon from images moves from out of scope into v1, and the note on official art
  says the model files hold numbers derived from pictures, not pictures.
- New dependency: LiteRT (`com.google.ai.edge.litert:litert`), bundled in the APK, CPU only. Recorded as ADR 0014.

## Capabilities

### New Capabilities
- `camera-image-recognition`: recognising a species from what it looks like in the aiming box, how that signal is combined
  with the name signal, and the developer readout of its scores.

### Modified Capabilities
- `camera-name-recognition`: lock-on now counts evidence from both signals, the hint mentions the Pokémon as well as its
  name, the crop sends a colour square to the image model, and offline recognition covers both models.
- `app-settings`: the settings screen gains the debug-only switch for the recognition scores.

## Non-goals

- Telling forms apart (Mega, Gigantamax, regional forms). The references are per species, so a match lists the species.
- Finding several Pokémon in one frame, or where in the frame a Pokémon is. The model sees one square crop.
- Retraining the model or changing the two asset files. A retrained model ships later through its own `chore` PR.
- The GPU or NPU. The shipped model file fails on the GPU delegate and the GPU was not meaningfully faster.
- Moving the aiming box or changing its size and shape.
- Tuning the final score cut-offs. This change ships starting values and the readout to tune them; the tuned values follow
  on device testing.

## Impact

- `:core:domain` (`scan` package): a reference gallery that parses `dexlens_references.bin` and ranks species, an image
  score type, and a combined lock-on that weighs text and image evidence. `LockOnTracker` and `ScanState.afterFrame`
  change to take per-species evidence instead of a plain set.
- `:core:datastore`, `:core:domain`, `:core:data`: one more boolean preference (recognition scores shown), with observe and
  set use cases.
- `:feature:pokedex:impl` (`scanner` package): a colour crop from the YUV frame, a LiteRT encoder behind an interface, the
  analyzer running both signals, the readout in the scanner area and the new hint string. New `litert` dependency.
- `:feature:settings:impl`: the debug-only switch.
- APK size grows by the LiteRT runtime's native library (the two model files are already in the APK).
- Each analysed frame takes longer, by about 26 ms on a Pixel 6 plus the crop. Older frames are still dropped rather than
  queued.
