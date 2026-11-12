# Review brief: Task 13a, thumbnails worked out at build time (web)

You check finished work and change nothing in the repo (no edits to tracked files, no commits, no git checkout/stash/reset/worktree, no sed -i in the real tree). Experiments go in a copy outside the repo (`git archive HEAD web | tar -x -C <temp dir>`, link node_modules). Never push.

Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Review the commit at HEAD (__HEAD__ `perf(web): thumbnails are worked out when the app is built`) against its base __BASE__: `git diff __BASE__..HEAD`.

Read first: task-13a-brief.md and task-13a-report.md in .superpowers/sdd/2026-11-12-grit-exercise-pictures/ (treat the report's claims as claims), the spec docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md sections 3.2 and 3.4, and review-perf-report.md (the last performance review) in the notes folder.

## What to check
1. The design is what the brief decided: thumbnails come from data built when the app is built (a Vite plugin / virtual module or equivalent), from the generated domain bundle; no thumbnail pose is solved in the browser; the idle warm-up (`prepareThumbnails`) and everything only it used are gone, with no leftover call, test, comment or dead helper. Search for leftovers.
2. It cannot go stale: nothing generated is committed; `vite build`, `vite dev` and vitest all get the data from the same plugin; changing a figure in the domain bundle changes the thumbnail with no manual step. Prove it in a copy: change one number in web/src/domain/generated's figure output path if that is feasible, or reason from the code and say which.
3. The picture is the same: the equality test compares, for EVERY catalogue figure, the decoded build-time shapes with `grit.figureShapes(name, thumbnailPose)`; read it, check its tolerance, and check it would fail if one shape were dropped, a role changed or a coordinate moved by more than the tolerance (mutate in your copy and run it). Render the full contact sheet (`npm run figures:sheet -- --label r13a all`, dark and light) and compare its thumbnails with a sheet rendered from the base commit copy: the thumbnails must match to within the rounding (say what the largest pixel difference is, if any).
4. Behaviour kept (run the tests, read them): a tile that starts on screen is drawn at first paint; far rows' drawings are still mounted lazily (or the report shows with numbers why not); theme following (role classes, not baked colours — check dark and light renders); the picker shows no thumbnails; tiles are hidden from screen readers; the lifter's own exercise shows an empty tile; a name with no figure draws nothing.
5. The rewritten test for the cold list (ExercisePicture.test.tsx): does it pin what is now true, and is anything that used to be pinned now unpinned?
6. Size and start-up: read the report's numbers (bundle raw/gzip, precache total, parse time) and check them against the built `web/dist` yourself (`npm run build`, then file sizes and gzip sizes). Say whether the data's format is sensible and whether the parse cost claim is measured or assumed.
7. scripts/figure-sheet.mjs and scripts/pictures-page.mjs still work (run each once with a small argument; the page script may write to docs/ — run it in your copy, not the repo).
8. Code quality: the plugin is small and readable, typed, with its own unit test; no model names except the commit's attribution trailer; no change to the @speed tests, their threshold, playwright.config.ts, Kotlin or app/.
9. Tests: `cd web && npm test && npm run typecheck && npm run build`; report counts. Do not run e2e or the speed tests (the controller does both on a quiet machine).

## Report
Write to /home/user/powertracker/.superpowers/sdd/2026-11-12-grit-exercise-pictures/review-13a-report.md: verdict (Ready / Needs fixes; Critical/Important/Minor counts), findings with file:line and a suggested fix, what you mutated and what happened, sizes you measured, test counts. Plain words.

Reply ONLY (under 12 lines): verdict, counts, each Important in one clause, sizes, test counts, report path.
