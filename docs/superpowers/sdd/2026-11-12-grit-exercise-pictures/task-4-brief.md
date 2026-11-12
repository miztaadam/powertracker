### Task 4: First batch (10 figures), then the owner checkpoint

**Files:**
- Modify: `figures/areas/MainLifts.kt`, `Shoulders.kt`, `Back.kt`, `Quads.kt` and `HamstringsGlutes.kt` (wherever each exercise's catalogue entry lives). Also `Kit.kt` if a piece needs adjusting, and the fingerprint golden.

**Exercises:**
- Squat;
- Bench;
- Deadlift;
- Strict press;
- DB shoulder press;
- Lat pulldown;
- Leg press;
- Pull-up;
- One-arm DB row;
- Romanian deadlift.

**Decisions:**
- **Sources:** each entry's poses come from its guide's setup and steps (`guideLines`). Any position the guide doesn't fix is sourced (Global Constraints) and noted in a code comment with its source.
- **Key checks, at least:**
  - Bench: `BarTouches(CHEST)` at START, `LockoutOverShoulders` at END.
  - Squat: bar across the upper back, the hip depth the guide gives, and feet on the floor.
  - Deadlift and RDL: `BarOverMidfoot`, with the bar close to the legs.
  - Strict press: lockout over the shoulders, and feet on the floor.
  - Pull-up: hands on the bar, and the chin above the bar at END if the guide says so.
  - Lat pulldown: seated, thighs under the pad.
  - Leg press: back on the pad, feet on the plate.
  - One-arm DB row: hand and knee on the bench.
- **Camera:** side view unless the movement is in the frontal plane.

**Steps:**

- [ ] **Step 1:** Add the 10 entries, test-first: each entry's checks fail before the poses are correct.
- [ ] **Step 2:** Run `./gradlew :domain:allTests`. Expected: `everyFigurePassesChecks` passes. Update the fingerprint golden.
- [ ] **Step 3:** Run `npm run figures:sheet -- batch1` to write the contact sheets.
- [ ] **Step 4:** Run the full suites. Expected: pass.
- [ ] **Step 5:** Commit `feat(domain): first ten exercise pictures`.
- [ ] **Step 6 (controller):**
  1. Independent review of the sheets against the guides and sources.
  2. Fixes.
  3. **Stop.** Send the owner the dark and light sheets with a phone notification, and wait for their go-ahead on the style. Any style changes go into the kit and renderer before Task 5.

### Tasks 5–12: Body-area batches

One task per batch, each in the same shape as Task 4: entries, kit pieces as needed, key checks, contact sheets, independent review. Each batch covers the rest of its area, leaving out the first-batch exercises.

| Task | Area file | About | Commit title |
|---|---|---|---|
| 5 | MainLifts | 21 | `feat(domain): main lift pictures` |
| 6 | Chest | 30 | `feat(domain): chest pictures` |
| 7 | Back | 31 | `feat(domain): back pictures` |
| 8 | Shoulders | 23 | `feat(domain): shoulder pictures` |
| 9 | Arms | 31 | `feat(domain): arm pictures` |
| 10 | Quads | 27 | `feat(domain): quad pictures` |
| 11 | HamstringsGlutes | 27 | `feat(domain): hamstring and glute pictures` |
| 12 | Calves + Core | 26 | `feat(domain): calf and core pictures` |

**Each task:**
- [ ] **Step 1:** Add entries test-first.
- [ ] **Step 2:** Run `./gradlew :domain:allTests` and update the fingerprint golden.
- [ ] **Step 3:** Run `npm run figures:sheet -- <area>`.
- [ ] **Step 4:** Run the full suites.
- [ ] **Step 5:** Commit.

Kit pieces are added as the batches need them: incline and decline bench, EZ bar, trap bar, kettlebell, cable handle and rope, dip bars, and the area's machines. Each piece needs its real-world size from a manufacturer-neutral typical dimension, noted in a comment.

