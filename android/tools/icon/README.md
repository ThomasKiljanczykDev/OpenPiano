# icon

Source and render pipeline for the OpenPiano launcher icon.

`icon.svg` is the single source of truth: a 108x108 viewBox SVG with three top-level groups — `background`, `foreground`,
`monochrome` — matching Android's adaptive-icon layer split.
It depicts a diagonal strip of piano keys (white keys in Ivory,
black keys near-black with an AshGray outline) over a solid Ebony background,
using the same hex values as `core:designsystem`'s `Color.kt`/`KeyColors.kt`.
The monochrome group is a simplified single-color silhouette (opaque strip with thin gaps marking the white-key divisions) for the Android 13+ themed-icon variant,
since that layer is rendered as a flat-tinted alpha mask rather than shown with its own colors.

Run:

```
android/tools/icon/render_icon.sh
```

Requires `rsvg-convert` (`brew install librsvg`), `cwebp` (`brew install webp`),
and `python3` (stdlib only). It regenerates, overwriting in place:

- Per-density adaptive-icon layer PNGs (`ic_launcher_background/_foreground/_monochrome.png`) under `app/src/main/res/mipmap-{m,h,xh,xxh,xxxh}dpi/`,
  referenced by `mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`.
- Legacy fallback bitmaps `ic_launcher.webp` / `ic_launcher_round.webp` per density,
  flattening background+foreground into a square and a circle-masked composite (some OEM launchers/theme engines still read these despite minSdk 29).
- `app/src/main/ic_launcher-playstore.png`, a 512x512 composite (no masking) for the Play Store listing.
- `fastlane/metadata/android/en-US/images/icon.png`, a copy of the above for fastlane.

`compose_svg.py` is a small helper (stdlib `xml.etree` only) that flattens the `background` and `foreground` groups into one temporary SVG — optionally clipped to a circle — for the legacy/Play-Store outputs,
which don't use the adaptive-icon layer split.

After editing `icon.svg`, just re-run the script; it overwrites every generated file in place.
