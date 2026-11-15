# Bodybuilding Mode, Stage 4 Implementation Plan: stretch and squeeze cues

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every catalogue exercise gets two short lines, where the target muscle is stretched and where it is squeezed, or one fixed line saying there is none (a hold, a carry). They show on the Today exercise card on the bodybuilding side and in the form guide.

**Architecture:** The lines live in the shared Kotlin domain beside the catalogue, in their own files (one per body area), keyed by catalogue name, so the area files and the catalogue objects the picker loads do not change. Both apps ask for an exercise's lines by name, the way they ask for its figure. No storage, no backup change.

**Tech Stack:** Kotlin Multiplatform (`domain/`, JS bridge), Android Compose, React, Robolectric, vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-13-grit-bodybuilding-mode-design.md` §8 "Stage 4", with §3 and §9. Stages 1–3 are merged (their plans are beside this one). The form guide content rules are in `docs/superpowers/plans/2026-11-11-grit-exercise-library.md` (Global Constraints).

## Owner decisions needed before building

1. **Where the lines show.** The spec says "in the form guide on both sides", but the standing rule is that the powerlifting side does not change while bodybuilding mode is off. **Recommended: the bodybuilding side only** (the Today card, and a "Stretch and squeeze" section in the form guide while bodybuilding mode is on). The other choice is the guide on both sides, as the spec says.
2. **The mockup's line will not ship.** The approved mockup's dumbbell bench line, "Elbows drop below the bench at the bottom", goes deeper than Grit's own guide, which lowers the dumbbells "to the sides of your chest". The lines never ask for more range than the guide (rule F2 below).
3. **The lines are derived, not quoted.** The pilot (below) found that the sources often say where a muscle is stretched ("lower until chest muscles are stretched"), but no source says where to squeeze. The squeeze line is worked out from the end of the guide's lifting step and the muscle being trained. No source is claimed for a benefit of squeezing; the owner's focus is the requirement.
4. **The designer's numbers and wording (not sourced):** each line at most 64 characters; the fixed line for an exercise with none: **"No stretch or squeeze to aim for: this one is held, not moved."**

## Global Constraints

- Everything in stages 1–3 Global Constraints still holds: plain words; no model names in code or commit bodies other than the attribution trailer; new commits only, never amend, never push, never touch `main`; stage files by path; never commit `.claude/`, `CLAUDE.md` or `.superpowers/`; never `playwright install`; e2e with `--trace off`; after a domain change run `./gradlew :domain:syncWebDomain :domain:checkWebDomain` and commit the bundle (revert reorder-only noise in `web/src/domain/generated/` with `git checkout --`).
- **Allowed sources only:** acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport. Search them with WebSearch and `allowed_domains`; never fetch them. Never forums (ExRx forum threads are forums), blogs or AI-written pages.
- **The idea's sources** (re-checked by Task 1 and recorded in `Sources.kt` as general sources, not shown in the app):
  - Maeo et al. 2021, "Greater Hamstrings Muscle Hypertrophy but Similar Damage Protection after Training at Long versus Short Muscle Lengths", https://pmc.ncbi.nlm.nih.gov/articles/PMC7969179/
  - "Lengthened partial repetitions elicit similar muscular adaptations as full range of motion repetitions during resistance training in trained individuals", https://pmc.ncbi.nlm.nih.gov/articles/PMC11829627/
  - ACE, "Muscles in Motion" (a shortening muscle is the one doing the lifting), https://www.acefitness.org/fitness-certifications/ace-answers/exam-preparation-blog/3580/muscles-in-motion/
  - Nothing is claimed for squeezing. The attentional-focus reviews found (PMC8622562, PMC8751186) do not support a hypertrophy benefit; do not cite them as if they did.
- **The rules for a line (each batch writes to them; each review checks them):**
  - **F1 Muscle.** A line names a muscle from the exercise's own `muscles` list, normally its first. Plain names: "chest", "lats", "quads", "hamstrings", "glutes", "calves", "side delts", "biceps", "triceps".
  - **F2 Range.** The stretch line is the point in the guide's own steps where that muscle is longest; it never asks for more range than the guide's steps and mistakes allow (no "below the bench" where the guide says "to the sides of your chest", no "lock out" where the guide says not to). Where a source says "until … stretched", use that point, as far as the guide allows.
  - **F3 Squeeze.** The squeeze line is the end of the guide's lifting step, where that muscle is shortest. No pause length, no number, no claim of a benefit.
  - **F4 Safety.** Nothing that contradicts the guide's setup, cues, mistakes or safety notes; no pushing into pain; no "as far as you can" on a loaded spine.
  - **F5 None.** Only exercises held or carried (planks, carries, holds, anti-rotation holds) get the fixed line instead. An exercise that moves always gets two lines.
  - **F6 Shape.** Each line one sentence, at most 64 characters, starting with a capital, ending with a full stop, no jargon ("eccentric", "concentric", "peak contraction" are out).
  - **F7 Sources.** Each exercise's lines list at least one `SourceId`, normally the ExRx or ACE page already listed for its guide, plus a general source where the line rests on the idea rather than the page.
- **Pilot samples (to be checked again in their batch; the owner sees these with the plan):**
  - DB bench: Stretch "Your chest stretches as the dumbbells reach your chest." Squeeze "Squeeze your chest as you press the dumbbells up."
  - DB fly: Stretch "Your chest stretches as the dumbbells reach shoulder level." Squeeze "Squeeze your chest as the dumbbells come together." (ExRx DBFly: "lower … until chest muscles are stretched"; "until nearly touching".)
  - RDL: Stretch "Your hamstrings stretch as your hips go back." Squeeze "Squeeze your glutes as you stand tall." (ExRx RomanianDeadlift: "until hamstring is stretched fully".)
  - Standing calf raise: Stretch "Your calves stretch as your heels drop." Squeeze "Squeeze your calves with your heels as high as they go." (ExRx: "lower heels … until calves are stretched"; "as high as possible".)
  - Lat pulldown: Stretch "Your lats stretch with your arms straight above you." Squeeze "Squeeze your lats with the bar at your upper chest."
  - Leg extension: Stretch "Your quads stretch with your knees bent at the start." Squeeze "Squeeze your quads as your legs straighten."
  - Plank: the fixed line.
- **Speed:** the catalogue objects (`grit.library()`) do not grow. The lines come from their own bridge call, asked for one exercise at a time. The web speed checks are run before Task 1 and after it (see the 2026-11-15 note in `docs/next-update.md`); a miss during this stage is re-run alone on a quiet machine before anything is changed, and the checks are never loosened.
- **The powerlifting side** follows the owner's decision 1. With bodybuilding mode off and decision 1 as recommended, every powerlifting screen and screenshot is unchanged.
- **No storage or backup change.** App version 1.5 on both apps (last task).

## Review Focus

1. **A line asks for more range than the guide** (the mockup's "below the bench"). The batch review checks every stretch line against F2; Task 1 adds the test `focus_neverMentionsBelowTheBench`, which is a tripwire only.
2. **Large text.** At Android font scale 1.3 on a 360 dp phone and 130% zoom on a 375 px phone, the Today card's two lines wrap and nothing is clipped. Pinned in Tasks 2 and 3.
3. **The lifter's own exercise, or a stored row under a catalogue name.** A custom exercise shows no lines and leaves no gap; a row restored from a backup under a catalogue name shows that name's lines, matched the way its guide is (ignoring case). Pinned in Tasks 2 and 3.
4. **The powerlifting side** (decision 1 as recommended): the Today card and the guide sheet show no lines with bodybuilding mode off, and switching the mode shows or hides them at once without a reload. Pinned in Tasks 2 and 3.
5. **The picker and the Exercises tab stay as fast:** `grit.library()` objects carry no lines, and the speed project passes before and after Task 1.

---

## Task 1: Domain model, bridge, generated docs and the main lifts

**Files:**
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/library/MuscleFocus.kt`
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/library/focus/MainLiftsFocus.kt` (one file per area later: `ChestFocus.kt`, `BackFocus.kt`, `ShouldersFocus.kt`, `ArmsFocus.kt`, `QuadsFocus.kt`, `HamstringsGlutesFocus.kt`, `CalvesCoreFocus.kt`)
- Modify: `Library.kt` (lookup), `Sources.kt` (the idea's sources), `domain/src/jsMain/.../web/GritDomain.kt` and `JsTypes.kt`, `domain/src/jvmTest/.../library/SourcesDocTest.kt` (the lines go into `docs/library/sources.md` and `docs/library/guides.html`)
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/library/MuscleFocusTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface MuscleFocus { val sources: List<SourceId> }` with `data class Lines(val stretch: String, val squeeze: String, override val sources: List<SourceId>)` and `data class NoFocus(override val sources: List<SourceId>)`.
  - `Library.NO_FOCUS_LINE = "No stretch or squeeze to aim for: this one is held, not moved."`
  - `Library.focus(name: String): MuscleFocus?`, matched like `Library.find` (ignoring case); null for a name not in the catalogue or not written yet.
  - JS: `@JsExport data class JsMuscleFocus(val stretch: String?, val squeeze: String?, val none: String?)` (exactly one of `none` or the pair is set) and `GritDomain.muscleFocus(name: String): JsMuscleFocus?`. `JsLibraryExercise` is unchanged.

