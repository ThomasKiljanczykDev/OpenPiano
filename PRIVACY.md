# Privacy policy — OpenPiano

Effective date: 2026-09-23

This policy covers the OpenPiano Android app,
package `dev.thomas_kiljanczyk.openpiano`, in both its `foss` and `play`
builds (see "Per-flavor difference" below). This document is the app's
full privacy policy and is intended to be linked from its Google Play
Console store listing.

## What is not collected

OpenPiano does not collect, store, or transmit:

- your name, email address, or any account identifier — the app has no
  account system and no sign-in
- audio, microphone, or camera data — the app has no recording feature
- location data
- contacts, files, or any other on-device personal data
- advertising identifiers
- any custom, in-app usage event (which key was pressed, which screen
  was opened, session length, and so on)

OpenPiano requests no runtime (dangerous) Android permissions.

## What is collected

### `foss` flavor

Nothing. The `foss` build has no analytics or crash-reporting dependency
of any kind and makes no network calls at all. It functions entirely
offline.

### `play` flavor

The build distributed on Google Play additionally includes:

- **Firebase Analytics**, using Firebase's automatic event collection
  only: aggregate active-user counts, and coarse country/city derived
  from IP address. No custom events are logged, and advertising-ID
  collection is explicitly disabled
  (`google_analytics_adid_collection_enabled=false`).
- **Firebase Crashlytics**: crash reports and NDK native-crash reports,
  used to diagnose and fix stability bugs. Crashlytics is disabled in
  debug builds and only runs in the release build distributed on Play.

Both are provided by Google's Firebase platform. See "Third parties"
below for Google's own handling of this data.

## Local data

OpenPiano stores your in-app settings (visible key count, key label
mode, octave/key shift, app language, and similar preferences) locally
on your device using Android's DataStore. The app itself never
transmits them. They are deleted when you uninstall the app or clear
its storage.

If Android backup is enabled on your device, Android may include these
settings in your device backup (for example, Google's backup service)
and restore them when you set up a new device. That backup is performed
by the operating system under your backup settings, not by OpenPiano,
and you can turn it off in your device's system settings.

## Data retention

- Local settings: retained on-device until you uninstall the app or
  clear its data; never transmitted by the app. Copies in an Android
  device backup follow your backup provider's retention.
- Firebase Analytics data (`play` flavor only): retained per Google's
  default Firebase Analytics retention settings. See Google's own
  policy, linked below.
- Firebase Crashlytics data (`play` flavor only): retained per Google's
  default Crashlytics retention settings. See Google's own policy,
  linked below.

## Third parties

The `play` flavor shares the automatically-collected data described
above with Google, via the Firebase SDK. Google's handling of that data
is governed by:

- Firebase's privacy and security terms:
  <https://firebase.google.com/support/privacy>
- Google's Privacy Policy: <https://policies.google.com/privacy>

The `foss` flavor shares data with no third party, because it makes no
network calls.

## Children's privacy

OpenPiano does not knowingly collect personal data from anyone,
including children. The app has no account system, no user-generated
content, and no communication features. The `play` flavor's automatic
Firebase Analytics collection does not identify individual users.

## Your choices

- Use the `foss` build (available at
  <https://github.com/ThomasKiljanczykDev/OpenPiano/releases>) for a build
  with no analytics or crash reporting whatsoever.
- Uninstalling the app removes all locally stored settings.

## Changes to this policy

If this policy changes, the updated version will be published at the
hosted URL below with a new effective date.

## Contact

Questions about this policy can be sent to:
thomas.kiljanczyk.dev@gmail.com

The hosted, canonical version of this policy is available at:
https://thomaskiljanczykdev.github.io/OpenPiano/privacy/
