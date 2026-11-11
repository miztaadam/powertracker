# Grit: exercise library, form guides and ⓘ

**Date:** 2026-11-11
**Status:** Approved in brainstorming. Awaiting the owner's review of this spec.
**Applies to:** Android (`app/`), web (`web/`) and the shared Kotlin domain (`domain/`). All three ship together.
**Part of:** the next update (`docs/next-update.md`). This is project 2 of 3; strict press follows it.

## 1. Purpose

The owner wants to build their exact routine. Today the Plan and Today pickers offer only the main lifts,
the coach's accessories and three generic extras. This project adds:

- a full gym exercise library (about 250 exercises);
- search and filters to find exercises in it;
- a safety-checked form guide for **every** exercise, old and new, behind an ⓘ button wherever an
  exercise appears.

**Success looks like this:**

- the owner finds any common gym exercise in a few seconds;
- any guide can be read between sets;
- no guide gives unsafe or unsourced advice;
- existing data, plans, history and backups are untouched.

**Decisions made with the owner:**

- The library is general weight training, not Olympic weightlifting.
- The coach keeps its own curated accessory list (`AccessoryMap` unchanged).
- Custom exercises get no notes field.
- The guides are checked against reputable sources and an independent review. The owner reads the
  finished text but is not asked to judge technique.

## 2. Architecture: a read-only catalogue in the shared domain

### 2.1 The catalogue

A new `domain/src/commonMain/kotlin/com/powertracker/domain/library/` package holds the catalogue. It is
static Kotlin data compiled into both apps, so it works offline and needs no download.

```kotlin
data class LibraryExercise(
    val name: String,                 // unique, case-insensitive; never renamed once shipped
    val aliases: List<String>,        // search terms and former names, e.g. "RDL"
    val area: BodyArea,               // primary grouping for browsing
    val equipment: List<Equipment>,   // what's needed; first = primary
    val muscles: List<String>,        // display names, same vocabulary as today's Exercise.muscles
    val kind: Kind,                   // COMPOUND or ACCESSORY: sets default sets × reps when added
    val category: Category,           // existing enum: SQUAT/BENCH/DEADLIFT/VARIATION/ACCESSORY
    val parentLift: MainLift?,        // for VARIATION only, as today
    val guide: FormGuide,
    val sources: List<SourceId>,      // at least one; see §4
)

data class FormGuide(
    val purpose: String,         // one or two sentences: what it trains and why
    val setup: List<String>,
    val steps: List<String>,
    val cues: List<String>,      // 2–3
    val mistakes: List<String>,  // 2–4
)
enum class BodyArea { MAIN_LIFTS, CHEST, BACK, SHOULDERS, ARMS, QUADS, HAMSTRINGS_GLUTES, CALVES, CORE }
enum class Equipment { BARBELL, DUMBBELL, KETTLEBELL, CABLE, MACHINE, BODYWEIGHT, BAND, SPECIALTY_BAR, SMITH }
enum class Kind { COMPOUND, ACCESSORY }
```

- `Library.all(): List<LibraryExercise>` lists the whole catalogue.
- `Library.find(name: String): LibraryExercise?` matches a name or an alias, ignoring case.
- `Library.search(query, areas, equipment): List<LibraryExercise>` is the pure search and filter function
  that both apps' pickers use. Matching ignores case and diacritics. Name matches rank before alias
  matches, and both rank before muscle matches. Within a rank, results are alphabetical.
- Every exercise already in `SeedLibrary` has a catalogue entry with **exactly the same name**. That
  includes the main lifts, every `AccessoryMap` exercise, and "Curl", "Row" and "Pull-up". The
  `SeedLibrary` ids and the order of its rows don't change.
- The JS facade (`GritDomain`) exposes `library()`, `findInLibrary(name)`, `searchLibrary(query, areas[],
  equipment[])` and `formGuide(name)` as plain data, the same way it exposes everything else. The
  generated web package is regenerated with `:domain:syncWebDomain`.

### 2.2 How a catalogue exercise reaches the user's data

There is no migration and no up-front insert. Approach 1 was chosen.

- The pickers browse the **catalogue**, together with the user's own custom exercises and any database
  exercise that is not in the catalogue.
- When the owner adds a catalogue exercise to the plan or logs it on Today, the app looks for a
  non-archived database exercise with the same name, ignoring case:
  - **If one exists**, the app uses it. This covers built-ins seeded earlier and anything restored.
  - **If none exists**, the app inserts a new row with `isBuiltIn = true` and the catalogue's `category`,
    `parentLift` and `muscles`, using the next auto id. This happens in the same transaction as the plan
    item or set.
- An archived exercise with that name is restored instead of being duplicated.
- Built-in rows are always resolved to the catalogue by name, so the form guide follows the name.
  Catalogue names never change once shipped. A future rename keeps the old name in `aliases`, and
  `find` matches it.
- **The database schema doesn't change and the backup format doesn't change.** Backups grow only by the
  exercises the owner actually uses. A backup from this version restores on the previous version,
  because the rows are ordinary exercise rows.

### 2.3 What stays the same

- `AccessoryMap`, the coach rules, `SeedLibrary` and ids, the Room and Dexie schemas, and the backup
  format don't change.
- Existing plan items, sets and history don't change.
- The coach's stall suggestions still name only its curated exercises. Those now also have guides.

## 3. User interface (both apps)

### 3.1 The exercise picker

The picker replaces today's short dialog on Plan ("Add exercise") and on Today ("Add exercise").

