# Exercise library, form guides and ⓘ — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a catalogue of about 250 gym exercises to the shared domain, each with a researched form guide. Give both apps a full-screen searchable picker and an ⓘ guide sheet wherever an exercise appears. Change no data, schema, backup format or coach rule.

**Architecture:**
- The catalogue is static Kotlin data in `domain/…/library/`. It holds one file per body area and a source registry, and both apps get it: Android directly, the web through the `GritDomain` JS facade.
- The pickers browse the catalogue. A catalogue exercise becomes a database row only when it is first added to a plan or logged. The row is matched by name, ignoring case, and is reused, restored or inserted in the same transaction as the plan item or set.
- Guide content arrives in body-area batches. Each content batch is a task whose reviewer checks every claim against the recorded sources using web search.

**Tech Stack:** Kotlin Multiplatform (`:domain`, JVM and JS), Jetpack Compose with Room (Android), React and TypeScript with Dexie (web), Vitest, Robolectric and Playwright.

**Spec:** `docs/superpowers/specs/2026-11-11-grit-exercise-library-design.md`

## Global Constraints

- **Unchanged:** `AccessoryMap`, every coach rule, `SeedLibrary` names, ids and order, the Room and Dexie schemas, and the backup format and its fixtures.
- **Catalogue names:** every catalogue name is unique, ignoring case, and never collides with any alias. A shipped name is never renamed; a later rename keeps the old name as an alias.
- **Seed and coach names:** every `SeedLibrary` name and every `AccessoryMap` exercise name exists in the catalogue with exactly that spelling.
- **Guide shape:** every guide has a purpose of one or two sentences, non-empty setup and steps, 2–3 cues, 2–4 mistakes, and at least one `SourceId` from the registry.
- **Fixed last line** of every guide sheet, verbatim: `Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist.`
- **Custom exercise sheet text**, verbatim: `No form guide for your own exercises.`
- **Picker empty state**, verbatim: `No exercises match. Try fewer filters, or create your own.`
- **Allowed sources:**
  - strength and conditioning bodies: NSCA, ACSM, ACE;
  - established exercise references: ExRx;
  - peer-reviewed papers on pmc.ncbi.nlm.nih.gov;
  - for the competition lifts, IPF material (powerlifting.sport).

  Never blogs, forums, social media, video comments or AI-written pages. ExRx forum threads are forums.
- **Where sources disagree:** use the most conservative version, or leave the point out.
- **Content rules:**
  - setup and bracing come before cues;
  - barbell work in a rack mentions safety pins or arms, or a spotter;
  - never advise training through pain;
  - no medical or rehab advice;
  - plain words, short sentences.
- **Search normalisation:** lowercase, strip diacritics, treat `-` and `_` as spaces, and collapse runs of spaces.
- **Search ranking:**
  1. names starting with the query;
  2. names containing it;
  3. aliases;
  4. muscles.

  Results within a rank are alphabetical.
- **Defaults when a catalogue exercise is added to Plan:** `COMPOUND` is 3 × 5, `ACCESSORY` is 3 × 10, both at the settings default RPE.
- **Layout:**
  - ⓘ label: `Form guide for <name>`;
  - touch targets ≥ 48 px or dp;
  - body text ≥ 16 px or sp;
  - no overflow at 375 px (web) or 360 dp (Android);
  - the web search input is ≥ 16 px;
  - on the web, picker open, keystroke and filter each settle within 100 ms in e2e.
- **Hygiene:**
  - every promise that isn't awaited has a `.catch`;
  - web tests unmount before closing the db;
  - e2e data checks use `expect.poll`.
- **Commits** end with the session's attribution lines.

## Review Focus

1. **A custom exercise with the same name as a catalogue entry**, for example a user-made "Leg press" from before this update. Adding the catalogue "Leg press" reuses the user's row instead of creating a duplicate, and ⓘ shows the catalogue guide. Pinned in L2 and L3 by `addCatalogue_reusesSameNamedCustomRow`.
2. **Search typed the way people type.** "pull up" (no hyphen), "PULL-UP", "RDL" and "romanian" all find the right entries. A run-together spelling such as "pullups" is not required to match. Pinned in L1 by `search_normalisesHyphensSpacesAndCase` and `search_findsAliases`.
3. **Tapping ⓘ inside the picker must not pick the exercise** or add it to the plan or session. Pinned in L2 and L3 by `pickerInfo_opensGuideWithoutPicking`.
4. **Large text** (Android font scale 1.3, web 130% zoom). The guide sheet scrolls to its last line, including the fixed safety line, and clips nothing. Pinned in L2 by `guideSheet_largeFont_scrollsToSafetyLine` and in L3 by an e2e check at 130% zoom.
5. **An exercise restored from a backup**, from either app, whose name is in the catalogue. The picker shows the catalogue entry once, not twice, and adding it reuses the restored row. Pinned in L2 and L3 by `restoredBuiltIn_appearsOnce_andIsReused`.

