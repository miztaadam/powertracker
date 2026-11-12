## Global Constraints

- **Style:**
  - follows `docs/library/pictures/bench-press-figure-draft.svg`: a solid figure, equipment at real-world scale, the bar and plates in the chalk accent;
  - two still poses per exercise, with no animation.
- **Sources:**
  - allowed: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport;
  - search with WebSearch `allowed_domains` only;
  - no blogs or forums, and never fetch these sites directly or work around bot checks.
- **Content:**
  - every picture agrees with its exercise's guide;
  - captions are at most 24 characters, non-blank, in the guide's wording;
  - where sources disagree, use the most conservative position;
  - pins or spotter appear only where the guide's safety line asks for them (heavy barbell rack work and weights moved over the face).
- **Guide sheet:**
  - the picture goes under the name and the area and equipment line, above the purpose text;
  - two panels side by side, each with its caption underneath;
  - one accessibility element, labelled `Picture: <start caption>, then <end caption>.`;
  - an exercise with no figure shows no picture area. Custom exercises keep `No form guide for your own exercises.`
- **Exercises tab:**
  - a thumbnail about 56 × 40 to the left of each row's name, hidden from screen readers;
  - custom exercises show an empty muted tile of the same size;
  - the row target stays at least 48;
  - the Plan and Today picker shows no thumbnails.
- **Colours:** role → token mapping:
  - `BODY` → main text colour;
  - `BODY_FAR` → main text colour mixed toward the surface;
  - `EQUIPMENT` → muted;
  - `ACCENT` → chalk accent;
  - `ACCENT_FILL` → chalk accent at low opacity;
  - `GAP` → the picture's surface.

  `BODY`, `EQUIPMENT` and `ACCENT` must each be at least 3:1 against the sheet surface in dark and light.
- **Layout:** no overflow at 360 dp or 375 px, and the captions are never cut off with an ellipsis.
- **Release:**
  - no schema, setting or backup change;
  - the work stays on branch `ccr-2f9842b5-owq43n`;
  - the merge to `main` happens once, after all 225 are done, with the owner's go-ahead.
- **Testing practice (owner):**
  - focused tests while building;
  - the full domain, app, web and e2e suites once per task, before its commit;
  - e2e always with `--trace off`;
  - never `playwright install`.
- **Commits:**
  - one squashed commit per task, using the title given in the task;
  - no model names;
  - each ends with the attribution lines given in the dispatch.

## Review Focus

- **Built-in rows named by an alias or a former name:** for example an "Overhead press" row from before the rename. Expected: it shows the Strict press picture. (Pinned in Task 1: `figure_resolvesAliasesAndFormerNames`.)
- **Large text:** at 200% font scale, the captions wrap and are never cut off, and the panels don't overflow. (Pinned in Tasks 2 and 3: `picture_largeText_captionsWrap`.)
- **Overlapping limbs:** for example an arm crossing the torso, or the far arm behind a bar. Expected: still readable. Near limbs are drawn after the body with a `GAP` outline, and far limbs before it. (Pinned in Task 1: `renderer_drawOrder_farBeforeBody_nearAfterWithGap`.)
- **Scrolling all 225 thumbnails:** expected to stay responsive on the web. (Pinned in Task 3: the Exercises tab `@speed` check, extended to include thumbnails.)
- **Switching theme while a sheet is open:** expected to recolour without a reopen, because colours are roles resolved at draw time. (Pinned in Tasks 2 and 3: `picture_followsThemeChange`.)

---

## Environment
- Repo /home/user/powertracker, branch ccr-2f9842b5-owq43n. Never push. Never switch branches.
- Domain JS bridge regenerated with `./gradlew :domain:syncWebDomain`; `:domain:checkWebDomain` must pass.
- Full suites: `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Known flake: e2e reload.spec (toHaveValue timeout) — re-run once if it fails; don't edit it.
- Chromium is preinstalled; require('playwright') works. Never run `playwright install`.
- Commit attribution lines (end of every commit message):
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01KboHw3MyLtVXrWR45tFnCq
