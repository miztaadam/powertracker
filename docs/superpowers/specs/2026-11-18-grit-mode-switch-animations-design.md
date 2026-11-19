# Mode switch animations: design

**Owner's request (2026-11-18):** "an animation should play when switching from powerlifting mode to bodybuilding mode and vice versa". For powerlifting: Mjölnir smashing down.

**The owner's choices:**
- **Drawn and animated in code** (option 1), not a video clip or a Lottie file.
- **Into powerlifting: Fable's "hammer drop",** tuned by the owner: smaller, hitting harder, and landing on the very bottom of the app. "Everything else is great."
- **Into bodybuilding: "the pump"** (option B), Fable's design.
- **The reference** is the prototype `mode-switch-fable.html`, published at https://claude.ai/artifact/U4Jz1Ya7MA9YkJzPFUKYes. A copy is in the repo at `docs/design/motion/mode-switch.html`. Its timeline numbers are the spec for timing, sizes and colours.

## Why
- Switching sides is the biggest change the app makes: a new look, a new plan and a new coach. Today the colours just change.
- The owner wants each switch to feel like each side's character:
  - **powerlifting** hits, with a small hammer and a huge impact;
  - **bodybuilding** swells, in a three-beat pump.

## Goals
1. **Turning Bodybuilding mode off** (in Settings, or wherever the app switches to powerlifting) plays the hammer drop. The powerlifting look bursts out from where the hammer lands.
2. **Turning it on** plays the pump. The bodybuilding blue floods out from the switch on the third beat.
3. **Both apps look and feel the same,** matching the prototype's timing, shapes and colours.
4. **Vibration,** with each app at the most it can do:
   - **Android app:** the impact uses the motor's full strength, through amplitude control where the phone has it, with aftershocks; the pump gives three building heartbeats.
   - **Web:** the prototype's patterns, which set duration only.

## Non-goals
- **Animations anywhere else** in the app.
- **A setting to turn the animations off,** beyond the phone's reduce-motion setting.
- **Changing what the switch does.** The stored setting, the welcome-back card, and which plan and coach apply stay exactly as they are.

## Anti-goals (a failure even if it works)
- **Copying Marvel's hammer.** The hammer is the prototype's original Viking-pendant shape, with the ᚦ rune.
- **Slowing the switch down.** The setting is stored at the tap, as now. The animation never blocks the app, and a tap anywhere skips to the end.
- **Body imagery that reads as crude.** The pump's veins are light, like lightning or roots.
- **Breaking accessibility:**
  - **Reduce motion:** with the phone's reduce-motion setting on, there's no animation, only the colour change, with the vibration and the sound;
  - **Screen reader:** a screen reader hears the switch's new state, as today;
  - **Nothing flashes more than 3 times a second.**

## Approaches (decided)
1. **Chosen: drawn in code on each app,** from the prototype's shapes and timeline. On Android this means Compose Canvas and graphics layers; on the web, inline SVG driven by one clock, as in the prototype.
2. **A generated video:** rejected. It's paid, against the owner's "no paid image service", see-through video is unreliable on iPhone, and it's large.
3. **Lottie:** rejected. It needs an animation tool, and nothing in this project uses one yet.

## What the lifter sees
- **Where it plays:** over the whole app, wherever the switch was flipped. Today that is the Settings switch "Bodybuilding mode".
  - **The hammer** lands on the bottom edge of the screen, centred, over the tab bar. *(My call: "the very bottom of the app".)*
  - **The pump's veins and flood** start from the switch itself. If the switch can't be located, they start from the bottom centre.
- **The hammer drop** (about 1.1 s):
  - a shadow gathers;
  - the hammer swings in from the upper right and lands in a 135 ms fall;
  - an 80 ms squashed hit-stop;
  - a white flash, a starburst, two shock rings, cracks that heal, chalk dust and sparks;
  - a hard decaying shake: the content hops as the shock reaches it, and the POWERLIFTING label stamps in;
  - the hammer lifts out of the screen.
- **The pump** (about 1.3 s):
  - three beats at 90, 400 and 750 ms, each bigger than the last;
  - the content swells, nearest the switch first;
  - branching veins of light run further on each beat;
  - the blue floods out in a circle on the third beat, and the BODYBUILDING label swells in.
