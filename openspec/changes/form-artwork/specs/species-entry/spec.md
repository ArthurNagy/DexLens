## MODIFIED Requirements

### Requirement: Forms
The entry SHALL list alternate varieties and SHALL show the selected variety's picture, types, stats, abilities and
measurements.

#### Scenario: Selecting a variety updates the entry
- **WHEN** Mega Charizard X is selected on Charizard's entry
- **THEN** its Fire and Dragon types and its stats are shown

#### Scenario: Selecting a variety shows its picture
- **WHEN** Mega Charizard X is selected on Charizard's entry and the artwork has been downloaded
- **THEN** the header shows Mega Charizard X's picture

#### Scenario: Variety without its own picture
- **WHEN** a variety with no official artwork is selected
- **THEN** the header shows the species picture
