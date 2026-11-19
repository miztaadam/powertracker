# The Grit guide: design

**Owner's request (2026-11-19):** "add a guide for new users taking them through the process of the app and explaining the coach etc. Keep a button for it in settings so even current users can experience it. Animate it to show how things will look as they progress."

**The owner's choices:**
- **What a new user should come away with:** all three of these, "ideally":
  - confident they can set up a plan and log their first workout;
  - trusting the coach, because they understand why it suggests what it does;
  - seeing the long-term payoff.
- **A. One tour, start to finish:** about 8 screens, roughly 2 minutes.
- **C. Animated pictures, then the real app.**
  - Each screen shows a drawn version of the app playing out.
  - The last screen opens the real Plan screen.
  - The "long-term user" examples use made-up sample data ("it can use fake data for the examples of a long term user").
- **The 8 screens below** were approved ("Sounds good").

## Why
- Grit does a lot that isn't obvious from its screens: two sides, templates, a coach with sourced rules, skips and pain holds, check-ins, backups.
- A new lifter should be able to start training at once and understand why the coach suggests what it does.
- A current lifter should be able to replay the guide from Settings.

## Goals
1. **A full-screen guide of 8 screens.**
   - It opens on first launch.
   - A **Guide** button in Settings replays it any time.
2. **Each screen** has a short title, one or two plain sentences, and an animated picture of the app with a sample lifter.
3. **The coach's explanations** say exactly what the coach does, taken from its rules and its sources in `CoachSources.kt`, never invented.
4. **Both apps** show the same guide, from one definition in the shared domain.

## Non-goals
- **Tips or pointers over the real screens** (option B, not chosen).
- **A help centre, search, or per-screen help.** The exercise guides (ⓘ) stay as they are.
- **Changing any screen of the real app,** except the Settings button and the first-launch hand-off.
- **Video, Lottie or images.** Everything is drawn in code, as for the mode-switch animations.

## Anti-goals (a failure even if it works)
- **Touching the lifter's data.** The sample lifter lives only inside the guide's drawings. Nothing is written to the database, the backup or the coach. The one exception is screen 2's side choice on a new install, which sets the real training side. Replaying the guide changes nothing.
- **Claims the coach doesn't make:** every number or rule in the text matches the code. The only exception is the sample lifter's progress, which is clearly an example ("for example").
- **A trap:**
  - Skip is on every screen;
  - Back, Next and swiping work;
  - the system Back button leaves the guide;
  - the first launch never shows the guide twice.
- **Slowing app start:** nothing in the guide is loaded or worked out until the guide opens.

## The 8 screens (owner-approved)
1. **Welcome.** What Grit is: two sides, one log, and a coach that adjusts from what you actually lift.
2. **Pick your side.**
   - Powerlifting or bodybuilding, animated with a small version of the hammer and the pump from 1.15.
   - **On a new install it really picks the side** (owner, 2026-11-19, option A): two buttons, **Powerlifting** and **Bodybuilding**, set the real training side, the same setting as the Settings switch, with no animation. On a replay it just shows the side the lifter is on.
   - **Screens 5 to 8 then describe that side:** its coach rules, its kind of progress and its templates. For example, swap suggestions and check-ins appear only on bodybuilding, and stall and weak-point advice only on powerlifting.
   - You can switch any time in Settings.
3. **Your plan.** Templates by days a week, the weekday picker, and Swap, shown filling in a sample week.
4. **Today.** Logging a set: the weight, the reps, the effort (RPE) and the tick, shown in motion. Then **Skip today**, and why telling the app matters.
5. **The coach: targets.**
   - How it sets the next weight from what you did, shown with a sample lift going up session by session.
   - On bodybuilding: reps first, then weight.
6. **The coach: when it holds back.** One sentence each, from the sourced rules:
   - deloads;
   - easing back in after time off;
   - the hold after a pain skip;
   - swap suggestions.
7. **Months later.** The sample lifter, fast-forwarded:
   - the Progress chart fills in;
   - the bests climb;
   - a check-in points at a lagging muscle.
8. **Your turn.**
   - **On a new install** (the side has no plan): **Choose a template** opens the real Plan screen with the template list.
   - **With a plan already:** **Done** and **See my plan**. Nothing of theirs is replaced *(my call)*.

