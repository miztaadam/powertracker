# SDD ledger — plan: docs/superpowers/plans/2026-11-12-grit-exercise-pictures.md
Spec: docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md (owner-approved). Execution: subagent-driven (owner). Branch base bf1ab78.

## Preflight scan
| Pair / task | Produces vs consumes | Finding |
|---|---|---|
| T1 ↔ T2/T3 | T1: Library.figure, FigureRenderer.shapes/viewBox, Role, facade figureInfo/figureShapes; T2/T3 consume them | ✔ names match. ✗ T2/T3 tests rely on "the Task 1 fixture registered as a test figure", but domain test sources aren't visible to app/web, and no real figure exists until T4 (e2e has nothing to show). |
| T3 ↔ T4 | T3 `figures:sheet -- <area|names…|all>`; T4 calls `figures:sheet -- batch1` | ✗ "batch1" isn't an accepted argument. |
| T1 ↔ T13 | T13 "switch on everyCatalogueExercise_hasFigure" | ✗ T1 never creates that test. |
| T4 ↔ T5–12 | same entry shape, kit grows per batch | ✔ |
| T2 ↔ T3 | same ten test names | ✔ |
| T1 self | area files "start empty" vs fingerprint golden "empty-set" | ✔ consistent (changed by R1 below) |
| T2 self | `guideSheet_noFigure_showsNoPictureArea` needs a catalogue exercise without a figure, which stops existing at T13 | ✗ needs a lookup seam |
| T3 self | creates scripts and e2e; e2e needs a real figure | see R1 |
| T4 self | ✔ |
| T5–12 self | brief extraction: one combined section | handle with a per-batch brief written by controller |
| T13 self | ✔ |

Ruling R1: T1 also ships the real `Bench` entry (draft geometry, guideLines from the Bench guide, checks BarTouches(CHEST)/LockoutOverShoulders) in figures/areas/MainLifts.kt; T2/T3 unit and e2e tests use "Bench"; T4 re-reviews and refines Bench against sources — gives every downstream task a real figure — if wrong: Bench enters before its sourced review (not released until T13 anyway).
Ruling R2: the no-figure UI tests use a lookup seam (Android: a `figureFor: (String) -> ExerciseFigure?` parameter defaulting to `Library::figure`; web: mock `grit.figureInfo`) — the no-figure state must stay testable after all 225 exist — if wrong: slight extra parameter.
Ruling R3: `figures:sheet` accepts `--label <name>` plus names/area/all; T4 runs `npm run figures:sheet -- --label batch1 <10 names>` — if wrong: sheet file name differs.
Ruling R4: T1 creates `everyCatalogueExercise_hasFigure` marked @Ignore with a comment "switched on in the final task" — T13 un-ignores it — if wrong: none.
Ruling R5: Tasks 5–12 get controller-written briefs from the plan's shared section + the row for that batch.

