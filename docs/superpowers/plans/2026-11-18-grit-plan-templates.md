# Plan templates (1.14): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** About 3 ready-made weekly plans on each side, set by days a week. A lifter picks one from an empty plan or the Plan menu, adjusts it in a draft (weekdays, Swap), and saves it as that side's plan.

**Architecture:**
- **Shared domain:** templates are fixed data. `PlanTemplates` turns one into the builder's existing `PlanDraft` and reuses `PlanBuilder.swap` and its volume count. Both apps get them through the regenerated web package.
- **Each app:** a templates screen (list, then draft) that reuses the bodybuilding builder's draft UI, generalised for powerlifting's fixed reps. Saving goes through the existing replace-plan path and sets that side's training days.

**Tech Stack:** Kotlin Multiplatform domain (`web/src/domain/generated`, checked by `:domain:checkWebDomain`), Android Compose and Room, React 18 and Dexie.

**Spec:** `docs/superpowers/specs/2026-11-18-grit-plan-templates-design.md`, approved 2026-11-18 ("Looks good"). Its section "Settled after the builder check" is binding.

## Global Constraints
- **Allowed sources only,** for every set, rep, RPE, days-a-week, sessions-per-lift and "who it suits" line: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport. Search with WebSearch, `allowed_domains` limited to those. Cite each in `coach/CoachSources.kt`, saying it was checked from the search summary.
- **The designer's own choices** (template names, day titles, which exercise goes on which day) are listed as such in the notes for the owner.
- **No named programs:** names describe the template, for example "4-day upper/lower".
- **Exercises:** catalogue exercises only.
- **A lift on more than one day has the same sets × reps @ RPE on every day it appears.**
- **Unchanged:** Build my plan and its output, every coach rule, adding exercises by hand, the backup format, and the picker's and Today's speed checks.
- **Version:** 1.14 on both apps, with Android versionCode 23 and versionName "1.14"; the web shows 1.14.0.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only, never amend;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`, and never `playwright install`;
  - existing tests pass unedited, or each change is listed with its reason.
- **Commit trailer:**
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
  `Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy`

## Review Focus
1. **Replacing a plan** touches only that side. History, known bests, check-ins, skips (their plan item set to null) and the other side's plan and training days are unchanged. Cancel on the confirm stores nothing.
2. **Weekdays:** choosing fewer or more days than the template has can't be saved. The saved items land on the chosen weekdays, in template order, and the side's training days become exactly those days.
3. **Main lifts and variations on powerlifting:** main lifts have no Swap, and a variation swaps only to variations of the same `parentLift`. Strict press shows only when `showStrictPress` is on.
4. **Powerlifting items save with fixed reps** (`repsHigh` null), bodybuilding items with their range. Each app's coach then gives no target to a new lifter and the right target with history, for a lift on two days too.
5. **Both apps show the same templates, drafts and swap lists,** from the shared domain.

---

### Task 1: Research the template contents (nothing committed)
**Files:** report only: `.superpowers/sdd/2026-11-16-grit-look/templates-task-1-report.md`.

- [ ] **Search the allowed sources** for:
  - days a week by experience (leads: the ACSM 2009 progression models via ExRx "Weight Training Guidelines"; NSCA "Determination of Resistance Training Frequency");
  - powerlifting main-lift sets, reps, RPE or %1RM and sessions per lift each week (lead: PMC8435792, minimum effective dose in powerlifters);
  - **fixed rep counts for powerlifting accessories;**
  - bodybuilding splits for advanced lifters (ACSM: 1–3 muscle groups per session).
  Record every query, the key result text and the URL.
- [ ] **Propose the six templates in full:**
  - each one's name, days, "who it suits" line, and each day's title and exercises (catalogue names) with sets × reps @ RPE;
  - each number tied to its source, or marked as the designer's.
  - **Bodybuilding** meets the spec's settled rule 3: big groups ≥10 counted weekly sets over ≥2 days, no group over `PhysiqueCoach.WEEKLY_CEILING`. Count it the way `PlanBuilder`'s volume counts.
  - **Powerlifting** meets the sourced sets and sessions per lift, and marks which entries appear only with strict press on.
- [ ] **Stop and report if:**
  - no allowed source gives a fixed accessory rep count;
  - a 5-day bodybuilding split can't meet rule 3;
  - any template can't be sourced.
  The owner decides these (spec, settled rule 2). Don't commit anything.

### Task 2: Templates in the shared domain
**Files:**
- Create `domain/src/commonMain/kotlin/com/powertracker/domain/plan/PlanTemplates.kt`.
- Modify `coach/CoachSources.kt` (new sources), `domain/src/jsMain/.../web/GritDomain.kt` (exports) and the regenerated `web/src/domain/generated`.
- Test: `domain/src/commonTest/.../plan/PlanTemplatesTest.kt`.

**Interfaces (produces):**
```kotlin
data class TemplateExercise(val name: String, val sets: Int, val repsLow: Int, val repsHigh: Int?, val rpe: Double, val onlyWithStrictPress: Boolean = false)
data class TemplateSession(val title: String, val exercises: List<TemplateExercise>)
data class PlanTemplate(val id: String, val mode: TrainingMode, val name: String, val suits: String, val sessions: List<TemplateSession>) { val dayCount: Int get() = sessions.size }
object PlanTemplates {
    fun forMode(mode: TrainingMode): List<PlanTemplate>                        // fewest days first
    fun byId(id: String): PlanTemplate?
    fun suggestedDays(count: Int, current: Set<DayOfWeek>): List<DayOfWeek>    // current when its size == count, else a fixed spread
    fun draft(template: PlanTemplate, days: List<DayOfWeek>, strictPress: Boolean): PlanDraft  // require(days.size == dayCount, distinct)
    fun alternatives(template: PlanTemplate, draft: PlanDraft, day: DayOfWeek, name: String): List<LibraryExercise>
    fun swap(draft: PlanDraft, day: DayOfWeek, from: String, to: String): PlanDraft   // PlanBuilder.swap
}
```
- **The draft:**
  - `days` are sorted Monday first, and session i goes on day i;
  - each `DraftExercise` carries the catalogue muscles, compound flag and `mainGroup`, so the volume is counted by `PlanBuilder`'s rules;
  - on powerlifting `repsHigh = repsLow`;
  - entries with `onlyWithStrictPress` are dropped when `strictPress` is false.
- **`alternatives`:**
  - empty for a main lift (category SQUAT, BENCH, DEADLIFT or STRICT_PRESS);
  - for a VARIATION, the other catalogue variations with the same `parentLift` not already on that day;
  - otherwise `PlanBuilder.alternatives` with no picks, **minus** main lifts, variations and any exercise already elsewhere in the week (an offered swap must never break the same-numbers rule);
  - every `DraftExercise` gets a non-null `slot` (via `PlanBuilder.kindOf`), so `alternatives` doesn't fall back to its wider same-main-group branch.
- **JS exports,** in the existing style of the `planBuilder*` exports:
  - `planTemplatesFor(mode)`, `planTemplateSuggestedDays(count, current)`, `planTemplateDraft(id, days, strictPress)`, `planTemplateAlternatives(id, draft, day, name)` and `planTemplateSwap(draft, day, from, to)`;
  - the draft travels as the existing `JsPlanDraft`.

- [ ] **Write the failing tests:**
  - every template's exercises are in the catalogue;
  - `forMode` gives Task 1's templates in day order;
  - each bodybuilding template's `draft(...).volume` meets settled rule 3;
  - each powerlifting template meets its sourced weekly sets and sessions per lift, counting competition lifts only;
  - a lift on two days has equal sets, reps and RPE on both;
  - `draft` rejects the wrong number of days and duplicate days;
  - `suggestedDays(3, {MON, WED, FRI}) == [MON, WED, FRI]`, and `suggestedDays(4, {MON, WED, FRI})` is the fixed spread;
  - strict-press entries appear only with `strictPress = true`;
  - `alternatives` is empty for squat, gives only same-`parentLift` variations for a variation, and gives the builder's list for an accessory;
  - `swap` keeps sets, reps and RPE;
  - for every template and every exercise, no offered alternative is a main lift or variation (unless the exercise is a variation), and after any offered swap a lift on two days still has equal sets, reps and RPE.
- [ ] Run `./gradlew :domain:allTests` and see them fail.
- [ ] Implement `PlanTemplates` with Task 1's agreed contents, add the sources and the exports, and regenerate the web package.
- [ ] Run `./gradlew :domain:allTests :domain:checkWebDomain` and see them pass.
- [ ] Commit `feat(domain): plan templates`.

### Task 3: Android: choose a template
**Files:**
- Create `app/src/main/kotlin/com/powertracker/ui/plan/templates/TemplatesScreen.kt` and `TemplatesViewModel.kt`.
- Modify `ui/plan/PlanScreen.kt` and `PlanViewModel.kt` (empty-plan button, menu item), `ui/AppShell.kt` (the route), and `ui/plan/builder/PlanBuilderScreen.kt` (pull its draft composables out for reuse, with no visible change to the builder).
- Tests: `app/src/test/.../ui/plan/TemplatesScreenTest.kt` and `TemplatesViewModelTest.kt`.

**Interfaces:**
- **Consumes:** Task 2's `PlanTemplates`, and `TrainingRepository.replacePlan(mode, items)`.
- **Produces:** the route `templates/{mode}`. `TemplatesViewModel(mode)` has the steps `LIST` and `DRAFT`, and the actions `choose(id)`, `toggleDay(day)`, `alternatives(day, name)`, `swap(day, from, to)`, `use()`, `confirmReplace()`, `cancelReplace()` and `back(): Boolean`. Its state is `templates`, `chosen`, `days`, `draft`, `canUse`, `confirming`, `saving`, `saved` and `failed`.
- **Saving:**
  - catalogue items are saved by name;
  - powerlifting items save `repsHigh` null;
  - the side's training days are set to the chosen days (find how the builder's `setDays` reaches settings, and give it the mode).
- **Strings:**
  - "Choose a template";
  - the list row: "<name>", "n days a week · <suits>", and the day titles joined with " · ";
  - "Use this plan";
  - the confirm: "Replace your powerlifting plan?" or "Replace your bodybuilding plan?", then "Your history and bests are kept.", with "Replace" and "Cancel";
  - the snackbar "Plan set: <name>."
- **Powerlifting empty plan:** the line "Your plan is empty. Start from a template or add exercises to each day." over **Choose a template**.

- [ ] **Write the failing tests:**
  - an empty plan shows **Choose a template** on both sides, and on bodybuilding **Build my plan** too;
  - the Plan menu on both sides has it;
  - the list shows the side's templates;
  - the draft starts with the suggested days, and **Use this plan** is disabled until the day count matches;
  - Swap is absent on a main lift and present on an accessory;
  - a save on an empty side stores the template's items on the chosen weekdays and sets the training days;
  - replacing shows the confirm, **Cancel** stores nothing, and **Replace** replaces only that side;
  - history, bests, skips and the other side are unchanged;
  - powerlifting items have `repsHigh` null;
  - with history, the coach's target for a lift on two days is right;
  - **replacing with state tied to the old plan** (`replacePlan` leaves advice alone and gives new item ids; powerlifting has never had a whole-plan replace): an open SWAP or SKIP card for an old item, then Apply, doesn't crash or write to a missing item (it does what the bodybuilding Rebuild does today, or is cleared if that crashes); a skip made today keeps its record; a session in progress keeps its logged sets.
- [ ] Run them and see them fail.
- [ ] Implement.
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*Plan*' --tests '*Template*'` and see them pass.
- [ ] Shoot the list, a draft on each side and the confirm, dark and light, as the builder's shots are made.
- [ ] Commit `feat(app): choose a plan template`.

