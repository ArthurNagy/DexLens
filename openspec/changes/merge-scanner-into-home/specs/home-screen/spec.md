# Spec Delta

## Purpose

Put the camera scanner and the species list on one home screen, so people can scan a game or search by typing without
changing screens, and both ways of finding a Pokémon fill the same list.

## ADDED Requirements

### Requirement: Scanner above a list sheet
The home screen SHALL show the camera scanner area at the top and a sheet below it holding the search field and the
species list. The scanner area SHALL be 3:2 below the status bar when the window has room for it.

#### Scenario: Home shows scanner and sheet
- **WHEN** the home screen opens on a phone in portrait
- **THEN** the scanner area fills the top at 3:2 and the sheet with the search field and the list fills the rest

#### Scenario: No separate scan step
- **WHEN** the home screen is showing
- **THEN** there is no Scan button and scanning does not open another screen

#### Scenario: Short window keeps the list reachable
- **WHEN** the window is too short for a 3:2 scanner area, such as a phone in landscape
- **THEN** the scanner area takes at most 45% of the window height and the search field stays visible

#### Scenario: Wide window keeps list and entry together
- **WHEN** the window is wide enough to show the list beside an entry
- **THEN** the scanner area and the sheet share the list pane and the entry opens beside them

### Requirement: Settings on the scanner area
The settings button SHALL sit at the top end of the scanner area and SHALL stay visible in both sheet positions.

#### Scenario: Settings opens from the scanner area
- **WHEN** the settings button is tapped
- **THEN** settings opens

#### Scenario: Settings stays while searching
- **WHEN** the sheet is expanded
- **THEN** the settings button is still visible above the sheet

### Requirement: Sheet expands for manual search
Focusing the search field or dragging the sheet up SHALL expand the sheet over the scanner area, leaving a thin strip above it.

#### Scenario: Tapping search expands the sheet
- **WHEN** the search field is tapped
- **THEN** the sheet expands and the keyboard opens

#### Scenario: Dragging up expands the sheet
- **WHEN** the sheet is dragged up
- **THEN** the sheet expands and the keyboard stays closed

### Requirement: Camera runs only while it can be seen
The camera SHALL be fully off whenever the sheet is expanded or the scanner area is not on screen, and SHALL run
otherwise once the camera permission has been granted.

#### Scenario: Expanding stops the camera
- **WHEN** the sheet expands
- **THEN** the camera turns off, the strip above the sheet is a plain dark bar and the torch button is hidden

#### Scenario: Collapsing restarts the camera
- **WHEN** the sheet returns to its collapsed position
- **THEN** the camera turns back on and scanning resumes

#### Scenario: Covering home stops the camera
- **WHEN** an entry or settings covers the home screen
- **THEN** the camera is off until the home screen shows again

#### Scenario: Entry beside the list keeps scanning
- **WHEN** an entry opens beside the list on a wide window
- **THEN** the camera keeps running in the list pane

### Requirement: Collapsing ends a manual search
Collapsing the sheet SHALL clear the typed text and close the keyboard. Back SHALL collapse an expanded sheet before it
does anything else on the home screen.

#### Scenario: Collapsing clears the typed text
- **WHEN** text has been typed and the sheet is dragged down to its collapsed position
- **THEN** the text is cleared, the keyboard closes and the full list is shown

#### Scenario: Back collapses the expanded sheet
- **WHEN** Back is pressed while the sheet is expanded and no entry is open beside it
- **THEN** the sheet collapses and the app stays open

#### Scenario: Clearing text keeps the sheet expanded
- **WHEN** the clear button is tapped while typed text is showing
- **THEN** the text is cleared, the full list is shown and the sheet stays expanded

#### Scenario: Returning from an entry keeps the search
- **WHEN** an entry opened from typed results on a phone is closed
- **THEN** the typed text, its results and the expanded sheet are as they were
