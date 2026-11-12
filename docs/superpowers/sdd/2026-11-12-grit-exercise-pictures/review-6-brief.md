# Review brief: Task 6, chest pictures

You check finished work and change nothing in the repo (no edits, no commits, no git checkout/stash/reset, no sed -i in the real tree). If you need to experiment, copy files to a temp folder outside the repo. Sheets you render go to web/.figure-sheets/ (git-ignored) — that is the only thing you may write, plus your report.

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Review the commit at HEAD (`feat(domain): chest pictures`) against its base e6cdd73: `git diff e6cdd73..HEAD`.

Read first:
- The brief the builder had: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-6-brief.md
- Its report: task-6-report.md (same folder). Treat its claims as claims: verify, don't trust.
- constraints.md and implementer-rules.md (same folder); spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- The last review, for the standard expected: review-5-report.md (same folder)

## What to check
1. Every picture against its own guide. The catalogue and guides: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/Chest.kt. The figures: domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/Chest.kt. For each chest exercise:
   - Render it and LOOK at it (Read the PNG): `cd web && node scripts/figure-sheet.mjs --label r6 --columns 1 "<name>"` gives web/.figure-sheets/r6-{dark,light}.png; or the whole area with `npm run figures:sheet -- --label r6chest chest` and crop. Check full size in dark, glance at light, and the thumbnail.
   - Does START and END show what the guide's setup and steps say, to an experienced lifter? Body position, bench angle, where the bar/dumbbells/handles are at each end, grip, feet. Anything anatomically odd (arms through the body, joints bent the wrong way, hands not on the handle, floating kit, body not touching the bench/seat/pad)?
   - Captions: 24 characters or fewer, from the guide's wording, and each fits the pose drawn above it.
   - Where the guide describes a difference from a related exercise (incline vs flat vs decline, close vs wide, low-to-high vs high-to-low cable, etc.), is that difference visible at thumbnail size? Compare near-duplicates side by side.
   - Camera: side or front — does the chosen view show what the guide is about?
   - Pins/spotter drawn only where the guide's safety line asks.
2. Every catalogue chest entry has a figure (by canonical name), and nothing else got one by accident. Count them.
3. Existing 31 figures unchanged: the builder says which kit/renderer changes it made. Render Squat, Bench, Deadlift, Sumo deadlift, DB shoulder press, One-arm row and three others of your choice and compare with how they looked before this task: the sheets rendered before Task 6 are still in web/.figure-sheets/ (batch1-*.png, mainlifts-*.png, and single-figure sheets) — render the same figures with the same arguments under a new label and compare pixels (as review 5 did; only the label text should differ). Do not use git worktree/checkout/stash. Say how you checked.
4. Sources: every position the guide doesn't fix must cite an allowed domain (acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport) in a code comment. Spot-check 5 citations with WebSearch restricted to that domain (allowed_domains) — never fetch the sites directly: does the page exist and support the claim?
5. Code and tests: BatchChestTest has a focused test per figure with real bounds (not bounds so wide they'd pass a wrong pose — try reasoning about 3 of them: would the test fail if the pose were visibly wrong?). New kit pieces each have a size comment and a KitTest case. The laziness rule: `chestFigures` is a map of Lazy, and the new test proves building the real area maps solves no pose — read it and say whether it would actually fail if an area built eagerly. No model names in code or the commit body other than the two attribution lines.
6. Tests: run `./gradlew :domain:jvmTest` (focused is fine: figures package) and `cd web && npm test`; report counts. You don't need to re-run Android or e2e. Do not run the speed tests (the controller does that on a quiet machine). When done, run `cd /home/user/powertracker && ./gradlew --stop`.

## Report
Write to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/review-6-report.md, in the shape of review-5-report.md: verdict line (Ready / Needs fixes; counts Critical/Important/Minor), a table of every figure with OK / Fix / Minor, then Important findings (file:line, what you saw, the fix you suggest), Minor findings, what passed, how you checked the existing figures. Plain words.

Reply ONLY (under 12 lines): verdict, counts, the figures that need fixing with one clause each, report path.
