![OpenPiano](../docs/images/OpenPiano-banner.png "OpenPiano")

<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="70" alt="Get it on Google Play" style="filter: grayscale(100%); opacity: 0.6;">

[<img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?logo=buymeacoffee&logoColor=black" height="40" alt="Buy me a coffee">](https://buymeacoffee.com/thomas.kiljanczyk.dev)

**Coming soon on Google Play**

# OpenPiano Android Client

Android app module for OpenPiano — see the [root README](../README.md) for what it is.

## Features

### Keyboard

Touch the keys to play. The overview strip above the keys doubles as a scrollbar,
so you can jump anywhere across the full 88-key range.

<p float="left">
  <img src="../docs/images/OpenPiano-keyboard-light.png" alt="Keyboard - light theme" height="360">
  <img src="../docs/images/OpenPiano-keyboard-dark.png" alt="Keyboard - dark theme" height="360">
</p>

### Settings

Key labels, visible key count, reverb, MIDI output, and touch hit-testing mode (point vs. area,
with a configurable overlap threshold) are all user-adjustable.

<img src="../docs/images/OpenPiano-settings.png" alt="Settings" height="480">

## Build

The Gradle build lives in `android/`.

```
cd android
./gradlew assembleDebug
./gradlew installDebug          # run on a connected device
```

Requires JDK 17 and the Android SDK with the NDK installed (native code is built as part of `assemble`).
See [AGENTS.md](../AGENTS.md#verification) for the full verification commands (detekt, lint, tests).

## Module structure

See [AGENTS.md](../AGENTS.md#modules) for the module map and dependency rules.
