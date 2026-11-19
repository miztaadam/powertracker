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
- The Seated cable fly guide has no mistake against going past the start. **Done in 1.13.1:** "Letting your hands go back past the level of your armpits."
- The Hack squat guide has no "stop if your hips leave the pad". **Done in 1.13.1:** "Stop sooner if your hips start to lift off the pad."
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
- Pictures in the web picker and plan builder: closed by the owner on 2026-11-18 after three trials (see "Web picker thumbnails: tried, closed"). Android has them since 1.10.
- The Exercises tab still opens slowly now and then (single slow opens, still under 100 ms).
- Tests that sometimes failed under load (looked into on 2026-11-18, test-only changes in 0721be6 and 4df3af3; no app cause found). Fixed in the tests: Android `PlanScreenTest.planAdd_tapAfterTheWindow_works` waited for the row in the database and then checked once that the add form had closed, which happens a moment later, when the write's answer reaches the screen (proven by holding that answer 300 ms); it now waits for both, as do `planAdd_changeNumbers_addStoresThem` and the rest-alert and vibrate-on-taps settings tests (the rest-alert one by the same shape, unproven: its failing line was never recorded, and it now prints the stored and shown values if it times out). Web `planAdd_cancel_storesNothing` polled 1 s for "Create your own", which is drawn only after the rest of the 225-row list; `createYourOwn` now waits for the list's frames, then finds it once. The web 5 s timeouts were plain work at about 80% of the limit: the Exercises tab tests are now laid out as on a phone (thumbnails near the screen only), and the browse and picker-search tests are split into parts under their own names. Verified: 3 quiet Android and 3 quiet web runs green, and one Gradle run with `npm test` back to back alongside, all green (web had failed in 5 of 5 loaded runs before). Still open: `planAdd_changeToOwnExerciseFails_afterThePickerWasClosed_theAddFormSaysSo` did not fail in 10 full runs here, so its cause is not established; its wait now prints where the message went, the test and wall clocks and the database work in flight when it times out. **Caught once on 2026-11-18,** in the full Android run on the merged 1.14 main (the same test also failed once in a full run during the 1.14 Android work; 2 of that branch's 6 full runs). What the recorder showed: at the first check the message was somewhere on screen, but the test's condition, the message shown over the add form, was not met. By 4.2 s on the test clock (2.7 s wall) the message was gone. The add form was still open, Tyre flip was not stored, and no database work was in flight. It passed 5 times alone and in the next full run. Not yet established: which copy of the message was showing. On Android the screen's own snackbar also gets it, behind the form (see the 1.12.1 notes), so the popup over the form may never have appeared. 1.14 changed Plan's first frame (`PlanUiState(loaded = false)`) and its header, which may have made it likelier. Next: the forced-order experiment, first in the next update. The capture is kept outside the repo. Next at risk on the web: the figure plugin test (about 2.7 s quiet, 4.9 s once under load).
- Done in 1.13.1 (see "Done: 1.13.1, the 1.13 follow-ups"): the two guide gaps, Seated cable fly and Hack squat.
- Done in 1.12.2 (see "Done: the 1.12.2 tidy-up"): the Android plan builder's 2 broken names at 320 dp × 1.3; every Plan write on Android catches a failure; a failed own-exercise save keeps what was typed.
- Done in 1.11 (see "Done: the 1.10 follow-ups"): the web tab's 4 broken names at 288 px; the Android tab at 130% text; the six stale web screenshots; the Today slide test (1cc526c, 80 of 80 runs, under emulated touch).

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

## Done: the 1.10 follow-ups (1.11, both apps)
Plan: `docs/superpowers/plans/2026-11-17-grit-1-11-follow-ups.md`. The owner said "Do them all" (2026-11-17). Three of the four are done; the web picker's pictures still wait (next section).

- **Android picker, at large text:** picker rows show the thumbnail from 309 dp of text room, down from 320 (13903f9). Text room is the screen's width over the text size (`screenWidthDp / fontScale`).
  - Measured on a 320 dp screen with the tile: 308 dp of room breaks "Chest-supported row" inside a word, and 309 keeps every catalogue name whole.
  - So 360 dp at 115% text and 412 dp at 130% now keep the pictures. 320 dp at 115% and 130%, and 360 dp at 130%, still leave them out.
- **Android Exercises tab, at large text:** tab rows show the thumbnail from 311 dp of text room (63ad807). Before, the tab showed it at every size.
  - On a 320 dp screen, 310 dp of room breaks "Chest-supported row" and 311 keeps every name whole. In 1.10 the tab broke 5 names at 320 dp × 1.15, 16 at 320 dp × 1.3 and 2 at 360 dp × 1.3.
  - So 320 dp at 115% and 130%, and 360 dp at 130%, leave the tile out, with no empty tile. 360 dp at 115% and 412 dp keep it.
  - At the default text size on 320 dp and up, nothing changes. A tab with no tiles asks for no warm-up.
- **Web Exercises tab, at large zoom:** under 320 px of viewport, a tab row has no thumbnail tile (e1402f2). A 375 px phone at 130% zoom is 288 px.
  - Smith shoulder press, Concentration curl, Bodyweight squat and Quadruped hip extension no longer break inside a word. From 320 px the tab is unchanged.
  - New e2e check: `exercisesTab_namesWholeAt288`, at 288 and 320 px.
- **Stale web screenshots:** `today-install`, `session-saved` and `lock-setup` (dark and light) are re-shot with the install card's wording since ca5877f (e1402f2).
- **Two test fixes:**
  - The Today slide test (three quick Log set taps) now waits for the card to scroll into view before the first tap (1cc526c).
  - The `lock-setup` screenshot now also waits for the Today screen behind the dialog: the "Install Grit" heading and its icon (2a59dd7). Before, 1 of 20 shots caught it half-loaded; after, 0 of 20, twice.
- **Found, not fixed in 1.11 (done in 1.12.2):** the Android plan builder breaks 2 names, Chest-supported row and Chest-supported rear delt raise, at 320 dp × 1.3, even with no picture. The row's tick box, equipment and info button leave too little room. It was already so in 1.9. The existing large-text test checks only 360 dp. A fix needs a smaller gap or hyphenation: the owner's or planner's call.
- **The web picker and plan builder** have no thumbnails in 1.11, as in 1.10.
- **Tests changed** (all follow from the new Android floors, and the web is unchanged):
  - `ExercisePictureTest.picker_namesWholeAtFontScale1_3`: "no tiles" became a per-case flag, since at 309 dp of room the tile now shows.
  - `ExercisePictureTest.picker_thumbnailEdge_319hides_320shows` became `picker_thumbnailEdge_308hides_309shows`.
  - `ThumbnailsTest.tab_stillWarmsUp_afterATilelessPicker` is replaced by `tab_largeText_startsNoWarmUp` and `tab_stillWarmsUp_afterATilelessPlanBuilder`, because the picker's floor is now below the tab's, so a tile-less picker under a tiled tab can't happen.
- Not yet tried on a real phone.

## Web picker thumbnails: tried, closed
**Closed by the owner on 2026-11-18.** No pictures in the web picker or plan builder. Reasons: decoded-image memory (about 24–54 MB), sheets that would have to be made in a browser and committed for every picture change, and slower first opens; Android already has pictures. The data below stays in case this is reopened. The web picker and plan builder have no thumbnails in 1.10 or 1.11. The web Exercises tab keeps its SVG pictures. The spike's files are local only and will be lost, so this section keeps what is needed to rebuild option B later.

### 1.10: three ways of scheduling the same SVG pictures (owner decision 3)
- Each tile was a React subtree of SVG shapes, about 10–12 ms of drawing on the picker's open.
- Round 1 (one batch 50 ms after the names), round 2 (2 pictures at a time) and round 3 (2 at a time with 33 ms after each) each moved that drawing rather than removing it, and each missed the bar on the open or on a keystroke straight after.
- The first screenful's pictures were in within 250 ms in every round.

### 1.11: a cheaper picture (follow-up Task 1). It missed, so nothing was built.
**Feasibility: the colours.** Read from the `.fig` role classes, over all 225 catalogue thumbnails:
- Six roles: BODY (225 figures), EQUIPMENT (225), GAP (225), BODY_FAR (188), ACCENT (155), ACCENT_FILL (103).
- Each figure uses 3 to 6 colours: 3 in 12 figures, 4 in 71, 5 in 51, 6 in 91. None uses one.
- Every role changes with dark and light. ACCENT, ACCENT_FILL, GAP (the tile's surface) and BODY_FAR (text mixed with the surface) also change with the side. That makes **4 palettes**.
- GAP paints the surface colour over shapes drawn before it, and ACCENT_FILL is 10% see-through. So drawing order matters, and **no single mask, nor one mask per colour, can rebuild the picture**. A baked-in image needs one set per palette (2 schemes × 2 sides).
- About 30 shapes per figure (5370 capsules, 623 circles, 527 polygons and 219 rects in all), in about 9 runs of the same colour (17 at most).

**What each format costs** (all 225 figures):
- Size:
  - today's compact shape data: 181 KB (52 KB gzipped);
  - the same shapes as SVG markup: 1.38 MB (195 KB gzipped);
  - merged paths, one `<path>` per run of a colour: 940 KB (210 KB gzipped);
  - SVG data URIs: 1.99 MB per palette, about 8 MB for 4;
  - a 3x PNG sheet: 1.05 MB per palette, about 4.2 MB for 4.
  - For scale, the whole `dist` was 2.2 MB, with a 1.88 MB JS chunk.
- Main-thread time, on a test page with 225 picker-like rows (Chromium, 375 × 812 at 3x, the DevTools task time over 400 ms):
  - empty tiles: 10–15 ms, the floor;
  - today's SVG for the first 24 rows: +15 ms built with DOM calls, +5 to 10 ms set as one `innerHTML` string. So most of 1.10's cost is the browser's SVG work, not React;
  - merged paths: +1 ms for 24 rows, +15 ms for all 225;
  - `<use>` of one symbol sheet: +2 to 5 ms for 24 rows;
  - (A) an SVG data URI as each row's background: +110 ms. One external SVG sheet with `<view>` fragments: seconds;
  - (B) a PNG sheet served over HTTP: +6 ms, all after the first two frames. From a blob URL +65 ms; one PNG per tile +140 ms.

**How it was timed** (to reuse for B):
- Picker open with Powerlifting, 375 × 812 at 3x, Chromium. Builds alternate each round and their order flips every round. Only quiet rounds count.
- Each build waits for `serviceWorker.ready` and network idle before the tap. For B, its sheet URL is decoded with `new Image().decode()` before the tap.
- Measures: the picker's open, and a keystroke typed straight after it.
- Pass rule: the A/A noise is the bootstrap 95% interval of the median of (base2 − base1). The candidate passes a measure when the median of (candidate − mean of the two bases) is no bigger than the larger of that interval's two ends, taken without sign (for [-3, 1] the bound is 3).
- Slow opens are those from 45 ms. The candidate's share must be no higher than the higher base's. Every keystroke must stay under 100 ms.

**Spike, 40 rounds each, at equal bundle sizes** (the fairer second run). Times in ms; "paired" is the median difference from the mean of the two bases, with its 95% interval.

| build | open median | worst | slow ≥45 | open paired | key median | key worst | key paired |
|---|---|---|---|---|---|---|---|
| base1 | 32.0 | 59 | 28% | | 38.0 | 45 | |
| base2 | 33.0 | 56 | 25% | A/A +0.0 [-3, 1], bound 3 | 36.5 | 50 | A/A -1.0 [-4, 2], bound 4 |
| empty tiles | 34.0 | 54 | 30% | +0.0 [-5, 4] within | 36.5 | 50 | -1.5 within |
| C, merged paths | 51.5 | 73 | 88% | +15.3 [8.75, 19.25] over | 48.5 | 69 | +10.3 over |
| B, sheet decoded first | 35.5 | 72 | 35% | +0.0 [-4.25, 3.5] within | 34.0 | 59 | -2.0 within |

B's slow-open share (35%, 14 of 40, against at most 28%) was over the rule, but not beyond chance at 40 rounds. That is why the 160-round run followed.

**Decision run, 160 rounds each:**

| build | open median | worst | slow ≥45 | open paired | key median | key worst | key paired |
|---|---|---|---|---|---|---|---|
| base1 | 33.5 | 70 | 27% | | 36.0 | 55 | |
| base2 | 33.0 | 69 | 19% | A/A -1.0 [-1, 0], bound 1 | 35.0 | 52 | A/A -0.5 [-1, 1], bound 1 |
| empty tiles | 33.0 | 62 | 19% | -1.5 [-3, -0.5] within | 34.0 | 51 | -1.5 within |
| B, sheet decoded first | 33.0 | 69 | 19% | -0.5 [-1.75, 0] within | 35.0 | 52 | -1.0 within |

**Why vector loses:**
- In an earlier 40-round run, every vector form missed: 1.10's React SVG +13.3 ms on the open, the same markup set by `innerHTML` +12.0, merged paths +8.3 to +9.3, SVG data URIs +31.0. Caveat: that run's builds differed in size by up to 2 MB, and it did not wait for the service worker. The second run, at equal sizes, confirms it (merged paths +15.3).
- On the test page merged paths took only +1 ms of main-thread time, yet the open is +8 to +15 ms. The likely reason: the open waits for the next frame, and that frame waits for the vector paths to be rasterised. So drawing antialiased paths is on the open's path whatever the format, React or not.
- Empty tiles cost nothing. Only a picture that is already a decoded bitmap reaches the baselines' speed.

**B passes only when its sheet is decoded before the first open.**
- Cold B, with the sheet precached but not decoded (40 rounds): open +5.8 ms [0, 13] against a bound of 1, slow opens 50% against 13% and 15%; keystroke straight after +19.3 ms [12.25, 23] against a bound of 2. It misses.
- So B needs a warm-up that decodes the current palette's sheet at app start or at idle, off the main thread, before the first open. That is start-up work and memory: about 18 MB per decoded 3x sheet.

**B's sizes** (all 4 palettes, measured with Chromium's encoders):
- One sheet is 2520 × 1800: 225 tiles of 168 × 120, which is a 56 × 40 tile at 3x. Used as each row's CSS background.
- 3x PNG: 4.25 MB (about 1.05 MB per palette). 2x PNG: 2.50 MB.
- WebP at quality 1: 2.84 MB at 3x and 1.55 MB at 2x. This is lossy, so the pixels change.
- Lossless WebP and PNG optimisers were not available, so their gain is not measured.
- A 3x sheet is scaled down on a 2x screen. Native 2x sheets add more size.

**The rasteriser question:** nothing in the tree can rasterise in Node, and Vercel's build has no Chromium. The likely choice is a native npm package (for example `@resvg/resvg-js`), a new dependency on the deploy build. Its antialiasing is not Chromium's, so the pictures would differ from 1.10's at edge level. Then either the tab's screenshots change, or the tab keeps its SVG and the app ships both formats.

**Merged paths are not pixel-exact.** Against today's markup, at 2x and 3x in all 4 palettes:
- no holes and no colour changes, but 100k to 160k edge pixels differ per palette (2–3% of the area);
- about 10 per tile differ by more than 32 levels, up to 94 to 120, on thin gap outlines.
- The cause: Skia antialiases a filled outline differently from a round-capped stroke or a circle, and overlapping edges of one colour only once. Today's markup set by `innerHTML` is pixel-exact.

**Not measured for B:** the 250 ms pictures-in check, the Exercises tab, and the bodybuilding side. The spike had one palette (powerlifting, dark).

**The next step, untried:** every pixel is a fixed blend of four colours (text, muted, accent and surface). So one set of 3 alpha sheets, stacked as CSS masks in the theme's colours, might serve all 4 palettes. Spike it for size, cost and exactness before building anything.

### After 1.12.1: one set of theme-coloured masks (owner's option 2). It missed, so nothing was built.
- **Idea:** 3 alpha sheets (text, muted, accent) painted as CSS masks in the theme's colours, so one set covers all 4 palettes.
- **Pixels:** close, not exact. In a test grid, 5–7% of tile pixels differ, about 80% of those by 1 level, worst 8, mostly on edges. In the app's tab at 3x, up to 33 levels, but today's SVG itself moves by up to 32 there. The tab's committed screenshots would change.
- **Size:** 273 KB (2x) or 432 KB (3x) as optimised PNG; 221/351 KB as lossy WebP. Option B needed 2.5/4.25 MB. Decoded memory, worked out not measured: about 24 MB (2x) or 54 MB (3x).
- **Speed, the briefed build (decoded ahead):** missed at 40 rounds: open +11.5 ms (powerlifting), +8.5 (bodybuilding), keystroke +5.8, slow opens (45 ms or more) 55–75%. Cold (not decoded) is about the same. The cost is the first paint of the masks, not the decode, so it got no 160-round run.
- **With one hidden masked tile painted at start-up (beyond the brief), 160 rounds:** open and keystroke within the bound, every keystroke under 100 ms, but slow opens (45 ms or more) were 33% against 9% and 14% for the two comparison builds. Worst open 70 ms, the same as the comparison builds, so no open came near 100 ms. With a real 1 s wait before the tap: still 40% against 18%. Bodybuilding at 40 rounds: open +4.0 against a bound of 3, slow opens 48% against 30%.
- **Start-up:** decoded about 220 ms after load; +17 ms main-thread time (+25 ms with the primer).
- **Pictures within 250 ms:** decoded 35–54 ms after the tap warm, 78–101 ms cold. On screen is not established: the only screenshot check came back 416–542 ms after the tap.
- **Exercises tab:** 5–12 ms faster to open with masks than with its SVG, and lighter on scroll.
- **Making the sheets:** `@resvg/resvg-wasm` installs without a native build (not tried on Vercel itself) but differs from Chromium at edges by up to about 98 levels, so the sheets would have to be made in Chromium and committed.
- The trial's tools and diff are local only (`.superpowers/…/mask-trial/`).

## Done: choose sets and reps when adding (1.12, both apps)
Spec: `docs/superpowers/specs/2026-11-17-grit-plan-add-sets-reps-design.md`; plan: `docs/superpowers/plans/2026-11-17-grit-plan-add-sets-reps.md`. The owner asked (2026-11-17): "When inputting exercises into my plan, I want to be able to select the reps sets."

**What the lifter gets:**
- On Plan's **Add** on a day, picking an exercise opens the sets and reps form, titled "Add to <Day>", with an **Add** button. Nothing is stored until **Add**.
- **The form starts at the same ready-made numbers as before:**
  - powerlifting: 3 × 5 for a compound lift and 3 × 10 for an accessory, at the default RPE;
  - bodybuilding: 3 × 8–12 @ 8, with Reps from and Reps to.
- **Add** stores exactly what is on the form, at the end of the day, with one tick. **Cancel**, back or a tap outside stores nothing and does not tick.
- **The form's limits are Edit's:** sets 1–10, fixed reps up to 20, a range's top no lower than its bottom and up to 30, and RPE in steps of 0.5.
- **Your own new exercise,** made from the picker's list, opens the same form once it is made. Cancel keeps it in your library and adds nothing. If making it fails, no form opens and the failure shows as before.
- **Unchanged:** editing a row, Today's **Add exercise**, the plan builder, the Exercises tab's "Add to plan", the ready-made numbers and the picker.
- The trade-off is one extra tap (**Add**) when the ready-made numbers are already right.

**Calls made while building:**
- **Change keeps the numbers on the form:** swapping the exercise inside the add form does not overwrite what you set, as in Edit. **Add** then stores the new exercise with those numbers.
- **The web form opens in the picker's place, with no blank moment:** the numbers are worked out at once from what the screen has already read, and the picker stays mounted but hidden while its form is open. Focus goes straight from the row into the form, and back to the day's **Add** when both close (8e0f7e1).
- **A double tap on Add stores one item, tested on both apps:**
  - Android: `planAdd_doubleTapAdd_storesOne`. With the guard removed, it stores 2.
  - Web: `planAdd_twoClicksBeforeARender_storeOne` (two clicks before React renders). With the guard removed, it stores 2. `planAdd_doubleTapAdd_storesOne` passes either way, because the form closes on the first tap.
- **Android keeps the add form across rotation and process death,** with the picked exercise and the numbers, and stores nothing until **Add** (`planAdd_survivesRecreation`).
  - Edit, Baseline and the picker still close on rotation, as before. The plan assumed Edit already survived it; it did not, and making them survive is a small follow-up if wanted.
- **The side is read at the tap:** **Add** stores on the side the app is on at that moment, as the Exercises tab's "Add to plan" does. The form's shape (a range or fixed reps) comes from the side when the exercise was picked, so the two can differ only if the side is switched while the form is open.
- **The Android add-form screenshots are a stand-in.** The capture cannot see a dialog's own window, so they draw the real form fields in a frame built from the dialog's defaults, over the Plan screen. The scrim covers only the content area there; on a phone it covers the whole screen.

**Existing tests changed:** each picked on Plan's Add and expected the item stored at once, and each now also taps **Add** on the form. Nothing else in them changed, except where noted.
- **Android:**
  - `PlanScreenTest`: `addPlanItem_showsUnderWeekday` (its comment too) and `addToADay_ticksOnce_changeExercise_doesNotTick`.
  - `ExercisePickerTest`: `pickerRow_doubleTap_addsOnce`, `createCustom_withCatalogueName_usesTheCatalogueEntry` and `compoundDefaults3x5_accessory3x10`, each in both halves; `pickerInfo_opensGuideWithoutPicking_inThePlan` (comment); and `restoredBuiltIn_appearsOnce_andIsReused`.
  - `BodybuildingScreensTest.plan_newBodybuildingEntry_is3x8to12at8_onItsOwnSide` called the old three-argument `add`, which is gone. It now calls `draft`, then `add` with the draft's numbers, and its assertions are unchanged.
- **Web, vitest:**
  - `PlanScreen.test.tsx`: `addPlanItem_showsUnderWeekday`, `upDown_reorderWithinTheDay`, `customExercise_isCreatedAndAdded`, `failedSave_saysSo`, `pickerAdd_ticksOnce` and `aFailedPickerAdd_doesNotBuzz`.
  - `ExercisePicker.test.tsx`: `pickerInfo_opensGuideWithoutPicking_inThePlan`, `restoredBuiltIn_appearsOnce_andIsReused`, `compoundDefaults3x5_accessory3x10`, `createCustom_withCatalogueName_usesTheCatalogueEntry` and `pickerRow_doubleTap_addsOnce`.
  - `bodybuilding.test.tsx`: `addingAnExercise_gives3SetsOf8To12AtRpe8_onThatSideOnly`.
- **Web, e2e:**
  - `plan.spec`, "the plan is edited through its dialogs…". It also checks that the add form fits 375 px with full-size targets. It leaves out only the hidden picker's controls, the picker being still mounted under the form (narrowed in the review fixes, below).
  - `narrow.spec`, "a plan row moves and nothing overflows" (3 viewports).
  - `library.spec`, "the muscle grid fits…" and "the picker fits a 375 px phone…".
  - `bodybuilding.spec`, "switching sides…".

**New tests and screenshots:** the `planAdd_…` tests in `PlanScreenTest` and `PlanScreen.test.tsx`; the e2e `planAdd_chooseSetsReps_storedAndShownInRow` (Bench on Monday, 5 × 3, Add, and the row reads 5 × 3 after a reload); and `plan-add-form` and `plan-add-form-bodybuilding` shots, dark and light, on both apps. Settings shots are re-shot for 1.12; no other screenshot changed.

Not yet tried on a real phone.

### Review fixes (1.12)
- **A double tap on a picker row no longer reaches the new form.** The form opens under the finger, so the second tap used to land on its **Add** (stored at once) or on the scrim (cancelled). Now, for 300 ms after it opens, the add form ignores taps on **Add**, **Cancel** and outside it, on both apps.
  - **Still live in that moment:** the steppers and **Change** (a stray tap there only moves a number, which stays on screen), the keyboard (Enter, Space, Escape), and Android's back.
  - **Screen readers:** Android's accessibility click is not held, since it does not come through a touch. On the web, a screen reader that activates with a plain click is not held, but one that simulates a tap (VoiceOver on iPhone) is held like a tap, for the same 300 ms.
  - **How:** Android drops touches on the two buttons until the moment is over, and turns off dismiss-on-outside-tap meanwhile. The web stops tap clicks (`detail` 1 or more) on the buttons and the scrim, measured on the clock events are stamped with, since `performance.now()` is faked in the speed checks and `Date` is fixed in e2e.
  - **Edit's form is unchanged.** A restored Android add form (after rotation) holds taps for a moment again, which does no harm.
  - **Tests:** `planAdd_secondTapOfADoubleTap_isIgnored` and `planAdd_tapAfterTheWindow_works`, on both apps. Each fails with the window taken out, and each kind of tap (Add, Cancel, outside) fails its own check when only its hold is taken out.
  - **Existing tests:** every test that picked and then tapped **Add** or **Cancel** at once was itself a double tap, so each now waits the moment out first (a clock step in the shared helpers, a 300 ms wait in e2e). These are Android's `PlanScreenTest` helper and its two inline taps, and `ExercisePickerTest.confirmAdd`; the web's `addForm` and `confirmAdd` helpers, `bodybuilding.test`, and the focus test's Cancel; and in e2e, plan, library, narrow and bodybuilding.
- **Android, two new tests:**
  - `planAdd_ownExerciseFails_opensNoForm`: the database insert itself fails. No form opens and nothing is stored. In 1.12 nothing on Android caught that failure: it escaped the view model's coroutine, which on a phone crashes the app, and the test expected that. The web shows "Couldn't save" instead. **Done in 1.12.1** (below): Android now catches it and says so, and the test is now `planAdd_ownExerciseFails_saysSo_opensNoForm`.
  - `planAdd_survivesRecreation_bodybuilding`: the restored form keeps Reps from and Reps to, and **Add** stores `repsHigh`.
- **Web e2e:** the 375 px tap-target check leaves out only the hidden picker's controls, not every 0 × 0 control on the page.

## Done: a failed own-exercise save no longer crashes Android (1.12.1, both apps' version)
- **What the lifter sees:** if creating their own exercise fails at the database (a full disk, a broken database), Android shows "Couldn't save — try again." in a snackbar, as the Exercises tab and the web already say a failed save. Before, the failure escaped the view model's coroutine and crashed the app.
- **Every place that creates one:** Plan's Add, **Change** in the plan's add and edit forms (one view model call, `PlanViewModel.addCustomExercise`), and Today's **Add exercise** (`TodayViewModel.createExercise`). The Exercises tab has no "Create your own".
- **On failure:** no form opens, Change leaves the form on its exercise, Today adds no card, nothing is stored, and nothing buzzes. Cancelling is not caught: it is passed on.
- **How:** the insert is caught as `addOnce` already catches its write. Plan's view model sends the message to the screen's snackbar (as Coach does); Today's sends a `SaveFailed` event. Plan and Today gained a snackbar for it.
- **Behind the form:** on Change, the message shows on Plan's own screen, under the open form's scrim, as the web's does under its form.
- **Tests (Android):** `planAdd_ownExerciseFails_saysSo_opensNoForm` (was `planAdd_ownExerciseFails_opensNoForm`, which expected the crash), `planAdd_changeToOwnExerciseFails_saysSo_formKeepsItsExercise`, `planEdit_changeToOwnExerciseFails_saysSo_rowKeepsItsExercise` and `TodayScreenTest.createYourOwn_whenTheSaveFails_saysSo_addsNothing_noTick`. Each fails without the catch. They wait for the message instead of a fixed 500 ms. The `ExpectCrash` rule, now unused, is gone.
- **Version:** Android 1.12.1 (versionCode 18), web 1.12.1. Settings shots re-shot on both apps; no other screenshot changed.
- **Not changed in 1.12.1:** other writes still did not catch a failure: among others, Plan's Save (edit), move, delete and known best. **Done in 1.12.2.**
- **Review notes (open, low):** Today's `SaveFailed` goes through a `MutableSharedFlow` with no replay, so if Today is off screen when the insert fails (you left the tab, or the screen is being recreated), the message is lost: there is no crash, but no message either. Plan's buffered `Channel` has no such gap.

## Done: 1.12.3, "Sound + vibrate" on one line (web; both apps' version)
The owner chose option A (2026-11-18): on the web, Settings' rest-alert segments are sized to their own labels, as Android's are.

- **Web Settings:** each segment starts at its label's width and takes an equal share of what is left (`SegmentedChoice`'s new opt-in `sizeToLabels`; every other use keeps equal segments). The labels' side padding is 1 px instead of 2.
  - At 306, 320, 360 and 390 px every label is on one line at 15 px, whole, with its text unchanged. The row still fills its width and every segment is 48 px tall. From 340 px wide every segment is also at least 48 px wide; at 320 px "Off" is about 31 px wide (it was about 60 with equal segments).
  - At 130% text on a 320 px phone (246 px of page) there is not room for one line: "Sound + vibrate" wraps at its spaces, onto three lines ("Sound" / "+" / "vibrate"), never inside a word and never clipped.
  - New e2e check in `web/e2e/narrow.spec.ts`, on a browser that can vibrate.
