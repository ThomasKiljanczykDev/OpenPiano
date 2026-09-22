# readme-screenshots

Renders `@Preview` composables from `core:ui`, `core:designsystem`, `feature:keyboard:impl`,
and `feature:settings:impl` to reference PNGs, for use in the project README.

Run:

```
cd android && ./gradlew :tools:readme-screenshots:updateDebugScreenshotTest
```

This renders every composable annotated with both `@Preview` and `@PreviewTest` (from `screenshot-validation-api`) to PNG under `src/screenshotTestDebug/reference/`.
Hand-copy the renders you want into the root `docs/images/` for embedding in the root README.
