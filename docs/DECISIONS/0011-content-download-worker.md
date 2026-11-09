# 11. Content download worker lives in :core:data

Date: 2026-11-09

## Status

Accepted

## Context

The optional artwork and flavor text download must survive leaving the screen, so it is WorkManager work with a
foreground notification. android-forge's stack note suggests a `:sync:work` module for `@HiltWorker` classes.

The worker's only job is to drive `ContentPackDownloader`, which is built from the database, network and file sources.
Those sources may only be used by `:core:data`, and `:core:data` exposes nothing public. A separate `:sync:work` module
would need either a public downloader or a dependency on the storage and network modules, breaking one of those rules.

Alternatives considered: a `:sync:work` module with an interface in `:core:domain`, which adds a module and a public
seam for a single worker; an application-scope coroutine, which would not survive process death.

## Decision

`ContentPackWorker` lives in `:core:data`, internal, next to the downloader it runs. `:app` provides the Hilt worker
factory.

## Consequences

- No new module for one worker, and the data module's public surface stays empty.
- If more background jobs appear, a `:sync:work` module can be reconsidered.