---

### Task L1: Catalogue, search and facade, with guides for the existing exercises

**Files:**
- Create the package `domain/src/commonMain/kotlin/com/powertracker/domain/library/` with these files:
  - `LibraryModels.kt`: `LibraryExercise`, `FormGuide`, `BodyArea`, `Equipment`, `Kind` and `SourceId`, exactly as in spec §2.1. `SourceId` is a value class wrapping a `String` key.
  - `Sources.kt`: `object Sources { val all: Map<SourceId, Source> }`, where `data class Source(val title: String, val publisher: String, val url: String, val accessed: String)`.
  - `Library.kt`: `object Library` with `all()`, `find(name)`, `search(query, areas, equipment)` and `normalise(text)`.
  - `areas/MainLifts.kt` (`MAIN_LIFTS` entries): Squat, Bench, Deadlift, plus every `AccessoryMap` VARIATION.
  - The other area files, holding every `AccessoryMap` ACCESSORY plus Curl, Row and Pull-up in their own areas.
  - Each area file exposes `internal val <area>: List<LibraryExercise>`.
- Modify: `domain/src/jsMain/kotlin/com/powertracker/domain/web/GritDomain.kt` and `JsTypes.kt` to add `library()`, `findInLibrary(name)`, `searchLibrary(query, areas, equipment)` and `formGuide(name)`, plus JS types for the entries and the guide.
- Regenerate `web/src/domain/generated/` with `./gradlew :domain:syncWebDomain`.
- Create: `docs/library/sources.md`, generated by a small test-run writer or a gradle task you choose. It lists the sources per exercise.
- Test: `domain/src/commonTest/kotlin/com/powertracker/domain/library/LibraryTest.kt` and an extension to `domain/src/jsTest/.../web/GritDomainTest.kt`.

**Interfaces:**
- Produces (Kotlin):
  - `Library.all(): List<LibraryExercise>`
  - `Library.find(name: String): LibraryExercise?` matches the name or any alias after normalising.
  - `Library.search(query: String, areas: Set<BodyArea> = emptySet(), equipment: Set<Equipment> = emptySet()): List<LibraryExercise>`. An empty set means no filter. Equipment matches any of the given values.
  - `Library.normalise(text: String): String`
- Produces (JS):
  - `GritDomain.library(): JsLibraryExercise[]`
  - `findInLibrary(name: string): JsLibraryExercise | null`
  - `searchLibrary(query: string, areas: string[], equipment: string[]): JsLibraryExercise[]`
  - `formGuide(name: string): JsFormGuide | null`

  Enum values are passed by name.

- [ ] **Step 1: Research the existing exercises.** For each exercise in `SeedLibrary.exercises()`, run web searches (WebSearch, extended mode), using `allowed_domains` restricted to the allowed sources. Record each source used in `Sources.kt`, with today's date as `accessed`. Write the guide only from what those sources say. Keep a short research log per exercise in your report: the queries, the URLs, and any points dropped because sources disagreed.
- [ ] **Step 2: Write the failing tests in `LibraryTest`:**
  - `names_areUniqueIgnoringCase_andNeverCollideWithAliases`
  - `everyGuide_isComplete` (shape per Global Constraints)
  - `everyEntry_hasKnownSources`
  - `everySeedName_isInTheCatalogue`
  - `everyAccessoryMapName_isInTheCatalogue`
  - `variations_haveAParentLift`
  - `search_normalisesHyphensSpacesAndCase`: "pull up", "PULL-UP" and "pull  up" each find "Pull-up".
  - `search_findsAliases`
  - `search_ranksNamesBeforeAliasesBeforeMuscles`
  - `search_filtersByAreaAndEquipment`
  - `search_emptyQueryReturnsAllInFilters`

  Leave out the ≥ 200 size test for now. Task L11 adds it.
- [ ] **Step 3:** Run `./gradlew :domain:allTests`. Expected: FAIL, because `Library` doesn't exist yet.
- [ ] **Step 4: Implement** the models, `Library`, the existing-exercise entries and their guides, and the facade. Regenerate the web package.
- [ ] **Step 5:** Run `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest` and `cd web && npm test && npm run typecheck`. Expected: PASS.
- [ ] **Step 6: Commit:** `feat(domain): exercise library with form guides for the existing exercises`

### Task L2: Android picker, ⓘ guide sheet and add-from-catalogue

