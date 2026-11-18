# Design

## Context

See proposal.md for why. The current code, as read on `main` (cc38a09):

- `OfflineContentPackRepository.start()` enqueues the unique `ContentPackWorker` job with
  `NetworkType.CONNECTED`. Offline, the job stays ENQUEUED and `stateFor()` maps that to
  `ContentPackState.Downloading`, which is why the offer and Settings show "0 of N". This change leaves the repository,
  the worker, the constraint and `ContentPackState` alone (Non-goals).
- `ContentOfferViewModel` (`:feature:settings:impl`) holds `ContentOfferState(content)` and, on create, marks the offer
  answered and collects `ObserveContentPackStateUseCase`. `ContentOfferDialog` renders `ContentOfferContent` inside a
  `DialogSceneStrategy` entry: its own window, with no `SnackbarHost`.
- `SettingsViewModel` holds `SettingsState(themeMode, scannerEnabled, content, versionName, pendingError,
  pendingMessage)`. `SettingsScreen` shows `pendingError` and `pendingMessage` in its Scaffold's snackbar and calls
  `onErrorConsumed()` / `onMessageConsumed()`. `ContentPackSection` renders the download section per
  `ContentPackState`.
- `rememberOpenLink(url, onUnavailable)` in `SettingsScreen.kt` starts an `ACTION_VIEW` intent from the composable's
  Context and calls `onUnavailable` on `ActivityNotFoundException`. Its tests use Robolectric's
  `checkActivities(true)` so that nothing resolves.
- `DataModule` binds each `:core:domain` repository interface to its internal `:core:data` implementation.
  `catchStorage { }` is :core:data's exception boundary for platform reads, and `PackageAppInfoRepository` already
  runs its `PackageManager` read through it. `:core:testing` holds `MutableStateFlow`-backed fakes such as
  `FakeContentPackRepository`.
- `ACCESS_NETWORK_STATE` is already in the merged release manifest, merged in by WorkManager 2.12.
- minSdk is 29, so `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` (API 29), `registerDefaultNetworkCallback` (API 24)
  and `onBlockedStatusChanged(Network, Boolean)` (API 29) need no version checks.

## Goals / Non-Goals

**Goals:**

- One connectivity signal, "the default network has INTERNET and VALIDATED and is not blocked", observed as a
  `Flow<Boolean>` from the domain layer and read next to the download state by both screens.
- One shared "open network settings" helper with a three-step fallback, used by the offer and Settings.
- Offline branches only where the issue asks for them: offer `NotDownloaded`; Settings `NotDownloaded` and
  `Interrupted`.

**Non-Goals:**

- No change to the offer's `Interrupted`, `Downloading` or `Ready` branches, or to Settings' `Downloading` and `Ready`.
- No "waiting for a connection" hint once the download has started (a possible follow-up in the issue).
- No connectivity model type, typed error or probe.

## Decisions

### D1. Domain: `ConnectivityRepository` + `ObserveInternetAvailableUseCase` in `core.domain.connectivity`

`interface ConnectivityRepository { fun observeInternetAvailable(): Flow<Boolean> }`: it never throws and emits the
current value as soon as it is collected. `ObserveInternetAvailableUseCase @Inject constructor(repository) : UseCase`
returns `repository.observeInternetAvailable()`. No `Result`, since nothing can fail that the caller could act on, and
no `DomainError.Typed`.

- *Alternative: fold connectivity into `ContentPackState`* (for example a `WaitingForNetwork` state). Rejected: the
  issue's Out of scope forbids it, and connectivity is a device fact, not a download state.
- *Alternative: put it on `ContentPackRepository`.* Rejected: one repository per domain type, and the issue keeps
  `ContentPackRepository` unchanged.

### D2. Data: internal `AndroidConnectivityRepository` in `core.data.connectivity`

`@Inject constructor(@ApplicationContext context: Context)`, bound with `@Binds` in `DataModule`. `observeInternetAvailable()`
is a `callbackFlow`:

