# Workout history on Progress: implementation plan (web)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A History button at the top right of Progress opens a read-only list of every past workout, newest first.

**Architecture:** A new `history` page in the web router. Its state is built in TypeScript from the history the app already loads, beside the lift and exercise histories. The Today card's "Last time" rows become one shared component used by both.

**Tech Stack:** React, Dexie `liveQuery`, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-25-grit-workout-history-design.md`

## Global Constraints

- Web only. No change under `domain/` or `app/` except the version number.
- Nothing new is stored. No Dexie version, no backup change.
- The Today block's markup and class names do not change (1.18's tests and committed shots stay as they are).
- History calls go through `router.tsx` (`navigate`, `goBack`), as every page's do.
- Version 1.19 (`versionCode` 35), as the last commit. 1.18 is complete at `929b437`: a "publish" before this is done merges that commit, not the branch tip.

## Review Focus

1. A long log (45 workouts): 20 drawn, then 40, then all, and the button goes.
2. A session with no sets: not listed, not counted.
3. An exercise logged, then another, then the first again: one group, in the order first logged.
4. A workout with kg and plate sets, a missed set, a set with no RPE.
5. `#/history` opened directly (a cold start): Back goes to Progress and replaces it.

---

### Task 1: the shared rows

- Create `web/src/ui/components/PastSets.tsx` and `PastSets.css`: `PastSets({ sets }: { sets: PastSet[] })` draws the `<ol>` that `LastTime` draws today, with the same class names; `pastSetLabel` moves with it. The row styles move from `today.css`.
- `LastTime.tsx` uses it. `ExerciseCard.test.tsx` and `bodybuilding.spec.ts` pass unchanged.

### Task 2: the state

- Create `web/src/state/useWorkoutHistory.ts`:

```ts
export type WorkoutExercise = { exerciseId: number; name: string; sets: PastSet[] }
export type Workout = { sessionId: number; date: string; side: TrainingMode; bodyweightKg: number | null; exercises: WorkoutExercise[]; setCount: number }
export type WorkoutHistoryState = { loading: boolean; workouts: Workout[] }
export function buildWorkoutHistory(history: JsHistory): WorkoutHistoryState
export function useWorkoutHistory(db: GritDb): WorkoutHistoryState
```

- Tests (`useWorkoutHistory.test.ts`): newest first from both sides; focus 2, 3 and 4; an archived exercise keeps its name; bodyweight.

### Task 3: the page and the button

- `router.tsx`: `{ page: 'history' }` ↔ `#/history`, with explicit branches. `router.test.tsx`.
- Create `web/src/screens/progress/WorkoutHistoryScreen.tsx` (`WorkoutHistoryScreen`, `WorkoutHistoryContent`), page size 20. Tests: the count, a card's lines and spoken rows, the empty state, focus 1.
- `ProgressContent` and `BodybuildingProgressContent` take `onOpenHistory` and put `SecondaryButton text="History" compact` in the header's `actions`. `App.tsx` wires it. Tests: the button on both sides, with and without data.

### Task 4: browser tests and shots

- `progress.spec.ts`: opened from Progress and back (no page left behind); focus 5; a shot of the screen, dark and light.
- `narrowDeeper.spec.ts`: History fits at 320 px and 200% text, both sides' Progress headers with the button.

### Task 5: version 1.19 and the notes

- Versions, Settings shots on both apps, the Progress shots the button changed, the new History shots, `docs/next-update.md`, README item 10 (covering 1.18 and 1.19 while neither is published), `docs/manual-checks.md`.
- The full checks in `CLAUDE.md`, with no source edit while the full browser run is going.
