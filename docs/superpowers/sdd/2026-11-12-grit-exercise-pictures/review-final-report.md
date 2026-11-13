# Final review: exercise pictures branch (HEAD 186b7ae, code at 32ea347)

## Verdict
**Ready to merge** once the owner has seen the "for the owner" list. Critical 0, Important 0, Minor 5.

Nothing in the repo was changed by this review. Experiments ran in copies under the scratchpad folder (`rf-copy`, `rf-jvm`, `rf-base`). I wrote contact sheets with new labels `rf12` and `rfall` under `web/.figure-sheets/` (ignored by git). The tree is clean (`git status` empty) and Gradle daemons are stopped.

The merge base is d795ffe (after `git fetch origin main`). The diff is 197 files; about 70 of them are the working notes to delete (docs/superpowers/sdd).

## Spec table (docs/superpowers/specs/2026-11-12-grit-exercise-pictures-design.md)
| § | Verdict | Where it shows |
|---|---|---|
| 1 Purpose (225 exercises, code-drawn, two poses, guide sheet and tab only, picker unchanged, no data change) | Met | 225 of 225 in `Figures.all` / `grit.library()` (my script: 225 figures, 0 missing); no schema, setting or backup file in the diff |
| 2.1 Body (segments, fixed lengths, joint limits) | Met | `Body.kt`, `BodyTest`, `FigureChecks` (joint-limit check on every pose) |
| 2.2 Kit (pieces added per batch, real scale, held pieces follow the pose) | Met | `Kit*.kt`, `KitTest` |
| 2.3 Figure entries (keyed by catalogue name, `Library.figure` resolves aliases) | Met | `Library.kt`; my check: "Overhead press" gives the Strict press captions; own exercise gives null |
| 2.4 Shapes (roles, draw order, fixed 400 x 300 view box, cache, JS facade) | Met | `FigureRenderer.kt`; all 225 view boxes are 400 x 300; `GritDomain.figureInfo/figureShapes/hasFigure` |
| 3.1 Guide sheet (under the name line, above purpose; two panels with captions; no overflow at 360 dp / 375 px; one labelled element; no picture area when no figure) | Met | Android `ExercisePicture.kt`, `FormGuideSheet.kt`; web `ExercisePicture.tsx` (`<figure role="img" aria-label="Picture: A, then B.">`, captions and svg `aria-hidden`); tests below |
| 3.2 Exercises tab thumbnail (56 x 40, left of the name, own exercises get an empty tile, target >= 48, hidden from screen readers, picker unchanged) | Met | Android `ExerciseThumbnail` (no semantics; `showThumbnails` only true on the tab); web `.lib-thumb` `aria-hidden`; `picker_hasNoThumbnails` in both apps |
| 3.3 Colours (role to token map, 3:1 contrast, Canvas on Android, inline SVG on the web) | Met | Android `FigureColors`, web `figure.css`. Token hex values are identical in `tokens.css` and `Color.kt` (dark and light). Far-limb mix is 45% toward the surface on both (`lerp(..., 0.45)` and `color-mix(... 55%)`); accent fill 10% on both. `picture_contrast_darkAndLight` in both apps |
| 3.4 Speed (cached per exercise, visible rows only, web check extended to the tab) | Met (not run by me, as instructed) | `pictures.spec.ts` "Exercises tab speed" `@speed`; controller reports 5 of 5 runs at 4/4 |
| 4.1 Sources | Met as far as can be checked here | Citations were spot-checked per batch by the controller; R7 is parked (see below) |
| 4.2 Automatic checks (limits, contacts, held items, key positions, no intersection, fit, integrity, completeness, parity) | Met | `FigureChecks.kt`, `FiguresTest` (7 of 7; `everyCatalogueExercise_hasFigure` is on, no `@Ignore`), golden `225:4cef8580b4d880a3` (count 225). My own check: JVM and JS shapes are identical for all 450 panels (13,478 shapes, largest difference 0) |
| 4.3 Review (sheets per batch, independent reviewer, owner page) | Met | review-4 to review-12 reports; `docs/library/pictures.html` regenerates byte for byte (same md5 from a clean copy of HEAD) |
| 5 Tests, eight names in both apps | Met | all eight exist under the exact names in `app/.../ExercisePictureTest.kt`, `web/src/screens/library/ExercisePicture.test.tsx` and `web/e2e/pictures.spec.ts`. I read the Android bodies: each asserts what its name says (e.g. `picker_hasNoThumbnails` scrolls to Bench in the picker and finds zero `exercise-thumbnail` nodes; `guideSheet_pictureLabel_readsBothCaptions` checks the label text, `Role.Image` and no child semantics) |
| 5 Screenshots, dark and light, both apps | Met | Android `docs/design/screens/`: `form-guide-picture-*` (bench), `form-guide-*` (squat), `form-guide-lat-pulldown-*`, `exercises-*`. Web `docs/design/web/screens/`: same names. I looked at the lat pulldown guide in both apps: same two figures, same captions |
| 6 Delivery (one merge, `docs/library/pictures.html`, no data change) | Met | page present, 3.5 MB, regenerates identically. Size: about 150-300 KB of code per app was estimated; the web came out larger (Minor 5) |

