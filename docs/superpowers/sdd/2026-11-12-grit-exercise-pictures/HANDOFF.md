# Handover: exercise pictures (plan in progress)

This folder is a tracked copy of the plan's working notes, which normally live in the git-ignored `.superpowers/sdd/2026-11-12-grit-exercise-pictures/`. The earlier session stopped here so a new session can take over.

## Where things stand

- **Spec:** `docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md` (binding authority).
- **Plan:** `docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md` (13 tasks).
- **Ledger:** `progress.md` in this folder. Read it all: preflight table, rulings R1–R7, parked findings, per-task status.
- **Done:** Tasks 1–3 (kit, Android, web + tools), reviewed and pushed.
- **Task 4 (first 10 figures):** built, reviewed, and fix round 1 applied (commit `757f2ee`, golden `10:d2bea818efe378a3`). Not yet marked complete.

## Next steps

1. Copy this folder back to the working location: `mkdir -p .superpowers/sdd && cp -r docs/superpowers/sdd/2026-11-12-grit-exercise-pictures .superpowers/sdd/`. Work from there, as before.
2. **Scoped re-review of Task 4's fix round 1** against `review-4-report.md` (the 6 Important findings) and the "Fix round 1" section of `task-4-report.md`. Re-render the sheets first:
   `cd web && npm ci && npm run figures:sheet -- --label batch1 Squat Bench Deadlift "Strict press" "DB shoulder press" "Lat pulldown" "Leg press" Pull-up "One-arm DB row" "Romanian deadlift"` (check the exact catalogue names). Sheets land in `web/.figure-sheets/` (git-ignored). Crop them to look at detail; they are very wide.
3. Fix anything the re-review finds (amend the single Task 4 commit if it is still unpushed, otherwise one fix commit).
4. **STOP for the owner's style approval.** Send the owner the dark and light sheets, with a phone notification, and wait for their go-ahead. Style changes go into the kit and renderer before Task 5. Known open points to mention honestly: the pull-up figure is still small; the leg-press back pad is reclined flatter than a typical machine; DB shoulder press is side-on.
5. Then Tasks 5–12 (one per body area; the controller writes their briefs, Ruling R5) and Task 13 (completeness check on, screenshots, pictures page, final review, merge decision).

## Rules that carry over

- One reviewed commit per task, on branch `ccr-2f9842b5-owq43n`. Push the branch after each task completes. Never push or merge to `main`, and no pull request, without the owner's yes.
- Owner testing preference: focused tests while building; the full Android, domain, web and e2e suites once per task before its commit; once more on main before pushing.
- Sources only from acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov, powerlifting.sport. No blogs or forums.
- Owner content directions: the audience is experienced lifters; pins or spotter only on heavy barbell rack work and weights moved over the face.
- Never run `playwright install`. Run e2e with `--trace off`.
- Release all 225 pictures at once; the work stays on the branch until then.
- Before the final merge, delete this tracked folder (`docs/superpowers/sdd/`) so the notes don't ship to `main`.
