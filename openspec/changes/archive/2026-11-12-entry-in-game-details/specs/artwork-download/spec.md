## MODIFIED Requirements

### Requirement: Stored format
Artwork SHALL be stored as WebP at about 384 pixels.

#### Scenario: Artwork is scaled and converted
- **WHEN** a piece of official artwork is saved
- **THEN** it is stored as a WebP no larger than 384 pixels on its long side

#### Scenario: Flavor text is imported
- **WHEN** the flavor text table has been downloaded
- **THEN** each entry shows its newest flavor text as its description, without line-break artifacts

### Requirement: Placeholders without content
Without downloaded content, the app SHALL show type-tinted placeholders and omit flavor text, with no broken UI.

#### Scenario: Entry without flavor text
- **WHEN** an entry opens and flavor text has not been downloaded
- **THEN** the entry has no description

### Requirement: Content appears without reopening
Downloaded art and flavor text SHALL appear on open screens as soon as they are available.

#### Scenario: Art appears when the download finishes
- **WHEN** the download finishes while the list or an entry is open
- **THEN** placeholders are replaced by artwork without reopening the screen

#### Scenario: Flavor text appears in an open entry
- **WHEN** flavor text finishes importing while an entry is open
- **THEN** the description appears without reopening the entry
