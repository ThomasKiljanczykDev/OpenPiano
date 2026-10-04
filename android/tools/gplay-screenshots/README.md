# gplay-screenshots

Renders localized Play Store phone screenshots, one set per fastlane locale: keyboard landscape (1890x1063), settings portrait (1063x1890).

```
cd android && ./gradlew :tools:gplay-screenshots:updateDebugScreenshotTest
tools/gplay-screenshots/export_to_fastlane.sh
```

`PlayLocalePreviews` preview names are fastlane locale directories.
`Screenshot<N><Screen>` sets listing order.
Output lands in `fastlane/metadata/android/<locale>/images/phoneScreenshots/` (gitignored; `cd-store-listing.yml` renders it in CI).
