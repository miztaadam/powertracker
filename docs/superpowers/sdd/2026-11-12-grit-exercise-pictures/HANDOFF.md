# Handover: exercise pictures (plan in progress)

This folder is a tracked copy of the plan's working notes, which normally live in the git-ignored `.superpowers/sdd/2026-11-12-grit-exercise-pictures/`. It is synced so a new session can take over if this one ends.

## Where things stand (synced after Task 13b)

- **Spec:** `docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md` (binding authority).
- **Plan:** `docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md` (13 tasks).
- **Ledger:** `progress.md` in this folder. Read it all: rulings R1–R19, parked findings, per-task status.
- **Done, reviewed and pushed:** Tasks 1–12 (all 225 pictures), Task 13a `edec171` (thumbnails worked out at build time, catalogue warmed once after first paint; review Ready) and Task 13b `32ea347` (completeness check on, screenshots, `docs/library/pictures.html`). The pictures page was sent to the owner.
- **Next:** the final whole-branch review from `review-final-brief.md` (set its HEAD to the branch head; a first run was lost to a container restart), then one fix wave as NEW commits (never amend pushed commits), then delete this tracked folder, then STOP and ask the owner about the merge (phone notification).
- **For the owner to decide:** the @speed picker check "a few hundred exercises" lists 475 rows (the 225 catalogue exercises plus 250 stand-ins written before the catalogue grew; its comment at `web/e2e/library.spec.ts:166` is out of date). It passes, at parity with before the pictures, but with little room. The controller does not change that test.
- **Parked for the fix wave:** the dev server does not refresh thumbnail data when the domain bundle changes while it runs (review-13a Minor); `docs/library/pictures/` holds three older draft files.
- **Usage limit and restarts:** subagents can hit a 5-hour usage limit, and the container can restart; background agents are then lost or stopped. Push finished commits at once; resume a stopped builder with SendMessage or re-dispatch from its brief.

## The cycle each batch followed (Tasks 5–12, for reference)

1. opus-builder builds from the brief: one local commit, full suites, report `task-N-report.md`.
2. sonnet-checker reviews from `review-N-brief.md` (copy the last one and adapt): every picture against its guide, older figures pixel-identical, tests real. It has no WebSearch, so the controller spot-checks five citations with WebSearch `allowed_domains`.
3. The controller looks at every render too, rules on the findings, writes `task-N-fix1-brief.md`, and resumes the builder to amend its commit.
4. The controller checks the fixed renders, runs `./gradlew :domain:jvmTest --tests '*figures*' :domain:checkWebDomain`, stops Gradle (`./gradlew --stop; pkill -f kotlin-daemon`), waits about a minute, runs the speed project 3 times (`cd web && npx playwright test --project=speed --no-deps --trace off`), pushes, renders `npm run figures:sheet -- --label <area> <area>` and sends the dark and light sheets to the owner with a phone notification. The owner said to carry on without waiting; they flag pictures afterwards.

## Task 13 (after Task 12)

1. `task-13a-brief.md`: thumbnails worked out at build time (Ruling R15; measurements in progress.md under "Speed finding" and "T13 speed design input"). One commit, reviewed by sonnet-checker. Gate: all four @speed checks pass in 5 consecutive quiet runs. Fallback if the numbers are poor: a prioritised, input-paused warm-up.
2. `task-13b-brief.md`: switch on `everyCatalogueExercise_hasFigure`, clear leftovers, re-record screenshots, build `docs/library/pictures.html`. One commit, reviewed.
3. Send the owner the pictures page; fix any flags.
4. Final whole-branch review by sonnet-checker against the owner's goal (every catalogue exercise has a correct two-pose picture and thumbnail in both apps, speed within budget, nothing else changed); one fix wave.
5. Parked items to close or carry: PMC ankle/hip limit sources (R7), the JS coach golden's mocha timeout under load, Android has no speed check for the thumbnail list.
6. Delete this tracked folder (`docs/superpowers/sdd/`) before the merge. Then STOP: the merge to `main` needs the owner's yes (phone notification).

## Rules that carry over

- One reviewed commit per task, on branch `ccr-2f9842b5-owq43n`. Push the branch after each task completes. Never push or merge to `main`, and no pull request, without the owner's yes. No force-push without the owner's yes (so never push a commit that will be amended).
- Never give two subagents the same files at the same time: batches run one after another (they share the kit files).
- Sources only from acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport. No blogs or forums; never fetch those sites directly.
- Audience: experienced lifters; pins or spotter only on heavy barbell rack work and weights moved over the face.
- Never run `playwright install`. Run e2e with `--trace off`.
- Release all 225 pictures at once; the work stays on the branch until then.
- Cloud environment "powertracker": Android SDK at `/opt/android-sdk`, `local.properties` has `sdk.dir` (git-ignored); the saved setup script is unproven until a fresh container is built.
