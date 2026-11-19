# Mode switch animations (1.15): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** turning Bodybuilding mode off plays the hammer drop, and turning it on plays the pump. Each comes with its own vibration and sound, and both apps look, feel and sound the same as the approved prototype.

**Architecture:**
- **One shared motion model** in the Kotlin Multiplatform domain: the timeline numbers, the shape path data, pure functions of time (poses, swell, reveal radius, flash, shake), and the vibration patterns. Android reads it directly, and the web reads it through the regenerated package.
- **Each app draws one overlay** above everything from that model: Compose Canvas on Android, inline SVG on the web. The overlay never takes input except a tap to skip.
- **The sounds** are rendered once from the prototype's Web Audio synthesis into audio files that both apps ship.

**Tech Stack:** Kotlin Multiplatform domain (`web/src/domain/generated`, checked by `:domain:checkWebDomain`), Android Compose, React 18.

**Spec:** `docs/superpowers/specs/2026-11-18-grit-mode-switch-animations-design.md`, approved by the owner on 2026-11-19 ("Animations and sounds are perfect"), including its "Settled after the builder check" section. The reference is `docs/design/motion/mode-switch-sound.html`, the approved prototype with sound; `mode-switch.html` is the same without sound. Its numbers are the spec for timing, sizes and colours.

## Global Constraints
- **Match the reference:** every constant comes from `mode-switch-sound.html`.
  - **The drop:** `HIT` is at the bottom edge, centre, with `S = 0.42`; `T_FALL` 115, `T_IMPACT` 250, `T_HOLD` 330, `T_SETTLE` 520, `T_LIFT` 640, `T_END` 1120; `VIBE` [420, 35, 180, 45, 110, 60, 60, 80, 30].
  - **The pump:** `P_BEAT` [90, 400, 750], `P_AMP` [.36, .62, 1], `P_RISE`, `P_FALL`, `P_REACH`, `P_SURGE`, `P_FLOOD` 380, `P_END` 1330; `P_VIBE` [28, 55, 120].
  - **Shapes:** the hammer path data, the crack, dust, spark and vein generation with the same seeds, and the colours.
  - Each app may round times to its frame clock.
- **The setting is stored at the tap,** as today. The new side's colours and content appear at the impact (drop) or at the third beat (pump).
- **What plays it:** only the lifter flipping the Settings "Bodybuilding mode" switch in this app. A restore, another tab or a welcome-back card never animates.
- **Skipping:**
  - a tap anywhere skips to the end; the tap does nothing else;
  - flipping again mid-animation skips the running one and plays the new one;
  - skipping stops the sound with a quick fade.
- **Vibration is always on,** whatever "Vibrate on taps" says (owner).
  - **Android:** `VibrationEffect.createWaveform` with amplitudes: 255 on the drop's slam, with aftershocks stepping down, and the pump's three beats building to 255. Where `!hasAmplitudeControl()`, use the timing-only pattern.
  - **Web:** `navigator.vibrate` with `VIBE` and `P_VIBE`, wrapped in try/catch.
