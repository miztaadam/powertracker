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

## Owner's manual iPhone checks

The automated tests run in Chromium, not Safari. Tick these off on a real iPhone, at the gym if you can.

- [ ] Install: the install card shows in Safari, and is gone when opened from the Home Screen.
- [ ] Separate storage: log a set in Safari, then add Grit to the Home Screen and open it. Note whether the installed app starts empty on your iOS version (Grit assumes it does). If it does, the Safari card's "Export backup", then Restore backup in the installed app, brings the log over.
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
- [ ] Recovery export with App lock on: on the recovery screen, an export asks for Face ID first, so the file may be downloaded (Files, Downloads) instead of opening the share sheet. Note which happens, and check the file is there.
- [ ] Restore: pick that file with the file picker. Confirm. The data matches. App lock is as it was on this phone, and "Restore safety copy" in Settings brings back what the restore replaced.
- [ ] Two tabs: open Grit in a second tab while a first is suspended. The second asks before taking over.
- [ ] Bodybuilding mode: turn it on in Settings and back off (with motion on, the hammer or the pump plays and the new look is revealed; with reduce motion on, the look and label change at once), log a set on each side, then export a backup and restore it on the other platform (Android to iPhone or the reverse); both sides' plans and sessions come through.
- [ ] Plan builder: on the bodybuilding side, open Plan, tap Build my plan (or Rebuild my plan in the menu), answer the three questions, tap Use this plan, then train its first day from Today.
- [ ] Check-in: on the bodybuilding side, open Progress and do a check-in (mark a body part lagging), then apply a suggestion from Coach and check the plan changed.
- [ ] Stretch and squeeze: on the bodybuilding side, read the stretch and squeeze lines on a Today card.
- [ ] Update: deploy a second version. The prompt "Update ready — tap to refresh" shows only after you finish a session, and the app reloads only when tapped.
- [ ] Two windows, one update: with Grit open in two windows (or tabs), deploy a new version and tap "Update ready — tap to refresh" in one. The other reloads by itself, with no tap.
- [ ] Home Screen: add Grit to the Home Screen. The icon and name are "Grit", and it opens on Today.
- [ ] The top of each screen sits clear of the status bar, with no smeared title.
- [ ] Numbers such as 180 or 100 kg are not phone links.
- [ ] Start a rest and lock the phone or switch apps, then come back. The tone sounds at rest end.
- [ ] Settings shows three rest alerts and the line "iPhones don't let web apps vibrate."
- [ ] In Safari (not installed), the install card shows the Share, then Add to Home Screen steps.
- [ ] The browser bar's colour matches the page in dark and light, on both training sides.
- [ ] Press a button: it sinks a little and springs back. Settings has no "Vibrate on taps" switch.
- [ ] Log a set: it slides in.
- [ ] Open another exercise on Today: it is smooth, with no gap.
- [ ] With reduce motion on, nothing slides.
- [ ] Choose Chest, then Barbell, in the picker: Bench is listed.
- [ ] On Plan, tap Add on a day and pick an exercise: a form opens at 3 × 5 or 3 × 10 (powerlifting) or 3 × 8–12 (bodybuilding). Add stores what you set; Cancel stores nothing.
- [ ] On Today, skip a planned exercise with no sets (⋮, Skip today) once with each answer: Busy or no equipment, Pain, Out of time and Just skip. It leaves the list and "<name> skipped." shows with Undo. Escape or a tap outside the sheet skips nothing.
- [ ] Skip one, then tap Undo on the snackbar: it is back in its place.
- [ ] Skip one, let the snackbar go, then tap Bring back on the "Skipped today" line: it is back in its place.
- [ ] Skip a main lift (Bench) for Pain: Coach shows its card with Dismiss only. Skip an accessory (Curl) for Pain: its card offers Swap buttons, and a swap changes the plan.
- [ ] After a pain skip, the next time that exercise is on Today its weight is held: the target does not go up.
- [ ] Templates: on an empty powerlifting plan, tap Choose a template, pick one, tick its weekdays and tap Use this plan: Plan shows its week on those days and "Plan set: <name>." Do the same on an empty bodybuilding plan, where Choose a template is the main button over Build my plan.
- [ ] Templates, weekdays: in a draft, untick a day: Use this plan is off and the cards read "Day 1", "Day 2"… Tick another day: the week moves onto it.
- [ ] Templates, Swap: a main lift (Squat, Bench, Deadlift) has no Swap; an accessory's Swap lists other exercises and a pick keeps its sets, reps and RPE.
- [ ] Templates over a plan: from the Plan menu (⋮), Choose a template, then Use this plan asks "Replace your … plan?". Cancel changes nothing; Replace sets the new week, and History and bests are as they were. Back from a draft returns to the list.
- [ ] Undo after a template: on a side with a plan, choose a template and tap Replace. "Plan set: <name>." shows with Undo. Tap Undo: the old plan and its training days are back, also after closing and reopening Grit. Choosing a template on an empty side shows no Undo.
- [ ] Undo after Rebuild my plan: on the bodybuilding side with a plan, Rebuild my plan from the menu (⋮) and tap Use this plan. "Plan saved." shows with Undo. Tap Undo: the old plan is back. Let the snackbar go instead: the new plan stays.
- [ ] Restore warning: export a backup while the bodybuilding side has no plan, then build a bodybuilding plan and restore that backup (Plan, Settings, Restore backup). "This backup has no bodybuilding plan" shows with Restore anyway and Cancel. Cancel: the plan is still there. Restore anyway: the backup's data is in, with no bodybuilding plan. A backup that has the plan shows "Replace all data?" as before.
- [ ] Mode switch, the pump: on the powerlifting side, open Settings and turn on Bodybuilding mode. Three beats, each bigger, with veins of light from the switch; on the third the blue floods out in a circle from the switch and the BODYBUILDING label swells in.
- [ ] Mode switch, the hammer: turn Bodybuilding mode off. The small hammer lands hard on the very bottom of the screen, with a flash, cracks and a hard shake; the powerlifting look bursts out from the hit, and POWERLIFTING shows under the title and fades.
- [ ] Mode switch on the Home Screen app: during each animation, the new screen opens inside the growing circle with the old screen outside it. If Safari can't do that, it should fall back cleanly: the old screen stays still outside the circle, or the new screen shows at once. Either way, afterwards nothing is left moved, shaded or stuck, and the next switch plays normally. Note which you see.
- [ ] Mode switch sound: with the ringer on, each animation has its own sound, in time with the hits. With the silent switch on, no sound.
- [ ] Mode switch vibration: iPhones don't let web apps vibrate, so nothing buzzes; the animation plays as normal.
- [ ] Mode switch with Reduce Motion on: no animation, the look changes at once, and the sound still plays.
- [ ] Mode switch, skip: during either animation, tap anywhere outside the switch. It jumps to the end and the tap does nothing else. Flip the switch again mid-animation: the running one ends and the other plays, on the side of the last flip.
- [ ] Guide, a new install in Safari: open Grit in a Safari tab with no data. The guide opens on "Welcome to Grit" before anything else, and the install card shows only after it. Finish it or tap Skip, then close Safari's tab and open Grit again: it does not open again. (Closed before the end, it opens again next time: it counts as seen only once it closes.)
- [ ] Guide on the Home Screen: add Grit to the Home Screen and open it. The installed app starts empty, so the guide opens there once too. Finish it or tap Skip, then close and reopen the app: it does not open again.
- [ ] Guide, the side pick: on that first launch, screen 2 has Powerlifting and Bodybuilding buttons. Tap Bodybuilding: the look turns blue at once, with no animation and no sound, and screens 3 to 8 talk about bodybuilding. On screen 8, Choose a template opens the bodybuilding templates.
- [ ] Guide, Skip: on a new install, tap Skip on any screen. Today shows, and the guide does not open again.
- [ ] Guide replay: Plan, Settings, Help, Guide ("See how Grit works"). It opens on screen 1 with no side buttons on screen 2. Go to the end and leave it (Done with a plan; without one, Skip, since Choose a template opens the templates): your side, plan and log are as they were.
- [ ] Guide with Reduce Motion on: each picture shows its end at once, and screen 2's hammer and pump stand still.
- [ ] Guide at large text (in Safari, aA, zoom to 200%; Grit's text is fixed in size, so the iPhone's Text Size setting does not enlarge it): no word is cut, Back and Next stay on screen, and where the pictures don't fit the text shows without them.
- [ ] Open Progress: the charts draw in.
- [ ] Change the range: the line draws again.
- [ ] With reduce motion on, the charts are complete at once.
- [ ] Count plates: with Lat pulldown in today's plan (add it on Plan if it isn't), start the session and log one kg set on it (60 kg × 10). Then ⋮ on Lat pulldown → Count plates on this machine → Whole plates: the sheet says "The coach starts this exercise fresh in plates. Your kg sets stay as they are." The field reads PLATE and is empty; + gives 1. Log plate 7 × 10: the row reads "plate 7 × 10", and the 60 kg set still reads 60 kg. Bench has no such item.
- [ ] Half steps: choose Half steps too; + goes 7, 7.5, 8 in the field. Tap − back to 7.5 and log it: the row reads "plate 7½". Back on Whole plates, the keypad offers whole numbers, and from the logged 7.5, − and + go to 7 and 8. If your keypad lets you type 7.5, it is refused with "Whole plates only".
- [ ] The next session: on a later day, on the bodybuilding side, with Lat pulldown on that day's bodybuilding plan (Progress rows are bodybuilding sessions only). The coach's chip is in plates ("plate 7 × …"), never kg. Log plate 7 × 10 there: Progress (bodybuilding) shows "plate 7 × 10". The chip in plates appears from the second plate session on: there is no suggestion until the first plate set is logged.
- [ ] Switch back to In kg (⋮ on Lat pulldown → Count plates on this machine → In kg): the sheet says "The coach goes back to kg. Your plate sets stay as they are." The field reads kg again. A kg target shows only if there was one: it needs kg sets from an earlier day (here the 60 kg set). In the exercise's history the 60 kg set still reads 60 kg and the plate sets still read "plate …".
- [ ] Backup: export on one app, restore on the other: the plate sets and the setting come across. (A 1.16 Android app says the file is too new.)
- [ ] Large text (in Safari, aA, zoom to 200%): nothing in the sheet or the stepper is cut.
- [ ] The menu on a long name: on Plan, add Cable chest press to today. On Today, tap ⋮ on its row on the start card; then start the session and, before logging a set on it, tap ⋮ on its card. Each time both "Skip today" and "Count plates on this machine" are fully on screen and each can be tapped. The bug this checks needed a narrow layout (about 306 px: a small phone, or a larger display or text size setting), so an ordinary phone may not show it either way.
- [ ] The menu with the keyboard up: on Cable chest press's card, tap the weight field and type a number, then tap ⋮ straight away while the keyboard is still showing. The menu stays open as the keyboard goes away, and both items are on screen. Turn the phone on its side with the menu open: it closes.
- [ ] Half → whole → half, one day: with Lat pulldown on today's plan, on Half steps too, and plate 7 × 12 logged on it on an earlier day (at or under the planned effort, powerlifting side), today's chip suggests plate 7½. In the Count plates sheet (⋮ on Lat pulldown → Count plates on this machine), choose Whole plates: the chip now suggests plate 8. Apply it, then in the same sheet choose Half steps too again: the plate 7½ suggestion does not come back, and the field keeps plate 8.
- [ ] Very narrow Today and other tabs (web): with Safari's page zoom (aA, then the larger A, to 200% or more), until Today is very narrow (the Text Size setting alone may not narrow the layout, since Grit's text is fixed in size): nothing is cut or overlapping, the page does not scroll sideways, the tabs show icons only, and a logged set takes two lines. Then Coach, Progress, Exercises, Plan and Settings at the same zoom: nothing cut or overlapping, no sideways scroll; Progress's chart dates take two lines each, and Settings' days go three to a row. Then, at the same zoom, a lift's history and an exercise's history (a logged set on two lines), the check-in, every step of the plan builder, the exercise picker (from Today and from Plan, with a search, a muscle and Create your own), the form guide (from Today's ⓘ, the Exercises tab and the picker), the Muscles sheet, Plan's add and edit forms and Set best: nothing cut or overlapping, nothing scrolls sideways, each one's Save, Add, Next or Done can be scrolled to and tapped, and a new search on the Exercises tab brings the list back to its top with the search field still on screen.

## Owner's manual Android checks (the web app in Chrome)

- [ ] The install card says to open the browser's menu (⋮), then Install app or Add to Home screen. With a log, its note says the log stays in this browser and suggests a backup now and then.
- [ ] Log a set in Chrome, then install Grit from Chrome's menu and open the installed app: the log is there.
- [ ] Installed, the launcher icon is round or squircle with the whole "GRIT" mark inside, nothing cut off.
- [ ] Settings shows Sound, Sound + vibrate, Silent and Off. With Sound + vibrate, rest end beeps and buzzes twice.
- [ ] On a narrow phone (360 px or less), no screen scrolls sideways. "Sound +" wraps over "vibrate" if needed.
- [ ] In Chrome's settings, set page zoom to 130% (Accessibility): no name on the Exercises tab breaks inside a word. If the page becomes narrower than 320 px, its rows show no pictures.
- [ ] Restore a backup from the Android app. An app set to vibrate comes back as Sound + vibrate.
- [ ] Two windows, one update: with Grit open in two windows (or tabs), deploy a new version and tap "Update ready — tap to refresh" in one. The other reloads by itself, with no tap.
- [ ] Press a button: it sinks a little and springs back.
- [ ] A normal Log set buzzes once. A new squat best buzzes twice.
- [ ] Turn off "Vibrate on taps" in Settings (under Rest alert): Log set no longer buzzes.
- [ ] Log a set: it slides in.
- [ ] Open another exercise on Today: it is smooth, with no gap.
- [ ] With reduce motion on, nothing slides.
- [ ] Choose Chest, then Barbell, in the picker: Bench is listed.
- [ ] On Plan, tap Add on a day and pick an exercise: a form opens at 3 × 5 or 3 × 10 (powerlifting) or 3 × 8–12 (bodybuilding). Add stores what you set; Cancel stores nothing.
- [ ] On Today, skip a planned exercise with no sets (⋮, Skip today) once with each answer: Busy or no equipment, Pain, Out of time and Just skip. It leaves the list and "<name> skipped." shows with Undo. Escape or a tap outside the sheet skips nothing.
- [ ] Skip one, then tap Undo on the snackbar: it is back in its place.
- [ ] Skip one, let the snackbar go, then tap Bring back on the "Skipped today" line: it is back in its place.
- [ ] Skip a main lift (Bench) for Pain: Coach shows its card with Dismiss only. Skip an accessory (Curl) for Pain: its card offers Swap buttons, and a swap changes the plan.
- [ ] After a pain skip, the next time that exercise is on Today its weight is held: the target does not go up.
- [ ] Templates: on an empty powerlifting plan, tap Choose a template, pick one, tick its weekdays and tap Use this plan: Plan shows its week on those days and "Plan set: <name>." Do the same on an empty bodybuilding plan, where Choose a template is the main button over Build my plan.
- [ ] Templates, weekdays: in a draft, untick a day: Use this plan is off and the cards read "Day 1", "Day 2"… Tick another day: the week moves onto it.
- [ ] Templates, Swap: a main lift (Squat, Bench, Deadlift) has no Swap; an accessory's Swap lists other exercises and a pick keeps its sets, reps and RPE.
- [ ] Templates over a plan: from the Plan menu (⋮), Choose a template, then Use this plan asks "Replace your … plan?". Cancel changes nothing; Replace sets the new week, and History and bests are as they were. The browser's back from a draft returns to the list.
- [ ] Undo after a template: on a side with a plan, choose a template and tap Replace. "Plan set: <name>." shows with Undo. Tap Undo: the old plan and its training days are back, also after closing and reopening Grit. Choosing a template on an empty side shows no Undo.
- [ ] Undo after Rebuild my plan: on the bodybuilding side with a plan, Rebuild my plan from the menu (⋮) and tap Use this plan. "Plan saved." shows with Undo. Tap Undo: the old plan is back. Let the snackbar go instead: the new plan stays.
- [ ] Restore warning: export a backup while the bodybuilding side has no plan, then build a bodybuilding plan and restore that backup (Plan, Settings, Restore backup). "This backup has no bodybuilding plan" shows with Restore anyway and Cancel. Cancel: the plan is still there. Restore anyway: the backup's data is in, with no bodybuilding plan. A backup that has the plan shows "Replace all data?" as before.
- [ ] Mode switch, the pump: on the powerlifting side, open Settings and turn on Bodybuilding mode. Three beats, each bigger, with veins of light from the switch; on the third the blue floods out in a circle from the switch and the BODYBUILDING label swells in.
- [ ] Mode switch, the hammer: turn Bodybuilding mode off. The small hammer lands hard on the very bottom of the screen, with a flash, cracks and a hard shake; the powerlifting look bursts out from the hit, and POWERLIFTING shows under the title and fades.
- [ ] Mode switch vibration: the hammer's impact buzzes hard, with aftershocks; the pump gives three building beats, in time with the picture. With "Vibrate on taps" off it still buzzes.
- [ ] Mode switch sound: each animation has its own sound, in time with the hits, at the media volume.
- [ ] Mode switch with Remove animations on (Settings, Accessibility): no animation, the look changes at once, and the vibration and the sound still play.
- [ ] Mode switch, skip: during either animation, tap anywhere outside the switch. It jumps to the end and the tap does nothing else. Flip the switch again mid-animation: the running one ends and the other plays, on the side of the last flip.
- [ ] Guide, a new install: open Grit in Chrome with no data. The guide opens before anything else, and the install card shows only after it. Finish it or tap Skip, then open Grit again: it does not open again. (Closed before the end, it opens again next time.)
- [ ] Guide, the side pick: on that first launch, screen 2 has Powerlifting and Bodybuilding buttons. Tap Bodybuilding: the look turns blue at once, with no animation, sound or buzz, and screens 3 to 8 talk about bodybuilding. On screen 8, Choose a template opens the bodybuilding templates.
- [ ] Guide, Skip: on a new install, tap Skip on any screen. Today shows, and the guide does not open again.
- [ ] Guide replay: Plan, Settings, Help, Guide ("See how Grit works"). It opens on screen 1 with no side buttons on screen 2. Go to the end and leave it (Done with a plan; without one, Skip, since Choose a template opens the templates): your side, plan and log are as they were.
- [ ] Guide with reduce motion on: each picture shows its end at once, and screen 2's hammer and pump stand still.
- [ ] Guide at large text (Chrome's page zoom at 200%, in Chrome's settings under Accessibility): no word is cut, Back and Next stay on screen, and where the pictures don't fit the text shows without them.
- [ ] Open Progress: the charts draw in.
- [ ] Change the range: the line draws again.
- [ ] With reduce motion on, the charts are complete at once.
- [ ] Count plates: with Lat pulldown in today's plan (add it on Plan if it isn't), start the session and log one kg set on it (60 kg × 10). Then ⋮ on Lat pulldown → Count plates on this machine → Whole plates: the sheet says "The coach starts this exercise fresh in plates. Your kg sets stay as they are." The field reads PLATE and is empty; + gives 1. Log plate 7 × 10: the row reads "plate 7 × 10", and the 60 kg set still reads 60 kg. Bench has no such item.
- [ ] Half steps: choose Half steps too; + goes 7, 7.5, 8 in the field. Tap − back to 7.5 and log it: the row reads "plate 7½". Back on Whole plates, the keypad offers whole numbers, and from the logged 7.5, − and + go to 7 and 8. If your keypad lets you type 7.5, it is refused with "Whole plates only".
- [ ] The next session: on a later day, on the bodybuilding side, with Lat pulldown on that day's bodybuilding plan (Progress rows are bodybuilding sessions only). The coach's chip is in plates ("plate 7 × …"), never kg. Log plate 7 × 10 there: Progress (bodybuilding) shows "plate 7 × 10". The chip in plates appears from the second plate session on: there is no suggestion until the first plate set is logged.
- [ ] Switch back to In kg (⋮ on Lat pulldown → Count plates on this machine → In kg): the sheet says "The coach goes back to kg. Your plate sets stay as they are." The field reads kg again. A kg target shows only if there was one: it needs kg sets from an earlier day (here the 60 kg set). In the exercise's history the 60 kg set still reads 60 kg and the plate sets still read "plate …".
- [ ] Backup: export on one app, restore on the other: the plate sets and the setting come across. (A 1.16 Android app says the file is too new.)
- [ ] Large text (Chrome's page zoom at 200%): nothing in the sheet or the stepper is cut.
- [ ] The menu on a long name: on Plan, add Cable chest press to today. On Today, tap ⋮ on its row on the start card; then start the session and, before logging a set on it, tap ⋮ on its card. Each time both "Skip today" and "Count plates on this machine" are fully on screen and each can be tapped. The bug this checks needed a narrow layout (about 306 px: a small phone, or a larger display or text size setting), so an ordinary phone may not show it either way.
- [ ] The menu with the keyboard up: on Cable chest press's card, tap the weight field and type a number, then tap ⋮ straight away while the keyboard is still showing. The menu stays open as the keyboard goes away, and both items are on screen. Turn the phone on its side with the menu open: it closes.
- [ ] Half → whole → half, one day: with Lat pulldown on today's plan, on Half steps too, and plate 7 × 12 logged on it on an earlier day (at or under the planned effort, powerlifting side), today's chip suggests plate 7½. In the Count plates sheet (⋮ on Lat pulldown → Count plates on this machine), choose Whole plates: the chip now suggests plate 8. Apply it, then in the same sheet choose Half steps too again: the plate 7½ suggestion does not come back, and the field keeps plate 8.
- [ ] Very narrow Today and other tabs (web): with Chrome's page zoom (Accessibility) at 200% or more, until Today is very narrow: nothing is cut or overlapping, the page does not scroll sideways, the tabs show icons only, and a logged set takes two lines. Then Coach, Progress, Exercises, Plan and Settings at the same zoom: nothing cut or overlapping, no sideways scroll; Progress's chart dates take two lines each, and Settings' days go three to a row. Then, at the same zoom, a lift's history and an exercise's history (a logged set on two lines), the check-in, every step of the plan builder, the exercise picker (from Today and from Plan, with a search, a muscle and Create your own), the form guide (from Today's ⓘ, the Exercises tab and the picker), the Muscles sheet, Plan's add and edit forms and Set best: nothing cut or overlapping, nothing scrolls sideways, each one's Save, Add, Next or Done can be scrolled to and tapped, and a new search on the Exercises tab brings the list back to its top with the search field still on screen.
- [ ] Back closes what is open (web, 1.17.4): on the Exercises tab, open an exercise's form guide and press the phone's Back (button or edge swipe): the guide closes and you are still on Exercises. The same with a ⋮ menu open (it closes, you stay on the screen), and on Plan with an exercise's edit form and the picker open over it (Change, then a muscle): Back steps back from the muscle, then closes the picker, then the form, and only the next Back leaves Plan. Check it both in Chrome and in the installed app.

