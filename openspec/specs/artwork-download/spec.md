# artwork-download Specification

## Purpose
Let people optionally add artwork and flavor text after install, and keep everything working with or without it.

## Requirements

### Requirement: First-launch offer
On first launch the app SHALL offer the download once with its size. Declining SHALL not affect any other feature.

#### Scenario: Offer on first launch
- **WHEN** the app starts for the first time
- **THEN** the offer is shown with the download size and the space it uses

#### Scenario: Skip is remembered
- **WHEN** the user skips the offer and restarts the app
- **THEN** the offer is not shown again

#### Scenario: Works after skipping
- **WHEN** the user skips the offer
- **THEN** the list, search, entries and camera work with placeholders

### Requirement: Download with progress
The download SHALL show progress, use limited concurrency, and continue when the screen is left.

#### Scenario: Progress shown
- **WHEN** the download is running
- **THEN** the number of completed items out of the total is visible

#### Scenario: Only the expected host
- **WHEN** the download runs
- **THEN** all requests go to raw.githubusercontent.com

#### Scenario: Limited concurrency
- **WHEN** the download runs
- **THEN** at most the configured number of files are fetched at the same time

### Requirement: Resumable
An interrupted download SHALL continue from where it stopped without fetching completed items again.

#### Scenario: Resume skips finished files
- **WHEN** a download restarts after some files were saved
- **THEN** only the missing files are fetched

#### Scenario: Failure can be retried
- **WHEN** the connection fails during the download
- **THEN** finished items are kept and the download can be resumed

#### Scenario: Partial files are never used
- **WHEN** a file is interrupted while being written
- **THEN** it is not shown as artwork and is fetched again later

### Requirement: Stored format
Artwork SHALL be stored as WebP at about 384 pixels.

#### Scenario: Artwork is scaled and converted
- **WHEN** a piece of official artwork is saved
- **THEN** it is stored as a WebP no larger than 384 pixels on its long side

#### Scenario: Flavor text is imported
- **WHEN** the flavor text table has been downloaded
- **THEN** each entry shows its newest flavor text as its description, without line-break artifacts

### Requirement: Works offline afterwards
Downloaded content SHALL be used from local storage only.

#### Scenario: Art offline after download
- **WHEN** the download has finished and the device goes offline
- **THEN** artwork and flavor text are still shown

### Requirement: Placeholders without content
Without downloaded content, the app SHALL show type-tinted placeholders and omit flavor text, with no broken UI.

#### Scenario: Entry without flavor text
- **WHEN** an entry opens and flavor text has not been downloaded
- **THEN** the entry has no description

### Requirement: Remove downloaded content
The user SHALL be able to delete downloaded content.

#### Scenario: Delete frees content
- **WHEN** the user deletes the downloaded content
- **THEN** placeholders return and the download can be started again

### Requirement: Content appears without reopening
Downloaded art and flavor text SHALL appear on open screens as soon as they are available.

#### Scenario: Art appears when the download finishes
- **WHEN** the download finishes while the list or an entry is open
- **THEN** placeholders are replaced by artwork without reopening the screen

#### Scenario: Flavor text appears in an open entry
- **WHEN** flavor text finishes importing while an entry is open
- **THEN** the description appears without reopening the entry

### Requirement: One picture per variety
The download SHALL fetch one picture for each variety, and every screen that shows a species SHALL use its default
variety's picture. A variety without official artwork SHALL be skipped without stopping the download.

#### Scenario: One picture for each variety
- **WHEN** the download runs
- **THEN** one picture is fetched and stored for each variety, including alternate varieties such as Mega Charizard X

#### Scenario: Species screens use the default variety
- **WHEN** the list, the scanner or the evolution tree asks for Charizard's picture by its species number
- **THEN** it gets the picture stored for Charizard's default variety

#### Scenario: Variety without official artwork is skipped
- **WHEN** the download reaches a variety that has no official artwork
- **THEN** it is skipped, the rest of the download continues, and the run finishes
