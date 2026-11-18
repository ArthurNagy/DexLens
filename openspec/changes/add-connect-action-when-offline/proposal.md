# Proposal

Issue: #33

## Why

The first-launch "Add artwork?" offer and Settings' download section show Download (or Resume) even when the device
has no internet connection. Tapped offline, the download sits at "0 of N" with no explanation and only starts once a
connection appears. The offer is shown once, so a confusing first try may be the only one the user gets. Offline, both
places should say there is no connection and offer a quick way to connect, while the app stays fully usable.

## What Changes

- The app watches whether the device has a usable internet connection (a default network that has internet and that
  Android has validated). It reads this from the system: no network request, no new host, no new permission.
- First-launch offer, content not downloaded, offline: it still shows the download size, says there is no connection,
  and offers "Connect to a network" and "Not now" in place of Download. It switches between this and the usual Download
  button by itself as the connection comes and goes, without being reopened.
- Settings download section, content not downloaded or download interrupted, offline: it says there is no connection
  and offers "Connect to a network" in place of Download or Resume. Delete stays. Download or Resume comes back as soon
  as the device is connected, without leaving Settings.
- "Connect to a network" opens Android's internet connectivity panel over DexLens; if that can't open, the device's
  network settings; if neither can open, a short message, with no crash.
- "Not now" on the offline offer behaves as today: the offer is not shown again and the app works with placeholders.

## Non-goals

Copied from issue #33's "Out of scope":

- Behaviour after the download has started. Losing the connection mid-download stays as it is today: the work waits
  for a connection and resumes, and finished items are kept
- No Wi-Fi-only option and no warning about metered or mobile data. The download keeps using any connection, as today
- No starting the download automatically on connect. The user still taps Download
- No change to when the offer appears (once, on first launch) or to the rule that seeing it counts as answered
- No in-app network picker or toggles. Only the system panel, with network settings as the fallback
- No connectivity probe, ping or new host. raw.githubusercontent.com stays the only host contacted, and only after the
  user starts the download
- No new feature module. The offer and Settings both stay in :feature:settings
- No change to ContentPackRepository, ContentPackWorker, the work constraint or ContentPackState. Connectivity is read
  next to the download state, never folded into it

Also unchanged (issue Notes): the offer's rarely reached Interrupted branch (Resume on first launch) keeps its current
look offline, and the Downloading and Ready states look the same online and offline on both screens.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `artwork-download`: the "First-launch offer" requirement gains offline behaviour: no-connection wording, the
  "Connect to a network" action with its fallbacks, live switching as the connection changes, skipping while offline,
  and reading connectivity without a request or a new permission.
- `app-settings`: the "Settings screen" requirement's download scenarios gain their offline counterpart: "Connect to a
  network" in place of Download or Resume, coming back when connected.

## Impact

- `:core:domain`: new `core.domain.connectivity` package (`ConnectivityRepository`, `ObserveConnectivityUseCase`) and the `Connectivity` enum (`Online` / `Offline`) in `:core:model`.
- `:core:data`: new internal `AndroidConnectivityRepository` in `core.data.connectivity`, bound in `DataModule`;
  `ACCESS_NETWORK_STATE` declared in its manifest (already in the merged release manifest through WorkManager, so the
  shipped permission set is unchanged).
- `:core:testing`: new `FakeConnectivityRepository`.
- `:feature:settings:impl`: `ContentOfferState`/`ContentOfferViewModel` (connectivity flag and a
  `networkSettingsUnavailable` flag shown inline in the offline offer), `ContentOfferDialog`/`ContentOfferContent`, `SettingsState`/`SettingsViewModel` (connectivity flag and a new
  message), `ContentPackSection`, a shared `rememberOpenNetworkSettings` next to `rememberOpenLink`, new `settings_`
  strings, tests and phone/tablet goldens.
- No new library, no new module, no ADR. ContentPackRepository, ContentPackWorker, its CONNECTED constraint and
  ContentPackState are untouched.
