# Version 1.14.1, plan safety nets: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Why:** the owner lost their bodybuilding plan on the web app (an installed PWA on Android, Chrome) around the 1.14 update. Only the plan was lost; history and settings were kept.
- 9 reproduced update paths (1.12.3 to 1.14, through "Update ready") never lost it.
- The only code that removes a whole plan is a user action: Build/Rebuild my plan, a template's Use this plan → Replace, or restoring a backup.

The cause isn't established. The owner approved two safety nets on 2026-11-19: "yes, build both".

**Goal:**
1. **Undo** after any whole-plan replace;
2. **A warning before a backup restore** that would remove a side's plan.

Both apps, the same behaviour.

## Global Constraints
- **Undo** puts back exactly what was there:
  - the side's plan items, with their **ids**, days, order and numbers;
  - the links from skips to those items, which the replace cleared (`forgetPlanItems`);
  - the side's training days.

  It runs in one transaction, all or nothing. If it fails, the snackbar says "Couldn't undo. Your new plan is kept." and nothing changes.
- **Where Undo appears:** the snackbar after the save:
  - "Plan set: <name>." for templates;
  - the builder's existing saved snackbar, or "Plan saved." if it has none.

  The action is **Undo**, for the snackbar's usual time. After that, the old plan is gone, as now.
- **Undo is offered only if the side had a plan before.** Saving onto an empty side gets no Undo.
- **Undo doesn't touch anything made after the save:** sessions, sets, advice, check-ins, or skips logged after it, and the other side.
- **The restore warning:** before a backup restore, compare each side. If the current side has plan items and the backup has none for that side, show a confirm before restoring:
  - title: "This backup has no <powerlifting/bodybuilding> plan";
  - body: "Restoring it removes your current <side> plan. Your other data is replaced by the backup's too.";
  - buttons: **Restore anyway** and **Cancel**;
  - if both sides are affected, name both in one dialog: "no powerlifting or bodybuilding plan";
  - otherwise the restore flow is unchanged.
- **Unchanged:** the backup format, the coach rules and every existing string.
- **Version 1.14.1:** Android versionCode 24, versionName "1.14.1"; web 1.14.1. The mode-switch animations release moves to 1.15, versionCode 25; update its spec's version line.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only, never amend;
  - stage by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`, never `playwright install`;
  - existing tests pass unedited, or each change is listed with its reason.

## Review Focus
1. **Undo after Undo, or after another save:** only the latest replace can be undone, and an old snackbar's Undo can't restore an older plan over a newer one. A stale Undo does nothing.
2. **Leaving the screen** while the snackbar shows: Undo still works, or is gone. It never applies to the wrong side.
3. **Undo after a session was logged on the new plan:** the session and its sets stay. Only plan items, skip links and training days revert.
4. **The restore warning** fires only for "current has items, backup has none", per side, and Cancel restores nothing.

---

### Task 1: Android
- **Undo:**
  - add `TrainingRepository.restorePlan(mode, snapshot)`. The snapshot is the side's items with ids, the skips' `planItemId` links that pointed at them, and the training days. It runs in one transaction, with the days kept consistent as in `34c7864`/`8f56a3d`;
  - `TemplatesViewModel` and `PlanBuilderViewModel` take the snapshot before replacing, and the save's snackbar carries **Undo**;
  - check how the template and builder snackbars reach Plan (AppShell) and route Undo through the same place.
- **The restore warning:** in `data/Backup.kt` and the Settings restore flow, compute "sides that would lose a plan" from the parsed file before restoring, and show the confirm.
- **Tests:**
  - Undo restores the ids, order, numbers, skip links and days;
  - a stale Undo does nothing;
  - Undo after a logged session keeps the session;
  - Undo failure leaves the new plan and shows the message;
  - no Undo when the side was empty;
  - the warning shows for each side and for both, Cancel restores nothing, and no warning when the backup has the plan.
- Commit `feat(app): undo a plan replace; warn before a restore removes a plan`.

### Task 2: Web
- Mirror Task 1:
  - `restorePlan(db, mode, snapshot)` in `web/src/db/catalogue.ts`, as one Dexie transaction over `planItems`, `skips` and `settings`, re-adding the items with their ids;
  - `useTemplates` and `usePlanBuilder` take the snapshot, and their snackbar gets **Undo**;
  - the backup restore UI shows the confirm.
- **Tests:** the same list, as vitest cases, plus an e2e test: template replace → Undo → the old plan is back after a reload.
- Commit `feat(web): undo a plan replace; warn before a restore removes a plan`.

### Task 3: Version 1.14.1, notes, full run
- Version both apps, and reshoot the Settings shots.
- **`docs/next-update.md`:**
  - add a "Done: 1.14.1" section, including the owner's report and the 9 paths that didn't reproduce it;
  - add an open follow-up: the plan-loss cause isn't established, and the owner's backup file is pending.
- **The animations spec:** version 1.15, versionCode 25.
- **README:** phone checks for Undo and the restore warning.
- **The full run:** Gradle, web `npm test`, `tsc`, build, full e2e, and 3 speed runs, one after another.
