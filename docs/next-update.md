# Grit — next update (after the web app ships)

Requested by the owner during the web build. Applies to **both** the Android app and the web app,
through the shared `:domain` module and design tokens. Each item gets its own spec → plan → build. Order agreed with the owner: (1) chalk accent + typed numbers on Today — shipped; (2) exercise library + form guides (items 3 and 4); (3) strict press.

## 1. Chalky grey buttons
- Change primary buttons from volt green (`#C9F73A`) to a chalky grey.
- **Owner decided: every volt accent**: buttons, current tab, switches, rest-timer progress, coach
  suggestion chip, chart highlights. The green goes everywhere; chalky grey replaces it throughout.
- Must still pass the automated contrast tests (Android `TokenContrastTest`, web `tokens.test.ts`).

## 2. Strict press as a main lift
- Add STRICT PRESS alongside squat, bench and deadlift: progression targets, stall detection, an
  accessory map by sticking point (e.g. lockout → triceps; off the shoulders → front delts / upper chest),
  Progress charts and records, lift history.
- **Owner decided: adding it to the total is optional.** A setting ("Include strict press in total",
  off by default) switches between the powerlifting total (squat + bench + deadlift) and squat + bench +
  deadlift + strict press. Progress labels the total so it is clear which one is shown.
- **Owner decided: no ratio against bench**, so the weak-point check leaves strict press alone.
- Compatibility: append to the end of the built-in exercise list so existing ids and data are unchanged.
  The backup gains a new lift name, so both apps update together (older versions reject it).

## 3. Form descriptions for every exercise + ⓘ info button
- Every exercise (main lifts, variations, accessories, the new library) gets: what it's for, setup,
  how to perform it, 2–3 cues, common mistakes.
- A small ⓘ button wherever an exercise appears: Today exercise card, Coach suggestions, Plan list,
  exercise picker. Opens a short sheet readable between sets.