Task 1 (figure kit, domain): dispatched (opus), BASE bf1ab78
Task 1: implementer agent a28e2a70bda87c6aa
Task 1: implementer done f631c97 (domain jvm 228, js 244, app 290, web 642, e2e 76).
Task 1 concerns → T4: confirm shank fraction (0.246H Winter vs 0.221H PMC11233572) with a clearly labelled allowed source; head/neck/torso split, ankle height 0.039H, widths, rack/machine sizes uncited; bench bottom elbow ~142° 2D — check.
Carry to T2/T3: polygons must fill with the non-zero rule (plate ring is one polygon); held equipment drawn last per brief.
Task 1: review dispatched.
Task 1: review Needs fixes — 3 Important: alias test asserts null after unregister (breaks once a real Strict press figure exists); BarOverMidfoot/HipDepth/plural points/grips/view-box check lack pass+fail tests; kit pieces (Dumbbell, CableStack, LatBar, LatPulldownSeat, LegPress45, PullUpBar), FRONT view and angled-slab path untested.
Task 1: minor (deferred): HEAD_RADIUS 10 vs Segment.HEAD 22.75 (head ~12% small); CHEST_BELOW_SHOULDER/CHEST_DEPTH uncited (BarTouches checks self-consistency) → T4 note; registerForTest no global restore guard; FigureChecks elbows.max() not round1; unbounded caches keyed on whole figure; GritDomain import order.
Task 1: fix round 1 dispatched (resume implementer).
Task 1: fix round 1 done d8f7d10 (jvm 242, js 258); tests found+fixed 4 kit bugs. Re-review dispatched.
Task 1: fix round 1/5 (3 addressed, 0 open; commits f631c97..d8f7d10)
Task 1: minor (deferred): view-box check's panel wiring untested; grip-surface contact never reports "into" (intentional, untested).
Task 1: complete (commits bf1ab78..d8f7d10, review clean)
Task 2 (Android pictures): dispatched (fable), BASE d8f7d10
Task 2: implementer agent a0f83578373c85dcb
Ruling R6: catalogue exercises without a figure yet show the empty muted tile on the tab (same as custom) — keeps rows aligned during development; at release every catalogue exercise has one — if wrong: none visible to users.
Task 2: implementer stopped by usage limit (resets 04:40 UTC); work so far in working tree; resume the same agent after reset.
Task 2: hand-back DONE adae3f3; review package review-2.diff; dispatching sonnet reviewer. Note: commit attribution lines to be normalised to constraints.md at squash.
Task 2: reviewer agent a4ac3f2884b152cae dispatched (sonnet), report → review-2-report.md
Task 2: review ✅ Approved (0C/0I/5 minor, parked: per-frame polygon-key hashing in FigureCanvas, hardcoded PANEL_ASPECT, uncached thumbnail lookup/colours, loose test assertions — see review-2-report.md). Attribution amended to constraints.md lines.
Task 2: complete (commit 933006e, review clean)
Task 2 design choices for T3 to match: thumbnail tile surfaceContainerHigh, 6dp corners, figure drawn on tile; guide panels no frame/background; captions onSurfaceVariant.
Task 3 (web pictures + tools): dispatched (fable), BASE 933006e
Task 3: implementer agent a6441f62d1cbfd25d
Side note: project .claude/ team files + CLAUDE.md (owner request, not part of plan) — commit separately after Task 3 lands.
Task 3: hand-back DONE_WITH_CONCERNS 5f5fa78 (playwright speed project → 1 worker; tab open 51–82 ms with all 225 figures — recheck headroom at T13; lazy thumbnails within 600px). Review package review-3.diff.
Task 3: reviewer agent ac602d5d127101930 (sonnet) → review-3-report.md
Team files committed as 288ce94 on top of 5f5fa78 (Task 3 review range stays 933006e..5f5fa78; any T3 fix goes as a separate commit).
Task 3: review spec ✅ quality Changes needed (0C/1I/5M). I1 lazy observer root. Fix round 1: resumed implementer for I1+M2(+M5); parked M1 (recheck speed at T13), M3, M4, M6.
Task 3: fix round 1 commit 0b11d33; scoped re-review dispatched
Task 3: re-review I1 ✅ M2 ✅, no new findings. Parked: M1 (re-measure tab speed with real figures at T13), M3 aspect hard-coded (both apps), M4 CSS-text vitest tests (e2e covers), M6 pictures.html built at T13.
Task 3: complete (commits 5f5fa78 + fix 0b11d33; 288ce94 between is the unrelated owner team-files commit)
Task 4 (first batch, 10 figures): dispatched (opus), BASE 0b11d33
Task 4: implementer agent aa9e498d26a8b04d2 (opus)
Task 4: hand-back DONE_WITH_CONCERNS a56b982, golden 10:5be660af9018f5b4; sheets web/.figure-sheets/batch1-{dark,light}.png. Concerns: ankle DF 20→35, hip flex 120→140 widened (sourced); DB shoulder press side view; deadlift start torso ~19°; strict press bar ~15cm in front at start; bench forearm ~16° off vertical; leg press carriage "Y".
Controller's own sheet look (held back from reviewer to avoid anchoring): deadlift start reads as stiff-leg (hips high, torso near flat); pull-up bar not readable (no visible bar; chin-over-bar unclear; figure small); leg press feet/carriage plate unclear; squat arms/bar placement to confirm.
Task 4: independent review dispatched (sonnet): code diff + sheets.
Task 4: reviewer agent a893da92023b8c3b3 → review-4-report.md
Task 4: review — 6 Important (deadlift, strict press, DB press, leg press, pull-up, DB row), 11 minor; joint-limit widening accepted (Ruling R7: ankle DF 35°, hip 140° accepted — PMC-sourced, conservative — if wrong: some poses slightly beyond typical range). Fix round 1: resumed implementer, amend single commit.
Owner: hand the plan over to a new cloud session (/brain). After T4 fix round 1: push, copy workspace to docs/superpowers/sdd/2026-11-12-grit-exercise-pictures/ (tracked), commit+push, stop here, give owner a paste prompt.
Task 4: fix round 1 done — amended commit 757f2ee, golden 10:d2bea818efe378a3; all 6 Important addressed (concerns: Arm.out kit concept for DB press, DB 13 cm in front; leg press pad reclined to 25°/plate tilt 10° to reach hip 113°; deadlift torso 30.7°; pull-up side view, figure still small; squat/lat/bench minors left).
Task 4: NEXT = scoped re-review of the 6 fixes against review-4-report.md (re-render sheets: cd web && npm run figures:sheet -- --label batch1 <10 names>), then STOP for owner style approval (send dark+light sheets, push notification). Task 4 not yet "complete".
HANDOVER: plan moved to a new cloud session at owner's request; this session stops after this line. Tracked copy of this workspace: docs/superpowers/sdd/2026-11-12-grit-exercise-pictures/.
RESUMED in new cloud session (/brain): branch fast-forwarded to 9de29d5; workspace copied back to .superpowers/sdd/. Commit attribution lines for this session: Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com> / Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy (replaces the lines in constraints.md).
Task 4: sheets re-rendered from 757f2ee (web/.figure-sheets/batch1-{dark,light}.png); catalogue name is "RDL" (not "Romanian deadlift").
Task 4: R7 follow-up — controller checked PMC9403702 (squat positions; hip flexion = 180° − trunk–thigh angle) and PMC10143703 (anterior knee displacement in barbell squats, review) by allowed-domain search: both exist and cover what Body.kt/MainLifts.kt cite them for. Exact hip values not visible in snippets; related PMC7276781 reports ~124–143° hip flexion at full squat depth in healthy adults, consistent with the 140° limit.
Task 4: scoped re-review of fix round 1 dispatched (sonnet-checker), report → review-4-fix1-report.md
Task 4: re-review PASS (0C/0I/5 minor, parked → apply with owner style changes before Task 5; see review-4-fix1-report.md). Gradle blocked in this session by proxy (dl.google.com, plugin portal) — suites not re-run; no code changed since 757f2ee.
Task 4: STOP for owner style approval — sheets sent with phone notification. Task 4 not marked complete until owner's go-ahead.