## Findings
No Critical and no Important findings.

**Minor 1: first open of two guides, and first scroll to two rows, is slow (both apps).** `Split squat` and `Smith split squat` take far longer to build than the rest. Median per figure is 0.9 ms on the JVM. The two split squats cost 140 ms and 190 ms on the JVM (one cold run in a unit test, after about 100 other figures). In Node/JS they cost 220 and 242 ms for `figureInfo` alone, 346 and 259 ms for build plus both poses. The next slowest are 54-67 ms. The cost is in building the figure (a solver in the area file), not in drawing. Where it lands:
- Web: opening those two guides (thumbnails come from build data, so the tab is not affected). The speed gate does not cover guide opening (spec §3.4 only asks for the tab).
- Android: `ExerciseThumbnail` calls `Library.figure(name)` and `FigureRenderer.shapes` inside composition (`app/src/main/kotlin/com/powertracker/ui/library/ExercisePicture.kt`, `remember(figure)`), so it runs on the main thread when the row scrolls in. A phone will be several times slower than this JVM, so I'd expect a visible hitch of roughly half a second, once per process, for those two rows. All other figures solve in under 20 ms on the JVM (3 over 20 ms). Each figure's result is then cached.
Suggested fix (follow-up, not a blocker): find why the split-stance figures build so slowly in the Quads area file and make the solve cheaper; or on Android compute thumbnails off the main thread (`produceState` on `Dispatchers.Default`, or a background warm-up that does not compete with scrolling).

**Minor 2: `web/e2e/library.spec.ts:166` comment is untrue.** See the owner list, item 1.

**Minor 3: the Vite dev server does not refresh the thumbnail data if the domain bundle is re-synced while it is running** (`web/plugins/figureThumbnails.ts`, `addWatchFile` on a virtual module). Build, vitest and a fresh dev start do. Already known from the 13a review. Fix: restart the dev server after `:domain:syncWebDomain`, or invalidate the virtual module in a `handleHotUpdate` for the bundle.

**Minor 4: unrelated files ride on this branch** (see Leftovers). The branch also carries a commit with model names in its message body.

**Minor 5: web size.** The spec said about 150-300 KB of code per app before compression. Measured by building the merge base and HEAD with `vite build` in copies:
- main bundle 983.9 KB to 1,582.1 KB raw (+598 KB), gzip 273.0 KB to 438.4 KB (+165 KB)
- precache 1,213.7 KiB to 1,798.8 KiB (+585 KiB)
Of that, the thumbnail data is 180.8 KB raw / 51.9 KB gzip; the rest is the figure code inside the Kotlin/JS domain bundle. This is about 2x the spec's estimate. For a phone PWA it is acceptable: +165 KB gzip once, then cached by the service worker; the 13a report measures the thumbnail data's parse at about 0.5 ms. It does load in the main bundle at start-up, so first launch on a slow connection pays it. I do not think it blocks the merge, but it is outside the spec's number, so the owner should know.

## Cross-app comparison (12 figures)
Figures: Squat (main lifts, rack, side), Sumo deadlift (main lifts, front view), Pec deck (chest, machine, front), Lat pulldown (back, machine/cable), DB pullover (back, spotter), Lateral raise (shoulders, front), Triceps pushdown (arms, cable), Leg press (quads, machine), Glute bridge (hamstrings and glutes, floor), Standing calf raise (calves), Front plank (core, bodyweight floor), Cable woodchop (core, cable, front-view turn). All nine areas are covered. The sheet is `web/.figure-sheets/rf12-dark.png` (and `-light`).

