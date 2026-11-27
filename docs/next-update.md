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
- **The unchanged dialogs:** a new spec, `web/e2e/narrowUnchanged.spec.ts`, runs at 160 × 284. It reads every selector from `narrowDeeper.css` on disk and opens each of these dialogs from its real opener: Templates' swap sheet and "Replace your plan?", the builder's swap sheet, the day chooser, Replace all data, the RPE sheet from Today and from Plan's form, the plate sheet, the skip sheet, the typo check, and the ⋮ menus on Today and Plan. No selector may match the dialog's scrim, anything inside it, or anything round it up to the page itself. The ancestor check catches a leak through an inherited property, such as `font-size` on `.shell-main`. For a menu, the check covers the ⋮ button and its list. The test stops if the file's shape changes. That shape is one `@media (max-width: 239px)` block, holding only plain rules and `@media (forced-colors: active)` blocks. Set best shows the check does find a match. To prove it fails, temporary rules were added and removed: `.gr-dialog .gr-btn`, `.shell-main { font-size }` and `body { font-size }` each made it fail. Limits:
  - It checks this one file only, and it does not see effects that reach a dialog other than through the dialog or an element round it.
  - The check on what is round a dialog fails on any future rule in this file whose selector matches `.shell-main`, `#root`, `body` or `html`, even one that cannot change a dialog (`.shell-main { padding: 0 }` fails 6 of the spec's 20 tests: the dialogs and menus drawn inside the screen). That is the price of catching inherited leaks. If it happens, scope the rule to what it lays out, or extend the check on purpose.
  - A selector is checked on a page at rest. `:hover`, `:active`, `:focus` and `:focus-visible` are dropped before matching, so a rule for them counts as always on. Proof: `.gr-dialog .gr-btn:hover` failed 8 tests and `.gr-dialog button:focus-visible` failed 10. A dropped state inside `:not(…)` would turn the meaning round, so the test stops with a message that the check needs extending (shown with `.gr-dialog .gr-btn:not(:hover)`). States that `matches()` sees for real (`:focus-within`, `:checked`, `:disabled`) are kept, so a rule for one of them fails only if that state is on while the dialog is checked.
  - Pinned as well: the bodyweight entry, a logged set's "Set 1", "Set up App lock again?", and the "This backup has no … plan" dialog. Not pinned: "Storage is full", which needs a full storage quota to open. Every one of these is a plain dialog with no class of its own.
  - `postcss` comes with Vite; it is not listed in `package.json`.
- **Escape and a tap above the form guide and the Muscles sheet:** the same spec, at 160 × 284, covers both sheets, at their top and scrolled until the head is out of view. Escape closes each, and so does a tap halfway down the strip above it. The tap point is worked out from the sheet's measured top, and the test checks the point is on the page behind the sheet. Focus then goes back to the opener. The strip above the sheet is 32 px for both sheets at 160 × 284. At 390 × 844 it is 32 px for the form guide and 312 px for the Muscles sheet. **For the owner:** at 160 px the strip is under 48 px for both sheets, and the Muscles sheet's strip is smaller than at 390 px. Nothing was changed. **Reading it (planner, 2026-11-22):** the form guide's strip is the same 32 px at 390 px, so nothing shrank there; the Muscles sheet loses its strip only because the sheet fills the short screen. The 160 px layout stands for a page zoomed to 200 %, so 32 px there is as tall on the phone as 64 px at normal size. What is new at this width is that Close scrolls away with the content (the owner's decision 1 on 1.17.3); scrolling back up brings it back. Recommended to the owner: leave it. The other choice, keeping Close pinned at this width, costs about a row of a 252 px sheet and would be an app change for a later release. **Decided (owner, 2026-11-22, "leave the strip, your pick"):** the strip stays as it is.
  - **Found by the check, not from 1.17.3 and at every width:** with a sheet or dialog open, the browser's Back leaves the screen (Exercises to Today) and the sheet goes with it; `Dialog` adds no history entry. Seen in Chromium with emulated touch only. **Built in 1.17.4 for Android, iPhone and iPad, and tested in Chromium with each phone's user agent:** Back there is meant to close the open dialog, sheet or ⋮ menu and leave the screen where it is; on Android through `CloseWatcher`, on iPhone and iPad through a hidden history step kept while something is open. The owner tried the preview of commit `5ec5eab` on their iPhone and their Android phone on 2026-11-22 and reported "works on both phones" (the code released is the same; only these notes differ). Nothing else has run on a real phone or in Safari. Still open: the toolbar Back on a desktop browser, which still leaves the screen.
- **`menu.spec.ts:132`:** reproduced under load (beside `narrow` and `narrowDeeper`, with extra CPU load). It failed 3 times in 30 runs, with ⓘ and ⋮ 2 to 34 px apart. The cause is in the test's measuring, not the app. After Start session, Today smooth-scrolls the open card into view for about a quarter of a second. Smooth scrolling shows up neither as an animation nor as a font load. The three box reads went out one by one, and under load a frame fell between them. In a recorded failure at 360 px, ⓘ was read at y 44 and ⋮ at y 60, yet on every frame the two sat on one line. The test now reads all its boxes in one go, once the fonts are in and the boxes are the same on two frames running. The two similar reads in the same file were changed the same way. An element that is there but not drawn now fails the read, naming it, as `boundingBox()`'s null did before. The assertions are unchanged. Under the same load, `menu.spec.ts` with `--repeat-each=5` passed 130 of 130.

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