**Files:**
- Create in `app/src/main/kotlin/com/powertracker/ui/library/`:
  - `ExercisePickerScreen.kt`: a full-screen dialog or route with search, chips and a `LazyColumn`.
  - `FormGuideSheet.kt`: a `ModalBottomSheet`.
  - `InfoButton.kt`
- Create: `app/src/main/kotlin/com/powertracker/data/CatalogueResolver.kt`, which resolves a catalogue name to an exercise id inside a Room transaction: reuse, restore an archived row, or insert.
- Modify:
  - the Plan add-exercise flow in `ui/plan/*`;
  - the Today add-exercise flow, replacing `ExercisePickerDialog` (`ui/today/TodayDialogs.kt:116`, `TodayScreen.kt:215`, `TodayViewModel` `addable`);
  - ⓘ placement: the Today exercise card and row, Coach suggestion lists, Plan rows, and the Progress lift-history header.
- Test:
  - `app/src/test/.../ui/library/ExercisePickerTest.kt`
  - `FormGuideSheetTest.kt`
  - `app/src/test/.../data/CatalogueResolverTest.kt`
  - updates to `TodayScreenTest` and the Plan tests
  - screenshots for the picker and the sheet, dark and light

**Interfaces:**
- Consumes: L1's `Library.search`, `Library.find` and `LibraryExercise.guide`.
- Produces:
  - `suspend fun CatalogueResolver.resolve(name: String): Long`, which returns the exercise id. It must be called inside the caller's `withTransaction` that adds the plan item or set.
  - `@Composable fun FormGuideSheet(exerciseName: String, onDismiss: () -> Unit)`. It shows the catalogue guide, or the custom text when `Library.find` returns null.

- [ ] **Step 1: Write the failing tests:**
  - **CatalogueResolverTest:**
    - `firstAdd_insertsOneBuiltInRow`
    - `secondAdd_reusesIt`
    - `seededBuiltIn_isReused`
    - `archivedSameName_isRestored`
    - `addCatalogue_reusesSameNamedCustomRow`
    - `insertAndPlanItem_areOneTransaction`: force the plan-item insert to fail, and no exercise row remains.
  - **ExercisePickerTest:**
    - `search_filtersAsYouType`
    - `areaAndEquipmentChips_filter`
    - `emptyResult_showsMessage`
    - `pickerInfo_opensGuideWithoutPicking`
    - `restoredBuiltIn_appearsOnce_andIsReused`
    - `yourExercises_listedFirst_whenNoQuery`
    - `compoundDefaults3x5_accessory3x10`
  - **FormGuideSheetTest:**
    - `showsAllSections_andSafetyLineLast`
    - `customExercise_showsNoGuideText`
    - `guideSheet_largeFont_scrollsToSafetyLine` at font scale 1.3
  - **TodayScreenTest / Plan tests:** `infoButton_opensGuide_fromEveryPlace`
  - **360 dp checks:** no overflow, and targets ≥ 48 dp.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3: Implement.** Match the design system in `docs/design/DESIGN.md`. The picker uses the existing `ChoiceChip`. Re-render the screenshots with `-Pscreenshots.update=true`, check them, and commit the new ones.
