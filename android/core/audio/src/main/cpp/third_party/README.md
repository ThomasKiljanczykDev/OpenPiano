# Vendored third-party sources

Both files come from <https://github.com/schellingb/TinySoundFont> at commit
`853a0a171759f1ddba0de1442133a75912bbeffa` (2026-07-19), unmodified.

| File | Origin | Licence |
|---|---|---|
| `tsf.h` | TinySoundFont | MIT |
| `stb_vorbis.c` | stb, redistributed by TinySoundFont | public domain / MIT |

`stb_vorbis.c` must be included **before** `tsf.h`; `tsf.h` gates its SF3
(Ogg/Vorbis) sample decoding on `STB_VORBIS_INCLUDE_STB_VORBIS_H`. Without it a
compressed soundfont loads and renders silence.
