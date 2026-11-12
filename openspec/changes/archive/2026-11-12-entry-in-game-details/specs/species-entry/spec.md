## MODIFIED Requirements

### Requirement: Full entry
The entry SHALL show the species' number, name, genus, types, height, weight, base stats, abilities including hidden
ones, egg groups, gender ratio and forms. It SHALL NOT list the species' names in other languages.

#### Scenario: Entry shows full data
- **WHEN** Bulbasaur's entry opens
- **THEN** it shows #0001, Seed Pokémon, Grass and Poison, 0.7 m, 6.9 kg, its six base stats, Overgrow and hidden Chlorophyll, its egg groups and its gender ratio

#### Scenario: Genderless species
- **WHEN** Magnemite's entry opens
- **THEN** the gender ratio is shown as genderless

#### Scenario: Unknown species
- **WHEN** an entry is opened for a number that does not exist
- **THEN** an error message is shown with a way back

#### Scenario: No names in other languages
- **WHEN** Bulbasaur's entry opens
- **THEN** no section lists its names in other languages

### Requirement: Adaptive layout
On wide windows the list and the entry SHALL show side by side, and on compact windows the entry SHALL replace the list
with a shared-element transition and predictive back. The back gesture SHALL play the same transition as the back arrow,
following the gesture's progress.

#### Scenario: Side by side on wide window
- **WHEN** a species is opened on a tablet or an unfolded foldable
- **THEN** the list stays visible beside the entry

#### Scenario: Replacing the entry on wide window
- **WHEN** a second species is tapped in the list on a wide window
- **THEN** the entry pane shows the new species and Back returns to the list state

#### Scenario: Back gesture matches the back arrow
- **WHEN** the user swipes back from an entry on a phone
- **THEN** the entry fades out at full size while its artwork moves back to the list row, without the entry shrinking

## ADDED Requirements

### Requirement: Short description
When downloaded Pokédex text exists for the species, the entry SHALL show one short description under its header: the
newest text in the display language, or in English when that language has none. Without downloaded text it SHALL show no
description.

#### Scenario: Description from downloaded text
- **WHEN** an entry opens and Pokédex text has been downloaded for the species
- **THEN** one description is shown under the types, and no list of entries per game

#### Scenario: No description without the download
- **WHEN** an entry opens and no Pokédex text has been downloaded
- **THEN** no description and no empty space are shown

### Requirement: Game picker
The entry SHALL have an "In game" section with a picker of games, one choice per game pair, listing only games with
level-up moves or wild locations for the selected variety. It SHALL start on the last game the user picked when that game
is listed, and otherwise on the newest listed game.

#### Scenario: Only games with data are offered
- **WHEN** the game picker for Sprigatito is opened
- **THEN** it lists Scarlet / Violet and no earlier games

#### Scenario: Picking a game updates the section
- **WHEN** the user picks another game
- **THEN** the moves and locations change to that game's

#### Scenario: Last picked game is remembered
- **WHEN** the user picked Sword / Shield on one entry and opens another species that is in Sword / Shield
- **THEN** that entry starts on Sword / Shield

#### Scenario: Newest game by default
- **WHEN** an entry opens and the last picked game has no data for this Pokémon, or no game was picked yet
- **THEN** the newest game with data is chosen

### Requirement: Level-up moves
For the chosen game, the section SHALL list the moves learnt by level up in level order, each with its level, name, type
and power as of that game. A variety without its own data, such as a Mega Evolution, SHALL use its species' default
variety.

#### Scenario: Moves learnt by level up
- **WHEN** Bulbasaur's entry is open with Scarlet / Violet chosen
- **THEN** its level-up moves are listed in level order, including Vine Whip with the Grass type

#### Scenario: Variety without its own moves
- **WHEN** Mega Venusaur is selected
- **THEN** the section shows Venusaur's moves and locations

#### Scenario: Variety with its own moves
- **WHEN** Alolan Vulpix is selected with Sun / Moon chosen
- **THEN** the section shows Alolan Vulpix's own level-up moves

### Requirement: Wild locations
For the chosen game, the section SHALL list where the Pokémon is found in the wild, each with how it is found, its level
range, and the game it is limited to when it is not in both games of the pair.

#### Scenario: Where to find
- **WHEN** Pikachu's entry is open with Red / Blue chosen
- **THEN** Viridian Forest is listed as a wild location with its level range

#### Scenario: Version-exclusive location
- **WHEN** a Pokémon is found in a location in only one game of the chosen pair
- **THEN** that location names the game it is limited to

#### Scenario: No location data for the game
- **WHEN** the chosen game has no wild location data at all, such as Scarlet / Violet
- **THEN** the section says location data is not available for that game

#### Scenario: Not found in the wild
- **WHEN** the chosen game has location data but none for this Pokémon
- **THEN** the section says it is not found in the wild in that game
