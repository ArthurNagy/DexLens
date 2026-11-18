## MODIFIED Requirements

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
