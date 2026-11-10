# app-settings Specification

## Purpose
Provide one simple place for the app's few options and the required credits.

## Requirements

### Requirement: Settings screen
The app SHALL provide a settings screen with the artwork download, a switch for the home screen scanner, theme choice, and credits and legal text.

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
