# artwork-download Specification

## Purpose
Let people optionally add artwork and flavor text after install, and keep everything working with or without it.

## Requirements

### Requirement: First-launch offer
On first launch the app SHALL offer the download once with its size. Declining SHALL not affect any other feature.
When the device has no internet connection, the offer SHALL say so and offer to connect to a network in place of
Download, and SHALL follow the connection as it comes and goes. Checking the connection SHALL send nothing.

#### Scenario: Offer on first launch
- **WHEN** the app starts for the first time
- **THEN** the offer is shown with the download size and the space it uses

#### Scenario: Skip is remembered
- **WHEN** the user skips the offer and restarts the app
- **THEN** the offer is not shown again

#### Scenario: Works after skipping
- **WHEN** the user skips the offer
- **THEN** the list, search, entries and camera work with placeholders

#### Scenario: Offer while offline
- **WHEN** the offer opens, the content has not been downloaded and the device has no internet connection
- **THEN** the offer still shows the download size, says there is no connection, and offers "Connect to a network" and "Not now" with no Download button

#### Scenario: Network that needs sign-in counts as offline
- **WHEN** the offer opens and the device's network has not been confirmed to reach the internet, for example Wi-Fi that still needs a sign-in page
- **THEN** the offer shows "Connect to a network" in place of Download

#### Scenario: Connect opens the internet panel
- **WHEN** the user taps "Connect to a network" on the offer
- **THEN** Android's internet connectivity panel opens over DexLens

#### Scenario: Network settings as the fallback
- **WHEN** the user taps "Connect to a network" on the offer and the device can't open the internet connectivity panel
- **THEN** the device's network settings open instead

#### Scenario: Offer can't open network settings
- **WHEN** the user taps "Connect to a network" on the offer and the device can open neither the panel nor its network settings
- **THEN** the offer shows a short message saying network settings can't be opened, the offer stays open, and the app does not crash

#### Scenario: Offer shows Download once connected
- **WHEN** the device gets an internet connection while the offline offer is open, for example after airplane mode is turned off in the panel
- **THEN** the offer switches to its usual Download button without being reopened

#### Scenario: Offer goes back offline
- **WHEN** the connection drops while the offer shows Download and before Download is tapped
- **THEN** the offer switches back to "Connect to a network" and "Not now"

#### Scenario: Skip while offline
- **WHEN** the user taps "Not now" on the offline offer
- **THEN** the offer is not shown again, and the list, search, entries and camera work as before with placeholders

#### Scenario: Connection check stays on the device
- **WHEN** DexLens checks whether the device is connected
- **THEN** it makes no network request and uses only ACCESS_NETWORK_STATE, which the release APK already declares, and no new permission

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
