# Grit: exercise pictures

**Date:** 2026-11-12
**Status:** Approved in brainstorming, section by section. Awaiting the owner's review of this spec.
**Applies to:** the shared Kotlin domain (`domain/`), Android (`app/`) and web (`web/`). All three ship together.
**Part of:** the next update (`docs/next-update.md`, "pictures on the Exercises tab").

## 1. Purpose

Every catalogue exercise (225) gets a picture of a person doing it. The picture helps the lifter recognise the exercise and see its key positions between sets.

**Decided with the owner:**
- **Style:** the code-drawn style from the bench press draft (`docs/library/pictures/bench-press-figure-draft.svg`):
  - a solid figure;
  - equipment drawn to real-world scale;
  - the bar and plates in the chalk accent;
  - dark and light versions from the theme.
- **No paid image service.** Everything is drawn by code.
- **Two still poses side by side** (start and end). No animation.
- **Where:**
  - the guide sheet;
  - a small thumbnail on Exercises tab rows.

  The Plan and Today picker doesn't change.
- **First batch:** the 10 exercises in §6. The owner sees them before the rest are made.
- **Release:** all at once, when all 225 are done. The work stays on the working branch until then.
- **Architecture:** approach A. The poses are data in the shared domain, and each app draws them.

**Success looks like this:**
- every catalogue exercise has a picture that agrees with its guide;
- both apps draw identical pictures;
- the pictures read clearly at phone size in dark and light;
- no picture shows a position that is unsafe or unsourced.

**Out of scope:**
- pictures for custom exercises;
- animation;
- thumbnails in the picker;
- any data, settings or backup change.

## 2. Architecture (shared domain)

A new package `domain/src/commonMain/kotlin/com/powertracker/domain/library/figures/`.

### 2.1 Body

- **Segments:** a fixed set of body segments: head, neck, torso, upper arm, forearm, hand, thigh, shin and foot. The arms and legs come in near and far copies.
- **Lengths:** fixed segment lengths from average adult proportions. The source of the proportions comes from the allowed sources (§4.1) and is recorded in a code comment.
- **Pose:**
  - a root position (the pelvis) and the torso angle;
  - joint angles: neck, shoulder, elbow, wrist, hip, knee and ankle, near and far.

  Joint positions are computed from the angles and the fixed lengths, so proportions never change between exercises.
- **Joint limits:** each joint has a normal human range. The ranges come from an allowed source and are recorded in the code (§4.2).

### 2.2 Equipment kit

- **Pieces:**
  - floor;
  - flat bench, incline bench and decline bench;
  - power rack with J-hooks and safety arms;
  - barbell with 45 cm plates;
  - EZ bar, trap bar and dumbbell;
  - kettlebell;
  - cable stack with handle, rope and bar attachments;
  - pull-up / dip bars;
  - the machines the catalogue uses (for example leg press, lat pulldown, leg extension, leg curl, chest press, pec deck, calf raise and hack squat).
- **Scale:** all pieces are at real-world scale in the same units as the body.
- **Attachment:** a piece can be fixed to the scene (bench, rack, machine) or held. Held pieces attach to the hands, or sit across the upper back for back-loaded bar work. A held piece's position follows from the pose. It is never placed independently.
- **Kit scope:** pieces are added only when a batch needs them.

### 2.3 Figure entries

```kotlin
enum class View { SIDE, FRONT }

data class Scene(val pose: Pose, val props: List<Prop>)

data class ExerciseFigure(
    val view: View,
    val start: Scene,
    val end: Scene,
    val startCaption: String,   // at most 24 characters, guide wording
    val endCaption: String,
    val thumbnail: Thumb,       // START or END: the more telling pose
    val guideLines: List<String>, // the guide setup/steps lines this picture shows
)
```

- **Storage:** entries are keyed by catalogue name, in one file per body area under `figures/areas/`, beside the catalogue's area files.
- **Lookup:** `Library.figure(name): ExerciseFigure?` resolves names and aliases the same way `Library.find` does.