## What the lifter sees
- **Controls** *(my calls)*:
  - Skip (top right) on every screen;
  - Back and Next (Next becomes the screen-8 buttons);
  - a row of 8 dots showing the current screen;
  - a swipe moves between screens.
- **Reduce motion:** each screen shows its end state with no animation *(my call)*.
- **Large text and narrow screens:** text wraps and is never cut. The picture shrinks to make room *(my call)*.
- **Colours:**
  - the guide uses the app's tokens for the side the lifter is on;
  - screen 2 shows both sides.
- **The sample lifter:** a made-up name-free lifter ("A sample lifter") with made-up numbers, labelled "Example" on each picture that shows numbers *(my call)*.

## First launch and the Settings button
- **First launch:** the guide opens when the app starts with **no sessions and no plan items on either side**, and the guide has never been finished or skipped on this device *(my call: that is what "new user" means here; existing users who update don't get it unasked, and use the button)*.
- **Where "seen" is kept:** on the device, like the dismissed install card (web `meta`; Android settings). It isn't in backups *(my call: restoring onto a new phone doesn't replay it)*.
- **The Settings button:** a **Guide** row in Settings, "See how Grit works". It's always there.

## Approaches
1. **Chosen: the guide defined once in the shared domain** (screens, text, sample data, animation timelines), with each app drawing it: Compose on Android, React with SVG on the web, as for 1.15's motion model.
2. **Each app with its own guide:** twice the work, and the apps could drift. Rejected.
3. **A web page shown inside the Android app:** it breaks the "same look on each app" rule, and needs a web view. Rejected.

## Constraints
- **Coach text:** every rule and number comes from the coach's code and `CoachSources.kt`, with the source noted beside the text in the domain definition. A test fails if a quoted number (for example 2%, 28 days, 10 sets) differs from the code's constant.
- **Speed:**
  - the guide's first screen shows within 300 ms of opening;
  - its animations keep their frames, as the 1.15 speed checks measure;
  - app start is unchanged.
- **Version:** 1.16 on both apps (versionCode 26), after 1.15.
- **Fixed rules:**
  - no model names in code or commits;
  - new commits only;
  - e2e with `--trace off`.

## Settled after the builder check (my calls)
1. **iPhone:** the Home Screen app has its own storage and starts empty, so a lifter who first opens Grit in Safari and then installs it sees the guide once in each. That's accepted: each copy really is a new start. In a browser tab, the guide comes first and the install card after it.
2. **Coach text:** the text sticks to *what* the coach does, in plain words. It says *why* only where the code holds a sourced reason, and it names no sources (`CoachSources.kt` says citations are never shown in the app). Numbers that are the product's own (owner-approved), such as 5%, 14 days, 28 days and 21 days, may be stated as what the coach does.
3. **Screen 2's picture** is a short, looping, cut-down version of the hammer and the pump, silent and with no vibration. Only the Settings switch plays the full ones.
4. **Screen 8 on a new install** opens that side's template list (the `templates` route). On bodybuilding it also offers **Build my plan**, below it, as the empty plan does.
5. **Order:** the guide is planned and built after 1.15, on top of its motion model.
6. **Where "seen" is kept:**
   - **Android:** its own DataStore key outside `AppSettings`, because a restore replaces the whole `AppSettings`;
   - **web:** a `meta` key, `guide:seen`, which a restore already keeps.

## Left to the builder
- **The shape of the guide's definition** in the domain, and how each picture is drawn: reusing the real screens' components with sample state where that's clean, or drawn mock-ups.
- **The exact wording,** within the screen outlines above. Plain words, and each screen's text under about 40 words.
- **The sample lifter's numbers,** which must stay realistic and inside the coach's own rules, worked out with the real coach code so the pictures match what the coach would do.
- **Tests on both apps:**
  - first launch shows the guide once, and never with data;
  - the Settings button;
  - Skip, Back, Next and swipe;
  - screen 8 on a new install and with a plan;
  - nothing is written to the data, compared before and after a replay;
  - reduce motion;
  - large text;
  - the coach numbers matching the code.
- **Screenshots:** each screen at its end state, dark and light, on both apps.
