# Handover: exercise pictures (plan in progress)

This folder is a tracked copy of the plan's working notes, which normally live in the git-ignored `.superpowers/sdd/2026-11-12-grit-exercise-pictures/`. It is synced so a new session can take over if this one ends.

## Where things stand (synced after Task 8)

- **Spec:** `docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md` (binding authority).
- **Plan:** `docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md` (13 tasks).
- **Ledger:** `progress.md` in this folder. Read it all: rulings R1–R14, parked findings, per-task status.
- **Done, reviewed and pushed:** Tasks 1–8. 114 of 225 pictures: main lifts 25, chest 30, back 33, shoulders 24, plus Leg press and RDL. Branch head after Task 8: `779d924` (golden `114:817892063d845bbc`).
- **Task 9 (arms, 31):** brief `task-9-brief.md`, builder dispatched on BASE `779d924`. If no `feat(domain): arm pictures` commit exists on the branch, start it again from the brief.
- **Briefs ready:** `task-10-brief.md` (quads, 27 new; set its BASE to the reviewed Task 9 commit).
- **Still to brief:** Task 11 hamstrings and glutes (27 new; RDL exists), Task 12 calves 9 + core 17, Task 13.

## The cycle for each batch (Tasks 9–12)

1. opus-builder builds from the brief: one local commit, full suites, report `task-N-report.md`.
2. sonnet-checker reviews from `review-N-brief.md` (copy the last one and adapt): every picture against its guide, older figures pixel-identical, tests real. It has no WebSearch, so the controller spot-checks five citations with WebSearch `allowed_domains`.
3. The controller looks at every render too, rules on the findings, writes `task-N-fix1-brief.md`, and resumes the builder to amend its commit.
4. The controller checks the fixed renders, runs `./gradlew :domain:jvmTest --tests '*figures*' :domain:checkWebDomain`, stops Gradle (`./gradlew --stop; pkill -f kotlin-daemon`), waits about a minute, runs the speed project 3 times (`cd web && npx playwright test --project=speed --no-deps --trace off`), pushes, renders `npm run figures:sheet -- --label <area> <area>` and sends the dark and light sheets to the owner with a phone notification. The owner said to carry on without waiting; they flag pictures afterwards.

## Task 13 (after Task 12)

- Switch on `everyCatalogueExercise_hasFigure`; re-record screenshots; build `docs/library/pictures.html` with `npm run figures:page`.
- **Speed work (see the "Speed finding" entry in progress.md):** the thumbnail warm-up competes with the first taps after start-up; the picker "few hundred" check fails about 1 run in 10 (102–113 ms). Two candidate designs are written there: (a) prioritise and pause the warm-up, (b) build-time thumbnail shapes. Gate: 3 consecutive quiet speed runs pass. Also re-check the Exercises tab opened cold, and rethink the web test `exercisesTab_coldList_buildsOnlyTheNearTilesFigures` (its fixture was reordered in Task 7).
- Parked: PMC ankle/hip limit sources (R7), JS coach golden mocha timeout under load.
- Final whole-branch review by sonnet-checker against the owner's goal; fix what it finds.
- Delete this tracked folder (`docs/superpowers/sdd/`) before the merge. Then STOP: the merge to `main` needs the owner's yes (phone notification).

## Rules that carry over

- One reviewed commit per task, on branch `ccr-2f9842b5-owq43n`. Push the branch after each task completes. Never push or merge to `main`, and no pull request, without the owner's yes. No force-push without the owner's yes (so never push a commit that will be amended).
- Never give two subagents the same files at the same time: batches run one after another (they share the kit files).
- Sources only from acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport. No blogs or forums; never fetch those sites directly.
- Audience: experienced lifters; pins or spotter only on heavy barbell rack work and weights moved over the face.
- Never run `playwright install`. Run e2e with `--trace off`.
- Release all 225 pictures at once; the work stays on the branch until then.
- Cloud environment "powertracker": Android SDK at `/opt/android-sdk`, `local.properties` has `sdk.dir` (git-ignored); the saved setup script is unproven until a fresh container is built.
