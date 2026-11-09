## ADDED Requirements

### Requirement: One picture per species
The download SHALL fetch one picture for each species, and every screen that shows a species SHALL use that picture.

#### Scenario: One picture for each species
- **WHEN** the download runs
- **THEN** one picture is fetched and stored for each species

### Requirement: Content appears without reopening
Downloaded art and flavor text SHALL appear on open screens as soon as they are available.

#### Scenario: Art appears when the download finishes
- **WHEN** the download finishes while the list or an entry is open
- **THEN** placeholders are replaced by artwork without reopening the screen

#### Scenario: Flavor text appears in an open entry
- **WHEN** flavor text finishes importing while an entry is open
- **THEN** the flavor text section appears without reopening the entry