How I compared:
1. **Same shapes.** I dumped every shape of every figure from the JVM (`Figures.all` + `FigureRenderer.shapes`, in a temporary JUnit test in a copy) and from the JS bundle (`GritDomain.figureShapes`, in Node), for all 225 figures x 2 poses: 450 panels, 13,478 shapes. Kind, role, order and every number are identical (largest difference 0). This covers the 12 and the rest.
2. **Same drawing code, read side by side.** Android `FigureCanvas.kt` against web `FigureSvg.tsx` and `figure.css`:
   - Draw order: both draw the list in the order given.
   - Colours and roles: BODY, BODY_FAR, EQUIPMENT, ACCENT, ACCENT_FILL, GAP map to the same tokens, with the same hex values (checked in both themes); the thumbnail tile is `surfaceContainerHigh` / `--container-high` on both.
   - Capsules: round-capped lines on both. Polygons: non-zero fill on both (plate rings).
   - View box: Android fits 400 x 300 whole and centred in the canvas; the web SVG uses the default meet/centre. Thumbnails are 53.3 x 40 inside 56 x 40 on both.
   - One difference in principle: an SVG clips to its box and a Compose `Canvas` does not. It cannot show, because the domain's fit check keeps every shape inside the view box.
3. **Screenshots.** I looked at the committed Android and web guide screenshots for Lat pulldown (dark): same two drawings and captions. The Android tests also check the drawn pixel colours in each panel against the role colours.
I did not run the Android screenshot tests again or draw the 12 through Compose. The comparison for Android is the identical shapes plus the code reading above.

