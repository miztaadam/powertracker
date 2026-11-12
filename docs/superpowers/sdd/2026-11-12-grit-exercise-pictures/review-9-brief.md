# Review brief: Task 9, arm pictures

You check finished work and change nothing in the repo (no edits, no commits, no git checkout/stash/reset, no sed -i in the real tree). If you need to experiment, copy files to a temp folder outside the repo. Sheets you render go to web/.figure-sheets/ (git-ignored) — that is the only thing you may write, plus your report.

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Review the commit at HEAD (c010525 `feat(domain): arm pictures`) against its base e3c219f (a docs-only commit on top of 779d924): `git diff e3c219f..HEAD`.

Read first:
- The brief the builder had: .superpowers/sdd/2026-11-12-grit-exercise-pictures/task-9-brief.md
- Its report: task-9-report.md (same folder). Treat its claims as claims: verify, don't trust.
- constraints.md and implementer-rules.md (same folder); spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md
- The last review, for the standard expected: review-8-report.md and review-7-report.md (same folder)

## What to check
1. Every picture against its own guide. The catalogue and guides: domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/Arms.kt. The figures: domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/areas/Arms.kt (31 new, none existed). For each arm exercise:
   - Render it and LOOK at it (Read the PNG): `cd web && node scripts/figure-sheet.mjs --label r9 --columns 1 "<name>"` gives web/.figure-sheets/r9-{dark,light}.png; or the whole area with `npm run figures:sheet -- --label r9arms arms` and crop. Check full size in dark, glance at light, and the thumbnail.
   - Does START and END show what the guide's setup and steps say, to an experienced lifter? Body position, bench angle, where the bar/dumbbells/handles are at each end, grip, feet. Anything anatomically odd (arms through the body, joints bent the wrong way, hands not on the handle, floating kit, body not touching the bench/seat/pad)?
   - Captions: 24 characters or fewer, from the guide's wording, and each fits the pose drawn above it.
   - Where the guide describes a difference from a related exercise (DB curl vs hammer curl, curl vs reverse curl vs drag curl, incline vs seated vs standing curls, preacher vs spider vs concentration, bar vs rope vs one-arm pushdown, the overhead/lying extensions, wrist curl vs reverse wrist curl, triangle vs close-grip push-up, triceps dip vs chest dip, etc.), is that difference visible at thumbnail size? Compare near-duplicates side by side.
   - Camera: side or front — does the chosen view show what the guide is about?
   - Pins/spotter drawn only where the guide's safety line asks.
2. Every catalogue arms entry has a figure (by canonical name), and nothing else got one by accident. Count them.
3. Existing 114 figures unchanged: the builder says which kit/renderer changes it made. Check as review 7 did: render the older areas from a copy of the base commit made with `git archive e3c219f web | tar -x -C <a temp dir outside the repo>` (link node_modules) and from the current tree, with the same arguments and NEW labels, and compare pixels (dark and light, thumbnails included; only the label line may differ). Cover at least: shoulders, back, chest, main lifts and the ten Task 4 figures. Do not use git worktree/checkout/stash. Say how you checked.
4. Sources: every position the guide doesn't fix must cite an allowed domain (acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport) in a code comment. You have no WebSearch, so do not try to verify pages; instead list in your report every citation in the new back code as: file:line, URL, the claim it is cited for, and whether the same URL appears in domain/.../library/Sources.kt (exact match). The controller checks them.
5. Code and tests: BatchArmsTest has a focused test per figure with real bounds (not bounds so wide they'd pass a wrong pose — try reasoning about 3 of them: would the test fail if the pose were visibly wrong?). New kit pieces each have a size comment and a KitTest case. The laziness rule: the arms map is a map of Lazy and AreaMapsLazyTest still passes. Lessons the brief asked the builder to apply — check each: every test bounds the legs where the guide implies straight/seated legs; captions name something the panel shows from the chosen camera; every new kit piece has a typical-size comment with units; for every new figure with an out-of-plane arm (`out != 0`) its own test would catch a wrong-way elbow. No model names in code or the commit body other than the two attribution lines.
6. Tests: run `./gradlew :domain:jvmTest` (focused is fine: figures package) and `cd web && npm test`; report counts. You don't need to re-run Android or e2e. Do not run the speed tests (the controller does that on a quiet machine). When done, run `cd /home/user/powertracker && ./gradlew --stop`.

## Report
Write to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/review-9-report.md, in the shape of review-8-report.md: verdict line (Ready / Needs fixes; counts Critical/Important/Minor), a table of every figure with OK / Fix / Minor, then Important findings (file:line, what you saw, the fix you suggest), Minor findings, what passed, how you checked the existing figures. Plain words.

Reply ONLY (under 12 lines): verdict, counts, the figures that need fixing with one clause each, report path.
