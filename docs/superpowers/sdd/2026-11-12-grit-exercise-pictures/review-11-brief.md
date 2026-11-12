# Review brief: Task 11, hamstring and glute pictures

You check finished work and change nothing in the repo (no edits, no commits, no git checkout/stash/reset, no sed -i in the real tree). If you need to experiment, copy files to a temp folder outside the repo. Sheets you render go to web/.figure-sheets/ (git-ignored) — that is the only thing you may write, plus your report.

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Review the commit at HEAD (__HEAD__ `feat(domain): hamstring and glute pictures`) against its base __BASE__: `git diff __BASE__..HEAD`.

Read first:
- The brief the builder had: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-11-brief.md
- Its report: task-11-report.md (same folder). Treat its claims as claims: verify, don't trust.
- constraints.md and implementer-rules.md (same folder); spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- The last review, for the standard expected: review-10-report.md and review-9-report.md (same folder)

## What to check
1. Every picture against its own guide. The catalogue and guides: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/HamstringsGlutes.kt. The figures: domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/HamstringsGlutes.kt (27 new; RDL existed and must be unchanged). For each new exercise:
   - Render it and LOOK at it (Read the PNG): `cd web && node scripts/figure-sheet.mjs --label r11 --columns 1 "<name>"` gives web/.figure-sheets/r11-{dark,light}.png; or the whole area with `npm run figures:sheet -- --label r11hg "<the area's id or label — see the top of scripts/figure-sheet.mjs>"` and crop. Check full size in dark, glance at light, and the thumbnail.
   - Does START and END show what the guide's setup and steps say, to an experienced lifter? Body position, bench angle, where the bar/dumbbells/handles are at each end, grip, feet. Anything anatomically odd (arms through the body, joints bent the wrong way, hands not on the handle, floating kit, body not touching the bench/seat/pad)?
   - Captions: 24 characters or fewer, from the guide's wording, and each fits the pose drawn above it.
   - Where the guide describes a difference from a related exercise (RDL vs stiff-leg deadlift vs DB RDL vs single-leg RDL vs good morning; kettlebell swing vs single-arm swing; lying vs seated vs standing vs cable leg curl; Nordic curl vs glute-ham raise; hip thrust vs glute bridge and their barbell, dumbbell, machine and single-leg versions; hip abduction vs adduction, etc.), is that difference visible at thumbnail size? Compare near-duplicates side by side.
   - Camera: side or front — does the chosen view show what the guide is about?
   - Pins/spotter drawn only where the guide's safety line asks.
2. Every catalogue hamstrings-and-glutes entry has a figure (by canonical name), and nothing else got one by accident. Count them.
3. Existing 172 figures unchanged: the builder says which kit/renderer changes it made. Check as review 7 did: render the older areas from a copy of the base commit made with `git archive __BASE__ web | tar -x -C <a temp dir outside the repo>` (link node_modules) and from the current tree, with the same arguments and NEW labels, and compare pixels (dark and light, thumbnails included; only the label line may differ). Render `all` in both. Do not use git worktree/checkout/stash. Say how you checked.
4. Sources: every position the guide doesn't fix must cite an allowed domain (acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport) in a code comment. You have no WebSearch, so do not try to verify pages; instead list in your report every citation in the new back code as: file:line, URL, the claim it is cited for, and whether the same URL appears in domain/.../library/Sources.kt (exact match). The controller checks them.
5. Code and tests: BatchHamstringsGlutesTest has a focused test per figure with real bounds (not bounds so wide they'd pass a wrong pose — try reasoning about 3 of them: would the test fail if the pose were visibly wrong?). New kit pieces each have a size comment and a KitTest case. The laziness rule: the area's map is a map of Lazy and AreaMapsLazyTest still passes. Lessons the brief asked the builder to apply — check each: every test bounds the legs where the guide implies straight/seated legs; captions name something the panel shows from the chosen camera; every new kit piece has a typical-size comment with units; for every new figure with an out-of-plane arm (`out != 0`) its own test would catch a wrong-way elbow. No model names in code or the commit body other than the two attribution lines.
6. Tests: run `./gradlew :domain:jvmTest` (focused is fine: figures package) and `cd web && npm test`; report counts. You don't need to re-run Android or e2e. Do not run the speed tests (the controller does that on a quiet machine). When done, run `cd /home/user/powertracker && ./gradlew --stop`.

## Report
Write to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/review-11-report.md, in the shape of review-10-report.md: verdict line (Ready / Needs fixes; counts Critical/Important/Minor), a table of every figure with OK / Fix / Minor, then Important findings (file:line, what you saw, the fix you suggest), Minor findings, what passed, how you checked the existing figures. Plain words.

Reply ONLY (under 12 lines): verdict, counts, the figures that need fixing with one clause each, report path.
