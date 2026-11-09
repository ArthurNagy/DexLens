## Purpose

Let people optionally add artwork and flavor text after install, and keep everything working with or without it.

## ADDED Requirements

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
- **THEN** entries show flavor text labelled with its game, without line-break artifacts

### Requirement: Works offline afterwards
Downloaded content SHALL be used from local storage only.

#### Scenario: Art offline after download
- **WHEN** the download has finished and the device goes offline
- **THEN** artwork and flavor text are still shown

### Requirement: Placeholders without content
Without downloaded content, the app SHALL show type-tinted placeholders and omit flavor text, with no broken UI.

#### Scenario: Entry without flavor text
- **WHEN** an entry opens and flavor text has not been downloaded
- **THEN** the flavor text section is absent

### Requirement: Remove downloaded content
The user SHALL be able to delete downloaded content.

#### Scenario: Delete frees content
- **WHEN** the user deletes the downloaded content
- **THEN** placeholders return and the download can be started again
