# Grit Web — Design

**Date:** 2026-11-11
**Status:** Approved in brainstorming, awaiting spec review
**Related:** Android Grit phase 1 — `docs/superpowers/specs/2026-11-11-powertracker-phase1-design.md`

## 1. Purpose

A web version of Grit that the owner installs on an **iPhone home screen** and uses as their **main
logbook**. It has full feature parity with Android Grit phase 1 (Today logging, Coach, Progress, Plan,
Settings, backups, rest timer, optional app lock). It gives **identical coaching** because it runs the
**same coach code**.

**Who it's for:** one lifter, on one iPhone. There are no accounts, no server, no cloud sync and no
tracking.

**Success:**
- the owner logs every session in it on the iPhone, including offline in the gym;
- data survives app updates and Safari's storage policies;
- a backup moves cleanly between Android and web in both directions.

### Out of scope

- Cloud sync.
- Push notifications.
- Lock-screen alerts.
- Vibration (iOS web apps can't vibrate).
- Desktop-optimised layouts. Desktop works, but the design targets phone size.
- The phase 2 program builder.

## 2. Decisions made

- **Sharing approach:** Kotlin Multiplatform. `:domain` compiles to JVM (Android) and JS (web). The web UI
  is React + TypeScript + Vite. The alternatives were rejected: a TypeScript port of the coach (two copies
  that drift) and Compose for Web (Wasm on Safari is too immature for daily use).
- **Repo:** the same `powertracker` repo, in a new `web/` folder.
- **Hosting:** static files on Vercel. Creating or connecting the Vercel project needs the owner's
  go-ahead at that point.
- **Storage:** IndexedDB through Dexie. Grit asks for persistent storage on first launch.
- **Rest alert on iPhone:** Sound (tone plus screen flash) / Silent (flash only) / Off. The Android
  "Vibrate only" value maps to Sound.
- **App lock:** optional and off by default, using a passkey (WebAuthn platform authenticator: Face ID /
  Touch ID) stored on the device. It is a convenience gate, not encryption.
- **Look:** the Android design system (`docs/design/DESIGN.md`), the Barlow fonts, and the grey
  barbell icon (`docs/design/logo/b-barbell-grey-*`).

## 3. Architecture

```
domain/   Kotlin Multiplatform: commonMain (all coach logic), jvm + js(IR, browser) targets
app/      Android — consumes :domain's JVM target; storage formats unchanged
web/      React + TypeScript + Vite PWA
  src/domain/   generated JS package from :domain (+ .d.ts), checked for staleness in CI/build
  src/db/       Dexie schema, versions/migrations, row <-> domain mapping, seed library
  src/backup/   encode/decode/restore (Android-compatible v1 JSON)
  src/state/    per-screen view models / hooks (no direct DB access from components)
  src/ui/       design system: tokens (CSS variables), components, icons
  src/screens/  Today, Coach, Progress, LiftHistory, Plan, Settings, Lock, Install
  src/platform/ service worker registration, rest timer, wake lock, audio, passkey lock, storage persistence
  public/       manifest.webmanifest, icons (from the grey barbell SVGs), fonts
```

### 3.1 Converting the domain to multiplatform

- `java.time.LocalDate` and `DayOfWeek` become `kotlinx.datetime.LocalDate` / `DayOfWeek` everywhere in
  `:domain`. `ChronoUnit` arithmetic becomes the kotlinx-datetime equivalents.
- Android `:app` adapts at its boundaries: Room converters keep storing **epochDay (Long)** and
  **DayOfWeek as 1–7**, so existing on-device data and the backup format are unchanged.
- Every domain test runs on JVM and JS (`./gradlew :domain:allTests`). The Android suite stays green.
- `:domain` gains a `jsMain` **export facade** (`@JsExport`) that takes and returns plain data (numbers,
  strings, arrays, simple objects with ISO dates) for:
  - `E1rm.estimate`;
  - `PlateLoader.perSide` / `round` / `step`;
  - `InputValidator.check` / `baselineKg`;
  - `Coach.targetsFor` / `evaluate`;
  - the `HistoryStats` functions the Progress screen needs: `weeklyTrend`, `currentE1rm`, PRs and weekly
    volume (`ProgressMath` moves into `:domain` so both apps share it);
  - `AccessoryMap` options and names;
  - the seed exercise library: the list moves from `app/.../SeedLibrary` into `:domain`, and both apps
    seed from it.
- A Gradle task builds the JS package into `web/src/domain/`. The web build fails if it is stale.

### 3.2 Web app

- React 18 + TypeScript (strict) + Vite.
- A small typed router: a bottom tab bar (Today / Coach / Progress / Plan), plus the `liftHistory/:lift`,
  `settings`, `lock` and `install` routes.
- State lives in hooks over Dexie live queries. Coach calls go through the facade, and the logic mirrors
  the Android view models: TARGET dedupe per exercise per day, Apply-only edits, typo confirm,
  double-tap guards, and Finish → evaluate → insert advice.
- The design system:
  - CSS variables from the DESIGN.md tokens, for dark (default) and light (follows the system setting);
  - Barlow / Barlow Condensed bundled as WOFF2 with tabular numerals;
  - components mirroring Android `ui/components`: StepperField, SetRow, PlateStrip, CoachSuggestion,
    AdviceCard, StatTile, charts (SVG), RatioBar, SegmentedChoice, Buttons, Pills, EmptyState,
    RestTimerBar.

## 4. Screens and behaviour

Every screen behaves as on Android. The Android code and tests are the reference for each rule.

- **Today:** start a session (optional bodyweight and readiness), logging, steppers and tap-to-type,
  Missed, the Floor / Mid / Lockout row on a failed rep or RPE ≥ 9.5, the plate strip, coach chips that
  change nothing until Apply, typo prompts, Finish in the header plus a last-exercise Finish, and the
  finish summary.
- **Coach:** card order (deload, re-entry, stall, weak point), detail per type, Apply and Dismiss with the
  same plan insertion, the snackbars, the empty state and expired-card hiding.
- **Progress:**
  - e1RM charts per lift and the total, with the 8 wk / 6 mo / 1 yr / All selector (default 6 mo);
  - Records, weekly volume, ratio bars and bodyweight;
  - "No sessions in this range" with the selector kept;
  - lift history with PR badges.
- **Plan and Settings:**
  - the plan editor: add, edit, reorder, delete, custom exercises and Set best;
  - all settings: change plates, default RPE, training days, rest durations, rest alert, app lock and
    keep screen awake;
  - backup export and restore, the storage status, the last backup date and the safety-copy restore.

### iPhone specifics

- Full-screen standalone display, with `viewport-fit=cover` and safe-area insets.
- Touch targets ≥ 48 px.
- Inputs at ≥ 16 px (no iOS zoom), with `inputmode="decimal"` for numbers.
- No overscroll bounce inside the app shell.
- **Install card:** when the site runs in Safari and not as an installed app, a card explains Share →
  Add to Home Screen and why it protects the data. The app still works without installing.

### Rest timer

- The countdown is based on an end time (`endsAt`), so the right time shows after the screen has been
  locked.
- It starts after every logged set, failed sets included, using the main or accessory duration.
- The bar has −30 s, +30 s and Skip.
- At zero, the Rest alert setting applies: Sound = a short tone through Web Audio, unlocked on the first
  user gesture, plus a screen flash; Silent = flash only; Off = nothing.
- While a rest runs, a Screen Wake Lock keeps the screen on when the "Keep screen awake during rests"
  setting is on (default on) and the API is available.

### App lock (optional, default off)

- Turning it on creates a passkey: WebAuthn with a platform authenticator and user verification
  required, relying party = Grit's own origin. The credential id is stored locally.
- Unlocking calls `navigator.credentials.get` with user verification.
- Grit asks on launch and after ≥ 60 s in the background (`visibilitychange` timestamps), and keeps the
  first hide time until the next unlock.
- Content is not rendered while locked.
- If WebAuthn or a platform authenticator is unavailable, Grit explains that and doesn't lock.

## 5. Data, updates and safety

- **Dexie tables:** exercises, sessions, sets, planItems, advice, baselines, settings (key–value) and
  meta (schema version, last backup date, safety copy).
  - Field names and meanings match Android.
  - Dates are ISO strings in storage. They become epochDay only in backups.
- **Seeding:** the built-in library is seeded on first run from the domain's shared seed list.
- **Upgrades:**
  - every schema change is a numbered Dexie version with an upgrade function and a test (data from
    version N−1 upgrades correctly);
  - before running any upgrade, Grit writes a **safety copy** (a full backup JSON) into `meta`;
  - if an upgrade throws, the app opens in recovery mode offering to restore the safety copy or export
    it.
- **App updates:**
  - the service worker precaches the build;
  - a new version installs in the background and shows "Update ready — tap to refresh";
  - it never reloads automatically, and never during an open session.
- **Storage persistence:** `navigator.storage.persist()` runs on first launch and when the app is
  installed. Settings shows "Storage: protected" or "Storage: not protected".
- **Backup reminder:** a reminder appears after 14 days without an export, or after 7 days if storage
  isn't protected.
- **Backups:**
  - the format is identical to Android version 1: `BackupFile` with settings and a list per table,
    dates as epochDay, weekdays 1–7, enums as names;
  - **decode** rejects non-JSON and non-object files, a missing version or version ≤ 0, and a version
    above 1 ("This backup is from a newer version of Grit."); any invalid content → "This file isn't a
    valid Grit backup.";
  - **restore** validates and maps every row first, then clears and inserts everything in one Dexie
    transaction, then applies the settings;
  - export goes through the share sheet (`navigator.share` with a file), with a download fallback, named
    `grit-backup-YYYY-MM-DD.json`; import uses a file input;
  - rest alert mapping. Android's decoder rejects unknown keys, so as part of step 1 Android's
    `BackupSettings` gains an optional field `webRestAlert: String? = null`, which Android stores and
    round-trips but doesn't use.
    - Web exports always write an Android-valid `restAlert`: Sound → `SOUND_AND_VIBRATE`, Silent →
      `VIBRATE`, Off → `OFF`. The web value goes in `webRestAlert` (`SOUND` / `SILENT` / `OFF`).
    - Web restore uses `webRestAlert` when it is present. Otherwise it maps `VIBRATE` /
      `SOUND_AND_VIBRATE` → Sound and `OFF` → Off.
- **Privacy:** a Content-Security-Policy of `default-src 'self'` (fonts and images self-hosted,
  `connect-src 'self'`). No analytics, no third-party requests.

## 6. Edge cases

- Private browsing or storage unavailable: a blocking warning that data won't be kept, with an export
  button.
- Storage full (QuotaExceeded): a clear error on that action, no partial writes, and an offer to export.
- Reload or app kill mid-session: the session and sets are already stored, and editor values persist
  per exercise.
- Two tabs open: the second shows "Grit is open in another tab", using a BroadcastChannel / Web Locks
  guard.
- Clock or date change mid-session: same rule as Android (the Today state is keyed per date, and old
  views don't write).

## 7. Testing

- **Domain:** `:domain:allTests` (JVM + JS) green. The Android `:app:testDebugUnitTest` stays green.
- **Web unit (Vitest):** row ↔ domain mapping round trips; each Dexie upgrade; backup decode and
  encode, including real Android export fixtures in both directions; rest-alert mapping; timer
  arithmetic; lock gate timing; hooks logic (dedupe, Apply-only, typo confirm, double-tap).
- **Web components (Testing Library):** log a set as prescribed; the miss row; the coach chip changes
  nothing until Apply; the typo prompt; Apply on a stall adds accessories; Dismiss; the Progress empty
  range; the plan editor add, edit and delete.
- **Browser E2E (Playwright, WebKit, iPhone-size viewport):**
  - a full session: start, log, finish, coach card, apply;
  - offline after the first load;
  - export, clear, restore;
  - a reload mid-session keeps everything;
  - an update prompt doesn't reload during a session;
  - a screenshot of every screen in dark and light, shown to the owner.

## 8. Delivery

- `web/` builds with `npm run build` to static files. `vercel.json` sets the CSP, long-cache headers for
  hashed assets, and no-cache for `index.html` and the service worker.
- Vercel: preview builds per branch, production from `main`. The project setup is done only after the
  owner says go.
- The README gains "Grit on iPhone": open the URL in Safari → Share → Add to Home Screen, and how to move
  data from Android with a backup.

## 9. Build order

1. Convert the domain to multiplatform, with the shared seed library and `ProgressMath` moved in, and
   the JS facade added. Android stays green.
2. Web scaffold, design system, Dexie storage and backups.
3. Screens: Today, then Coach, then Progress and lift history, then Plan and Settings. Fable builds the
   UI.
4. Rest timer, app lock, install card, updates and storage persistence.
5. Vercel deployment (after the owner's go-ahead) and the final whole-branch review.
