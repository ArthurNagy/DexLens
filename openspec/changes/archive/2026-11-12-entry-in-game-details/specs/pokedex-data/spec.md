## MODIFIED Requirements

### Requirement: Bundled species coverage
The app SHALL bundle all 1,025 species with their Pokédex number, names in every available language, genus, types,
height, weight, base stats, abilities including hidden ones, evolution line with triggers, egg groups, gender ratio
and forms. It SHALL also bundle each variety's level-up learnset per game with the moves' names, types and power as of
that game, and the wild locations where each variety is found per game version, with the method and level range.

#### Scenario: All species present offline
- **WHEN** the app opens its database for the first time with no network
- **THEN** 1,025 species are available and each has at least one type, six base stats and one ability

#### Scenario: Names in every language
- **WHEN** a species is read
- **THEN** its name is available in each of the 12 languages PokeAPI provides species names for

#### Scenario: Tricky names preserved
- **WHEN** the names of Nidoran♀, Mr. Mime, Farfetch'd, Flabébé, Type: Null, Ho-Oh, Porygon-Z and Pecharunt are read
- **THEN** each matches its official spelling, including symbols, accents and punctuation

#### Scenario: Learnsets bundled
- **WHEN** the bundled data is checked
- **THEN** Bulbasaur learns Vine Whip by level up in every game from Red / Blue to Scarlet / Violet

#### Scenario: Move as of the game
- **WHEN** a move's type changed between games, such as Bite becoming Dark in Gold / Silver
- **THEN** the learnset for an earlier game shows the earlier type

#### Scenario: Locations bundled
- **WHEN** the bundled data is checked
- **THEN** Pikachu is found in Viridian Forest in Red and in Blue
