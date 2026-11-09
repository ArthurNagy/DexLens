## MODIFIED Requirements

### Requirement: Browsable Pokédex list
The home screen SHALL show all species in Pokédex order, each with its art or placeholder, number, name and type chips.

#### Scenario: List in Pokédex order
- **WHEN** the home screen loads with an empty search
- **THEN** all 1,025 species are listed from #0001 onward with number, name and types

#### Scenario: Shows loading state
- **WHEN** the list has not finished loading
- **THEN** a shimmering skeleton of the list is shown instead of an empty list

#### Scenario: Shows error state
- **WHEN** the list cannot be read
- **THEN** a message and a retry action are shown
