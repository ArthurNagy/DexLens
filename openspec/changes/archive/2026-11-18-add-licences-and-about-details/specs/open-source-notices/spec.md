# Spec Delta

## Purpose

Show every third-party library bundled in the app with its licence and notice texts, inside the app and without a
network connection, so the app meets its open-source licence obligations.

## ADDED Requirements

### Requirement: Library list
The app SHALL provide an open-source licences screen, reached from Settings, that lists every third-party library
bundled in the installed build, each with its name, version and licence names, ordered by name.

#### Scenario: Libraries listed with their licences
- **WHEN** the user opens the open-source licences screen
- **THEN** each bundled library is listed in name order with its name, version and the names of its licences

#### Scenario: Licences listed offline
- **WHEN** the device has no network connection and the user opens the open-source licences screen
- **THEN** the full list is shown and the app makes no network request

### Requirement: Library licence text
Opening a library SHALL show its copyright holder when known, the full text of each of its licences, and the full
text of any notices bundled with that library, without any network request. A licence template's placeholder
copyright line SHALL never be shown as if it were the holder.

#### Scenario: Licence text shown
- **WHEN** the user opens a library from the licences list
- **THEN** the library's name, version and the full text of each of its licences are shown

#### Scenario: Copyright holder shown
- **WHEN** the user opens a library whose copyright holder is known
- **THEN** the holder is shown with the licence, and no `<year> <owner>` placeholder line is shown

#### Scenario: Bundled notices shown
- **WHEN** the user opens a library that ships its own third-party notices, such as ML Kit text recognition or LiteRT
- **THEN** those notices are shown in full after its licence texts

#### Scenario: Licence text offline
- **WHEN** the device has no network connection and the user opens a library
- **THEN** its full licence text is shown and the app makes no network request

#### Scenario: Unknown library
- **WHEN** a library that is not bundled is requested, for example from a saved back stack after an update removed it
- **THEN** a short message says the library can't be found, instead of an empty screen or a crash

### Requirement: Licences layout
On a wide window the licences list and the opened library's licence SHALL show side by side, with a placeholder
before any library is opened. On a narrow window the licence SHALL replace the list, and Back SHALL return to it.

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