- Shipped in the app (works offline), defined once in the shared domain so both apps show identical text.
- Custom exercises can hold the user's own form notes.
- **Owner decided: the text is checked against reliable published sources, not left to the owner's
  review** (the owner doesn't have this expertise). Process:
  - Every description is researched online against reputable sources: strength and conditioning bodies
    (e.g. NSCA, ACSM, ACE), established exercise references (e.g. ExRx), and powerlifting federation
    or coaching material for the competition lifts. Points that sources disagree on are left out or
    stated in their most conservative form.
  - Each exercise's sources are recorded alongside its text in the repo (not shown in the app), so
    any tip can be traced.
  - Safety comes first: setup and bracing before cues; safety bars/spotter notes for squat and bench;
    no advice to train through pain; one standard line in every sheet: "Stop if you feel sharp pain.
    For an injury or a condition, check with a doctor or physiotherapist."
  - A separate independent review pass checks the text against the sources before it ships.
  - The owner still sees the finished text and can flag anything that reads oddly, but is not asked
    to judge technique.

## 4. Much bigger exercise library
- Expand the built-in exercises so the Plan page can match an exact routine: lift variations, upper push
  and pull, legs, posterior chain, core, arms, conditioning, grouped by category and tagged by muscle.
- Plan exercise picker gets search plus category and muscle filters.
- New exercises are appended (existing ids, plans, history and backups unaffected); each one gets a form
  description (item 3).
- **Owner decided: as vast as possible**: every common equipment type (barbell and specialty bars,
  dumbbells, kettlebells, cables, machines, bands, chains, bodyweight), while staying **easy to find**
  (search, category and muscle filters, plus an equipment filter) and with **a description and ⓘ info
  button for every exercise** (item 3).

## Bodybuilding mode: stage 1 done (version 1.2, both apps)
Spec: `docs/superpowers/specs/2026-11-13-grit-bodybuilding-mode-design.md`; plan: `docs/superpowers/plans/2026-11-13-grit-bodybuilding-mode-stage-1.md`.

What the lifter gets:
- A **Bodybuilding mode** switch, first in Settings (group "Training style"). It takes effect at once and the powerlifting plan is kept.
- A blue look and a small **BODYBUILDING** label on every top-level screen while it is on. The powerlifting side looks as before.
- Its own plan and training days, with rep ranges (new entries 3 × 8–12 @ 8).
- A coach that goes reps first, then weight: once every set reaches the top of the range at or under the target RPE, it adds weight and aims for the low end again; otherwise "Beat it." with last time's reps.
- One training log for both sides. Each coach reads its own side, and a lift coming back after a long time on the other side is eased in, never the old top set plus an increase.
- Progress on this side: each planned exercise with its best set of its last four sessions and whether it is going up; a row opens the exercise history.
- A welcome-back card on Today and Coach when returning to a side left more than 30 days ago: resume the plan, or open it to change it.
- Backup version 3 on both apps: an older app refuses a new backup, so update both together.

Notes:
- The weight increase is 2% of the last weight, from the ACSM guidance as summarised on ExRx (exrx.net, Weight Training Guidelines; recorded in `CoachSources.kt`).
- On the bodybuilding side a deload can come from low readiness alone; the other fatigue signals read powerlifting sessions only until stage 3 brings a sourced bodybuilding one.
- Not yet tried on real phones, so the bodybuilding line in README's manual checklist matters: tick it off before relying on the mode.

## Bodybuilding mode: stage 2 done, the plan builder (version 1.3, both apps)
Plan: `docs/superpowers/plans/2026-11-13-grit-bodybuilding-mode-stage-2.md`.

What the lifter gets:
- On the bodybuilding side, the app builds the weekly plan from three questions: how many days (3 or 4) and which weekdays; which exercises they do now (ticked for them from their plans and the last month's log), or, with none, which they would like to do (with none at all, "Choose for me" uses the app's own list); and which body parts they most want to improve (up to 3, or skip).
- A draft week before anything is saved: each day with its exercises and sets (8–12 reps at RPE 8), the weekly sets for each of the 12 body parts with a quiet mark on any under 10, and any of their exercises left out, with the reason.
- **Swap** on any exercise, for another of the same type (a press for a press, a curl for a curl); **Another draft** for a different choice of the exercises the app picked (their own picks stay); **Use this plan** saves it as the bodybuilding plan and sets the bodybuilding training days. If a bodybuilding plan exists, it asks first: the log is kept. Leaving before then changes nothing. The powerlifting plan is never touched.
- Where to find it: **Build my plan** on an empty bodybuilding Plan, **Rebuild my plan** in the bodybuilding Plan menu, and "My goals have changed" on the bodybuilding side.
- The same answers always give the same draft, offline. No new settings are stored, so the backup format is unchanged.

The sourced numbers (recorded in `CoachSources.kt`):
- About 10 sets a week for each muscle group for growth, and every major muscle group trained at least twice a week: the ACSM 2026 position stand on resistance training, as announced by ACSM and in its infographic.
- Compound lifts before accessory work: ExRx's summary of the same ACSM guidance.

The designer's own numbers, not from the sources (for the owner to judge):
- The session templates and their sizes: 3 days are three full-body sessions of 8, 9 and 8 exercises; 4 days are upper and lower twice, 8 exercises on an upper day and 6 on a lower day.
- 4 sets for a session's first exercise and for the exercises of a priority body part; 3 for the rest.
- At most 3 priority body parts, chosen from 12.
- At most one held-over pick a session (an exercise of the lifter's that has no place of its kind left).
- The hand-picked default exercises for each kind of place in a session.

What the default weeks give (no picks, no priorities): the big muscle groups (chest, lats, quads, hamstrings, glutes, upper back, biceps, triceps) 10 or more counted weekly sets; side delts 9; calves, abs and rear delts 6; each trained on at least two days. A priority always adds sets to its own body part and never takes any from another.

Not yet tried on real phones: the plan builder line in README's manual checklist matters.

Still to come, each with its own plan (spec §8): stage 3, check-ins, the lagging-muscle coach and a body-part Progress; stage 4, stretch and squeeze cues.

## Bodybuilding mode: stage 3 done, check-ins and the lagging-muscle coach (version 1.4, both apps)
Plan: `docs/superpowers/plans/2026-11-14-grit-bodybuilding-mode-stage-3.md`.

What the lifter gets:
- A **check-in** every 4 weeks on the bodybuilding side (and at any time from Progress): every body part starts "on track", and the lifter marks the ones that are lagging and the ones growing well. The coach never judges this for them.
- **More work for a lagging body part**, as a suggestion in the Coach feed: one more set on up to two of its exercises, or else one new exercise. One tap adds it to the plan; nothing changes without that tap. If the plan has changed since, only what still matches is added, and the card says so.
- **A swap** when an exercise has been stuck for 3 weeks (no heavier weight and no more reps), with up to 3 alternatives that train the same body part. Body parts marked growing well are left alone: no more work and no swaps.
- A **Body parts** section on Progress: for each body part, its ratings at the last 4 check-ins, the sets done in the last 7 days, its weekly sets in the plan, and how many of its exercises are going up.
- Check-ins go in the backup, now version 4. A version 3 file still restores (with no check-ins); an older app refuses a version 4 file, so update both apps together.
- The powerlifting side is unchanged: no check-in, no new suggestions, the same Progress.

The sourced number (recorded in `CoachSources.kt`, `CoachSources.WEEKLY_CEILING`):
- At most **20 counted sets a week for one body part**: Baz-Valle et al. 2022, "A Systematic Review of The Effects of Different Resistance Training Volumes on Muscle Hypertrophy", J Hum Kinet 81: 199–210 (https://pmc.ncbi.nlm.nih.gov/articles/PMC8884877/). In trained men, 12–20 weekly sets per muscle was the best range for growth, with no added benefit above 20. The coach takes 20 as a ceiling, not an aim. Grit counts a set in full for every body part an exercise works, so a body part reaches 20 sooner than by the study's count: the ceiling errs on the side of less.

The designer's own numbers, not from the sources (for the owner to judge):
- A round of more work adds one set to at most 2 exercises for the body part, or else one new exercise.
- No exercise is taken above 4 sets.
- A new exercise is 3 × 8–12 at RPE 8, added at the end of a training day.
- A check-in falls due 28 days after the last one.
- An exercise is stuck when it has not moved for 21 days and was done at least twice in that time.
- No second swap for the same exercise within 28 days.
- Up to 3 alternatives with a swap.

The two whole-week limits, not from the sources, approved by the owner on 2026-11-14:
- No allowed source gives a ceiling on a whole week's sets. In its place the week grows slowly: at most 3 body parts get more work from one check-in, and no session grows above 10 exercises.

Not yet tried on real phones: the check-in line in README's manual checklist matters.

## Bodybuilding mode: stage 4 done, stretch and squeeze cues (version 1.5, both apps)
Plan: `docs/superpowers/plans/2026-11-15-grit-bodybuilding-mode-stage-4.md`.

What the lifter gets:
- **Two short lines for each exercise** on the bodybuilding side: where the muscle is stretched and where it is squeezed. They show on the Today card and in a "Stretch and squeeze" section of the form guide while bodybuilding mode is on.
- **One fixed line for holds and carries** (a plank, a carry, the Pallof press and the like), in the guide only: "No stretch or squeeze to aim for in this one." The Today card shows nothing for these. 13 exercises get the fixed line and 212 get two lines.
- Every catalogue exercise has its lines or the fixed line. The lifter's own exercises show none, and leave no gap.
- The powerlifting side is unchanged while bodybuilding mode is off, and switching the mode shows or hides the lines at once.

The idea's sources (recorded in `Sources.kt` as general sources, not shown in the app):
- Maeo et al. 2021, "Greater Hamstrings Muscle Hypertrophy but Similar Damage Protection after Training at Long versus Short Muscle Lengths", https://pmc.ncbi.nlm.nih.gov/articles/PMC7969179/
- "Lengthened partial repetitions elicit similar muscular adaptations as full range of motion repetitions during resistance training in trained individuals", https://pmc.ncbi.nlm.nih.gov/articles/PMC11829627/
- ACE, "Muscles in Motion" (a shortening muscle is the one doing the lifting), https://www.acefitness.org/fitness-certifications/ace-answers/exam-preparation-blog/3580/muscles-in-motion/

How the lines were written:
- The sources often say where a muscle is stretched, but not for every exercise, and none says where to squeeze. So each line is worked out from the exercise's own guide by rules F1–F7 in the plan: it names a muscle the exercise trains; the stretch is where the guide's steps make that muscle longest, never more range than the guide allows; the squeeze is the end of the guide's lifting step; nothing goes against the guide's cues, mistakes or safety notes; an exercise with no real stretch or squeeze gets the fixed line; each line is one plain sentence; and each exercise lists its ExRx or ACE pages.
- The allowed sites can be searched but not opened, so every source was checked against its search summary, not the full page. A few pages came back only as links, and a few supporting sentences came from the matching ExRx page in another muscle folder.
- An independent review checked every batch against each exercise's guide and the allowed sources, and the fixes it asked for were made, or settled by a ruling and listed under Follow-ups.
- Nothing claims a benefit for squeezing; it is there because the owner asked for the focus.

The designer's own calls, not from the sources, approved by the owner on 2026-11-16:
- Each line is at most 64 characters.
- The fixed line's wording, above. The owner approved the shorter line on 2026-11-16, after being asked, because the old ending ("…this one trains you to stay steady") fitted the sled push, the carries and the woodchop poorly.

Not yet tried on real phones: the stretch and squeeze line in README's manual checklist matters.

Follow-ups noticed during stage 4:
- The Seated cable fly guide has no mistake against going past the start.
- The Hack squat guide has no "stop if your hips leave the pad".
- On the web at 130% zoom on a 375 px phone, the Today card's RPE stepper runs past the edge (it did before stage 4). **Fixed in 1.6:** under 375 px the RPE stepper takes its own row, and the 130% zoom check covers the steppers again.

## Done: the new look (version 1.6, both apps)
Spec: `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md`. Plan: `docs/superpowers/plans/2026-11-16-grit-look-stage-1.md`. This is stage 1 of the new look and motion; nothing behaves differently.

What the lifter sees:
- **Soft layered cards, in dark and light.** Cards no longer have a thin border. They stand off the background by a step in tone and one soft shadow. In dark there is also a faint light line along each card's top edge. Menus and the picker's muscle tiles get the same; the shell's buttons lose their border and take no shadow. Rows inside a card keep a softer divider line. Rows in the exercise picker and on the Exercises tab get no shadow.
- **A dark grey accent on the powerlifting side, in place of chalk.** It covers the buttons, the current tab, switches, rest progress, the coach chip and the chart lines.
  - Dark: `#93979D`, with near-black ink `#0B0C0E`. Light: `#4A4D52`, with near-white ink `#F6F7F8`.
  - The soft accent backgrounds: `#2A2C2F` with `#DEDFE1` text in dark, `#DCDEE0` with `#2F3135` text in light.
  - **The owner chose the grey.** The examples' dark grey `#868A91` failed the contrast tests as text, so the owner chose option A: the lighter `#93979D` for both the fill and the text. Every colour pair passes the contrast tests.
- **Bodybuilding keeps its blue and the Barlow font.** Only its cards change.
- **State outlines stay as they were:** the selected chip and segment, the planned-set ring, the switch-off border, text-field borders and rings, the number field's underline and the focus ring.
- How it is drawn: the web uses one shadow token per scheme. Android draws the same shadows as soft copies of the card's shape (`dropShadow`), plus a 1 dp inner line for the dark highlight. So the shadows also show in the screenshots.
- **The rest bar has its own layer** on both apps (the card shadow), so in dark it no longer runs into an open card behind it.
- **Narrow phones (web), under 375 px wide:** on Today the RPE stepper moves to its own row under weight and reps (buttons stay 48 px, text 16 px), and the tab bar's pills may narrow. At 375 px and wider nothing moves. This includes 360 px, a common Android width.
- The screenshots on both apps were taken again. Not yet looked at on a real phone.

### iPhone and phone fixes (web app, also in 1.6)
The owner's list of nine, from commits `182f1ad` and `92704c9`:
1. **Home Screen setup: changed.** Numbers are no longer phone links; the manifest gets an id; a new maskable icon keeps the whole mark inside Android's round or squircle mask. The rest was already fine.
2. **Status-bar strip: changed.** In the installed iPhone app, the top of each screen sits below the status bar.
3. **Tab bar: changed.** Its height is one shared token now. In 1.6 it kept its full bottom inset; 1.6.1 applies the suggested 14 px trim (see below), because on a real iPhone the full inset left a tall empty band under the labels.
4. **View Transitions: do not apply.** Grit uses none.
5. **Theme colour: changed.** One theme-colour tag, set before the first paint on both sides, so the browser bar matches the page in dark and light.
6. **Narrow phones: changed.** A check at 306 and 320 px covers Today with an exercise open, the picker, Settings, Progress and Coach on both sides; it checks that nothing scrolls sideways, inside the scrolling screen too. It found the rest alert's segments and Today's RPE stepper (which was already too wide in 1.5) too wide; both are fixed. No root font-size steps were needed.
7. **Sound on iPhone: changed.** The tone unlocks on the first touch and recovers after a call or a trip to another app. A tone that cannot play within 1 s is dropped, so it never sounds long after the rest.
8. **Vibration: changed.** A new rest alert, "Sound + vibrate", where the browser can vibrate (Android). iPhones hide it and say "iPhones don't let web apps vibrate."
9. **Install card: changed.** On Android it says to open the browser's menu (⋮). Chrome's installed app shares the browser's storage, so on Android the separate-storage sentence is replaced (owner, 2026-11-16) by: "Your log stays in this browser, so the installed app opens with it. Export a backup now and then to keep a copy." iPhone keeps its sentence about Safari's separate storage.

Push notifications do not apply: Grit sends none.

### Done: iPhone bottom fix (1.6.1)
The owner tried 1.6 as a Home Screen app on iOS 26 and saw two things at the bottom of the screen, both now fixed:
- **A tall empty band under the tab labels.** The tab bar now keeps 20 px of the iPhone's 34 px home-bar space instead of all of it. The snackbar moves with it. Sheets and full-screen views still keep the full space, as the guide says.
- **A darker strip under the tab bar.** In the Home Screen app, the page's full height stops short of the bottom of the screen. The app's frame is now pinned to the screen's edges, so it always reaches the bottom. The page colour is now on the page's root too.
- New checks stand in for an iPhone's home bar in Chromium and measure both. Real phones: not tried yet.

### Done: the iOS 26 Home Screen gap, second fix (1.6.2)
In 1.6.1 the owner still saw a band of page about 55 pt high below the tab bar on the iPhone.
- **Cause:** a WebKit bug (301108). In a Home Screen app that draws under the status bar, iOS 26 makes the page's viewport short by the status bar's height, so anything sized to the screen stopped that much above the bottom.
- **Fix:** in the iOS Home Screen app only, the app's frame reaches down by the status bar's height too. It is capped at the full screen height, so it cannot run past the screen once Apple fixes the bug. The page behind gets the same minimum height. The same fix is used by another open-source app framework (frappe-ui).
- **Checked on the owner's iPhone (iOS 26, Home Screen app) on 2026-11-16: fixed.** Chromium cannot reproduce the bug, so the automated check only proves the rule is applied.

### Follow-ups noticed during the new look
- The Exercises tab speed check, in quiet timing against 1.5.0, had two single slow opens (80 and 90 ms). Both are still under the 100 ms limit, and the medians were the same as 1.5.0. Timed before the sound-engine fix; the release's quiet speed runs passed, but the tab was not re-timed against 1.5.0 after it.
- An Android test, `PlanBuilderScreenTest`, failed during full runs on a closed database connection. Fixed separately (commits `cc9a24d`, `6824288`): every screen and view-model test now closes its database only once nothing reads it.
- The picker's occasional slow first open in 1.6 (one run took 114 ms; the limit is 100 ms) came from the new iPhone sound unlock: it made the sound engine inside the first tap. The engine is now made just after start-up, so a tap only wakes it.
- "Sound + vibrate" wraps onto two lines on 320–390 px phones, so on most Android phones. The owner may prefer a shorter label.
- An Android app set to vibrate only comes back as "Sound + vibrate" when its backup is restored in an Android browser, since the web has no vibrate-only alert.
- Thumbnails in the exercise picker stay paused. They come after the look and motion work.

## Done: press feedback and vibration (version 1.7, both apps)
Spec: `docs/superpowers/specs/2026-11-16-grit-look-and-motion-design.md`. Plan: `docs/superpowers/plans/2026-11-16-grit-motion-stage-2.md`. This is stage 2 of the new look and motion.

- **Press:** everything you can tap sinks to 97% of its size when pressed and springs back when you let go. The tab bar's icons go to 92%, because they are small.
- **What buzzes:**
  - Log set and Missed give one short tick.
  - A new best for squat, bench, deadlift or strict press buzzes twice. The first set of a lift counts as a best.
  - Apply and Swap on the coach, Add to plan and Add to today give one tick.
  - Each step on Today's weight, reps and RPE steppers gives one tick.
  - Nothing else buzzes.
- **Where it works:** the Android app, and the web app on Android phones. iPhones can't vibrate.
  - The web app shows the switch wherever the browser says it can vibrate. In practice that is Android browsers.
  - Desktop Chrome shows the switch too, but nothing buzzes there.
- **The switch:** "Vibrate on taps" sits under Rest alert in Settings. It is hidden on iPhone. It is not in backups (owner, option A): it belongs to the phone, and after a restore it is on.
- **Backups:** 1.7 reads backups as before, and its backup readers ignore settings they don't know, so a later version can add a setting without breaking them.
- **Reduce motion:** with the phone's reduce-motion setting on, nothing sinks or springs.
- Not yet tried on a real phone.

## Done: things slide into place (version 1.8, both apps)
Plan: `docs/superpowers/plans/2026-11-16-grit-motion-stage-3.md`. This is stage 3 of the new look and motion.

- **What slides:**
  - Today: the card you open opens in place, and the one closing shrinks with its content. Logged sets slide in and out. Adding or removing an exercise slides the cards below.
  - Coach: a card you apply or dismiss slides away.
  - Plan: rows slide past each other when you move one, and slide in and out when you add or delete one.
- **What does not slide:** changing tabs, the exercise picker, the Exercises tab, dialogs and Progress.
- **Timing:** about 280 ms; nothing takes longer than 350 ms.
- **Reduce motion:** with the phone's reduce motion setting on, nothing slides.
- **On the web:** the tab bar, the header and the rest bar stay still while things slide.
- **A tap or click during a slide always lands:** it ends the slide at once and reaches what is under it.
- **Log set on the web:** the new set shows about one frame later than in 1.7 (roughly 10 to 14 ms), the frame the slide needs.
- **Fixed (found by the owner):** in the picker and on the Exercises tab, choosing **Chest** then **Barbell** left out Bench.
  - Now a body area also lists its main lift and that lift's variations: Bench under Chest, Squat under Quads, Deadlift under Hamstrings & glutes, Strict press under Shoulders.
  - They still show under Main lifts too. On the Exercises tab they sit under the chosen area's heading.
- **Fixed:** Plan rows now fit 306–320 px phones and 130% zoom.
- Not yet tried on a real phone.

## Done: charts fill in (version 1.9, both apps)
Plan: `docs/superpowers/plans/2026-11-16-grit-motion-stage-4.md`. This is stage 4, the last stage, of the new look and motion.

- **Lines draw in:** on Progress (powerlifting side), lines draw from left to right in 600 ms: the trend line, the total sparkline with its shaded area, and the bodyweight line. They draw again when you change the range.
- **Bars grow:** the weekly volume bars grow one after another, all done within 700 ms.
- **Ratio markers slide:** each marker slides into place in 450 ms. The bands themselves show at once.
- **What never moves:** numbers, labels and stat tiles.
- **Unchanged:** bodybuilding Progress has no charts, so nothing changes there. Lift history has no chart on either app.
- **Reduce motion:** with the phone's reduce motion setting on, every chart is complete at once.
- **Speed:** opening Progress is not slower. Its first paint stays within 10 ms of 1.8.
- Not yet tried on a real phone.

## The new look and motion: done (1.6 to 1.9)
- **1.6:** the new look: soft layered cards and a dark grey accent on the powerlifting side.
- **1.7:** everything you tap sinks and springs back, and the main actions buzz ("Vibrate on taps").
- **1.8:** things slide into place on Today, Coach and Plan.
- **1.9:** charts fill in on Progress.

**Open follow-ups:**
- Pictures in the web picker and plan builder: tried and waiting (see "Web picker thumbnails: tried, waiting"). Android has them since 1.10.
- "Sound + vibrate" wraps onto two lines on narrow phones.
- Two guide gaps: the Seated cable fly guide and the Hack squat guide (see the 1.5 notes).
- The Exercises tab still opens slowly now and then (single slow opens, still under 100 ms).
- The web Exercises tab breaks 4 long names inside a word at 288 px (a 375 px phone at 130% zoom): Smith shoulder press, Concentration curl, Bodyweight squat, Quadruped hip extension. It did in 1.9 too; found while trying the web picker.
- The Android Exercises tab at 130% text was not checked when the picker's large-text rule went in (1.10).

## Done: thumbnails in the picker (1.10, Android app)
Plan: `docs/superpowers/plans/2026-11-17-grit-picker-thumbnails.md`.

- **What you see:**
  - Plan's **Add** on a day, Today's **Add exercise** and the plan builder's exercise list lead each row with the Exercises tab's thumbnail. The picker has them on both sides; the plan builder is on the bodybuilding side only.
  - The tile, the picture and its colours are the tab's, in dark and light. Picker rows have no shadow.
  - Your own exercises have an empty tile, so the names stay lined up. The muscle grid and the chips are unchanged.
  - The picture is decorative: a row's spoken name, and its ticked state in the plan builder, are unchanged.
- **Names at once, pictures straight after:** a row comes up with its name and an empty tile at full size, and the picture fills the tile when it is ready, so nothing moves. A picture that is already ready shows in the row's first frame.
- **Narrow phones and large text:** the room for a row is the screen's width over the text size (`screenWidthDp / fontScale`).
  - Picker rows show the thumbnail from 320 dp of room.
  - The plan builder's rows, which also have a tick box, show it from 360 dp.
  - Below that a row has no tile at all, so no catalogue name breaks inside a word.
  - The Exercises tab is unchanged at every width.
- **The warm-up:** the picker and the plan builder ask for the tab's warm-up, in the order their own list shows.
  - Only the first request counts, so the one background worker still works every thumbnail out once per process, whichever list opens first. Rows on screen are served first.
  - Nothing is worked out on the main thread (`ThumbnailsTest`, which now covers the picker).
  - A list that shows no tiles (large text) asks for no warm-up.
- **The web app** is 1.10 too, with no visible change: its picker waits (next section).
- Not timed on a phone: the Android speed check is the main-thread test. Not yet tried on a real phone.

## Web picker thumbnails: tried, waiting
Owner decision 3 (2026-11-17): the web picker and plan builder stay without thumbnails, because no try kept the picker's speed. The Exercises tab is unchanged.

- **The bar**, against 1.9.0, alternating builds, 160 quiet rounds each:
  - the picker's median first open within 3 ms, and its worst within 10 ms;
  - a keystroke typed straight after the open within 5 ms of 1.9.0's median, and always under 100 ms;
  - the tab no slower.
- **Where the cost is:** drawing the pictures, about 10–12 ms on the open. The empty tiles and the watchers cost nothing measurable.
- **Round 1, one batch 50 ms after the names:** open median +2 ms, but its 95% interval reached 6.5 ms, so not proven. A keystroke straight after: +8 ms median, and 1 in 160 took 107 ms.
- **Round 2, 2 pictures at a time** (on-screen rows back to back, the rest 33 ms apart): open +3 ms, at the limit. A keystroke straight after: +9.5 ms median, a miss.
- **Round 3, 2 at a time with 33 ms after each:** a keystroke straight after passed (+4 ms median, worst 68 ms). The open missed: median 50 ms against 39 ms. Opens fall into a fast group and a slow group, and round 3 landed in the slow group in 64% of opens, against 47% for 1.9.0.
- In every round the first screenful's pictures were in within 250 ms (round 3: 112–183 ms). Each round moved about 10 ms of drawing; none removed it.
- **The untried lever:** a cheaper picture, such as one prebuilt image per thumbnail in place of SVG shapes. It would need its own plan, with its cost measured first.
- The rounds' code diffs are kept locally, outside the repo.

## Next project idea (owner, 2026-11-12): pictures on the Exercises tab
- Each exercise's guide shows a picture of a person doing it. First thoughts (not decided):
  - simple line drawings in one consistent style, two poses per exercise (start and end), working in dark and light;
  - vector images to keep the app small (roughly 3–8 MB for 225 drawings);
  - sources: a licensed illustration set (accurate, costs money, won't cover all 225) or generated drawings (cheap, fits the list, but every pose must be checked against the guide's sources). Free images (e.g. Wikimedia) are patchy and inconsistent. ExRx/ACE images are copyrighted and can't be used.
  - suggested first step: generate a trial batch of 10–15 and check accuracy before committing to all 225.
- Code side is small: an image per catalogue entry, shown at the top of the guide sheet in both apps.
- **Owner decided (final): no paid image service.** The figures are drawn in code (SVG) by Claude: solid figure shapes, plates and equipment to scale, the bar and plates in the theme accent, dark and light from theme colours. A draft is in `docs/library/pictures/bench-press-figure-draft.svg`. The earlier Gemini trial (`bench-press-example.jpg` and its brief) is kept only as a record of what was tried.
- Why it suits Grit: free, accurate by construction (each pose is drawn from the guide's positions), about 2–4 KB per exercise (under 1 MB for all 225), sharp at any size, and theme-aware.
- Build approach: a shared figure kit (body, limbs, bench, rack, barbell, dumbbell, cable, machine parts), so each exercise is two pose definitions rather than a drawing from scratch. Review each batch against its guide, as with the text.
- Show the two poses side by side, or let the lifter tap between them.

## How future projects are built (owner, 2026-11-12)
- While building a task, run only the tests for the part being changed.
- Run the full Android, domain, web and e2e suites once per task, before its commit, and once on main before pushing.

## Done (2026-11-13): app version and "AppsbyAdam"
- Both apps are version 1.1 and end Settings with "Grit 1.1 · AppsbyAdam"; the web line also shows the build's short commit id ("Grit 1.1 · a1b2c3d · AppsbyAdam").
- Bump Android `versionName`/`versionCode` and web `package.json` together for each update; a web test fails when they disagree.

## Done (2026-11-13): Android thumbnails off the main thread
- The Exercises tab no longer works a thumbnail out on the main thread. One background worker (`ThumbnailLoader` in `app/.../ui/library/Thumbnails.kt`) looks the figure up and places its shapes, one thumbnail at a time, and keeps every ready one for the life of the process.
- A row draws a ready thumbnail in its first frame; until then it shows its empty tile, the same size, so nothing moves. Rows on screen are served before the warm-up.
- The warm-up: when the tab first shows its list, the worker works out every catalogue thumbnail in the list's order, once per process. Since 1.10 the picker and the plan builder ask for the same warm-up in their own list's order; whichever list opens first sets the order, and it still runs once. A list that shows no tiles (large text, see 1.10) asks for none.
- The Android speed check is a test that no thumbnail is worked out on the main thread (`ThumbnailsTest`).
- Not measured: no real phone was available, so frame times before and after are unknown. Check by scrolling the Exercises tab fast right after opening the app.

## Done (2026-11-15): faster exercise lists (version 1.4.1, both apps)
- The exercise picker and the Exercises tab show the first screenful of exercises at once; the rest of the list follows straight after.
- Measured on the build machine, the 1.4 build against the 1.4.1 build, alternated, 32 fresh opens each: opening the tab went from a median of about 70 to 55 ms (worst 93–98 to 70–75 ms), opening the picker from about 56 to 47 ms (worst 71–95 to 56–69 ms).
- The two speed checks still hold the screens to 100 ms, now timed on the first screenful; they then wait for the whole list.
- "Create your own" appears below the list once the whole list is drawn.
- Android is unchanged.
- Not measured on a real phone.

## Follow-up (2026-11-13, measured 2026-11-15): the 100 ms speed checks have no headroom
The web app holds two screens to 100 ms on opening, after each keystroke and after each filter change: the exercise picker (`web/e2e/library.spec.ts`) and the Exercises tab (`web/e2e/pictures.spec.ts`), both tagged @speed.

Measured on 2026-11-15 on the build machine (production build, a fresh page for each open, timed the way the checks time it):
- **The Exercises tab is the tight one.** Opening it: median about 70–76 ms, 90% of opens under about 90 ms, worst 96–108 ms (30 opens, then 4 batches of 16). The one miss in two full e2e runs was this check (104 ms).
- **The picker has a little more room.** Opening it with the catalogue (225 rows): median about 55–64 ms, worst 86–93 ms, with one 125 ms outlier in 64 opens. Keystrokes and filter changes in the picker take about 20–75 ms (not logged for the tab). The 475-row picker check was not measured this way.

Where an open's time goes (about, from comparing builds):
- **About 25 ms is the measuring itself.** The tests fix the date with Playwright's clock, which also replaces the page's `requestAnimationFrame` and `performance.now`. So "two frames" is two 16 ms timer ticks, and a no-op takes 20–32 ms to time.
- **About 15–25 ms is the first open after a page load.** Every check times a first open; later opens of the same screen are faster (picker about 32 ms, tab about 50 ms).
- **About 15 ms is drawing the rows.** Drawing only 40 rows instead of all 225 cut the picker's median from about 60 to 47 ms and the tab's from about 70 to 58 ms.
- **About 10 ms is the tab's thumbnails.**

Tried and dropped:
- Working out the muscle groups at start-up: no measurable change.
- Memoising the rows: the tab draws its list twice on opening (once before its plan data arrives, once after), and memoising cut the second draw to 26 rows, but the open got no faster across 64 opens. Batches of the same build differ by 5–10 ms, so a gain of that size cannot be seen here.

What would give real room, each needing the owner's say because it changes what the checks hold the screens to:
1. **Draw the first screenful at once and the rest straight after.** Done in version 1.4.1 (see above).
2. **Time real frames** (untried: not yet shown that the date can stay fixed without faking the frame timer). Keep the fixed date but not the faked frame timer while timing, so the 100 ms is spent on the app rather than on timer ticks.
3. **Change the limit**, to one agreed for a real phone.

Nothing here says what a real phone does: no phone was available.
