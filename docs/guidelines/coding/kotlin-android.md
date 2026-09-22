# Kotlin / Android conventions

For the module map and dependency direction,
see the `## Modules` section of the root `AGENTS.md` — it isn't restated here.

## Naming

- Classes/objects/interfaces: `UpperCamelCase`. Functions/properties: `lowerCamelCase`.
  Constants: `UPPER_SNAKE_CASE` for `const val`.
- Composables are the one exception to function naming: `UpperCamelCase`,
  named as a noun (they return `Unit` but represent UI).
- Test methods use backtick-quoted sentences (see existing tests under `core/model/src/test`).

## Nullability

- No bare `!!`. If a non-null assertion is genuinely safe,
  add a one-line comment explaining the invariant that makes it safe; otherwise handle the null case.
- Prefer `?:`, `?.let`,
  and sealed/enum modeling over nullable flags where the null actually means "one of several states."

## Scope functions

- `let` — transform a non-null value inline, especially after `?.`.
- `run` — compute a result from a receiver's members without needing `this` returned.
- `with` — group several calls on the same receiver when you don't need the fluent `.` chaining.
- `apply` — configure an object and return it (builder-style construction).
- `also` — a side effect (logging, validation) that shouldn't change the chained value.

## Coroutines and Flow

Never hardcode `Dispatchers.IO` / `Dispatchers.Default` in injected code.
Inject a `CoroutineDispatcher` qualified with `@Dispatcher(OpenPianoDispatcher.…)` from `core:common` (`core/common/src/main/kotlin/dev/thomas_kiljanczyk/openpiano/core/common/di/Dispatcher.kt`),
provided by `CoroutineModule` in the same package. This keeps dispatchers swappable in tests.

This does not apply to the `core:audio` touch-to-sound path — see AGENTS.md's hot-path rules and `docs/guidelines/coding/compose.md`.
