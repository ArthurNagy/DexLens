Issue: none

## Why

The entry's two longest sections are the least useful while playing. "Pokédex entries" repeats several paragraphs of
downloaded game text, and "Names in other languages" is a table most people never read (search already finds those names).
What players look up next to a game is what the Pokémon learns and where to catch it, in the game they are playing.

The back gesture also looks broken. The back arrow fades the entry while the artwork slides back into the list, but the
system gesture uses Navigation 3's default predictive animation, which shrinks the whole entry into a card while the
artwork flies out of it, so the two overlap.

## What Changes

- Remove the "Pokédex entries" and "Names in other languages" sections from the entry.
- Show one short description under the header: the newest downloaded Pokédex text in the display language. Without the
  optional download there is no description, and the genus stays the one-line summary.
- Add an "In game" section with a game picker (one choice per game pair, such as Scarlet / Violet). For the chosen game it
  shows the moves learnt by level up and where to find the Pokémon in the wild. The picker lists only games with data for
  this Pokémon, starts on the last game the user picked when it is available, and otherwise on the newest one.
- Bundle level-up learnsets, move names, types and power, and wild encounter locations in the prebuilt database, generated
  from the same pinned PokeAPI commit.
- Give the predictive back gesture the same fade as the back arrow, so the gesture scrubs the shared-element transition
  instead of shrinking the entry.
- Update docs/MVP_SPEC.md: level-up learnsets and wild locations move from out of scope into v1. Other move data (TMs,
  egg moves, tutors, move descriptions) stays out.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `species-entry`: the full entry loses names in other languages and gains a short description, the new in-game section,
  and a predictive back that matches the back arrow.
- `pokedex-data`: the bundled data gains learnsets, moves and wild encounter locations.
- `artwork-download`: downloaded flavor text appears as the entry's one description instead of a section listing every game.

## Non-goals

- TM, egg, tutor or event moves, move descriptions, accuracy or PP.
- Encounter rates, time of day, seasons or other encounter conditions.
- Location data for Brilliant Diamond / Shining Pearl, Legends: Arceus, Scarlet / Violet and later games. PokeAPI has none
  at the pinned commit, so the section says the data is not available for that game.
- Changing how search uses names in other languages.

## Impact

- `tools/build_database.py`, `:core:database` (new entities and a DAO, schema version 2), `:core:model`, `:core:domain`,
  `:core:datastore`, `:core:data`, `:core:testing`, `:feature:entry:impl`, `:app` (`AppNavigation`).
- The bundled database grows from 2.0 MB to 5.2 MB on the device, and from 0.9 MB to 2.3 MB in the APK.