1. A `NetworkCallback` tracks the current default `Network` with its last `usable` flag and `blocked` flag, and sends
   `usable && !blocked`. `usable` means the network has `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`.
   - `onCapabilitiesChanged(network, caps)`: make `network` the current one, resetting `blocked = false` when it
     differs from the previous current network, store `usable`, then send.
   - `onBlockedStatusChanged(network, blocked)`: store `blocked` for the current network, then send.
   - `onLost(network)`: send false only when `network` is the current one, and ignore any other network.
   - Since API 26, `onAvailable` is always followed by `onCapabilitiesChanged`, so it needs no override.
2. One `catchStorage { }` block runs the platform calls: get the `ConnectivityManager` (null fails the block), seed by
   sending the `usable` value of `activeNetwork`'s capabilities (no network or null capabilities is false, and the
   seed records `activeNetwork` as the current network), then `registerDefaultNetworkCallback(callback)`. It returns
   the manager. Exceptions stop here, as in `PackageAppInfoRepository`. `activeNetwork` and `getNetworkCapabilities`
   can throw `SecurityException` on some Android 11 builds, and registration can throw a `RuntimeException` when the
   app holds too many callbacks.
3. On `Err`, the flow sends `true` and stays open until cancelled. The screens then fall back to today's behaviour:
   Download shows, and the worker still waits for a network. `catchStorage` has already logged the failure.
4. `awaitClose { registered.onOk { manager -> manager.unregisterNetworkCallback(callback) } }`: the flow unregisters
   only if registration succeeded, because `unregisterNetworkCallback` throws `IllegalArgumentException` for a
   callback that was never registered. The flow ends with `.distinctUntilChanged()`.

There is no hand-written try/catch. `trySend` is thread-safe, so the callbacks run on the system's connectivity thread
with no dispatcher: no `Dispatchers.*` and no `DispatcherProvider` needed. The seed runs before registration, so the
callback thread sees its writes.

"Connected" means INTERNET **and** VALIDATED (binding, from the issue). WorkManager 2.12 schedules `CONNECTED` as
JobScheduler `NETWORK_TYPE_ANY`, which waits for a validated network. With INTERNET alone, Download would show behind
a captive portal and the job would sit at "0 of N" again. A blocked default network (for example an OEM per-app
"mobile data off" setting, or always-on VPN lockdown while the VPN is down) is treated as offline for the same reason.

`:core:data`'s manifest declares `ACCESS_NETWORK_STATE`, so the read doesn't depend on WorkManager's manifest merge.
The merged permission set stays the same (verified in task 5.3). `core.data.connectivity` uses nothing from
`:core:network`.

- *Alternative: `NetworkCallback` with a `NetworkRequest` for INTERNET+VALIDATED (`registerNetworkCallback`).*
  Rejected: it reports any matching network, not the one the app's traffic and WorkManager actually use.
- *Alternative: an HTTP probe.* Rejected: Out of scope (no probe, no new host), and it would break "sends nothing".
- *Alternative: a hand-written try/catch around the platform calls.* Rejected: `catchStorage` is the project's
  exception boundary. It already rethrows `CancellationException` and `Error`, and it logs.

### D3. Presentation state: `internetAvailable` on both states, `true` initially (fail-open)

`ContentOfferState` gains `internetAvailable: Boolean` and `networkSettingsUnavailable: Boolean` (persistent state, not
a one-shot slot: see D4). `SettingsState` gains `internetAvailable: Boolean` (it already has `pendingMessage`). Both
`Initial` values use `internetAvailable = true`, and the offer's `networkSettingsUnavailable = false`.

Both ViewModels collect `ObserveInternetAvailableUseCase` in an intent started from the on-create block, wrapped in
Orbit's `repeatOnSubscription { }`, so the system callback is registered only while the UI collects state and is
removed while the screen is stopped. The existing `ContentPackState` collection stays as it is.

**Trade-off:** an offline user sees Download for a frame, until the first emission arrives right after collection
starts. Then the dialog or section reflows to the offline version (one added line, one button swapped).

Starting at `true` is acceptable because it fails open:
- A tap in that frame does exactly what Download does today: it enqueues the job, which waits for a network. Nothing
  breaks, and nothing is lost.
- Existing tests and goldens built on `Initial` don't change.
- If connectivity can't be read at all (D2 step 3), the screens stay on today's behaviour instead of being stuck on
  "offline".

