# Spec Delta

## MODIFIED Requirements

### Requirement: Scanner above a list sheet
While the scanner is switched on, the home screen SHALL show the camera scanner area at the top and a sheet below it
holding the search field and the species list. The scanner area SHALL be 3:2 below the status bar when the window has
room for it. The preview SHALL show behind the sheet's top corners only where the area's width, at the camera
picture's orientation on the display, guarantees it covers them unenlarged, as on a phone in portrait. The analysed
region SHALL NOT change.

#### Scenario: Home shows scanner and sheet
- **WHEN** the home screen opens on a phone in portrait
- **THEN** the scanner area fills the top at 3:2 and the sheet with the search field and the list fills the rest

#### Scenario: No separate scan step
- **WHEN** the home screen is showing
- **THEN** there is no Scan button and scanning does not open another screen

#### Scenario: Short window keeps the list reachable
- **WHEN** the window is too short for a 3:2 scanner area, such as a phone in landscape
- **THEN** the scanner area takes at most 45% of the window height and the search field stays visible

#### Scenario: Wide window keeps list and entry together
- **WHEN** the window is wide enough to show the list beside an entry
- **THEN** the scanner area and the sheet share the list pane and the entry opens beside them

#### Scenario: Camera behind the sheet corners
- **WHEN** the home screen shows with the scanner on, the sheet collapsed and the camera running, in a window where the scanner area's width, at the camera picture's orientation on the display, guarantees the camera picture reaches past the visible area by the sheet's corner radius above and below without being enlarged, such as a phone in portrait or the list pane of a portrait tablet or foldable
- **THEN** the live camera preview shows behind both of the sheet's rounded top corners, with no black or empty wedges

#### Scenario: Corners where filling would enlarge the preview
- **WHEN** the home screen shows with the camera running in a window where filling the sheet's corners would need the preview to be enlarged, such as a phone in landscape (a 360dp list pane), the list pane of a landscape tablet or unfolded foldable, or a compact window on a landscape display
- **THEN** the preview is not enlarged and ends at the sheet's top edge, and the sheet's corners show the scanner area's dark background

#### Scenario: Corner preview keeps the scanner layout
- **WHEN** the camera preview shows behind the sheet's corners
- **THEN** the visible scanner area is 3:2 below the status bar, or 45% of the window height where that cap applies, and the aiming box, the hint or match label, the torch and the settings button all end above the sheet's top edge

#### Scenario: Preview under the sheet not analysed
- **WHEN** a frame is analysed, in any window
- **THEN** only the sensor region under the aiming box is sent to recognition, that region is the same whether or not the preview shows behind the sheet's corners, and no part of the preview under the sheet is analysed

#### Scenario: Sheet corners fade with the camera
- **WHEN** the camera preview shows behind the sheet's corners and the sheet is dragged or settles between collapsed and expanded
- **THEN** the area behind its corners fades toward the top bar colour along with the rest of the scanner area, with no black flash at the corners

#### Scenario: Scanner messages fill the sheet corners
- **WHEN** the scanner area shows the permission prompt, the "camera unavailable" message or a scan error
- **THEN** the sheet's corners show the scanner area's dark background and the message is centred in the visible scanner area

#### Scenario: Scanner off has no corner gap
- **WHEN** the home screen shows with the scanner switched off
- **THEN** there is no camera, and the list's surface starts right below the top bar with no gap