- **Version:** Android 1.12.3 (versionCode 20), web 1.12.3. Settings shots re-shot on both apps (the web ones also show the new row).

## Done: the 1.12.2 tidy-up (both apps)
Plan: `docs/superpowers/plans/2026-11-18-grit-1-12-2-tidy-up.md`. The owner said "go ahead" (2026-11-18).

- **A failed plan write no longer crashes Android.** Add, Save (edit), Delete, the up and down arrows and "Set best" now go through one helper in `PlanViewModel` that catches the failure and says "Couldn't save — try again." in Plan's snackbar. Cancelling is passed on. `addOnce` and `addCustomExercise` are unchanged.
  - **Move** was two separate writes; both are now one transaction, so a failure leaves neither row moved.
  - **A failed Add** keeps the add form open with its numbers, and Add works again. The form now closes, and ticks, once the item is stored rather than at the tap. The message shows in Plan's snackbar, under the form's scrim.
  - **Web:** all five writes already said a failure; new vitests confirm save, remove, move and setBaseline. The web's failed Add closes its form, as before.
- **A failed own-exercise save keeps what was typed** (Plan's Add, Plan's Change and Today's Add exercise, on both apps). The create form stays open with the name, category, parent lift and muscles; the message is shown over the form (Android: a popup at the foot of the screen; web: a snackbar inside the dialog); Create works again. A successful create closes it, and the next one starts empty.
  - Android keeps the form across a rotation wherever the picker itself survives one: Today and Plan's Change. Plan's Add picker still closes on rotation, as before (not in the add form's saved state).
  - On Android the screen's own snackbar also gets the message, behind the picker; on the web only the form says it.
- **Narrow phones:**
  - **Android plan builder:** a row with no thumbnail closes up the gaps beside its tick box and before its equipment (8 and 12 dp to 4 and 8). Chest-supported row and Chest-supported rear delt raise no longer break inside a word at 320 dp × 1.3. Rows with a thumbnail, the picker and the tab are unchanged, and no screenshot changed. The large-text test now checks 320 dp too.
  - **Android Settings:** each rest alert is whole on one line at 320 dp and at 360 dp × 1.3, at 15 sp (new check); nothing changed. At 320 dp × 1.3 (246 dp of room) all three labels are cut short ("Vibrate", "Sound +", "Of"): below the plan's bar, left open.
  - **Web Settings, not changed (stop rule):** the four segments share the row equally. At 320 px each has 56 px of text room, and "Sound + vibrate" needs 84 px even at 12 px; even the whole screen split four ways (80 px) is too narrow. At 360 px it has 66 px, at 390 px 73 px. Content-sized segments, as Android has, or a shorter label would fit: the owner's call.
- **Version:** Android 1.12.2 (versionCode 19), web 1.12.2. Settings shots re-shot on both apps; no other screenshot changed.

Not yet tried on a real phone.

## Done: skip a planned exercise (1.13)
Spec: `docs/superpowers/specs/2026-11-18-grit-skip-planned-exercise-design.md`; plan: `docs/superpowers/plans/2026-11-18-grit-skip-planned-exercise.md`. The owner asked (2026-11-18): "It should leave today's list and the coach should know."

**What you see on Today:**
- Each planned exercise with no logged sets has a ⋮ with **Skip today**: on the start card's rows before a session, on the folded rows, and on the open exercise card. A row with sets, or an exercise you added today, has none.
- **Skip today** opens "Why skip <name>?" with four answers: **Busy or no equipment**, **Pain**, **Out of time** and **Just skip** (no reason). Back, Escape or a tap outside cancels: nothing is stored and nothing buzzes.
- The exercise leaves the list, with one tick, and a snackbar reads "<name> skipped." with **Undo**. Two plan lines of the same exercise leave together.
- A "Skipped today" line at the end of the list names each skipped exercise with **Bring back**, which returns it to its place. Adding a skipped exercise again (Today's Add exercise, or the Exercises tab's Add to today) brings it back the same way.
- No session is needed: a skip belongs to the date and the side shown.
- With every planned exercise skipped, the start card reads "WEDNESDAY · 2 SKIPPED", the count on the "Skipped today" line (1.13.1; in 1.13 it read "WEDNESDAY · 0 EXERCISES"); in a session the usual "No exercises yet" stays above the line.
- Finish's summary adds ", n skipped": "No new coach notes, 1 skipped".
- Names on Today's rows stay whole: a long name wraps between words, and its plan goes under it.

**What the coach does:** a card comes at the moment of the skip, on the skip's side. Skips are counted per exercise and per side over the last 28 days, this skip included.
- **Pain:** a card every time, unless an open pain card for that exercise is already showing (then no second one), or one was dismissed in the last 7 days (1.13.1, the quiet spell: counted from the day the card was made, as the 7-day rule counts, so a card made today or up to 6 days ago holds it back and one made 7 or more days ago does not). Either way the hold still applies, since it comes from the skip, not the card. An applied pain card starts no quiet spell, and the quiet spell covers main lifts' Dismiss-only cards too. On a main lift (squat, bench, deadlift, strict press), or with no alternative left: "You skipped <name> because of pain. Its weight won't go up until you do it again. If the pain persists or gets worse, see a physician." with Dismiss only. On an accessory: the same with up to 3 alternatives for the same muscles (the coach's usual swap list), each a "Swap for X" button.
- **Busy or no equipment:** on an accessory, from the 2nd busy skip in 28 days: up to 3 alternatives on different equipment. "You've skipped <name> twice in 4 weeks because you were busy or had no equipment. Swap it for one of these?" With no other equipment it falls through to the next rule.
- **Any reason but Out of time:** on an accessory, from the 3rd such skip in 28 days: "You've skipped <name> 3 times in 4 weeks. Keep it in your plan, or swap it for one of these?", with up to 3 alternatives.
- **Out of time:** nothing.
- The texts give the real count ("twice", "3 times", "4 times").
- **One card at a time:** no new busy or repeat card while a skip or swap card for the exercise is open on that side, or within 7 days of the last skip card. A dismissed card holds back a new busy or repeat card of the same or a lower rank (pain, then busy, then repeat) for 28 days. Skip cards do not count towards one card per lift per week.
- **A pain card is held back only by an open pain card** for the exercise on that side, **or by a pain card for it on that side dismissed in the last 7 days** (1.13.1); the weight is held either way. An open busy or repeat card for it is deleted and the pain card takes its place. An open swap card stays, and the pain card comes as well. Busy and repeat cards, open or answered, the 7 days after any other skip card and the 28-day dismissal memory do not hold back a pain card. A dismissed pain card still holds back busy and repeat cards for 28 days, as before.
- **Swap** replaces the exercise in every plan item on that side, keeping day, order, sets, reps and RPE. A skip card hides once its exercise has left that side's plan, and comes last in the Coach feed: tag "Skipped", title "<name> skipped".
- **Undo and Bring back** delete the skip and its card if still open. An applied swap or a dismissal stays. Skipping again with another reason replaces the open card. Undoing a pain skip does not bring back a busy or repeat card it replaced.

**The pain hold:**
- The latest pain skip of an exercise holds it on both sides until a session on either side, after the skip day, has a set of it. Today's own session releases it from tomorrow, so nothing moves mid-session. There is no time limit.
- **It blocks increases only, reps included.** Powerlifting: no more weight, and no extra rep at the same weight that progression would add. The held aim is the last top set's weight, at the planned reps or the last reps if higher. Decreases still apply (the 5% drop after a missed top set, a drop after too high an RPE), and so do deloads and easing back in.
- **Bodybuilding:** no weight increase is given, and the reps aim does not reset; the target is "Beat it" at last time's weight, with the usual aim of last time's reps + 1, up to the top of the range.
- A pain skip, and any Undo or Bring back, delete today's open target for that exercise on both sides, and it is worked out again at once, held or not.
- The pain line's sources are in `CoachSources.kt` (`SKIP_PAIN_PHYSICIAN`, `SKIP_PAIN_TRAIN_AROUND`).

**Storage and backups:**
- A skip is its own record: date, side, exercise, plan item and reason ("Just skip" stores none). One per exercise, date and side; skipping again replaces the reason and keeps the record. A skip never becomes a set or a session, so no number, chart or best changes.
- Android database version 5, web database version 5: a new, empty skips table; nothing already stored changes.
- Deleting a plan item keeps its skips with the plan item cleared; editing one keeps it. On Android, deleting an exercise deletes its skips (the web has no exercise delete). Android's plan save now updates a row in place instead of replacing it, so an edit keeps its skips' plan item.
- **Backups are version 5** on both apps, with a `skips` list (`[]` when there are none). A version 4 (1.12) backup restores with no skips and clears any stored. **An older app (1.12) turns version 5 backups away** as too new.
- Fixtures: `android-v5.json` and `web-v5.json` are live, and each app restores the other's; the v4 files are frozen.

**Calls made while building:**
- ", n skipped" counts that day's skips on the session's side, a skip whose exercise was swapped away included.
- The skip card has the swap card's look: warning tone, a skip icon, tag "Skipped", title "<name> skipped", last in the feed.
- The four answers ignore taps while the sheet slides up and for 300 ms after; a tap outside cancels at once, on both apps; back and the keyboard are never held.
- The ⋮ is also on the start card's rows and the open card, not only on the folded rows.
- The "Skipped today" line shows at once, not only after the snackbar, and not on Finish's summary.
- With everything skipped, the start card says "<day> · n skipped" (1.13.1; "0 exercises" in 1.13) with no "nothing in your plan" text.
- The thresholds are "from the 2nd" and "from the 3rd", not "exactly", so a dismissed card can come back once its 28 days are over.
- A busy skip with no alternative on other equipment falls through to the 3-skips rule.
- Adding a skipped exercise again takes its skip back, as Bring back does.
- Today's targets are deleted on both sides by a pain skip or an unskip, since the hold crosses sides.
- Skipping again with another reason replaces the open card (no screen offers it yet).
- The web's Undo snackbar stays 4 s, its usual time; Android's is the short snackbar.
- Bring back's accessible name is "Bring back <name>" on both apps; its visible text stays "Bring back".
- Web: a hyphen is a break point in a name; below 260 px a folded row stacks its guide button and ⋮ to give the name room.
- Android: a folded row or the open card narrower than 260 dp over the font scale (320 dp with 130% text, or 360 dp with 130%) stacks its guide button and ⋮ the same way. Checked at 320 dp × 1.0, 360 dp × 1.3 and 320 dp × 1.3.
- Web: re-adding a skipped exercise from the picker ticks, as any picker add does; Bring back does not.
- The web's skip shots use Curl as the accessory (the web coach gives Row no alternatives with that plan); Android's use Barbell row.

**Existing tests changed** (each forced by the new advice type or backup version, or by the final review's pain rule; no assertion weakened):
- **Domain:** `CoachTest.prior()` and `CoachSnapshot.payload()` handle the new type and payload, so the files compile.
- **Domain, final review (a pain skip always tells you):** in `SkipCoachTest`, `openSwapCard_blocksSkipCard`, `openSkipCard_blocksAnother` and `cardInLast7Days_blocksAnother` probed with a pain skip, which those rails no longer block. They are now `openSwapCard_blocksBusyAndRepeatCards`, `openSkipCard_blocksBusyAndRepeatCards` and `cardInLast7Days_blocksBusyAndRepeatCards`, probing with busy and repeat skips. New pain variants pin the new rule: `openSwapCard_pain_stillGivesThePainCard`, `openNonPainSkipCard_pain_givesThePainCard`, `openPainSkipCard_pain_givesNoSecond`, `cardInLast7Days_pain_stillGivesThePainCard` and `dismissedPainCard_pain_stillGivesThePainCard`. `SkipFacadeTest.afterSkipGivesTheCardAsPlainData` (js) probed the stored busy card with a pain skip; it now probes with a third busy skip, and checks that a pain skip still gives its card and that an open pain card holds back a second.
- **Web, final review:** in `skips.test.ts`, `painSkip_deletesTodaysOpenTarget_…`'s "not a target" control card is now an open pain card. An open busy card for the same exercise and side is now deleted by the pain skip.
- **Android, final review:** `SkipTodayScreenTest.skip_bringBack_onTheSkippedLine` taps Bring back by its name, "Bring back Bench".
- **Android:**
  - `BackupTest`: `v5File_isTooNew` is now `v5File_onVersion4Decoder_isTooNew`; `v4RoundTrip_keepsCheckInsAndTheNewAdvice` expects `Backup.VERSION`.
  - `BackupFixtureTest`: the live fixture is `android-v5.json` (with `androidV4Fixture` for the frozen file); `androidFixture_holdsBothSides` expects 5; `webV4Fixture_restoresOnAndroid` compares with the file upgraded to 5; the helpers add `"skips": []`.
  - `ScreenshotTest`: new captures only.
- **Web:**
  - `checkIns.test.ts` `upgrade3to4_…` opens with the schema up to version 4.
  - `backup.test.ts`: the live fixture is `web-v5.json`; the round trip expects `BACKUP_VERSION`; `v4File_onVersion3Decoder_isTooNew_andVersion5IsTooNewHere` is now `v4File_onVersion3Decoder_isTooNew` (version 6 is checked in the new version 5 test); the helpers add `skips: []`.
  - `e2e/backup.spec.ts`: the exported version is 5.

**Screenshots:**
- **Android:** new `today-skip-menu`, `today-skip-sheet`, `today-skipped` and `coach-skip`, each also `-bodybuilding`, dark and light (16). Changed: `today-start`, `today-next` and `today-bodybuilding` (the ⋮, and names whole). Settings re-shot for 1.13.
- **Web:** new `today-skip-menu`, `today-skip-sheet`, `today-skipped`, `coach-skip` and `coach-skip-pain`, each also `-bodybuilding`, dark and light (20). Changed: `today` and `today-bodybuilding` (the ⋮, names whole), `rpe-sheet` (the ⋮ behind the sheet) and `showcase` (the skip icon). Settings re-shot for 1.13.

**Version:** Android 1.13 (versionCode 21), web 1.13.0; both show "Grit 1.13".

**Task 1 signatures** (domain; from the code as shipped):
- `Coaching.afterSkip(skip: Skip, skips: List<Skip>, history: TrainingHistory, plan: List<PlanItem>, today: LocalDate, priorAdvice: List<Advice>): List<Advice>`. It gives zero or one SKIP advice, with the skip's side. `skips` is every stored skip, `skip` included. `plan` is the whole plan; `priorAdvice` is every stored advice. On a pain skip the caller first deletes that side's OPEN busy or repeat SKIP card for the exercise; both apps' storage does this.
- `Coaching.targetsFor(mode, day, history, plan, settings, today, priorAdvice, skips: List<Skip> = emptyList()): List<Advice>`. Pass every skip from both sides, since the pain hold crosses sides. `Coach.targetsFor` and `BodybuildingCoach.targetsFor` take the same `skips`.
- `BodybuildingCoach.repsToBeat(item: PlanItem, history: TrainingHistory, setNumber: Int, today: LocalDate, skips: List<Skip> = emptyList()): Int`.
- `Coaching.applySkipSwap(payload: AdvicePayload.Skipped, choice: String, mode: TrainingMode, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange`.
- `SkipCoach.held(exerciseId: Long, skips: List<Skip>, history: TrainingHistory, today: LocalDate): Boolean`.
- `data class Skip(id: Long, date: LocalDate, mode: TrainingMode, exerciseId: Long, planItemId: Long?, reason: SkipReason?)`.
- `enum class SkipReason { BUSY, PAIN, TIME }`. "Just skip" is a null reason.
- `AdvicePayload.Skipped(skipId: Long, exerciseId: Long, name: String, reason: SkipReason?, alternatives: List<String>)`, with the advice type `SKIP`.
- **JS exports** (`GritDomain`, in `web/src/domain/generated`):
  - `JsSkip(id, date, mode, exerciseId, planItemId, reason)`;
  - `coachingAfterSkip(skip, skips, history, plan, today, prior)`;
  - `coachingTargetsForWithSkips(mode, weekday, history, plan, settings, today, prior, skips)`;
  - `coachingApplySkipSwap(payloadJson, choice, mode, plan, exercises)`;
  - `repsToBeatWithSkips(item, history, setNumber, today, skips)`;
  - `advicePayload(json)` decodes `kind: "Skipped"` with `exerciseId`, `name`, `alternatives`, `skipId` and `skipReason`.

**Open follow-ups:**
- Plan and Exercises tests that sometimes failed under load: looked into on 2026-11-18 (see "Tests that sometimes failed under load" under "The new look and motion"). Android `planAdd_tapAfterTheWindow_works` and the rest-alert test (its likely cause; unproven), and web `planAdd_cancel_storesNothing`, `addToPlan_choosesDay_andUsesDefaults` and `exercisesTab_browseByMuscleAndSearchWork`, are fixed in the tests. Open: Android `PlanScreenTest.planAdd_changeToOwnExerciseFails_afterThePickerWasClosed_theAddFormSaysSo` (cause not established; it now prints what it saw on a timeout).
- The web has no speed test for opening Today, which gained the ⋮ and the wrapping names; the speed checks cover only the picker and the Exercises tab.
- **Check-in may lose its place on a reload (1.14, unchecked):** the web's plan templates lost theirs after a reload on the draft until `a06f573`, which steps off the leftover history entry. The check-in screen has the same kind of step entry and probably the same case after a reload on its second step. Not checked or changed.
- **A stale coach card stays open (1.14):** after a plan is replaced (Choose a template, or Rebuild my plan), a coach card (swap or skip) for an exercise no longer in the plan says "Your plan has changed since; nothing was swapped" on Apply and stays open until dismissed. It could close itself, or hide, once its exercise has left the plan.

Not yet tried on a real phone.

## Done: 1.13.1, the 1.13 follow-ups (both apps)
Plan: `docs/superpowers/plans/2026-11-18-grit-1-13-1-follow-ups.md`.

- **Two guide lines (both apps, from the shared guide):**
  - Seated cable fly gains a third mistake: "Letting your hands go back past the level of your armpits." (ACE Standing Incline Cable Flyes, as the low-to-high cable fly already reads it.)
  - Hack squat's first step now reads "Lower by bending your hips and knees until your knees are just short of fully bent. Stop sooner if your hips start to lift off the pad." (ExRx Sled Hack Squat.)
  - Both checked on 2026-11-18 from the search summary. No allowed source says "behind the body" outright, so that wording is not used.
- **Today, when everything is skipped:** the start card reads "<WEEKDAY> · n SKIPPED", for example "WEDNESDAY · 2 SKIPPED". It used to read "WEDNESDAY · 0 EXERCISES".
  - n is the count on the "Skipped today" line.
  - It shows only when nothing is left on the list. With one exercise left it still reads "· 1 exercise". Undo and Bring back put the exercise count back.
  - With nothing planned it still reads "Nothing planned".
- **A quiet spell after a pain card is dismissed:** for 7 days after a pain card for an exercise on a side is dismissed, a new pain skip of it on that side gives no new card.
  - The 7 days count from when the card was made, not when it was dismissed. Advice stores no dismissal date, and adding one would change the database and the backups. So a card made 0 to 6 days ago holds a new one back, and one made 7 or more days ago does not. A card left open for a long time and then dismissed starts a short quiet spell, or none.
  - The weight is still held during the quiet spell, since the hold comes from the skip, not the card.
  - An applied pain card starts no quiet spell. Main lifts' Dismiss-only cards count.
  - Busy and repeat cards keep their rules. A dismissed pain card still holds them back for 28 days.
- **Gentler alternatives on the pain card (machine or cable before free weights): researched, not built.** No allowed source gives a rule for ordering alternatives when an exercise hurts.
  - The nearest is NSCA's older-adults position statement: "consider weight machines (as opposed to free weights)" for joint pain or limited range. It is not enough: it covers older adults only, its reason is that machines limit the range, it says nothing about cables, Smith, bands or bodyweight, and its full wording was seen only as a search summary.
  - The other results were about machines' stability in general, one lift (the Smith squat), or a survey that showed association only.
  - The alternatives stay in their current order.
- **Existing tests changed:**
  - Android `SkipTodayScreenTest.skipEveryPlannedExercise_noRestDayView_skippedLineShown` and the web test of the same name in `SkipToday.test.tsx`: "0 exercises" became "2 skipped", the text this update replaces.
  - Domain `SkipCoachTest.openPainSkipCard_pain_givesNoSecond`: its "not once it was answered" probe used a pain card dismissed 2 days ago, which the quiet spell now holds back; it uses an applied card.
  - Domain `SkipCoachTest.cardInLast7Days_pain_stillGivesThePainCard`: it covers applied cards only; dismissed ones are covered by the new `quietSpell_*` tests.
- **Version:** Android 1.13.1 (versionCode 22), web 1.13.1. Settings shots re-shot on both apps; no other screenshot changed.

Not yet tried on a real phone.

## Done: plan templates (1.14, both apps)
Spec: `docs/superpowers/specs/2026-11-18-grit-plan-templates-design.md`; plan: `docs/superpowers/plans/2026-11-18-grit-plan-templates.md`. The owner asked (2026-11-18): "We should have more built templates for powerlifters and for bodybuilders that they can choose from to get started."

**What you see:**
- **An empty plan:** on powerlifting, "Your plan is empty. Start from a template or add exercises to each day." over **Choose a template**. On bodybuilding, **Choose a template** is the main (full-width) button, with **Build my plan** under it as a secondary button. Adding exercises by hand still works.
- **The Plan menu (⋮), on both sides while the side has a plan:** **Choose a template**, after **Rebuild my plan** on bodybuilding. Nothing is called empty, and no menu is drawn, before the stored plan is read.
- **The list:** the side's three templates, fewest days first. Each card shows its name, "n days a week · <who it suits>" and its days' titles joined with " · ".
- **The draft,** after tapping one:
  - the template's name as the title, with "Choose a template" over it;
  - "Which days?" with the seven weekday chips, ticked to start on the side's own training days when they are as many, else a fixed spread (3: Mon, Wed, Fri; 4: Mon, Tue, Thu, Fri; 5: Mon, Tue, Wed, Fri, Sat). The line under them reads "Mon, Wed, Fri", or "Choose n days." while the count is off;
  - one card per day, "<Weekday> · <title>" ("Day n · <title>" while the count is off), each exercise with sets × reps @ RPE (fixed reps on powerlifting, "3 × 5 @ 8"; a range on bodybuilding, "3 × 8–12 @ 8");
  - **Swap** on every exercise except the main lifts (squat, bench, deadlift and strict press). A variation swaps only to another variation of the same lift; anything else uses the builder's similar-exercise list. Nothing already in the week is offered, so a lift on two days keeps the same numbers on both;
  - the weekly sets per body part on bodybuilding, as the builder's draft shows;
  - **Back** (to the list) and **Use this plan**, which works only with exactly the template's number of days ticked. Changing the days never rebuilds the draft: swaps are kept, and the days move onto the chosen weekdays in week order.
- **Strict press:** the powerlifting templates include it only when Settings' strict press is on, when the screen opens. A saved plan is a snapshot.
- **Saving:** on a side with a plan, "Replace your powerlifting plan?" (or bodybuilding) and "Your history and bests are kept.", with **Cancel** and **Replace**. Cancel stores nothing. A save replaces that side's plan and sets that side's training days to the chosen days, both or neither: if it fails, "The plan could not be saved. Nothing was changed." shows at the top of the draft and Use this plan can be tried again. Then back to Plan with "Plan set: <name>."
- **Back:** on both apps, back from the draft returns to the list; from the list it leaves.

**The six templates** (Acc = 3 × 10 @ 8; [SP] = only with strict press on; every bodybuilding entry 8–12 @ 8, sets as shown):
- **3-day powerlifting** ("New to powerlifting: 2–3 days a week"), main lifts 3 × 5 @ 8:
  - Squat & bench: Squat, Bench, Seated cable row (Acc), Cable crunch (Acc);
  - Deadlift & bench: Deadlift, Bench, [SP] Strict press, Lat pulldown (Acc), Triceps pushdown (Acc);
  - Squat & deadlift: Squat, Deadlift, [SP] Strict press, Lying leg curl (Acc), Face pull (Acc).
- **4-day powerlifting** ("Some training behind you: 3–4 days a week"), main lifts 4 × 4 @ 8, variations 3 × 5 @ 8:
  - Squat & bench: Squat, Bench, Seated cable row, Cable crunch;
  - Deadlift: Deadlift, [SP] Strict press, Close-grip bench, Lat pulldown, Lying leg curl;
  - Squat & bench: Squat, Bench, One-arm DB row, Face pull;
  - Deadlift: Deadlift, [SP] Strict press, Pause squat, Back extension, Triceps pushdown.
- **5-day powerlifting** ("Experienced, a year or more of training: 4–6 days a week"), main lifts 5 × 3 @ 8, variations 3 × 5 @ 8:
  - Squat & bench: Squat, Bench, Seated cable row, Cable crunch;
  - Deadlift: Deadlift, [SP] Strict press, Close-grip bench, Lat pulldown, Lying leg curl;
  - Variations: Pause squat, Spoto press, Chest-supported row, Face pull, Hammer curl;
  - Squat & bench: Squat, Bench, One-arm DB row, Triceps pushdown;
  - Deadlift: Deadlift, [SP] Strict press, Long-pause bench, Back extension, Cable crunch.
- **3-day full body** ("New to lifting: 2–3 days a week, the whole body each day"):
  - Full body A: Leg press 3, DB press 4, Lat pulldown 3, Seated leg curl 3, Lateral raise 3, DB curl 3, Rope pushdown 3;
  - Full body B: DB RDL 3, Incline DB press 3, Seated cable row 4, Leg extension 3, Face pull 3, Standing calf raise 3, Cable crunch 3;
  - Full body C: Hack squat 4, Machine chest press 3, Chin-up 3, Lying leg curl 4, Face pull 3, Triceps pushdown 3, Cable curl 3.
- **4-day upper/lower** ("Some training behind you: 3–4 days a week"):
  - Upper A: DB press 4, Seated cable row 4, DB shoulder press 3, Lat pulldown 3, Lateral raise 3, Face pull 3, DB curl 3, Rope pushdown 3;
  - Lower A: Leg press 4, RDL 3, Leg extension 3, Seated leg curl 3, Standing calf raise 3, Cable crunch 3;
  - Upper B: Incline DB press 4, Machine row 3, Chin-up 4, Cable crossover 3, Lateral raise 3, Face pull 3, Cable curl 3, Triceps pushdown 3;
  - Lower B: Hack squat 4, Hip thrust 3, Lying leg curl 4, Bulgarian split squat 3, Standing calf raise 3, Cable crunch 3.
- **5-day body-part split** ("Experienced, a year or more of training: 4–6 days a week"):
  - Chest & back A: Machine chest press, Wide-grip cable row, Lat pulldown, Cable crossover (3 each);
  - Legs A: Leg press 4, RDL 3, Leg extension 3, Seated leg curl 3, Standing calf raise 3, Cable crunch 3;
  - Shoulders & arms: Lateral raise 4, then Cable lateral raise, DB curl, Rope pushdown, Cable curl, Triceps pushdown (3 each);
  - Chest & back B: Incline DB press, T-bar row, Neutral-grip pull-up, Pec deck (3 each);
  - Legs B: Hack squat 4, Hip thrust 3, Lying leg curl 4, Bulgarian split squat 3, Cable crunch 3.
- **Checked by the domain tests:** every bodybuilding big group (chest, lats, quads, hamstrings, glutes, upper back, biceps, triceps) gets 10 or more counted sets a week on 2 or more days, the builder's way of counting, and no group goes over the coach's ceiling of 20 (highest: 12 on bb-3, 19 upper back on bb-4, 17 glutes on bb-5). Squat, bench and deadlift get 6, 8 and 10 working sets a week, on 2 sessions each, with strict press off or on.

**The sourced numbers** (all in `CoachSources.kt`, checked on 2026-11-18 from the search summary):
- **Days a week by experience** (`TEMPLATE_DAYS`): NSCA "Determination of Resistance Training Frequency" (beginner 2–3, intermediate 3–4, advanced 4–6, advanced being a year or more), and the ACSM 2009 progression models as ExRx's "Weight Training Guidelines" summarise them (2–3, 3–4, 4–5). The templates take 3, 4 and 5, inside every band.
- **Main-lift loading** (`TEMPLATE_STRENGTH_LOAD`, `TEMPLATE_STRENGTH_PHASE`, `TEMPLATE_NSCA_TABLE`): ExRx's "Weight Training Guidelines", summarising ACSM 2026 and 2009 (strength at 80% 1RM or more, 2–3 sets an exercise, 2 or more sessions a week); NSCA "Preparatory Period" (basic strength 80–95% 1RM, 2–6 sets of 2–6 reps); the NSCA Essentials table via ACE (maximum strength 6 reps or fewer at 85% or more).
- **The floor** (`TEMPLATE_STRENGTH_FLOOR`): PMC8435792 on powerlifters' minimum effective dose, 3–6 working sets a week of 1–5 reps above 80% 1RM at RPE 7.5–9.5 over 1–3 sessions a lift. Every template is at or above it.
- **Reps @ RPE to %1RM** (`TEMPLATE_LOAD_CHART`): ExRx "Predicting One-rep Max" (5 = 87%, 6 = 85%), the NSCA Training Load Chart, PMC10933212 and PMC7706636 (RPE as reps in reserve). So 4 × 4 @ 8 is about 85% and 5 × 3 @ 8 about 87%.
- **The accessory range** (`TEMPLATE_ACCESSORY_RANGE`): ExRx "Sample Powerlifting Program", 3 × 8–10; with the NSCA's 6–12 for hypertrophy.
- **Splits for experienced lifters** (`TEMPLATE_SPLIT_ADVANCED`, `TEMPLATE_SPLIT_BODYBUILDERS`): ACSM's simplified 2009 summary (advanced 4–6 days, 1–3 muscle groups a session) and PMC7698840 (bodybuilders: a 5-day split, major groups twice a week).
- Bodybuilding's 3 × 8–12 @ 8 is the owner's default, as in the builder.

**The designer's choices, for the owner:**
- The template names, ids and day titles; which exercises are chosen and which day each goes on; the fixed weekday spreads.
- The reps and sets picked inside the sourced ranges: 3 × 5, 4 × 4 and 5 × 3 for the main lifts, 3 × 5 for every variation, RPE 8 on all of them, and every "4" set on bodybuilding.
- **2 sessions a week for every lift in every template:** the sources allow 1–3; the templates differ by sets and reps instead.
- **Accessories at 3 × 10 @ 8:** no allowed source gives a fixed accessory rep count for powerlifting. 10 is the owner's pick inside the sourced ranges (ExRx's 8–10, the NSCA's 6–12); it is marked as such in `CoachSources.kt`.
- **The Cable crunch on both Legs days of the 5-day split** (the owner's addition): it gives abs 6 sets over 2 days, still short, and makes those days 4 muscle groups. So the "who it suits" line keeps only "4–6 days a week" and drops ACSM's "1–3 muscle groups a session".
- "Not in the catalogue." as the left-out reason, if a template exercise were ever missing.

**The owner's decisions:**
- Templates differ by days a week (A).
- A draft the lifter can adjust before saving: weekdays and Swap (A).
- Approach 1: fixed templates in the shared domain, shown in the builder's draft screen.
- Accessories (A): 3 × 10 @ RPE 8. The designer proposed 10, inside the sourced ranges, and the owner chose it; it is marked as such in `CoachSources.kt`.
- **Choose a template** is the main button on the empty bodybuilding plan (owner, 2026-11-18: "keep choose a template as the main button").

**A replace with old state:**
- History, bests, check-ins, coach cards and the other side's plan and days are untouched. Skips keep their records, their plan item cleared, as when a plan item is deleted. A session in progress keeps its logged sets.
- **A coach card about the old plan** acts on the plan as it is now, as after Rebuild my plan. If its exercise is not in the new plan, Apply says "Your plan has changed since; nothing was swapped" and the card stays open until it is dismissed. If the exercise is in the new plan, the swap lands on the new items.

**The 1.14 review fixes (also in 1.14):**
- **The bodybuilding builder's save is now all or nothing on Android,** as the templates': the plan and the days in one transaction, the days last and put back on a failure. So its "Nothing was changed" is true. The web builder already saved in one transaction.
- **The builder's bar:** at 320 dp with 130% text, Android cut "Use this plan" to "Use this" and "Choose for me" too. Both apps now keep the bar half and half while the main label fits half; otherwise Back takes its own width. On the web, at 320 px with 130% zoom, "Choose for me" ran past the screen and "Use this plan" overlapped Back; there the bar's faces also get a little less side padding. Nothing changes at normal widths, and no builder shot changed.
- **The weekday chips on Android go 4 + 3 at 360 dp with default text** (seven to a row had cut "Mon" to "Mo"), in the builder too. The screenshots are drawn at 412 dp, where the chips stay in one row, so no builder or template shot changed.

**Existing tests changed:**
- **Android:** `PlanBuilderViewModelTest.whenSettingTheDaysFails_theBuilderStaysOpenWithTheError_andTryingAgainSaves` asserted that the plan had been replaced before the failing days write (the old Ruling P7 order). It now asserts that nothing was stored, which is the fix.
- **Domain:** `PlanTemplatesTest`'s swap sweep (new in 1.14) is now `noOfferedSwapIsAMainLiftOrAlreadyInTheWeekOrBreaksTheSameNumbersRule`, also checking that no offered alternative is already in the week.
- No other existing test was edited; `thePowerliftingPlan_hasNoBuilder` (web) and the empty powerlifting plan's no-menu test (Android) pass unedited.

**Screenshots:**
- **Android:** new `templates-list`, `template-draft` (pl-4), `template-draft-bodybuilding` (bb-4) and `template-confirm`, dark and light. Changed: `plan` (the ⋮ in the header) and `plan-add-form` (drawn over it). Settings re-shot for 1.14.
- **Web:** the same four new shots, dark and light. Changed: `plan`, `plan-add-form` (over the new empty-plan line and button), `plan-add-form-bodybuilding` (over the new button order) and `showcase` (the new icon). Settings re-shot for 1.14.
- **Left as they are:** the web's `form-guide-*` and `today-rest-*` already differed before 1.14, and `exercise-guide-actions-*` varies between runs in a patch of a figure picture (both dark and light in the 1.14 run). None was reshot.

**Version:** Android 1.14 (versionCode 23), web 1.14.0; both show "Grit 1.14".

**Open follow-ups:** see the two 1.14 lines under the 1.13 open follow-ups (check-in on a reload; the stale coach card).

Not yet tried on a real phone.

## Done: 1.14.1, plan safety nets (both apps)
Plan: `docs/superpowers/plans/2026-11-19-grit-1-14-1-plan-safety-nets.md`.

**The owner's report:** on the web app, installed from Chrome on Android, the bodybuilding plan was lost around the 1.14 update, after tapping "Update ready". History and settings were kept; only the plan was gone.

**What was checked:**
- 9 update paths, from 1.12.3 to 1.14 through "Update ready", were reproduced. None lost the plan.
- No code cause was found. The only code that removes a whole plan is something the lifter does: Build or Rebuild my plan, a template's Use this plan → Replace, or restoring a backup.
- The cause isn't established. The owner approved two safety nets on 2026-11-19 ("yes, build both").

**Undo after a whole-plan replace (both apps):**
- After a template's Use this plan, or the builder's Use this plan, over a side that had a plan, Plan's snackbar ("Plan set: <name>." or "Plan saved.") carries **Undo**.
- Undo puts back that side's old plan items with their ids, days, order and numbers, the skips' links to them, and the side's training days, in one transaction. If it fails, "Couldn't undo. Your new plan is kept." shows and nothing changes.
- No Undo on a side that had no plan. Undo never touches the other side, or anything made after the save (sessions, sets, advice, check-ins, later skips).
- Only the latest replace can be undone. An older snackbar's Undo, or Undo after Undo, does nothing.
- Leaving Plan drops the snackbar, and its Undo with it. An Undo already tapped still finishes.
- A save that fails and rolls back leaves the earlier save's Undo working (Android review fix; on the web a rolled-back save never changes the ids the Undo checks).
- **The builder's new "Plan saved.":** the builder had no saved message before; it now shows "Plan saved." after every save, with Undo only over a plan.

**The restore warning (both apps):**
- Before a backup restore, each side is compared. If a side has plan items now and the backup has none for it, the confirm is "This backup has no powerlifting plan" (or bodybuilding, or "no powerlifting or bodybuilding plan" for both), in place of "Replace all data?".
- The body: "Restoring it removes your current bodybuilding plan. Your other data is replaced by the backup's too." With both sides: "Restoring it removes your current powerlifting and bodybuilding plans. Your other data is replaced by the backup's too."
- **Restore anyway** restores; **Cancel** restores nothing. With no side losing its plan, the flow is unchanged. A backup item with no side counts as powerlifting, as on the way in.
- The backup format, the coach rules and the existing strings are unchanged.

**Where the apps differ (accepted, all Low in the review):**
- **The stale rule:** Android uses a token (the side's latest snapshot, kept in memory, so lost with the process) plus a check that the side's item ids are still the replace's. The web uses the id check only.
- **The Undo time:** Android's is `SnackbarDuration.Short`, 4 s, which the phone's accessibility settings can lengthen. The web's is a fixed 4 s.
- **The warning on safety-copy restores:** only the web has a safety-copy restore ("Restore safety copy"), so only the web warns there.
- **A failed Undo after leaving Plan:** Android shows the message the next time Plan opens; the web shows nothing.
- **Editing the plan right after a save:** the review listed this as a difference (on Android any change turns Undo off; on the web, adding or removing an item does). In the code, both apps' Undo checks the same thing besides Android's token: the side's item ids must still be the replace's. So on both, adding or removing an item turns Undo off, and an in-place number edit within the 4 s is reverted by Undo.
- On the web, Undo stores the side's days as an explicit settings row, even when they were the default; the value is the same.

**Tests:**
- **New, Android:** `PlanUndoTest` (ids, order, numbers, skip links and days back, the other side untouched; no Undo on an empty side; a stale Undo; never the other side; a logged session kept; a failed Undo; a rolled-back save keeps the earlier Undo; the builder's Undo) and `BackupRestoreWarningTest` (sides per side, both and none; the strings; Cancel; Restore anyway; no warning).
- **New, web:** `src/db/restorePlan.test.ts`, `src/screens/plan/planUndo.test.tsx` (Undo gone after 4 s) and `src/screens/settings/restoreWarning.test.tsx`; e2e cases in `templates.spec.ts` (template over a plan, Undo, reload, the old plan and days back) and `bodybuilding.spec.ts` (Rebuild my plan, "Plan saved." with Undo, reload, the old plan back).
- **Existing test changed:** web `TemplatesScreen.test.tsx`, `onSaved` was expected with `('3-day powerlifting')` and is now expected with `('3-day powerlifting', objectContaining({ mode: 'POWERLIFTING' }))`, since a replace now also hands over its Undo. No other existing test was edited.

**Version:** Android 1.14.1 (versionCode 24), web 1.14.1. Settings shots re-shot on both apps. The mode-switch animations move to 1.15 (versionCode 25).

**Open follow-up:**
- **The cause of the plan loss isn't established.** The safety nets make a replace or a restore recoverable or warned, but don't explain the report. The owner's backup file is pending; look at it when it comes (does it have the bodybuilding plan, and from what date).

Not yet tried on a real phone.

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
