# Spec Delta

## MODIFIED Requirements

### Requirement: Settings screen
The app SHALL provide a settings screen with the artwork download, a switch for the home screen scanner, theme choice, and credits and legal text.

#### Scenario: Download from settings
- **WHEN** settings opens and the content has not been downloaded
- **THEN** a download action with the size is shown

#### Scenario: Scanner switch applies
- **WHEN** the user changes the scanner switch
- **THEN** the home screen follows it and the app remembers the choice after a restart

#### Scenario: Theme choice applies
- **WHEN** the user picks a theme
- **THEN** the app switches to it and remembers it

#### Scenario: Credits shown
- **WHEN** settings opens
- **THEN** PokeAPI (BSD-3-Clause), ML Kit text recognition, LiteRT and the MobileNetV4 image model are credited, DexLens is said to be open source under the MIT License, and the unofficial fan project notice is shown

## ADDED Requirements

### Requirement: App version shown
The settings screen SHALL show the installed app's version name, so a user reporting a problem can say which version
they run. When the version can't be read, no version line SHALL be shown.

#### Scenario: Version shown
- **WHEN** settings opens
- **THEN** the About section shows the installed version, for example "Version 0.1.0", matching the build's version name

#### Scenario: Version unavailable
- **WHEN** settings opens and the installed version can't be read
- **THEN** the About section shows no version line and everything else in it as usual

### Requirement: Open-source licences entry
The settings screen SHALL offer an "Open-source licences" entry in its About section that opens the open-source
licences screen.

#### Scenario: Licences opened from settings
- **WHEN** the user taps "Open-source licences" in settings
- **THEN** the open-source licences screen opens

### Requirement: Source code link
The settings screen SHALL offer a "Source code" entry that hands https://github.com/ArthurNagy/DexLens to another app
on the device. DexLens itself SHALL fetch nothing, and SHALL show a short message when no app can open the link.

#### Scenario: Source code opened in the browser
- **WHEN** the user taps "Source code"
- **THEN** the device's browser or app chooser opens https://github.com/ArthurNagy/DexLens and DexLens makes no network request

#### Scenario: No app for the source link
- **WHEN** the user taps "Source code" and no installed app can open web links
- **THEN** DexLens shows a short message saying the link can't be opened, and does not crash
