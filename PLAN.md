# OpenPiano — v1 Plan (piano keyboard)

Hand-off document. Every technical claim below is cited. Do not substitute unverified alternatives.

## 1. Decisions (locked)

| # | Decision | Choice |
|---|---|---|
| 1 | Repo layout | Full LyricCast shape, `android/` nested under repo root |
| 2 | MIDI in signal path | No. MIDI-shaped **in-process** bus (lock-free SPSC ring buffer). No `MidiService` IPC in the hot path |
| 3 | Synth engine | TinySoundFont (`tsf.h`), MIT, vendored |
| 4 | Sound bank | Salamander Grand Piano **SF3** (~5 MB) + vendored `stb_vorbis.c` |
| 5 | External MIDI hardware | v2. Bus is MIDI 1.0 from day one; `core:midi` not built in v1 |
| 6 | minSdk | **26** — floor set by `SharingMode::Exclusive`; the Android 13 allowance is not needed (§3.1) |
| 7 | Audio output | Oboe 1.10.0, `PerformanceMode::LowLatency` + `SharingMode::Exclusive` |
| 8 | Touch | Single container-level `pointerInput`, Canvas-rendered keys |
| 9 | Settings | Proto DataStore |
| 10 | Velocity | Fixed `100` in v1; Y-position velocity deferred |

## 2. Grounding

### 2.1 Android provides no software synthesizer
`android.media.midi` is transport and routing only. A synth must be supplied by the app.
- <https://developer.android.com/ndk/guides/audio/midi>
- <https://midi.org/developing-midi-applications-on-android>

### 2.2 Why MIDI is not the transport
`MidiService` is a centralized broker process; all virtual-device traffic crosses it via binder IPC.
Both endpoints here are in one app, so the IPC buys nothing and costs latency and jitter.
- <https://developer.android.com/reference/android/media/midi/MidiDeviceService>
- <https://developer.android.com/reference/android/media/midi/package-summary>

For v2,
AMidi is the adapter: `AMidiOutputPort_receive` is non-blocking and documented safe to call inside an audio callback.
`AMidiInputPort_send` **blocks** — never call it from the audio thread.
- <https://developer.android.com/ndk/guides/audio/midi>

### 2.3 Oboe
- 1.10.0, Apache-2.0, `com.google.oboe:oboe:1.10.0`, ships as AAR with prefab.
- AAudio + MMAP on API 27+; `SharingMode::Exclusive` API 26+.
- Oboe sets buffer size to 2× burst automatically.
- Forbidden in `onAudioReady()`: allocation, file/network I/O, mutexes, sleep, stopping/closing the stream,
  `read()`/`write()` on the invoking stream.
- <https://github.com/google/oboe/blob/main/docs/FullGuide.md>
- <https://github.com/google/oboe/blob/main/docs/GettingStarted.md>
- <https://github.com/google/oboe/releases>

Measured round-trip latency (Google, OboeTester).
**Sample rate is the dominant term** — do not hardcode 44100:

| Configuration | Latency |
|---|---|
| All recommendations followed | 20 ms |
| Shared (not exclusive) mode | 26 ms |
| 44100 Hz via AAudio | 160 ms |
| 44100 Hz via Oboe SRC | 23 ms |
| Performance mode not LowLatency | 205 ms |
| Maximum buffer size | 53 ms |

- <https://developer.android.com/games/sdk/oboe/low-latency-audio>

### 2.4 TinySoundFont real-time safety
From `tsf.h`'s own header comment:

> "Your audio output which calls the tsf_render* functions will most likely run on a different thread than where the playback tsf_note* functions are called.
> In which case some sort of concurrency control like a mutex needs to be used so they are not called at the same time. Alternatively,
> you can pre-allocate a maximum number of voices [...] That way memory re-allocation will not happen during tsf_note_on and TSF should become mostly thread safe."

With `tsf_set_max_voices` set,
`tsf_note_on` does **not** allocate — it reuses a free voice or kills the voice furthest into release.
Without it, TSF grows the voice array via `TSF_REALLOC` in increments of 4.

