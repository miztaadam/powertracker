# Last time on the Today card: implementation plan (shared domain and web)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the web Today card, replace the Stretch and Squeeze lines with every set of the exercise's last session on this side, on both sides.

**Architecture:** One shared domain rule says which session is "last time" and returns its sets; the bodybuilding coach uses the same rule. The web asks for every exercise's last time in one bridge call per loaded history and puts it on each `TodayItem`; a small `LastTime` component draws it.

**Tech Stack:** Kotlin Multiplatform (`domain/`), its generated JavaScript, React and Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-25-grit-last-time-on-today-design.md`

## Global Constraints

- Web first. The Android card is ported in a later plan, once the owner has used the web one; until then it keeps its Stretch and Squeeze lines.
- "Last time" is the latest session with `date < before` and the given side that holds the exercise. Each set is shown in the unit it was logged in.
- The coach's stored reason text is not changed.
- The Stretch and Squeeze lines stay in the ⓘ guide sheet.
- No new database read on Today, and one bridge call per loaded history object.
- `web/src/domain/generated/` is only ever written by `./gradlew :domain:syncWebDomain`.
- Version 1.18 on both apps (`versionCode` 34). Nothing is pushed to `main`.

## Review Focus

1. The latest session with the exercise is on the other side: the block shows this side's, or nothing.
2. A session that mixes kg and plate sets: each row in its own unit.
3. A missed set, and a set with no RPE: "missed" shows, and no "@" with nothing after it.
4. Many sets (8) at 288 px and at 130% zoom: every row fits, nothing clipped.
5. A session from another year: the heading carries the year.

Each has its test in the task that owns the code (1 and 2 in Task 1, 3 and 5 in Task 4's unit tests, 4 in Task 4's browser test).

---

### Task 1: the shared rule

**Files:**
- Create: `domain/src/commonMain/kotlin/com/powertracker/domain/history/LastTime.kt`
- Modify: `domain/src/commonMain/kotlin/com/powertracker/domain/coach/BodybuildingCoach.kt` (`lastSets`)
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/history/LastTimeTest.kt`

**Interfaces — produces:**

```kotlin
/** An exercise's sets, in logged order, in one earlier session. */
data class LastTime(val date: LocalDate, val sets: List<LoggedSet>)
fun TrainingHistory.lastTime(exerciseId: Long, mode: TrainingMode, before: LocalDate): LastTime?
fun TrainingHistory.lastTimes(mode: TrainingMode, before: LocalDate): Map<Long, LastTime>
```

- [ ] Tests: none before → null; picks the latest of several; ignores a session on `before` and later; ignores the other side even when later (focus 1); keeps logged order and mixed units (focus 2); `lastTimes` equals `lastTime` for every exercise in a mixed history, including two sessions on one date.
- [ ] Run `./gradlew :domain:jvmTest --tests '*LastTimeTest*'` (or the module's JVM test task): fails to compile.
- [ ] Implement. `lastTimes` is one pass over the sessions from newest to oldest; a tie on the date goes to the session later in the list, as `lastTime` does.
- [ ] `BodybuildingCoach.lastSets` becomes `history.lastTime(exerciseId, TrainingMode.BODYBUILDING, today)?.sets`.
- [ ] Run the new test and `*BodybuildingCoach*`: pass. Commit.

### Task 2: the bridge

**Files:**
- Modify: `domain/src/jsMain/kotlin/com/powertracker/domain/web/JsTypes.kt`, `GritDomain.kt`
- Test: `domain/src/jsTest/kotlin/com/powertracker/domain/web/GritDomainTest.kt`
- Generated: `web/src/domain/generated/*`; export `JsLastTime` from `web/src/domain/index.ts`

**Interfaces — produces:**

```kotlin
@JsExport data class JsLastTime(val exerciseId: Int, val date: String, val sets: Array<JsSet>)
fun GritDomain.lastTimes(history: JsHistory, mode: String, before: String): Array<JsLastTime>
```

- [ ] Test: a two-side history gives one entry per exercise done on the side, with the ISO date and the sets in order; an unknown `mode` throws naming the field.
- [ ] Implement, then `./gradlew :domain:syncWebDomain` and `./gradlew :domain:checkWebDomain`. Commit with the generated files.

### Task 3: the Today item

**Files:**
- Modify: `web/src/state/useToday.ts`
- Test: `web/src/state/useToday.test.tsx`

**Interfaces — produces:** on `TodayItem`:

```ts
/** The exercise's sets in its last session on this side before the day shown; null when there is none. */
lastTime: { date: string; sets: { weightKg: number; reps: number; rpe: number | null; failed: boolean; unit?: LoadUnit }[] } | null
```

- [ ] Tests: an item with an earlier session on this side carries its date and sets; none on this side → null; today's own sets are never in it.
- [ ] Implement with a `WeakMap<JsHistory, Map<string, Map<number, …>>>` keyed by side and date, beside `topSets`.
- [ ] `npx vitest run src/state/useToday.test.tsx` and `npx tsc -b`: pass. Commit.

### Task 4: the block on the card

**Files:**
- Create: `web/src/screens/today/LastTime.tsx`
- Modify: `web/src/screens/today/ExerciseCard.tsx` (drop `Cues`), `today.css`, `web/src/ui/narrowTabs.css` or wherever `.today-cue` is narrowed, `web/src/screens/progress/dates.ts` only if a helper is missing
- Test: `web/src/screens/today/ExerciseCard.test.tsx`, `web/e2e/bodybuilding.spec.ts`, `web/e2e/narrow.spec.ts`

**What it draws:** a `section` named "Last time" between today's sets and the entry (and above the "next" button once the sets are done), with the heading `Last time · Wed 18 Nov` in the eyebrow style and accent text colour, and an ordered list, one item per set: number, `25 kg × 10`, `@ 8` when there is an RPE, `missed` for a failed set. Each item reads to a screen reader as one sentence: "Set 1: 25 kg × 10 at RPE 8", ", missed" when failed, plates as `grit.plateSpoken`. Nothing at all when `lastTime` is null.

- [ ] Rewrite the "stretch and squeeze on the exercise card" unit tests for the block: both sides show it; order (after today's sets, before the weight field; before the next button when complete); null shows nothing and leaves no empty box; a missed set and a set with no RPE (focus 3); last year's date has the year (focus 5); the spoken sentences; the cue lines are no longer on the card.
- [ ] Rewrite the 130% zoom browser test for the block, with eight sets seeded last week (focus 4), and make sure a narrow-screen Today story holds a last-time block.
- [ ] Implement; remove the `.today-cue*` styles.
- [ ] Run the two unit files, `npx tsc -b`, and the `bodybuilding`, `narrow`, `today` and `shell` specs (iPhone, then `E2E_UA=android` for those in CI's list). Commit.

### Task 5: version 1.18 and the notes

- [ ] `app/build.gradle.kts` 1.18 / 34; `web/package.json` and lock 1.18.
- [ ] Re-shoot: web (`UPDATE_SCREENSHOTS=1 npm run e2e -- --trace off`) and Android Settings (`./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`); commit only shots that changed.
- [ ] `docs/next-update.md` entry, README item 10, `docs/manual-checks.md`.
- [ ] The checks in `CLAUDE.md`, the timing checks included. Commit and push the branch.
