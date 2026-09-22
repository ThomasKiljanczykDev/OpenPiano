![OpenPiano](docs/images/OpenPiano-banner.png "OpenPiano")

<img src="docs/images/google-play-badge-unavailable.png" height="60" alt="Not yet available on Google Play">
<img src="docs/images/fdroid-badge-unavailable.png" height="60" alt="Not yet available on F-Droid">
<a href="https://buymeacoffee.com/thomas.kiljanczyk.dev"><img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-FFDD00?logo=buymeacoffee&logoColor=black" height="45" alt="Buy me a coffee"></a>

[![CI (Android)](https://github.com/ThomasKiljanczykDev/OpenPiano/actions/workflows/ci-android.yml/badge.svg?branch=main)](https://github.com/ThomasKiljanczykDev/OpenPiano/actions/workflows/ci-android.yml)
[![Publish privacy policy](https://github.com/ThomasKiljanczykDev/OpenPiano/actions/workflows/cd-privacy-policy.yml/badge.svg?branch=main)](https://github.com/ThomasKiljanczykDev/OpenPiano/actions/workflows/cd-privacy-policy.yml)
[![License: GPL v3+](https://img.shields.io/badge/License-GPLv3%2B-blue.svg)](LICENSE)

# OpenPiano

An open source touchscreen piano keyboard app with low-latency sound and USB MIDI output,
so it can also drive external gear or a DAW.

No ads, no in-app purchases, no account.
It exists because every "free" piano app I tried interrupted playing with full-screen ads.

## Not affiliated

This project is unrelated to these other "OpenPiano" projects:
- [OpenPiano — Virtual Piano for Windows](https://sourceforge.net/projects/openpiano/) (Justagwas)
- [OpenPiano JUCE piano engine](https://github.com/michele-perrone/OpenPiano) (michele-perrone)

## Overview

See the [Android app](android) for build instructions, the module map, and screenshots.

## Privacy

The Play Store build (`play` flavor) uses Firebase Analytics for aggregate usage metrics (active users, country,
city) via Firebase's automatic collection only — no custom events,
no advertising ID collection — and Firebase Crashlytics for crash reports (disabled in debug builds).
The `foss` flavor, used everywhere else, has neither.

The full policy is in [PRIVACY.md](PRIVACY.md).

## Releasing

[docs/RELEASING.md](docs/RELEASING.md) is the release runbook,
covering the version bump, the signing keystore and the GitHub Actions release workflow.
Store listing text and images live under [fastlane/metadata](fastlane/metadata).

## License

OpenPiano is licensed under the GNU General Public License v3.0 or later — see [LICENSE](LICENSE).
Third-party components and their licenses are listed in [NOTICE.md](NOTICE.md).

## Attribution

The bundled sound bank `android/core/audio/src/main/assets/piano.sf3` is derived from "Salamander Grand Piano V3" by Alexander Holm,
licensed under [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/).

Full provenance, including the conversion steps and the upstream license text, is recorded in `android/core/audio/src/main/assets/LICENSE.txt`.
The scripts that produce the SF3 from the upstream source live in `android/tools/soundfont/`.
