# Spec Delta

## MODIFIED Requirements

### Requirement: Adaptive layout
On wide windows the list and the entry SHALL show side by side, and on compact windows the entry SHALL replace the list
with a shared-element transition and predictive back. The back gesture SHALL play the same transition as the back arrow,
following the gesture's progress. An entry opened from the list SHALL show a Back arrow only while it fills the window
on its own; an entry opened from another entry SHALL always show one, returning to that species.

#### Scenario: Side by side on wide window
- **WHEN** a species is opened on a tablet or an unfolded foldable
- **THEN** the list stays visible beside the entry

#### Scenario: Replacing the entry on wide window
- **WHEN** a second species is tapped in the list on a wide window
- **THEN** the entry pane shows the new species and Back returns to the list state

#### Scenario: Back gesture matches the back arrow
- **WHEN** the user swipes back from an entry on a phone
- **THEN** the entry fades out at full size while its artwork moves back to the list row, without the entry shrinking

#### Scenario: No Back arrow on an entry beside the list
- **WHEN** a species entry opened from the home list shows beside it on a tablet or unfolded foldable
- **THEN** the entry pane shows no Back arrow

#### Scenario: Entry Back arrow on a phone
- **WHEN** the user opens a species entry on a phone in portrait and taps the entry's Back arrow
- **THEN** the arrow was shown, and the home list is shown again

#### Scenario: Entry Back arrow follows the window size
- **WHEN** the window changes size while a species entry is open, such as folding or unfolding a foldable or resizing the window
- **THEN** the entry's Back arrow appears once the entry fills the window on its own and disappears once it shows beside the list, and the same species stays open

#### Scenario: Back arrow to the previous species beside the list
- **WHEN** an entry opened from another entry's evolution line shows beside the home list on a tablet or unfolded foldable
- **THEN** it shows a Back arrow, and tapping it returns to the previous species with the list still beside it
