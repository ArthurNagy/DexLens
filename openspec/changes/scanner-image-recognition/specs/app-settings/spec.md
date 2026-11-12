## MODIFIED Requirements

### Requirement: Settings screen
The app SHALL provide a settings screen with the artwork download, a switch for the home screen scanner, theme choice, and credits and legal text. Debug builds SHALL also show a developer switch for the recognition scores readout.

#### Scenario: Download from settings
- **WHEN** settings opens and the content has not been downloaded
- **THEN** a download action with the size is shown

#### Scenario: Scanner switch applies
- **WHEN** the user changes the scanner switch
- **THEN** the home screen follows it and the app remembers the choice after a restart

#### Scenario: Theme choice applies
- **WHEN** the user picks a theme
- **THEN** the app switches to it and remembers it

#### Scenario: Credits shown
- **WHEN** settings opens
- **THEN** PokeAPI is credited and the unofficial fan project notice is shown

#### Scenario: Scores switch in debug builds
- **WHEN** settings opens in a debug build
- **THEN** a "Show recognition scores" switch is shown, off until the user turns it on, and the app remembers the choice after a restart

#### Scenario: No scores switch in release builds
- **WHEN** settings opens in a release build
- **THEN** no recognition scores switch is shown
