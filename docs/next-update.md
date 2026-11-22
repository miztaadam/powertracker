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
- Tests that sometimes failed under load (looked into on 2026-11-18, test-only changes in 0721be6 and 4df3af3; no app cause found). Fixed in the tests: Android `PlanScreenTest.planAdd_tapAfterTheWindow_works` waited for the row in the database and then checked once that the add form had closed, which happens a moment later, when the write's answer reaches the screen (proven by holding that answer 300 ms); it now waits for both, as do `planAdd_changeNumbers_addStoresThem` and the rest-alert and vibrate-on-taps settings tests (the rest-alert one by the same shape, unproven: its failing line was never recorded, and it now prints the stored and shown values if it times out). Web `planAdd_cancel_storesNothing` polled 1 s for "Create your own", which is drawn only after the rest of the 225-row list; `createYourOwn` now waits for the list's frames, then finds it once. The web 5 s timeouts were plain work at about 80% of the limit: the Exercises tab tests are now laid out as on a phone (thumbnails near the screen only), and the browse and picker-search tests are split into parts under their own names. Verified: 3 quiet Android and 3 quiet web runs green, and one Gradle run with `npm test` back to back alongside, all green (web had failed in 5 of 5 loaded runs before). Then open, since found (see the open follow-ups, 2026-11-19): `planAdd_changeToOwnExerciseFails_afterThePickerWasClosed_theAddFormSaysSo` did not fail in 10 full runs here, so its cause was not established; its wait now prints where the message went, the test and wall clocks and the database work in flight when it times out. **Caught once on 2026-11-18,** in the full Android run on the merged 1.14 main (the same test also failed once in a full run during the 1.14 Android work; 2 of that branch's 6 full runs). What the recorder showed: at the first check the message was somewhere on screen, but the test's condition, the message shown over the add form, was not met. By 4.2 s on the test clock (2.7 s wall) the message was gone. The add form was still open, Tyre flip was not stored, and no database work was in flight. It passed 5 times alone and in the next full run. Not yet established: which copy of the message was showing. On Android the screen's own snackbar also gets it, behind the form (see the 1.12.1 notes), so the popup over the form may never have appeared. 1.14 changed Plan's first frame (`PlanUiState(loaded = false)`) and its header, which may have made it likelier. Next: the forced-order experiment, first in the next update. The capture is kept outside the repo. Next at risk on the web: the figure plugin test (about 2.7 s quiet, 4.9 s once under load).
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
- **Behind the form:** on Change, the message shows on Plan's own screen, under the open form's scrim, as the web's does under its form. **Changed on 2026-11-19:** on Android, Plan's snackbar now says a failed own exercise only if no window over Plan put it on screen; the window on top says it (see the open follow-ups, 2026-11-19). Today is as above.
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
  - On Android the screen's own snackbar also gets the message, behind the picker; on the web only the form says it. **Changed on 2026-11-19, for Plan:** Android's Plan snackbar now gets only a failure that no window of its own ever put on screen (its picker or add form closed first, or the failure came after), as on the web, so the message is no longer shown twice. Today's snackbar still gets every one, as the 1.12.1 notes say.
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
- Plan and Exercises tests that sometimes failed under load: looked into on 2026-11-18 (see "Tests that sometimes failed under load" under "The new look and motion"). Android `planAdd_tapAfterTheWindow_works` and the rest-alert test (its likely cause; unproven), and web `planAdd_cancel_storesNothing`, `addToPlan_choosesDay_andUsesDefaults` and `exercisesTab_browseByMuscleAndSearchWork`, are fixed in the tests. Android `PlanScreenTest.planAdd_changeToOwnExerciseFails_afterThePickerWasClosed_theAddFormSaysSo`: **cause found on 2026-11-19, in the app, fixed in `da05bd5`.** The add form decided whether to say a failed own exercise from Change at the moment the failure came back, from whether the picker was open then. If the failure came back with the picker still open and the picker was closed before the next frame, the picker had counted it but was never drawn again to say it, and the form had not counted it: only Plan's own snackbar said it, under the form's scrim. The test hit this because on a busy machine the insert can answer before the Create tap has returned (`withContext` does not suspend when its block is already done), and the test's Cancel and Close came in the same frame; a lifter hits it by tapping Close in the frame the failure lands. The form now decides once the failure has been through a frame, as the picker does. **Then (Android, 2026-11-19):** Close one or two frames after the failure lands, before the picker's popup had appeared, still left only Plan's snackbar (forced: 1 and 2 frames failed). Proof: `TestDatabase` can hold its work or run it at once; `..._inTheFrameThePickerCloses_theAddFormSaysSo` and `..._answeredWithinTheCreateTap_thenThePickerClosed_theAddFormSaysSo` failed every time before the fix, with the captures' signature, and pass after it. After the fix: the original test 20 of 20 alone, `PlanScreenTest` 4 of 4 under CPU load (it had failed 2 of 11 class runs before), full `:app:testDebugUnitTest` 3 of 3 (810 tests). Why 1.14 made it likelier was not established. **Web:** forced in one `act` (the rejection lands, then Close), the message was said nowhere: the picker counted it as its own and closed before drawing it. Fixed in `48fbd29`: a failure the picker has counted but not drawn goes to whoever opened it when it closes (`planAdd_ownExerciseFails_inTheMomentThePickerCloses_planSaysSo`; web suite 1320 green). **Then (web):** after Change's picker had closed, the web said it only in Plan's own snackbar, outside the add form's dialog and under its scrim (z-index 10 against 20). **Both closed on 2026-11-19:** a failed own exercise from Change is now held by the add form until a window has said it through (its snackbar has run its 4 s), not an event checked against which window is open at one moment. The window on top says it: Change's create form, else its picker, else the add form. One that closes, or that another comes over, before its message has run its course leaves it held. So Close in the frame the failure lands, 1 or 2 frames later, or while the picker is saying it, all end with it said once over the add form. Android: `HeldFailures` and a `FormMessage` for it; the add form's failed Adds use it too, so it has one message, not two. The web, by the owner's call for parity, says it inside the add form's dialog (`useHeldFailures`, `HeldMessage`). While the add form is open, only the form says it. If the add form closes before it has put the message on screen, Plan's snackbar says it; one already shown, even cut short, is not handed on to Plan. Plan's Add and Today's Add exercise use the same holding inside their picker, one per opening, and their screens' snackbars are as before (web guard: `planAdd_ownExerciseFails_saidByTheCreateForm_thenClosed_planDoesNotRepeatIt`). **Visible changes:** inside the add form's windows, the message follows the window on top until it has run 4 s unbroken. So a picker closed mid-message says it again over the add form, where before the message went with the picker. Cancel on the create form mid-message has the picker's list say it again, and opening the create form again mid-message has the form say it again (both apps, Today and Plan's Add too). On Android a failed Add's message, then Change, is said again over the picker. Proof: forced orders on both apps (same frame, 1, 2 and 60 frames on Android; the same in separate commits on the web), each checked frame by frame for 6 s: one copy at a time, over the add form only, one snackbar in all. Two guards: the picker said it through, then Close leaves the form silent; on the web, the add form closed first leads to Plan saying it once. Before the change, the 1, 2 and 60 frame orders failed on Android, and all four failed on the web. **Then (review follow-up, 2026-11-19):** a create or pick that works clears a failure still held, so the add form no longer says "Couldn't save" over the exercise just changed when the picker closes (both apps; on Android this also clears a failed Add the form was still saying). On Android, Plan's own snackbar no longer gets every failed own exercise: it says one only when no window of its own put it on screen, as on the web, so it is no longer shown twice under the scrim. The add form's held failure is kept across a rotation. Proof: the Android checks now count Plan's snackbar too, and saw two copies in all five Change tests before the change. New tests on both apps, for a second Create that works and for a row picked after Cancel, failed before. Still shown twice on Android: a failed Add (Plan's snackbar under the scrim, and the form).
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

## Done: 1.15, mode switch animations (both apps)
Spec: `docs/superpowers/specs/2026-11-18-grit-mode-switch-animations-design.md`; plan: `docs/superpowers/plans/2026-11-19-grit-mode-switch-animations.md`. The reference: `docs/design/motion/mode-switch.html` and, with the sounds, `docs/design/motion/mode-switch-sound.html`.

**What the lifter gets:** flipping Bodybuilding mode in Settings plays an animation over the whole app, with its own sound and vibration.
- **Into powerlifting: the hammer drop** (about 1.1 s). The hammer swings in and lands on the bottom edge of the screen; the flash, starburst, shock rings, cracks, chalk and sparks follow, with a hard decaying shake. The powerlifting look bursts out from the hit at the impact (250 ms), and a POWERLIFTING stamp shows just below the title, then fades.
- **Into bodybuilding: the pump** (about 1.3 s). Three beats at 90, 400 and 750 ms, each bigger; veins of light run from the switch; the blue floods out in a circle from the switch on the third beat.
- The setting is stored at the tap, as before. A tap anywhere outside the switch's row skips to the end. Flipping again mid-animation ends the running one and plays the new one.
- A backup restore, another tab, or anything else that changes the stored side changes the look with no animation.

**The owner's decisions:**
- The hammer drop for powerlifting, tuned: smaller, hitting harder, and landing on the very bottom of the app.
- The pump for bodybuilding.
- Vibration is always forced, whatever "Vibrate on taps" says.
- Reduce motion: no animation, but the vibration and the sound still play.
- The sounds are approved (2026-11-19: "Animations and sounds are perfect").

