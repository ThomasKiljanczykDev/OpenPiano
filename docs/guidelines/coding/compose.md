# Compose conventions

## Stateful / stateless split

Prefer two overloads for a screen-level composable: a stateful one that owns/collects state (e.g.
from a `ViewModel`) and a stateless one that takes plain parameters and lambdas.
This makes the stateless version directly previewable and testable without a `ViewModel`.

## `remember` vs `rememberSaveable`

- `remember` — survives recomposition only.
  Use for values you're fine losing on configuration change or process death (derived/cached values,
  animation state).
- `rememberSaveable` — survives configuration change and process death via the saved-instance state.
  Use for anything the user would be annoyed to lose (a selection, scroll position, a toggle).

## Side effects

- `LaunchedEffect(key)` — launch a coroutine tied to a composable's lifecycle,
  restarted when `key` changes (e.g. collecting a `Flow` into state).
- `DisposableEffect(key)` — register/unregister a non-Compose listener or resource,
  with an `onDispose` cleanup block.
- `SideEffect` — publish Compose state to non-Compose code on every successful recomposition,
  with no cleanup needed.

## Previews

Add a `@Preview` for the stateless overload with representative sample data,
not the `ViewModel`-backed stateful one.

## Touch-to-sound path

`feature:keyboard:impl`'s pointer handling calls `AudioEngine.noteOn` / `noteOff` directly from the touch callback.
Never route this through a coroutine, a `Flow`, a `ViewModel`,
or anything that triggers recomposition before the call — see AGENTS.md's `## Hot path — core:audio` section for why.