**This design removes the caveat entirely**: events are drained and all `tsf_*` calls are made *inside* the audio callback,
so TSF is touched by exactly one thread.

- <https://github.com/schellingb/TinySoundFont>
- <https://raw.githubusercontent.com/schellingb/TinySoundFont/main/tsf.h>

### 2.5 SF3
SF3 = SF2 with Ogg/Vorbis-compressed samples.
`tsf.h` decodes it only if `stb_vorbis.c` is included first:

```c
#include "stb_vorbis.c"
#define TSF_IMPLEMENTATION
#include "tsf.h"
```

Gated internally on `#ifdef STB_VORBIS_INCLUDE_STB_VORBIS_H`. Without it,
compressed samples are silently skipped (`shdr->start = shdr->end = ...
= 0`) — a soundfont that loads but is silent.

Samples are **fully decompressed into RAM at load**. SF3 reduces download size, not runtime memory.
Budget ~25 MB heap and 1–2 s of first-load decode.

### 2.6 Sound bank licensing
Salamander Grand Piano: **CC BY 3.0** (Alexander Holm).
The public-domain claim below did not survive checking — the upstream repository's README and LICENSE both state CC BY 3.0 Unported,
so the app must carry the attribution. See §11.1. `SalamanderC5-Lite` SF2 ≈ 24.5 MB,
7 velocity layers (ppp/pp/p/mp/mf/f/ff-fff).
- <https://github.com/sfzinstruments/SalamanderGrandPiano>
- <https://sites.google.com/view/hed-sounds/salamander-c5-light>

**Action required before coding:** obtain or produce an SF3 build and verify its license file ships in the repo.
If no public-domain SF3 exists,
convert the public-domain SF2 with Polyphone and record the provenance in `android/core/audio/src/main/assets/LICENSE.txt`.

### 2.7 Play size limits
200 MB compressed download per device configuration. Not a constraint at ~5 MB.
- <https://support.google.com/googleplay/android-developer/answer/9859372>

## 3. Configuration

### 3.1 minSdk stays 26
Nothing in the stack requires API 33:

| Requirement | Min API |
|---|---|
| AAudio + MMAP (via Oboe) | 27 |
| `SharingMode::Exclusive` | 26 |
| `android.media.midi` (MIDI 1.0) | 23 |
| TinySoundFont | none |

On API 26 Oboe falls back to OpenSL ES; AAudio MMAP applies from API 27.

API 33 adds MIDI **2.0 on the USB transport only**; `MidiUmpDeviceService` is API 35 (Android 15).
Both are out of scope per decision 5. Raising minSdk would cut device reach for nothing.
- <https://source.android.com/docs/core/audio/midi>
- <https://midi.org/community/midi-news-stories/android-13-developer-preview-2-support-for-midi-2-0>

Revisit only if v2 targets MIDI 2.0.

### 3.2 Identifiers
- namespace / applicationId: `dev.thomas_kiljanczyk.openpiano`
- Convention plugin prefix: `openpiano.*`
- build-logic group: `dev.thomas_kiljanczyk.openpiano.buildlogic`

Delete the `com.example.openpiano` template package.

### 3.3 Toolchain
Adopt LyricCast's version catalog, pruned. Keep OpenPiano's newer AGP and Gradle.

| | Value | Source |
|---|---|---|
| AGP | 9.4.0 | keep OpenPiano's |
| Gradle | 9.6.0 | keep OpenPiano's wrapper |
| Kotlin | 2.4.10 | LyricCast |
| KSP | 2.3.11 | LyricCast |
| Hilt | 2.60.1 | LyricCast |
| detekt | 2.0.0-alpha.6 | LyricCast |
| compileSdk / targetSdk | 37 | both |
| JVM toolchain / target | 17 | LyricCast |

Drop from the catalog: Room, Cast, Nearby, Firebase, protobuf-*, zip4j, zstd, coil, reorderable, mediarouter, exifinterface, baselineprofile, screenshot.
Add: `oboe`.