**My calls** (the spec's "Settled after the builder check"):
1. **Light scheme:** the screens use the app's light tokens. The effects keep the reference's colours, except that in light the white flash becomes a dark punch (`#2A2E33` at half the flash opacity), and the cracks, chalk and veins get darker edges so they stay visible. The fix round added ring 1, the sparks and the starburst's edge to the light set (`ModeSwitchMotion.LightColors`, one set for both apps). The owner sees the light shots before the merge.
2. **The switch is the first row of Settings, which scrolls:** the pump's vein fan aims from the switch towards the screen's centre, scaled to reach the edges; the flood grows from the switch until it covers the screen.
3. **The POWERLIFTING stamp:** the powerlifting side has no label, so a stamp in the label's style shows at the impact and fades by the end. No permanent label was added. (Since the fix round it sits just below the title, taking no room, on both apps.)
4. **Only the lifter's own flip in this app starts an animation.**
5. **Colours:** the app's tokens for the screens, the reference's numbers for the effects only.
6. **The hammer lands on the bottom edge of the screen.** Android's Settings has no tab bar, so it lands on the bare edge; on the web it lands over the tab bar.
7. **The content changes too:** the new side's real screen opens inside the circle. On the web through a view transition, on Android inside a clip over a picture of the old side.
8. **Tests:** the web e2e turns motion off on every page, so the animation tests turn it on.
9. **The reference** is committed at `docs/design/motion/mode-switch.html`.
10. **Version** 1.15, versionCode 25.

**Where the apps differ (accepted):**
- **Sound sync:** the web plays from the clock's offset, joins late and re-syncs drift. Android's `SoundPool` can't seek: it starts at the tap, drops a sound more than 150 ms late, and never re-syncs.
- **Format:** Android ships Ogg. The web plays Ogg, or the WAV copy where the browser can't play Ogg.
- **When the sound is decoded:** Android decodes when Settings opens. The web fetches the bytes when Settings opens and decodes at the tap.
- **Silent mode:** Android plays no sound in silent or vibrate mode. The web can't read the ringer: an iPhone's silent switch mutes Web Audio, and Android browsers follow the media volume.
- **Vibration:** Android uses amplitudes where the phone has amplitude control, and `USAGE_MEDIA` on Android 13 and later. The web sends `navigator.vibrate` timings only; iPhones get none.
- **The old side during the reveal:** Android records it 60 ms before the moment, so its swells freeze from then. The web's view transition captures it at the moment.
- **The circle:** on Android it moves with the shake and the swell. In the web's view transition it is fixed to the screen.
- **Without view transitions (web only):** the old side's colour shows outside the circle, not its content.
- **If a picture of the reveal cannot be animated (web only):** the old side stays still, or the new side shows at once (below).
- **Shake and swell:** the web applies the reference's `deviceShakeY` and `deviceScale` to the shell and the overlay; Android doesn't use them.
- **The switch at a pump's tap:** on the web it turns blue at the tap, as in the reference. On Android it doesn't.
- **A tap on the switch during the reveal:** the web forwards the click from `<html>`; Android lets it through its row.
- **The clock:** Android catches the picture up to the time since the tap once, then adds each frame's step. The web works out the time since the tap on every frame.
- **Going away:** the web listens for `visibilitychange`. Android ends on Settings' `ON_PAUSE` (leave) and the host's `ON_STOP` (hidden).
- **Speed checks:** Android times the draw under Robolectric. The web times real frames on the production build.

**The web speed work** (`.superpowers/sdd/2026-11-16-grit-look/115-speed-fix-report.md` and `115-gate-hardening-report.md`):
- **No `<html>` write per frame:** the old side moves on its own picture, a paused animation on `::view-transition-old(root)`, instead of a CSS variable on `<html>` that restyled the whole document every frame.
- **A shorter moment:** the view transition's update no longer lays out the new side (the blocks are measured and the stamp placed in the next frame), and the shell is not redrawn when the side shown changes. The update went from a median 8.8 ms to 2.2 ms.
- **Reveal work per frame** went from about 7 ms to about 4 ms (the gate's pooled median: the old build 6.5–8.2; this one 3.9–5.0 before the release run, and 4.4–5.2 in it).
- **Deterministic pins,** on the held clock:
  - no `<html>` writes mid-reveal (at most 16 over a whole switch);
  - no `getBoundingClientRect` in the update;
  - the circle grows frame by frame to the whole screen;
  - the `endReveal` fallback: where the circle can't be animated, the reveal ends at once on the new side.
- **The `pseudoElement` fallback:** an animation on a picture that the browser applies to `<html>` itself instead is cancelled at once; the old side then stays still, or, for the circle, the new side shows at once.
- **The circle's and the old side's animations are cancelled** at the reveal's end, and when a reveal is cut short, so nothing is left behind.
- **The gate** (`web/e2e/modeSwitch.spec.ts`, `@speed`): the app's own script per frame, the frame gaps, the tap, against an idle control and a CPU check that names a busy machine instead of the app, with per-kind reveal-work limits (pump 6.0 ms, drop 5.5 ms, pooled 5.5 ms).

**Tests:**
- **New, domain:** `ModeSwitchMotionTest` (the timeline, the shapes, the colours, the light set, the vein fan) and `FixedFormatTest` (the shapes' number format, byte for byte on the JVM and in Kotlin/JS, across its Int and Long paths). `ModeSwitchBridgeTest` for the web exports.
- **New, Android:** `ModeSwitchOverlayTest`, `ModeSwitchFeedbackTest`, `ModeSwitchShellTest`; in `PlanScreenTest`, a failed Add then Cancel with no frame between, held by Plan and said once (it fails on `PlanScreen.kt` from before `0bb5edc`).
- **New, web:** `ModeSwitchOverlay.test.tsx`, `modeSwitchFeedback.test.ts`, `e2e/modeSwitch.spec.ts` (the behaviour, the pins, the fallbacks, the offline sounds and the speed gate).
- **Tightened, `ModeSwitchMotionTest`:** the hammer's 22 shapes and the rune are pinned in full (before, the count and two shapes); the five gradients and every effect colour are pinned, copied from the reference page (before, only the gradients' ids); the light set is pinned; the stamp is exactly .5 at 880 ms; the vein fan's centre must be within 10° of its aim from 24 switch places on 4 screens (before, 20° from one), every vein within 85° of the aim, and the fan wider than 100°.
- **Existing tests changed:** Android `PlanViewModelTest`'s add-failure test is split (with the form's own `onFailed`, Plan's messages stay empty), and `PlanScreenTest`'s forced-order Close checks now idle the main looper after Close (`6247996`). The new 1.15 tests were also reworked in the fix round (the skip now cancels and ticks 40 ms on both apps; the shots check the reveal and the light colours; `framesKeepTime` includes the reveal). The held-failure work that landed alongside (Android `PlanScreenTest` and `TestDatabase`, web `PlanScreen.test.tsx`) is described in the held-failure note above, under 1.13.

**The Android vibration note:** the switch's vibration uses `USAGE_MEDIA` on Android 13 and later, so "Vibrate on taps" and touch feedback off don't mute it. It can still be muted by the system's Media vibration switch, the master vibration switch and battery saver. None of this is phone-tested.

**Version:** Android 1.15 (versionCode 25), web 1.15. Settings shots re-shot on both apps.

**Owner decisions (2026-11-19, "approved"):**
- **The light shots are approved.**
- **The lost-frame rule is report-only.** Two or more animations of one kind with late frames now give a loud LATE FRAMES warning, and the run passes. The work limits and the held-clock pins stay hard. Before this, the rule failed 2 of 13 quiet runs on `55240dc`, and 1 of 4 on the release build (the full e2e run: pump 2, a 50.0 ms gap; pump 3, a 33.3 ms gap; the CPU check called the machine quiet).
- **Each kind's own work limit is report-only** (owner, 2026-11-19, "Ship now"). On the merged release the drop's median read 6.30 ms against 5.5, on a quiet machine with the same tree that had passed. Its median rests on about 34 frames a run; across 25 earlier runs of the fixed build it read 3.7–5.1. A pump or drop over its own limit now gives a SLOW KIND warning. The pooled limit (5.5 ms) and the held-clock pins stay hard.
- **The whole timing check is report-only for 1.15** (owner, 2026-11-19, "Ship on the fixed tests"). After the container restarted, the pooled limit failed 4 of 5 runs of the fixed build on the same tree that had passed. Old and fixed builds, interleaved on that machine: the old build read 9.2–11.5 ms of work per frame (6.5–8.2 before the restart), the fixed one 4.8–5.8. The fix still roughly halves the work, but a limit in fixed milliseconds can't tell a slower machine from a slower app. Late frames, slow reveals, machine noise and machine busy are now loud warnings. What still fails the run: the reveal not run, motion off, far too few frames, the cold/warm proofs, and the calibration. The hard checks on the speed fixes are the held-clock pins in the e2e project.
- **One skipped frame at the colour-change moment is accepted,** and so are the occasional late frames mid-reveal. Chromium draws no frame while the view transition captures the new side, in nearly every pump.

**Untested:**
- **Safari/WebKit and Firefox.** Only Chromium was available. Where a browser can't animate the reveal's pictures, the fallback applies: the old side stays still, or the new side shows at once.
- On real phones: the vibration strength, the media-usage vibration with touch feedback off, the iPhone WAV fallback, the light colours on a real screen, and 60 fps on a mid-range Android phone.

**Follow-ups:**
- The 5.5 / 6.0 / 5.5 ms work limits are absolute milliseconds on this container (4 vCPUs, headless Chromium). A faster or slower machine needs them measured again.
- **Rebuild the timing check on a same-run reference:** serve the build before the fix (`0a8604f`) next to HEAD in the same run, interleave them, and fail on the ratio or difference rather than on fixed milliseconds. Then it can be a hard gate again.
- **Recalibrate the per-kind work limits** (folds into the same-run rebuild above). Give the drop a sample comparable to the pump's (more repeats, or discard the first animation of each kind). Re-derive both limits from `0a8604f` and HEAD and freeze them before the acceptance runs (`0a8604f` fails 10 of 10, the drop-2× mutant 5 of 5, HEAD passes 20 of 20). Then make them hard again. Until then, a drop-only slowdown is caught only by the pooled limit, which catches it weakly.
- **The pump's margin is thin:** in the release's full e2e run the pump's reveal work read 5.70 ms against its 6.0 limit (pooled 5.20 against 5.5), on a window the CPU check called quiet. When the limit was frozen, this build's highest was 5.30. The three speed runs after it read pump 4.60–4.70.
- `1abd100` on its own fails `npx tsc -b` (fixed by `7582ae5`). Skip it when bisecting.
- No test can tell the `onPseudo` try/catch from the `ready` promise's `.catch`: removing the try/catch keeps every test green.
- On a slow app with one idle miss, the gate says MACHINE BUSY, which hides the slow app (2 of 5 old-build runs). The run still fails.
- In the drop mutants, the pump medians rose too, and nobody knows why.
- `tools/render-mode-switch-sounds.js` has no test.

## Done: 1.16, the guide (both apps)
Spec: `docs/superpowers/specs/2026-11-19-grit-guide-design.md`; plan: `docs/superpowers/plans/2026-11-19-grit-guide.md`. The reports: `.superpowers/sdd/2026-11-16-grit-look/116-*.md` (not committed).

**What the lifter gets:**
- **A guide to Grit, in eight short screens:** what Grit is, the two sides, the plan, logging a set on Today, how the coach moves targets, when it holds back, what months of training look like, and where to start. Each screen has a small moving picture: from screen 3 on, the real app with a made-up sample lifter, marked "Example" where it shows numbers. Skip, Back and Next on every screen, dots for where you are, and a swipe between screens.
- **On a new install it opens by itself, once:** only when there are no sessions and no plan on either side, so a lifter who updates doesn't get it unasked. Screen 2 really picks your side there (with no animation), and the screens after it describe that side. Screen 8 opens that side's templates (and on bodybuilding offers Build my plan). Skipping or finishing it means it doesn't open again on that device.
- **Replay it any time from Settings** (on Plan), under Help: **Guide**, "See how Grit works". A replay changes nothing: it never touches your side, plan or log; with a plan, screen 8 offers Done and See my plan.
- **Android, the Today screen:** the tick now draws itself in when you log a set, and the "Skipped today" line slides up into place when you skip. Neither plays when a screen opens, comes back or scrolls, nor with animations off.
- **Welcome back and easing back in now apply only to a side you had trained before the break.** A side you switched away from before ever training it isn't greeted as returning, and its first targets aren't eased. Picking a side on the first launch doesn't count as leaving the other one.
- **On the web, at large text the guide shows its text without pictures** where both don't fit, and the text scrolls only if it still doesn't fit. Back and Next stay on one row.
- With reduce motion (or the app's motion off), each screen shows its end at once.

**The owner's decisions:**
- The eight screens, and that screen 2 sets the real side on a new install (option A).
- Smoother motion: the pictures flow between steps; the guide feeds in-between values (a weight counting up, a chart drawing in).
- The three extras: the tick drawing in, a tap ring on the button just before its result (guide only), and the "Skipped today" line rising in. The tick and the line are on Android's real Today screen too; the web's real Today is unchanged.
- Welcome back needs a side trained before it was left (spec point 10), with its stored card gated the same way.
- The pictures' layout (spec point 11): WELCOME and SIDE are art with no phone outline; WELCOME's side colours are plain panels, not buttons; one phone size on the six screens that have one; no page keeps room for screen 8's second button.
- Large text on the web: the text first (reversing the same day's least-size rule).

**My calls** (the spec's "Settled after the builder check", and while building):
1. **"New user"** means no sessions and no plan items on either side. "Seen" is kept on the device (web `meta` `guide:seen`; Android its own settings key outside `AppSettings`), not in backups, and kept by a restore.
2. **iPhone:** the Home Screen app has its own storage, so a lifter who opens Grit in Safari and then installs it sees the guide once in each. In a browser tab the guide comes first, the install card after it.
3. **Coach text** says what the coach does in plain words, each number built from the code's own constant (a test fails if they differ), and names no sources.
4. **Screen 2's picture** is a short silent loop of the 1.15 hammer and pump, drawn with the mode switch's own paint code (moved into a shared file on each app, behaviour unchanged). No sound or vibration.
5. **The first-launch side pick dates no side as left** (spec point 9). The Settings switch is unchanged.
6. **The pictures are the real screens' own content,** shown small with sample state: `TodayContent`, `PlanContent`, `TemplatesContent`, `CoachContent`, `ProgressContent`.
7. **No phone below 112 CSS px** on the web: where a phone would be smaller, there is none. WELCOME's and SIDE's art is drawn whole or not at all (not below 120 × 200 px; SIDE not below 212 px wide, so its two names fit).
8. **Version** 1.16, versionCode 26. No database change (Room v5, Dexie v5).

**The rule table's findings** (the comment at the top of `domain/.../guide/Guide.kt`):
- Each side's screens show only the rules that side's coach runs: stall and weak-point advice on powerlifting only; swaps and check-ins on bodybuilding only. On powerlifting, screen 7 ends on the bests and the total.
- **The pain hold has no day count:** after a pain skip the exercise's weight doesn't go up until it is next logged. The day counts belong to other rules: 14 days off for easing back in, 21 days between deloads and before a stuck bodybuilding exercise gets a swap, 28 days between check-ins.
- The deload's 21-day wait applies only after an applied deload, so the guide doesn't state it. The weak-point rule has no line (screen 6's word budget). `ProgressionRule`'s numbers are now named constants, with no behaviour change.

**Where the apps differ (accepted):**
- **Large text:** Android keeps the phone at its size and the text scrolls; the web drops the phone where it and the full text don't both fit, and Back and Next keep one row without their icons.
- **The tick:** on Android it is the real Today screen's, over 280 ms. On the web it is the guide's own stand-in, drawn over its row's 150 ms fade, so it isn't drawn before the set's numbers.
- **The smooth motion:** Android's real screens already animate, and the guide inherits that. The web's real screens animate only from taps, so the guide adds its own layout animation. MONTHS_LATER's chart draw-in on the web is the real screen's CSS, played once in real time, not on the guide's clock.
- **The swipe:** Android's is the pager's drag, with the page following the finger; the web's moves a screen once a sideways swipe passes 50 px.
- **Screen 8's size:** the web measures both sets of controls (a narrow phone can wrap "Choose a template" onto a row of its own), and screen 8 is smaller by their difference only. Android takes one button's height.
- **Landscape at the default text size (web):** no phone on the six outlined screens below about 154 px of room for it (844 × 390, 664 × 390, 667 × 375, 568 × 320); the text shows in full.

**Speed:** the structural checks are hard (the guide's chunk isn't loaded before it opens; no layout reads per step; screen 2's loop runs nothing on a held clock). The time to the first screen's title only reports, and asserts its 300 ms limit only when it is under 150 ms on the machine, as the owner chose for 1.15's timing.

**Review fixes before the release:**
- **Web SIDE in a low landscape place:** at 664 × 390 the two names "Powerlifting" and "Bodybuilding" ran into each other. Each side's column is now as wide as its box or its name, whichever is wider, and below 212 px the art isn't drawn.
- **Coach:** an easing-back-in card that doesn't count on bodybuilding (made on powerlifting for a break bodybuilding wasn't trained before) no longer holds back that side's stuck-exercise swap for 21 days. One that counts still does.
- **Web install card:** "Not now" hides the card once the answer is saved (or the save fails), so a reload straight after can't bring it back.

**Tests:**
- **New, domain:** `GuideTest`, `GuideSampleTest` (the copy's numbers from the constants, the sample lifter inside the coach's own rules), `GuideBridgeTest`, `ReEntryGateTest`, `ReEntryStuckTest`.
- **New, Android:** `GuideScreenTest`, `GuidePicturesTest`, `GuideFirstLaunchTest`, `MainActivityGuideTest`, the tick and skipped-line tests on Today, and the guide's shots in `ScreenshotTest`.
- **New, web:** `GuideScreen.test.tsx`, `pictures.test.tsx`, `guideSeen.test.ts`, `e2e/guide.spec.ts` (first launch, the seam, replays writing nothing, every picture's first and last step on a held clock, reduce motion, 200 % text on three phones, the outline sizes, landscape SIDE, the speed report), and the install card's save order.
- **Changed:** every e2e storage state now comes from `e2e/storage.ts` (seven specs' `test.use` lines), and a guard test fails on one written by hand; `MainActivityLockTest` marks the guide seen in its setup; `coach.spec.ts` waits for the stored statuses before reloading; a test that pinned the old welcome back after the Settings switch now asserts none.

**Version:** Android 1.16 (versionCode 26), web 1.16. Settings shots re-shot on both apps (the Guide row).

**Untested:**
- **Safari/WebKit and Firefox.** Only Chromium was available. The web guide's art needs container queries (`cqw`, `cqh`: Safari 16 and later) and `:has()` (Safari 15.4); the web tick needs `stroke-dashoffset` on a `pathLength` path.
- **Real phones:** the first launch in Safari and on the Home Screen, real browser text scaling or zoom (the e2e halves the viewport for 200 %), a real screen reader, the Android tick at large font sizes, and the swipe.
- Landscape at large text.

**Follow-ups:**
- **`vibrateOnTaps` resets on an Android restore** (found while planning; not part of this work).
- **23 real web Today shots** in `docs/design/web/screens/` render differently on this machine (the steppers' fields at slightly different widths), from before this work. None was re-shot.
- The guide's web shots are not byte-stable between runs (SVG anti-aliasing in SIDE, a level or two elsewhere); only real changes are committed.
- Three pushed merge commits on the branch (`058df64`, `7e33e2a`, `7c60549`) fail `tsc -b` until `2869f92`. Skip them when bisecting.
- Spec point 11 doesn't state WELCOME's and SIDE's least room (120 × 200 px; SIDE 212 px wide).
- A skip ring (a tap ring on Skip today's menu) was not built; it could be anchored as Log set's is.

## Done: 1.16.1, Known bests show the best (both apps)

**What the lifter gets:**
- **Each Known bests row on Plan shows the lift's best** under its name: "Best e1RM 182.5 kg". It is the higher of the best you typed and your best logged estimate ever (the same number as Progress's Records and Lift history's "Best e1RM"), from powerlifting sessions only, to the half kilo. A lift with neither shows no line, and its row looks exactly as before.
- **With Settings → Progress → Strict press on, strict press is always in Known bests.** Before, it was there only once the lift had an exercise of its own (added to a plan or a session, or made by the strict press upgrade from an "Overhead press" or similar), so with the switch on and no such exercise it was missing. Now its Set best makes that exercise (as picking it from the catalogue does) and stores the best, together or not at all.

**The owner's decisions:**
- The figure: the higher of typed and lifted, one shared domain function (`ProgressMath.knownBest`, `grit.knownBest` on the web), not Progress's 28-day "current" figure.
- The words: "Best e1RM", Progress's own term.
- The switch off hides nothing: a strict press that has an exercise keeps its row and its best, as before 1.16.1, because the coach uses that best whatever the switch says.

**My calls:**
1. With the switch on and no strict press exercise, the row is the catalogue's "Strict press", stored nowhere (id 0) until its Set best. An exercise in the strict press category under any name, archived or not, counts as having one, so the switch never adds a second row.
2. Several strict press exercises (the upgrade can leave aliases such as "OHP" in the category) each keep their row, as before. They all show the same figure, the lift's. Their typed bests are one per exercise, and the latest typed one is the lift's, as in Progress's fallback, so a Set best on one of them changes the line on all of them, even to a lower number. A tie differs: of two typed on the same day, `knownBest` takes the higher, while Progress's `currentE1rm` takes the first in list order. Archived main lifts still show, as before.
3. **Web precache limit raised to 3 MiB** (`vite.config.ts`). The main chunk passed the PWA plugin's 2 MiB default (1.16: 2,095.89 kB; 1.16.1: 2,097.53 kB). At the default, `npm run build` stops with "error during build" and the plugin's message: "Configure "workbox.maximumFileSizeToCacheInBytes" to change the limit: the default value is 2 MiB. … Assets exceeding the limit: - assets/index-….js is 2.1 MB, and won't be precached."

**Tests:**
- **Domain:** `ProgressMathTest` (no best; typed only; lifted beats typed; typed beats lifted; the all-time record; failed sets, variations and bodybuilding sessions ignored; the latest typed best of the lift's own exercises) and `GritDomainTest`.
- **Android:** `KnownBestsViewModelTest`, and `KnownBestsScreenTest` (the line after Set best; the switch on with no exercise, then its Set best; the switch off; a two-line name at 320 dp with Set best beside it). New shot: `docs/design/screens/plan-known-bests-{dark,light}.png`.
- **Web:** `KnownBests.test.tsx` (the same cases). In `e2e/plan.spec.ts`, not run here: Known bests with bests set and the switch on, on dark and light, with the shot `plan-known-bests-{dark,light}.png`, and a 320 × 568 check of a two-line name beside Set best.

**Version:** Android 1.16.1 (versionCode 27), web 1.16.1. No database change (Room v5, Dexie v5). A row with no best is drawn as before: Android's `plan-{dark,light}.png` re-rendered byte for byte the same. The web and guide shots were not re-rendered here.

## Done: 1.17, counting plates on stack machines (both apps)
Spec: `docs/superpowers/specs/2026-11-20-plate-count-design.md`; plan: `docs/superpowers/plans/2026-11-20-grit-plate-count.md` (its "As built" note lists what changed after approval). The reports: `.superpowers/sdd/2026-11-20-plates/117-*.md` (not committed).

**What the lifter gets:**
- **Count plates on this machine,** per exercise, in the exercise's ⋮ menu on Today: on its card, its folded row and the start card's row, before or during a session, with or without sets. Every exercise starts in kg. The item is on machine and cable accessories from the catalogue and on your own accessories. Squat, bench, deadlift, strict press, their variations, the catalogue's Smith exercises and the assisted machines never have it. Your own accessory is offered it whatever you named it, unless the name says "assisted", so one you named after a Smith or dumbbell movement is offered too.
- **The sheet asks "How does this machine show its weight?"** with three choices: **In kg**, **Whole plates** (1, 2, 3 …) and **Half steps too** (1, 1½, 2 …). A choice applies when tapped; Done closes the sheet. One setting covers both sides.
- **Logging in plates:** the weight field reads PLATE and starts empty. + on an empty field gives plate 1, and − and + move one plate, or half a plate with half steps. The field never goes below plate 1 or above plate 50. Anything else typed is refused with "Whole plates only", "Whole or half plates" or "Plate 1 to 50". A set reads "plate 7 × 10" or "plate 7½ × 10", and a screen reader says "plate 7 and a half". A plate set has no bar picture. A jump of one step never warns. A jump of more than one step that is also more than 30 % above the last plate asks "That is a big jump from plate 3." (with whole plates, plate 3 to 5 asks and plate 10 to 12 does not; with half steps, plate 3 to 4 and plate 1 to 2 ask too).
- **The coach works in plate steps on both sides.** It reads only the exercise's plate sets, so it starts the exercise fresh: there is no target until the first plate set is logged.
  - **Bodybuilding:** stay on the same plate and add reps. When every planned set reaches the top of the range at or under the target effort, go one step up, back to the bottom of the range.
  - **Powerlifting:** on target or easier, one more rep on the same plate, up to 12. At 12 reps, one step up and back to the planned reps; a plan of 12 reps or more earns the step each time. A little too hard (more than 0.5 and less than 1.5 over the target RPE; exactly 0.5 over counts as on target), or just after a step up with the effort rising: repeat, keeping the reps you earned. A missed top set, or one 1.5 or more over the target RPE: a step down (95 % or 97.5 % of the plate number, rounded down, at least one step).
  - **A lighter week (deload) or a return after 14 days off:** 90 % or 85 % of the plate number, rounded down, at least one step down, never below plate 1. Both at once take the lower, not two steps, and a lighter week of several sessions loses one step only. Example: plate 7 in a deload is plate 6, and easing back in is plate 5 (5½ with half steps).
  - **First time on the bodybuilding side:** the heaviest plate logged without a miss on the powerlifting side in its last 28 days of sessions on the exercise. When those sets are more than 28 days old, the plate is eased once by the 85 % rule above.
- **Switch back to kg any time:** the coach picks the old kg sets up again, and the plate sets keep reading "plate …" in the exercise's history. Both histories are kept as they are. When the change starts a new unit and the exercise has sets in the old one, the sheet says "The coach starts this exercise fresh in plates. Your kg sets stay as they are." or "The coach goes back to kg. Your plate sets stay as they are."
- **Plate sets never count in kg figures:** not in estimated max, totals, volume, records or Known bests, and none is celebrated as a record. Bodybuilding Progress shows "plate 9 × 10" and compares plate with plate.
- **Backups carry plates:** each set's unit and each exercise's setting (backup version 6). Android restores the web's backups and the web restores Android's.
- **Between Whole plates and Half steps too:** this is not a change of unit, so there is no note and the coach keeps its plate history. A logged 7½ stays 7½; − and + from it go to 7 and 8. With Whole plates on, typing 7.5 is refused and the coach's targets land on whole plates: a repeat or a lighter week rounds down (7), and a step up goes to 8. A plate 7½ suggestion made earlier that day no longer fits the steps: it is not shown, and a whole-plate one replaces it. A whole-plate suggestion stays when half steps are turned on.

**What a lifter who stays in kg can notice:**
- Settings shows version 1.17.
- On the web, a long kg headline in the coach's chip that used to end in "…" (such as "112.5 kg × 8–12 @ 8") now wraps.
- A 1.17 backup is refused by 1.16.1 and older (see "Web safety").
- The ⋮ menu also shows, with Count plates on this machine as its only item, on the start card's rows (and folded rows and open card) of machine, cable and custom accessory exercises that had no ⋮ before: ones added today, and planned ones that already have sets.
- The web skip sheet's tap guard was moved into shared code (`web/src/screens/today/tapGuard.ts`, used by the skip and plate sheets) with no change in behaviour.

**Web safety:**
- **Open Grit windows reload themselves when an update takes over.** When a new version of the web app takes over, it reloads every Grit window the old version was running, so a window showing "Grit is open in another tab" or the lock screen no longer stays on old code. This reaches windows still on 1.16.1: seen on the real 1.16.1 build updated to 1.17, in eight two-window runs out of eight, each window loading once (a probe, not a kept test). Seen working in Chromium only. In the update from 1.16.1 itself a window comes back on the screen it first loaded at (a window opened on Today and moved to Plan comes back on Today); from 1.17 on it comes back where it was. After the reload, two windows race for the one-tab lock, so either may come back as the window in use and the other as "Grit is open in another tab".
- **The new "Update Grit to carry on" screen.** From 1.17 on, a copy of Grit older than the log on the device shows it in place of the app: "This copy of Grit is older than your log, so it can't open it safely. Nothing has been changed." It has one button, **Update and reload**, which fetches the newer version and reloads into it, or says "Couldn't update. Check your connection and try again." It opens nothing and offers no export.
- **A 1.17 backup is refused by 1.16.1 and older** as too new, and nothing is changed. The web app updates itself; the Android app does not, so update Android before restoring a newer backup there.
- **Never roll the web deploy back below 1.17: fix forward.** A 1.16.1 copy opens a log that 1.17 wrote. It would read plate numbers as kg and export backups without the plate markers, and it has no "Update Grit to carry on" screen.

**The owner's decisions:**
- The design, with **half steps allowed** (some stacks have a small add-on weight). Released as 1.17, after 1.16; the 1.16 guide is unchanged.
- The four design answers: custom variations are not offered (accessories only); no exercise chart in 1.17; the assisted machines are not offered and stay in kg; the powerlifting progression as proposed (one more rep up to 12, then one step up and back to the planned reps, and a step down after a missed or much-too-hard top set).
- **A repeat keeps the reps earned** on the powerlifting side (the kg rule's repeat goes back to the planned reps).
- **The "Update Grit to carry on" guard goes in 1.17.**
- **The ⋮ menu is the way in.** There is no shortcut on the field label.
- **Switching back to kg builds on the old kg number** (2026-11-21): plate sessions count as training, so an old kg top set is not eased for its age when the exercise was trained in plates meanwhile. Fourteen days with no sessions, or a lighter week, still ease it. The design's "as if plates had been a break" wording is corrected to match.

**My calls** (the plan's Planner's calls, then the calls made while building):
1. **Names:** `LoadUnit { KG, PLATES }` and `PlateCount { OFF, WHOLE, HALF }`; the columns and keys are `unit` and `plateCount`. `weightKg` keeps its name everywhere and means the plate number beside `PLATES`. Renaming it would have touched every store, backup and payload.
2. **"Current unit" is one history filter,** `inCurrentUnits()`, applied where the coach reads. That gives one place to check, not a unit parameter on every statistic. Sessions emptied by the filter are kept, because the 14-day break and easing back in count sessions.
3. **Which exercises are offered:** the row's own category decides. A custom accessory is offered whatever its name, except one holding the word "assisted" ("Unassisted row" is offered; "Machine-assisted dip" is not). The three assisted machines are left out by the same word. A custom row can carry a catalogue name, so both of the design's rules can apply to one row, and the row's own category settles it.
4. **A stored setting on an exercise that is not offered counts as off** (`unitOf`), so a main lift can never be in plates, even from an edited backup.
5. **Typed plates run from 1 to 50** ("Plate 1 to 50"). The design left the upper bound to the builder.
6. **Powerlifting details:** a repeat keeps last time's reps when they are above the plan's (confirmed by the owner); the extra rep never aims under the plan's reps; a logged half with Whole plates on gives the lower whole plate for a repeat, an added rep and a step down (7½ missed is plate 7, already one whole step under).
7. **No 5 % cap in plates:** one plate is nearly always more than 5 %.
8. **The typo check has a second form** (`InputValidator.check` with a unit and a step, `checkLoadInput` on the web) instead of a changed `checkInput`, so the existing callers and tests stay as they were.
9. **Backups refuse a plate set under plate 1** on both apps, as a corrupt file ("A plate set below plate 1"), with nothing changed. (1.17.1: see below)
10. **The sheet:** a choice applies when tapped; its accessible name is "Count plates on <name>", because the heading alone would share a name with the form guide's sheet; going back to kg has its own note, "The coach goes back to kg. Your plate sets stay as they are." (the design gives only the first).
11. **The PLATE field shows 7.5,** the text the field reads back; everywhere else prints "7½".
12. **The field's numbers carry their unit,** and numbers kept in the other unit are ignored as well as deleted on a change, so a kg number can never land in the PLATE field.
13. **A pain skip still deletes that day's open targets of both units:** the coach's next pass makes the current unit's again.
14. **Backup fixtures:** each app writes its own version 6 fixture in its storage task, and the cross-restore is a separate task after both merge.
15. **Dexie version 6 stamps nothing** (the design said it would stamp every set kg and every exercise off). An absent field already reads as kg or off, and existing tests compare whole rows across an upgrade. Version 6 is declared for the number and the safety copy.
16. **The item is also on the start card,** so the setting can be changed before a session starts. The design named only the exercise's menu on Today.
17. **`FatigueRule` skips non-kg main-lift sets,** though the design calls it unchanged: it reads raw `weightKg`, and the guard costs a real log nothing.
18. **Backups and stored rows never write a default:** a row carries `unit` only for a plate set and `plateCount` only for a plate-counted exercise. A kg-only log exports exactly as version 5 did, except for the version number. The frozen fixtures and whole-row tests hold as they were.
19. **"Assisted" is matched as a word,** after the catalogue's name folding (lower case, hyphens as spaces). It matches at the start of the name or after a space, so "Unassisted" is not caught.
20. **The tap guard:** for 300 ms after the sheet has opened (and while it slides up), taps on its three choices do nothing, as on the skip sheet. A double tap on the menu item never picks a choice. Done is not held.
21. **The sheet shows what is stored:** its mark is the stored setting. A tapped choice is marked at once and stays marked while its save is in flight. A tap is compared with what is marked, so the last tap wins.
22. **A number is never relabelled:** a set entered in one unit is not saved when the exercise's setting changed before Log set (another tab, a slow tap). The lifter types it again in the new field. A set held by the typo dialog keeps the unit it was checked in.
23. **An off-grid target is not used:** a plate 7½ target, once Whole plates is on, is neither shown nor applied and does not count as known. The coach stores a fresh one on the grid beside it; nothing stored is deleted or rewritten. Kg targets are not affected.
24. **The out-of-range backstop:** a plate set under plate 1 or over plate 50 is never stored, whatever built it (for example a hand-edited kept field on the web). A half with Whole plates on is still stored, since a logged half stands.
25. **The menu item's icon is Material's Tune** (sliders) on both apps, beside Skip today's icon.
26. **The coach's chip wraps instead of being cut** (web): at 320 px and 200 % text, "plate 12½ × 8–12 @ 8" wraps at a space, and below 220 px of chip the text takes the whole width with Apply and Dismiss under it. Where it fits it is one line, as before. Kg headlines that used to end in "…" (such as "112.5 kg × 8–12 @ 8") now wrap too.

**Where the apps differ (accepted):**
- **The sheet's mark after a save:** Android shows the stored setting again once the save has settled. The web keeps the tapped choice marked until the stored setting shows it, or until the save fails, so the mark doesn't flick back while the screen catches up. If another tab changes the setting mid-save, the web's mark stays on the tapped choice until the next tap or until the sheet closes.
- **The coach's chip:** on the web its headline wraps on a narrow phone with large text. On Android it stays one line, and it fits at 320 dp and twice the font size.
- **The field's numbers:** the web keeps them across a reload, so its unit check also covers numbers kept from before a switch or from another tab. Android keeps them in memory.
- **A plate target's stored number:** Android writes "8.0", the web "8". Both read either.
- **Only the web** reloads its open windows on an update and has the "Update Grit to carry on" screen. The Android app doesn't update itself.
- **The sheet:** a bottom sheet on Android, the web's sheet dialog. Words, choices, order and test tags are the same.

**What was not built:**
- No conversion between plates and kg, and no question about what a plate weighs.
- No exercise chart.
- No plates on the main lifts, their variations (built-in or custom) or Smith exercises.
- The assisted machines stay in kg.
- No per-gym or per-side profiles: one setting per exercise.

**Findings:**
- **From "Where the code differs from the design":**
  - versionCode is 28: 1.16.1 took 27.
  - Android has stand-ins too (a catalogue exercise added on Today with no row yet): choosing plates there makes the row, as on the web.
  - `ProgressionRule.MAX_REPS` and `InputValidator.MAX_JUMP_FACTOR` became `internal`, with no value change.
  - The two apps test each other's backups through shared fixtures in `web/src/backup/__fixtures__/`.
  - "Every existing backup test passes unchanged" could not hold: the version-pinned tests were edited, each as the plan lists. One more edit: `webFixture_isUpToDate` passes the new `plates` option to `buildSampleDb`, because the test can't equal `web-v6.json` without it.
  - A custom row can carry a catalogue name, so a custom accessory named after a dumbbell or Smith catalogue entry is offered.
- **An older web app opens a newer database.** Declaring Dexie version 6 does not make 1.16.1 refuse a log that 1.17 wrote. After a `VersionError`, Dexie 4.4.7 opens a newer database whose stores match. A stale 1.16.1 window could then read plate numbers as kg and export backups without the plate markers. What protects now:
  - The new service worker reloads every window the old one controlled. This should reach 1.16.1 too (reasoned from how the worker reloads windows, not tested against a real 1.16.1 page).
  - From 1.17 on, `openGritDb` refuses a database a newer Grit wrote, before any read, write or safety copy, and shows "Update Grit to carry on". A database counts as newer when its native version / 10, rounded down, is above the declared version: the 61 Dexie leaves after a schema patch still opens, and 70 is refused. A database with no stores is left to the recovery screen.
  - Still unprotected: a 1.16.1 window that no worker controls, or a browser where the worker can't navigate windows. Such a window keeps only 1.16.1's own "versionchange" screen while it holds a connection.
- **`client.url`:** Chromium reports a window's address as the one it loaded at, and ignores `replaceState`. Sending a window to that address was a jump within the page, so a window that had moved to another route did not reload at all. The worker now sends each window to its address with a `u` parameter that is new on every update, and the app takes it out before anything reads the address. Each window keeps its route in session storage, so it comes back where it was. This works only for windows already on 1.17: a 1.16.1 window comes back on its load-time route.
- **The installed version was rounded:** `installedVersion` and `openGritDb` rounded native / 10. A Dexie-patched version 5 database at native 55 to 59 read as 6, so the 5 to 6 upgrade got no safety copy and no upgrade-failed wrap. Both now round down, as the older-copy guard does.
- `PlateStack.check` on a number beyond a whole number's range (such as 1e30) answers STEP, not RANGE. No input reaches it, because both apps bound what can be typed.
- **Switching back to kg is not a break when the exercise kept being trained in plates** (the design said "as if plates had been a break"; see its dated note). The 14-day break rule reads the gap in the whole log, and plate sessions of the exercise count as training, so an old kg top set is not eased for its age alone, as for a kg lifter who leaves one exercise out for months while training others. It is eased on the return from 14 days with no session at all, and while a lighter week or easing back in is active. One more case eases it: with a session on the other side in the last 28 days and no kg set of it on this side in those days, the side-return rule (`ReturnRule`, which reads only current-unit sets) eases it as it would a kg exercise left out on this side. And on the bodybuilding side, an exercise with no set there in its current unit takes a first-time target from the powerlifting side's sets in that unit, eased when they are more than 28 days old, as for any first time. The same holds the other way (old plate sets, recent kg sessions, plates turned back on).

**Tests:**
- **Domain:** jvm 974, js 1023, 0 failed. New: `PlateStackTest`, `InputValidatorPlatesTest`, `UnitsNeverMixTest`, `TargetUnitTest`, `ProgressionRulePlatesTest`, `CoachPlatesTest`, `BodybuildingCoachPlatesTest`, `ReturnRulePlatesTest`, `GritDomainPlatesTest`.
- **Android:** 1096 tests, 1 skipped (the guide frame strips, which run only when asked), 0 failed. New: `PlateCountStorageTest`, `TodayPlatesViewModelTest`, `PlateCountScreenTest`, `PlatesProgressTest`, and tests added to `MigrationTest` (5 to 6, 1 to 6), `BackupTest`, `BackupFixtureTest` (both apps' version 6 fixtures) and `EntryTest`. New shots: `today-plates-{powerlifting,bodybuilding}-{dark,light}`, `today-plates-menu-{dark,light}` and `plate-sheet-{dark,light}` in `docs/design/screens/`.
- **Web:** vitest 1612 tests in 87 files; `tsc -b` and the build clean. Playwright (`npx playwright test --trace off`, e2e and speed): 390 passed, 12 skipped (the owner's guide-video recordings, as before), 0 failed. Speed project alone, three times: 6 passed each time. The mode switch's frame check printed its LATE FRAMES warning (a warning only) in the full run and in the third speed run. New: `PlateCount.test.tsx`, `useTodayPlates.test.tsx`, `olderCopy.test.ts`, `AppOlderCopy.test.tsx`, `OlderCopyScreen.test.tsx`, `updateApp.test.ts`, `reloadMarker.test.ts`, `swReloadClients.test.ts`, and e2e `plates.spec.ts` (end to end, half steps, no item on Bench or Assisted dip, a stand-in getting its row, the shots), `olderCopy.spec.ts`, the two-window and first-install cases in `update.spec.ts`, and plates at 320 px and 200 % text in `narrow.spec.ts`. The same eight shots in `docs/design/web/screens/`.
- **Edited existing tests:** only the version pins the plan lists (both apps), plus the `plates` option above. Three of the older-copy screen's own new assertions changed when "Updating…" began to be announced.
- **Release build:** `./gradlew :app:assembleRelease` gives versionCode 28, versionName 1.17 (the APK was not distributed).

**What only reports:** nothing new in 1.17. `update.spec.ts` prints, beside its assertions, what the worker saw when it took over on the first and the second deploy (windows, addresses, navigations: two `console.log` lines), and how many times the page loaded after Update and reload on the older-copy screen (a third). The 1.15 and 1.16 speed reports are as before.

**Version:** Android 1.17 (versionCode 28), web 1.17. Room version 6 (`MIGRATION_5_6` adds the two columns with kg / off defaults), Dexie version 6 (declared, no upgrade step), backup version 6. Update both apps together. Settings re-shot on both apps for the version line (they said 1.16; 1.16.1 had not re-shot them): six shots on each app, each byte-identical across two runs here, differing from the old ones only in the digit.

**Not tested:**
- No real phone or emulator.
- Safari and Firefox, including the reload on update there: whether the worker can navigate windows, which address it reports and whether the route comes back. The README's two-window check is the test.
- Real screen readers.
- Real browser text scaling: the e2e halves the viewport for 200 %.
- The web sheet's mark when another tab changes the setting during a save.
- A real 1.16.1 build refusing a 1.17 backup: both apps' tests check this with their version 5 reader.
- The reload on update reaching a real 1.16.1 page has no kept test: the e2e fakes a new deploy by rewriting the tested build's `sw.js`, so its windows run 1.17's app code. It was checked once by hand-run probe before the release (the real 1.16.1 build served, then 1.17 deployed over it, Chromium headless): one window, two windows three times, four route variants, and a first install; every 1.16.1 window ended on 1.17 after one load, with no loop. A rollback to 1.16.1 in the same probe showed the documented harm: the plate set "plate 7 × 10" read "7 kg × 10".
- The "Update Grit to carry on" screen on a phone: reaching it needs a log written by a newer build than 1.17, so the README has no check for it. The e2e checks it.

**Follow-ups:**
- **Today at 320 px and 200 % text scrolls sideways** on the web, in kg as in plates: the page is 191 px wide in a 160 px layout, eyebrows end in "…", a button sits outside the screen and two tab labels are clipped. This was there before plates, and the narrow test checks only the page and the sheet.
- **The recovery screen's unhandled rejection:** with a database that has no stores, its first look could reject unhandled (a synchronous Dexie `InvalidTableError` inside `Promise.all`). Fixed on main (commit `3a46ae4`, merged in `800060e`).
- **Which way the assisted machines count:** more weight is easier there, and nothing says whether lifters log the assistance or the load. That question is for its own release.
- **Two flaky tests under load:** `SkipTodayViewModelTest.skip_undo_removesSkipAndOpenCard` and `ModeSwitchOverlayTest.framesKeepTime_dropAndPump`.
- **Two number forms in stored target payloads:** "8.0" on Android, "8" on the web. Both read either.
- **An earlier half-step suggestion can show again** (cosmetic): after Half steps too → Whole plates → Half steps too on one day, two open plate suggestions exist (the 7½ one and the whole-plate one), and both apps show the first. So once the whole-plate one is applied, the earlier plate 7½ suggestion can show again. Fix: show the latest open one instead.
- **A hand-edited backup can carry an off-grid or too-high plate number** (such as 7.3 or 80): both apps accept it, and 7.3 prints as "plate 7½". Only a plate under plate 1 is refused. Fix: refuse these too. (1.17.1: see below)
- **The three `console.log` lines in `web/e2e/update.spec.ts`** are deliberate reporting, as "What only reports" says, not leftovers.
- **Still open from 1.16:**
  - `vibrateOnTaps` resets on an Android restore.
  - The web's real Today shots render differently on this machine (the steppers' fields at slightly different widths). They were not re-shot here. The same goes for the web's today-rest, rpe-sheet, coach-bodybuilding and showcase shots.
  - The guide's web shots are not byte-stable between runs.
  - Spec point 11 doesn't state WELCOME's and SIDE's least room.
  - The skip ring was not built.

## Done: 1.17.1, the menu stays on screen, and three follow-ups (both apps)
1.17.1 is the change from 1.17 (`6cbd265`) to `38cc5cc`, plus the version and these notes. The reports: `.superpowers/sdd/2026-11-21-1171/1171-*.md` (not committed).

**What the lifter gets:**
- **The ⋮ menu on Today stays on screen** (web). On a narrow phone, a long name such as "Cable chest press" could push an exercise's ⋮ onto a line of its own at the left of the card, and its menu then opened off the left edge of the screen: "Skip today" could not be seen and "Count plates on this machine" was cut. Now ⓘ and ⋮ stay together at the right of the card, and a menu opens where it fits: under its button as before, or lined up the other way, or 8 px in from the edge of the screen; above its button when there is no room under it. It stays inside the screen's own padding (in the installed app that is below the status bar and the notch, and clear of a landscape phone's side insets) and above the rest bar and the tab bar. A menu taller than that room scrolls inside itself. Turning the phone or a change of the screen's width closes an open menu and puts the focus back on its ⋮; a change of height, such as the on-screen keyboard coming up or going away, and pinch zoom keep it open and on screen. A very long one-word name breaks inside the open card, a folded row or the start card instead of running past its edge. This is the same menu as Plan's ⋮, so it behaves the same way there.
- **The newest suggestion of the day decides** (both apps). After Half steps too → Whole plates → Half steps too on one day, the earlier plate 7½ suggestion no longer comes back once the whole-plate one has been applied or dismissed. If the newest suggestion is open it is the chip; if it was applied it fills the weight; if it was dismissed the weight is as with no suggestion. An older applied suggestion no longer fills the weight when there is a newer one.
- **A backup with an impossible plate number is refused** (both apps). Every plate set in a backup must be a whole or half plate from plate 1 to plate 50. If one is not, the backup is refused as damaged, with "A plate set below plate 1", "A plate set above plate 50" or "A plate set that is not a whole or half plate", and nothing is changed. The app never logs such a number either.
- **Today fits a 320 px phone at 200 % text** (web; at 239 px of layout and narrower, about 160 px here): the page no longer scrolls sideways, and nothing is cut or overlapping.
  - The tab bar shows its icons only; screen readers still hear each tab's name.
  - Finish sits under the title, and the date may take two lines.
  - A logged set takes two lines ("115 × 8", then "@ 8" with its tick), three with a Failed or Top set pill.
  - The three points of "Where did it stall?" (and "Where was it slow?") sit one over the other; a long one ("Off the shoulders") wraps inside its point.
  - A folded exercise's plan and count wrap under its name.
  - Before a session, the install and backup cards' buttons are stacked, and the readiness numbers take three rows (2, 2 and 1), each a full 48 px target.
  - STRETCH and SQUEEZE sit above their lines, and the rest bar has two columns (the clock and −30 s, then +30 s and Skip).
  - The screen's and the cards' sides are a little narrower at that width.
  - The other tabs at that width are not fixed yet.

**The owner's decisions:**
- Fix the four 1.17 follow-ups: Today at 320 px and 200 % text, the earlier half-step suggestion coming back, hand-edited backups with off-grid or too-high plate numbers, and the recovery screen's rejection (already fixed on main; see Findings).
- Finish the narrow screen properly, with: an icons-only tab bar, Finish under the title, a logged set on two lines, and tidied before-session cards.
- The ⋮ menu bug, reported by the owner from their phone (about 306 px wide, with "Cable chest press"), goes in this release.

**My calls:**
1. **The newest suggestion also wins over an older applied one.** Among today's suggestions for an exercise in its current unit and on its current steps, the newest (highest id) decides, whatever its state. So an older applied suggestion no longer fills the weight or sets the sets to do when a newer open or dismissed one is there. Nothing stored is changed or deleted. Kg exercises hold at most one suggestion a day, so kg is as before.
2. **The three backup messages, grid first.** A plate number is checked on the half-plate steps first, then for plate 1 to 50, whatever the exercise's setting: 0.3 and 7.3 are "not a whole or half plate"; 0 and 0.5 are on the steps, so they read "below plate 1"; 50.5 and 80 read "above plate 50". A logged 7½ still restores on an exercise now on Whole plates, or in kg. When a file has several bad sets, the first one in the file names the fault. The backup version stays 6.
3. **Logging uses the same rule as restoring:** a plate set that is not a whole or half plate from 1 to 50 is never stored, confirmed or not (1.17 refused only one outside 1 to 50). A 7½ logged with Whole plates on is still stored.
4. **At 160 px the tabs keep their 32 px width** (160 / 5) and the bar's 80 px height. Five 44 px tabs would need 220 px.
5. **Three readiness rows** (2 + 2 + 1), so each number keeps its 48 px target. Three a row would not fit at 48 px.
6. **The rest clock shares a row** with −30 s, and +30 s with Skip, in two columns, rather than the clock on a line of its own.
7. **The Failed pill goes on a third line** under the effort, as Top set does: "@ 10", the pill and the mark do not fit on one line.
8. **The menu's placement:** a menu that fits opens exactly as before (under, right edges aligned). Otherwise it lines up with the button's left edge, or else moves over to 8 px from the edge of its room. With no room under the button it opens above it, if there is room there. The room is the app's scrolling screen inside its padding on every side (the status bar, the notch and the installed app's extra band at the top; a landscape phone's side insets) and above the rest bar or the tab bar; outside the app's screen it is the window. With room on neither side it opens under; only a menu taller than the whole room is capped at the room less 8 px each end, and scrolls inside itself. Only once that placement is on the page does the screen scroll, just enough to bring the menu inside the room, and on opening its first item takes the focus without scrolling. What is guaranteed: a menu no taller than the room ends wholly inside it, so in the installed app never under the status bar or the notch; outside the installed app on iOS, the top padding is only the safe area. Turning the phone, or a change of the layout viewport's width (`window.innerWidth`), closes it and puts the focus back on its button. A change of height only (the on-screen keyboard going away as the menu takes the focus from a field, or coming up), or a change of the visual viewport alone (pinch zoom), keeps it open and places it again, at most once a frame, with the focus left where it is. A scroll of the visual viewport alone leaves it open.
9. **A very long one-word name may break** inside the open card's title, a folded row's name and the start card's plan line (`overflow-wrap: break-word`), only where it would otherwise run past the card.
10. **The set row's spans:** the weight and reps, and the effort, each have a box of their own (`gr-set-load`, `gr-set-effort`), so a narrow screen can put them on two lines. At other widths the row looks as before.
11. The narrow rules apply only inside the app's screen and rest bar (`.shell-main`, `.shell-rest`), so the guide's pictures of Today are drawn as before. Added to make room, beyond the owner's list: the screen's sides 8 px (were 16), the cards' padding at most 12 px (were 16), the summary's two tiles stacked, and the plan line's name beside its ⋮ with the coach's line and the plan under them.

**Where the apps differ:**
- **The menu and the narrow Today are web only.** Android's card head can't show the menu bug (see Findings), and Android's Today was not changed for narrow screens.
- The newest-suggestion rule, the three backup messages, their order and the logging rule are the same on both apps.

**What was not built:**
- The other tabs at 160 px (see Follow-ups).
- No repair for a plate set that 1.17 already restored from a hand-edited file. A 1.17 install that restored a hand-edited or damaged backup holding an odd plate set (such as 7.3 or 80) keeps it, and now exports a backup that its own restore refuses. Only a hand-edited or damaged file restored into 1.17 can lead here: neither app ever logged such a number.

**Findings:**
- **The menu bug is older than plates: it dates from 1.13**, when the ⋮ came in with only "Skip today" in its menu (at least 168 px wide, right edges aligned with the ⋮). That menu was already cut whenever the ⋮ wrapped alone to the left of the card. 1.17 made it much worse: "Count plates on this machine" makes the menu about 266 px wide (the builder's measurement at 306 px, where the menu started at about −43 px).
- **Android can't show it.** Its card head is one row: the name and its pill in a `WrappingPair` that takes the remaining width, then ⓘ and ⋮ last (stacked one over the other at large text), so ⓘ and ⋮ never wrap alone to the left. Its menu is Material's `DropdownMenu`, a popup placed inside the window. Read from the code only; nothing was changed or tested on Android.
- **Two more overflows at 160 px, found by review and by the new test:** a missed set's Failed pill fell into the set number's 20 px column and drew over "@ 10", and a miss's Floor / Mid / Lockout row ran past the screen (the page was 277 px wide). The test's check for text over text and text spilling out of its box missed the pill at first, because the pill's box was 20 px wide and only its text overflowed; it now catches both.
- **The readiness chips were really 31 px wide** in the first narrow layout (reported there as about 33 px): `.today-grow` set `min-width: 0`, which beat the chip's 48 px. In 1.17 at that width they were 12 px. They now keep 48 px.
- **Found by review of the menu fix:** its scroll and focus ran on the click's first render, before the placement was on the page, so a menu that then opened above its button could be cut at the top (seen in the e2e at 844 × 390 and 664 × 390 with the rest bar, before the fix). Also: a change of the visual viewport's width (pinch zoom) closed the menu; the cap took the padding off twice (boxes are border-box), so a capped menu was 18 px short; the side insets were not kept clear; and at 160 px, "Off the shoulders" ran past its point and a folded row's plan and count ran past the screen.
- **The recovery screen's rejection** listed as queued in the 1.17 notes was already fixed on main (`3a46ae4`, merged in `800060e`, both in this release's history).

**Tests:**
- **Domain:** jvm 974, js 1023, 0 failed (unchanged: 1.17.1 has no domain change); `checkWebDomain` passes.
- **Android:** 1108 tests, 1 skipped (the guide frame strips, as before), 0 failed. New: in `BackupTest`, `aPlateSetAbovePlate50_isCorrupt`, `aPlateSetOffTheHalfPlateGrid_isCorrupt`, `aRefusedBackup_changesNothing` (every table and the settings around a refused file, through the check Settings runs before a restore), `theFirstBadPlateSetInFileOrder_namesTheFault` and `aHalfPlateOnAWholePlateOrKgExercise_stillRestores`; in `TodayPlatesViewModelTest`, the four newest-suggestion tests, two kg controls and `aPlateSetOffTheHalfPlateGrid_isNeverStored`. No existing Android test was edited.
- **Web:** vitest 1644 tests in 89 files, 0 failed; `tsc -b` and the build clean. New: the same backup and newest-suggestion tests in `backup.test.ts` and `useTodayPlates.test.tsx`, `menuPlacement.test.ts` (placement and the room's insets) and `Menu.test.tsx` (what closes a menu and what keeps it open, the focus back on its button, the listeners removed, and placing before focusing). One existing backup test was edited: it turned a squat's 140 kg set into a plate set, which is now refused, so it uses plate 7.
- **e2e:** `menu.spec.ts` (26 tests: 306, 320, 360, 390 and 160 px, kg and plates, the start card, the open card and a folded row; a one-word 42-letter name on the open card, and on the folded row and the start card at five widths; a menu taller than its room; closing on a change of width; staying open on a change of height with the weight field focused, then Count plates and Skip today opening their sheets; Plan's ⋮ on a change of height and in landscape; landscape 844 × 390 and 664 × 390 with the rest bar; a 71 px top inset), and in `narrow.spec.ts` 11 new tests under "Today at 320 px and 200 % text" (nothing planned on each side; kg and plates all day on each side, with a missed set and its miss point; every kind of logged set in kg and plates on the powerlifting side and plates on the bodybuilding side; every lift's miss points; the tab bar). These check that the page doesn't scroll sideways, and that no control is cut, overlaps another or text, or reaches past its card, no text overlaps text or spills out of its box, and no word breaks inside itself. The builder saw every new menu test and the narrow tests fail on the code before their fix, except the readiness rows check, added with its fix. `menu.spec.ts` and `narrow.spec.ts` five times each in one worker: 130 and 265 passed.
- **Full run:** Playwright (`npx playwright test --trace off`, e2e and speed): 427 passed, 12 skipped (the owner's guide-video recordings, as before), 0 failed. The mode switch's frame check printed its LATE FRAMES warning once (a warning only). Speed project alone, twice: 6 passed each time, with the LATE FRAMES warning in the second (a warning only). No timing test failed, so none was rerun alone. The web shots: every one written by the screenshot specs is byte-identical to one of the runs made here before the changes, or (Settings) to the committed 1.17.1 shot.
- **Release build:** `./gradlew :app:assembleRelease` gives versionCode 29, versionName 1.17.1 (the APK was not distributed).

**What only reports:** nothing new in 1.17.1. The LATE FRAMES warning of the mode switch's frame check is a warning only, as before.

**Version:** Android 1.17.1 (versionCode 29), web 1.17.1. No database change (Room 6, Dexie 6) and no backup change (version 6); a 1.17 backup restores in 1.17.1 unless it holds a plate set this release refuses. Update both apps together. Settings re-shot on both apps for the version line: six shots on each app, each byte-identical across two runs here, differing from the 1.17 shots only in the version line ("Grit 1.17.1 · AppsbyAdam" on Android, "Grit 1.17.1 · a1b2c3d · AppsbyAdam" on the web, a little wider than before and still on one line).

**Not tested:**
- No real phone. The menu fix has been seen only in Chromium at emulated widths (iPhone 13 emulation, 160 to 390 px).
- Safari and Firefox.
- `:has()`: the narrow layout of a logged set (two or three lines) and of the bar picture (its words under the bar) needs it, Safari 15.4+ or Firefox 121+. Without it those two keep their wide layout at 160 px, and can reach past the screen there. Every other narrow rule works without it.
- Real 200 % text: the e2e halves the viewport to 160 × 284 for a 320 px phone at 200 %.
- Real screen readers, including the tabs' hidden names at 160 px.
- Rotation, the on-screen keyboard and pinch zoom: simulated by resizing the window and firing `resize`, `orientationchange` and visual viewport events. A real keyboard has not been tried. With pinch zoom the menu stays open, but it is placed against the page, not the zoomed-in part, so it is not guaranteed to be inside what is visible while zoomed; and if a browser changes the page width while pinching, the menu closes. The unit tests check that a change of height or a pinch keeps the menu open, not that it is placed again; the browser tests check the placing. The README's web lists now ask for the keyboard and rotation checks on a phone.
- A real notch or status bar: the e2e stands in a 71 px top padding for the installed app's.
- Owner, 2026-11-21, on their own phone with live 1.17.1: 'the three dots worked great'.

**Follow-ups:**
- **The other tabs at 160 px** (a 320 px phone at 200 % text): Progress and Settings scroll sideways; Exercises and Plan clip text. Each needs a pass like Today's.
- **Still open from 1.17:**
  - Which way the assisted machines count.
  - Two flaky tests under load: `SkipTodayViewModelTest.skip_undo_removesSkipAndOpenCard` and `ModeSwitchOverlayTest.framesKeepTime_dropAndPump`.
  - Two number forms in stored target payloads: "8.0" on Android, "8" on the web. Both read either.
  - The three `console.log` lines in `web/e2e/update.spec.ts` are deliberate reporting.
- **Still open from 1.16:** `vibrateOnTaps` resets on an Android restore; the web's real Today shots render differently on this machine; the guide's web shots are not byte-stable between runs; spec point 11 doesn't state WELCOME's and SIDE's least room; the skip ring was not built.

## Done: 1.17.2, the other tabs fit a very narrow screen (web)
1.17.2 is the change from 1.17.1 (`9c9b8a0`) to the commits after it on this branch, plus the version and these notes. The report: `.superpowers/sdd/2026-11-21-1172/1172-report.md` (not committed).

**What the lifter gets:**
- **Coach, Progress, Exercises, Plan and Settings fit a 320 px phone at 200 % text** (web; at 239 px of layout and narrower, about 160 px here), on both sides, as Today has since 1.17.1: the page no longer scrolls sideways, and nothing is cut or overlapping. No button, field, chip or choice is narrower than before, and those that are 48 px on a phone at normal text are 48 px here too; some are shorter than before only because their words no longer break letter by letter, and every one of those is still 48 px or more.
  - The titles are a size smaller and whole ("Exercises" no longer ends in "…"); an eyebrow, a card's tag ("Weak point · Strict press") and a button's words wrap instead.
  - Plan's ⋮ and Settings go under the title; Settings' back arrow sits over its title.
  - **Coach:** a card's buttons are one over the other, the width of the card. An accessory's name has the list's whole width, with its sets × reps and ⓘ under it. A lift's e1RM sits over its trend, a deload's two figures one over the other, and a ratio's name over its rating and figure.
  - **Progress:** the total sits over its trend, a size smaller, and the lifts go one under the other. The chart's range is two choices to a row. The chart keeps its three dates, each on two lines ("18" over "May"; every date of two words takes two lines at this width). A record's e1RM goes under the lift, and each lift's weekly volume sits over its bars. On the bodybuilding side a row's best sets have the row's width, with the trend and the chevron under them, and a body part's rating goes under its name.
  - **Exercises:** the whole tab scrolls, the search and the chips with the list. A row's name, muscles and equipment have the row's width, with its ⓘ under them on the right. A new search or filter no longer takes the list back to its top at this width (the list does not scroll on its own there).
  - **Plan:** a row's sets × reps @ RPE wraps at its spaces and its four controls sit under it, two to a line. Set best sits under its lift's name and best.
  - **Settings:** a switch or a stepper goes under its setting's name and line, on the right; a stepper's value is over its − and +. The rest alerts are one over the other, and the training days go three to a row.
  - A very long one-word name breaks only where it would otherwise run past its card or row.

**The owner's decisions:**
- Fix the other tabs at this width, with Today's standing choices: nothing cut, nothing overlapping, no sideways scroll; text wraps; a control row that cannot fit stacks; touch targets keep their size even at the cost of a row; a header's action drops under its title; screen sides 8 px and card padding 12 px.
- Work faster (2026-11-21): while building, run only the tests for the part being changed; the full suites run once, on merged main (see "How future projects are built").

**For the owner to decide: the chart's dates.** At this width the chart's three dates overlapped. They now take two lines each, every date of two words (call 4), so nothing overlaps or is cut; the other ways of fitting them are under Follow-ups, with shots of each handed back with the report.

**My calls:**
1. **One stylesheet, `web/src/ui/narrowTabs.css`,** every rule inside `@media (max-width: 239px)`, inside the app's screen (`.shell-main`) and leaving Today out (`.screen:not(.today-screen)`), so Today's 1.17.1 rules and shots are as they were and the guide's pictures, drawn outside the app's screen, are untouched.
2. **The titles one size down at this width** (26 px instead of 34 px): "Exercises" and "Settings" at 34 px are wider than the screen, and a single word cannot wrap.
3. **Progress's total one size down** (44 px instead of 64 px): "643.5" at 64 px is wider than the card.
4. **The chart's dates on two lines**, every date of two words (each is as wide as its longest word), so all three dates stay under the plot without overlapping (the row under the plot is 16 px taller). The other ways of making them fit are under Follow-ups.
5. **Progress's four lift tiles one under the other**, each the card's width, rather than two to a row of about 60 px.
6. **The chart range two to a row** (8 wk, 6 mo / 1 yr, All), each a 48 px target; one row gave each about 22 px.
7. **Settings' days three to a row** (Mon Tue Wed / Thu Fri Sat / Sun), each a 48 px chip. Three fit only if the days card gives up its sides (its border is transparent; in forced colours its left and right outline is not drawn at this width).
8. **Settings' rest alerts one over the other**, as Today's miss points: side by side each had about 25 to 50 px.
9. **A stepper's value over its − and +** (− at the left, + at the right): − value + is 141 px or more, and the row has 120.
10. **Exercises scrolls as a whole** at this width: the header, search and chips alone are about as tall as the screen above the tab bar, so the list had no room.
11. **Exercises' ⓘ on a line of its own** under a row's text, on the right: beside the text it leaves the name about 96 px, and "Concentration curl" would break inside a word. It opens the same guide as the row; hiding it at this width is an option (Follow-ups).
12. **Only buttons as wide as their card get tighter sides** and may break a word wider than the card; other buttons keep their width, and their words wrap only at spaces.
13. **Controls under their text go to the right** (Plan's row controls, Set best, a switch, the coach's ⓘ), as Plan's rows already do under 360 px; Plan's header ⋮ and Settings and a card's buttons go to the left, as Today's Finish.
14. **Dialogs are left as they were:** every rule leaves out a dialog opened from these screens (`:not(.gr-dialog *)`), so the exercise picker and Plan's and Settings' dialogs look as in 1.17.1, the same opened from Plan as from Today, until they are made to fit on purpose. (A first version let their buttons' words break, "Replace" and "Change" inside the word.)
15. **Forced colours:** the days card gives up its side borders at this width; in forced colours it draws its outline inside itself instead (`outline-offset: -1px`), so it is outlined on all four sides and the days stay 48 px.
16. **Exercises' pictures at this width** are made only for rows near the screen: a row is measured against the box that really scrolls (the list where it scrolls, as before; else the nearest box round it that does, the screen here). Before this, every one of the 225 pictures was made, though the tiles are hidden under 320 px.
17. **Changes to the checks** (in `web/e2e/narrow.spec.ts`, shared with Today's tests, which stay green): an open menu and a message are drawn over the screen, so each is held to the overlap rule within itself, not against the screen beneath; a row holding an open menu is not "clipped" by it; a word wider than the whole of its card (or list row, or screen) may break; Settings' RPE info button, a 48 px target centred on its 24 px icon, is held to the same rule as a stepper's caption action; and the new tests draw every Exercises row (rows off screen are not drawn and are guessed at 65 px each, which made them seem to overlap).

**What was not built:**
- **The screens one step deeper** were checked once at 160 × 284 (shots handed back with the report) and not fixed. A bottom action bar that stays put over a scrolled screen (check-in, the plan builder, the form guide) is drawn over the content by design, so the overlaps with it found by the check are left out below. What still breaks there:
  - Lift history (squat, strict press): scrolls sideways (246 to 258 px): its logged set rows run past the screen. Its title, eyebrow and chart are fixed by the shared rules.
  - Exercise history (bodybuilding, a plate row and the one-word name): scrolls sideways (202 to 260 px): its set rows run past the screen. Its title is fixed by the shared rules.
  - Check-in (both steps): the muscle list's names spill out of their two columns and overlap the next column's tick box, and the page scrolls sideways (181 px). Its header is fixed by the shared rules.
  - Plan builder: the day chips are cut and overlap each other. Its header is fixed by the shared rules.
  - Templates (both sides): nothing found (the header fault 1.17.1 had there is fixed by the shared rules).
  - Exercise picker (from Plan): row names and muscles break letter by letter or are squeezed to nothing, and the muscle tiles' names spill.
  - Form guide sheet (a catalogue exercise, and an own exercise with the one-word name): the sheet scrolls sideways, Add to today and Add to plan overlap each other and run past it, its "where" line spills, and two lines of steps break inside words.
  - Add to plan's day chooser: nothing found.
  - Muscles sheet: the group names spill out of their two columns over the next column's tick box, the sheet scrolls sideways, and its title breaks inside the word ("Muscle" / "s").
  - Create your own dialog (from the picker): the dialog scrolls sideways, and its title and a field label spill.
  - Plan's add and edit forms: the dialog scrolls sideways (200 px in 112), the steppers run past it and their buttons overlap their labels, and the exercise's name and its eyebrow are squeezed to a letter a line.
  - Set best dialog: the field label spills.
  - Replace all data dialog, RPE sheet: nothing found.
- The rest bar is shown on Today only (`App.tsx`), so it never shows over these tabs: there is no such state to fix.
- Nothing on a deeper screen got worse by the check's count, kind by kind, apart from the action bars named above.

**Findings:**
- The probe before the fix (1.17.1, 160 × 284; counted before the checks were refined as in call 17, so some Exercises and menu counts include what those refinements leave out) found faults in every full state of the five screens: Coach (75 faults with every card on the powerlifting side): the page 330 px wide (458 px on the bodybuilding side, where the one-word name is in a Swap button), button rows overlapping (Apply and Dismiss), tags and accessories' names cut or squeezed to nothing, a stat note cut; Progress (75): the page 257 px wide (168 px on the bodybuilding side), the tiles' figures past the screen, the range choices 22 px wide and cut, the chart's dates overlapping, records and volume rows past the card; Exercises (up to 253): names and muscles broken letter by letter or cut, a heading cut, the list with no room; Plan (13 to 31): the page 163 to 171 px wide, the title cut to "P…", row text and Known bests squeezed to nothing; Settings (up to 218): the page 213 px wide, steppers and rest alerts past the screen, days 17 px wide and overlapping, the title cut. Coach and Progress with nothing in them were already clean.
- The 1.17.1 note that Exercises had "text squeezed to 0 px" was partly the check: rows off screen are not drawn (`content-visibility: auto`) and are measured at a guessed 65 px.
- "Create your own" is in the exercise picker only, not on the Exercises tab.
- No touch target on the five screens is smaller at 160 px than in 1.17.1 below 48 px; many are larger (the days 17 → 48 px wide, the range choices 22 → 55 px, the rest alerts 25–52 → 110 px, the lift tiles 43 → 118 px). Rows that 1.17.1 made very tall by breaking their text letter by letter are shorter, and still 48 px or more. The tabs keep 1.17.1's 32 × 80 px (that release's call 4).

**Tests:**
- New in `web/e2e/narrow.spec.ts`, 18 tests under "the other tabs at 320 px and 200 % text". Each state is checked on the whole page with nothing left out.
  - On each side: every tab with nothing in it; Coach with every card the side shows (powerlifting: welcome back, deload, stall with long and one-word names, two weak points with the ratio and the sticking point, easing back in, skipped; bodybuilding: welcome back, check-in due, more work by sets and by a new exercise, stuck with its swaps, skipped, deload, easing back in), then its message after Apply deload; Plan with long names, every row's ⋮ open, each menu also held wholly inside the screen above the tab bar; Exercises with own exercises, search text, no match, every body area, a piece of equipment and the end of the list; Exercises with two muscles chosen; Settings with every row on a phone that can vibrate, changed, after a backup and after a restore with its safety copy; the touch targets.
  - Powerlifting side only: Progress with history and strict press in every range (the bodybuilding side has no chart or ranges), with a record from last year; Plan's own ⋮ and Known bests.
  - Bodybuilding side only: Progress with check-ins, its body parts and a plate row.
  - Not tied to a side: Settings in forced colours (the days card outlined all round, the days 48 px), and Exercises making only the pictures of rows near the screen.
  - **The touch-target tests compare 160 px against 390 px on the same build** (each target at least as large as at 390 px, up to 48 px each way), not against 1.17.1. The comparison with 1.17.1 (Findings) was a one-off run against the saved 1.17.1 build.
  - The builder saw them fail before their fix: the narrow-tab tests on 1.17.1 (the touch-target tests against the saved 1.17.1 build), the forced-colours test without its rule, the pictures test before its fix (225 pictures made). Run once more at 239 px (a 306 px phone at 130 % zoom is 235 px) from a copy, not committed: all passed.
- New in `ExercisePicture.test.tsx`: a list that does not scroll is measured from the box round it that does (seen failing first).
- Run while building, last on the final code: `narrow.spec.ts` (71 tests), `menu.spec.ts` (26), `pictures.spec.ts`, `exercises.spec.ts` and `library.spec.ts` (Exercises at 187 px) together, 117 passed; the speed project once (6 passed; the mode switch's LATE FRAMES warning printed, a warning only), as the pictures fix touches the list it times; the vitest files for what changed (`App*.test.tsx`, `appVersion.test.ts`, `manifest.test.ts`, `cards.test.ts`, 59 passed; `src/screens/library/`, 98 passed); `npx tsc -b` and `npm run build` clean; `:app:compileDebugKotlin` passes; the Android Settings screenshot test, twice.
- **Shots:** the 13 screenshot specs were run twice before the change and once after (178 shots each run). After, 169 match one of the two runs before byte for byte; the six Settings shots differ in the version line only and match the re-shot committed ones; `exercise-guide-actions-light` and `guide-side-powerlifting-dark` matched a run before once their specs were rerun; `form-guide-light` differs in about 176 pixels of anti-aliasing on the figure's edges (390 px wide, where no rule of this release applies), in the run after and its rerun alike, and the committed shot differs from both runs before as well. No tracked shot was rewritten. Today's 160 px shots (six states on each side, the screen and the whole screen) are byte-identical to 1.17.1's.
- **The full suites had not run when this was written:** Gradle, web unit, the full Playwright run and the speed project run once on merged main, after the owner approves.

**Version:** Android 1.17.2 (versionCode 30), web 1.17.2. Android changes nothing but the version. No database or backup change. Settings re-shot on both apps for the version line: six shots on each, each byte-identical across two runs, differing from 1.17.1's in the version line only.

**Not tested:**
- No real phone: only Chromium at an emulated 160 × 284.
- Safari and Firefox.
- **The full suites had not run when these notes were written:** Gradle, web unit, the full Playwright run and the speed project ×3 run once on merged main.
- `:has()` is not used by this release's rules; `:not(.gr-dialog *)` (a complex selector inside `:not()`) needs Safari 9+, Chrome 88+ or Firefox 84+; `:is()` needs Safari 14+, Chrome 88+ or Firefox 78+.
- Forced colours: only Chromium's emulation.
- Real 200 % text: the e2e halves the viewport to 160 × 284 for a 320 px phone at 200 %.
- Real screen readers.

**Follow-ups:**
- **The chart's dates (for the owner):** two lines each is built (A). Other ways: only the first and last date at this width (B); the dates in a smaller type on one line (C: at 9 px "18 May" and "10 Aug" still touch); or the y labels inside the plot, giving the dates its full width (not tried). Shots of A, B and C are handed back with the report.
- **Done in 1.17.3:** ~~Exercises at this width does not take the list back to its top on a new search or filter~~ (`ExerciseBrowser` resets the list's own scroll, which no longer scrolls here). The search and chips are at the top of the screen, so a new search usually starts there anyway.
- **Exercises' ⓘ** takes a line of its own in every row at this width. It opens the same guide as the row, so it could be hidden at this width (screen readers keep the row's own button).
- **Done in 1.17.3:** ~~The deeper screens (list above): each needs a pass like this one. Their dialogs are left exactly as in 1.17.1 by this release.~~
- **239 to 240 px:** a layout width between them (a fractional zoom) gets neither these rules nor the wide ones' room, as with Today's rules and the tab bar's since 1.17.1.
- **Still open from 1.17.1:** which way the assisted machines count; two flaky Android tests under load; two number forms in stored target payloads; the guide's web shots not byte-stable between runs (seen again here: four guide shots differ between two runs on the same code).

## Done: 1.17.3, the deeper screens and dialogs fit a very narrow screen (web)
1.17.3 is the change from 1.17.2 (`7d99e74`) to the commits after it on this branch, plus the version and these notes. The report: `.superpowers/sdd/2026-11-21-1173/1173-report.md` (not committed).

**What the lifter gets:**
- **The screens one step deeper and the dialogs and sheets fit a 320 px phone at 200 % text** (web; at 239 px of layout and narrower, about 160 px here), as Today and the tabs have since 1.17.1 and 1.17.2: nothing scrolls sideways, and nothing is cut or overlapping. No button, field, chip or tick box is smaller than on a phone at normal text, up to 48 px.
  - **Lift history and exercise history:** a logged set reads on two lines, as on Today: "115 × 8", then "@ 8" with its mark. Top set and PR go under it, one over the other where the two do not fit side by side, and Failed after them. A long plate set ("plate 12½ × 12") breaks once, after the weight.
  - **Check-in:** the muscles are one to a row, each name whole beside its tick box.
  - **Plan builder:** the days go three to a row (Mon Tue Wed / Thu Fri Sat / Sun), each a full-size target. The exercises step scrolls as a whole, the question, search and chips with the list, and each row shows the whole name beside its tick box. The body parts are one to a row. In the draft, a body part's name has the row's width, with "Under 10" and its sets under it. Back and Next (or Use this plan) share the bar at the foot, and a label wraps at its spaces. "Use this plan?" fits.
  - **The exercise picker:** Close sits over the title, and the title is a size smaller and whole. A row's name, muscles and equipment have the row's width, with its ⓘ under them on the right. The muscles to browse are one to a row.
  - **The form guide:** Close sits at the top right, with the name and its "where" line under it. At this width they scroll with the guide instead of staying at the top. Add to today and Add to plan share the bar at the foot, each half of it, with their words on two lines. The safety line's mark sits over its words.
  - **The Muscles sheet:** laid out the same way: Close, then "Muscles" whole, then the muscles one to a row.
  - **Create your own, Plan's add and edit forms, Set best:** the dialog uses more of the screen (8 px round it, 12 px inside). Its buttons are one over the other, the dialog's width. A field's label wraps. In Plan's form the exercise's name has the dialog's width, with Change under it, and each stepper sits under its label, its number over − and +.
  - **The same wherever they open, Today included:** at this width the picker, the form guide, Create your own and the Muscles sheet look the same whether they are opened from Today, Plan, Exercises or the builder. Before, the picker and Create your own opened from Today took a little of Today's own narrow layout, and so looked slightly different from the same dialogs opened from Plan.
  - **A new search or filter on the Exercises tab goes back to the top of the list again** at this width. The screen scrolls back to the search field and no further, so the field you are typing in stays on screen and the list starts under it. The builder's exercises step does the same.
  - Unchanged: Templates, Add to plan's day chooser, Replace all data, the RPE sheet, the plate sheet, the skip sheet and the typo check look exactly as before at this width, and nothing changes at 240 px and wider.

**The owner's decisions:**
- Fix the deeper screens and dialogs that 1.17.2 listed as not fixed, and the Exercises search's return to the top, with the standing choices for this width: nothing cut, nothing overlapping, no sideways scroll; text wraps; a row that cannot fit stacks; touch targets keep their size; a header's action drops under its title; 8 px sides and 12 px card padding; a logged set on two lines, as on Today.
- A dialog looks the same at this width wherever it is opened, Today included. The dialogs that already fit stay exactly as they are.
- Work faster (2026-11-21): targeted tests while building; the full suites run once on merged main.

**For the owner to decide:**
- **The form guide's and the Muscles sheet's head at this width** (call 9). Built (A): the head scrolls away with the guide, so the guide gets the sheet's whole height; Close is at the top, and a tap above the sheet or Escape closes it too. The other way (B): the head stays at the top as at other widths, which takes about half of the sheet's 250 px (Close, then a name of up to three lines). Shots of A and B are handed back with the report.
- **The builder's exercises step after a search** (call 12). The field stays on screen, and the list starts just under the chips, a short swipe away. Back and Next and the tab bar leave about 130 px, so the first result cannot be on screen with the field. Other ways: hide the chips while typing at this width, or let Back and Next scroll away on this step.
- **A failed save's message over a small dialog.** At this height, while "Couldn't save — try again." is up (4 s), it is drawn over the foot of Create your own and of Plan's add form, Cancel and Create or Add among it. Once the message goes, both buttons can be tapped. Left as it is (A). Other ways: (B) at this width, show the message at the top of the screen; (C) give the dialog room at its foot while a message is up.

**Decided (owner, 2026-11-22, "Approved"):** A stands on all three, as built. The approval request said that approving accepts A on each.

**Released 2026-11-22.** The full suites ran once, on merged main, and passed: Gradle, 1,650 web unit tests, 465 browser tests (12 skipped), and the speed project 6 of 6 three times.

**Follow-ups from the review (not in 1.17.3):**
- No test pins the dialogs that must stay unchanged at 160 px (Templates, the day chooser, Replace all data, the RPE, plate and skip sheets, the typo check). They were compared by screenshot only.
- No test covers Escape or a tap above the sheet closing the form guide and the Muscles sheet at 160 px. Both were checked by hand.
- `menu.spec.ts:132` ("⋮ menu at 360 px, kg") failed once during the review while other work was running: ⓘ and ⋮ were 12 px apart. Alone it passed 100 of 100 across its widths, and it passed in the full run on merged main.

**What was done about them (tests only, no app or CSS change):**
- **The unchanged dialogs:** a new spec, `web/e2e/narrowUnchanged.spec.ts`, runs at 160 × 284. It reads every selector from `narrowDeeper.css` on disk and opens each of these dialogs from its real opener: Templates' swap sheet and "Replace your plan?", the builder's swap sheet, the day chooser, Replace all data, the RPE sheet from Today and from Plan's form, the plate sheet, the skip sheet, the typo check, and the ⋮ menus on Today and Plan. No selector may match the dialog's scrim, anything inside it, or anything round it up to the page itself. The ancestor check catches a leak through an inherited property, such as `font-size` on `.shell-main`. For a menu, the check covers the ⋮ button and its list. The test stops if the file's shape changes. That shape is one `@media (max-width: 239px)` block, holding only plain rules and `@media (forced-colors: active)` blocks. Set best shows the check does find a match. To prove it fails, temporary rules were added and removed: `.gr-dialog .gr-btn`, `.shell-main { font-size }` and `body { font-size }` each made it fail. Limits: it checks this one file only, and it does not see effects that reach a dialog other than through the dialog or an element round it.
- **Escape and a tap above the form guide and the Muscles sheet:** the same spec, at 160 × 284, covers both sheets, at their top and scrolled until the head is out of view. Escape closes each, and so does a tap halfway down the strip above it. The tap point is worked out from the sheet's measured top, and the test checks the point is on the page behind the sheet. Focus then goes back to the opener. The strip above the sheet is 32 px for both sheets at 160 × 284. At 390 × 844 it is 32 px for the form guide and 312 px for the Muscles sheet. **For the owner:** at 160 px the strip is under 48 px for both sheets, and the Muscles sheet's strip is smaller than at 390 px. Nothing was changed.
- **`menu.spec.ts:132`:** reproduced under load (beside `narrow` and `narrowDeeper`, with extra CPU load). It failed 3 times in 30 runs, with ⓘ and ⋮ 2 to 34 px apart. The cause is in the test's measuring, not the app. After Start session, Today smooth-scrolls the open card into view for about a quarter of a second. Smooth scrolling shows up neither as an animation nor as a font load. The three box reads went out one by one, and under load a frame fell between them. In a recorded failure at 360 px, ⓘ was read at y 44 and ⋮ at y 60, yet on every frame the two sat on one line. The test now reads all its boxes in one go, once the fonts are in and the boxes are the same on two frames running. The two similar reads in the same file were changed the same way. The assertions are unchanged. Under the same load, `menu.spec.ts` with `--repeat-each=5` passed 130 of 130.

**My calls:**
1. **One new stylesheet, `web/src/ui/narrowDeeper.css`,** every rule inside `@media (max-width: 239px)`. The screen rules sit inside the app's screen (`.shell-main`) and leave out a dialog drawn inside the screen (`:not(.gr-dialog *)`). The dialog rules are scoped to the dialog's own class. `Dialog` takes an optional `className`, which goes on the panel and, with "-scrim", on the scrim. A dialog given none keeps exactly its classes, so the dialogs that already fit are untouched. Only these take a class: the picker (`lib-picker`), the form guide (`lib-guide-sheet`), the Muscles sheet (`lib-muscles-sheet`), Create your own (`lib-create-dialog`), Plan's add and edit form (`plan-item-dialog`), Set best (`plan-best-dialog`) and "Use this plan?" (`pb-confirm-dialog`). The builder gets `pb-builder`, because Templates shares its `pb-screen` class and must not change.
2. **Today's rules inside a dialog:** Today's narrow rules (its eyebrows and button labels wrap) reach into the picker and Create your own, because both are drawn inside Today. Rather than touch `today.css`, the dialogs' own rules state the same thing for every opener, more specifically. The picker and Create your own now look the same from Today and from Plan, and a test compares the two.
3. **History's set rows** reuse Today's two-line layout under the history card's own class (`.history-session`), not Today's rule. The difference: Top set and PR go under the effort (one over the other where the two do not fit side by side), and Failed after them, so a failed top set or a top set that is a record never puts one pill over another (Today's rule puts both on the same line; Today's sets never carry a PR). A set with no RPE has its mark alone on the second line, as on Today.
4. **The muscle lists one to a row** (check-in, builder body parts, Muscles sheet), each a full 48 px row. Two to a row left each name about 24 px.
5. **The picker's muscle tiles one to a row.** Three to a row left each tile 37 px wide (114 px on a normal phone) and names such as "Hamstrings" spilled. Two to a row would still be too narrow for "Hamstrings" without breaking it.
6. **The builder's days three to a row,** as Settings' in 1.17.2: the card gives up its side borders so three 48 px chips fit, and in forced colours it draws its outline inside itself.
7. **The builder's exercises step scrolls as a whole,** as the Exercises tab does at this width: the question, search and chips left the list no room. Back and Next stay at the foot.
8. **Bottom bars keep their buttons side by side, each half the bar, with their words wrapped** (builder, check-in, the form guide's Add to today and Add to plan). Stacked, the bar would be about 120 px of a screen that has about 200 above the tab bar. Each button is still 48 px or more each way.
9. **The form guide's and the Muscles sheet's head scroll with the sheet at this width** (they stay at the top at every other width). With Close on its own line and a name that can take three lines, a head that stays put would take about half of the sheet's 250 px. Close is at the top of the sheet; a tap above the sheet, or Escape, also closes it.
10. **The small dialogs' buttons one over the other,** in their order (Cancel, then the action), each the dialog's width, as a card's buttons on Coach (1.17.2). The dialog keeps 8 px from the screen's edges and 12 px inside, instead of 24 and 24, which had left 64 px for its content.
11. **Plan's steppers as Settings':** the label on its own line, the number over − and +. "− 3 +" is 168 px and the dialog has 112.
12. **Search back to the top (`listScroll.ts`).** At every width but this one, nothing changes: the list's own box goes back to its top, as before, and no style or layout is read beyond that (a cached media query decides). At this width, where the list does not scroll on its own (the Exercises tab, the builder), the screen scrolls back at most to where the search field's top meets the screen's top. It never scrolls past the field, and a screen scrolled less than that stays where it is. "The top" here means the field fully on screen with the list starting under it. On the Exercises tab the first row is then on screen. In the builder, Back and Next and the tab bar leave about 130 px, which the field and the chips fill, so the first row starts just under them, a short swipe away. Opening the list moves nothing. The picker's list still scrolls on its own at this width, so it goes back to its own top.
13. **"Use this plan?" is fixed too** (its line spilled). It is the builder's own dialog and was not on 1.17.2's list.
14. **A very long one-word name breaks** only where it is wider than the whole of its dialog or row, as before.

**What was not built:**
- No change to Today, to the tabs' 1.17.2 rules, to the guide or to the gate screens. The dialogs that already fit (Templates, the day chooser, Replace all data, the RPE, plate and skip sheets, the typo check) are byte-identical in shots at 160 × 284 before and after.
- Lift history has no chart or range (that is on Progress), so there was none to fix.
- Create your own and Set best have no error line under a field: Create stays off until the form is complete, and Set best's Save stays off for an impossible number. Both were tested in that state, as were their messages: "Couldn't save — try again." over Create your own (from the picker and from Change), over Plan's add form once Change's picker has closed, and on Plan after a best that could not be saved. See "For the owner to decide" for the message over a small dialog.
- The check-in and the builder are the bodybuilding side's own (on the powerlifting side their pages lead back to Coach and Plan), so they were tested on that side only.

**Findings:**
- The probe before the fix (1.17.2 at 160 × 284; the counts are from the new checks): lift and exercise history scrolled sideways in every state (217 to 311 px), with 7 to 51 parts past the screen. The check-in scrolled sideways (181 px) and 18 names spilled. In the builder: the days overlapped (11), and the exercises step had up to 182 names squeezed to nothing, 128 words broken and items under the bar. The body parts scrolled sideways (201 px). The draft scrolled sideways (192 px), with Use this plan off the screen. "Use this plan?" had its line spilling. The picker, from Today and from Plan alike, had up to 173 broken words, 12 squeezed names and 22 spills, its title spilling, and its list scrolling sideways (162 to 168 px). From Plan, Create your own was past the screen. Create your own scrolled sideways (115 px in 112). Plan's forms scrolled sideways (200 px in 112), with up to 30 parts outside and 7 overlaps. Set best's label spilled. The form guide scrolled sideways (184 px), its "where" line and safety line spilled, Add to today and Add to plan overlapped and ran past it, and Add to plan could not be tapped. The Muscles sheet scrolled sideways (181 px) with 19 spills. Touch targets: the builder's days were 20 px wide (50 at 390 px), the picker's muscle tiles 37 px (114), and Create your own's two fields 38 px (268). The swap sheet, the day chooser and the check-in's last step were clean.
- After the fix, every state above passes with nothing left out by name.
- In the probe on 1.17.2, the picker and Create your own opened from Today already differed from the same dialogs opened from Plan, because Today's narrow rules reached into them (their eyebrows and button labels wrapped). They now match.
- The builder's `pb-screen` class is shared with Templates, hence `pb-builder` (call 1).
- The guide and the gate screens use none of the targeted classes. The guide's pictures draw Today, Coach, Progress, Plan and Templates outside the app's screen, and no dialog. `olderCopy` and the lock and storage dialogs use `Dialog` without a class. The guide's tests open no dialog at 239 px or narrower. `pictures.spec.ts`'s large-text test opens the form guide at 187 px, where these rules apply: its captions still wrap, the panels are still two side by side, and it passes unchanged.

**Tests:**
- New: `web/e2e/narrowChecks.ts`, the checks and stories (not a spec). `wrongHere` checks whatever is on top, the open dialog or else the screen: sideways scroll of the page, of a box in it or of the dialog; parts past the screen or the dialog; text spilling or squeezed to nothing; overlaps; controls past their card; and words broken inside themselves. A bar that stays put is a layer of its own, and `clearOfBars` checks that nothing has to stay under one at either end of its scroll. `reachable` scrolls to a control and checks that it is wholly on screen and that nothing is drawn over its middle. `keepsTargets` compares each target at 160 × 284 with 390 × 844, up to 48 px. Nothing is left out by name beyond what 1.17.2's checks already left out: a chip row may scroll sideways, the RPE info button is a 48 px target laid over its line by design, and an open menu and a message are each checked within themselves. A box scrolled out of its list is not counted as drawn.
- New: `web/e2e/narrowDeeper.spec.ts`, 20 tests. Lift history (four lifts, with records, failed sets with their miss points, no RPE and bodyweight). Exercise history (bodybuilding side: plate sets with "plate 12½ × 12", the one-word and long names). The check-in (each step, some ticked). The plan builder (every step, its Muscles sheet, the swap sheet and "Use this plan?"). The picker from Today and from Plan on both sides (recent exercises, every body area, every muscle, equipment, searches, no match, its end) and Create your own (the one-word name, a variation without its lift). Plan's add and edit forms on both sides (the one-word and long names, Change's picker, every stepper reachable). Set best, with Plan after a save and after a failed save. Create your own and Plan's add form after a failed save. The form guide from the Exercises tab (no session; a session open; the exercise already added; the day chooser), from Today's ⓘ and from the picker, with and without pictures, on both sides. The Muscles sheet on both sides. The touch targets (exercise history, the check-in, every step of the builder, the picker, the Muscles sheet, the form guide with a session open, Plan's add form, Create your own). The picker and Create your own the same from Today as from Plan. The search back to the top on the Exercises tab and in the builder at 160 px, and unchanged at 390 px.
- New unit tests: `listScroll.test.ts` (4) and, in `components.test.tsx`, the dialog's class on its panel and scrim, and none without one.
- Seen failing first on the 1.17.2 build: every narrow state above (16 of the 16 probe tests, every one with at least one faulty state; the swap sheet, the day chooser and the check-in's last step were already clean); the search tests at 160 px (the 390 px one passes on both, as it should); and the same-from-Today test. The dialog's class test was seen failing before its code. `listScroll.test.ts` was seen failing only because its module was not there yet; the behaviour it checks was seen failing in the e2e search test.
- Run while building, last on the final code: `narrowDeeper.spec.ts` (20 passed); `narrow.spec.ts`, `menu.spec.ts`, `pictures.spec.ts` (its large-text caption test opens the form guide at 187 px), `exercises.spec.ts` and `library.spec.ts` with it (136 passed; the one failure there, the builder's search test, was the test's own assertion, changed to call 12 and then passing); `bodybuilding.spec.ts` (check-in and builder), `templates.spec.ts`, `plan.spec.ts` and `progress.spec.ts` (62 passed); the 13 screenshot specs (267 passed, 12 skipped, as before); the vitest files for what changed (`src/screens/library`, `src/screens/plan`, `src/ui/components`, `src/App*`: 22 files, 337 passed; `appVersion.test.ts` with `App*`: 39 passed); `npx tsc -b` and `npm run build` clean; `:app:compileDebugKotlin` passes; the speed project once (6 passed; the mode switch's SLOW KIND warning printed, a warning only).
- **Shots:** the 13 screenshot specs were run twice on the 1.17.2 build and once after (183 shots each run). After, 166 match a run before byte for byte; the six Settings shots differ in the version line only and match the re-shot committed ones. Eleven differed the first time and were rerun once: nine then matched. Two still differ: `guide-side-first-launch-powerlifting-dark` (153 pixels in the animated side pictures, at 390 px, where no rule of this release applies) and `mode-switch-drop-impact-light` (its two runs before differ from each other too). No other tracked shot was rewritten.
- **Dialogs that must not change:** Templates, the day chooser, Replace all data, the RPE sheet, the plate sheet, the skip sheet and the typo check, shot at 160 × 284 on 1.17.2 and after, each twice: all 14 shots are byte-identical before and after. The RPE sheet opened from Plan's add form is identical too, but for the strip above the sheet where Plan's form, now laid out for this width, shows dimmed behind it.
- **Touch targets:** in the screens and dialogs of this release, none is smaller at 160 px than at 390 px, up to 48 px. On 1.17.2 the builder's days were 20 px wide (50 px at 390), the picker's muscle tiles 37 px wide (114 px at 390) and Create your own's fields 38 px wide (268 px at 390); now the days are 48 px wide, the tiles 144 px and the fields the dialog's width.
- No existing test was edited.
- **The full suites had not run when this was written:** Gradle, web unit, the full Playwright run and the speed project ×3 run once on merged main, after the owner approves.

**Version:** Android 1.17.3 (versionCode 31), web 1.17.3. Android changes nothing but the version. No database or backup change. Settings re-shot on both apps for the version line: six shots on each, each byte-identical across two runs, differing from 1.17.2's in the version line only.

**Not tested:**
- No real phone: only Chromium at an emulated 160 × 284.
- Safari and Firefox. The rules use `:is()` (Safari 14+, Chrome 88+, Firefox 78+) and `:not(.gr-dialog *)` (Safari 9+, Chrome 88+, Firefox 84+); no `:has()`.
- The on-screen keyboard: with it up, the room left above it is smaller than 284 px, and the search's "back to the field" was checked without it.
- Real 200 % text, real screen readers, forced colours beyond Chromium's emulation.
- **The full suites had not run when these notes were written.**

**Follow-ups:**
- **The builder's exercises step at this width** has room for the search field and the chips only, so a search's first result starts just under them. Hiding the chips while typing, or letting Back and Next scroll away on this step, would show it.
- **The form guide at this width** shows little of the guide at a time: the head scrolls away, but Add to today and Add to plan keep about 80 px at the foot.
- The other generic dialogs at this width (Replace all data, the typo check, the lock and storage dialogs) still have only 64 px inside. They fit their words today, but a longer line would not.
- **Still open from 1.17.2:** the chart's dates (for the owner), the Exercises tab's ⓘ on its own line, the 239 to 240 px gap. **From 1.17.1:** which way the assisted machines count; two flaky Android tests under load; two number forms in stored target payloads; the guide's web shots not byte-stable between runs.

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
- Run the full Android, domain, web and e2e suites once per task, before its commit, and once on main before pushing. (Replaced on 2026-11-21 by the owner's line below.)
- Owner, 2026-11-21: to work faster, the full suites (Gradle, web unit, full Playwright, speed ×3) run once, on merged main, before the push; while building, only the tests for the part being changed (the spec for the screen or part changed, the unit tests for the files touched, and `npx tsc -b`); reviews stay; when a feature is for both apps, build one app first and port it once the owner has used it.

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
