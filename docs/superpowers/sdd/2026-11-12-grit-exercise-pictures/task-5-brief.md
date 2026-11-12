# Task 5 brief: main lift pictures (21 figures)

Read first, fully:
- Rules: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/implementer-rules.md
- Constraints: /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/constraints.md (but use the attribution lines in this brief, not the ones in constraints.md)
- Spec (authority): docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- Plan, "Tasks 5–12" and "Task 4" sections: docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md
- How Task 4 was done (the pattern to follow): .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-4-report.md, and its reviews review-4-report.md and review-4-fix1-report.md. The review findings there are lessons: poses must read correctly to an experienced lifter, not only pass the checks.

BASE: c2ddaa6. Branch ccr-2f9842b5-owq43n. Never push. Never switch branches.

## What to build

Figures for every MAIN_LIFTS catalogue exercise that has none yet (catalogue: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/MainLifts.kt; figures: domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/MainLifts.kt). The 21:
Pause squat, Front squat, Pin squat, Safety-bar squat, Box squat, High-bar squat, Tempo squat, Spoto press, Long-pause bench, Pin press, Larsen press, Close-grip bench, Board press, Barbell floor press, Wide-grip bench, Deficit deadlift, Paused deadlift, Paused deadlift at the knee, Block pull, Sumo deadlift, Trap-bar deadlift.

- Each pose comes from that exercise's own guide (setup + steps). Captions ≤24 characters, from the guide's wording. Positions the guide doesn't fix: sourced from the allowed domains only, cited in a code comment (URL + what it supports), conservative where sources disagree.
- Reuse the approved Squat / Bench / Deadlift poses as the base for their variants, and change only what the variant's guide changes (e.g. pause squat END/START = the squat's bottom; front squat = front rack, elbows forward, more upright torso; Larsen = feet off the floor/legs straight; floor press = on the floor, upper arms touch the floor; Spoto = bar paused about 2–3 cm above the chest; board press = boards on the chest; pin press/pin squat = bar on pins at the guide's height; deficit = standing on a plate/platform; block pull = plates on blocks at the guide's height; sumo = wide stance, hands inside the knees, more upright torso — this is a FRONT-view candidate only if the side view can't show it, your call with a comment; trap bar = hex bar, hands at the sides, more knee bend).
- "Style approved": the owner approved the Task 4 style. Do not change the look of the existing 10 figures. Kit/renderer changes that alter an existing figure's drawing are allowed only if needed, and must be listed in the report with the reason.
- Kit pieces you may need (add with a manufacturer-neutral typical size, in a comment): safety squat bar (camber + handles), trap/hex bar, plyo/squat box, bench-press boards (one "2-board" stack is enough), pulling blocks, deficit platform. Each new piece gets a test like the Task 4 kit tests.
- Pins/spotter: only where the guide's safety line asks (constraints.md). Pin squat and Pin press use pins as the exercise itself.
- Key checks per entry, at least: squats → bar position for that variant (back / front rack / SSB on back), hip depth the guide gives, feet on the floor; bench variants → BarTouches(CHEST) or the guide's stop point at START/END as fits, LockoutOverShoulders at the lockout pose, head/back/hips on the bench (floor press: on the floor); pulls → BarOverMidfoot and bar close to the legs (trap bar: handles beside the legs, so adapt or add a check with a test).
- Each entry gets a focused test in a new `BatchMainLiftsTest.kt` (same style as BatchOneTest.kt), with real bounds on the key angles/distances.

## Also include (parked Minor findings from the Task 4 re-review)
In `domain/src/commonTest/kotlin/com/powertracker/domain/library/figures/BatchOneTest.kt`:
1. Strict press bar-in-front bound 6..11 → about 7..10.5 (actual 10.0).
2. One-arm row: add an assertion that the END elbow is more than 8 cm above the torso line (actual ~14); tighten the dumbbell bound to about 0..21.
3. DB shoulder press: assert START grip.x − shoulder.x in 8..16 cm.
Do not change any Task 4 pose.

## Testing
- Focused while building: `./gradlew :domain:jvmTest --tests '*BatchMainLiftsTest*'` etc.
- Before the commit, run: `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain` and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Update the fingerprint golden and re-record only screenshots that change.
- The Android suite `:app:testDebugUnitTest` CANNOT run in this session (no Android SDK; installing it is waiting on the owner). Do not try to install an Android SDK or accept licences. Say in the report which app tests you could not run. Do not change app/ code.
- Never run `playwright install`.

## Sheets
`cd web && npm run figures:sheet -- --label mainlifts "main lifts"` (or the area id; check how the script names it) — check every figure at full size and thumbnail size, dark and light, by cropping the sheet (it's very wide), and fix what reads wrong before you hand back.

## Commit
One commit on top of BASE, title `feat(domain): main lift pictures`, a short body, ending with exactly:

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy

No model names elsewhere in the message or code.

## Report
Full report to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/task-5-report.md (per implementer-rules.md: per figure the sources and judgement calls, tests with counts, RED/GREEN evidence, kit changes, any change to existing figures, sheet paths, concerns). Reply with ONLY (under 12 lines): Status, commit, golden, one-line test summary, concerns, report path.
