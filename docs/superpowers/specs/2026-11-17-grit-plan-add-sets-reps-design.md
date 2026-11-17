# Choose sets and reps when adding to the plan: design (short)

**Owner's request (2026-11-17):** "When inputting exercises into my plan, I want to be able to select the reps sets." The owner confirmed the place: picking an exercise from Plan's **Add** on a day.

## Why
- **Today:** picking adds the exercise straight away, with ready-made numbers:
  - powerlifting: 3 × 5 for a compound lift and 3 × 10 for an accessory, at the default RPE;
  - bodybuilding: 3 × 8–12 @ 8.
- **The gap:** changing them takes a second trip, through the row's menu and then **Edit**. The owner wants to set them while adding.

## What changes
- On Plan's **Add**, picking an exercise opens the existing sets/reps form (Android `PlanItemDialog`, web `PlanItemDialog`) straight away. It is filled in with the same ready-made numbers that adding uses today (`newItem` on Android; the web's equivalent).
- **The form's title** is "Add to <Day>", and its confirm button reads **Add**:
  - **Add** saves the item with the chosen numbers, at the end of the day. It ticks once, when stored, as today.
  - **Cancel** (or back, or outside) adds nothing.
- **What the form holds:**
  - Powerlifting: Sets, Reps and RPE.
  - Bodybuilding: Sets, Reps from, Reps to and RPE.
  - **Change** swaps the exercise, as in Edit.
- **Your own new exercise,** created from the list: once it is created, the same form opens for it. Cancel leaves the new exercise in your library but adds nothing to the plan. Creating it is already saved before the form, as today.
- **Both apps, both sides.**

## Not changing
- Editing a row later.
- Today's **Add exercise**.
- The plan builder.
- The Exercises tab's "Add to plan" (`addOnce`).
- The ready-made numbers themselves.

## Approaches
- **(Chosen) Reuse the Edit form after the pick.** The form, its limits and its tests already exist. This is the smallest change, and it is consistent with Edit.
- **Sets and reps inside the picker rows:** this crowds the picker, which is shared with Today and the plan builder, and puts its 100 ms speed checks at risk.
- **Add now, then open Edit automatically:** Cancel would leave an item behind, or need an undo. That is worse than not saving until **Add**.

## Trade-off
One extra tap (**Add**) when the ready-made numbers are already right.

## Left to the builder
- How the form takes an unsaved item (id 0) and the picked entry, and saves through the existing add path. It must keep the catalogue-row creation in the same transaction.
- The exact wording of strings beyond the title and button.
- Tests, on both apps:
  - pick, then the form shows the defaults;
  - change the sets and reps, then **Add** stores exactly those;
  - **Cancel** stores nothing;
  - the bodybuilding range;
  - your own exercise;
  - one tick per add.
- Screenshots: the new form in add mode, on both sides, dark and light.
