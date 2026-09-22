# banner

Source and render pipeline for the OpenPiano marketing banner.

`banner.svg` is the single source of truth: a 1024x500 viewBox SVG (the Play Store feature-graphic aspect ratio)
depicting the "OpenPiano" wordmark in Ivory over a solid Ebony background,
with a horizontal row of piano keys along the bottom (white keys in Ivory,
black keys near-black with an AshGray outline), using the same hex values as `core:designsystem`'s `Color.kt`/`KeyColors.kt`.
Unlike the launcher icon's diagonal key strip,
this uses a horizontal strip — a better fit for the wide landscape aspect ratio.

Run:

```
android/tools/banner/render_banner.sh
```

Requires `rsvg-convert` (`brew install librsvg`). It regenerates, overwriting in place,
`docs/images/OpenPiano-banner.png` (README banner, 1024x500),
`fastlane/metadata/android/en-US/images/featureGraphic.png` (Play feature graphic, 1024x500) and
`docs/images/OpenPiano-social-preview.png` (GitHub social preview, 1280x640 — upload it by hand
under Settings > General > Social preview; the REST API exposes no endpoint for it).

After editing `banner.svg`, just re-run the script; it overwrites the generated PNG in place.
