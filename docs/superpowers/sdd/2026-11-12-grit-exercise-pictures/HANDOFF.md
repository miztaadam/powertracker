# Handover: exercise pictures (plan in progress)

This folder is a tracked copy of the plan's working notes, which normally live in the git-ignored `.superpowers/sdd/2026-11-12-grit-exercise-pictures/`. It is synced so a new session can take over if this one ends.

## Where things stand (synced during the final fix wave, 2026-11-13)

- **Spec:** `docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md` (binding authority).
- **Plan:** `docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md` (13 tasks).
- **Ledger:** `progress.md` in this folder. Read its last 15 entries first: the final review, the owner's decisions and the fix wave.
- **Done, reviewed and pushed:** Tasks 1–13. All 225 pictures; build-time thumbnails `edec171`; completeness check on, screenshots and `docs/library/pictures.html` in `32ea347`. Final whole-branch review: Ready to merge, 0 Critical, 0 Important, 5 Minor (`review-final-report.md`).
- **In progress: the final fix wave** (`task-14-fixwave-brief.md`), new commits on top of the branch head, never amended:
  - A. Owner's finding: in the Smith calf raise (and the Barbell calf raise, same pose) the upper body reads as facing left while the feet point right. Redraw the bar-on-back hold so the body faces the way the feet point. The builder also lists other upright bar-on-back figures with the same look, for the owner; they are not changed.
  - B. Split squat and Smith split squat take 140–240 ms to build; make them quick without changing the drawing.
  - C. The dev server refreshes the thumbnail data when the domain bundle changes.
  - D. One stale comment in `web/e2e/library.spec.ts` (text only).
  - If the builder was cut off: check `git status` and `git log`; fixes already committed are done; finish the rest from the brief. Delete any temporary test file the builder left (for example `TmpDumpTest.kt`) before committing.
- **After the fix wave, in this order:**
  1. Look at the fixed calf raise renders; run `./gradlew :domain:jvmTest --tests '*figures*' :domain:checkWebDomain`, stop Gradle, run the speed project 5 times (`cd web && npx playwright test --project=speed --no-deps --trace off`); regenerate `docs/library/pictures.html` if the builder did not (`cd web && npm run figures:page`).
  2. Send the owner the new Smith calf raise render and the list of other upright bar-on-back figures.
  3. Remove from the branch the files the owner does not want merged: `.claude/agents/haiku-worker.md`, `.claude/agents/opus-builder.md`, `.claude/agents/sonnet-checker.md`, `.claude/skills/brain/SKILL.md`, `CLAUDE.md` (none exists on `main`). Do it last, with `git rm --cached` so the local copies stay: they define the subagents and the brain skill the session uses.
  4. Delete this tracked folder (`docs/superpowers/sdd/`).
  5. Push. Then STOP and ask the owner for a yes on the merge to `main` (phone notification). Never merge or open a pull request without it.
- **Owner decisions (2026-11-13):** the picker "few hundred" speed check stays at 475 rows (only its comment is corrected); the Claude files above are not to be merged; the download size is fine; Android thumbnails off the main thread is a follow-up task (written into `docs/next-update.md`), not part of this merge. The two unrelated sections in `docs/next-update.md` stay unless the owner says otherwise.
- **Usage limit and restarts:** subagents and the session can hit a 5-hour usage limit, and the container can restart; background agents are then lost or stopped. Push finished commits at once; resume a stopped builder with SendMessage or re-dispatch from its brief.

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
