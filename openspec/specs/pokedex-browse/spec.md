# pokedex-browse Specification

## Purpose
Let people browse every species in Pokédex order and find one quickly by name or number, offline.

## Requirements

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

### Requirement: Search as you type
Results SHALL update as the query changes, matching name prefixes while ignoring case, accents and punctuation.

#### Scenario: Prefix updates as you type
- **WHEN** the query changes to "pika"
- **THEN** Pikachu is listed without submitting the query

#### Scenario: Accents ignored
- **WHEN** the query is "flabebe"
- **THEN** Flabébé is listed

#### Scenario: Punctuation ignored in search
- **WHEN** the query is "mr mime"
- **THEN** Mr. Mime is listed

#### Scenario: Best match listed first
- **WHEN** the query is "mew"
- **THEN** Mew is listed before Mewtwo

#### Scenario: Clearing restores the list
- **WHEN** the query is cleared
- **THEN** the full list is shown again

#### Scenario: No results shows empty state
- **WHEN** nothing matches the query
- **THEN** an empty state naming the query is shown

### Requirement: Search by Pokédex number
A query made of digits, with an optional leading "#", SHALL find species by number.

#### Scenario: Search by number
- **WHEN** the query is "25"
- **THEN** Pikachu is listed first

#### Scenario: Search by padded number
- **WHEN** the query is "#025"
- **THEN** only Pikachu is listed

### Requirement: Search in other languages
Search SHALL match species names in every bundled language, and SHALL show the matched name when it differs from the
displayed one.

#### Scenario: Search other language
- **WHEN** the query is "Bulbizarre"
- **THEN** Bulbasaur is listed with "Bulbizarre" shown as the matched name

#### Scenario: Search Japanese name
- **WHEN** the query is "ピカチュウ"
- **THEN** Pikachu is listed

### Requirement: Open an entry from the list
Tapping a list item SHALL open that species' full entry.

#### Scenario: Tapping opens the entry
- **WHEN** a species is tapped in the list
- **THEN** its entry opens

### Requirement: Accessible list
Each list item SHALL expose its number, name and types to screen readers as a single description.

#### Scenario: List item announces details
- **WHEN** a screen reader focuses a list item
- **THEN** it announces the number, name and types together