Keep from LyricCast `gradle.properties`: `org.gradle.parallel`, `caching`, `configuration-cache`, `configuration-cache.parallel`, `tooling.parallel`, `jvmargs=-Xmx8192m`.
Drop `android.experimental.enableScreenshotTest`.

## 4. Target tree

```
/
  AGENTS.md                       # ported, OpenPiano-specific
  CLAUDE.md                       # "@AGENTS.md"
  README.md
  PLAN.md
  .gitignore
  .github/
    workflows/ci-android.yml
    actions/android-setup/action.yml
  android/
    settings.gradle.kts           # TYPESAFE_PROJECT_ACCESSORS, includeBuild("build-logic")
    build.gradle.kts
    gradle.properties
    gradlew  gradlew.bat
    gradle/wrapper/               # existing, 9.6.0
    gradle/libs.versions.toml
    gradle/gradle-daemon-jvm.properties
    build-logic/
      settings.gradle.kts
      convention/
        build.gradle.kts
        src/main/kotlin/
          AndroidApplicationConventionPlugin.kt
          AndroidApplicationComposeConventionPlugin.kt
          AndroidLibraryConventionPlugin.kt
          AndroidFeatureConventionPlugin.kt
          AndroidHiltConventionPlugin.kt
          AndroidNativeConventionPlugin.kt        # NEW
          ComposeLibraryConventionPlugin.kt
          KotlinQualityConventionPlugin.kt
          dev/thomas_kiljanczyk/openpiano/buildlogic/Detekt.kt
    config/detekt/detekt.yml
    config/detekt-rules/                          # ExcessiveComment house rule
    app/
    core/common/
    core/model/
    core/designsystem/
    core/ui/
    core/datastore-proto/
    core/data/
    core/audio/                                   # NEW — native
    core/testing/
    feature/keyboard/impl/
```

Not ported in v1: `baselineprofile`, `tools/readme-screenshots`, Firebase/Crashlytics/google-services, Room, Cast, Nearby, `core/domain`, `core/session`, `core/sync`, `core/tutorial`.
CI `android-setup` action drops the `google-services.json` stub step accordingly.

### 4.1 Module responsibilities

| Module | Contents |
|---|---|
| `core:model` | Pure Kotlin. `MidiMessage`, `Note`, `KeyboardLayout`, `KeyLabelMode`. No Android deps |
| `core:common` | Dispatcher qualifiers, `@ApplicationScope` |
| `core:designsystem` | Theme, color, type, key-color tokens |
| `core:ui` | Shared composables |
| `core:datastore-proto` | `user_preferences.proto` + serializer |
| `core:data` | `UserPreferencesRepository` over DataStore |
| `core:audio` | Oboe + TSF + JNI. `AudioEngine` interface, `OboeAudioEngine`, Hilt module, SF3 asset |
| `feature:keyboard:impl` | Compose keyboard, `KeyboardViewModel`, navigation |
| `app` | `OpenPianoApplication`, `MainActivity`, NavHost |

## 5. `core:audio`

### 5.1 Gradle

```kotlin
plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.native)
    alias(libs.plugins.openpiano.android.hilt)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.audio"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.oboe)
}
```

`AndroidNativeConventionPlugin` sets:
- `buildFeatures.prefab = true`
- `externalNativeBuild.cmake { path = "src/main/cpp/CMakeLists.txt" }`
- `defaultConfig.externalNativeBuild.cmake.arguments += "-DANDROID_STL=c++_shared"`
- `ndk.abiFilters = ["arm64-v8a", "armeabi-v7a", "x86_64"]`
- 16 KB page alignment for Android 15+: verify AGP/NDK defaults produce it;
  add `-Wl,-z,max-page-size=16384` if not. Oboe ≥1.9.3 is already 16 KB-compatible.

CMake:
```cmake
find_package(oboe REQUIRED CONFIG)
target_link_libraries(openpiano_audio oboe::oboe)
```

### 5.2 Native files — `core/audio/src/main/cpp/`

```
CMakeLists.txt
third_party/tsf.h            # vendored, MIT, unmodified, with upstream commit noted
third_party/stb_vorbis.c     # vendored, public domain
MidiMessage.h
LockFreeQueue.h
SynthEngine.h / .cpp
AudioEngine.h / .cpp
jni_bridge.cpp
```

