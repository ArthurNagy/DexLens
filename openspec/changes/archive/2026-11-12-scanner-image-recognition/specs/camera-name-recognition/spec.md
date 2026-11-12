## MODIFIED Requirements

### Requirement: Lock on across frames
A species SHALL be locked only after it appears in several recent frames, and unknown text SHALL keep scanning quietly.
A name candidate alone SHALL lock after three of the last five frames. Picture evidence SHALL lock as the
camera-image-recognition capability allows: sooner when it agrees with a name, later when it stands alone.

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
- **WHEN** only text that is not a species name is seen and no species reaches the image-only threshold
- **THEN** the list does not change and scanning continues

### Requirement: Camera controls and guidance
The scanner area SHALL show an aiming box, a hint to point at the Pokémon or its name and, while the camera is running on
a device with a flash, a torch toggle, and only the aiming box SHALL be analysed.

#### Scenario: Hint and aiming box shown
- **WHEN** the camera is running and nothing is matched
- **THEN** the aiming box and the hint "Point at a Pokémon or its name" are visible

#### Scenario: Torch toggles
- **WHEN** the torch button is tapped on a camera with a flash
- **THEN** the torch changes state and the button reflects it

#### Scenario: Torch returns with the camera
- **WHEN** the torch was on and the camera restarts after the sheet collapses
- **THEN** the torch is on again

#### Scenario: Crop follows the aiming box
- **WHEN** a frame is analysed in any device orientation
- **THEN** only the sensor region under the aiming box is sent to text recognition, and only an upright colour square centred in that region is sent to the picture signal

#### Scenario: Aiming box clear of the controls
- **WHEN** the camera is running in any window size
- **THEN** the aiming box is centred, does not overlap the status bar or the buttons, and is no more than half again as wide as it is tall

### Requirement: Works offline with on-device recognition
Recognition SHALL run entirely on the device with models bundled in the app, with no download and no network. This
covers both the text recogniser and the picture model.

#### Scenario: Recognition in airplane mode
- **WHEN** the device has no network on first launch
- **THEN** text recognition still returns results

#### Scenario: Picture recognition in airplane mode
- **WHEN** the device has no network on first launch and the aiming box shows a clear sprite
- **THEN** the picture signal still scores species
