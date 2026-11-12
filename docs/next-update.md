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

## Next project idea (owner, 2026-11-12): Bodybuilding mode
- A single toggle (Settings) that switches the app, **including the coach**, to bodybuilding-style training.
- First thoughts (to brainstorm with the owner, not decided):
  - **Coach goal:** muscle growth, not a heavier top set. Weekly sets per muscle, rep ranges (e.g. 6–12, 10–15), rep progression before load, proximity to failure.
  - **Stall advice:** a muscle that isn't progressing gets more weekly sets, a different exercise for it, or a deload. Miss-point accessories don't apply.
  - **Progress:** weekly sets per muscle group and per-exercise rep/load trends.
  - **Plan:** organised by body part or a push/pull/legs split. It builds on the 225-exercise library and the muscle groups.
- **The UI must visibly change when the mode is on** (owner request). Options to mock up:
  - a second accent colour;
  - a mode label;
  - screens built for the mode.
  Fable mocks these up and the owner picks.
- Logged data stays the same in either mode.
