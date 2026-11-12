### Task 3: Web pictures and tools

**Files:**
- Create: `FigureSvg.tsx`, `ExercisePicture.tsx`, `ExercisePicture.test.tsx`, `web/e2e/pictures.spec.ts`, `web/scripts/figure-sheet.mjs`, `web/scripts/pictures-page.mjs`.
- Modify: `FormGuideSheet.tsx`, `ExerciseBrowser.tsx` (`thumbnails?: boolean`), `ExercisesScreen.tsx` (passes `thumbnails`), `library.css`, `package.json` (scripts `figures:sheet` and `figures:page`), `.gitignore` (`web/.figure-sheets/`), and the screenshot spec.

**Interfaces:**
- Consumes: the facade's `grit.figureInfo` and `grit.figureShapes`.
- Produces:
  - `<ExercisePicture name />`;
  - `<ExerciseThumbnail name? />`;
  - `npm run figures:sheet -- <area|names…|all>`, which writes `web/.figure-sheets/<label>-dark.png` and `-light.png` (6 figures per row, captions under each panel, the name above each figure);
  - `npm run figures:page`, which writes a self-contained `docs/library/pictures.html` (inline SVG, grouped by body area, dark and light toggle).

**Decisions:**
- Colours are CSS variables:
  - `BODY_FAR` = `color-mix(in srgb, var(--text) 55%, var(--surface))`;
  - `ACCENT_FILL` = `var(--accent)` at `fill-opacity: .10`.
- Shapes are memoised per name and pose.
- The thumbnail SVG has `aria-hidden="true"`. The guide picture has `role="img"` with the label.

**Steps:**

- [ ] **Step 1: Write the failing tests:**
  - in `ExercisePicture.test.tsx`: the same ten test names as Task 2, using 375 px for the width check;
  - in `pictures.spec.ts`: the guide sheet picture fits at 375 px, and the Exercises tab thumbnails render.
  - Extend the Exercises tab `@speed` check: open, type and filter, each within 100 ms, with thumbnails.
- [ ] **Step 2:** Run `npx vitest run src/screens/library/ExercisePicture.test.tsx`. Expected: FAIL.
- [ ] **Step 3:** Implement, including both scripts. Check `figures:sheet` on the fixture.
- [ ] **Step 4:** Run the full suites and re-render the screenshots. Expected: pass.
- [ ] **Step 5:** Commit `feat(web): exercise pictures, contact sheet and pictures page tools`.