- **Sound** follows media volume and silent mode; there's no setting.
- **Reduce motion** (Android: animator duration scale 0, or the system's remove-animations setting; web: `prefers-reduced-motion`): no animation, only the colour change, but the vibration and the sound still play.
- **Light scheme:**
  - the screens use the app's light tokens;
  - the flash becomes a dark punch (`#2A2E33` at half the flash opacity);
  - the cracks, dust and veins get a darker edge.
- **Where the effects start:**
  - **The pump's veins** fan from the switch towards the screen's centre, scaled to reach the edges. The flood grows from the switch.
  - **The hammer** lands on the bottom edge of the screen, centred.
- **POWERLIFTING stamp:** a stamp in `ModeLabel`'s style appears where the label sits at impact, then fades out by `T_END`. No permanent label is added.
- **Accessibility:**
  - the screen reader announces the switch state as today;
  - the overlay is hidden from accessibility;
  - nothing flashes more than 3 times a second.
- **Speed:**
  - the first animation frame shows within one frame of the tap;
  - frames keep time (the speed tests in Tasks 2 and 3);
  - no work happens at app start.
  - The existing picker, Exercises tab and Today speed checks still pass.
- **Version:** 1.15 on both apps, Android versionCode 25.
- **Fixed rules:**
  - no model names in code or commit bodies;
  - new commits only, never amend;
  - stage files by path;
  - never commit `.claude/`, `CLAUDE.md` or `.superpowers/`;
  - e2e with `--trace off`, and never `playwright install`;
  - existing tests pass unedited, or each change is listed with its reason.
- **Commit trailer:**
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
  `Claude-Session: https://claude.ai/code/session_01KboHw6FdBYrNDSMx1QWyzy`

## Review Focus
1. **Leaving Settings, or the app going to the background, mid-animation:** the animation ends cleanly at its end state. The sound stops, the vibration is cancelled, and nothing stays drawn over the app.
2. **Rapid switching** (three flips in a second): the setting matches the last flip, the colours match it, and only one animation and one sound are running.
3. **Large text and narrow screens** (320 at 130%): the switch's position is measured correctly, so the veins and flood start at the switch even when Settings is scrolled.
4. **No vibrator, no amplitude control, or sound blocked** (a web page that hasn't been tapped yet, or a browser without Web Audio): the animation still plays, with nothing thrown.
5. **Light mode:** every effect stays visible. The owner sees the light shots before the merge.

---

### Task 1: The shared motion model and the sound files
**Files:**
- Create `domain/src/commonMain/kotlin/com/powertracker/domain/motion/ModeSwitchMotion.kt`.
- Export it from `domain/src/jsMain/.../web/GritDomain.kt`.
- Test: `domain/src/commonTest/.../motion/ModeSwitchMotionTest.kt`.
- Create the sound render script `tools/render-mode-switch-sounds.js`, which runs the reference's synthesis in headless Chromium with `OfflineAudioContext` and writes the files.
- Add the sounds: `app/src/main/res/raw/grit_hammer_drop.ogg` and `grit_pump.ogg`, and `web/public/sounds/grit_hammer_drop.ogg` and `grit_pump.ogg`. Use the same bytes for both apps. Use OGG Vorbis if `ffmpeg` is available on the build machine, otherwise WAV; record which in the report.

**Interfaces (produces):**
```kotlin
object ModeSwitchMotion {
    // timeline (ms) and constants, exactly the reference's
    val DROP: DropTimeline; val PUMP: PumpTimeline
    val DROP_VIBE: LongArray; val PUMP_VIBE: LongArray          // ms, timing-only patterns
    val DROP_AMPLITUDES: IntArray; val PUMP_AMPLITUDES: IntArray // 0..255, the same length as their patterns
    val HAMMER_PATHS: List<MotionPath>                          // SVG path data, fill/stroke colour tokens, opacity
    fun dropFrame(tMs: Double, w: Double, h: Double): DropFrame // hammer pose, ghosts, flash, burst, rings, crack and dust/spark progress, shake, reveal radius, stamp scale
    fun pumpFrame(tMs: Double, w: Double, h: Double, src: Point): PumpFrame // swell, glow, halos, rings, vein front, flood radius, pill scale
    fun cracks(w: Double, h: Double, hit: Point): List<MotionPath>          // seeded as in the reference
    fun veins(w: Double, h: Double, src: Point): List<VeinPath>              // seeded; fan aimed from src to the centre, scaled to the edges
    fun dust(hit: Point): List<Particle>; fun sparks(hit: Point): List<Particle>
}
```
The frame types are plain data classes with only numbers in them. The JS exports mirror the same functions, returning plain objects, in the style of the existing `planBuilder*` exports.

- [ ] **Write failing tests against the reference's numbers:**
  - `dropFrame` at 0, 249, 250, 300, 520, 640 and 1120 gives the reference's pose and flash values (within 0.01);
  - `pumpFrame` at each beat onset and at `P_BEAT[2] + P_FLOOD`;
  - the reveal radius covers the screen's far corner by the end;
  - the vein fan points from a top-left switch towards the centre;
  - each pattern and its amplitudes have the same length;
  - the flash count is at most 3 per second over the whole timeline;
  - the seeded shapes are the same on every call.
- [ ] Run `./gradlew :domain:allTests` and see them fail.
- [ ] Implement it by porting the reference's `render`, `renderPump`, `fallPose`, `hammerPose`, `beatEnv`, `veinFront`, `rng` and the shape builders. Regenerate the web package.
- [ ] Render the sounds with the script. Check that each file's length (1.9 s and 2.0 s) and peak (≤ −1 dBFS) match the reference's measurement.
- [ ] Run `./gradlew :domain:allTests :domain:checkWebDomain` and see them pass.
- [ ] Commit `feat(domain): the mode switch motion model, and its sounds`.

### Task 2: Android
**Files:**
- Create `app/src/main/kotlin/com/powertracker/ui/motion/ModeSwitchOverlay.kt`, `ModeSwitchController.kt` and `ModeSwitchFeedback.kt` (vibration and sound).
- Modify `ui/AppShell.kt` so the overlay sits above the navigation host, `ui/plan/SettingsScreen.kt` so the switch reports its position and starts the controller, and the theme host so the side's colours apply at the controller's moment.
- Tests: `app/src/test/.../ui/motion/ModeSwitchOverlayTest.kt` and `ModeSwitchFeedbackTest.kt`.

**Interfaces:** consumes Task 1's `ModeSwitchMotion`. `ModeSwitchController` has `start(kind, switchCenter)`, `skip()` and `state`, plus `shownMode`, the side the theme shows, which lags the stored mode until the impact or the third beat.

- **Drawing:** Compose `Canvas` and `graphicsLayer`, driven by one `withFrameMillis` clock.
  - The hammer, cracks and veins come from the path data through `PathParser`.
  - The reveal is a circular clip of the new side's screen over the old one. Render the old side under the new one during the reveal, following spec "Settled" 7.
- **Sound:** `SoundPool`, loaded lazily on the first switch, never at app start.
- **Vibration:** `VibrationEffect.createWaveform` with the amplitudes where supported. Cancel it on skip.
- **Tests:**
  - turning the switch off stores at the tap, and the colours change at 250 ms while they're still the old side's at 249 ms; the pump changes at the third beat;
  - a tap skips;
  - flipping again mid-animation;
  - leaving Settings mid-animation;
  - reduce motion: no overlay drawn, but the vibration and the sound are still requested;
  - "Vibrate on taps" off still vibrates;
  - no amplitude control falls back to the plain pattern;
  - a missing vibrator throws nothing;
  - the screen reader's state text;
  - the overlay isn't in the accessibility tree;
  - nothing is worked out on the main thread at app start, in the `ThumbnailsTest` style;
  - frames keep time: no frame of the animation takes longer than 16 ms to draw under Robolectric's clock, measured the way the existing main-thread check measures.
- [ ] Write the failing tests, see them fail, implement, see them pass. Run `./gradlew :app:testDebugUnitTest --tests '*Motion*' --tests '*Settings*' --tests '*Shell*'`, then the full suite once.
- [ ] Shots: held frames at impact (`T_IMPACT + 20`) and at the third beat (`P_BEAT[2] + 120`), dark and light, at 412 dp.
- [ ] Commit `feat(app): the hammer drop and the pump`.

### Task 3: Web
**Files:**
- Create `web/src/ui/motion/ModeSwitchOverlay.tsx`, `useModeSwitch.ts` and `modeSwitchFeedback.ts`.
- Modify the Settings screen's switch, `web/src/ui/theme.ts` (the shown side lags the stored side, as on Android), and the shell (where the overlay is mounted).
- Tests: vitest `ModeSwitchOverlay.test.tsx` and `web/e2e/modeSwitch.spec.ts`. The e2e turns motion on explicitly (spec "Settled" 8).

- **Drawing:** inline SVG driven by one `requestAnimationFrame` clock, like the reference, using Task 1's exports.
  - **The reveal:** use `web/src/ui/viewTransition.ts` where it fits, or a circular `clip-path` over the new screen.
- **Sound:** fetch and decode the two files on the first switch (the tap unlocks the `AudioContext`), and play them at the clock's offset, as the reference does.
- **Vibration:** `navigator.vibrate`.
- **Tests:** the same list as Task 2, adapted to the web.
  - The e2e checks that the stored setting changes at the tap and `data-mode` changes at the impact, and that a tap skips.
  - **A speed check** (`@speed`): every animation frame is drawn within 16 ms of its rAF time over the whole drop and the whole pump, on the production build, measured with real frames rather than Playwright's fake clock.
  - The existing speed checks still pass.
- [ ] Write the failing tests, see them fail, implement, see them pass. Run `npm test`, `npx tsc -b`, `npx playwright test e2e/modeSwitch.spec.ts e2e/motion.spec.ts --trace off`, and one speed run.
- [ ] Shots: the same as Task 2, for the web.
- [ ] Commit `feat(web): the hammer drop and the pump`.

### Task 4: Version 1.15, notes, the owner's look, full run
- [ ] Set the version to 1.15 on both apps (versionCode 25) and reshoot the Settings shots.
- [ ] **`docs/next-update.md`:** add a "Done: 1.15, mode switch animations (both apps)" section covering the owner's decisions, my calls, any Android and web differences, and any tests that changed.
- [ ] **README:** add phone checks on each app: both animations, the vibration strength, the sound, reduce motion, and a tap to skip.
- [ ] **Full run:** Gradle, web `npm test`, `tsc`, the build, the full e2e and 3 speed runs, one after another.
- [ ] Commit `chore: version 1.15, notes`.
- [ ] **Stop before the merge.** Send the owner the light-mode shots (spec "Settled" 1). Merge only after they approve.