### 5.3 Design rules — non-negotiable

1. **One writer, one reader.** UI thread writes the queue; audio thread reads it. SPSC only.
2. **Power-of-two capacity** (1024), fixed at construction. `std::atomic<uint32_t>` head/tail,
   release on write / acquire on read. Queue full ⇒ drop the event and bump a counter; never block.
3. **Every `tsf_*` call happens on the audio thread**, inside `onAudioReady`, after draining the queue.
   No mutex anywhere in the audio path.
4. `tsf_set_max_voices(tsf, 64)` immediately after load, **before** the stream starts.
5. SoundFont loading (file I/O + Vorbis decode) happens on a background thread with the stream stopped.
   v1 loads once at startup; no hot-swap.
6. Nothing in `onAudioReady` allocates, locks, sleeps, or does I/O.

### 5.4 MIDI message encoding
Packed `int32`: `(status << 16) | (data1 << 8) | data2`. Status is MIDI 1.0
(`0x90` note-on, `0x80` note-off) with channel in the low nibble.
This is the exact shape a v2 AMidi adapter emits, so the bus needs no change when external hardware lands.

### 5.5 Stream configuration

```cpp
oboe::AudioStreamBuilder builder;
builder.setPerformanceMode(oboe::PerformanceMode::LowLatency)
       ->setSharingMode(oboe::SharingMode::Exclusive)
       ->setFormat(oboe::AudioFormat::Float)
       ->setChannelCount(oboe::ChannelCount::Stereo)
       ->setUsage(oboe::Usage::Game)
       ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
       ->setDataCallback(this)
       ->setErrorCallback(this);
```

- Do **not** call `setSampleRate` — take the device native rate,
  then pass `stream->getSampleRate()` to `tsf_set_output`. Forcing 44100 costs ~140 ms (§2.3).
- `Usage::Game` is Google's documented recommendation for lowest latency.
  If this proves wrong for a music app on real hardware, re-measure before changing it.
- Implement `onErrorAfterClose` to rebuild the stream — exclusive streams disconnect more readily (headphone unplug,
  route change). Losing audio on a headphone unplug is a v1 bug, not a v2 polish item.
- Log `getXRunCount()` in debug builds.

### 5.6 Callback

```cpp
DataCallbackResult onAudioReady(AudioStream*, void* audioData, int32_t numFrames) {
    MidiMessage msg;
    while (queue_.pop(msg)) {
        applyToSynth(msg);          // tsf_channel_note_on / tsf_channel_note_off
    }
    tsf_render_float(tsf_, static_cast<float*>(audioData), numFrames, /*flag_mixing=*/0);
    return DataCallbackResult::Continue;
}
```

Timing granularity is one burst (~2–4 ms at 48 kHz with double buffering) — below the perceptual threshold.
Sample-accurate sub-block rendering (split the block at event boundaries,
render additively with `flag_mixing=1`) is a deferred refinement;
it requires timestamps on the queue and is only worth it once external MIDI arrives.

### 5.7 Kotlin surface

```kotlin
interface AudioEngine {
    fun start()
    fun stop()
    fun noteOn(note: Int, velocity: Int)
    fun noteOff(note: Int)
}
```

`noteOn`/`noteOff` are **synchronous JNI calls that only enqueue**.
They must never be routed through a coroutine, `Flow`,
or the ViewModel — that would put a dispatcher hop and a recomposition in the latency path.
The UI's pointer handler calls the engine directly.

Lifecycle: `start()` on the process `ON_START`, `stop()` on the process `ON_STOP` (`ProcessLifecycleOwner`), never per-activity.
Engine is an `@Singleton`; the native engine and the decoded soundfont live for the process and are freed at process death.

## 6. `feature:keyboard:impl`

### 6.1 Rendering
One `Canvas` draws all keys.
Do not compose a `Box` per key — hundreds of composables recomposing per touch will miss frames and fight over pointer ownership.

Read pressed-note state **inside** the draw lambda so only the draw phase invalidates, not composition.

