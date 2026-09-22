# AI-assisted development

OpenPiano is a solo hobby project with no backend, no user accounts,
and no cloud infrastructure — these rules are scoped to what actually exists here: a Gradle/Android build,
a signed release artifact, and a vendored soundfont with its own license.

## Never

- Commit a signing keystore or `keystore.properties`, if/when they exist.
- Commit `local.properties`.
- Disable a quality gate (detekt, lint, a test) without a one-line reason stating why — see `AGENTS.md`.
- Add a detekt baseline. `AGENTS.md` already forbids this outright: tune the config or fix the source.

## Ask first

- Force-pushing any branch.
- Changing files under `.github/` (CI workflows).
- Adding a new dependency to the version catalog.
- Changing the SF3 soundfont asset or its licensing metadata (see PLAN.md §2.6 — the bundled Salamander Grand Piano sample is CC BY 3.0 and requires attribution).

## Always

- Follow Conventional Commits v1.0.0 for commit messages.
- Run `cd android && ./gradlew build detekt lint` before claiming a change is done.
- Prefer editing an existing file over creating a new one.
