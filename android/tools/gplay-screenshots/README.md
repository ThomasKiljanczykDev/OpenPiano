# gplay-screenshots

Renders localized Play Store phone screenshots (1890x1063, one set per fastlane locale).

```
cd android && ./gradlew :tools:gplay-screenshots:updateDebugScreenshotTest
tools/gplay-screenshots/export_to_fastlane.sh
```

`PlayLocalePreviews` preview names are fastlane locale directories.
`Screenshot<N><Screen>` sets listing order.
Output lands in `fastlane/metadata/android/<locale>/images/phoneScreenshots/` (gitignored; `cd-store-listing.yml` renders it in CI).
