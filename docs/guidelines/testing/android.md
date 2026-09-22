# Testing

## Philosophy

Prefer black-box, behavior-level tests over ones that assert on implementation details.
Test what a module promises (inputs/outputs, emitted MIDI events, layout geometry),
not how it gets there — that leaves room to refactor internals without rewriting the test suite.

## JVM unit tests

`core:model` is pure Kotlin with no Android dependency,
so it's tested with fast JVM unit tests (`src/test`) — see `core/model/src/test/kotlin/.../{NoteTest,PianoTest,KeyboardLayoutTest,MidiMessageTest}.kt`.
Other pure-Kotlin/JVM-testable logic (e.g. `core:midi`, `core:data`) follows the same pattern.

## `core:audio` hot path is exempt

The code inside `onAudioReady` (see AGENTS.md's hot-path rules) can't allocate, lock,
or do I/O — which rules out most unit-testing techniques for it directly.
It's verified manually and via instrumented smoke checks instead (see `core/audio/src/androidTest` and `NativeQueueSelfTest`/`NativeSynthProbeTest`),
not unit tests of the JNI/audio-thread code itself.

## Compose UI tests

Instrumented UI tests live under a module's `src/androidTest`, using `androidx.compose.ui.test.junit4.v2.createComposeRule`,
`Modifier.testTag` + `onNodeWithTag`/`performTouchInput` to drive interaction,
and a fake collaborator (e.g.
`core:testing`'s `FakeAudioEngine`) instead of the real `AudioEngine` to assert which notes fired without touching real audio hardware.
See `feature/keyboard/impl/src/androidTest/kotlin/.../PianoKeyboardTouchTest.kt` for the pattern.
