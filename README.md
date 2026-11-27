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
- **Guide** — eight short screens on how Grit works. It opens once on a new install, and any time
  from Settings (Help, Guide).
- **App lock** — optional. Asks for a fingerprint, face or the phone's screen lock when Grit opens
  and after a minute in the background.

## Build, run, test

Needs JDK 17 or newer and the Android SDK (platform 37); point `local.properties` at it
(`sdk.dir=/path/to/android-sdk`). minSdk is 26.

```sh
./gradlew :app:assembleDebug          # debug APK in app/build/outputs/apk/debug/
./gradlew :app:installDebug           # install on a connected phone or emulator
./gradlew :domain:allTests :app:testDebugUnitTest   # all Kotlin tests (also run on every PR)
```

No emulator is needed: `:domain` is plain Kotlin, tested on the JVM and on Node, and the Android tests in `app/src/test` run
under Robolectric. If Maven Central answers HTTP 429 on the first
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

For each update, bump `versionName` and `versionCode` in `app/build.gradle.kts` and `version` in
`web/package.json` together (a web test fails when they disagree); Settings shows the version.

## Grit on iPhone

Grit also runs on iPhone as a web app. It works offline and keeps your log on the phone.

1. Open the Grit address in Safari.
2. Tap Share, then Add to Home Screen.
3. Open Grit from the Home Screen. Installed, your log is protected from being cleared.

In the exercise picker, tap the ⓘ beside an exercise for its form guide, or use "Browse by muscle" to find exercises by muscle.

The installed app keeps its own log, apart from Safari's, and starts empty. If you have already
logged in Safari, do this so the log comes along:

1. In Safari, tap Export backup before you add Grit to the Home Screen. The button is on the install
   card on Today, and in Plan, Settings.
2. Add Grit to the Home Screen and open it from there.
3. In the installed app, open Plan, then Settings, then Restore backup, and pick that file.

From then on, use only the installed app. Safari clears the data of sites you have not opened for a
while, so a log left behind in the Safari tab can disappear.

## Moving from Android

1. In the Android app, open Settings and tap Export backup.
2. Send the file to the iPhone with AirDrop, or save it in iCloud Drive.
3. In Grit on the iPhone, open Plan, then Settings, then Restore backup, and pick the file.
4. Confirm "Replace all data?". Your log, plan and coach notes come over.

The backup format is the same in both apps, so you can also go from iPhone back to Android.

## Developing

The web app is in `web/`. See `web/README.md` for the full notes.

```sh
cd web
npm ci
npm test            # unit tests
npm run e2e         # end-to-end tests (Chromium with iPhone 13 emulation)
cd ..
./gradlew :domain:syncWebDomain   # rebuild the shared Kotlin domain for the web app
```

## Phone checks after a release

The automated tests run in an emulated phone, never on a real one and never in Safari. These ten are what only a real
phone can answer. Do them after each release on each phone you use: the iPhone app from the Home Screen, the Android app,
and Chrome on Android if you open Grit there. They take about ten minutes.

- [ ] **1. It updated.** Open Grit, then Plan, Settings: the last line names the new version. (Web: "Update ready — tap
      to refresh" shows, never in the middle of a session; tap it. Android app: after Update Project and Run in
      Android Studio.)
- [ ] **2. Your log is still there.** Progress shows your charts and records. On the iPhone app, Settings says
      "Storage: protected".
- [ ] **3. Offline** (web): turn on airplane mode, close Grit completely, open it and log a set.
- [ ] **4. Log a set.** Start a session and log a set: it shows with the right numbers, and the rest timer starts.
- [ ] **5. The rest alert after locking the phone.** Start a rest, lock the phone or switch apps, and come back before
      it ends: at the end it sounds or buzzes, as chosen in Settings. The Android app should alert while still locked.
- [ ] **6. App lock**, if you use it: Grit asks when it opens and after a minute away, and Face ID, fingerprint or the
      screen lock lets you in.
- [ ] **7. Backup out.** Plan, Settings, Export backup: the share sheet or the file picker opens and the file is saved.
- [ ] **8. Backup in**, only when you are moving your log or the release notes say the backup changed, because it
      replaces everything on that phone: restore the other phone's file and check the data matches.
- [ ] **9. Back closes what is open.** On Exercises, open a form guide and go Back (button or edge swipe): the guide
      closes and you are still on Exercises.
- [ ] **10. This release's own change.** 1.23, both apps: the warm-up checklist. On Today, start a session and open
      a barbell lift you have done before, before logging a set. Above the weight it says "WARM-UP · NOT LOGGED" with
      one or two lighter sets side by side (about half the weight for 12, then about three quarters for 5 on the big
      lifts), each with its plates per side under it. Tap one: it is ticked and struck through. Tap + on the weight:
      the sets follow and the buttons stay where they are. Log your first set: the checklist is gone, and History
      shows only the set you logged.

The full list, feature by feature (201 checks), is in [`docs/manual-checks.md`](docs/manual-checks.md). Use it when a
release touched a feature and you want that feature's own checks.

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