- [ ] **Step 4:** Run `./gradlew :domain:allTests :app:testDebugUnitTest :app:assembleDebug`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(android): exercise library picker and form guide sheet`

### Task L3: Web picker, ⓘ guide sheet and add-from-catalogue

**Files:**
- Create in `web/src/screens/library/`:
  - `ExercisePicker.tsx`
  - `FormGuideSheet.tsx`
  - `InfoButton.tsx`
  - CSS
- Create: `web/src/db/catalogue.ts`, providing `resolveCatalogue(db, name)` inside a Dexie `rw` transaction.
- Modify:
  - the Plan add flow (`web/src/screens/plan/*`, `web/src/state/usePlan.ts`);
  - the Today add flow (`web/src/screens/today/TodayDialogs.tsx:91` `ExercisePickerDialog`, `TodayScreen.tsx:144`, `useToday.ts`);
  - ⓘ placement in the same places as L2.
- Test: `web/src/screens/library/*.test.tsx`, `web/src/db/catalogue.test.ts`, and `web/e2e/library.spec.ts`. Add the picker and sheet to the screenshots spec.

**Interfaces:**
- Consumes:
  - L1's `grit.searchLibrary`, `grit.findInLibrary` and `grit.formGuide`;
  - the existing db tables and the `usePlan` / `useToday` actions.
- Produces:
  - `resolveCatalogue(db: GritDb, name: string): Promise<number>`, which returns the exercise id. The caller runs it inside the same transaction as the plan-item or set write.
  - `<FormGuideSheet name={string} onClose={() => void} />`

- [ ] **Step 1: Write the failing tests.** Use the same test names and assertions as L2 Step 1, in TypeScript. In `library.spec.ts`:
  - picker open, type and filter each settle within 100 ms with the full catalogue;
  - the sheet scrolls to the safety line at 130% zoom;
  - no overflow at 375 px;
  - targets ≥ 48 px.
- [ ] **Step 2:** Run `cd web && npx vitest run src/screens/library src/db`. Expected: FAIL.
- [ ] **Step 3: Implement**, reusing the W7 components: Dialog, SegmentedChoice chips, TextField and Card. Re-render the screenshots with `UPDATE_SCREENSHOTS=1`.
- [ ] **Step 4:** Run `npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`. Expected: PASS.
- [ ] **Step 5: Commit:** `feat(web): exercise library picker and form guide sheet`

### Tasks L4–L10: Content batches, one per body area

Each batch has the same shape and its own review. The batches and their rough sizes:

| Task | Area file | Scope (about) |
|---|---|---|
| L4 | `areas/Chest.kt` | 30: barbell, dumbbell, incline/decline, machine, cable, fly, dip, push-up variants |
| L5 | `areas/Back.kt` | 35: rows (barbell, dumbbell, cable, machine, chest-supported, T-bar), pulldowns, pull-ups/chin-ups, pullovers, shrugs, back extensions, face pulls |
| L6 | `areas/Shoulders.kt` | 28: overhead and seated presses, landmine, raises (lateral, front, rear), upright row, Arnold, cable and band work, rotator cuff |
| L7 | `areas/Arms.kt` | 32: curls (barbell, EZ, dumbbell, hammer, incline, preacher, cable, concentration), triceps (pushdowns, extensions, skull crushers, kickbacks, dips), forearms and grip |
| L8 | `areas/Quads.kt` | 30: squat variations not already listed, leg press, hack, lunges, split squats, step-ups, leg extension, sled |
| L9 | `areas/HamstringsGlutes.kt` | 30: RDL/SLDL variants, good morning, leg curls, Nordic, glute bridge/hip thrust variants, kettlebell swing, cable pull-through, abduction |
| L10 | `areas/CalvesCore.kt` | 35: calf raises (standing, seated, leg press, single-leg), planks, carries, Pallof, rollouts, hanging raises, crunch/cable crunch, anti-rotation, back extension holds |

**For each batch Task L<N>:**

**Files:** create or extend `domain/src/commonMain/kotlin/com/powertracker/domain/library/areas/<Area>.kt`. Add new sources to `Sources.kt`. Regenerate `docs/library/sources.md` and the web domain package.

**Interfaces:** consumes L1's models. Produces nothing new: entries are picked up by `Library.all()`.

- [ ] **Step 1: Research.** For each exercise, search the allowed sources (WebSearch, extended mode, with `allowed_domains`). Record the URLs in `Sources.kt`. Keep a research log in the report.
- [ ] **Step 2: Write the entries,** following the content rules in Global Constraints. Give each entry:
  - aliases for common names, such as "OHP" or "RDL";
  - an area and equipment;
  - the muscles, using the vocabulary that already exists;
  - its kind and category.
- [ ] **Step 3:** Run `./gradlew :domain:allTests :domain:syncWebDomain :domain:checkWebDomain`. Expected: PASS, with L1's integrity tests now covering the new entries.
- [ ] **Step 4: Commit:** `feat(domain): <area> exercises and form guides`

**Task review for content batches:** the reviewer re-checks every guide's claims against its listed sources, using WebSearch restricted to the allowed domains. It flags:
- any claim not supported by a source;
- any unsafe instruction;
- missing safety notes (for example rack pins);
- a disallowed source;
- jargon.

### Task L11: Final catalogue check and the owner's reading page

**Files:**
- Modify: `LibraryTest.kt` to add `catalogue_hasAtLeast200Entries`, plus a test that no `areas/*.kt` file is empty.
- Create: `docs/library/guides.html`, a single readable page of every guide grouped by area, with a search box. It is generated from `Library.all()` by the same writer as `sources.md`. The controller publishes it as a private Artifact for the owner's read-through.
- Modify: `README.md`. Add one line under "Grit on iPhone" pointing to the ⓘ guides.

- [ ] **Step 1:** Add the tests. Run `./gradlew :domain:allTests`. Expected: PASS.
- [ ] **Step 2:** Generate `guides.html` and `sources.md`. Run the full verification:
  - `./gradlew :domain:allTests :domain:checkWebDomain :app:testDebugUnitTest :app:assembleDebug`
  - `cd web && npm test && npm run typecheck && npm run build && npm run e2e -- --trace off`
- [ ] **Step 3: Commit:** `docs(library): owner reading page and source list`
