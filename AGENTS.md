# AGENTS.md — OpenPiano

## Repo layout

The Gradle build lives in `android/`, not at the repo root. Run every build command from there:

```
cd android && ./gradlew <task>
```

The repo root holds `AGENTS.md`, `CLAUDE.md` (`@AGENTS.md`), `README.md`, `PLAN.md`, `LICENSE`,
`NOTICE.md`, `PRIVACY.md`, `.gitignore`, `.github/`, `android/`, `docs/`, `fastlane/` (store metadata, F-Droid-readable),
and `fdroid/` (fdroiddata recipe). Release process: `docs/RELEASING.md`. The IDE project lives at `android/` too;
`android/.gitignore` ignores `.idea/` and allowlists the shareable files,
and a root `.idea/` is ignored outright. `PLAN.md` is the v1 spec; it is authoritative for design decisions.

## Modules

| Module | Contents |
|---|---|
| `core:model` | Pure Kotlin. `MidiMessage`, `Note`, `Piano`, `KeyboardLayout`, `KeyLabelMode`. No Android deps |
| `core:common` | Dispatcher qualifiers, `@ApplicationScope`, `allowingThreadDiskReads` StrictMode scoping helper |
| `core:designsystem` | Theme, color, type, key-color tokens |
| `core:ui` | Shared composables |
| `core:datastore-proto` | `user_preferences.proto` + serializer |
| `core:data` | `UserPreferencesRepository` over DataStore |
| `core:audio` | Oboe + TSF + JNI. `AudioEngine` interface, `OboeAudioEngine`, Hilt module, SF3 asset |
| `core:analytics` | `AnalyticsHelper` interface. No Firebase dependency — see "Product flavors" below |
| `core:midi` | MIDI I/O support |
| `core:testing` | Shared test utilities |
| `feature:keyboard:impl` | Compose keyboard, `KeyboardViewModel`, navigation |
| `feature:settings:impl` | Settings screen, `LocaleManager` |
| `app` | `OpenPianoApplication`, `MainActivity`, NavHost |
| `baselineprofile` | Startup/key-tap/settings-nav baseline profile generation |
| `tools:readme-screenshots`, `tools:gplay-screenshots`, `tools:screenshot-mocks` | Screenshot generation tooling, not shipped |

Dependency direction: `feature` depends on `core`, never the reverse.
A `core` module must not reference any `feature` module. `app` may depend on both.

## Product flavors

`app` builds two flavors on the `distribution` dimension,
both sharing the applicationId `dev.thomas_kiljanczyk.openpiano`:

- `foss` — no Firebase dependency of any kind. Default for local dev and most CI jobs (`assembleFossDebug`, `testFossDebugUnitTest`, ...).
  Needs an inert stub `app/src/foss/google-services.json` purely to satisfy the Google Services Gradle plugin's per-variant task,
  which runs unconditionally regardless of which flavor links Firebase.
- `play` — adds Firebase Analytics (automatic collection only — active users, country, city;
  no custom events, no advertising ID) and Firebase Crashlytics (crash + NDK native-crash reports,
  disabled in debug builds). Requires a real `app/src/play/google-services.json` (gitignored,
  not committed) from your own Firebase project.

Flavor-specific code lives under `app/src/play/kotlin/...` and `app/src/foss/kotlin/...`.
Both provide a Hilt `di/AnalyticsModule.kt` binding `core:analytics`'s `AnalyticsHelper` interface — `play` to a `FirebaseAnalyticsHelper` wrapping `FirebaseAnalytics`,
`foss` to a `NoOpAnalyticsHelper`. `core:analytics` itself has zero Firebase dependency;
only `app`'s `play` source set touches the Firebase SDK.

## Conventions

- General Kotlin/Compose/testing conventions live in `docs/guidelines/`;
  this file covers OpenPiano-specific and hot-path rules only.
- **Comments: default to none.** Write one only for a hidden constraint, a workaround,
  or a non-obvious invariant. No restating identifiers, no section-header comments,
  no `@param`/`@return` that repeats the parameter name.
  `comments-house > ExcessiveComment` fails the build above 4 lines — that is a backstop, not the bar.
- Never write a comment narrating the conversation that produced a change.
  `style > ForbiddenComment` rejects `as requested`, `per the user`, `phase N`.
- detekt is the sole Kotlin style gate. ktlint runs inside it (ruleset id `ktlint` in detekt 2.x).
  No Spotless. No standalone ktlint plugin.
- **No detekt baseline, ever.** Tune the config or fix the source; never freeze a finding.
- Release builds are signed only from `android/keystore.properties` or `RELEASE_*` env vars; never commit a keystore.
- Commit messages follow Conventional Commits v1.0.0. Scope optional.
- Never disable a quality gate without a one-line reason stating why.
- Never force-push `main`.

## Hot path — `core:audio`

These rules are non-negotiable; violating them costs latency or produces glitches that no test will catch.

- Nothing inside `onAudioReady` may allocate, take a lock, sleep, or do I/O.
  That includes anything that transitively does so.
- Every `tsf_*` call happens on the audio thread, inside `onAudioReady`, after draining the event queue.
  TSF is then touched by exactly one thread and needs no mutex.
- The event queue is single-producer/single-consumer: UI thread writes, audio thread reads.
  Fixed power-of-two capacity. Full queue drops the event and bumps a counter; it never blocks.
- `AudioEngine.noteOn` / `noteOff` are synchronous JNI calls that only enqueue.
  The Compose pointer handler calls them **directly** — never through a coroutine, a `Flow`, or a ViewModel.
  A dispatcher hop or a recomposition in the touch-to-sound path is a bug.
- SoundFont loading (file I/O, Vorbis decode) happens on a background thread with the stream stopped.

## C++ style

`core/audio/src/main/cpp` is outside detekt.
It is formatted with clang-format using `android/.clang-format`: LLVM base, 4-space indent,
100-column limit, pointer alignment left.

`core/audio/src/main/cpp/third_party/` is excluded from formatting and must stay byte-identical to upstream.
Record the upstream commit rather than editing a vendored file.

## Verification

```
cd android && ./gradlew assembleFossDebug testFossDebugUnitTest :core:model:test detekt lint
```

`:app` has two flavors (see "Product flavors" above),
so unqualified `build`/`test`/`lint`/`detekt` tasks now assemble/check both — the flavor-qualified form above is the fast local loop.
Both must pass before a change is considered done.
