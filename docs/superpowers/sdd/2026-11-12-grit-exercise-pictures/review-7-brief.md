# Review brief: Task 7, back pictures

You check finished work and change nothing in the repo (no edits, no commits, no git checkout/stash/reset, no sed -i in the real tree). If you need to experiment, copy files to a temp folder outside the repo. Sheets you render go to web/.figure-sheets/ (git-ignored) — that is the only thing you may write, plus your report.

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Review the commit at HEAD (f52f10f `feat(domain): back pictures`) against its base 13f8a93: `git diff 13f8a93..HEAD`.

Read first:
- The brief the builder had: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-7-brief.md
- Its report: task-7-report.md (same folder). Treat its claims as claims: verify, don't trust.
- constraints.md and implementer-rules.md (same folder); spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- The last review, for the standard expected: review-6-report.md and review-5-report.md (same folder)

## What to check
1. Every picture against its own guide. The catalogue and guides: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/Back.kt. The figures: domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/Back.kt (30 new; Pull-up, Lat pulldown and One-arm DB row existed and must be unchanged). For each new back exercise:
   - Render it and LOOK at it (Read the PNG): `cd web && node scripts/figure-sheet.mjs --label r7 --columns 1 "<name>"` gives web/.figure-sheets/r7-{dark,light}.png; or the whole area with `npm run figures:sheet -- --label r7back back` and crop. Check full size in dark, glance at light, and the thumbnail.
   - Does START and END show what the guide's setup and steps say, to an experienced lifter? Body position, bench angle, where the bar/dumbbells/handles are at each end, grip, feet. Anything anatomically odd (arms through the body, joints bent the wrong way, hands not on the handle, floating kit, body not touching the bench/seat/pad)?
   - Captions: 24 characters or fewer, from the guide's wording, and each fits the pose drawn above it.
   - Where the guide describes a difference from a related exercise (wide vs close vs neutral vs underhand grip, pull-up vs chin-up variants, bent-over vs chest-supported vs seated rows, etc.), is that difference visible at thumbnail size? Compare near-duplicates side by side.
   - Camera: side or front — does the chosen view show what the guide is about?
   - Pins/spotter drawn only where the guide's safety line asks.
2. Every catalogue back entry has a figure (by canonical name), and nothing else got one by accident. Count them.
3. Existing 61 figures unchanged (the builder also touched figures/areas/Chest.kt and Poses.kt — say what changed there and whether any chest drawing moved): the builder says which kit/renderer changes it made. Render the whole chest area, Pull-up, Lat pulldown, One-arm DB row, Squat, Bench, Deadlift and RDL and compare with how they looked before this task: the sheets rendered before Task 7 are in web/.figure-sheets/ (chest-{dark,light}.png = `npm run figures:sheet -- --label chest chest`; cmpb-*.png = the ten Task 4 figures, arguments in review-6-report.md; mainlifts-*.png) — render the same figures with the same arguments under a NEW label (never reuse an existing label: it overwrites the old sheet) and compare pixels; only the label text should differ. Do not use git worktree/checkout/stash. Say how you checked.
4. Sources: every position the guide doesn't fix must cite an allowed domain (acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport) in a code comment. You have no WebSearch, so do not try to verify pages; instead list in your report every citation in the new back code as: file:line, URL, the claim it is cited for, and whether the same URL appears in domain/.../library/Sources.kt (exact match). The controller checks them.
5. Code and tests: BatchBackTest has a focused test per figure with real bounds (not bounds so wide they'd pass a wrong pose — try reasoning about 3 of them: would the test fail if the pose were visibly wrong?). New kit pieces each have a size comment and a KitTest case. The laziness rule: the back map is a map of Lazy and AreaMapsLazyTest still passes. The builder also changed one web test (web/src/screens/library/ExercisePicture.test.tsx, exercisesTab_coldList_buildsOnlyTheNearTilesFigures): read the diff and say whether the test still proves what its name says, or was weakened. For every new figure with an out-of-plane arm (`out != 0`), say whether its own test would catch a wrong-way elbow (the brief asked for an explicit bound). No model names in code or the commit body other than the two attribution lines.
6. Tests: run `./gradlew :domain:jvmTest` (focused is fine: figures package) and `cd web && npm test`; report counts. You don't need to re-run Android or e2e. Do not run the speed tests (the controller does that on a quiet machine). When done, run `cd /home/user/powertracker && ./gradlew --stop`.

## Report
Write to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/review-7-report.md, in the shape of review-6-report.md: verdict line (Ready / Needs fixes; counts Critical/Important/Minor), a table of every figure with OK / Fix / Minor, then Important findings (file:line, what you saw, the fix you suggest), Minor findings, what passed, how you checked the existing figures. Plain words.

Reply ONLY (under 12 lines): verdict, counts, the figures that need fixing with one clause each, report path.
