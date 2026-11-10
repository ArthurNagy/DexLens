# 13. The scanner lives in the Pokédex feature

Date: 2026-11-10

## Status

Accepted

## Context

The camera moved from its own screen to the top of the home screen, above the species list. The home screen belongs to
`:feature:pokedex`, and the camera code was in `:feature:scanner:impl`. The project rules forbid one feature's `impl` from
using another's, so the home screen could not simply compose the camera.

Alternatives considered: keeping `:feature:scanner` and exposing the camera panel as a Hilt-bound composable from its
`:api`, which isolates the camera dependencies, but `:api` modules hold only Nav keys and it would add a new cross-feature
pattern for one screen; moving the camera into a `:core` module, which would put CameraX and ML Kit below every feature
for the sake of one.

## Decision

Merge the scanner into `:feature:pokedex:impl`, in a `scanner` package, and delete `:feature:scanner:api` and `:impl` along
with `ScannerKey`. One ViewModel and one state decide what the list shows, whether the answer came from typing or from the
camera.

## Consequences

- `:feature:pokedex:impl` now carries the CameraX and ML Kit dependencies, the camera permission and the two bundled model
  files, so model updates are copied to `feature/pokedex/impl/src/main/assets/`.
- A later feature that needs the camera would have to extract this package first.
