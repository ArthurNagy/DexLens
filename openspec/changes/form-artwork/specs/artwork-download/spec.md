## REMOVED Requirements

### Requirement: One picture per species
**Reason**: Replaced by "One picture per variety", so the entry can show alternate varieties such as Megas.
**Migration**: None. A species' default variety shares its id, so every species picture keeps its name and path.

## ADDED Requirements

### Requirement: One picture per variety
The download SHALL fetch one picture for each variety, and every screen that shows a species SHALL use its default
variety's picture. A variety without official artwork SHALL be skipped without stopping the download.

#### Scenario: One picture for each variety
- **WHEN** the download runs
- **THEN** one picture is fetched and stored for each variety, including alternate varieties such as Mega Charizard X

#### Scenario: Species screens use the default variety
- **WHEN** the list, the scanner or the evolution tree asks for Charizard's picture by its species number
- **THEN** it gets the picture stored for Charizard's default variety

#### Scenario: Variety without official artwork is skipped
- **WHEN** the download reaches a variety that has no official artwork
- **THEN** it is skipped, the rest of the download continues, and the run finishes
