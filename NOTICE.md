# Third-party notices

OpenPiano is licensed under GPL-3.0-or-later (see `LICENSE`). This file
collects the licenses of the third-party works it bundles or vendors,
whose terms are compatible with but distinct from the app's own license.

## Salamander Grand Piano V3 (bundled sound bank)

`android/core/audio/src/main/assets/piano.sf3` is derived from
"Salamander Grand Piano V3" by Alexander Holm.

- License: Creative Commons Attribution 3.0 Unported (CC BY 3.0)
- License text: <https://creativecommons.org/licenses/by/3.0/legalcode>
- Upstream source: <https://github.com/sfzinstruments/SalamanderGrandPiano>
- Upstream original recording: <https://archive.org/details/SalamanderGrandPianoV3>

Full provenance, including every processing step applied to the samples
and the generated SF3, is recorded in
`android/core/audio/src/main/assets/LICENSE.txt`.

## TinySoundFont (`tsf.h`)

Vendored, unmodified, at
`android/core/audio/src/main/cpp/third_party/tsf.h`, from
<https://github.com/schellingb/TinySoundFont>, commit
`853a0a171759f1ddba0de1442133a75912bbeffa` (2026-07-19).

- License: MIT
- Copyright (c) 2017-2022 Bernhard Schelling
- Full text: see the license header inside `tsf.h` itself, or
  <https://github.com/schellingb/TinySoundFont/blob/main/LICENSE>

## stb_vorbis (`stb_vorbis.c`)

Vendored, unmodified, at
`android/core/audio/src/main/cpp/third_party/stb_vorbis.c`, redistributed
by the TinySoundFont project at the same commit as `tsf.h` above, from
<https://github.com/nothings/stb>.

- License: dual public domain (Unlicense) / MIT, author's choice
- Full text: see the license block at the end of `stb_vorbis.c` itself, or
  <https://github.com/nothings/stb/blob/master/LICENSE>

## Oboe

`core:audio` links Oboe (`com.google.oboe:oboe`) as a Gradle/prefab
dependency; it is not vendored source in this repository.

- License: Apache License 2.0
- Copyright Google LLC
- Source: <https://github.com/google/oboe>
- Full text: <https://github.com/google/oboe/blob/main/LICENSE>
