# Version 1.13.1, the follow-ups: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The open follow-ups from 1.5 and 1.13, which the owner approved on 2026-11-18 ("do them all"):
1. the two guide gaps;
2. Today's heading when everything is skipped;
3. a quiet spell after a pain card is dismissed;
4. gentler alternatives on the pain card, **only if the allowed sources support a rule**.

**Tech Stack:** Kotlin Multiplatform domain (shared with the web through `web/src/domain/generated`), Android Compose, React 18.

**Specs:**
- `docs/superpowers/specs/2026-11-18-grit-skip-planned-exercise-design.md` (1.13);
- the 1.5 notes in `docs/next-update.md` (the guide gaps).

## Global Constraints
- **Allowed sources, as for every guide line and coach rule:** acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport. Search with `allowed_domains` limited to those. Cite each source like the existing guide and coach entries (`library/Sources.kt` and `coach/CoachSources.kt`), saying it was checked from the search summary.
- **No training claim without a source.** If the sources don't support an item, it is not built, and the report says why.
- **Both apps behave the same.** Domain changes reach the web through the regenerated package.
- **Fixed rules:**
  - versions 1.13.1 on both apps, with Android versionCode 22 and versionName "1.13.1" (it must match the web's shown version);
  - no model names in code or commit bodies;
  - new commits only, and never amend;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`, and never `playwright install`.
- **Existing tests** pass unedited, unless a task says otherwise. List any that changed, with the reason.

## Review Focus
1. **The guide lines** fit the existing guide style and length rules, and contradict none of the guide's own cues.
2. **The heading** shows the skipped count only when every planned exercise is skipped. Any other mix is unchanged.
3. **The quiet spell** applies to pain cards only. The hold still applies during it, and a dismissed busy or repeat card keeps its 28-day memory.
4. **Gentler ordering,** if built, never removes alternatives, only reorders them. Main lifts are unaffected.

---

### Task 1: The two guide gaps (domain)
- **Seated cable fly:** add a mistake about going past the start (hands travelling behind the body at the open end).
- **Hack squat:** add the safety line "stop if your hips leave the pad", or its sourced equivalent.
- Each line follows the guide's existing format and limits, and comes with an allowed source. Find where the guides live (the catalogue under `domain/src/commonMain/.../library/`).
- **Tests:** each exercise's guide contains the new line. The existing guide-format tests pass.
- Commit `feat(domain): two guide gaps filled`.

### Task 2: Today's heading when everything is skipped (both apps)
- When every planned exercise on Today is skipped (none left), the start card's eyebrow reads "<WEEKDAY> · <n> SKIPPED", for example "WEDNESDAY · 3 SKIPPED", instead of "· 0 EXERCISES".
- **Tests on both apps:**
  - all skipped gives the new text;
  - one left gives the existing "1 EXERCISE" text, unchanged;
  - Undo or Bring back restores the count.
- Update the `SkipTodayScreenTest` assertion that pinned "0 EXERCISES", with that reason.
- Re-shoot only the shots that change.

### Task 3: A quiet spell after a pain card is dismissed (domain)
- **The rule:** when a pain card for an exercise was **dismissed** in the last 7 days, a new pain skip of that exercise gives no new card. The hold still applies.
  - It is 7 days, the same as the 7-day rule busy and repeat cards already have. It is a product rule (the designer's), not a training number.
- **An APPLIED pain card doesn't start a quiet spell.**
- **Tests:**
  - a pain card dismissed 3 days ago, then a pain skip: no card, and the hold applies;
  - dismissed 8 days ago: a card;
  - applied 3 days ago: a card;
  - busy and repeat dismissal memory unchanged.
- Update the 1.13 notes' pain bullet.

### Task 4: Gentler alternatives on the pain card (domain, research-gated)
- **First, search the allowed sources** for guidance on choosing exercises when an exercise causes pain. For example: machine or cable versions that guide the path and reduce stabilising demands, a smaller range, or lighter isolation options.
- **Only if a source supports a concrete, catalogue-checkable rule,** such as "guided equipment (machine, cable) before free weights", reorder the pain card's alternatives by that rule.
  - Never drop an alternative.
  - Never claim the alternative is pain-free.
  - Keep the card's wording.
- **If no source supports a rule,** build nothing. Record the searches and the reason in the report and in `docs/next-update.md`, and tell me.
- **Tests,** if built:
  - the ordering on a known catalogue exercise;
  - the busy and repeat lists unchanged;
  - main lifts unaffected.

### Task 5: Version 1.13.1, notes, full run
- Version 1.13.1 on both apps, plus the Settings shots.
- `docs/next-update.md`:
  - add a "Done: 1.13.1" section;
  - remove the closed follow-ups.
- Full run:
  - Gradle `:domain:allTests --rerun-tasks :domain:checkWebDomain :app:testDebugUnitTest --rerun-tasks`;
  - then web `npm test`, typecheck, build, the full e2e and 3 speed runs, one after another on a quiet machine.

Then: an independent review, fixes, and a merge under the owner's standing permission.
