# Spec Delta

## ADDED Requirements

### Requirement: Generation thumb in every home layout
The generation fast-scroll thumb SHALL work in every home layout: the sheet collapsed under the scanner, the sheet
expanded, the scanner switched off, and the list pane beside an entry on a wide window. Its track SHALL span only the
part of the list that is visible. Dragging it SHALL NOT move the sheet or close an entry open beside the list.

#### Scenario: Thumb in the collapsed sheet
- **WHEN** the scanner is on, the full list was scrolled into a later generation before the sheet was collapsed with Back, the collapsed list is scrolled back a little so the thumb shows, and the thumb is dragged to generation 9
- **THEN** the track and its ticks lie within the visible part of the list, above the bottom of the window, the list jumps to #0906 Sprigatito, and the sheet stays collapsed with the search field where it was

#### Scenario: Swiping up in the collapsed sheet expands it first
- **WHEN** the full list is swiped up with the sheet collapsed
- **THEN** the sheet expands as it does today, and the thumb shows only once the list itself scrolls

#### Scenario: Generation control in the collapsed sheet
- **WHEN** TalkBack adjusts the generation control with the scanner on and the sheet collapsed
- **THEN** the list jumps to that generation's first species and the sheet stays collapsed

#### Scenario: Thumb in the expanded sheet
- **WHEN** the thumb is dragged with the sheet expanded and no text typed
- **THEN** the track spans the visible list below the search field, the list jumps by generation, and the sheet stays expanded

#### Scenario: Thumb with the scanner off
- **WHEN** the thumb is dragged with the scanner switched off
- **THEN** the track spans the visible list below the search field and the list jumps by generation

#### Scenario: Thumb beside an open entry
- **WHEN** the thumb is dragged in the list pane while an entry is open beside it on a wide window
- **THEN** the track spans only the list pane's visible list, the list jumps by generation, and the entry beside it stays open showing the same species
