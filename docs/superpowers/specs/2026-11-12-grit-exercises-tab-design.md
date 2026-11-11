# Grit: Exercises tab

**Date:** 2026-11-12
**Status:** Approved in brainstorming. Awaiting the owner's review of this spec.
**Applies to:** Android (`app/`) and web (`web/`). Ships together with strict press (`2026-11-12-grit-strict-press-design.md`), under one plan.

## 1. Purpose

The library has 225 exercises, each with a form guide. At the moment the only way to see them is from inside Plan's or Today's "Add exercise" picker. The owner wants a dedicated place to browse and discover exercises, read their guides, and add the ones they like to their program.

**Decided with the owner:**
- It is a fifth bottom tab.
- Tapping an exercise opens its full guide, with **"Add to plan"** and, during a workout, **"Add to today"**.

## 2. Tab

- The tab is labelled **Exercises** and uses a dumbbell or list icon matching the existing icon style.
- Tab order: **Today, Coach, Progress, Exercises, Plan**.
- Web route is `#/exercises`. Android route is `exercises`.
- The tab bar must still fit five tabs:
  - at 375 px (web) and 360 dp (Android);
  - with touch targets of at least 48;
  - with labels fully visible, never cut off with an ellipsis.

  The existing tab bar tests are extended to five.
- The current-tab highlight is the warm chalk indicator pill, as for the other tabs.

## 3. Browse screen

- The screen header reads "Exercises", with the eyebrow "225 exercises". The count is the catalogue size plus the lifter's own exercises.
- The body reuses the picker's browsing parts rather than copying them:
  - search;
  - body-area chips;
  - equipment chips;
  - the "Browse by muscle" grid;
  - muscle mode;
  - rows with ⓘ.

  The browsing content is extracted into a shared component used by both the picker dialog and this tab. On Android it is a composable. On the web it is a component.
- The pick, double-tap and custom-name rules don't apply here, because a row tap opens the guide instead of picking.
- The lifter's own (custom) exercises appear under "Your exercises", the same as in the picker. Their sheet shows the existing "No form guide for your own exercises." text, with the add buttons.
- The tab keeps its search, filters and muscle mode while the lifter switches tabs, but not across an app reload.

## 4. Guide sheet with actions

- Tapping a row, or its ⓘ, opens the existing guide sheet with an action area at the bottom. On the tab both do the same thing.
- **Add to plan** is a primary button.
  - Tapping it asks which training day: a list of the lifter's training days from settings, in weekday order. If no training days are set, it shows all seven days.
  - Picking a day adds a plan item through the existing catalogue resolver, using the existing defaults: COMPOUND is 3 × 5, ACCESSORY is 3 × 10, at the settings default RPE.
  - A snackbar then says "Added to <Day>", with an action "Open Plan".
  - If the exercise is already on that day, nothing is added and the snackbar says "Already in your plan on <Day>." This matches the coach's existing message.
- **Add to today** is a secondary button.
  - It appears only while today's session is open and unfinished.
  - It adds the exercise to today's session the same way Today's own "Add exercise" does, including the rule that no database row is created until the first set is logged.
  - A snackbar then says "Added to today", with an action "Open Today".
  - If the exercise is already in today's session, the snackbar says "Already in today's session."
- Both actions are guarded against a double tap.
- A failed write shows the existing "Couldn't save — try again." snackbar and changes nothing.
- The sheet's existing safety line stays as the last line of the guide text, and the actions sit below it.

## 5. Data

There is no schema, setting or backup change. Writes go through the existing plan and Today paths, including the catalogue resolver, its single transaction and its name rules.

## 6. Tests (both apps, same names)

- `exercisesTab_isTheFourthOfFiveTabs_andFits`
- `exercisesTab_browseByMuscleAndSearchWork`
- `tapRow_opensGuideWithActions`
- `addToPlan_choosesDay_andUsesDefaults`
- `addToPlan_alreadyOnDay_saysSo`
- `addToToday_onlyDuringOpenSession`
- `addToToday_createsNoRowUntilFirstSet`
- `actions_doubleTap_addOnce`
- `failedWrite_showsMessage_andChangesNothing`
- `customExercise_showsNoGuideText_withActions`
- `tabState_survivesSwitchingTabs`

The web e2e suite adds a five-tab fit check at 375 px and an add-to-plan flow.

Screenshots: the Exercises tab, the guide sheet with actions, and the day chooser, in dark and light, for both apps.

## 7. Delivery

This spec is built in the same plan as strict press, after the strict press tasks, because the tab also shows strict press in the catalogue. Fable builds the UI and each task gets its own review. A final review covers the whole branch.
