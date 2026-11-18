# Spec Delta

## MODIFIED Requirements

### Requirement: Licences layout
On a wide window the licences list and the opened library's licence SHALL show side by side, with a placeholder
before any library is opened. On a narrow window the licence SHALL replace the list, and Back SHALL return to it.
The licence SHALL show a Back arrow only while it fills the window on its own, never while it shows beside the list.

#### Scenario: Licence beside the list
- **WHEN** the user opens a library on a tablet or unfolded foldable
- **THEN** its licence shows beside the licences list, and opening another library replaces it

#### Scenario: Licence placeholder on a wide window
- **WHEN** the licences screen opens on a wide window before any library is chosen
- **THEN** a placeholder asking to pick a library is shown beside the list

#### Scenario: Back to the licences list on a phone
- **WHEN** the user opens a library on a phone and presses Back
- **THEN** the licences list is shown again

#### Scenario: Leaving the licences on a wide window
- **WHEN** a library's licence shows beside the list on a wide window and the user taps the list's Back control
- **THEN** settings is shown again in one step

#### Scenario: No Back arrow on a licence beside the list
- **WHEN** a library's licence shows beside the licences list on a tablet or unfolded foldable
- **THEN** the licence pane shows no Back arrow, and the list's Back control is still shown

#### Scenario: Licence Back arrow on a phone
- **WHEN** the user opens a library on a phone in portrait and taps the licence's Back arrow
- **THEN** the arrow was shown, and the licences list is shown again

#### Scenario: Licence Back arrow follows the window size
- **WHEN** the window changes size while a library's licence is open, such as folding or unfolding a foldable or resizing the window
- **THEN** the licence's Back arrow appears once the licence fills the window on its own and disappears once it shows beside the list, and the same library stays open
