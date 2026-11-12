## Purpose

Recognise a species from what it looks like in the scanner's aiming box, entirely on the device, and combine that with
the name signal so the scanner works on screens without a readable name and picks better between close names.

## ADDED Requirements

### Requirement: Recognise a species from the picture
Each analysed frame's aiming box SHALL be compared against the bundled reference pictures of every species, giving each
species a similarity score. A species SHALL count as seen in the picture only when its score reaches the candidate
threshold. The picture signal SHALL name species only, never forms.

#### Scenario: Clear sprite scores its species
- **WHEN** the aiming box shows a clear sprite of Pikachu and no text
- **THEN** Pikachu has the highest picture score and reaches the candidate threshold

#### Scenario: Everyday scene scores nothing
- **WHEN** the aiming box shows a room, a hand or a blank screen
- **THEN** no species reaches the candidate threshold and nothing is locked

#### Scenario: Form counts as its species
- **WHEN** the picture signal recognises Mega Charizard X
- **THEN** the candidate is Charizard

### Requirement: Picture alone locks only when sure
Without a name candidate in the frame, a species SHALL lock from the picture only after it reaches the image-only
threshold in four of the last five frames. It SHALL lock alone when it leads the runner-up by the lead margin. Otherwise
the species that reach the image-only threshold within the margin of the best, at most three, SHALL be offered together.
A species seen only in the picture SHALL never be offered alongside another lock before meeting this rule itself.

#### Scenario: Sprite without a name locks
- **WHEN** no name is readable and one species is above the image-only threshold, with a clear lead, in four of the last five frames
- **THEN** that species is locked

#### Scenario: Three picture frames are not enough
- **WHEN** no name is readable and one species is above the image-only threshold in only three of the last five frames
- **THEN** nothing is locked

#### Scenario: Weak picture score does not lock
- **WHEN** no name is readable and the best species stays above the candidate threshold but below the image-only threshold in every frame
- **THEN** nothing is locked

#### Scenario: Close picture scores offered as choices
- **WHEN** no name is readable and two species stay above the image-only threshold within the lead margin of each other for four of the last five frames
- **THEN** both are listed in Pokédex order

#### Scenario: At most three picture choices
- **WHEN** no name is readable and, in four of the last five frames, five species reach the image-only threshold within the lead margin of the best
- **THEN** only the three highest-scoring species are listed

#### Scenario: Picture-only species does not ride along
- **WHEN** a name locks in this frame and another species has been seen only in the picture in three of the last five frames
- **THEN** only the named species is listed

### Requirement: Name and picture agreeing lock faster
A species that is both a name candidate and a picture candidate in the same frame SHALL lock once that has happened in
two of the last five frames.

#### Scenario: Agreement locks after two frames
- **WHEN** Pikachu is both a name candidate and a picture candidate in two of the last five frames
- **THEN** Pikachu is locked

#### Scenario: Name alone still needs three frames
- **WHEN** Pikachu is a name candidate in two of the last five frames and never a picture candidate
- **THEN** nothing is locked

### Requirement: Picture decides between close names
When one piece of recognised text matches several species equally well, the picture SHALL keep only the one among them
with the highest picture score, provided that score reaches the candidate threshold and leads the others by the lead
margin. Otherwise the text SHALL behave as it does without the picture signal.

#### Scenario: Picture picks the gender
- **WHEN** the recognised text is "NIDORAN" with no readable symbol and the picture candidate is Nidoran♂
- **THEN** only Nidoran♂ is a candidate

#### Scenario: Picture settles a misread
- **WHEN** a misread name is equally close to two species and the picture candidate is one of them
- **THEN** that species is a candidate and the other is not

#### Scenario: Close picture scores keep both names
- **WHEN** the recognised text is "NIDORAN" with no readable symbol and Nidoran♀ and Nidoran♂ score within the lead margin of each other
- **THEN** Nidoran♀ and Nidoran♂ are both candidates

#### Scenario: No picture keeps both names
- **WHEN** the recognised text is "NIDORAN" with no readable symbol and no species reaches the candidate threshold
- **THEN** Nidoran♀ and Nidoran♂ are both candidates

### Requirement: Name wins over the picture
When a frame has any name candidate, picture candidates SHALL count in that frame only for species the text also names.

#### Scenario: Name decides between close pictures
- **WHEN** Pikachu and Raichu both score as picture candidates and the recognised text is "RAICHU"
- **THEN** Raichu is the candidate and Pikachu is not

#### Scenario: Name and picture disagree
- **WHEN** the recognised text is "EEVEE" in three of the last five frames while the picture's best species is Vaporeon
- **THEN** Eevee is locked and Vaporeon is not

### Requirement: Text recognition works without the picture model
If the image model cannot be loaded or fails on a frame, scanning SHALL continue on text alone with today's rules, and
the user SHALL see no error.

#### Scenario: Model missing keeps names working
- **WHEN** the image model fails to load and the recognised text is "PIKACHU" in three of the last five frames
- **THEN** Pikachu is locked and no error is shown

#### Scenario: Slow picture frame keeps names working
- **WHEN** the picture signal has not answered a frame in time and the recognised text is "PIKACHU"
- **THEN** the frame counts as a name-only frame and scanning continues