- It opens full screen, with a title and a close button.
- **Search box** at the top. It filters as the owner types, using `Library.search`.
- **Filter chips:**
  - a row for body area, with one area selected at a time or "All";
  - a row for equipment, where several can be selected and the results must match any of them.
- **Your exercises:** when no search or filter is active, a section at the top lists recently used and
  custom exercises.
- **Rows:** each row shows the name, a muscles subtitle, an equipment icon or label, and an ⓘ button.
  Tapping the row picks the exercise. Tapping ⓘ opens the guide without picking.
- **Empty result:** "No exercises match. Try fewer filters, or create your own." It comes with the
  existing custom-exercise action.
- **Default sets and reps when added to Plan:** `COMPOUND` gives 3 × 5 @ the default RPE, and
  `ACCESSORY` gives 3 × 10 @ the default RPE. The owner can edit them as now.
- **Web:** the list stays fast with the full catalogue (one virtualised list, or one plain list, whichever
  passes the performance check in §5). The search input is at least 16 px. Nothing overflows at 375 px.
- **Android:** a `LazyColumn`. Nothing overflows at 360 dp.

### 3.2 The ⓘ form guide sheet

- **Where ⓘ appears:**
  - the Today exercise card and the Today row;
  - Coach suggestion lists;
  - Plan rows;
  - the Progress lift-history header;
  - picker rows.
- **Shown as:** a bottom sheet on Android and an equivalent sheet or dialog on the web. It can be read
  one-handed. Body text is at least 16 px or 16 sp, at a comfortable line length.
- **Content order:** name; area and equipment; purpose; Setup; How to do it; Cues; Common mistakes. Every
  sheet ends with this fixed line:
  "Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist."
- **Custom exercises:** the sheet says "No form guide for your own exercises." ⓘ stays visible so the
  layout is the same.
- **Accessibility:** ⓘ is labelled "Form guide for <name>". The touch target is at least 48 px or 48 dp.

## 4. Writing the guides safely

### 4.1 Sources

- **Source registry:** a registry in the catalogue (`SourceId` → title, publisher, URL, date accessed)
  lists only reputable sources:
  - strength and conditioning bodies (for example NSCA, ACSM, ACE);
  - established exercise references (for example ExRx);
  - for the competition lifts, powerlifting federation or recognised coaching material.

  Blogs, social media and AI-generated content are not used.
- **Per exercise:** each catalogue entry lists the sources it was checked against. Sources aren't shown
  in the app. A generated `docs/library/sources.md` lists them by exercise for traceability.
- **Disagreement:** where sources disagree, the guide uses the most conservative version or leaves the
  point out.
- **Safety content:**
  - Setup and bracing come before cues.
  - Squat, bench and other barbell exercises done in a rack mention safety pins, safety arms or a
    spotter.
  - No guide tells the owner to train through pain.
  - No guide gives medical or rehab advice.

### 4.2 Process

1. **Research and draft**, one batch per body area. Each batch's guides are drafted only from that
   batch's recorded sources.
2. **Independent review.** A separate reviewer checks every guide in the batch against its sources:
   - every claim is supported;
   - nothing is unsafe;
   - the five sections are complete;
   - the reading level is plain.

   Fixes are made, then the batch is accepted.
3. **Owner read-through.** The owner gets the finished guides as a readable page and flags anything that
   reads oddly. Flags are fixed before merge. The owner isn't asked to judge technique.

## 5. Tests

- **Catalogue integrity (domain, runs on JVM and JS):**
  - names are unique, ignoring case, and don't collide with any alias;
  - every entry has non-empty purpose, setup and steps, 2–3 cues, 2–4 mistakes, at least one area and
    equipment value, and at least one known source;
  - every `SeedLibrary` name exists in the catalogue;
  - every `AccessoryMap` exercise name exists in the catalogue;
  - VARIATION entries have a `parentLift`;
  - the catalogue has at least 200 entries.
- **Search:**
  - exact name, alias (e.g. "rdl" finds Romanian deadlift) and partial name;
  - area filter and equipment filter, alone and combined;
  - ranking order;
  - an empty query returns everything in the selected filters.
- **Adding from the catalogue (both apps):**
  - adding a catalogue exercise creates one database row the first time and reuses it the second time;
  - a seeded built-in is reused, not duplicated;
  - an archived same-name exercise is restored;
  - the plan item or set and the new row are saved in one transaction.
- **Backups:** a backup made after adding catalogue exercises restores on this version, and its
  exercises table contains only ordinary rows. The existing Android and web fixtures still pass both
  ways.
- **UI (both apps):**
  - the picker search and filters work;
  - ⓘ opens the right guide from every place in §3.2;
  - a custom exercise shows the "no form guide" text;
  - targets are at least 48 px or dp;
  - no overflow at 375 px (web) or 360 dp (Android);
  - on the web, picker open, type and filter each respond within 100 ms on the e2e machine with the full
    catalogue.
- **Screenshots:** the picker and the guide sheet, in dark and light, for both apps.

## 6. Size

The guide text adds roughly 250 KB of strings to each app before compression. That is acceptable: the
web app is cached once for offline use, and the Android app ships with R8.

## 7. Delivery

- **Branch:** the existing working branch. Merge to `main` only with the owner's go-ahead.
- **Build order:**
  1. catalogue structure, search, and the existing exercises with guides;
  2. pickers and ⓘ in both apps;
  3. the remaining body-area batches (research → review → owner read-through).
- **Who builds what:** Fable builds the UI. Research and drafting use a capable model with web search. A
  separate reviewer checks each guide batch. The final whole-branch review comes last.
