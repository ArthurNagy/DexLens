# camera-name-recognition Specification

## Purpose
Identify a species from its name on a game screen using the camera, entirely on the device.

## Requirements

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
- **THEN** both are listed in Pokédex order

#### Scenario: Nickname keeps scanning
- **WHEN** only text that is not a species name is seen
- **THEN** the list does not change and scanning continues

### Requirement: Camera permission
While the scanner is switched on, the scanner area SHALL explain why the camera is needed and SHALL handle denial without blocking the rest of the app.

#### Scenario: Rationale before asking
- **WHEN** the camera permission has not been granted
- **THEN** an explanation and a button to grant it are shown in the scanner area

#### Scenario: Denied permanently
- **WHEN** the permission is denied and the system will no longer ask
- **THEN** a button to open the app's settings is shown in the scanner area

#### Scenario: System dialog waits for a tap
- **WHEN** the app opens without the camera permission
- **THEN** the system permission dialog does not appear until the user taps the button

#### Scenario: Search works without the camera
- **WHEN** the camera permission is missing or the device has no usable camera
- **THEN** the list and search work as usual, and the scanner area says why it is empty

### Requirement: Camera controls and guidance
The scanner area SHALL show an aiming box, a hint to point at the Pokémon's name and, while the camera is running on a device with a flash, a torch toggle, and only the aiming box SHALL be analysed.

#### Scenario: Hint and aiming box shown
- **WHEN** the camera is running and nothing is matched
- **THEN** the aiming box and the hint "Point at the Pokémon's name" are visible

#### Scenario: Torch toggles
- **WHEN** the torch button is tapped on a camera with a flash
- **THEN** the torch changes state and the button reflects it

#### Scenario: Torch returns with the camera
- **WHEN** the torch was on and the camera restarts after the sheet collapses
- **THEN** the torch is on again

#### Scenario: Crop follows the aiming box
- **WHEN** a frame is analysed in any device orientation
- **THEN** only the sensor region under the aiming box is sent to recognition

#### Scenario: Aiming box clear of the controls
- **WHEN** the camera is running in any window size
- **THEN** the aiming box is centred, does not overlap the status bar or the buttons, and is no more than half again as wide as it is tall

### Requirement: Works offline with on-device recognition
Recognition SHALL run entirely on the device with a model bundled in the app, with no download and no network.

#### Scenario: Recognition in airplane mode
- **WHEN** the device has no network on first launch
- **THEN** text recognition still returns results

### Requirement: Matches shown in the home list
On lock the app SHALL give haptic feedback, list the locked species in the home list in place of the full list, and name them in the aiming box.

#### Scenario: Match filters the list
- **WHEN** a species is locked
- **THEN** the phone vibrates and the home list shows only the locked species

#### Scenario: Match named in the aiming box
- **WHEN** a species is locked
- **THEN** its name replaces the hint in the aiming box and screen readers announce it

#### Scenario: Match opens the entry
- **WHEN** a species is locked and the user taps it in the list
- **THEN** its entry opens

### Requirement: Scanning continues after a match
Analysis SHALL keep running after a lock. A match SHALL stay listed until a lock adds a species it does not include, or the user clears or replaces it.

#### Scenario: Analysis continues during a match
- **WHEN** a match is listed and the camera is running
- **THEN** frames are still analysed

#### Scenario: Different name replaces the match
- **WHEN** a match is listed and a species outside it locks
- **THEN** the list shows the new lock and the phone vibrates

#### Scenario: Same name changes nothing
- **WHEN** a match is listed and a lock contains only species already listed
- **THEN** the list does not change and the phone does not vibrate

#### Scenario: Match stays when the name leaves
- **WHEN** a match is listed and its name is no longer in the camera's view
- **THEN** the match stays listed

### Requirement: Clearing or replacing a match
The user SHALL be able to return from a match to the full list, and typed text SHALL always take over from a match.

#### Scenario: Clearing a match restores the list
- **WHEN** the clear button in the search field is tapped while a match is listed
- **THEN** the full list is shown and the hint returns to the aiming box

#### Scenario: Back clears a match
- **WHEN** Back is pressed while a match is listed, the sheet is collapsed and no entry is open beside it
- **THEN** the full list is shown and the app stays open

#### Scenario: Typing replaces a match
- **WHEN** the user types in the search field while a match is listed
- **THEN** the match is cleared and the list answers the typed text

#### Scenario: Match kept when the sheet is dragged
- **WHEN** a match is listed and the sheet is dragged up and back down without typing
- **THEN** the match is still listed