- [ ] **Step 1:** Run the web speed project on a quiet machine three times (`./gradlew --stop; pkill -f kotlin-daemon`, wait 60 s, `cd web && npx playwright test --project=speed --no-deps --trace off`). Record the times in the report.
- [ ] **Step 2: Failing tests** in `MuscleFocusTest`:
  - `focus_namesOnlyCatalogueExercises`: every key in every focus map is a catalogue name, spelled exactly, and appears once.
  - `focus_linesHaveTheirShape`: every `Lines` line is non-blank, at most 64 characters, starts with a capital and ends with "."; `NO_FOCUS_LINE` is as written above.
  - `focus_hasSources`: every entry has at least one source, each a key of `Sources.all`.
  - `focus_neverMentionsBelowTheBench`: no line contains "below the bench".
  - `focus_mainLiftsAreAllWritten`: every `BodyArea.MAIN_LIFTS` exercise has an entry.
  - `focus_findsByNameIgnoringCase`: `Library.focus("dB FLY")` equals `Library.focus("DB fly")` once Chest is written; for now test it with `"SQUAT"` and `"Squat"`; `Library.focus("My own lift")` is null.
- [ ] **Step 3:** Implement the model, the lookup (one map joined from the area maps) and the bridge. Re-check the idea's sources with WebSearch (allowed domains) and add them to `Sources.kt`.
- [ ] **Step 4: Main lifts content.** For each `MAIN_LIFTS` exercise, research and write its lines to rules F1–F7. Keep a research log in the report (exercise, the query, the URL, the sentence that supports the stretch point).
- [ ] **Step 5:** Extend the sources and guides writers so each exercise shows its two lines (or the fixed line) and their sources. Regenerate: `./gradlew :domain:jvmTest --tests '*SourcesDocTest*' -Psources.update=true`.
- [ ] **Step 6:** `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`. Expected: PASS.
- [ ] **Step 7:** Run the speed project three times again on a quiet machine and record the times beside Step 1's. A miss is re-run alone before anything else is done.
- [ ] **Step 8: Commit** `feat(domain): stretch and squeeze lines, with the main lifts`.

