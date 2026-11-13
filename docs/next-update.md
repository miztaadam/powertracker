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
- The warm-up: when the tab first shows its list, the worker works out every catalogue thumbnail in the list's order, once per process. The picker starts no thumbnail work.
- The Android speed check is a test that no thumbnail is worked out on the main thread (`ThumbnailsTest`).
- Not measured: no real phone was available, so frame times before and after are unknown. Check by scrolling the Exercises tab fast right after opening the app.

## Follow-up (2026-11-13): the picker speed check has no headroom
- The web app's exercise picker is held to 100 ms by two end-to-end checks (`web/e2e/library.spec.ts`, tagged @speed). On the build machine they land between about 85 and 118 ms, so a run misses now and then.
- Measured on a quiet machine, the same checks alternating between the build before bodybuilding mode and the build with it: 11 of 12 runs passed before, 10 of 12 after (misses at 110 ms before; 105 and 100 ms after). The two builds are not measurably different; the check was already at its limit.
- Worth doing as its own task: find where the picker's time goes when it opens with the whole catalogue (225 rows, or 475 with a lifter's own), and either make it faster or agree a limit with the owner that reflects a real phone. Do not loosen the check without the owner's say.
