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

## Grit on iPhone

Grit also runs on iPhone as a web app. It works offline and keeps your log on the phone.

1. Open the Grit address in Safari.
2. Tap Share, then Add to Home Screen.
3. Open Grit from the Home Screen. Installed, your log is protected from being cleared.

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

## Owner's manual iPhone checks

The automated tests run in Chromium, not Safari. Tick these off on a real iPhone, at the gym if you can.

- [ ] Install: the install card shows in Safari, and is gone when opened from the Home Screen.
- [ ] Storage: Settings says "Storage: protected" after installing.
- [ ] A private Safari window shows the warning. A normal window and the installed app do not. "Continue anyway" is remembered.
- [ ] Offline in the gym: turn on airplane mode, close Grit, open it, and log a set.
- [ ] Face ID lock: turn on App lock and make the passkey. It unlocks on launch and after a minute away.
- [ ] The passkey works on the final address. A passkey made on a preview address will not work on the real one.
- [ ] Safari may show the Face ID prompt on launch without a tap. Note whether it does.
- [ ] After a cancelled prompt, the Unlock button brings up Face ID.
- [ ] "Can't unlock?": delete the Grit passkey in Settings, Passwords. Tap Unlock (it fails). Tap "Can't unlock?" (Face ID, the app opens). Check there is one Grit passkey. Relaunch and unlock with it. Try the same from the Recovery screen if you can reach it.
- [ ] App switcher: with App lock on, swipe to the app switcher. The card shows the Grit icon, not your log.
- [ ] A 30 second trip to another app mid-session does not lock the app later.
- [ ] Rest tone with the ringer on, and with the ringer off.
- [ ] Audio: the tone plays after the first tap. Check it still plays after a phone call or music interrupts the audio.
- [ ] The screen stays awake during a rest (wake lock).
- [ ] With Reduce Motion on, the rest alert fades instead of flashing.
- [ ] Backup: Export backup opens the share sheet. Save it to iCloud Drive.
- [ ] Restore: pick that file with the file picker. Confirm. The data matches.
- [ ] Two tabs: open Grit in a second tab while a first is suspended. The second asks before taking over.
- [ ] Update: deploy a second version. The prompt "Update ready — tap to refresh" shows only after you finish a session, and the app reloads only when tapped.

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