Layout:
- White keys: uniform width, full height.
- Black keys: width ≈ 0.6 × white, height ≈ 0.62 × white,
  centered on the boundary between their two white neighbours (C♯ D♯ · F♯ G♯ A♯).
  Real pianos offset these slightly; v1 uses centered.
- Hit test **black keys first**, then white.

### 6.2 Touch

Single `Modifier.pointerInput` on the container:

```kotlin
awaitPointerEventScope {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        for (change in event.changes) {
            when {
                change.changedToDownIgnoreConsumed() -> { /* map pointerId -> note, noteOn */ }
                change.changedToUpIgnoreConsumed()   -> { /* noteOff, unmap */ }
                change.positionChanged()             -> { /* glissando: noteOff old, noteOn new */ }
            }
            change.consume()
        }
    }
}
```

- State is a `MutableMap<PointerId, Int>` (pointer → MIDI note).
- **Multi-touch** falls out of one map entry per `PointerId`.
- **Press & hold** falls out of note-on/note-off semantics: the note sustains until the pointer lifts.
  No timer, no long-press detector.
- Do **not** use `detectTapGestures` — it consumes and is effectively single-pointer.
- `change.consume()` stops ancestor scroll containers from stealing the gesture.
- Guard against a pointer that lifts outside the composable leaving a stuck note.

Reference: <https://developer.android.com/develop/ui/compose/touch-input/pointer-input>

### 6.3 Configurable UI (mimicking existing apps)

v1:
- Visible white-key count, 7–21, default 10. Slider in settings.
- Overview strip above the keyboard: the whole 88-key piano drawn small,
  with the played window outlined over it. Touch or drag centres the window on the finger.
- Shift buttons flanking the overview: outer `«` / `»` move one octave (7 white keys),
  inner `‹` / `›` move one white key. Anchor clamped so the window stays inside A0–C8.
- Key labels: off / C-only / all note names.
- Landscape-first; keyboard fills width.

Deferred to v2: two-row split keyboard, sustain latch, Y-position velocity, pitch bend, instrument picker.

### 6.4 Settings

`user_preferences.proto`:
```proto
message UserPreferences {
  reserved 2;                     // was lowest_octave, replaced by lowest_note
  int32 visible_white_keys = 1;   // default 10
  KeyLabelMode label_mode  = 3;   // default LABEL_C_ONLY
  int32 lowest_note        = 4;   // MIDI note of the leftmost white key, default C3
}
```

Proto DataStore, repository in `core:data`, exposed as `Flow`, collected by `KeyboardViewModel`.
Settings changes affect layout only — never the audio path.

## 7. Execution order

1. **Restructure.** Move the existing scaffold to `android/`. Add `build-logic`, convention plugins, `config/detekt`, `config/detekt-rules`,
   root `AGENTS.md`/`CLAUDE.md`/`README.md`, `.github/`. Rename package to `dev.thomas_kiljanczyk.openpiano`.
   Rewrite the catalog. Verify: `./gradlew build detekt lint`.
   **Commit — the repo currently has zero commits.**
2. **Empty modules.** Create every module in §4 with its `build.gradle.kts` and namespace,
   wired to convention plugins. Verify `./gradlew build`.
3. **`core:audio` — silence.** Oboe stream open/close, Float/Stereo/native-rate/LowLatency/Exclusive,
   callback writes zeros. JNI bridge, lifecycle. Verify: opens on device, log the negotiated rate,
   burst size, and whether Exclusive was actually granted (`getSharingMode()` — the request can be downgraded).
4. **`core:audio` — sound.** Vendor `tsf.h` + `stb_vorbis.c`. Load the SF3 from assets. Ring buffer. Drain-and-render.
   Verify: a hardcoded note plays.
5. **`feature:keyboard:impl` — layout.** Canvas keys, correct geometry, no audio.
6. **Wire touch.** Multi-touch map, direct engine calls, pressed-state rendering.
7. **Config.** Proto DataStore, octave switching, visible-key count, labels.
8. **Measure.** OboeTester on a real device. Confirm Exclusive granted, native rate, low XRun count.
   Record the measured round-trip latency in `README.md`.