**Review:** the domain review checks the model and bridge, and the content review (below) checks the main lifts' lines.

## Task 2: Android: the Today card and the guide sheet

**Files:**
- Modify: `app/src/main/kotlin/com/powertracker/ui/today/ExerciseCard.kt`, `app/src/main/kotlin/com/powertracker/ui/library/FormGuideSheet.kt` (and its callers only as far as they must pass the side)
- Test: the existing `ExerciseCard` and `FormGuideSheet` Robolectric test files, plus screenshots under `docs/design/screens/`.

**Interfaces:** consumes `Library.focus(name)` and `Library.NO_FOCUS_LINE`.

- [ ] **Step 1: Failing tests:**
  - `bodybuildingCard_showsStretchAndSqueeze`: a bodybuilding Today card for "Squat" shows "STRETCH" with its stretch line and "SQUEEZE" with its squeeze line, between the logged sets and the weight entry (as the mockup).
  - `bodybuildingCard_noFocus_showsNothing`: for an exercise with `NoFocus`, and for a custom exercise, the card shows neither label and leaves no gap.
  - `powerliftingCard_showsNoCues`: the same exercise on the powerlifting side shows neither label.
  - `guideSheet_bodybuilding_hasStretchAndSqueeze`: with bodybuilding mode on, the sheet has a "Stretch and squeeze" section after "Cues", with both lines, or `NO_FOCUS_LINE`; with it off, no such section (decision 1 as recommended; if the owner chooses both sides, the section shows on both and this test says so).
  - `guideSheet_restoredRowUnderCatalogueName_showsLines`: a stored row named "squat" (lower case) shows Squat's lines.
  - `bodybuildingCard_largeFont_wrapsWithoutClipping`: at font scale 1.3 on a 360 dp screen, both lines are fully shown.
- [ ] **Step 2:** Implement. The labels use the existing eyebrow style; the lines use body text (≥ 16 sp). Read the side the way the card already does (`side`); the sheet reads the training mode the way the mode label does.
- [ ] **Step 3:** Update only the screenshots that change: the bodybuilding Today screen (dark and light) and a bodybuilding guide sheet. Powerlifting screenshots must not change.
- [ ] **Step 4:** Focused tests, then `./gradlew :domain:checkWebDomain :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(app): stretch and squeeze on the bodybuilding Today card and guide`.