## Done: 1.17.4, Back on a phone closes the open dialog, sheet or menu (web: Android, iPhone and iPad)
1.17.4 is the change from 1.17.3 to the commits after it on this branch, plus the version and these notes. Web only: the Android app already does this itself and gets only the version. Built in two parts: Android first (`CloseWatcher`), then iPhone and iPad (a history entry of the app's own), in the same release.

**What the lifter should get** (built and tested in Chromium with each phone's user agent; the only real-phone evidence is the owner's try of the preview on their iPhone and Android phone, 2026-11-22, see the owner's decisions below):
- **On a phone, Back is meant to close what is open and leave the screen where it is.** With a dialog, a sheet or a ⋮ menu open, Back closes it, the same as Escape or a tap outside does, and you stay on the screen you were on. Before, Back left the screen (Exercises to Today, say) and took the sheet with it.
  - **Android:** the phone's Back, button or edge swipe.
  - **iPhone and iPad:** the swipe back from the left edge in Safari, and Back in the app opened from the Home Screen icon.
- **One at a time, the newest first.** With the form guide open over the picker, Back closes the guide and leaves the picker. With Plan's edit form, the picker over it and a muscle open in the picker, Back steps back from the muscle first, then closes the picker, then the form, as the Android app does. Only when nothing is open does Back leave the screen, and it then goes where it always went.
- A closed menu gives the focus back to its ⋮, and a closed dialog to what opened it, as Escape does.
- **Escape on a desktop works as before.** Checked in Chromium only: any other desktop browser that has `CloseWatcher` takes the Android path, untested.

**Where it works:**
- **Android: built, through `CloseWatcher`, and tested in Chromium with Chrome on Android's user agent.** Meant for Chrome on Android in the browser and installed; the installed case rests on a reading of Chromium's source given in the brief, not on a device test, and was not read again here. Firefox for Android 149+ and Samsung Internet 28+ ship the same API and should behave the same; not confirmed. On a real Android phone: the owner tried the preview and reported it works (2026-11-22); which browser, and whether installed, was not said.
- **iPhone and iPad: built, through a hidden history step kept while something is open (call 6), and tested in Chromium with Safari's iPhone user agent.** Meant for Safari and the installed app. On a real iPhone: the owner tried the preview and reported it works (2026-11-22); whether in Safari, installed or both, and how the swipe looked, was not said. Not tried on an iPad. This path wins over `CloseWatcher` on these devices, even once Safari ships it, so nothing there depends on whether an edge swipe ever counts as a close request.
- **An Android browser without `CloseWatcher`** (an older Chrome, or another browser without the API) behaves as before this release: Escape works as before and Back leaves the screen.
- **Unchanged:** the toolbar Back on a desktop browser (not a close request, and the history path runs on iPhone and iPad only), which still leaves the screen with a dialog open.
- **Not verifiable here:** a real Android Back press, and a real iPhone swipe. The tests stand in for both: Chromium with `CloseWatcher` for Android, and Chromium with Safari's iPhone user agent, driven by history Back, Forward and reload, for the iPhone. That proves the app's logic, not Safari's own timing, the swipe's look (Safari animates a swipe between two entries of one page) or the installed app. **The owner tried both phones on a preview link before the merge** and reported it works; the details above were not reported back (README, the Android and the iPhone web checks).

**The owner's decisions:**
- From the brief for part 1: use the browser's `CloseWatcher`; where it does not exist, behaviour and code path stay as they are; a Back press does exactly what Escape and a tap outside do (the dialog's `onDismiss`, one per press, the newest first).
- Part 1 also ruled out history entries for dialogs (the router's `navigate` overwrites `history.state`, "Use this plan?" and Templates' Replace step back in history straight after closing, and Templates and the check-in keep their own step entries).
- **Owner, 2026-11-22: ship iPhone and Android together, in this release.** The "no history entries" rule is lifted **for iPhone and iPad only**, under constraints from an inventory of the app (one count against the stack, asynchronous reconcile, other `popstate` listeners never see the app's own steps, every history call of the app waits for the dialog entries to be dropped, orphan entries handled, menus included). Android and desktop never run that path.
- **Owner's check, 2026-11-22, on their iPhone:** in Safari and from the Home Screen icon, swiping back from a Templates draft returns to the template list, so a same-document history entry (`pushState` and `popstate`) works there. That is the ground for part 2.
- **Tried on real phones (owner, 2026-11-22):** the owner tried the preview of commit `5ec5eab` on their iPhone and their Android phone and reported "works on both phones". They did not say whether each was tried both in the browser and installed, or how the iPhone swipe looked. This is the only real-phone evidence; Safari's timing limits below were not provoked.

**My calls, Android (part 1):**
1. **One watcher for the whole app, not one per dialog** (`web/src/ui/components/closeRequests.ts`). While any dialog, sheet or ⋮ menu is open, exactly one `CloseWatcher` is alive. Its `close` goes to the newest open thing only (a stack in the order they opened, the same order as `Dialog`'s `openDialogs`). After a `close`, the spent watcher is replaced at once if anything is still open; when the last thing closes, the watcher is destroyed. Why: the browser groups watchers made with no user activation between them, and one close request closes a whole group. In real Chromium (153), on a probe page, watchers made with no tap between them did group: three made at load, one Escape closed the second and third together. A per-dialog design would have depended on that grouping in every case where a dialog opens by itself or a spent watcher is re-armed (on a phone, two dialogs opened at load would probably have closed together). With one live watcher there is never a group of more than one, so this cannot happen.
2. **Escape is a close request where `CloseWatcher` exists**, so in that branch `Dialog` adds no Escape `keydown` listener and `Menu` ignores Escape in its own key handler. Otherwise one Escape would dismiss twice, and the picker's dismiss ("step back from a muscle") twice would leave the muscle and close the picker. Without the API, both keep exactly the Escape handling they had.
3. **The feature test is made when a dialog or menu opens**, not once at load, and the same answer decides both "use the watcher" and "skip the Escape key". The API is reached through a small local type, as TypeScript's DOM library has no `CloseWatcher` yet; no new dependency.
4. **The ⋮ menu takes part in the same stack.** A menu item that opens a dialog: the menu lets go and the dialog takes the watcher. No ⋮ menu is drawn inside a dialog today.
5. Focus return, the inert page, the Tab trap and the tap outside are untouched. The guide (its Escape = Skip), snackbars, the lock screens and full-screen replacement screens are untouched.

**My calls, iPhone and iPad (part 2):**
6. **One module, `web/src/ui/components/historyCloses.ts`, owns the count, the listener and the gate.** It is turned on in `main.tsx` before the app renders, on an iPhone or iPad only (`isIos()` from `platform/device.ts`: the user agent says iPhone, iPad or iPod, or a Mac reports touch, as an iPad can). Off those devices, every function is a synchronous pass-through: `afterDialogEntries(fn)` runs `fn` at once, `historyBack()` is `history.back()`, `holdHistoryEntry` does nothing, no listener is added and the app renders at once. On those devices `closeRequestsSupported()` is false, so Dialog and Menu keep their Escape keydown handling and add the history path.
7. **A count, not an entry per dialog.** Each open dialog, sheet or ⋮ menu counts once (menus as on Android). The entries are pushed with the same address and a state that carries only `{ gritDialog: depth }`: no copy of `gritFrom`, `templatesStep` or `checkInStep`, so nothing reading those is misled by them. A microtask after any open or close, the entries are brought to the number of open things: pushes when more are open, one `history.go(-n)` when fewer. A close and an open in the same tick (Add to plan → the day chooser; the no-plan dialog → "Replace all data?"; Plan's Add, where the form opens before Create your own closes) make no history call at all. Closes in any order are fine: only the count matters.
8. **The lifter's Back is told from the app's own steps by a count of steps asked for.** The module's own `go(-n)` and the app's `history.back()` (now `historyBack()`, so the module knows of it) each expect one `popstate`. Its own are stopped there (`stopImmediatePropagation`; its listener is the first on the page). The app's go on to the app's listeners. Any other `popstate` that lands on a lower depth is the lifter's Back over one of the entries: it is stopped there too, so Templates and the check-in never see it, and it dismisses the newest open thing, once per entry popped, committed at once (`flushSync`), so the count afterwards is what is really still open. A dismiss that leaves the dialog open (the picker stepping back from a muscle) gets its entry back straight away, for the next Back.
9. **Every history call of the app goes through `afterDialogEntries`:** `navigate`, `goBack` and `leave` in `router.tsx` (with their reads of `gritFrom`), Templates' step-off at mount, its draft `pushState`, its two other steps back and `drop`, the check-in's `pushState`, `dropStepEntry` and its Back button (with their reads of `templatesStep` and `checkInStep`), and `App.tsx`'s `location.replace` plus `navigate` on leaving the guide (as one call). With entries on the stack, the calls queue, in order; the entries are dropped with one step back; then the calls run, reading `history.state` only then; then whatever is still open (Storage is full and "Set up App lock again?" outlive a route) gets its entries again on top of where the app now is. A queued call behind an app step back waits for that step's `popstate` and the app's handling of it. Off iPhone and iPad these are the old calls, run at once.
10. **Orphan entries are stepped off at once,** seen as a `popstate` onto one of the entries with nothing open: Forward after a Back-close, and a Back onto one. **A reload on one** (history state survives a reload; nothing is open after it) is stepped off before the app renders: `main.tsx` waits for that one step, on iPhone and iPad only. So Templates and the check-in mount on their own entry, and a reload with "Replace your plan?" open on the Templates draft ends exactly as a reload on the draft with nothing open (the list). A re-lock that unmounts everything drops the entries like any close.
11. **A tab tapped while a ⋮ menu is open** (a menu does not make the page inert): the module takes in-page link taps while its entries are on the stack, drops the entries, then follows the link, so the entry is never buried under the new screen and Back from the new tab returns to the screen, not to a menu.
12. **The app's own `history.back()` calls became `historyBack()`** and its `pushState` calls are inside the gate, in `router.tsx`, `TemplatesScreen.tsx`, `CheckInScreen.tsx` and `App.tsx`. Part 1 had left those files alone; part 2's constraints need it. Their behaviour off iPhone and iPad is unchanged: the gate runs the same code at once.

**Findings (real Chromium 153, headless):**
- **Grouping, as seen** (the spec's page, `html.spec.whatwg.org`, could not be read from here: the host is outside this session's network, so the rule was not read, only tested): on a probe page with three watchers made at load, Escape #1 closed the 2nd and 3rd together and Escape #2 the 1st. That fits the brief's rule (1 + user activations groups) once the activation Playwright's page already reports before any tap is counted (next point): two groups, the second shared. On a phone, two watchers made at load with no tap would probably share one group. After a tap, the next watcher got its own group. A single watcher re-armed after each `close`, with no tap in between, caught every Escape (three in a row). A watcher re-armed inside the `close` handler itself was separate from the one that fired. After two taps, re-arming the top watcher four times in a row never fell into the group under it.
- **Playwright's page already reports a user activation before any tap** (`navigator.userActivation.hasBeenActive` was true on a fresh page), so the grouping cannot be reproduced exactly as on a phone here. That is a further reason for call 1: with one live watcher the result does not depend on it.
- **Cases checked, each with one live watcher at most at every step** (for (d): "Set up App lock again?" is modal, so it is never open together with a tapped dialog; the one case of two open at once with one opened by itself is "Storage is full" over the Count plates sheet, and that follows a tap): (a) Plan → Edit → Change → Quads: press #1 steps back from the muscle and nothing else closes, #2 closes only the picker, #3 the form (focus back on the row's ⋮); by Escape and by `requestClose()` alike. (b) The form guide over the picker: one press closes the guide only, the picker stays on Quads. (c) The RPE sheet over Plan's form: the sheet, then the form. (d) "Set up App lock again?", opened by itself at load: one press closes it; then a tapped form with the RPE sheet over it, one press each. And "Storage is full", opened by itself over a tapped Count plates sheet: the newer first, then the sheet (focus back on its ⋮). The menu: Escape and `requestClose()` close it, focus back on its button; the header's ⋮ too; an item that opens a dialog still works and Back then closes the dialog. With the form guide open on Exercises, after a close request the URL, the current tab and `history.length` are unchanged.
- **Without `CloseWatcher` and off iPhone and iPad (a desktop browser without the API, jsdom) nothing changes:** both `Dialog` and `Menu` take their old branch, which is the old code plus calls that do nothing there (`holdHistoryEntry` returns a no-op). Every existing Escape unit test runs that branch in jsdom and passes untouched, and the Chromium test with `CloseWatcher` removed shows the old Escape behaviour in a real browser.
- An `<input type="search">` with text in it takes Escape for itself in Chromium (it clears and no close request is sent). Grit uses no search-type input (the search fields are text inputs), so Escape there still closes as before.

- **The e2e default project already emulates an iPhone.** `playwright.config.ts` uses `devices['iPhone 13']`, whose descriptor carries Safari's iPhone user agent. So every existing spec now runs the iPhone path by default. Part 1's claim that "every Escape test in the other specs runs the close-request path" was true when written and is no longer true. Since part 2, those specs run the iPhone path, and only `backCloses.spec.ts`, which now pins Chrome on Android's user agent, runs `CloseWatcher`. `E2E_UA=android` runs any spec with Chrome on Android's user agent instead (same viewport and touch). It is not for re-shooting the committed screenshots, which are taken with the iPhone user agent (the install card, for one, words itself for the phone it is on).
- **iPhone, cases 1 to 11 (`backClosesIphone.spec.ts`, Back as `page.goBack()`), each ending with an ordinary Back to where the case started and no entry of the app's left:** (1) a sheet on Exercises: Back closes it, still on Exercises, and the app makes no history call at all after that Back (`pushState`, `replaceState`, `go`, `back` and `forward` counted in the page); the next Back goes to Today. (2) The nesting: the picker on a muscle over Plan's form steps back, then closes, then the form closes, focus on the row's ⋮; the form guide over the picker closes alone; the RPE sheet over the form, then the form. (3) A ⋮ menu (focus back on ⋮); a menu item that opens a dialog; Plan options → Choose a template, then Back to Plan; a tab tapped with a menu open, then Back to the screen, not the menu. (4) Closed by Close, by a tap above the sheet and by Escape, then Back: one ordinary step. (5) The builder's "Use this plan?": Back closes only the question; Replace lands on Plan; Back from Plan to Today, as before. (6) Templates: Back with "Replace your plan?" open closes only the question and stays on the draft (on its draft entry), with no history call of the app's after it (so Templates did not step back from its draft either); Replace lands on Plan; the draft's Back to the list and to Plan still works after a question was cancelled; Close from the draft lands on Plan. (7) Form guide → Add to plan → the day chooser: Back closes the chooser; a day, then "Open Plan", lands on Plan; Back to Exercises. (8) The check-in's two steps by Back and by its own Back button, as before. A reload on step 2 with nothing open is pinned as the baseline: step 1 shows, still on step 2's entry, so the first Back after it changes nothing visible and the next leaves (as it always did; not from this release). A reload on step 2 with "Set up App lock again?" open over it (on its entry): the dialog opens again by itself, Back closes it, then every Back matches the baseline exactly. (9) A reload with the form guide open: nothing open, no entry, Back to Today. A reload on the Templates draft with "Replace your plan?" open: the list, as a reload on the draft; Back to Plan, then Today. (10) Forward after a Back-close: nothing opens, the entry is stepped off, Back to Today. (11) Six opens and closes by Escape: `history.length` grows by at most one entry ahead of the screen, Back is one ordinary step, and Forward goes back to Exercises.
- **Two Backs a few milliseconds apart,** after a Back that left the picker open (stepping back from a muscle): sent by Playwright (`goBack()` returns before the page's `popstate` handler has run), the second also left the screen; sent from the page itself (`history.back()` from a timer right after the first `popstate`), the second was taken by the picker again and changed nothing (its step back had not drawn yet). Two plain closes in a row were fine. A swipe takes hundreds of milliseconds, so a lifter should not meet this; the spec waits for each Back to settle, as swipes do.

**Limits:**
- **A dialog whose dismiss leaves it open takes every Back while it is open,** as it takes every Escape today. Every `onDismiss` in the app was read: the only one that can leave its dialog open is the picker's, which steps back from a muscle once and then closes (two presses, no trap). "Set up App lock again?" and "Storage is full" close on dismiss in the code (the brief expected they might stay open). The early-tap guards (Plan's add form, the skip and plate sheets) hold back clicks only, not a dismiss, so Back closes them at once, as Escape did. A future dialog whose `onDismiss` does nothing would keep Back.
- **Android:** in the moment between a `close` and the next watcher there is none (no wait in between, so not reachable by a person). The installed app's Back rests on Chromium's source, not a device.
- **iPhone: entries pushed with no tap.** A dialog that opens by itself ("Set up App lock again?" at load, "Storage is full") and the entry put back after the picker steps back are pushed without a fresh tap. Chromium's toolbar Back is known to skip entries a page adds without a user gesture. Whether Safari's swipe does anything like it is not known here, and `goBack()` does not model it. If it does, Back over such an entry would leave the screen as before. The README asks for the self-opened case on the phone.
- **iPhone: Forward after a Back-close** lands on the closed dialog's entry and is stepped straight off it, so that Forward does nothing visible. Each open, and each route call, replaces the forward entries, so they never pile up.
- **iPhone: a second Back within a few milliseconds** of the last (above).
- **iPhone: a slow or missing step back.** A step back the module asks for is never asked for twice. If it has not arrived after 1.5 s and the page has not moved, it is taken as still on its way: the app's queued history calls (opening or leaving a screen) wait, and it is looked at again every 1.5 s. Looks only count while the page is visible, so an app put in the background does not give up the moment it comes back. After three such looks (about 4.5 s on screen), the queue is let go: the app's calls run from where the page is. The step stays expected: if it arrives later, its `popstate` is still taken as the module's own and stopped, and whatever it leaves is reconciled or stepped off as an orphan. No other step back is asked for until then. What the lifter sees, as pinned by unit tests and then reproduced by the independent check in Chromium with an iPhone user agent and an artificially delayed step (never seen without that delay, and never on a real phone):
  - **The step never arrives:** a screen change asked for meanwhile (a tab, a button that opens a screen) happens about 4.5 s late. Back from that new screen returns to the screen the dialog was on, in one press. If nothing was asked for meanwhile, the first Back changes nothing visible (one dead Back: it takes off the leftover entry), and the next is ordinary.
  - **The step arrives after the queue was let go:** with nothing else done, the page ends where it should, with no entry left. If the lifter had moved on (to Plan, say), the late step takes them back to the screen the dialog was on (Exercises). That cannot be told apart from the lifter's own Back, so it is not undone.
  - Before the first fix round, the give-up asked for a second step, and when the late first one came the second left the screen (the independent check's repro, now a test).
- **iPhone: Safari's pushState limit.** Safari refuses `pushState` beyond about 100 calls in 30 seconds (it throws). The throw is caught: whatever opened stays open with no entry (Back then leaves the screen, as before 1.17.4), and the next open or close tries again.
- **iPhone: an app update arriving while something is open costs one Back.** The service worker's update reload replaces the page while the sheet's entry is the current one. The new page has nothing open and no entry of the app's, and the page's own entry, with the same address, is still under it. So the first Back after such an update does nothing visible; the next is an ordinary Back. Not fixed: there is no small, clearly sound fix (stepping back onto the old entry would load the page again).
- **Desktop toolbar Back** still leaves the screen with a dialog open.

**Tests:**
- New unit tests, `web/src/ui/components/closeRequests.test.tsx` (12), with a stubbed `CloseWatcher`: a watcher on open, destroyed on close and on unmount; one dismiss per close request and no keydown listener in that branch (Escape alone does nothing there); a fresh watcher after a dismiss that leaves the dialog open; two dialogs: the newer, then the older, one live watcher throughout; a dialog that steps back then closes; no leak under StrictMode; focus back to the opener; the menu (closes, focus to its button; ignores Escape itself; an item that opens a dialog); and on an iPhone user agent no watcher at all, with Escape on keydown. With the `CloseWatcher` path turned off, 10 of the first 11 fail.
- New unit tests, `web/src/ui/components/historyCloses.test.ts` (22), against jsdom's history: on Android, desktop and with nothing installed, every function is a synchronous pass-through and `pushState`, `go` and listeners are never touched; on an iPhone (and an iPad that says it is a Mac) one entry per open thing a microtask after it opens; closes out of order; a same-tick close and open with no history call; the lifter's Back dismissing the newest only, unseen by later listeners; a dismiss that stays open getting its entry back; queued calls in order, reading the page's own state; a route call with something open; the app's own step back reaching its listeners, with the calls behind it waiting; Forward onto an orphan; the start-up step-off off a reload, before render; a step carried out 2.2 s late (asked for once, its late `popstate` swallowed, the next Back ordinary); the lifter's Back queued behind that slow step (one ordinary step, the only `popstate` the app sees); a `pushState` that throws (open with no entry, Back leaves as before, the next change recovers). The first two fail on the earlier give-up; the third left two unhandled errors there. Then the bound on that hold: a step that never arrives (the queue let go after three visible looks; a screen change then runs; Back from it is one press back; with nothing done meanwhile, one dead Back), a step that arrives 6 s late with and without the app having moved on, and looks made while the page is hidden not counting.
- New e2e, `web/e2e/backCloses.spec.ts` (10), now pinned to Chrome on Android's user agent: `CloseWatcher` there, and no entry of the iPhone path (opening a menu adds nothing to history); (a) by Escape and by `requestClose()`, (b), (c), both (d), the menu, the URL and history after a close, and the old path with `CloseWatcher` removed.
- New e2e, `web/e2e/backClosesIphone.spec.ts` (24), pinned to Safari's iPhone user agent: cases 1 to 11 above, and two with `history.go` slowed in the page. The checker's repro: the step 2.2 s late, the sheet closed by Close, nothing pressed: still on Exercises, no entry, the next Back ordinary (on the earlier give-up it ended on Today). And Templates' Replace with the step 0.8 s late, so the save comes first: Templates' own step off its draft has to wait for the dialog's entry to be gone. With `afterDialogEntries` forced to run at once, this test fails (the Back after it stays on Plan), so it pins the gate in e2e. The slowed `go` is the one contrivance; nothing waits on a sleep to pass. Apart from it, the gate is pinned by the unit tests: in the other e2e cases the step back is quick enough that running the calls at once would mostly work too.
- The existing router, Templates, check-in, Dialog and Menu unit tests are unedited and pass (they run without an iPhone user agent, so on the pass-through).
- **Run on the final code, each spec on its own (`--project e2e --trace off`):**
  - Default project (Safari's iPhone user agent, so the iPhone path): `backClosesIphone` 21 passed (22 after the last test edits, rerun alone with `backCloses` 10), `templates` 20, `bodybuilding` 23, `guide` 105 (12 skipped, as before), `progress` 8, `plan` 11, `update` 6, `library` 4, `rpe` 6, `skip` 15, `plates` 12, `menu` 26, `narrowUnchanged` 20. No failure, so no other iPhone-only path got in the way.
  - `E2E_UA=android` (Chrome on Android's user agent, checked in the page): `backCloses` 10 passed, `templates` 20, `bodybuilding` 23, `plan` 11, `guide` 105 (12 skipped), `progress` 8, `update` 6, `menu` 26.
  - Vitest for the touched files and their neighbours (`src/ui/components`, `router`, Templates, check-in, the `App*` tests, `src/screens/library`, Plan, plate count, skip, guide, `appVersion`, `src/platform`): 39 files, 553 passed. `npx tsc -b` and `npm run build` clean.
  - The picker's `@speed` checks, alone (`e2e/library.spec.ts --project speed --no-deps`), under the iPhone path: 3 passed. The other `@speed` specs (`guide`, `modeSwitch`, `pictures`) were not run.
  - Run by the independent check after part 2, under the default (iPhone) user agent: `today`, `session`, `exercises`, `shell`, `reload`, `guide`, `templates`, `menu`, `skip`, `plates`, `rpe` (204 passed, 12 skipped) and `plan`, `bodybuilding`, `coach`, `backup`, `update`, `safeguards`, `library`, `progress`, `modeSwitch`, `fill`, `smoke`, `olderCopy` (100 passed); and the full vitest suite (92 files, 1,676 passed).
  - Still not run since part 2: `narrow`, `narrowDeeper`, `pictures`, the screenshot specs, `offline`, `motion`, `slide`, `press`, `showcase`, and the `@speed` tests other than the picker's.
  - Not covered by e2e: a re-lock with something open, and a dialog outliving its route (unit test only).
  - After the fix round (the give-up, `pushState`, the notes), each on its own: `backClosesIphone` 24 passed, `backCloses` 10, `templates` 20, `bodybuilding` 23, `plan` 11; vitest for `src/ui/components`, `router`, Templates and check-in: 9 files, 168 passed; `npx tsc -b` and `npm run build` clean.
  - After bounding the hold: `backClosesIphone` 24 passed, `backCloses` 10; vitest `src/ui/components` 6 files, 122 passed (`historyCloses.test.ts` 22); `npx tsc -b` and `npm run build` clean.
  - Part 1's runs, below, were before part 2.
- Part 1, run on its own final code: `backCloses` 10 passed, `library` 4, `plan` 11, `rpe` 6, `skip` 15, `plates` 12, `menu` 26, `narrowUnchanged` 20, `templates` 20, `guide` 105 (12 skipped), `bodybuilding` 23; at that time all of them ran the `CloseWatcher` path (there was no iPhone gate yet). Vitest: 16 files, 321 passed.
- An earlier, unintended run of nearly the whole Playwright suite (a spec with `@speed` tests pulls in the whole `e2e` project as a dependency), on part 1's feature commit before the version bump, was stopped after about 450 tests with no failure; it included `narrow`, `narrowDeeper` and `pictures`. `library` then always ran with `--project e2e`, so its `@speed` tests did not run.
- **The full suites had not run when these notes were written:** they run once on merged main.

**Release routine for 1.17.4 (on merged main):** the default full Playwright run takes the iPhone path (the default project emulates an iPhone 13, with Safari's user agent). So after it, one extra pass runs with `E2E_UA=android`, over the dialog and history specs: `backCloses`, `library`, `plan`, `rpe`, `skip`, `plates`, `menu`, `templates`, `bodybuilding`, `guide`, `today`, `session`, `progress`, `update` (not the screenshot specs, which are iPhone shots). For example, from `web/`: `E2E_UA=android npx playwright test e2e/backCloses.spec.ts e2e/library.spec.ts … --project e2e`.

**Released 2026-11-22** (owner: "approved"). On merged main (`2b27544`):
- Gradle (domain tests, the web-domain check, the app's unit tests): passed. Web unit tests: 1,684 of 1,684. Type check and build: clean. Speed project, three times: 6 of 6 each. The extra `E2E_UA=android` pass: 252 passed, 12 skipped.
- **Full Playwright run, first time: 512 passed, 1 failed, 12 skipped, 6 not run** (the speed tests, held back by the failure; they passed three times in their own runs). The failure was `guide.spec.ts:105` (`guide_browserBackAndEscape_leaveTheGuide_forTheScreenItWasOpenedFrom`), at line 119: Escape did not leave the guide.
  - **Cause: the test presses Escape before the guide listens.** An instrumented copy, repeated under heavy load (1 failure in 450), recorded one `keydown` listener on `window` when Escape arrived, against two in passing runs; the guide's listener was attached straight afterwards, and nine seconds later the page was still on `#/guide` (the iPhone path would have let go after about 4.5 s, so it is not what held it). The guide's code and this test are unchanged since 1.17.3. Whether it could already fail on 1.17.3 is not known: 200 of 200 passed there under the same load, which is too few to tell at about 1 in 300.
  - Alone it passed 30 of 30; under load 1 failed in 150.
- **Full Playwright run, second time, same commit, nothing changed: 519 passed, 12 skipped.** Main was pushed after this run.
- Live: `assets/index-DjVSL2N1.js` contains 1.17.4; `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` (JavaScript, with its `activate` listener) and is served `cache-control: no-cache`.

**Follow-up from the release (rides the next one):** `guide.spec.ts:105` presses Escape as soon as the guide is visible, and the guide adds its Escape listener in an effect just after it is drawn. A sound fix waits on a named condition that holds only once the listener is attached; if nothing on the page shows that, it is an app change (for example the guide marking itself ready), not a test tweak. No retry and no longer timeout, as with the menu test.
- **Done (test only):** before the Escape, the test now waits for the guide's title to have the focus (`toBeFocused()` on `guide-title`). That is the named condition: the title is focused in one place only, the first page's effect (`GuideScreen.tsx:215-217`), and React runs one commit's passive effects in a single uninterrupted pass, the page's (a child) before the guide's (its parent), so once the focus can be seen from the test, the guide's effect that adds the Escape listener has run. A probe (a copy of the test counting `window` keydown listeners, with a capturing `focusin` listener on the document) gave the same counts in all 70 runs, 30 idle and 40 under CPU load: 1 with the guide closed (another listener of the app's), still 1 at the moment the title takes the focus (the page's effect runs first, as expected), 2 in a microtask queued from that focus (the pass has finished by then), these two on both openings of the guide; and, around the reopening after `goForward`, 2 after the test's focus wait and 2 as Escape arrives. The count at the focus moment itself is 1 by design and is no risk: nothing outside the page can run until the pass ends. This does not show the failure can never happen again; it shows only that the test no longer presses Escape before the guide listens.
- Tests that press Escape on a dialog right after it is visible have the same shape in principle on the iPhone path (the dialog adds its Escape listener in an effect, in which it also moves the focus into the panel). None has failed, and they are left as they are.
- **Done since (test only): every close request a test sends to a dialog or a ⋮ menu now waits until it listens.** Two shared waits in `web/e2e/helpers.ts`; no app change, no assertion changed, no sleep, retry or longer timeout.
  - `dialogListening(dialog)` waits until the page around the dialog is inert (every element sibling of its scrim, and of each ancestor up to `body`, has `inert`: what `isolateFromPage` leaves) **and** the focus is inside its panel. Both are left by the effect that, in the same synchronous pass, then adds the Escape listener (iPhone path) or registers with the app's close watcher (Android path) and, on an iPhone, for a history entry; neither user agent's own mechanism is waited on. Either half alone has a hole, which the other closes: a dialog with an `autoFocus` field (Create your own, Set best, the bodyweight entry) has the focus before its effect, and the page can be inert around a dialog before its effect in two shapes found by the probe: the day chooser that the form guide's Add to plan opens in the same commit that closes the guide (the guide's marks are still on until the same effect pass), and a dialog left open when one over it closes (Plan's form once the picker is taken off the page and before the picker's cleanup has run). In both the focus is on nothing until the effect or the cleanup moves it into the dialog, as its last step in the cleanup's case. Hence two rules of use: await it once the dialog is open, before a tap that redraws what had the focus (a muscle tile in the picker takes the focus out of it for good; the waits sit before that tap), and again for a dialog once one over it has closed.
  - `menuListening(menu)` waits for the focus on the menu's first item. On the Android path the menu arms the watcher in an effect and its key handler leaves Escape to it; the first item is focused by a layout effect of the next commit, which React renders only after the first commit's effects have run. On the iPhone path Escape is the menu's own key handler, on from the moment it is drawn, so the wait costs nothing there.
  - Why it holds, per shape (each false at draw, true after the effect): (a) the first dialog on a page (Plan's form): its own siblings are not inert at draw; (b) in place over another (the picker over Plan's form, Create your own over the picker): the lower dialog's scrim or wrapper is a non-inert sibling until this effect marks it; (c) portaled over another (the RPE sheet over Plan's form, the form guide over the picker): `#root`, not inert, is a sibling of the new scrim in `body`; (d) `autoFocus`: the inert half; (e) out of order (Plan → Add → the picker → Create your own → Create): the add form is drawn in the commit that also removes Create your own, whose cleanup runs first in the pass, and the add form's effect then marks the picker's (hidden) wrapper beside it; the picker's own marks stay, and the wait passed on the add form under both user agents, so no site timed out and there is no app finding.
  - **Probe** (a scratch spec, kept out of the repo): an init script counting `document` keydown listeners and constructed and destroyed `CloseWatcher`s, recording the condition synchronously when a scrim is put on the page or taken off it, and, from a `MutationObserver` plus a `focusin` listener, the counts in a microtask from the moment the condition first holds for each dialog; in that microtask it also sent a close request (a `keydown` Escape on the iPhone path, `requestClose()` on the newest watcher on Android) and the test checked which dialog closed. One run per user agent, and one more each for the form left under the picker. At draw the condition was false for all eleven dialogs drawn (Plan's form, the picker, Create your own, the RPE sheet, the form guide over the picker and on Exercises, the bodyweight entry, Set best, Plan's add picker and add form, the day chooser); the page around the day chooser was already inert then, its focus outside. When it first held, the iPhone path had the dialog's listener on: 1 with one dialog open, 2 and 3 stacked, 2 for the add form (the picker's and its own, Create your own's gone); on Android a live watcher every time (1), and in every shape the close request sent then closed the newest dialog only (the one under it stayed). For the form left under the picker: false when the picker was taken off (inert, focus outside), true after its cleanup, with 1 listener on the iPhone path (the picker's gone), and a close request then closed the form. Menus on Android: 0 watchers when drawn, 1 live at the first item's focus, and `requestClose()` then closed the menu.
  - **Inventory**, `web/e2e` (Escape, the `requestClose()` stand-in and `page.goBack()`): 56 sends to a dialog or a menu. 43 changed: `backCloses` 20 (its `escape`/`backButton`/`press` senders, the picker's wait inside `pickerOnAMuscle`), `backClosesIphone` 8 (Escape at test 11; the Backs to Plan's form after the picker or the RPE sheet has closed, at 2a, 2b and 2c, and to the picker after the form guide over it has closed, at 2b; "Close from the draft" and the day chooser at test 7, which had no depth wait; and Plan's form opened by the row menu's Edit, in `editFormOpen`, whose depth wait was met by the menu's entry, which stays for the form as the guide's does for the day chooser), `library` 2, `narrow` 2 (menus), `narrowUnchanged` 5 (one menu), `pictures` 1, `plan` 1 (menu), `plates` 1 (menu), `rpe` 1, `skip` 2 (one menu). 13 left: the Escape at test 4 in `backClosesIphone` (line 314), which the first write-up counted as changed and the independent review found unchanged: its `open()` already waits for the sheet's entry at depth 1, which only the sheet's own effect can bring about, so the listener is on; 11 Backs in `backClosesIphone` that already wait on their entry's depth, where only the dialog's own effect can bring the depth to that number (its entry is pushed a microtask after the effect): a sheet, a question or the lock dialog opened over nothing, the ⋮ menu, the guide over the picker (depth 3), the RPE sheet over the form (2), and the picker at 2 before and after it steps back from a muscle (the same picker, nothing closed over it), and the menu Escape in `backCloses` without CloseWatcher (the menu's key handler, on when drawn). Out of scope: the guide's Escape (done), and 26 `goBack()`s with nothing open (11 in `backClosesIphone`, 11 in `bodybuilding`, `progress` and `templates`, and 4 in `guide`, where the guide is a route, not a dialog). **Independent review (2026-11-22): sound, with these limits named.** A Back on the iPhone path needs the history entry as well as the dialog listening, since the entry is pushed a microtask after the effect or later; each of the seven converted Backs also waits on its entry's depth. The review's own probe checked the condition at every task boundary (what a test can observe) in six shapes under both user agents: whenever the condition held, the newest dialog took the close request. Not run by the review: `narrow` and `pictures`; no repeated or loaded runs.
  - Runs, from `web/`, one at a time, `--project e2e --trace off`: each of the ten changed specs passed under the default user agent (`backCloses` 10, `backClosesIphone` 24, `library` 4, `pictures` 12, `plan` 11, `plates` 12, `rpe` 6, `skip` 15, `narrow` 71, `narrowUnchanged` 20) and again with `E2E_UA=android` (the same counts; `narrow` and `pictures` run under it as well); `npx tsc -b` clean.
  - This makes the tests wait until the dialog or menu listens before they send it a close request. It does not show that a test can never fail for another reason.

**Published 2026-11-23 (owner: "publish"): `CLAUDE.md`, these notes and the test follow-ups, on main as `cabcc06`.** No app change and no version change (still 1.17.4, versionCode 32); no APK was built. On merged main: `./gradlew :domain:allTests` and `:app:testDebugUnitTest` passed, `:domain:checkWebDomain` passed; `npm run typecheck` clean; web unit tests 1,684 of 1,684; build clean; the full Playwright run 519 passed, 12 skipped, first time; the speed project three times, 6 of 6 each; the `E2E_UA=android` pass 252 passed, 12 skipped. Live: `assets/index-d9NjgCQ-.js` carries build id `cabcc06` and 1.17.4; `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` and is served `cache-control: no-cache`. Because every build carries its commit id, open copies of the web app reload once for this build although nothing a lifter sees changed. `CLAUDE.md` at the root is new in the repository: until now a local file of that name was kept out of git.

**Version:** Android 1.17.4 (versionCode 32), web 1.17.4. Android changes nothing but the version. No database or backup change. Settings re-shot on both apps for the version line: six shots on each, and no other tracked shot changed. The web six were byte-identical across two runs; the Android six were shot once (`:app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`, build successful). Part 2 changes no shot.

**Follow-ups:**
- When Safari ships `CloseWatcher`, iPhone and iPad keep the history path (owner's constraint). Moving them over would be a separate decision, and it would depend on an edge swipe counting as a close request.
- The desktop toolbar Back still leaves the screen with a dialog open.
- **Should an Android user-agent run join the default full run?** `E2E_UA=android` exists now. The default project has only ever run with the iPhone user agent, so the Android branches (the install card's Android text, `CloseWatcher` everywhere but `backCloses.spec.ts`) are covered only where a spec pins them.

## Done: 1.17.5, a finished session stays finished when the app is closed and opened (both apps)
Reported by the owner on 2026-11-23: "When I finish a workout on the today page I hit finish. If I close and reopen the app the session becomes active again and I have to click finish again."

**Cause:** Finish was kept only in the Today screen's memory (`Local.summary`, in `TodayViewModel.kt` and in `useToday.ts`). Nothing stored it, and a session row has no "finished" mark. An app opened again found today's session and showed it as open, with its Finish button. Both apps did this, from their first version; it is not from a recent change. Reproduced in a failing test on each app before anything was changed.

**What the lifter gets:**
- **After Finish, closing and opening Grit shows "Session saved" again,** with the same numbers (sets, exercises, new coach notes, skipped), not the session with a Finish button. The same after a reload of the web app, and after Android has closed the app in the background.
- **Keep logging opens the session again, and it then stays open** over a close and reopen, until Finish is tapped again.
- The next day opens on the new day's start card, as before.
- "Add to today" on the Exercises tab stays refused for a finished session after a reopen too, as it was before the app was closed.

**How:**
- Finish stores a small note on the device: the session's id, its day, and the summary's four numbers. Android: one key of its own in the settings store (`finished_session`, beside `guide_seen`, outside `AppSettings`). Web: one `meta` row (`today:finished`).
- **Stored before the summary is shown,** so "Session saved" on screen means it is stored. If that write fails, the summary still shows and nothing crashes; only the reopen is lost.
- **Read back before Today has its data** (`loading` stays true until then), so the session never flashes as open first, and "Add to today" cannot slip in before the read. The web already waited there for the kept numbers; Android's `build` gained the same gate (`restored`).
- **Used only when it names the session stored for the day on screen** (same id, same day). A note from another day, a damaged one, or one about a session that is gone finishes nothing.
- Keep logging deletes it. Restore backup deletes it on both apps: it is about the replaced log.
- **No database, Dexie or backup change.** The note is not in a backup: it belongs to the device, like the kept numbers and the guide's "seen".

**My calls:**
1. **A reopened finished session shows the same "Session saved" card** it showed when Finish was tapped. The owner was not asked: it is the plain reading of "stays finished", and Keep logging is on that card for going back in.
2. **A note on the device, not a column on the session.** A column would mean a new Room version, a new Dexie version and a new backup version (both apps updated together, older copies refusing the file) for a mark that matters for one day.
3. **The numbers are stored, not counted again on reopen.** The count of new coach notes cannot be worked out afterwards, and the card should say what it said.
4. **The Android tests forget the note before each test** (`TestDatabase.before`). The settings store is one per process and outlives each test's database, where session ids start again at 1; without this, one test's Finish could open another test's Today on the summary.

**Limits:**
- **Two copies open at once (web):** a second tab, or Safari beside the Home Screen app's own log, does not hear of a Finish in the other until it is reloaded. Android's two view models for one day (should that ever happen) likewise.
- A set deleted from Progress after Finish leaves the card's numbers as they were at Finish.
- A summary left on screen overnight without closing the app still holds the screen until it is dismissed, as before.
- **Not checked on a phone.** The tests stand in for closing and opening: a new view model over the same stored data (Android, Robolectric), a remounted hook, and a real page reload in Chromium (web).

**Tests:**
- Android, `TodayViewModelTest`: `finishedSession_staysFinished_whenTheAppIsOpenedAgain` (failed before the fix; also Keep logging, then a third open) and `finishedNote_ofAnotherDayOrAnotherSession_finishesNothing`. `SettingsRepositoryTest`: the note round-trips, a change of settings leaves it, damaged text reads as none. `BackupTest.restore_forgetsTheFinishedSession`.
- Web, `useToday.test.tsx`: `finishedSession_staysFinished_afterAReload` (failed before the fix; also Keep logging, then a third mount), `finishedYesterday_reloadedToday_opensOnTheNewDay`, `finishedNote_ofASessionThatIsGone_isIgnored`, `finishedNote_thatIsDamaged_isIgnored`. `backup.test.ts`: the restore test now also expects `today:finished` gone (failed before the fix).
- Web e2e, `reload.spec.ts`: `reloadAfterFinish_staysFinished` reloads the page with "Session saved" showing.

**Checks, on the branch (`ccr-2f9842b5-owq43n`), 2026-11-23, one at a time:**
- Gradle: `:domain:allTests` passed (the domain is unchanged); `:app:testDebugUnitTest` passed, 1,113 tests, 0 failed, 1 skipped; `:domain:checkWebDomain` passed. `:app:assembleDebug` built before the change, and `:app:assembleRelease` after it: versionCode 33, versionName 1.17.5 (the APK was not distributed).
- Web: `npm run typecheck` clean; unit tests 1,688 of 1,688; build clean.
- **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 520 passed, 12 skipped, first time** (519 before, plus the new reload test).
- **The `E2E_UA=android` pass,** the 1.17.4 list plus `reload.spec.ts`: 254 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- **After those runs, two small changes from a review, each rerun:** the web's Finish now stores the note first and only then lets go of the day (before, a phone unlocked after midnight during that one write could have moved Today to the new day with the summary unseen); and the Android test also asserts that "Add to today" is refused on the reopened finished session, as the web test does. Rerun on that final code: `:app:testDebugUnitTest` (1,113, 0 failed, 1 skipped), the type check (clean), and `reload`, `session` and `today` specs (8 passed). The web unit tests (1,688 of 1,688) and the build (clean) too. The full Playwright run, the Android pass and the speed project were not repeated after it.
- **Not run on merged main:** these ran on the branch. The routine's run on merged main comes with "publish".
- **Not checked on a phone,** neither app.

**Shots, before and after the change:** the full run was repeated on the code before this release (`47fc711`, in a scratch worktree, `--project e2e`: 513 passed, 12 skipped), and the 174 web shots of the two runs were compared. 163 are byte-identical, every Today shot among them. The six Settings shots differ in the version line only (about 280 pixels in a 17 by 27 pixel box). Five differ by a few pixels in pictures no part of this release touches, shots already known not to be byte-stable between runs: `form-guide-dark` (169 pixels), `guide-side-first-launch-bodybuilding-light` and `guide-side-first-launch-powerlifting-light` (221 each), `guide-targets-bodybuilding-dark` and `-light` (9 each).

**Found on the way, open:** a re-shoot here of the unchanged code (`47fc711`) rewrote 36 tracked web shots and wrote 5 that are not tracked (`older-copy-*`, `plan-known-bests-*` among them). Looked at in two: in `today-dark` the three stepper columns sit about 8 pixels further left than in the committed shot, and in `form-guide-dark` the plate's outline is grey where the committed one is chalk. The cause was not looked for (the browser or its fonts in this session, or shots left behind by an earlier release, are both possible). None of them was committed: only the six Settings shots were, as the routine says. So the committed web shots, other than Settings, are not what this session's Chromium draws. **Settled on 2026-11-24:** the committed shots were out of date, not the browser; they were re-shot (see "Done (2026-11-24)").

**Published 2026-11-23 (owner: "publish"), on main as `c5f7754`** (a `--no-ff` merge of the branch; its tree is the branch's, `02b2a91`). On merged main, one at a time, before the push:
- Gradle: `:domain:allTests` passed; `:app:testDebugUnitTest` passed, 1,113 tests, 0 failed, 1 skipped; `:domain:checkWebDomain` passed.
- Web: `npm run typecheck` clean; unit tests 1,688 of 1,688; build clean.
- **Full Playwright run: 520 passed, 12 skipped, first time.** The `E2E_UA=android` pass (the 1.17.4 list plus `reload.spec.ts`): 254 passed, 12 skipped. Speed project alone, three times: 6 of 6 each. So the two late changes have now been through every run.
- Live, about a minute after the push: `/` names `assets/index-2b98uWcf.js`, which carries 1.17.5 (no 1.17.4) and build id `c5f7754`; `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` and is served `cache-control: no-cache`.
- Android: `:app:assembleRelease` gives versionCode 33, versionName 1.17.5. **No APK was handed over:** the owner installs from Android Studio (Update Project on `main`, then Run), which builds and installs the debug build on the phone.
- **Still not checked on a phone,** neither app: the owner's checks are in `README.md`, "Finished stays finished (1.17.5)".
- A session finished before the update has no note, so it shows as open once more after the update; one more Finish, and it stays finished.

**Version:** Android 1.17.5 (versionCode 33), web 1.17.5. No database or backup change. Settings re-shot on both apps for the version line: six shots on each. Android: `:app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`, build successful, and no other tracked shot changed. Web: from the full run above; the other shots it rewrote were put back (see the shots paragraph).

## Done (2026-11-24): the web tests run in CI, ten phone checks, the web shots re-shot (no app change, no version change)
Asked for by the owner on 2026-11-24, from a list of possible improvements: "do these and show me the skip ring design". Still 1.17.5 (versionCode 33): nothing a lifter sees changed.

**1. The web tests run by themselves.** `.github/workflows/tests.yml` has a second job, `web`, beside `kotlin`:
- Type check, unit tests, build, then the browser tests as an iPhone (`--project e2e`) and as an Android phone (`E2E_UA=android`, the 1.17.4 list plus `reload.spec.ts`; the list now lives in the workflow, and `CLAUDE.md` points there).
- It installs the Chromium that Playwright's version was built for (`npx playwright install --with-deps chromium`). Here the tests keep using the container's own Chromium.
- **The timing checks (`@speed`) are not in CI:** they hold two screens to 100 ms on the machine's clock and have no headroom (see the 2026-11-13 follow-up), so on a shared runner they would measure the runner. Changing that limit is the owner's call. They stay a check to run by hand.
- No retries and no longer timeouts in CI: a test that fails only there is a finding.
- The `kotlin` job also runs `:domain:checkWebDomain` now.
- **When it runs:** on pull requests, on pushes to `main`, and by hand on any branch. On a push to `main` it reports after the live site has updated, so it does not hold a release back: the checks on merged main before the push stay as they are.
- The repository is private, so the runs use the account's Actions minutes.
- **First run** (by hand on the work branch, `e260e2c`, run 45341802266, 39 minutes): the `kotlin` job passed (17 min: domain tests, app tests, the generated-domain check). In `web`, the type check, unit tests (7 min) and build passed. **The browser tests failed by one test, the same one in both passes:** as an iPhone 513 passed, 1 failed, 12 skipped (21.8 min); as an Android phone 253 passed, 1 failed, 12 skipped (9.0 min).
  - **The failure, and its cause:** `guide.spec.ts:509`, `guide_today_skippedRow_slidesAndTheRestCloseUp`, at its line 544. It compared the properties of the "Skipped today" line's animation in a fixed order (`opacity, transform, opacity, transform`) and got `transform, opacity, transform, opacity`: the same two properties in both keyframes, listed the other way round. The order a keyframe lists its properties in is the browser's. **CI runs Chromium 156** (the build Playwright 1.64.1 ships, v1248); **this container has Chromium 153** (v1243), where the order is the one the test expected. Not an app fault, and not load: it failed in both passes at the same line.
  - **Fix (test only, `ed521fa`):** the properties are sorted before they are compared, as the file's other animation checks already do (a `Set` at the ring, `.sort()` at the tick). It still checks one animation with opacity and transform in each of two keyframes. Passes here on 153.
  - **So the local runs and CI are on different Chromiums.** Everything checked here before a release runs on 153; CI is three versions newer. The one difference found is this test's. Which of the two is closer to the phones is not known.
  - **The artifact upload failed too:** "Artifact storage quota has been hit" (the account's, not this run's). The two upload steps now carry `continue-on-error`, so a full store cannot be the reason a run is red; until the owner frees space (GitHub, Settings, Billing, or deleting old artifacts), a failed run leaves no report to download, only its log.
  - A run costs about 55 runner-minutes (17 + 34 to 39), where the Kotlin job alone cost about 12 to 17, and the repository is private. **At the last week's pace (about 23 runs in seven days, all but one a push to main) that is roughly 1,200 minutes a week,** against roughly 300 before. The account's plan and its included minutes cannot be read from here: for the owner to check (GitHub, Settings, Billing). If the allowance runs out, the runs stop or are charged. Running the Android pass only on main would save almost nothing, since nearly every run is a push to main; running the web job only when `web/` or `domain/` changed, or dropping the Android pass, would.
- **Second run** (`ed521fa`, run 45346888893), with that test fix: **the `web` job passed, every step** (type check, unit tests, build, the iPhone pass in 21.8 min, the Android pass in 8.9 min; 39 minutes). So the web tests run in CI and pass on Chromium 156.
- **In the same run the `kotlin` job hung** and was ended by its 45-minute limit: the domain tests passed (4 min), then `:app:testDebugUnitTest` printed nothing for 38 minutes. In the first run, an hour earlier, it took 11.6 minutes and passed. The two commits differ in one web test file and the workflow's upload steps, nothing the app's tests read.
  - Gradle prints nothing while tests run, so the log does not say which test was running. **A mechanism that produces exactly this was then proven here, and is the most likely cause** (below). No log or thread dump of that run shows what it was actually waiting on.
  - **Not seen before:** the 25 runs before today show no hang (four failures, which are the known flaky tests: `SkipTodayViewModelTest.painSkip_nextTodayTarget_isHeld` on `cabcc06`, `SkipTodayViewModelTest.skip_undo_removesSkipAndOpenCard` on `9c9b8a0`, `PlanScreenTest.planAdd_changeToOwnExerciseFails_…` on `b49d7d3`; each failed, none hung). Three runs include 1.17.5's test change (`TestDatabase.before` now forgets the finished note inside `runBlocking`): two passed (`c5f7754` on main, `e260e2c`), this one hung. So 1.17.5 was the suspect.
  - **So that the next hang says where:** the app-test step now has its own 30-minute limit, and if it is still going after 25 minutes it prints the test JVM's threads first (`jstack`: which test, and where it waits).
  - **The mechanism (proven), most likely the cause (not shown for that run): a settings write left in flight when a test ends blocks the settings store for every test after it.** The store (DataStore) takes its writes one at a time and runs each write's change in the context of whoever asked. A write begun by a view model is handed back to the main thread. Under Robolectric the main thread runs only when a test lets it, and its pending work is thrown away when the test ends. If a test ends between the hand-off and the main thread running it, that write never finishes and every later write queues behind it. The next test's set-up then waits in `runBlocking`, which has no time limit: a hang.
    - **Why since 1.17.5:** Keep logging (`dismissSummary`) now starts such a write and nobody waits for it. `ExercisesScreenTest`'s finished-session test ends two assertions after calling it. Whether the write is through by then is a race, which the slower runner lost once in three runs. My `TestDatabase.before` (also 1.17.5) is the `runBlocking` it hung in, and every test class's own `settings.update { AppSettings() }` set-up would have hung the same way.
    - **The mechanism, shown:** a scratch test started a write on the main dispatcher and did not let the main thread run: a second, blocking write timed out; once the main thread ran, it went through. Then two tests in order, the first ending with such a write in flight: the second test's write timed out. With the fix it goes through in 20 ms.
    - **Not an app fault.** On a phone the main thread always runs, so the write finishes. Nothing in the app changes.
  - **Fix (test only):** `TestDatabase` no longer blocks on the store. Before and after each test it makes its own write off the main thread and lets the main thread run until that write is through, which is only once every write before it is. So a write a screen began is finished while its test's main thread still exists, and a store that stays stuck fails the test after 10 s with a message instead of hanging. New test `SettingsWriteLeftInFlightTest` (the two-test case above). Tests that do not use `TestDatabase` (the two `MainActivity` ones) get no such wait.
  - A repeat of the whole app suite on two processors, to catch the hang here with a thread dump, was started and stopped after a few minutes, once the mechanism test had answered it.
- **Third run** (`e7a0009`, run 45355199099), with the test set-up fix: **green, both jobs, every step, in 34 minutes.** `kotlin` 17 min (app tests 11.3 min); `web` 34 min (unit tests 5.6, the iPhone pass 18.9, the Android pass 8.0). One green run does not show the hang is gone, since it came once in three; the mechanism test does that, and if it comes back the step now says where. Here, on the fix: the whole app suite, 1,115 tests, 0 failed, 1 skipped.
- **Found in the history, not from today:** the run on `cabcc06` (the publish before 1.17.5) was red in CI, and so were three earlier pushes to main, each by one flaky test. Nothing reports a red run to the owner unless he looks, and the artifact store has been full since at least 2026-11-21 (`9c9b8a0`'s upload failed the same way).

**2. Ten phone checks per release.** `README.md` had 185 checks, none ticked. They moved, word for word, to `docs/manual-checks.md` (three lists: iPhone, Chrome on Android, the Android app). `README.md` now has "Phone checks after a release": nine standing checks that only a real phone can answer (it updated; the log is still there; offline; log a set; the rest alert after locking; App lock; backup out; backup in, only when moving the log or when the backup changed; Back closes what is open) and a tenth for the release's own change (now 1.17.5's "finished stays finished", which is still not done on a phone). `CLAUDE.md` says where a release's checks go from now on: its one check in item 10, its detailed checks in `docs/manual-checks.md`.
- **My choice of the nine,** not the owner's: he can swap any.

**3. The web shots re-shot.** 33 tracked shots in `docs/design/web/screens/` rewritten and 4 added that were never committed (`older-copy-{dark,light}`, `plan-known-bests-{dark,light}`: their specs were not run in the sessions that wrote them).
- **Cause found: the committed shots were out of date, not this machine.** The code that last wrote `today-dark.png` (`bf17b19`, 2026-11-18, version 1.12.3) was checked out and its `screenshots.spec.ts` run here: `today-dark` and `today-light` came out byte for byte as committed. So this Chromium draws that code as the committed shots show it, and the difference is what later releases changed (the steppers' columns sit about 8 px further left). This corrects the earlier notes ("render differently on this machine", from 1.16 on, and 1.17.5's "found on the way").
- `form-guide-{dark,light}` were last shot on 2026-11-12, before the accent turned grey (`b2f896b`, 2026-11-16). The plate's outline is the accent (`.fig-accent`), so grey is right and the old chalk outline was the old accent. Not an app finding.
- Each release since had compared a run before with a run after, never with the committed files, which is why nothing flagged it.
- **Shot twice** (the 14 specs that write shots, `--project e2e`, 270 passed and 12 skipped each time): 169 of 174 identical between the two runs. **Not byte-stable, 5:** `exercise-guide-actions-{dark,light}`, `guide-side-first-launch-bodybuilding-{dark,light}`, `guide-targets-powerlifting-light`. They will show as changed on a later re-shoot with nothing changed; the second run's copies are the ones committed.
- Looked at by eye: six of the new shots (Today, Today with a rest, the RPE sheet, Coach on the bodybuilding side, the skip menu, the skipped line). All whole screens, nothing cut or missing. The other 31 were not looked at one by one.
- Correction to 1.17.5's note: that re-shoot wrote 4 untracked shots, not 5 (the fifth untracked entry was the scratch worktree's link to `node_modules`).
- The Android shots (`docs/design/screens/`) were not re-shot: the 1.17.5 run rewrote only its six Settings shots, so they are current.

**4. The skip ring: shown, not built.** Asked for as "the skip ring design". **There is no drawn design,** and my summary to the owner ("designed but not built") overstated it. What exists:
- one line in 1.16's follow-ups: "A skip ring (a tap ring on Skip today's menu) was not built; it could be anchored as Log set's is";
- the guide spec's rule for the ring it would copy (`docs/superpowers/specs/2026-11-19-grit-guide-design.md`, point 7): "A tap ring pulses once on the button just before its result appears. It shows only in the guide."
- Today, on the guide's screen 4, the ring pulses on Log set before the set is ticked; the skip step has none: the last row just leaves and "Skipped today" rises in.
- Shown to the owner: three frames of the web guide's screen 4 with motion on, on a held clock. Two are the app as it is (the ring on Log set; the skip step settled). The middle one is a mock-up made in the page for the picture only: a ring of the same make, at the same point of its pulse, on the ⋮ of the row about to be skipped. Kept out of the repository. Nothing was built; whether to build it is the owner's.

**Checks:** no app code changed, so no version bump. `README.md`, `CLAUDE.md` and the notes are prose. The workflow is checked by its three runs (above), the last green on both jobs. The shots by the two runs. The Android test set-up change by its new test and by the whole app suite here (1,115 tests, 0 failed, 1 skipped). The one web test changed (`guide.spec.ts:509`) passed here alone on Chromium 153 and in CI's full runs on 156. **Not run for this, here:** the web unit tests, the full local Playwright run and the timing checks (CI ran the first two; the routine's runs on merged main come with "publish").

## Open (2026-11-24): a user reports the web app "buggy" on an iPhone, with no details
The owner passed on, on 2026-11-24, that a user called the app "buggy" on an iPhone and said nothing more. No symptom, no version, no screen. **No app code was changed for it.** Nothing here runs Safari, so an iPhone fault can be neither confirmed nor ruled out from this container.

**What is known:**
- 1.17.4 (2026-11-22) changed how Back works on an iPhone and iPad only (`historyCloses.ts`: a history entry per open dialog, sheet or menu). It is the one large iPhone-only change of the week, and it was never confirmed on a phone.
- Until 1.17.5 (published 2026-11-23, 20:33 UTC) a finished session came back as open after a reload, on the web as on Android. Three pushes to main since 2026-11-22 each gave open copies of the web app an update. A report of "it came back as active" or "it reloaded by itself" may be from before 1.17.5.
- Reading `historyCloses.ts`: after a sheet or menu is closed by a tap, the module asks for a step back and holds the app's own history calls (a tab tap among them, while entries are on the stack) until that step's `popstate` arrives. In Chromium it arrives within milliseconds. If Safari delivered it late or not at all, a tab tap could wait up to 4.5 s (three looks, 1.5 s apart). **That is a reading of the code, not something seen.**

**Tried: the browser tests in WebKit, Safari's engine, on GitHub (by hand).** New: `E2E_BROWSER=webkit` in `playwright.config.ts`, and a `webkit` job in the workflow that runs only when started by hand with its box ticked (then the two usual jobs do not run). It is not part of the automatic runs.
- **First run: 103 of 103 failed at their first step.** Not the app: Playwright's WebKit windows refuse private file storage, as a private Safari window does, so Grit showed its own "Private browsing won't keep your log" screen instead of the app (read from the failed page's content, which the job now prints, since the artifact store is full). The tests now start as if "Continue anyway" had been chosen, in the WebKit pass only (`e2e/storage.ts`).
- **Then: 102 of 103 passed, in 9.4 minutes.** `backClosesIphone` 24 (the iPhone Back path, in an engine without CloseWatcher, as on the phone), `menu` 26, `skip` 15, `plan` 11, `progress` 8, `today` 5, `coach` 4, `library` 4, `reload` 2 (a reload mid-session, and after Finish), `session` 1, `offline` 1, `smoke` 1.
- **The one failure is the test's own:** `backup.spec.ts` clears the site's data with a Chromium-only tool (`newCDPSession`). The export before that step worked in WebKit. Left as it is, not skipped.
- **What this does and does not show:** the app starts, logs, finishes, reloads, works offline and closes dialogs on Back in WebKit on Linux. It is not iOS Safari: no Home Screen app, no edge swipe, no real touch or keyboard, not Safari's own timing of `popstate`. A pass here is not an iPhone pass.
- Cost: three by-hand runs, about 24 runner-minutes together.

**Asked of the owner, for the reporter:** the last line of Plan, Settings (the version); a Safari tab or the Home Screen icon; one example of what was tapped and what happened (a screen recording is best); when it started; and to export a backup first and not delete the Home Screen app, which takes its log with it. **And for the owner's own iPhone:** checks 1, 9 and 10 in `README.md`, in Safari and from the Home Screen icon (9 is the iPhone Back path).

**Offered by the owner:** running the session on his Mac with Xcode. That would give the real thing: the iPhone Simulator runs iOS Safari, with Add to Home Screen, and Safari's Web Inspector attaches to it or to a phone on a cable.

**Parked meanwhile:** the polish pass asked for the same day (`/polish the whole app`). Its four read-only audits and a browser sweep are done and nothing was changed; the findings, unverified, are in `docs/polish-audit-2026-11-24.md`, and no polish goes into a fix for this.

**Handed to a session on the owner's Mac (2026-11-24):** the cloud session could not attach to the Mac (its folder is in the owner's Remote Control list but not offered to cloud sessions; cause not known). The steps for the Simulator are in `docs/handoff-2026-11-24.md`.

## Done: 1.18, last time's sets on the Today card (web; the Android card is not ported yet)
Asked by the owner on 2026-11-25, with a screenshot of the bodybuilding Today card: "where the stretch and squeeze instructions are I would like it to display what you achieved on this exercise the previous week instead. Same for powerlifting side." Then, on the design: "could it show all sets from the time the exercise was done previously?" Spec: `docs/superpowers/specs/2026-11-25-grit-last-time-on-today-design.md`; plan: `docs/superpowers/plans/2026-11-25-grit-last-time-on-today.md`.

**What the lifter gets (web app):**
- On the open Today card, between today's sets and the weight entry, a block headed **"Last time · Wed 18 Nov"** with **one row for every set of that session**, in the order logged: set number, weight × reps, "@ RPE" when one was logged, "Missed" on a missed set. A plate-counted set reads as a plate number.
- **On both sides.** The bodybuilding card had the Stretch and Squeeze lines there; the powerlifting card had nothing.
- **"Last time" is the latest earlier session on this side that has the exercise,** whatever its date (a missed week, or a lift done twice a week, still shows the most recent one). Each side shows its own sessions, as its coach reads them. The year is added to the date when it is not this year.
- An exercise never done on this side shows nothing there, and the card is shorter.
- The rows are read-only: quieter than today's rows, no tick, nothing to tap. Once the day's sets are done the block stays, above the button to the next exercise.
- **The Stretch and Squeeze lines are no longer on the Today card.** They are still in the exercise's ⓘ guide on the bodybuilding side.

**Owner decided:** the block replaces Stretch and Squeeze; it is on both sides; every set of the last session shows (not a one-line summary).

**My calls** (each said to the owner before building, except 4 to 6):
1. "Previous week" is read as the last session with the exercise, not the calendar week before. The date in the heading says which day it was.
2. The coach's box is left alone. On the bodybuilding side its sentence ("Last time: 26 × 11, 10, 9. Beat it.") shows the same numbers while the box is open, so they are on the card twice until it is applied or dismissed. That sentence is stored text that the app matches by pattern, so it was not touched.
3. Web first, Android after the owner has used it (the owner's rule of 2026-11-21).
4. Each set shows in the unit it was logged in (kg or plate number), even after the exercise's plate setting has changed. **This is the one case where the block and the coach's box can name different days:** just after a machine is switched between kg and plates, both coaches read only the sets in the new unit (`inCurrentUnits`), so their "Last time" can be an earlier session, while the block shows the session really done last. Otherwise they name the same session: each coach reads its own side's sessions before today, as the block does (checked in `Coach.targetsFor` and `BodybuildingCoach`). The powerlifting coach's sentence gives that session's top set only; the block gives every set.
5. The block has no inset at its sides, so its set numbers sit under today's (the Stretch and Squeeze lines were inset by 4 px).
6. The in-app guide's sample Today cards show no "Last time" block (their sample card has no earlier session to show), so on the bodybuilding side those two pictures lost their Stretch and Squeeze lines and gained nothing.

**How:**
- `domain`: `TrainingHistory.lastTime(exerciseId, mode, before)` and `lastTimes(mode, before)` in `history/LastTime.kt` are the one rule for "last time". `BodybuildingCoach.lastSets` now calls it (no change in what the coach says: its tests pass unchanged). The web gets `GritDomain.lastTimes`, one call for all the cards.
- Web: `TodayItem.lastTime` (`useToday.ts`), worked out from the history Today already holds and kept for as long as that history object, like the top sets. No new database read. `LastTime.tsx` draws it; `Cues` and its styles are gone from `ExerciseCard.tsx` and `today.css`.
- **Android app: the Today card is unchanged** and still shows Stretch and Squeeze on the bodybuilding side. It carries the shared rule and the version number only. Its port is the next step: the same field on `TodayItem` (`TodayViewModel.kt`) from `history.lastTimes(mode, date)`, and a block in `ExerciseCard.kt` in place of `MuscleCues`.
- No database, Dexie or backup change.

**Limits:**
- Until the Android port, the two apps differ on this card.
- With many sets last time the card is long: each set is a 24 px row, so eight sets put about 220 px above the weight entry.
- **Not checked on a phone,** and not in Safari.

**Tests:**
- Domain: `LastTimeTest` (6): none; the latest of several, every set in order; strictly before the day; its own side only, even when the other side is later; each set in its own unit; `lastTimes` agrees with `lastTime` for every exercise, two sessions on one day included. `GritDomainTest.lastTimes_oneEntryForEachExerciseDoneOnTheSide`.
- Web, `ExerciseCard.test.tsx`, "last time on the exercise card" (7), in place of the six Stretch and Squeeze tests: every set in order with what a screen reader hears; its own side and no Stretch or Squeeze; never done shows nothing and today's own sets are not "last time"; its place on the card before and after the day's sets are done; another year; a plate set beside a kg set; it follows the side of today's session.
- Web e2e, `bodybuilding.spec.ts`: "last time's sets sit between today's sets and the weight entry, unclipped at 130% zoom", with eight sets (one missed, one with no RPE), in place of the Stretch and Squeeze zoom test. The very narrow Today tests in `narrow.spec.ts` (320 px at 200% text, kg and plates, both sides) already had four weeks of history, so they now hold the block: they failed on the first layout (a row wider than the card) and pass now that a row's weight, reps and effort can each go to the next line.

**Checks, on the branch (`ccr-2f9842b5-owq43n`), 2026-11-25, one at a time:**
- Web: `npm run typecheck` clean; unit tests 1,687 of 1,687 (1,688 before: the eight Stretch and Squeeze tests on the card became seven); build clean.
- **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 512 passed, 12 skipped, 2 failed.** The two were `showcase.spec.ts`'s "showcase renders every component" (dark and light), with "connection refused" on port 4174: the run had reused a dev server left over from an earlier run that I had stopped part way, and that server went away. Not the app: `showcase.spec.ts` rerun alone, starting its own server, passed 3 of 3. **So there has been no single all-green full run of this code yet;** the run on merged main before a publish is one.
- **The `E2E_UA=android` pass** (CI's list): 254 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- Gradle: `:domain:allTests` passed; `:domain:checkWebDomain` passed. `:app:testDebugUnitTest`: passed, 1,115 tests, 0 failed, 1 skipped (counted from the result files).
- **Not run on merged main,** and **not checked on a phone or in Safari.**

**Shots:** from the full run, 32 tracked web shots were rewritten. 26 are committed, each changed by this release: the six Settings shots (the version line), the sixteen `today-*` shots that hold a card with an earlier session or had the Stretch and Squeeze lines, and the four bodybuilding guide pictures of the Today card (`guide-today-bodybuilding-*`, `guide-targets-bodybuilding-*`), which lost their Stretch and Squeeze lines. Six were put back as drift of a few bytes in pictures this release does not touch (`exercise-guide-actions-*`, `form-guide-light`, `guide-side-bodybuilding-light`, `guide-targets-powerlifting-*`). The 1.17.5 notes say `today-dark` is drawn a few pixels differently in this environment than in its committed shot; the committed `today-*` shots now carry whatever of that there is. Android: `:app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`, build successful; the six Settings shots changed (the version line) and no other tracked shot did.

**Version:** Android 1.18 (versionCode 34), web 1.18.0 (shown as 1.18). The Android app's screens are unchanged.

**1.18 is complete at commit `929b437`.** The branch carries 1.19 (History) after it. A "publish" of 1.18 alone merges that commit, not the branch tip.

## Done: 1.19, History on Progress (web; the Android app is not ported yet)
Asked by the owner on 2026-11-25: "lets also add a "History" button at the top right of the UI on the progress tab showing all previous workouts, times,dates,sets,reps,weight etc". Spec: `docs/superpowers/specs/2026-11-25-grit-workout-history-design.md`; plan: `docs/superpowers/plans/2026-11-25-grit-workout-history.md`.

**What the lifter gets (web app):**
- A **History** button at the top right of Progress, on both sides, there whether or not that side has anything logged.
- It opens **History**: every workout with at least one set, newest first, from both sides in one list. Each workout is a card: its date ("Tue 10 Nov 2026"), then its side, how many exercises and sets, and the bodyweight when one was logged that day; then each exercise in the order it was first logged, with every set of it (weight × reps, "@ RPE" when logged, "Missed", a plate number for a plate-counted set). The rows are the Today card's "Last time" rows.
- The count at the top ("35 workouts") is of the whole log. **The first 20 workouts are drawn; "Show older workouts" draws 20 more each time.**
- Read-only: nothing in the list can be tapped. Back returns to Progress.

**Owner decided: dates only, no times.** Grit has never stored a workout's time of day or its length, only its date. Recording them was offered (it would change the stored log and the backup file on both apps, and past workouts would still have no time); the owner chose to leave times out.

**My calls** (in the design the owner saw, except 3 to 5):
1. Read-only, both sides in one list, web first, its own version.
2. The first 20, then 20 more each time: a long log is never drawn whole on a phone.
3. Today's own session is in the list as soon as it has a set. A session with no sets is left out and not counted.
4. An archived exercise's sets still show, under its name.
5. No "Top set" or "PR" marks: the screen says what was done; Progress's lift histories mark the records.
6. The in-app guide's picture of Progress has no History button (the picture has nowhere to open it).
7. Beside the button, Progress's small caption wraps onto a second line when it has no room ("Best set per session" on a 320 px phone at 130% zoom was cut by 4 px before this), instead of being cut with dots.

**How:**
- `router.tsx`: a `history` page, `#/history`. `App.tsx` draws `WorkoutHistoryScreen` for it and gives Progress `onOpenHistory`.
- `state/useWorkoutHistory.ts`: `buildWorkoutHistory(history)` groups each session's sets by exercise, beside the lift and exercise histories; read live, like them.
- `ui/components/PastSets.tsx` (with `PastSets.css`): the "Last time" rows moved out of `LastTime.tsx` unchanged, same class names, so the Today block and the History list are one component.
- `screens/progress/HistoryButton.tsx` in both Progress headers; `WorkoutHistoryScreen.tsx`.
- **No database, Dexie or backup change. No change under `domain/`. The Android app has the version number only.**

**Limits:**
- No search, no filter by side or exercise, no editing or deleting from this screen.
- Until the Android port, History is in the web app only.
- **Not checked on a phone,** and not in Safari.

**Tests:**
- `useWorkoutHistory.test.ts` (5): newest first from both sides with side, counts and bodyweight; a session with no sets is no workout; an exercise come back to is one group; every kind of set as logged, an archived exercise under its name; a set of an exercise with no row.
- `WorkoutHistoryScreen.test.tsx` (6): the button opens the list and Back returns without leaving History behind; the button on both sides with nothing logged; opened directly; 45 workouts drawn 20 at a time; singular counts; loading.
- `router.test.tsx`: `#/history`, opened from the button in the whole app.
- e2e, `progress.spec.ts`: the button at the right of the title; 35 workouts, 20 drawn, then all; the cards' lines; Back three ways (the arrow, the phone's Back, opened directly). Two older checks there counted buttons whose name ends in "history" and now look for the lifts' tiles only. `narrowDeeper.spec.ts`: Progress with the button, and History with each side's story, at 320 px and 200% text. `progress.spec.ts` also holds the header at 246 px on both sides: nothing cut, the button beside the title.

**Checks, on the branch (`ccr-2f9842b5-owq43n`), 2026-11-25, one at a time, on the 1.19 code (which holds 1.18):**
- Web: `npm run typecheck` clean; unit tests 1,699 of 1,699; build clean.
- **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 519 passed, 12 skipped, 0 failed, first time,** with no source change while it ran. This is also the all-green full run that 1.18's own code had not had.
- **The `E2E_UA=android` pass** (CI's list): 257 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- Gradle: `:domain:allTests` passed; `:domain:checkWebDomain` passed. `:app:testDebugUnitTest`: passed, 1,115 tests, 0 failed, 1 skipped (counted from the result files).
- **After those runs, one small change from a review, then rerun:** Progress's caption now wraps beside the History button (call 7), with its test. On that final code: `progress`, `bodybuilding`, `narrow`, `narrowDeeper` and `guide` specs with `UPDATE_SCREENSHOTS=1`, 234 passed, 12 skipped, and no Progress or History shot changed; `E2E_UA=android` `progress` and `bodybuilding` specs, 36 passed; speed, 6 of 6; type check clean; unit tests 1,699 of 1,699; build clean. **The full Playwright run above was before this change;** the run on merged main before a publish covers it whole.
- **Not run on merged main,** and **not checked on a phone or in Safari.**

**Shots:** from the full run, 23 tracked web shots were rewritten and two are new. 14 are committed: the six Settings shots (the version line), the six Progress shots (the History button), and the new `history-dark` and `history-light`. Eleven were put back as drift in pictures this release does not touch (`exercise-guide-actions-*`, three `guide-side-first-launch-*`, `guide-side-powerlifting-light`, the four `guide-targets-*`, and `today-rest-light`, whose dark twin did not change). Android: `:app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`, build successful; the six Settings shots changed (the version line) and no other tracked shot did.

**Version:** Android 1.19 (versionCode 35), web 1.19.0 (shown as 1.19). The Android app's screens are unchanged.

**1.18 and 1.19 published together, 2026-11-25 (owner: "publish"), on main as `18f154f`** (a `--no-ff` merge of the branch at `5a46e6f`; its tree is the branch's). On merged main, one at a time, before the push, with no source change:
- Web: `npm run typecheck` clean; unit tests 1,699 of 1,699; build clean.
- **Full Playwright run: 521 passed, 12 skipped, first time** (519 on the branch, plus the two header tests added after it). So the header change has now been through the whole run.
- The `E2E_UA=android` pass (CI's list): 259 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- Gradle: `:domain:allTests` passed; `:domain:checkWebDomain` passed; `:app:testDebugUnitTest` passed, 1,115 tests, 0 failed, 1 skipped.
- Live, a few minutes after the push: `/` names `assets/index-CNAqNnza.js`, which carries 1.19.0 (no 1.18.0, no 1.17.5), build id `18f154f`, and the new screens' words ("Last time · ", "Show older workouts"); `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` (unchanged) and is served `cache-control: no-cache`.
- Android: `:app:assembleRelease` gives versionCode 35, versionName 1.19. **No APK was handed over.** The Android app from `main` shows 1.19 in Settings and is otherwise as 1.17.5: its Today card still has Stretch and Squeeze on the bodybuilding side, and its Progress has no History button.
- **Still not checked on a phone or in Safari:** the owner's check is item 10 of README's "Phone checks after a release"; the detailed ones are "Last time on the Today card (web, 1.18)" and "History on Progress (web, 1.19)" in `docs/manual-checks.md`.

**Open, next:** port both to the Android app once the owner has used them on the web. Last time: `TodayItem.lastTime` from `history.lastTimes(mode, date)` in `TodayViewModel.kt`, and a block in `ExerciseCard.kt` in place of `MuscleCues`. History: a screen beside `LiftHistoryScreen.kt` and a button in Progress's header.

## 1.19.1 (2026-11-25): Android, "Add exercise" drawn over the card just added

**The bug (owner, on the emulator):** on Today, after adding an exercise, the "Add exercise" button sat on top of the new card's heading. Each further add left it higher; after the second it was under the "Today" header.

**Cause:** the new card opens and pushes the button's place below the screen. The button starts its slide there, the list lets go of it at its next change, and what was left of the slide (about 900 px) stayed on the button when the list brought it back (`slidingReorder` in `LayoutMotion.kt`).

**Fix:** a slide cut short that way is over. Commit `12f2b3d` on the branch `fix/add-exercise-button-out-of-place`, with a test in `LayoutMotionTest` that failed before it. Android only: the web app's Today does not use this code (not run in a browser).

**Checked:** on the emulator (Medium Phone, Android 17), the bug twice with the old build, then two adds in a row with the fix, the button below the card both times. `:app:testDebugUnitTest` on the owner's Windows PC: 1,116 run, 1,096 pass, 19 fail, 1 skipped.

**The 19 fail on Windows only** (CI on Linux passes them), from backslashes in file paths, not from the app:
- All 18 of `MigrationTest`: `androidx.sqlite` 2.6.2's `SupportSQLiteDriver.open` compares the database's name after the last `/`, so a Windows path never matches. The upgrade tests therefore do not run on this PC.
- `NoVoltTest.scansTheAppSourcesAndTheDesignDoc`: it looks for paths ending `ui/theme/Color.kt`. A two-line fix, not made.

**On the owner's phone (OPPO CPH2913, Android 17), at the owner's request:** a signed release built from this branch, installed over 1.17.4 as an update (same signing key, data kept). It opened with no crash in the log; nothing was tapped and no screen was looked at. It reads 1.19 (versionCode 35), the same as main's 1.19, but has this fix and the build-tool update (AGP 9.5.2): that build had no version bump, so 1.19.1 (versionCode 36) is what installs over it.

**Version:** Android 1.19.1 (versionCode 36), web 1.19.1. The web app is unchanged but for its version line. The owner said "publish" on 2026-11-25.

**Also in this release (owner's commit `27aff59`):** release signing read from the git-ignored `keystore.properties` (no key or password is in the repository; without that file the release APK is unsigned, as before), `keystore.properties` in `.gitignore`, and AGP 9.5.0 to 9.5.2.

**Shots:** the six Settings shots of each app, re-shot on the Windows PC. The Android run rewrote only those six. The web run (`e2e/plan.spec.ts` alone, 11 passed) also rewrote four Plan shots, which differ only by this PC's drawing and were put back.

**The checks cannot all pass on this PC, so GitHub's run on the same commit stands in for them:**
- Kotlin: the 19 Windows-only failures above.
- Web unit tests: 49 of 1,699 fail here because this PC has Node 26, not 22: Node 26 has its own `localStorage`, which is undefined without `--localstorage-file` ("Cannot read properties of undefined (reading 'removeItem')"), and its dates read "Sept" where the tests expect "Sep". `npm run typecheck` and `npm run build` pass here.
- `gradle/gradle-daemon-jvm.properties` (made by Android Studio on this PC, Java 25 for the Gradle daemon) is deliberately not committed: CI uses Java 21.

**Noticed, not looked into:** on the emulator, exercises added on Today with no set logged were gone after the app restarted.

## Done: 1.20, last time's sets and History in the Android app
The owner, 2026-11-25, after installing the Android app: "its missing the update tab from progress?" 1.18 and 1.19 put both screens in the web app only (web first, by the owner's rule of 2026-11-21); this is their Android port. Specs: `docs/superpowers/specs/2026-11-25-grit-last-time-on-today-design.md` and `…-grit-workout-history-design.md`.

**What the lifter gets (Android app), the same as in the web app:**
- **Today:** on the open card, between today's sets and the weight entry, "Last time · Wed 4 Nov" and one row for every set of the exercise's last session on this side (weight × reps, "@ RPE" when logged, "Missed", a plate number for a plate-counted set). On both sides. Nothing there for an exercise never done on this side. **The Stretch and Squeeze lines are no longer on the card;** they stay in the ⓘ guide on the bodybuilding side.
- **Progress:** a **History** button at the top right, on both sides. It opens every workout with at least one set, newest first, both sides in one list: the date, the side, how many exercises and sets, the bodyweight when logged, then each exercise with every set. Read-only; Back returns to Progress.

**Where the Android app differs from the web, on purpose:**
- **No "Show older workouts".** The Android list is lazy (only what is on screen is drawn), so a long log needs no pages.
- The History button is 40 dp tall, as Today's Finish is (a compact button); the web's is 48 px.

**How:**
- `TodayItem.lastTime` (`TodayViewModel.kt`) from the shared `history.lastTimes(mode, date)`, asked once for all the cards, of the log as stored. `LastTimeBlock` in `ExerciseCard.kt` in place of `MuscleCues`.
- `ui/components/PastSets.kt`: the read-only rows, one composable for Today and History; each row speaks the web's sentence ("Set 1: 25 kg × 10 at RPE 8"). The weights end on one line and the "@" signs start on one; at a large font size a row's parts wrap whole.
- `WorkoutHistoryViewModel.build` mirrors the web's `buildWorkoutHistory`; `WorkoutHistoryScreen.kt`; `Routes.WORKOUT_HISTORY`, opened single-top so a double tap opens one screen; `ProgressContent(onOpenHistory = …)`, null in the guide's picture of Progress, which has no button.
- **No database or backup change. No change under `domain/` or `web/` but the web's version number.**

**My calls:**
1. Done now, before the owner has used the web versions for long: the owner installed the Android app and asked where History was.
2. The guide's sample Today cards show no "Last time" (they have no earlier session), as in the web app.
3. The screenshot samples (`Showcase.kt`, `BodybuildingShowcase.kt`) got a last session each, matching their coach's "Last time: …" sentence, so the committed Today shots show the block.

**Tests (Android):**
- `BodybuildingContentTest`: the five Stretch and Squeeze tests became five for the block: every set between the logged sets and the weight entry on both sides; nothing and no gap when never done; kept above the next button; another year and a plate set; a 320 dp phone at font scale 1.3 with eight sets.
- `TodayViewModelTest.lastTime_isEverySetOfTheExercisesLastSessionOnThisSide_andNeverTodaysOwn`.
- `WorkoutHistoryBuildTest` (6) and `WorkoutHistoryScreenTest` (6): the button opens the list and Back returns; the button on both sides with nothing logged; 45 workouts in one lazy list, the oldest reachable; singular counts; loading; a 320 dp phone at font scale 1.3, History and Progress's header with the button on both sides.

**Limits:**
- **Not run on a phone or an emulator from here:** the tests draw the screens in Robolectric. The owner's phone is the check.
- Left as it was: the 1.19.1 notes' "exercises added on Today with no set logged were gone after the app restarted" (noticed on the emulator, not looked into).

**Checks, on the branch (`ccr-2f9842b5-owq43n`, with main's 1.19.1 merged in), 2026-11-26, one at a time:**
- Gradle, on the final code: `:domain:allTests` passed; `:domain:checkWebDomain` passed; `:app:testDebugUnitTest` passed, 1,129 tests, 0 failed, 1 skipped (1,115 before, plus the new ones). `:app:assembleRelease` builds: versionCode 37, versionName 1.20, unsigned here (no `keystore.properties` in this container).
- Web (unchanged but for its version): `npm run typecheck` clean; unit tests 1,699 of 1,699; build clean. **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 521 passed, 12 skipped, first time.** The `E2E_UA=android` pass: 259 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- GitHub's run on main's `f79f111` (1.19.1), which that release's notes lean on: both jobs passed (read 2026-11-26).
- **Not run on merged main. Not run on a phone or an emulator:** the Android screens were drawn in Robolectric tests only.

**Shots:** Android, from `ScreenshotTest` with `-Pscreenshots.update=true`: the Today shots that hold a card with an earlier session or had Stretch and Squeeze, the six Progress shots (the History button), the four bodybuilding guide pictures of the Today card (Stretch and Squeeze gone), the new `history-dark` and `history-light`, and the six Settings shots (the version line). Web: the six Settings shots; five others were put back as drift.

**Version:** Android 1.20 (versionCode 37), web 1.20.0 (shown as 1.20). The web app is unchanged but for its version line.

**Published 2026-11-26 (owner: "Publish"), on main as `678747c`** (a `--no-ff` merge of the branch at `b9e49b0` onto main's `f79f111`; its tree is the branch's). On merged main, one at a time, before the push, with no source change:
- Web: `npm run typecheck` clean; unit tests 1,699 of 1,699; build clean. **Full Playwright run: 521 passed, 12 skipped, first time.** The `E2E_UA=android` pass: 259 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- Gradle: the three tasks first answered "up to date" in a second each, because the merged tree is the branch's tree, byte for byte, and they had passed on it. They were then made to run again (`--rerun`): `:domain:jvmTest` and `:domain:jsNodeTest` passed; `:app:testDebugUnitTest` passed, 1,129 tests, 0 failed, 1 skipped. `:domain:checkWebDomain` passed.
- Live, about two minutes after the push (the first look, a minute after, still had 1.19.1): `/` names `assets/index-QMXJxVef.js`, which carries 1.20.0 (no 1.19.1) and build id `678747c`; `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` (unchanged) and is served `cache-control: no-cache`.
- Android: `:app:assembleRelease` gives versionCode 37, versionName 1.20, unsigned in this container. **No APK was handed over:** the signed release is built on the owner's PC (`keystore.properties` is there), as 1.19.1 was.
- **Still not run on a phone or an emulator.** The owner's check is item 10 of README's "Phone checks after a release".

**Agreed next, in this order (owner, 2026-11-26):** (1) "Last time" you can act on: the row for the set about to be done highlighted, and a "Use set N" button that fills weight and reps (design sent, not yet confirmed); (2) a calendar in History; (3) warm-up sets; (4) supersets. Supersets change the stored log and the backup, so both apps ship together; warm-ups too if they are saved in the log.

## Done: 1.21, "Use set N" under Last time (both apps)
Picked by the owner on 2026-11-26 from a list of ideas ("1": make "Last time" something you act on). The idea as listed had a tap on a row copy it into the fields; the design sent instead was one button, and the owner did not object after being asked twice, so the button was built. **If the owner wants tappable rows after all, that is a change to make.**

**What the lifter gets (both apps):**
- While a card takes sets, **the row of "Last time" with the number of the set about to be logged is highlighted**: its figures in the full text colour, its number in the accent, and **the word "NEXT" after it** (on the powerlifting side the accent is a grey close to the other greys, so colour alone was too faint). A screen reader hears ", your next set" after it.
- Under the rows, **one button: "Use set 2: 25 kg × 9"**. It puts that row's weight and reps in the fields. The RPE stays as it was, and nothing is logged.
- After each set is logged, the highlight and the button move to the next row.
- **No highlight and no button** past the number of sets done last time, or once the day's sets are done (until "Add another set"). **Highlight but no button** when that set was logged in another unit than the exercise counts in now (a plate number never goes in a kg field).
- **Highlight but no button** also for a plate number off the machine's grid now (plate 7½ once the machine counts whole plates), as the coach's targets are held to.
- **A weight done last time as the set with the same number is no longer asked about as a "big jump".** The typo check holds a set back when the weight is more than 30% from the last one; last time's own ramp (100, 140, 170) does exactly that, so "Use set 2" then Log would have asked "is this right?" every time. Now it is logged at once. This holds however the weight got into the field (typed too). Any other jump is asked about as before, and so is a high rep count.

**Why a button and not tappable rows:** the rows are 24 px tall; a tap target must be 48. Making each row a target would double the block's height on the card.

**Both apps in one release.** From 1.21 the owner uses the Android app day to day and found 1.18/1.19 missing there, so features now go to both apps together unless the owner says otherwise (this sets aside the web-first rule of 2026-11-21; said to the owner on 2026-11-26).

**How:**
- Web: `PastSets` takes `next` (the row to mark); `LastTime` takes `next` and `onUse`; `ExerciseCard` passes how many sets are logged while the entry shows, and an `onUse` that calls the existing `actions.edit` with the row's weight and reps. No new state.
- Android: the same in `PastSets(next = …)`, `LastTimeBlock(next, onUse)` and `CardContent`, through the existing `TodayActions.onEdit`.
- No database, backup or `domain/` change.

**Tests:**
- Web, `ExerciseCard.test.tsx`: the row marked and the fields filled, nothing logged, then the next row after a set is logged; nothing marked past last time's sets or with the day's sets done, and the sixth row after "Add another set"; a set in another unit is marked without a button. Three earlier tests now expect ", your next set" on the first row. e2e `bodybuilding.spec.ts`: the marked row and the button at 130% zoom with eight sets, a full-size target inside the screen, and it fills the fields. The very narrow Today tests (`narrow.spec.ts`) hold the button and pass.
- Web, added after the review: the ramp (set 2 at 140 after 100 is logged at once; a typed 190 for set 3 is still held); a number typed and left in the field, then the button, behaves as Apply does; a plate number off the grid is marked without a button.
- Android, `BodybuildingContentTest`: the row marked and `onEdit` called with the row's weight and reps, nothing logged; no button past last time's sets, in another unit, or off the plate grid; the button and the longest label whole on a 320 dp phone at font scale 1.3. `TodayViewModelTest.aWeightDoneLastTimeAsTheSameSet_isNotHeldAsATypo_butAnotherJumpStillIs`.

**After the first check round, from a review, five changes, then the whole round again:** the word "NEXT" on the marked row; the "big jump" exemption above (the ramp test was seen to fail with the exemption switched off, then pass with it); the plate grid rule; the Android button's label may take two lines (on a 320 dp phone at font scale 1.3 "Use set 11: plate 12½ × 15" had lost its last three characters); and every part of an Android row on one baseline (the smaller "NEXT" had made its row taller).

**Checks, on the branch (`ccr-2f9842b5-owq43n`), 2026-11-26, one at a time, on the final code:**
- Web: `npm run typecheck` clean; unit tests 1,705 of 1,705 (1,699 before 1.21, plus six); build clean. **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 521 passed, 12 skipped, first time,** with no source change while it ran. The `E2E_UA=android` pass: 259 passed, 12 skipped.
- **Speed project alone, six times: five at 6 of 6, one at 5 of 6.** The miss was the Exercises tab's "settles within 100 ms" check, on a screen this release does not touch; it is the check the follow-up of 2026-11-15 measured as having no headroom (worst opens 96 to 108 ms). The machine's load average was about 3.6 on 4 cores at the time. Not chased further; the three runs on merged main before a publish stand.
- Gradle: `:app:testDebugUnitTest` passed, 1,134 tests, 0 failed, 1 skipped (1,129 before 1.21, plus five). `:domain:allTests` and `:domain:checkWebDomain` answered "up to date": nothing under `domain/` changed since they last ran and passed (1.20). `:app:assembleRelease` builds: versionCode 38, versionName 1.21, unsigned in this container.
- A ramp end to end in the browser, in a scratch test not kept: 100 logged, "Use set 2: 140 kg × 3", Log: logged with no dialog, and the button moved to set 3.
- **Not run on merged main. Not run on a phone or an emulator.**

**Shots:** web, from the full run: the six Settings shots (the version line); `today-bodybuilding-*` and `today-plates-bodybuilding-*` (the marked row, "NEXT" and the button); and `today-rest-*`, the same in two runs: the card is taller with the button under one row, so the shot is scrolled further down. Eight others were put back as drift of a few bytes. Android: `today-*` and `today-bodybuilding-*` (the powerlifting sample now has five sets last time, so its fourth row is marked) and the six Settings shots.

**Version:** Android 1.21 (versionCode 38), web 1.21.0 (shown as 1.21).

**Published 2026-11-26 (owner: "publish"), on main as `f654b48`** (a `--no-ff` merge of the branch at `c66674e` onto main's `678747c`; its tree is the branch's). On merged main, one at a time, before the push, with no source change:
- Web: `npm run typecheck` clean; unit tests 1,705 of 1,705; build clean. **Full Playwright run: 521 passed, 12 skipped, first time.** The `E2E_UA=android` pass: 259 passed, 12 skipped. **Speed project alone, three times: 6 of 6 each** (the Exercises tab check that missed once on the branch passed all three).
- Gradle, each made to run (`--rerun`), not answered from the branch's results: `:domain:jvmTest` and `:domain:jsNodeTest` passed; `:app:testDebugUnitTest` passed, 1,134 tests, 0 failed, 1 skipped. `:domain:checkWebDomain` passed.
- Live, about two minutes after the push: `/` names `assets/index-DjhOoHrI.js`, which carries 1.21.0 (no 1.20.0), build id `f654b48` and the words "Use set "; `sw.js` lists it, imports `sw-reload-clients.js?v=18e47dd61b2c` (unchanged) and is served `cache-control: no-cache`.
- Android: `:app:assembleRelease` gives versionCode 38, versionName 1.21, unsigned in this container. **No APK was handed over:** the signed release is built on the owner's PC.
- **Still not run on a phone or an emulator.** The owner did not say how 1.20 looked on the phone before saying "publish". The owner's check is item 10 of README's "Phone checks after a release".

**Next, agreed (owner, 2026-11-26), both apps together each time:** a calendar in History; warm-up sets (ask first: saved in the log, or a checklist that is not saved); supersets (owner decided: saved in the plan, linked once on the Plan tab; this changes the stored plan and the backup, so both apps ship together).

## Built, not published: 1.22, a calendar in History (both apps)
Picked by the owner on 2026-11-26 from the list of ideas ("2"), design sent the same day, owner: "Go".

**What the lifter gets (both apps), at the top of History, over the list:**
- **One month at a time, Monday to Sunday,** opening on this month: **a dot under every day with a workout,** today in a ring, the other days in grey.
- **A day with a dot is a button:** it brings that day's workout to the top of the list below. The list itself is as before: every workout, newest first.
- **Arrows** go back as far as the month of the first workout, and forward to this month.
- **Under the grid: "5 workouts in November · 12 weeks in a row".** The count is of the month on screen. The weeks in a row are calendar weeks (Monday to Sunday) with at least one workout, counted back from this week; a week in progress with none yet does not break the run (it is then counted back from last week). With no run, only the month's count shows.
- No calendar on an empty log (the empty state shows instead). Nothing new is stored.

**My calls** (the design the owner saw, except 3 to 7):
1. The list stays whole; a tap on a day jumps to its workout (the alternative offered, the list showing only the month on screen, was not asked for).
2. The streak counts weeks, not days: nobody lifts every day.
3. The weekday names are single letters (M T W T F S S), so seven columns fit any phone.
4. Only a day with a workout is a button; the others are numbers for the eye, hidden from a screen reader. A workout day reads "Tue 10 Nov 2026: Powerlifting · 1 exercise · 2 sets. Show it".
5. The jump is at once, with no animation, and adds no history entry: Back still leaves for Progress.
6. The dots are one colour for both sides (the workout's card says which side).
7. A workout dated after today (a wrong clock) is listed but opens no month ahead of this one.

**Limits, said to the owner in the report before publishing:**
- **Web: a tap on a day months back draws every workout down to it** (whole pages of 20, as "Show older workouts" would), which is what the pages were there to avoid; the calendar is then a long scroll above. The Android list is lazy and has neither cost.
- The calendar is tall: six rows of 48 px in a month like November 2026, before the first workout's card.
- Day buttons are 48 px tall everywhere. Their width is the screen's: 50 px or more from a 360 px phone up, 45 px on a 320 px phone, and 34 px on a 320 px phone at 130% zoom (measured on the web). Android, asserted in its tests: at least 48 dp wide at 360 dp, at least 44 dp at 320 dp.
- At 320 px and 200% text (about 160 px) the web calendar keeps inside the screen with its month name over the arrows and smaller figures; its columns are then about 20 px wide.

**How:**
- `domain`: `weeksInARow(workoutDays, today)` in `history/TrainingWeeks.kt`, exposed as `GritDomain.weeksInARow`; the generated web code is regenerated. Each app passes the dates of the workouts History lists, so the dots, the month's count and the run cannot disagree.
- Web: `screens/progress/WorkoutCalendar.tsx` (the month maths in UTC on the ISO strings, as `dates.ts`); `WorkoutHistoryContent` takes a required `today`, holds the pages shown and the picked workout, and after the render scrolls its card into view and puts the focus on its date. `workoutSummary` moved to its own file. Narrow rules in `narrowDeeper.css`.
- Android: `ui/progress/WorkoutCalendar.kt`, the first item of History's lazy list; a tap scrolls the list to the workout's item. `WorkoutHistoryContent` takes a required `today`. The list's side padding moved onto its items, so the calendar can reach closer to the screen's edge than the cards.
- No database or backup change.

**Tests:**
- Domain: `TrainingWeeksTest` (8): none; a run of three however many workouts a week has; this week empty does not break the run; this week and last both empty is zero; a missed week ends it; Monday to Sunday; across New Year; a day after today is not counted. `GritDomainTest.weeksInARow_…`.
- Web, `WorkoutHistoryScreen.test.tsx` (11, five new): the month, a button on each workout day, the ring, the counts; the arrows' limits; no run says only the month's count; a tap brings a workout up, also the 50th down the log, and again on a second tap; the month maths (a month starting on a Sunday, on a Monday, a leap February, across a year). e2e `progress.spec.ts`: the calendar at 375 px (30 day cells, all at least 47.5 px wide and inside the screen), a month back and a tap (the card at the top and focused), the first month and a tap past the first twenty (35 drawn), the address unchanged. `narrowDeeper.spec.ts`'s History tests hold the calendar at 160 px.
- Android, `WorkoutHistoryScreenTest` (18, six new): the same cases; a tap brings the 30th workout to the top of the lazy list; every day at least 48 dp each way and inside the screen at 360 dp; whole at 320 dp and font scale 1.3.

**Checks, on the branch (`ccr-2f9842b5-owq43n`), 2026-11-26, one at a time, on the final code:**
- Web: `npm run typecheck` clean; unit tests 1,711 of 1,711 (1,705 before, plus six); build clean. **Full Playwright run (iPhone path, with `UPDATE_SCREENSHOTS=1`): 521 passed, 12 skipped, first time,** with no source change while it ran. The `E2E_UA=android` pass: 259 passed, 12 skipped. Speed project alone, three times: 6 of 6 each.
- Gradle, each made to run (`--rerun`): `:domain:jvmTest` (988 tests, 0 failed) and `:domain:jsNodeTest` passed; `:app:testDebugUnitTest` passed, 1,141 tests, 0 failed, 1 skipped (1,134 before, plus seven). `:domain:checkWebDomain` passed. `:app:assembleRelease` builds: versionCode 39, versionName 1.22, unsigned in this container.
- Looked at, in scratch tests not kept: the web calendar's sizes at 390, 360, 320, 246 and 240 px (nothing cut, the page never wider than the screen); the light theme with today as a workout day (a button with the ring and the dot).
- **Not run on merged main. Not run on a phone or an emulator.** The month's name and the weekday letters come from the phone's own language data, which the tests here (English) do not prove for another language.

**Shots:** web, from the full run: `history-*` (the calendar), `showcase-*` (the icon list has the new left arrow) and the six Settings shots; five others were put back as drift of a few bytes. Android: `history-*` and the six Settings shots.

**Version:** Android 1.22 (versionCode 39), web 1.22.0 (shown as 1.22).

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