Steps 3 and 4 are the risk. Do not start step 5 until a note sounds.

## 8. Testing

| Layer | Approach |
|---|---|
| Key geometry / hit testing | JVM unit tests on a pure-Kotlin `KeyboardLayout` in `core:model`. Extract the math so it is testable without Compose |
| MIDI encoding | JVM unit tests on pack/unpack |
| Ring buffer | C++ test, or JNI-driven instrumented test asserting no drops under load |
| Multi-touch | Compose instrumented test with `performMultiModalInput` / multi-pointer `performTouchInput`; assert engine calls via a fake `AudioEngine` |
| Settings repository | JVM test over a test DataStore (LyricCast pattern) |
| Audio output | Manual. No automated assertion on sound in v1 |

CI ports LyricCast's `ci-android.yml`: `lint` and `detekt` in parallel, then `assemble`, then `unit-tests`,
then a GMD instrumented matrix. Drop the readme-screenshots step and the `google-services.json` stub.
NDK build increases `assemble` time — expect the cache to matter.

## 9. Conventions (inherited from LyricCast AGENTS.md)

- **Comments: default to none.** Only for a hidden constraint, workaround, or non-obvious invariant.
  No restating identifiers, no section headers, no `@param`/`@return` that repeat the name.
  `comments-house > ExcessiveComment` fails the build above 4 lines — a backstop, not the bar.
- Never write a comment narrating the conversation that produced the change.
  `style > ForbiddenComment` rejects `as requested`, `per the user`, `phase N`.
- detekt is the sole Kotlin style gate, ktlint inside it. No Spotless, no standalone ktlint.
- **No detekt baseline, on purpose.** Tune the config or fix the source; never freeze a finding.
- Conventional Commits v1.0.0, scope optional.
- Never disable a quality gate without a one-line reason. Never force-push `main`.

The C++ in `core:audio` is outside detekt. Adopt clang-format and state the style in `AGENTS.md`;
otherwise it will drift.

## 10. Open items for the implementer (resolved — see §11)

1. Source a public-domain Salamander **SF3** and ship its license file.
   Converting the PD SF2 with Polyphone is the fallback. This blocks step 4.
2. Confirm `Usage::Game` beats `Usage::Media` on real hardware before locking it in.
3. Confirm AGP 9.4.0 + the pinned NDK emit 16 KB-aligned `.so` files without an explicit linker flag.
4. Decide the SF3 load-time UX (~1–2 s decode): splash, or keyboard visible but muted with an indicator.
5. Verify LyricCast's detekt 2.0.0-alpha.6 + `detekt-rules-ktlint-wrapper` resolves against Kotlin 2.4.10 in this build before porting `config/detekt-rules` wholesale.

## 11. Resolutions

Recorded during implementation. Each supersedes the corresponding open item in §10.

### 11.1 Sound bank
No public-domain Salamander SF3 exists.
`android/core/audio/src/main/assets/piano.sf3` (2.36 MB) was built from the CC BY 3.0 Salamander Grand Piano V3 FLAC samples: 30 pitches,
velocity layer 13 of 16, mono, register-dependent truncation with a 0.3 s fade, Ogg/Vorbis via libsndfile,
assembled into a hand-written sfbk by `android/tools/soundfont/build_sf3.py`.
Attribution ships in `assets/LICENSE.txt` and `README.md`.

`tsf_riffchunk_read` does not skip the RIFF word-alignment pad byte,
so every chunk payload in the generated font is padded to even length with the pad counted in the chunk size.
An odd-sized `smpl` chunk makes `tsf_load_filename` return NULL with no error.

### 11.2 `Usage::Game`
Unverified against `Usage::Media` on hardware. Kept as written; §5.5 stands.

### 11.3 16 KB page alignment
No linker flag needed. AGP 9.4.0 with NDK 29.0.14206865 emits `LOAD` segments aligned to 0x4000.

### 11.4 SF3 load-time UX
Keyboard is visible and interactive immediately;
a `LinearProgressIndicator` shows until `AudioEngine.isReady` flips. No splash.