### 2.4 Shapes

- **Output:** `FigureRenderer.shapes(figure, which: START|END): List<Shape>` turns a scene into a flat, ordered list of primitives:
  - circle;
  - capsule (a thick line with round ends);
  - rounded rectangle;
  - polygon.
- **Colour roles:** each primitive carries a role, never a colour:
  - `BODY`;
  - `BODY_FAR` (far-side limbs, a lighter shade);
  - `EQUIPMENT`;
  - `ACCENT` (bar and plates outline);
  - `ACCENT_FILL` (plate face, low opacity);
  - `GAP` (an outline in the surface colour, so overlapping limbs stay readable).
- **Draw order:** behind to front (far limbs, equipment behind the body, body, near limbs, held equipment). The renderer owns the order.
- **Frame:** each figure has a fixed view box, the same aspect ratio for every exercise, so the layout never shifts.
- **Caching:** results are computed once per exercise and pose, then cached.
- **JS facade:** `GritDomain` exposes `figureShapes(name, which)` and `figureInfo(name)` (captions, thumbnail choice, view box) as plain data. The generated web package is regenerated with `:domain:syncWebDomain`.

## 3. User interface (both apps)

### 3.1 Guide sheet

- **Placement:** the picture sits under the name and the area and equipment line, above the purpose text.
- **Layout:** two panels side by side across the sheet's width, each with its caption underneath.
- **Fit:** no overflow at 360 dp (Android) or 375 px (web). The captions are never cut off with an ellipsis.
- **Screen readers:** the picture is one element, labelled "Picture: <start caption>, then <end caption>."
- **Missing picture:** exercises without a figure show no picture area. This only happens during development or for custom exercises. Custom exercises keep their existing "No form guide for your own exercises." text.

### 3.2 Exercises tab thumbnail

- **Size and position:** a thumbnail of the entry's chosen pose, about 56 × 40, on the left of each row's name.
- **Custom exercises:** they show an empty muted tile of the same size, so names stay aligned.
- **Rows:** the touch target is still at least 48. The thumbnail is decorative (hidden from screen readers), because the row already names the exercise.
- **Picker:** the Plan and Today picker doesn't change.

### 3.3 Colours

- **Mapping:** roles map to existing tokens:
  - `BODY` → main text colour;
  - `BODY_FAR` → main text colour mixed toward the surface;
  - `EQUIPMENT` → muted;
  - `ACCENT` → chalk accent;
  - `ACCENT_FILL` → chalk accent at low opacity;
  - `GAP` → the surface the picture sits on.
- **Contrast:** body, equipment and accent each reach at least 3:1 against the sheet surface in dark and light, checked by tests.
- **Drawing:**
  - **Android** draws the shapes on a Compose `Canvas`.
  - **The web** draws them as inline SVG elements with CSS variables.

### 3.4 Speed

- **Rendering cost:** shapes are cached per exercise. Thumbnails are drawn only for visible rows: `LazyColumn` on Android, and the existing list on the web.
- **Web performance check:** the existing check (open, type and filter, each within 100 ms) is extended to the Exercises tab with thumbnails.

## 4. Keeping pictures correct

### 4.1 Sources

- **Allowed sources:** the same as the guides: acefitness.org, exrx.net, nsca.com, acsm.org, pmc.ncbi.nlm.nih.gov and powerlifting.sport. No blogs or forums.
- **Default source:** each picture is drawn from its exercise's guide. `guideLines` records which setup and steps lines it shows, and the captions reuse the guide's wording.
- **Positions the guide doesn't describe:** a position the picture needs but the guide doesn't give (for example a joint angle at the bottom of a lift) is checked against an allowed source, or kept neutral. Where sources disagree, the picture uses the most conservative position.
- **Owner directions:** the owner's content directions apply. Pins or spotter appear in the picture for heavy barbell rack work and weights moved over the face, matching the guide's safety line.

### 4.2 Automatic checks (domain, run on JVM and JS)