## Outliers (whole sheet, dark, thumbnail size; `rfall-dark.png`)
No pose re-argued. Nothing is plainly wrong. Floor lines are present on every figure that stands or lies on the floor; none doubled. Captions: 225 of 225 non-blank, longest is exactly 24, none over, no start caption equal to its end caption. Style is consistent across batches (same body, line weight, accent for bar/plates/handles, equipment grey, far limbs lighter). Things a reader will notice:
- Smaller than their neighbours in the tile, because the fit is by the tallest/widest item: the whole pull-up family (Pull-up, Weighted pull-up, Assisted pull-up, Band-assisted pull-up, Neutral-grip pull-up, Chin-up, Weighted chin-up, Assisted chin-up), the standing no-equipment figures (One-arm DB press, Arnold press, Kettlebell press, Front raise, Lateral raise, Suitcase carry, Waiter's carry, Lateral band walk, Lateral lunge, Curtsy lunge) and the tall rack figures. They are thin and tall at 56 x 40, but readable.
- Larger/denser: floor figures (Front plank, Side plank, Glute bridge, Clamshell, Side-lying hip abduction) fill the width; their thumbnails are wide and short.
- Rear delt raise / Seated rear delt raise / Chest-supported rear delt raise: the head is tucked into the bent torso, which reads as headless at thumbnail size. Ruled on in batch 6/7; mentioned only because it is the one place a reader may think a figure is missing a part.

## Speed design (task 13a)
- **No leftover warm-up code.** No hits for `prepareThumbnails`, `prepareDrawing`, `whenIdle` or `requestIdleCallback` anywhere in web/src, e2e, scripts, app or domain. The one warm-up left is `warmCatalogue()` (`web/src/state/exercisePicker.ts`, called in `main.tsx`), which per ruling R19 warms the catalogue only (library + `searchLibrary('', [], [])`); `warmCatalogue.test.ts` checks it never touches a figure.
- **Data cannot go stale.** `web/plugins/figureThumbnails.ts` generates the data from the generated domain bundle at build, dev start and vitest start. No generated thumbnail file is tracked (`git ls-files` shows only source files). I tested the whole chain: in a copy I changed a Kotlin body constant, ran `:domain:syncWebDomain`, then ran the plugin against the old and the new bundle. All 225 thumbnail entries changed with no other step. A bundle edited without a sync is caught by `:domain:checkWebDomain` (passed).
- **Equality test covers every figure.** `exercisesTab_thumbnailData_isEveryFiguresThumbnailPose_asTheDomainDrawsIt` (ExercisePicture.test.tsx:297) asserts the keys equal the 225 catalogue names, and for each: same view box, same kinds and roles in order, every number within 0.05 view-box units (0.007 px at 56 px wide).
- **Cold list.** A tile that is far off screen is not drawn (IntersectionObserver); the cold-list test pins that fewer than 30 draw.
- **Sizes.** Matches the 13a report: main bundle 1,582,093 B raw / 438,359 B gzip; precache 1,798.82 KiB; thumbnail data 180,817 B raw / 51,850 B gzip, parsed once at start-up in about 0.5 ms. Reasonable for a phone PWA (see Minor 5).
- Not run: the speed tests. The 13a numbers: tab opened right after load median 75 ms (worst 89) against 105 (worst 143) before; picker with 475 rows median 62.5 against 61.

## Android thumbnails (advice for the owner)
There is no speed check on Android. The tab is a `LazyColumn`, so only visible rows compose; each row's `ExerciseThumbnail` looks the figure up and, on first composition, solves its thumbnail pose on the main thread (`remember(figure)`, then cached globally in `FigureRenderer`). The shapes list is remembered per row and the polygon paths per shape list. Per-figure cost on the JVM across all 225 (cold JIT, build plus both poses): median 0.9 ms, 95th percentile 5.4 ms, 3 figures over 20 ms (Squat 20 ms, Split squat 141 ms, Smith split squat 191 ms), sum 691 ms. A phone is slower but nearly every row costs well under a frame or two. The exception is the two split squats (Minor 1). Worth a small follow-up task, not a blocker.

## Leftovers and hygiene
- **@Ignore / skip / todo added by the branch:** none net. The only `@Ignore` ever added (`everyCatalogueExercise_hasFigure`) was removed in 32ea347; no `.only`, `.skip`, `fixme`, `@Disabled`, `assume` in the diff. No `debugger`; the `console.log` lines are in the two CLI scripts (`web/scripts/figure-sheet.mjs`, `pictures-page.mjs`), which are meant to print.
- **Unrelated to the pictures** (tracked in the diff): `.claude/agents/{haiku-worker,opus-builder,sonnet-checker}.md`, `.claude/skills/brain/SKILL.md`, `CLAUDE.md` (commit 288ce94 "chore: project-level agent team and /brain skill"); and two sections in `docs/next-update.md` that are not about pictures ("How future projects are built", "Next update item: app version and AppsbyAdam"). The owner should decide whether they ride along with this merge.
- **Model names:** none in app, domain, web source, tests or scripts. Commit 288ce94's message body names `opus-builder, sonnet-checker and haiku-worker`; all other commits name a model only in the trailer lines. The spec and plan (docs/superpowers) name models in the "Who builds" lines; the tracked notes do too.
- **Generated web bundle matches the Kotlin:** `:domain:checkWebDomain` passed.
- **docs/superpowers/sdd/:** 70 tracked working-note files. Must be deleted before the merge (`git rm -r docs/superpowers/sdd`). Nothing in the app or docs links to it (searched the whole tree outside sdd and `.superpowers`). The spec and plan stay under docs/superpowers/specs and plans.
- **.gitignore** covers `web/.figure-sheets/` and `.superpowers/` (checked with `git check-ignore`).
- **docs/library/pictures/** (three files: `bench-press-figure-draft.svg`, `bench-press-example.jpg`, `bench-press-example.brief.md`). Both the old and new files were added before this plan (commits 0516d9d, 7268757). What links to them:
  - The SVG draft is referenced by the spec (line 13), the plan (line 23), `domain/.../areas/MainLifts.kt:107` and `domain/commonTest/.../FigureFixtures.kt:6` (code comments naming it as the style the figures were drawn from), and `docs/next-update.md:77`.
  - The jpg and the brief are referenced only by `docs/next-update.md:77` ("kept only as a record of what was tried") and the brief's own text.
  - Nothing links to `docs/library/pictures.html` from these files, and `pictures.html` does not link to them (it is self-contained; the page was opened offline with no requests).
  - Recommendation: keep the SVG (the code comments and the spec still point at it). The jpg and its brief are the owner's record of the paid-service trial; nothing breaks if they stay or go. The folder and the file `pictures.html` sit side by side without a clash. If the owner wants a tidier docs/library, the jpg and the brief could be deleted and the sentence in next-update.md edited. Their call.

## Parked items (progress.md)
- **PMC ankle/hip limit sources (R7): still open, needs the owner's acceptance.** Ankle dorsiflexion 35° and hip flexion 140° are widened beyond the first values; the controller found the PMC pages exist and are consistent (PMC7276781 gives about 124-143° hip flexion at full depth), but the exact numbers were not visible in the snippets. The risk if wrong is that a few poses sit slightly beyond a typical range; no one has flagged a figure that looks wrong.
- **JS coach golden mocha timeout under load: still open, not reproduced.** No change in the branch touches it. It passed in my full run (jsNodeTest 560 of 560, no timeout). It stays a known flake.
- **Picker speed margin: still open, needs the owner.** After 13a the picker with 475 rows opens at a median of 62.5 ms (BASE 61 ms), so there is no more room than before; the controller got 5 of 5 gate runs. I did not run the speed tests. See the owner list, item 1, for what that test now measures.
- **Cold tab: closed.** The tab no longer solves poses; opened right after load its median fell from 105 ms to 75 ms and its worst from 143 to 89 ms.
- **Web test rewritten in 13a (`exercisesTab_browseByMuscleAndSearchWork`): closed.** Only its set-up changed (`layOutLikeAPhone`); it passes in my full vitest run, and a guard in the test pins that fewer than 30 tiles draw.

## Tests I ran
- `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest`: BUILD SUCCESSFUL. jvmTest 545, jsNodeTest 560, app 302; 0 failed, 0 skipped. Note: Gradle reported `:domain:jvmTest` and `:app:testDebugUnitTest` as UP-TO-DATE (it re-ran only `jsTest` and `checkWebDomain`), so those two counts come from the result files of the last identical run at this commit. `:domain:checkWebDomain` passed.
- `cd web`: `npm test` 41 files, 661 passed; `npm run typecheck` clean; `npm run build` OK (precache 19 entries, 1,798.82 KiB).
- `npm run e2e -- --trace off --project e2e`: 88 passed (the speed project excluded as instructed). No flakes, no re-run needed.
- `./gradlew --stop` run at the end; tree clean afterwards.
- Extra checks: JVM vs JS shapes identical for 450 panels; `pictures.html` regenerates to the same md5 from a clean copy; stale-data experiment (Kotlin change, sync, thumbnails change); merge-base vs HEAD bundle sizes.

## For the owner
1. **What the "few hundred" picker test now measures.** `web/e2e/library.spec.ts:166` says "The catalogue grows to about 250 entries; until it has, the lifter's own exercises stand in for them." That was written to measure the picker at about 250 rows, with 250 made-up exercises standing in for a catalogue that did not exist yet. The catalogue is now complete at 225, and the test still adds 250, so it lists 475 rows (it asserts `catalogue.length + 250`). It therefore measures the picker at nearly twice the size its own comment says it was meant for, and it passes at about 62 ms median (BASE: 61), so it is the tightest of the speed checks. The picker at the real size (225) is covered by the test above it. Decision for the owner: (a) keep 475 as a deliberate stress size and correct the comment, or (b) re-aim it at the size the comment names. I am not proposing to loosen the 100 ms gate in either case. The 13b builder left the comment alone on purpose.
2. **The unrelated files on the branch** (`.claude/` agents and skill, `CLAUDE.md`, two unrelated sections in `docs/next-update.md`): merge them or move them to their own commit/branch.
3. **docs/library/pictures/**: keep the SVG; the jpg and brief are the owner's call (see Leftovers).
4. **R7 limits (ankle 35°, hip 140°)**: accept as drawn, or ask for a source check.
5. **Web size** is about twice the spec's estimate (+165 KB gzip, +585 KiB precache); acceptable in my view, but it is outside the spec's number.
6. **Split squat and Smith split squat** are slow to build (see Minor 1); a follow-up task, or accept.
7. **Before the merge:** delete docs/superpowers/sdd (70 files), run the full suites once more on main, and get the owner's go-ahead (spec §6).
