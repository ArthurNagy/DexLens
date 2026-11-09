# 12. Prefer Kotlin's atomics over Java's

Date: 2026-11-09

## Status

Accepted

## Context

Concurrent code occasionally needs a single value that several coroutines update, such as the first failure of a
parallel download. Kotlin has two options: `java.util.concurrent.atomic` and the standard library's
`kotlin.concurrent.atomics`.

The Kotlin API works in common code, so `:core:model` and `:core:domain` could use it without a JVM-only import, and its
names (`load`, `store`, `fetchAndAdd`) say what each operation does. It is still marked `@ExperimentalAtomicApi` in
Kotlin 2.4, so every use needs an opt-in and its shape could change in a later Kotlin release.

Alternatives considered: staying with `java.util.concurrent.atomic`, which is stable and needs no opt-in, but keeps a
platform API in code that is otherwise plain Kotlin; atomicfu, which adds a compiler plugin for no gain here.

## Decision

Use `kotlin.concurrent.atomics` whenever an atomic is needed, in production code and tests, with
`@OptIn(ExperimentalAtomicApi::class)` scoped to the function or class that uses it. Do not import
`java.util.concurrent.atomic`.

Reach for an atomic only when state cannot live somewhere simpler. Screen state belongs in the Orbit `State`, whose
`reduce` already serializes updates.

## Consequences

- One atomics API across the codebase, which the `android-reviewer` agent checks through the rule in `CLAUDE.md`.
- A future Kotlin release may rename or change this API. The uses are few and the compiler will point at each one.