## Task 3: Web: the Today card and the guide sheet

Runs beside Task 2. Never runs `syncWebDomain`: it uses the bundle Task 1 committed.

**Files:**
- Modify: `web/src/screens/today/ExerciseCard.tsx`, `web/src/screens/library/FormGuideSheet.tsx` (and their callers only as far as they must pass the side), the matching CSS.
- Test: `ExerciseCard` and `FormGuideSheet` vitest files; one e2e check in `web/e2e/` at 130% zoom; screenshots under `docs/design/web/screens/`.

**Interfaces:** consumes `grit.muscleFocus(name)` (wrap it once in `web/src/domain` like `figureInfo`).

- [ ] **Step 1: Failing tests:** the web twins of Task 2's six tests, with the same names in camel case and the same assertions; the large-text one is an e2e check at 130% zoom on a 375 px phone that nothing in the card is clipped (use the existing `overflowing` pattern from `e2e/library.spec.ts`).
- [ ] **Step 2:** Implement to match Android's order and wording.
- [ ] **Step 3:** Update only the changed screenshots (`UPDATE_SCREENSHOTS=1` limited to those specs). `form-guide-{dark,light}.png` change for real only if the sheet they show is on the bodybuilding side; otherwise they must be unchanged, and any invisible-noise rewrite of them is reverted, never committed.
- [ ] **Step 4:** Focused tests, then `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(web): stretch and squeeze on the bodybuilding Today card and guide`.

## Tasks 4–10: Content batches, one per body area

| Task | File | Exercises (as now in the catalogue) |
|---|---|---|
| 4 | `focus/ChestFocus.kt` | Chest |
| 5 | `focus/BackFocus.kt` | Back |
| 6 | `focus/ShouldersFocus.kt` | Shoulders |
| 7 | `focus/ArmsFocus.kt` | Arms |
| 8 | `focus/QuadsFocus.kt` | Quads |
| 9 | `focus/HamstringsGlutesFocus.kt` | Hamstrings and glutes |
| 10 | `focus/CalvesCoreFocus.kt` | Calves and core |

The batches run one at a time (they all regenerate `sources.md`, `guides.html` and the web bundle), and may run while Tasks 2 and 3 are being built.

**For each batch:**
- [ ] **Step 1: Research** each exercise in the allowed sources (WebSearch, `allowed_domains`). Log the query, the URL and the supporting sentence for each stretch point.
- [ ] **Step 2: Write** its lines to rules F1–F7, reading the exercise's own guide first.
- [ ] **Step 3:** Add a test line to `MuscleFocusTest`: `focus_<area>IsAllWritten` (every exercise of that area has an entry).
- [ ] **Step 4:** Regenerate the docs, then `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(domain): stretch and squeeze lines for <area>`.

**Content review (each batch, and Task 1's main lifts):** an independent reviewer re-checks every line against its exercise's guide and sources, using WebSearch restricted to the allowed domains. It flags: a line that breaks F1–F7; a stretch point the guide does not reach; a squeeze that is not the end of the guide's lifting step; anything unsafe; a disallowed source; jargon.

## Task 11: Finish: every exercise written, version 1.5, notes

**Files:** `MuscleFocusTest.kt`, `app/build.gradle.kts`, `web/package.json`, `docs/next-update.md`, `README.md`, Settings screenshots on both apps.

- [ ] **Step 1:** Replace the per-area tests with `focus_everyCatalogueExerciseIsWritten` and make `Library.focus` non-null for every catalogue name. Run `./gradlew :domain:allTests`. Expected: PASS.
- [ ] **Step 2:** Version 1.5 on both apps (Android versionName 1.5 / versionCode 6; web 1.5.0) and the Settings screenshots that show it.
- [ ] **Step 3:** `docs/next-update.md`: a stage 4 section (what the lifter gets; the idea's sources; that the lines are derived and how; the designer's 64-character cap and fixed line; phones untried). README: one line in the manual phone checklist ("read the stretch and squeeze on a bodybuilding Today card").
- [ ] **Step 4:** The full run: `./gradlew :domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest`, and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: PASS.
- [ ] **Step 5: Commit** `chore: version 1.5; stretch and squeeze notes`.

Then: the whole-branch check, quiet speed runs, the owner's read of `docs/library/guides.html` (published as a private page), and the owner's yes before any merge.