### Task 4: Web: choose a template
**Files:**
- Create `web/src/state/useTemplates.ts`, `web/src/screens/plan/templates/TemplatesScreen.tsx` and its test.
- Modify `web/src/screens/plan/PlanScreen.tsx` and the builder's draft view (shared, with no visible change).
- Add `web/e2e/templates.spec.ts`.

**Interfaces:**
- **Consumes:** Task 2's JS exports, `replacePlan(db, mode, items)` from `web/src/db/catalogue.ts`, and the builder's `draftItems` (extended for fixed reps).
- **Produces:** the same steps, state, strings and behaviour as Task 3.

- [ ] **Write the failing tests:** Task 3's list, including the replace-with-old-state cases, as vitest and e2e cases. Also, at 320 px and 130% zoom, the list and the draft have no sideways scroll and no clipped names.
- [ ] Run them and see them fail.
- [ ] Implement.
- [ ] Run `npm test`, `npx tsc -b` and `npx playwright test e2e/templates.spec.ts --trace off`, and see them pass.
- [ ] Shoot the same shots as Task 3, for the web.
- [ ] Commit `feat(web): choose a plan template`.

### Task 5: Version 1.14, notes, full run
- [ ] **Version 1.14 on both apps,** and the Settings shots.
- [ ] **`docs/next-update.md`:** a "Done: plan templates (1.14)" section, covering:
  - what the lifter sees;
  - the sourced numbers;
  - the designer's choices, for the owner;
  - any tests that changed.
- [ ] **README:** the phone checks for templates on each app.
- [ ] **Full run, one step after another, on a quiet machine:**
  1. `./gradlew :domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest --rerun-tasks`;
  2. web `npm test`, `npx tsc -b` and `npm run build`;
  3. the full e2e with `--trace off`;
  4. 3 speed runs.
- [ ] Commit `chore: version 1.14, notes`.

Then: an independent review, fixes, a merge under the owner's standing permission, and the live check.
