# species-entry Specification

## Purpose
Show everything the app knows about one species on a single screen, adapted to the window size.

## Requirements

### Requirement: Full entry
The entry SHALL show the species' number, name, genus, types, height, weight, base stats, abilities including hidden
ones, egg groups, gender ratio, forms and names in other languages.

#### Scenario: Entry shows full data
- **WHEN** Bulbasaur's entry opens
- **THEN** it shows #0001, Seed Pokémon, Grass and Poison, 0.7 m, 6.9 kg, its six base stats, Overgrow and hidden Chlorophyll, its egg groups and its gender ratio

#### Scenario: Genderless species
- **WHEN** Magnemite's entry opens
- **THEN** the gender ratio is shown as genderless

#### Scenario: Unknown species
- **WHEN** an entry is opened for a number that does not exist
- **THEN** an error message is shown with a way back

### Requirement: Forms
The entry SHALL list alternate varieties and SHALL show the selected variety's types, stats, abilities and measurements.

#### Scenario: Selecting a variety updates the entry
- **WHEN** Mega Charizard X is selected on Charizard's entry
- **THEN** its Fire and Dragon types and its stats are shown

### Requirement: Computed type matchups
The entry SHALL show weaknesses, resistances and immunities computed on the device from the type chart.

#### Scenario: Dual type multiplies
- **WHEN** matchups are computed for Grass and Poison
- **THEN** Fire is 2×, Psychic is 2×, Grass is ¼× and Water is ½×

#### Scenario: Four times weakness
- **WHEN** matchups are computed for Bug and Steel
- **THEN** Fire is 4×

#### Scenario: Immunity
- **WHEN** matchups are computed for Ghost and Poison
- **THEN** Normal and Fighting are immunities

### Requirement: Evolution line
The entry SHALL show the species' evolution line with how each step is triggered, and SHALL open another species' entry
when tapped.

#### Scenario: Evolution line with triggers
- **WHEN** Ivysaur's entry opens
- **THEN** Bulbasaur, Ivysaur and Venusaur are shown, with level 16 and level 32 as the triggers

#### Scenario: Branching evolution
- **WHEN** Eevee's entry opens
- **THEN** every Eevee evolution is shown with its own trigger

#### Scenario: Tapping evolution opens entry
- **WHEN** a species in the evolution line is tapped
- **THEN** that species' entry opens

### Requirement: Adaptive layout
On wide windows the list and the entry SHALL show side by side, and on compact windows the entry SHALL replace the list
with a shared-element transition and predictive back.

#### Scenario: Side by side on wide window
- **WHEN** a species is opened on a tablet or an unfolded foldable
- **THEN** the list stays visible beside the entry

#### Scenario: Replacing the entry on wide window
- **WHEN** a second species is tapped in the list on a wide window
- **THEN** the entry pane shows the new species and Back returns to the list state

### Requirement: Placeholder art
Without downloaded art, the list and the entry SHALL show type-tinted placeholders.

#### Scenario: Placeholder shown without art
- **WHEN** a species has no downloaded art
- **THEN** a placeholder tinted with its types is shown and no network call is made
