## Purpose

Identify a species from its name on a game screen using the camera, entirely on the device.

## ADDED Requirements

### Requirement: Match recognised text to species names
Recognised text SHALL be matched against bundled species names after normalising case, accents, punctuation and gender
symbols. Exact matches SHALL be tried before fuzzy ones.

#### Scenario: Uppercase game name
- **WHEN** the recognised text is "PIKACHU"
- **THEN** Pikachu is a candidate

#### Scenario: Punctuation variants match
- **WHEN** the recognised text is "MR.MIME", "FARFETCH'D" or "Type: Null"
- **THEN** the corresponding species is a candidate

#### Scenario: Gender symbol selects the species
- **WHEN** the recognised text is "NIDORAN♀"
- **THEN** only Nidoran♀ is a candidate

#### Scenario: Missing gender symbol offers both
- **WHEN** the recognised text is "NIDORAN" with no readable symbol
- **THEN** Nidoran♀ and Nidoran♂ are both candidates

#### Scenario: Name inside a sentence
- **WHEN** the recognised text is "Wild PIDGEY appeared!"
- **THEN** Pidgey is a candidate

#### Scenario: Other Latin-script language
- **WHEN** the recognised text is "BULBIZARRE"
- **THEN** Bulbasaur is a candidate

### Requirement: Tolerate common misreads
Matching SHALL accept a bounded edit distance that treats 0/O, 1/I/L and 5/S as equal, and SHALL be stricter for short names.

#### Scenario: Digit misread as a letter
- **WHEN** the recognised text is "P1KACHU" or "5NORLAX"
- **THEN** the corresponding species is a candidate

#### Scenario: One wrong letter in a long name
- **WHEN** the recognised text is "BULBASAUB"
- **THEN** Bulbasaur is a candidate

#### Scenario: Short names stay strict
- **WHEN** the recognised text is "New", "Mud" or "Abro"
- **THEN** Mew, Muk and Abra are not candidates

#### Scenario: Ordinary game words ignored
- **WHEN** the recognised text is "FIGHT BAG POKéMON RUN" or "What will you do?"
- **THEN** there are no candidates

### Requirement: Lock on across frames
A species SHALL be locked only after it appears in several recent frames, and unknown text SHALL keep scanning quietly.

#### Scenario: Lock after repeated frames
- **WHEN** the same species is a candidate in three of the last five frames
- **THEN** it is locked

#### Scenario: Single frame does not lock
- **WHEN** a species is a candidate in one frame and absent from the next
- **THEN** nothing is locked

#### Scenario: Several names offered as choices
- **WHEN** two species both reach the lock threshold
- **THEN** both are offered as choices

#### Scenario: Nickname keeps scanning
- **WHEN** only text that is not a species name is seen
- **THEN** no result is shown and scanning continues

### Requirement: Result sheet
On lock the app SHALL give haptic feedback, pause analysis and show a sheet that opens the entry.

#### Scenario: Result opens the entry
- **WHEN** a species is locked and the user taps it in the sheet
- **THEN** its entry opens

#### Scenario: Analysis pauses during result
- **WHEN** the result sheet is showing
- **THEN** frames are not analysed until the sheet is dismissed

#### Scenario: Dismissing resumes scanning
- **WHEN** the result sheet is dismissed
- **THEN** scanning resumes with the lock history cleared

### Requirement: Camera permission
The screen SHALL explain why the camera is needed and SHALL handle denial without blocking the rest of the app.

#### Scenario: Rationale before asking
- **WHEN** the camera permission has not been granted
- **THEN** an explanation and a button to grant it are shown

#### Scenario: Denied permanently
- **WHEN** the permission is denied and the system will no longer ask
- **THEN** a button to open the app's settings is shown

### Requirement: Camera controls and guidance
The screen SHALL show an aiming box, a hint to point at the Pokémon's name and, when the camera has a flash, a torch
toggle, and only the aiming box SHALL be analysed.

#### Scenario: Hint and aiming box shown
- **WHEN** the camera is running
- **THEN** the aiming box and the hint "Point at the Pokémon's name" are visible

#### Scenario: Torch toggles
- **WHEN** the torch button is tapped on a camera with a flash
- **THEN** the torch changes state and the button reflects it

#### Scenario: Crop follows the aiming box
- **WHEN** a frame is analysed in any device orientation
- **THEN** only the sensor region under the aiming box is sent to recognition

### Requirement: Works offline with on-device recognition
Recognition SHALL run entirely on the device with a model bundled in the app, with no download and no network.

#### Scenario: Recognition in airplane mode
- **WHEN** the device has no network on first launch
- **THEN** text recognition still returns results
