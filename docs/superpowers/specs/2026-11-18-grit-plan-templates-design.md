# Plan templates: design

**Owner's request (2026-11-18):** "We should have more built templates for powerlifters and for bodybuilders that they can choose from to get started." They are for **both** new lifters and lifters switching programs.

**The owner's choices:**
- **Templates differ by days a week (A).** Each is labelled with who it suits, where the sources support it.
- **A preview the lifter can adjust before saving (A).** They choose weekdays and can Swap exercises.
- **Approach 1.** Templates are fixed plans in the shared domain, shown in the plan builder's draft screen.

## Why
- **Powerlifting:** a fresh install starts with an empty plan on Monday, Wednesday and Friday. The lifter adds every exercise by hand, and that side has no builder.
- **Bodybuilding:** an empty plan has **Build my plan**. It fits one draft to the lifter's own exercises, in 3 or 4 days. There is no choice of program shapes.
- **What the owner wants:** a new lifter can start training in a few taps, and an experienced lifter can switch to a different program without rebuilding the plan by hand.

## Goals
1. **About 3 templates on each side, set by days a week** (for example 3, 4 and 5 days). Each has a name, its day count, a line on who it suits (sourced) and its week of exercises.
2. **Where they are reached:** from an empty plan, as the first choice, and at any time from each side's Plan menu.
3. **Before saving, a draft preview:** the lifter chooses the weekdays (the template fixes how many), can **Swap** exercises, and saves with **Use this plan**.
4. **Saving replaces that side's plan and training days,** after a confirm when the side already has a plan.
5. **Both apps behave the same,** from one template definition in the shared domain.

## Non-goals
- Lifters saving their own plans as templates, or sharing templates.
- Multi-week programs: periodisation, week-by-week percentages, deload weeks or peaking for a meet. A template is one repeating week, as plans are now. The coach keeps its own progression and deload rules.
- Templates by goal or by equipment.
- Remembering which template a plan came from. The backup format is unchanged.
- Changing **Build my plan**. It stays, beside the templates, on the bodybuilding side.

## Anti-goals (a failure even if it works)
- **A number with no source.** Sets, reps, effort (RPE), days a week, how often a lift or muscle is trained, and the "who it suits" lines all come from the allowed sources. Anything that is the designer's own (names, which exercise goes on which day) is marked as such for the owner.
- **Copying a named program,** from a book, coach or brand. Templates are generic and their names describe them ("4-day upper/lower").
- **Losing data on a switch.** Replacing a plan never touches history, known bests, check-ins, skips or the other side's plan.
- **A coach that misreads a template.** Every template uses only what the coach already handles. If a template has the same lift twice in a week with different reps, each day's target must be right. If the coach can't do that, the template doesn't do it.

## Approaches (the owner picked 1)
1. **Chosen: fixed plans in the shared domain, shown in the draft screen.** Each template is written out once: name, day count, who it suits, and each day's exercises with sets, reps (fixed on powerlifting, a range on bodybuilding) and RPE. Choosing one makes a draft week in the builder's existing draft model (`PlanDraft`), with its existing Swap and save. The draft screen also has to work on the powerlifting side.
2. **Preset answers to the bodybuilding builder:** cheapest on bodybuilding. But the builder makes only 3- or 4-day weeks, powerlifting has no builder, and the templates would be barely different from **Build my plan**.
3. **Each app keeping its own templates:** twice the work, and the two apps could drift apart.

## What the lifter sees
- **An empty plan:**
  - **Powerlifting:** a line and a **Choose a template** button, where the empty plan shows nothing today. Adding exercises by hand still works.
  - **Bodybuilding:** **Choose a template** beside the existing **Build my plan**.
