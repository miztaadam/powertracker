# Grit

A private, offline powerlifting logbook for Android with a rules-based coach. It reads your
progression and tells you what to lift next, when a lift has stalled (and which accessories to
add), and when to deload. Nothing leaves the phone: the app has no internet permission.

The app is called **Grit**. The repository, the Gradle modules and the package / application id
(`com.powertracker`) keep the original name, Powertracker; renaming them would orphan installed data.

## What is in it

- **Today** — the planned session with coach targets, set logging, plate loading and a rest timer
  (ongoing notification; at zero it vibrates, vibrates and plays a tone, or stays quiet, as set in Settings).
- **Coach** — advice cards (target, stall, deload, weak point, re-entry). Nothing changes until you tap Apply.
- **Progress** — e1RM charts, records and per-lift history.
- **Plan** — the weekly plan, plus **Settings**: change plates, default RPE, training days, rest
  lengths and rest alert, app lock, backup export and restore (JSON).
- **App lock** — optional. Asks for a fingerprint, face or the phone's screen lock when Grit opens
  and after a minute in the background.

## Build, run, test

Needs JDK 17 or newer and the Android SDK (platform 37); point `local.properties` at it
(`sdk.dir=/path/to/android-sdk`). minSdk is 26.

```sh
./gradlew :app:assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew :app:installDebug           # install on a connected phone or emulator
./gradlew :domain:test :app:testDebugUnitTest   # all tests
```

All tests are JVM tests: `:domain` is plain Kotlin, and the Android tests in `app/src/test` run
under Robolectric, so no emulator is needed. If Maven Central answers HTTP 429 on the first
download of dependencies, run again with `--max-workers=1`.

## Release APK

```sh
./gradlew :app:assembleRelease        # app/build/outputs/apk/release/app-release-unsigned.apk
```

The release build is optimised and shrunk with R8 (`optimization { enable = true }` in
`app/build.gradle.kts`). No signing key is kept in the repository, so the APK is unsigned; sign it
before installing, for example:

```sh
apksigner sign --ks my-release-key.jks --out grit.apk app/build/outputs/apk/release/app-release-unsigned.apk
adb install grit.apk
```

After building a release, open it once on a phone: R8 problems only show at run time.

## Where things are

- `domain/` — pure Kotlin: e1RM, plate maths, history statistics and the coach rules.
- `app/` — Room database, settings (DataStore), backup, rest timer service, app lock and the Compose UI.
- `docs/design/DESIGN.md` — the design system (colour, type, components, rules).
- `docs/design/screens/` — mockups rendered from the real screens by `ScreenshotTest` and
  `LauncherIconTest`. A normal test run only writes them to `app/build/screenshots/`; to update the
  committed ones run
  `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' --tests '*LauncherIconTest*' -Pscreenshots.update=true`.
- `docs/design/logo/` — the launcher icon concepts and the brief.
- `docs/superpowers/specs/2026-11-11-powertracker-phase1-design.md` — the product design;
  `docs/superpowers/plans/` — the build plan.