- **Joint limits:** every joint in every pose is within its normal range.
- **Contact points:** each entry declares its contacts (for example feet on the floor, head, upper back and hips on the bench, back on the pad). A check confirms each contact point is within a small tolerance of its surface.
- **Held equipment:** held equipment's grip sits in the hands, or the bar sits across the upper back for back-loaded bar work.
- **Key positions:** where a guide names a key position, a check confirms it. Examples:
  - bench press: the bar touches the chest at the bottom;
  - strict press: lockout over the shoulders;
  - squat: hip depth as the guide gives it;
  - deadlift: the bar over the mid-foot.

  The checks for each exercise are listed with its entry.
- **No intersection:** no body segment passes through the floor, a bench or a machine.
- **Fit:** every shape is inside the view box.
- **Integrity:**
  - names match the catalogue;
  - captions are at most 24 characters and not blank;
  - `guideLines` are real lines of that exercise's guide;
  - every kit piece a figure uses exists.
- **Completeness:** every catalogue exercise has a figure. This check is switched on in the final task.
- **Parity:** a fingerprint of every figure's shape list matches between the JVM and JS builds.

### 4.3 Review

- **Contact sheet:** every batch is rendered to a contact sheet of images, dark and light.
- **Independent review:** a separate reviewer checks each picture against its guide text and sources:
  - the positions agree;
  - nothing unsafe is shown;
  - the captions are right;
  - it reads clearly at thumbnail size.

  Fixes are made before the batch is accepted.
- **Owner checks:**
  - **The first batch (§6)** goes to the owner for the style before any further batch.
  - **At the end**, the owner gets a page with all 225 pictures (`docs/library/pictures.html`) and flags anything that looks odd. The owner isn't asked to judge technique. Flags are fixed before merge.

## 5. Tests (both apps, same names)

- `guideSheet_showsTwoPosePicture_withCaptions`
- `guideSheet_pictureLabel_readsBothCaptions`
- `guideSheet_noFigure_showsNoPictureArea`
- `exercisesTab_rowsShowThumbnail`
- `exercisesTab_customExercise_showsEmptyTile`
- `picker_hasNoThumbnails`
- `picture_noOverflow_atPhoneWidth`
- `picture_contrast_darkAndLight`

**Screenshots:**
- the guide sheet with a picture, for the bench press, squat and lat pulldown;
- the Exercises tab with thumbnails.

Both in dark and light, for both apps.

## 6. Delivery

- **Branch:** the existing working branch. The pictures merge to `main` once, when all 225 are done, with the owner's go-ahead.
- **Tasks, in order:**
  1. **Kit (domain):** body, joint limits, kit pieces for the first batch, renderer, shapes, the automatic checks, and the JS facade.
  2. **Android:** the canvas drawing, the guide sheet picture and the tab thumbnails.
  3. **Web:** the SVG drawing, the guide sheet picture and the tab thumbnails.
  4. **First batch:** Squat, Bench, Deadlift, Strict press, DB shoulder press, Lat pulldown, Leg press, Pull-up, One-arm DB row and Romanian deadlift. Review, then **stop**: send the owner the pictures, with a phone notification, and wait for their go-ahead on the style.
  5. **Remaining batches, one per body area:** chest, back, shoulders, arms, quads, hamstrings and glutes, calves, core. Each batch adds its kit pieces, figures and checks, and gets an independent review.
  6. **Finish:**
     - switch on the completeness check;
     - the owner's pictures page;
     - the screenshots;
     - a final whole-branch review;
     - then the merge decision, with a phone notification.
- **Data:** no schema, setting or backup change.
- **Size:** about 150–300 KB of code per app before compression.
- **Testing practice (owner preference):**
  - while building, only the tests for the part being changed;
  - the full Android, domain, web and e2e suites once per task, before its commit;
  - once more on main before pushing.
- **Who builds:**
  - Opus builds the kit and the figures;
  - Fable builds the app screens;
  - Sonnet reviews each task;
  - Opus does the final review.