*Alternatives:* `false` makes every online user, the common case, see the offline version briefly, and it fails
closed when connectivity can't be read. A nullable "unknown" adds a third layout for one frame.

### D4. The "can't open network settings" message

- **Settings:** `SettingsViewModel.onNetworkSettingsUnavailable()` sets
  `pendingMessage = R.string.settings_network_settings_unavailable`. The existing Scaffold snackbar and
  `onMessageConsumed()` show it.
- **Offer: an inline line, no snackbar** (maintainer's decision). It is persistent state, so it is a flag, not a
  consumable `pendingMessage` slot.
  - `ContentOfferViewModel.onNetworkSettingsUnavailable()` sets `networkSettingsUnavailable = true`.
  - `ContentOfferContent` shows `settings_network_settings_unavailable` as a `Text` (`offer:message`) directly under
    the "Connect to a network" button while the flag is true, with `liveRegion = Polite` so TalkBack reads it. It
    appears only in the offline `NotDownloaded` branch.
  - `ContentOfferViewModel.onNetworkSettingsOpened()` clears the flag. `rememberOpenNetworkSettings` takes an
    optional `onOpen` callback next to `onUnavailable` and calls exactly one of them per tap, so each tap ends in a
    single intent: a failed retry keeps the line, and a try that opens the panel or settings removes it. (Orbit
    reduces off the main thread, so a "clear, then maybe set again" pair of intents could land out of order and hide
    the message after a failed retry. The implementation review caught this.) The ViewModel also clears the flag in
    the same `reduce` that sets `internetAvailable = true`, so the line disappears when the connection returns.
  - The online branch never renders the line, so the existing online `offer` golden stays the same.
- *Alternative: a `SnackbarHost` inside the dialog surface.* Rejected by the maintainer. It adds a host to a dialog
  window, a timed message that is easy to miss in a one-time offer, and a layout slot in the online branch too.
- *Alternative: a Toast.* Rejected: it isn't the project's pattern, and it is harder to test.

### D5. `rememberOpenNetworkSettings(onUnavailable)` next to `rememberOpenLink` in `SettingsScreen.kt`

It returns a `() -> Unit` that starts `Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)` from `LocalContext`. On
`ActivityNotFoundException` it starts `Intent(Settings.ACTION_WIRELESS_SETTINGS)`, and on a second
`ActivityNotFoundException` it calls the latest `onUnavailable` (held with `rememberUpdatedState`, as `rememberOpenLink`
does). It is used by `ContentOfferDialog` (wired to `ContentOfferViewModel::onNetworkSettingsUnavailable`) and by
`SettingsScreen` (wired to `SettingsViewModel::onNetworkSettingsUnavailable`). The ViewModels never see an Intent or a
Context.

- On Android 12+ the panel intent shows the SystemUI Internet dialog over DexLens.
- On Android 10–11 (API 29–30, inside minSdk) it opens Settings' `SettingsPanelActivity`, a bottom sheet over
  DexLens. Task 6.1 checks it on an API 29 or 30 emulator. If a plain `startActivity` doesn't open the panel there,
  the helper launches the panel through a `rememberLauncherForActivityResult(StartActivityForResult())` launcher (the
  panel docs show `startActivityForResult`), with the result ignored. The same `ActivityNotFoundException` fallback
  wraps `launcher.launch(...)`.
- The flow brings the outcome back (the screens recompose when the connection changes), so no result is read.

### D6. UI branches

`ContentOfferContent(state: ContentPackState, internetAvailable: Boolean, networkSettingsUnavailable: Boolean,
onDownload, onConnect, onSkip, onHide, modifier)`:

- `NotDownloaded` + offline: `SizeText()`, the no-connection line, a `Button` "Connect to a network"
  (`offer:connect`), the inline message line while `networkSettingsUnavailable` (`offer:message`), and the existing
  `TextButton` "Not now" (`offer:skip`). No `offer:download`.
- `NotDownloaded` + online, and every other state whatever the connection: unchanged.

`ContentPackSection(content, internetAvailable, onDownload, onConnect, onCancelDownload, onDeleteContent)`, with
`SettingsContent` gaining `internetAvailable` (read from state) and `onConnect`:

- `NotDownloaded` + offline: `SizeText()`, the no-connection line, a `Button` "Connect to a network"
  (`settings:connect`).
- `Interrupted` + offline: `ProgressRow`, `settings_interrupted`, the no-connection line, then the "Connect to a
  network" `Button` (`settings:connect`) and the existing Delete `OutlinedButton` (`settings:delete`) in a `FlowRow`
  with `spacedBy(AppSpacing.sm)` both ways. The two labels together need about 380dp at 1.0 font scale, more than a
  compact window minus padding, so the buttons wrap onto two lines rather than being squeezed. The online
  `Interrupted` row (Resume + Delete) is unchanged.
- `Downloading`, `Ready`, and the online branches: unchanged.

A private `OfflineText()` composable renders the no-connection line (bodyMedium) with
`semantics { liveRegion = LiveRegionMode.Polite }`, so TalkBack announces it when the connection drops while the
screen is open. The "Not now" behaviour is untouched: seeing the offer already marks it answered on create.

New strings, all in `feature/settings/impl/src/main/res/values/strings.xml` with the `settings_` prefix:

| key | text |
|---|---|
| `settings_offline` | No internet connection. |
| `settings_connect` | Connect to a network |
| `settings_network_settings_unavailable` | Network settings can't be opened on this device. |

The wording states the fact and never promises an instant switch, because validation after airplane mode takes a few
seconds and never happens on a captive portal (issue risk).

### D7. Form factors

Only `:app` (phones, tablets, foldables) is touched. The offer is a dialog scene sized to its content at every window
size. The Settings section keeps its `maxContentWidth` cap.

- The offline offer and the offline `NotDownloaded` section add one line and swap one button, stacked full width as
  today, so compact and expanded windows lay them out the same way as the existing branches.
- The offline `Interrupted` section differs by window size. On compact windows and at large font scales, the
  `FlowRow` wraps Delete under Connect. On expanded windows (and the tablet golden), both buttons fit on one row.

Goldens cover the phone (Pixel 7) and tablet (MediumTablet) qualifiers for each offline branch. No other form factor
exists in this project.

### D8. Layering check

The feature `impl` gains a dependency only on `ObserveInternetAvailableUseCase` (`:core:domain`). `AndroidConnectivityRepository`
stays `internal` in `:core:data`. `FakeConnectivityRepository` goes in `:core:testing`. `FlowRow` comes from Compose
foundation, already on the classpath. No new library, module or platform-wide decision, so no ADR, as the issue says.

## Risks / Trade-offs

- [After airplane mode is turned off, Download comes back only once Android validates the network, usually a few
  seconds, and never behind a captive portal] → This matches when the worker would run. The wording makes no promise
  of an instant switch, and the QA journey waits for the switch instead of asserting it at once.
- [Robolectric only partly supports default-network callbacks and validated capabilities] → Keep
  `AndroidConnectivityRepository` thin. Its test builds capabilities with `ShadowNetworkCapabilities` and drives the
  callbacks from `ShadowConnectivityManager.getNetworkCallbacks()` by hand (task 2.3). If a branch can't be reached,
  the project-wide 85% `coverageVerify` gate absorbs it. Don't add a coverage exclusion.
- [OEMs and API levels handle the panel intent differently] → The fallback chain covers a missing panel. Task 6.1 runs
  Connect on an API 29/30 emulator as well as a current one, and task 6.2 asks for one real Android 12+ device (with
  the maintainer's consent). D5 names the activity-result launcher as the fix if API 29–30 needs it.
- [Offline users see Download for one frame before the reflow (D3)] → This is accepted as fail-open. A tap then
  behaves exactly as today.
- [The changed `ContentOfferContent` / `ContentPackSection` / `SettingsContent` / `ContentOfferState` signatures] →
  Every caller is in `:feature:settings:impl`. Tasks 3.3, 4.2 and 4.3 list the call sites and update them, and each
  of those tasks runs that module's tests.

## Migration Plan

None. There is no stored data and no new permission. Rolling back is a plain revert.
