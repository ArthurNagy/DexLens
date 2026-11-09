Issue: none

## Why

The app's core job is finding a species and reading about it, offline. This change turns the bundled data into a
search-first home and a full entry screen.

## What Changes

- A home screen with a browsable Pokédex-order list under a Material 3 search bar, with sprite or placeholder, number,
  name and type chips.
- Prefix search as you type, ignoring case and accents, with punctuation-free names, Pokédex numbers and other languages.
- A full entry: types, stats, abilities, matchups computed from the type chart, evolution line, egg groups, gender
  ratio, forms and names in other languages.
- A list-detail layout on tablets and foldables, and a shared-element transition from list to entry on phones.

## Capabilities

### New Capabilities
- `pokedex-browse`: the home list and search behaviour.
- `species-entry`: the full entry for one species, including computed type matchups.

### Modified Capabilities

## Non-goals

- The camera (next change) and the artwork download (later change). Art slots show placeholders until then.
- Learnsets.

## Impact

New `:feature:pokedex` and `:feature:entry` modules, entities in `:core:model`, use cases in `:core:domain`, repositories
in `:core:data`.
