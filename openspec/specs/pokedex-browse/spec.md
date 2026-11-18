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

### Requirement: Jump through the list by generation
While the full list in Pokédex order is showing, scrolling it SHALL reveal a fast-scroll thumb on the list's end edge
that steps by generation. Dragging it SHALL jump the list to each generation's first species, taken from the bundled
data. The thumb SHALL show only while the list is scrolled or dragged, and SHALL draw nothing while the list is still.

#### Scenario: Thumb appears while scrolling
- **WHEN** the full list in Pokédex order is scrolled
- **THEN** a fast-scroll thumb fades in on the list's right edge in a left-to-right language

#### Scenario: Thumb on the left in right-to-left languages
- **WHEN** the full list is scrolled while the layout is right-to-left
- **THEN** the thumb fades in on the list's left edge, and the generation bubble shows on its right

#### Scenario: Thumb fades after scrolling stops
- **WHEN** scrolling and dragging have both stopped for about 1.5 seconds
- **THEN** the thumb has faded out, and no thumb, track, tick or bubble is drawn

#### Scenario: Still list keeps full-width rows
- **WHEN** the full list is showing and has not been scrolled
- **THEN** no thumb or track is drawn, the rows span the list's full width, and tapping a row at its end edge opens that species' entry

#### Scenario: Dragging jumps to each generation's first species
- **WHEN** the thumb is dragged to generation 1, then 4, then 9
- **THEN** the thumb snaps to each generation in turn, and the list's top row becomes #0001 Bulbasaur, then #0387 Turtwig, then #0906 Sprigatito

#### Scenario: Bubble and ticks while dragging
- **WHEN** the thumb is held on generation 4
- **THEN** a bubble on the list side of the finger reads "Generation 4 · Sinnoh", and tick marks labelled 1 to 9 show along the track

#### Scenario: Lifting the finger clears the bubble
- **WHEN** the finger is lifted after dragging the thumb
- **THEN** the bubble and the tick marks disappear, and the thumb fades out about 1.5 seconds later

#### Scenario: Thumb follows hand scrolling
- **WHEN** the list is scrolled by hand until a generation 5 species is the top row
- **THEN** the thumb sits at generation 5's position on the track

#### Scenario: No thumb over search results
- **WHEN** text is typed and the list is scrolled
- **THEN** no thumb appears and no generation control is available

#### Scenario: No thumb over a scan match
- **WHEN** a scan match is listed
- **THEN** no thumb appears and no generation control is available

#### Scenario: No thumb while loading, failed or empty
- **WHEN** the list is loading, has failed to load, or shows no results
- **THEN** no thumb appears and no generation control is available

#### Scenario: Thumb returns with the full list
- **WHEN** typed text is cleared and the full list shows again
- **THEN** scrolling the list shows the thumb again

### Requirement: Generation control for screen readers
While the full list in Pokédex order is showing, a screen reader SHALL reach a generation control after the search field
and before the first list row, announced as an adjustable slider whose value is the current generation and its region.
Adjusting it SHALL jump the list by one generation. The control SHALL be available even while the visual thumb is hidden.

#### Scenario: TalkBack reaches the generation control
- **WHEN** TalkBack moves on from the search field while the full list is at its top
- **THEN** before the first list row it reaches a control announced as a slider with the value "Generation 1, Kanto"

#### Scenario: Adjusting the control jumps a generation
- **WHEN** the generation control is adjusted up three times from generation 1 with TalkBack's swipe gesture
- **THEN** each step moves one generation and jumps the list to that generation's first species, and the last step announces "Generation 4, Sinnoh" with #0387 Turtwig as the top row

#### Scenario: Adjusting the control down a generation
- **WHEN** the generation control is adjusted down once from generation 4 with TalkBack's swipe gesture
- **THEN** the list jumps to #0252 Treecko as the top row and TalkBack announces "Generation 3, Hoenn"

#### Scenario: Control available while the thumb is hidden
- **WHEN** the full list is showing and has not been scrolled, so no thumb is drawn
- **THEN** the generation control is still exposed to TalkBack with the current generation

#### Scenario: Control follows the list
- **WHEN** the list is scrolled by hand until a generation 7 species is the top row
- **THEN** the generation control's value is "Generation 7, Alola"