- **On every screen:** the animation plays over whatever screen holds the switch. The new side's colours apply at the impact or the third beat.
- **A tap anywhere** during an animation skips to the end state. The tap does nothing else.
- **Flipping the switch again during an animation** skips the running one and plays the new one.
- **Vibration:**
  - **always on** (owner, 2026-11-18: "ignore vibration setting and force it"): the "Vibrate on taps" switch doesn't affect it. With reduce motion on there's no animation, but the vibration still plays, since it isn't motion *(my call)*;
  - on iPhone the web app can't vibrate, so it shows the animation only.

## Sound (owner, 2026-11-18: "get Fable to design sounds for both animations")
- **Each animation has its own sound,** timed to the clock: the hammer's swing, impact and aftershock; the pump's three beats and the flood.
- **Made in code:** synthesised with the Web Audio API, with no recordings and no paid service. The apps ship the same sounds, rendered from that synthesis to small audio files, so both apps sound identical.
- **Volume:** sounds follow the phone's media volume and silent mode. There's no in-app setting *(my call)*.
- **Designed by Fable** in the prototype, `docs/design/motion/mode-switch-sound.html`. Approved by the owner on 2026-11-19: "Animations and sounds are perfect".

## Constraints
- **Timeline:** match the prototype's numbers. Each app may round them to its frame clock.
- **Speed:**
  - the first frame of the animation shows within one frame of the tap;
  - no frame drops below the device's refresh rate on the build machine's emulator or browser, measured the way the existing speed checks measure;
  - nothing is worked out at app start.
- **Size:** the shapes are vector, a few KB, with no images.
- **Fixed rules:**
  - version 1.15 on both apps, Android versionCode 25;
  - no model names in code or commits;
  - new commits only, never amend;
  - e2e with `--trace off`.

## Settled after the builder check (my calls)
1. **Light scheme:**
   - the screens use the app's light tokens;
   - the effects keep the prototype's colours, except that in light the white flash becomes a dark punch (`#2A2E33` at the prototype's flash opacity, halved), and the cracks, dust and the pump's veins get a darker edge so they stay visible;
   - the owner sees the light screenshots before the merge.
2. **The switch at the top of Settings** (it is the first row there, and Settings scrolls):
   - the pump's vein fan aims from the switch towards the screen's centre, and its lengths are scaled to reach the screen's edges;
   - the shapes, the branching and the timing stay the prototype's;
   - the flood circle grows from the switch until it covers the screen.
3. **"POWERLIFTING stamps in":** the powerlifting side has no label (`ModeLabel` draws only on bodybuilding). A POWERLIFTING stamp, in the label's style, appears in the label's place at impact and fades out by the end of the animation. No permanent label is added.
4. **What starts an animation:** only the lifter flipping the switch in this app. A backup restore, another browser tab, or anything else that changes the stored side changes the look with no animation, as today.
5. **Colours:** the app's tokens win for the screens; the prototype's numbers are for the effects only.
6. **The hammer lands on the bottom edge of the screen.** In Settings there is no tab bar (Android `AppShell.kt:112`), so the hammer lands on the bare bottom edge.
7. **The content changes too, not just the colours.** For example, Settings shows different rows on each side. On the web, the reveal shows the real new screen: use the existing `web/src/ui/viewTransition.ts` snapshot helper where it fits. Android reveals the new screen the same way, inside a clip.
8. **Tests:** the web e2e turns motion off on every page (`e2e/motion.spec.ts`), so the animation tests turn it on explicitly.
9. **The reference** is committed at `docs/design/motion/mode-switch.html`.
10. **The version** is 1.15 (versionCode 25). It ships after 1.14 (plan templates).

## Left to the builder
- How each app layers the animation over the app (an overlay above the navigation host on Android, a fixed layer on the web), and how the theme change is timed to the impact or the third beat while the setting is stored at the tap.
- How the prototype's SVG paths become Compose `Path`s. Share the numbers through a small table in each app, or in the shared domain if that's simpler.
- The Android vibration code: `VibrationEffect.createWaveform` with amplitudes where `hasAmplitudeControl()`, and the plain pattern otherwise.
- **Tests on both apps:**
  - switching plays the right animation;
  - the setting is stored at the tap;
  - the colours change at the impact or the third beat;
  - a tap skips;
  - flipping again mid-animation;
  - reduce motion gives no animation but keeps the vibration and the sound;
  - "Vibrate on taps" off still vibrates;
  - the screen reader announces the switch state;
  - no flashing above 3 a second.
- **Speed checks:** a test that each animation keeps its frames on time. On Android, the main-thread check style of `ThumbnailsTest`.
- **Screenshots:** held frames at impact and at the third beat, dark and light, on both apps.
