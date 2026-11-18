# Skip a planned exercise: design

**Owner's request (2026-11-18):** "if a user has an exercise planned for a day but doesn't do that exercise for whatever reason. They need to be able to tell the app." Then: "It should leave today's list and the coach should know." The owner chose **B**, an optional one-tap reason, and **approach 1**, a skip stored as its own record.

## Why
- **Today, a planned exercise you don't do just stays empty.** It can't be removed from Today; only exercises you added yourself, with no sets, can be taken back. **Finish** counts only exercises with sets.
- **The coach doesn't penalise it,** because it works only from logged sets. But nothing records that you skipped it, or why. It stays on Today for the rest of the day, and nothing changes next time.
- **The owner wants two things:** the list cleared so they can get on with the session, and a coach that responds to skips sensibly.

## Goals
1. On Today, any planned exercise with no logged sets can be skipped for today. It leaves the list.
2. The skip is stored, with the date, the exercise, the plan item, the side and an optional reason.
3. The coach responds to skips according to the reason, on both sides (powerlifting and bodybuilding).
4. Both apps (Android and web) behave the same, and skips are in backups.

## Non-goals
- Moving an exercise to another day.
- Skipping a whole day or a whole session.
- Changing the plan. The exercise is back on its day next week.
- A history screen of skips, and skips shown on Progress.
- Skipping exercises you added yourself. They keep today's take-back.

## Anti-goals (a failure even if it works)
- **Skips leaking into your numbers.** A skip must never count as a set, a missed set, a failure or a stall, and never change e1RM, best lifts, weekly volume, charts or progression.
- **Nagging.** At most one skip-driven card per exercise at a time, and dismissing it follows the coach's existing dismissal memory.
- **Medical advice.** The pain response suggests an alternative and holds the weight. It never diagnoses, and any health wording comes only from the approved sources.

## Approaches (the owner picked 1)
1. **Chosen: a skip is its own record,** beside sessions but separate from sets. The coach rules read skips directly. Every existing count of sets stays exactly as it is.
2. **A skip as a special empty set in the session:** less new storage, but every chart, total and rule that counts sets would have to filter it out. Missing one place would quietly corrupt the numbers.
3. **Skips remembered only for the day:** the list clears, but the coach never sees skips over time, which fails "the coach should know".

## What you see on Today
- Each planned exercise with **no logged sets** has **Skip today** in its row menu (⋮). It isn't a direct button, so it can't be hit by accident.
- **Tapping it asks why,** in a small sheet. The reason is optional:
  - **Busy or no equipment**
  - **Pain**
  - **Out of time**
  - **Just skip** (no reason)

  Back, or tapping outside the sheet, cancels the skip. Nothing is stored.
- The exercise leaves the list, and a snackbar reads "<Exercise> skipped." with **Undo** for the usual snackbar time. Undo removes the stored skip.
- **After the snackbar:** a quiet line at the end of Today's list reads "Skipped today: <names>", each with **Bring back**. Bringing one back removes its skip and returns it to its place in the list. *(My call: you can change your mind for the rest of the day, as promised to the owner.)*
- **Skipping doesn't need a started session.** The skip belongs to the date, not a session, so you can skip before you start.
- **Finish's summary** adds "1 skipped" (or "n skipped") when there were skips that day.
- **One tick** of haptics on a skip, as for other main actions. None on cancel.
- **An exercise with logged sets** has no **Skip today**. The sets you did count, and the rest are simply not done, as today.

## What the coach does
The coach reads skips over the last 28 days, the window it already uses for dismissal memory. Busy and no-reason counts are per exercise and per side. *(My call: the pain hold belongs to the lifter, like deloads and easing back in. A pain skip holds that exercise's weight on both sides, and a logged session of it on either side ends the hold.)*

**When cards appear** *(my call)*: at the moment of the skip, not at Finish. Skips don't need a session, and the coach otherwise runs only at Finish. Undo, or Bring back, takes back any card that its skip created and that is still open. *(Thresholds are my calls, product rules rather than training numbers, so they need no source. The owner can change them.)*

- **Busy or no equipment:** the 2nd such skip of the same exercise within 28 days creates a SWAP card for a similar exercise that uses different equipment, chosen from the catalogue the way the coach's existing swaps are. **Apply** swaps the plan item, and **Dismiss** follows the usual memory.
- **Pain:** on the first pain skip, these happen together:
  - **A card:** it suggests a gentler alternative for the same muscles, with SWAP's Apply and Dismiss.
  - **The weight is held:** the coach makes no weight increase for that exercise until a session of it is logged after the pain skip. A logged session ends the hold: it means you did it again, so the pain apparently allowed it.
  - **Wording on persistent pain:** for example, seeing a professional if it lasts. This must come from the approved sources, looked up during planning and cited in `CoachSources.kt` like the coach's other numbers. If the sources support no such line, the card goes without it.
- **Out of time:** nothing changes, and there is no card.
- **Just skip (no reason):** the 3rd skip of the same exercise within 28 days, of any reason except out of time, creates one card asking whether to keep it in the plan, offering a swap. *(My call: the card has only Swap and Dismiss, with no "Remove from plan", since changing the plan beyond a swap is a non-goal.)* It is not stacked on a busy or pain card that is already open for that exercise.
- **Skips are never failures.** No rule may read a skip as a missed set, a stall or a reason to deload or ease back in. `ReturnRule` (time off) keeps reading sessions only.

## Storage, backups and edge cases
- **A skip record holds:**
  - id;
  - date;
  - side (mode);
  - exercise id;
  - plan item id (nullable, since the item may later be deleted);
  - reason (nullable: busy, pain or time).
- **One skip per exercise per date per side.** Skipping again replaces the reason.
- **Backups include skips.**
  - An older backup restores with no skips.
  - A backup with skips restored into an older app version is turned away as too new, which is that app's existing rule (it rejects unknown keys outside `settings`). The backup format version goes up.
- **When things are deleted:** deleting a plan item keeps its skips, so the coach's history stays true. Deleting a custom exercise also removes its skips.
- **Dates use the same local-date rule as sessions** (`today()`).

## Constraints
- **Training numbers and health wording** come only from the approved sources, via a web search limited to the allowed domains. They are cited in `CoachSources.kt`.
- **Unchanged:**
  - all existing set counts, charts and coach outputs, when there are no skips;
  - the picker and its 100 ms speed checks;
  - Today's opening speed.
- **Fixed rules:** both apps' versions agree; no model names in code or commits; new commits only.

## Left to the builder
- Schema versions and migrations (Android Room v4 → v5; the web Dexie schema), and the backup format field.
- The sheet's exact layout and string wording, beyond the four reasons, "Skip today", "Bring back" and "Skipped today".
- How the existing SWAP advice is reused for the new cards. Prefer reusing the existing advice type and payload, if they fit.
- How "similar exercise with different equipment" and "gentler alternative" are chosen from the catalogue's existing data.
- **Tests on both apps:**
  - skip, then the exercise leaves the list and the skip is stored;
  - cancel stores nothing;
  - Undo and Bring back;
  - no Skip on an exercise with sets;
  - Finish's count;
  - each coach rule's threshold, plus one skip short of it;
  - pain holds the weight increase, and a logged session releases it;
  - skips never change any set-based number;
  - backups round-trip.
- **Screenshots:** the row menu, the reason sheet and the skipped line, on both sides, dark and light.