- **The Plan menu on both sides:** **Choose a template**. On bodybuilding it is next to **Rebuild my plan**; on powerlifting the menu is new, if Plan has none there today.
- **The list:** the side's templates, fewest days first. Each shows its name, "n days a week", who it suits, and its days' titles (for example "Upper · Lower · Upper · Lower").
- **The draft,** after tapping a template:
  - **The weekdays to train,** as many as the template has. A sensible spread is ticked to start with *(my call: the side's current training days when they have the same count, otherwise a spread such as Mon, Wed, Fri)*.
  - **Each day's exercises,** with sets × reps @ RPE.
  - **Swap** on each exercise, using the builder's existing similar-exercise rule.
  - **Use this plan** saves it, and **Back** returns to the list.
  - The bodybuilding draft also shows each body part's weekly sets, as the builder's draft does.
- **Main lifts** (squat, bench, deadlift, and strict press when the lifter has it on) have no Swap in powerlifting templates, in line with the owner's decision A for skips: main lifts are never swapped out. See "Settled after the builder check" for variations and strict press.
- **The confirm,** when the side already has a plan: "Replace your <side> plan? Your history and bests are kept." with **Replace** and **Cancel**.
- **After saving:** back to Plan with the new week, and a snackbar "Plan set: <template name>."

## Templates (content decided in planning, from the sources)
These are the expected shapes. The planner checks each against the sources and changes any the sources don't support.
- **Powerlifting:**
  - 3 days, for newer lifters;
  - 4 days, for lifters with some training behind them;
  - 5 days, for experienced lifters.
  - Squat, bench and deadlift each a set number of times a week, plus a few accessories a day.
- **Bodybuilding:**
  - 3-day full body, for newer lifters;
  - 4-day upper/lower;
  - 5-day split, for experienced lifters.
  - Each muscle group at least 10 sets a week and at least twice a week (the builder's existing sourced rules, `BUILDER_WEEKLY_SETS` and `BUILDER_FREQUENCY`), unless a source supports otherwise for a split. If it can't be met, the planner brings it to the owner.
- **Leads found so far (2026-11-18, from search summaries):**
  - **ACSM progression models (2009),** as summarised by ExRx: novice 2–3 days a week, intermediate 3–4, advanced 4–5. For advanced lifters it suggests 4–6 days, training 1–3 muscle groups per session.
  - **NSCA:** advanced 4–6 days a week.
  - **PMC review of powerlifters:** about 3–6 working sets a week of 1–5 reps above 80% 1RM at RPE 7.5–9.5, over 1–3 sessions for each lift.
- **Exercises come from the existing catalogue only,** with the builder's order rule: compound lifts before accessory work (`BUILDER_ORDER`).

## Settled after the builder check (my calls unless marked)
1. **A lift on more than one day has the same sets × reps @ RPE on every day it appears.** Both coaches find "last time" by exercise, not by plan item (`HistoryStats.lastTopSet`, `ProgressionRule.target`, `BodybuildingCoach.lastSets`). Different reps on two days would give one day the wrong target. A heavy/light week is made with a variation instead (for example pause squat), which is its own exercise. Only the competition lift counts towards the sourced "sessions per lift" and "weekly working sets". Variations are extra and use the same sourced rep and RPE ranges.
2. **Powerlifting accessories need a sourced fixed rep count,** since powerlifting items have fixed reps (`repsHigh` null). The planner searches the allowed sources first. If none gives one, the planner brings it to the owner before building: a designer's number marked as such, or no accessories. The builder's 3 × 8–12 is a bodybuilding range and isn't borrowed.
3. **Bodybuilding volume matches the builder's own default weeks:**
   - the big groups (chest, lats, quads, hamstrings, glutes, upper back, biceps, triceps) reach 10 or more counted weekly sets and two days;
   - the small groups may come in under 10 and are shown as short, as in the builder's draft;
   - no group goes over `PhysiqueCoach.WEEKLY_CEILING` (20), counted the way that ceiling counts.

   The domain test checks exactly this.
4. **Swap on a powerlifting template:**
   - main lifts have no Swap;
   - a variation of a main lift swaps only to other variations of the same lift;
   - accessories use the builder's similar-exercise rule.
5. **"Strict press on" means the existing `showStrictPress` setting.** When it is on, the powerlifting templates include strict press; when it is off, they don't. A saved plan is a snapshot: turning the setting on or off later changes nothing in it.
6. **The template's sourced RPE is used in the draft,** not the Settings "Default RPE", which keeps applying to exercises added by hand. Every number can still be edited on Plan afterwards.
7. **A new lifter gets no first target,** as today. With no history or known best, the coach gives a lift no target. The tests check that, plus a right target when there is history.
8. **Saving uses the existing `replacePlan(mode, items)` on Android** (it clears only that side and leaves advice as it is), and the web's equivalent, which the planner checks is the same.

## Storage, backups and edge cases
- **No new storage.** Templates live in code. Saving writes ordinary plan items and the side's training days, through the same path as the builder's **Use this plan**, and removes the side's old plan items.
- **When the old plan is replaced:**
  - skips keep their records (plan item set to null, as when a plan item is deleted);
  - open coach cards about the old plan's items behave exactly as when **Rebuild my plan** replaces a plan (the planner checks what that does today and matches it);
  - history, bests, check-ins and the other side are untouched.
- **A template exercise the lifter has deleted, or one missing from the catalogue,** can't happen, since catalogue entries can't be deleted. If one is ever missing, it is left out of the draft and named, as the builder's left-out list does.
- **Backups are unchanged.**

## Constraints
- **Training numbers and "who it suits" lines** come only from acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport, searched with `allowed_domains`. They are cited in `CoachSources.kt`, saying they were checked from the search summary.
- **Unchanged:**
  - **Build my plan** and its output;
  - every coach rule;
  - the picker and Today's speed checks;
  - adding exercises by hand.
- **Fixed rules:** both apps' versions agree (this is 1.14); no model names in code or commits; new commits only.

## Left to the builder
- How the template data is shaped, and how a template becomes a `PlanDraft`, including weekday mapping and powerlifting's fixed reps.
- How the draft screen is shared or generalised for the powerlifting side on each app.
- Exact strings beyond "Choose a template", "Use this plan", "Replace", "Cancel" and the confirm and snackbar text above.
- **Tests on both apps:**
  - every template builds a valid draft on its side;
  - weekday choice;
  - Swap, with main lifts having none;
  - Use this plan on an empty side, and replacing a side's plan after the confirm, and Cancel changing nothing;
  - history, bests, skips and the other side untouched after a replace;
  - the coach's targets for a template's exercises: none for a new lifter, and right ones with history, including a lift that appears on two days.
- **Domain tests:**
  - every exercise exists in the catalogue;
  - each bodybuilding template meets its sourced weekly sets and frequency;
  - each powerlifting template meets its sourced sets and frequency.
- **Screenshots:** the list, a draft on each side, and the confirm, dark and light, on both apps.