### 11.5 detekt 2.0.0-alpha.6
Resolves against Kotlin 2.4.10,
but the group is `dev.detekt` and the config schema changed: `build:` and `formatting:` are gone,
the ktlint ruleset id is `ktlint`, `LongParameterList` and `TooManyFunctions` renamed their thresholds,
and integer rule options must be quoted strings.
`config/detekt-rules` (the `ExcessiveComment` house rule) was not ported — the detekt 2.x rule API differs from 1.x and the rule is a backstop,
not the bar (§9).

### 11.6 AGP 9 built-in Kotlin
AGP 9 applies the Kotlin plugin itself and rejects an explicit `org.jetbrains.kotlin.android`,
so the convention plugins apply only the AGP plugin. `CommonExtension` lost its type parameters.

### 11.7 Measured stream configuration
Pixel 9 (tokay), Android 16, debug build:

```
stream open: rate=48000 burst=96 sharing=Exclusive perf=LowLatency format=Float
AudioEngineStats(sampleRate=48000, framesPerBurst=96, exclusive=true, xRunCount=0, droppedEvents=0)
```

Exclusive was granted, not downgraded.
Round-trip latency via OboeTester is still unmeasured (§7 step 8).

## 12. v2 — MIDI Output (USB)

v1's scope table (§1,
decisions 2 and 5) deferred external MIDI hardware entirely — no `MidiService` IPC in the audio hot path,
`core:midi` not built.
v1 (the in-app keyboard and TinySoundFont synth) is now complete enough to build on top of,
and this section starts v2: the Android device sends MIDI 1.0 out over USB to a DAW running on a PC.
BLE MIDI is wanted too but is a separate, later plan — this section covers USB only.

**Transport.** USB peripheral mode,
exposed by the kernel's USB gadget MIDI function once the phone is cabled to a host — system-level,
independent of any app, no `MidiDeviceService` declaration needed.
The device it creates shows up to on-device apps as a `MidiDeviceInfo` of `type == TYPE_USB` ("Android USB Peripheral Port").
`UsbMidiOutputPort` is a `MidiManager` **client**: it registers a `MidiManager.DeviceCallback`,
opens that `TYPE_USB` device,
and writes into its input port — the framework relays those writes out over the physical USB cable to whatever the connected host (a DAW) has listening.
`MidiDeviceService` registers the *app* as a virtual device other apps on the phone can open;
it is not bridged to the physical USB port, so it cannot serve this role.
Confirmed working on a Pixel 9;
the Android emulator has no physical USB controller and cannot exercise this path,
so any new target device needs manual on-device confirmation. `android.software.midi` is declared `required="false"`;
devices without MIDI support fall back to the in-app synth only, detected via `PackageManager.hasSystemFeature(FEATURE_MIDI)`.

**Module.** `core:midi` — plain Kotlin/Android + Hilt, no Oboe/native dependency.
It owns `MidiOutputPort` (the transport-agnostic interface: `isConnected: StateFlow<Boolean>`,
`noteOn`/`noteOff`) and `UsbMidiOutputPort` (the only implementation so far,
a `MidiManager` client as described above).
A future `BleMidiOutputPort` slots in behind the same interface without changing its public shape.

**Threading model.** MIDI-out is a sibling consumer of the touch event,
not a tap on `core:audio`'s internal SPSC queue.
The Compose pointer handler in `feature:keyboard:impl` calls `AudioEngine.noteOn/noteOff` and `MidiOutputPort.noteOn/noteOff` independently and synchronously on the same UI thread — no dispatcher hop,
no new thread, and no interaction with the audio thread or its lock-free ring buffer.
This keeps the §2.2 warning (an `AMidi`-style blocking send must never happen on the audio thread) satisfied by construction,
since the MIDI-out path never touches that thread at all.

**Settings.** `user_preferences.proto` gained `midi_output_enabled` (bool, default false, field 6).
No output-channel selector yet — output is hardcoded to channel 0,
matching the synth's existing hardcoded `CHANNEL = 0`.