## Owner's manual checks in the Android app itself

- [ ] Press a button, a chip or a row: it sinks a little and springs back.
- [ ] A normal Log set buzzes once.
- [ ] Log a new squat best: it buzzes twice.
- [ ] Turn off "Vibrate on taps" in Settings (under Rest alert): Log set no longer buzzes.
- [ ] Log a set: it slides in.
- [ ] Open another exercise on Today: it is smooth, with no gap.
- [ ] With reduce motion on, nothing slides.
- [ ] Choose Chest, then Barbell, in the picker: Bench is listed.
- [ ] On Plan, tap Add on a day and pick an exercise: a form opens at 3 × 5 or 3 × 10 (powerlifting) or 3 × 8–12 (bodybuilding). Add stores what you set; Cancel stores nothing.
- [ ] On Today, skip a planned exercise with no sets (⋮, Skip today) once with each answer: Busy or no equipment, Pain, Out of time and Just skip. It leaves the list and "<name> skipped." shows with Undo. Back or a tap outside the sheet skips nothing.
- [ ] Skip one, then tap Undo on the snackbar: it is back in its place.
- [ ] Skip one, let the snackbar go, then tap Bring back on the "Skipped today" line: it is back in its place.
- [ ] Skip a main lift (Bench) for Pain: Coach shows its card with Dismiss only. Skip an accessory (Curl) for Pain: its card offers Swap buttons, and a swap changes the plan.
- [ ] After a pain skip, the next time that exercise is on Today its weight is held: the target does not go up.
- [ ] Templates: on an empty powerlifting plan, tap Choose a template, pick one, tick its weekdays and tap Use this plan: Plan shows its week on those days and "Plan set: <name>." Do the same on an empty bodybuilding plan, where Choose a template is the main button over Build my plan.
- [ ] Templates, weekdays: in a draft, untick a day: Use this plan is off and the cards read "Day 1", "Day 2"… Tick another day: the week moves onto it.
- [ ] Templates, Swap: a main lift (Squat, Bench, Deadlift) has no Swap; an accessory's Swap lists other exercises and a pick keeps its sets, reps and RPE.
- [ ] Templates over a plan: from the Plan menu (⋮), Choose a template, then Use this plan asks "Replace your … plan?". Cancel changes nothing; Replace sets the new week, and History and bests are as they were. The phone's back from a draft returns to the list.
- [ ] Undo after a template: on a side with a plan, choose a template and tap Replace. "Plan set: <name>." shows with Undo. Tap Undo: the old plan and its training days are back, also after closing and reopening Grit. Choosing a template on an empty side shows no Undo.
- [ ] Undo after Rebuild my plan: on the bodybuilding side with a plan, Rebuild my plan from the menu (⋮) and tap Use this plan. "Plan saved." shows with Undo. Tap Undo: the old plan is back. Let the snackbar go instead: the new plan stays.
- [ ] Restore warning: export a backup while the bodybuilding side has no plan, then build a bodybuilding plan and restore that backup (Settings, Restore backup). "This backup has no bodybuilding plan" shows with Restore anyway and Cancel. Cancel: the plan is still there. Restore anyway: the backup's data is in, with no bodybuilding plan. A backup that has the plan shows "Replace all data?" as before.
- [ ] Mode switch, the pump: on the powerlifting side, open Settings and turn on Bodybuilding mode. Three beats, each bigger, with veins of light from the switch; on the third the blue floods out in a circle from the switch and the BODYBUILDING label swells in.
- [ ] Mode switch, the hammer: turn Bodybuilding mode off. The small hammer lands hard on the very bottom of the screen, with a flash, cracks and a hard shake; the powerlifting look bursts out from the hit, and POWERLIFTING shows under the title and fades.
- [ ] Mode switch vibration: the hammer's impact is the strongest buzz the phone can give, with aftershocks; the pump gives three building beats. With "Vibrate on taps" off it still buzzes, and on Android 13 and later also with the phone's touch feedback off. If it never buzzes, check the phone's Media vibration switch (Sound and vibration, Vibration and haptics), the main vibration switch and battery saver: those can still mute it.
- [ ] Mode switch sound: each animation has its own sound, in time with the hits, at the media volume. In silent or vibrate mode, no sound. The very first switch after installing (the pump) has its sound too.
- [ ] Mode switch with Remove animations on (Settings, Accessibility): no animation, the look changes at once, and the vibration and the sound still play.
- [ ] Mode switch, skip: during either animation, tap anywhere outside the switch. It jumps to the end and the tap does nothing else. Flip the switch again mid-animation: the running one ends and the other plays, on the side of the last flip.
- [ ] Guide, a new install: install Grit fresh and open it. The guide opens on "Welcome to Grit". Finish it or tap Skip, then close Grit and open it again: it does not open again. (Closed before the end, it opens again on screen 1, with the side you picked kept.)
- [ ] Guide, the side pick: on that first launch, screen 2 has Powerlifting and Bodybuilding buttons. Tap Bodybuilding: the look turns blue at once, with no animation, sound or buzz, and screens 3 to 8 talk about bodybuilding. On screen 8, Choose a template opens the bodybuilding templates, and Back from them lands on Plan.
- [ ] Guide, Skip: on a new install, tap Skip on any screen. Today shows, and the guide does not open again.
- [ ] Guide replay: Plan, Settings, Help, Guide ("See how Grit works"). It opens on screen 1 with no side buttons on screen 2. Swipe through to the end and leave it (Done with a plan; without one, Skip, since Choose a template opens the templates): your side, plan and log are as they were.
- [ ] Guide with Remove animations on (Settings, Accessibility): each picture shows its end at once, and screen 2's hammer and pump stand still.
- [ ] Guide at large text (font size one or two steps up): no word is cut, Back and Next stay on screen, and the text scrolls beside the picture.
- [ ] Today: log a set. Its tick draws itself in. Skip an exercise: the "Skipped today" line slides up into place. Open Today again: both are simply there, with no animation.
- [ ] Open Progress: the charts draw in.
- [ ] Change the range: the line draws again.
- [ ] With reduce motion on, the charts are complete at once.
- [ ] Open Add on a Plan day: the names come up at once, with the pictures straight after.
- [ ] In that picker, your own exercise has an empty tile.
- [ ] With text one or two steps up (115% or 130%), no exercise name in the picker or on the Exercises tab breaks inside a word. Where a row has too little room (large text on a narrower phone), it shows no tile at all.
- [ ] With text one step up, on a phone 360 dp wide or more, the picker and the Exercises tab keep their pictures.
- [ ] Count plates: with Lat pulldown in today's plan (add it on Plan if it isn't), start the session and log one kg set on it (60 kg × 10). Then ⋮ on Lat pulldown → Count plates on this machine → Whole plates: the sheet says "The coach starts this exercise fresh in plates. Your kg sets stay as they are." The field reads PLATE and is empty; + gives 1. Log plate 7 × 10: the row reads "plate 7 × 10", and the 60 kg set still reads 60 kg. Bench has no such item.
- [ ] Half steps: choose Half steps too; + goes 7, 7.5, 8 in the field. Tap − back to 7.5 and log it: the row reads "plate 7½". Back on Whole plates, the keypad offers whole numbers, and from the logged 7.5, − and + go to 7 and 8. If your keypad lets you type 7.5, it is refused with "Whole plates only".
- [ ] The next session: on a later day, on the bodybuilding side, with Lat pulldown on that day's bodybuilding plan (Progress rows are bodybuilding sessions only). The coach's chip is in plates ("plate 7 × …"), never kg. Log plate 7 × 10 there: Progress (bodybuilding) shows "plate 7 × 10". The chip in plates appears from the second plate session on: there is no suggestion until the first plate set is logged.
- [ ] Switch back to In kg (⋮ on Lat pulldown → Count plates on this machine → In kg): the sheet says "The coach goes back to kg. Your plate sets stay as they are." The field reads kg again. A kg target shows only if there was one: it needs kg sets from an earlier day (here the 60 kg set). In the exercise's history the 60 kg set still reads 60 kg and the plate sets still read "plate …".
- [ ] Backup: export on one app, restore on the other: the plate sets and the setting come across. (A 1.16 Android app says the file is too new.)
- [ ] Large text (font size one or two steps up): nothing in the sheet or the stepper is cut.
- [ ] The ⋮ menu: on Today, tap ⋮ on an exercise that has both items (Cable chest press on today's plan): "Skip today" and "Count plates on this machine" both show.
- [ ] Half → whole → half, one day: with Lat pulldown on today's plan, on Half steps too, and plate 7 × 12 logged on it on an earlier day (at or under the planned effort, powerlifting side), today's chip suggests plate 7½. In the Count plates sheet (⋮ on Lat pulldown → Count plates on this machine), choose Whole plates: the chip now suggests plate 8. Apply it, then in the same sheet choose Half steps too again: the plate 7½ suggestion does not come back, and the field keeps plate 8.

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
