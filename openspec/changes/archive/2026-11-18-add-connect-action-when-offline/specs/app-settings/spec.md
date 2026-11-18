## MODIFIED Requirements

### Requirement: Settings screen
The app SHALL provide a settings screen with the artwork download, a switch for the home screen scanner, theme choice, and credits and legal text.
When the device has no internet connection, the artwork download SHALL say so and offer to connect to a network in
place of Download or Resume, and SHALL bring Download or Resume back as soon as the device is connected.

#### Scenario: Download from settings
- **WHEN** settings opens and the content has not been downloaded
- **THEN** a download action with the size is shown

#### Scenario: Download from settings while offline
- **WHEN** settings opens with no internet connection and the content has not been downloaded
- **THEN** the download section shows the size, says there is no connection, and offers "Connect to a network" with no Download button

#### Scenario: Resume from settings while offline
- **WHEN** settings opens with no internet connection and the download was interrupted
- **THEN** the download section shows the progress so far, says there is no connection, and offers "Connect to a network" with no Resume button, while "Delete downloaded content" is still offered

#### Scenario: Settings download returns when connected
- **WHEN** the device gets an internet connection while settings shows "Connect to a network"
- **THEN** Download or Resume comes back without leaving settings, and goes away again if the connection drops before it is tapped

#### Scenario: Connect from settings
- **WHEN** the user taps "Connect to a network" in settings
- **THEN** Android's internet connectivity panel opens over DexLens, or the device's network settings when the panel can't open, or a short message saying network settings can't be opened when neither can, and the app does not crash

#### Scenario: Scanner switch applies
- **WHEN** the user changes the scanner switch
- **THEN** the home screen follows it and the app remembers the choice after a restart

#### Scenario: Theme choice applies
- **WHEN** the user picks a theme
- **THEN** the app switches to it and remembers it

#### Scenario: Credits shown
- **WHEN** settings opens
- **THEN** PokeAPI (BSD-3-Clause), ML Kit text recognition, LiteRT and the MobileNetV4 image model are credited, DexLens is said to be open source under the MIT License, and the unofficial fan project notice is shown
