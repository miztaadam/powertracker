# Grit: the new look and motion

Owner's brief (2026-11-16): "elevate the current UI to a premium, AAA-quality experience" on both apps, feeling "heavy, tactile and fluid", with five motion ideas: press feedback, layout fluidity, seamless expansion, haptics, and data that animates in. Examples, approved for direction: https://claude.ai/artifact/Eapqz591Q2LX8Y75g2uwHK (four interactive pages; the numbers on them are made up).

## 1. Decisions made with the owner (2026-11-16)
- **Keep every feature.** This is a restyle and motion pass over the screens Grit has; nothing is removed, and every existing test still passes (only screenshots and tests that pin the old look change).
- **Colours.**
  - The powerlifting side keeps its dark (and light) palette, but its accent changes from chalk to **dark grey**: `#93979D` with ink `#0B0C0E` in dark (owner picked `#868A91` on 2026-11-16, then chose this hair-lighter grey, option A, because `#868A91` fails the contrast tests as accent text on raised panels). In light it is `#4A4D52` with ink `#F6F7F8` (7.9:1). These are the designer's light values, shown in the examples.
  - The bodybuilding side keeps its blue, unchanged.
  - This changes the powerlifting side on purpose: the standing rule that it does not change is lifted for this project only.
- **Type:** Barlow and Barlow Condensed stay. The brief's Inter or Roboto was not taken.
- **Surfaces:** soft layered cards, with tone steps and a soft shadow, replace the 1 dp hairline borders on cards. DESIGN.md's "No shadows" rule is rewritten to match.

## 2. What maps onto what (inventory of both apps, 2026-11-16)
| Brief | Where it lands in Grit |
|---|---|
| Soft cards | `PtCard` (Android) and `Card` (web), and every card built on them: Today, Coach, Plan, Progress, Settings, check-in, picker tiles. Row dividers inside cards stay, as lighter tone steps. |
| Press feedback | Buttons, the stepper −/+ buttons, chips, segments, picker rows, Today exercise rows, coach card buttons, the tab bar. |
| Layout fluidity | Logged set rows appearing and leaving (Log set, delete, "Add another set"); Today's rows when the open card changes; Coach cards on Apply or Dismiss (Android already animates this); Plan rows on move up or down, add and delete. |
| Seamless expansion | Today's exercise card opening in place from its collapsed row. Grit has no separate session screen. |
| Haptics | Log set, a missed set, a new best (PR), Apply on a coach card, stepper ticks: the Android app, and the web app on Android phones (`navigator.vibrate`, as 1.6's "Sound + vibrate" rest alert does). iPhones give web apps no vibration, so on iPhone nothing buzzes and the setting is hidden with the plain line 1.6 added. |
| Data animation | Line charts drawing in, sparklines, the weekly volume bars, ratio bars, the rest bar, on Progress and lift history. |

Shown in the examples, but **not in Grit** (each would be a new feature with its own rules, so none is in this project unless the owner asks): a ring counting the session's sets, goals for each lift, a weekly volume target, and a separate full-screen session view.

## 3. Rules the work keeps
- **State outlines stay:** the selected chip and segment, the planned-set ring, the switch-off border, focused and error fields, and the focus ring. Only card and container hairlines go. The contrast tests stay, updated for the new accent.
- **Motion has an off switch:** each app gets one way to run with motion off, and every test that reads positions or screenshots uses it. Real users who ask for reduced motion get none, as the web does today. Each motion stage adds a few tests with motion on. The switch arrives in stage 2, with the first motion (the press springs), not stage 3.
- **Speed:** the 100 ms checks on the exercise picker and the Exercises tab stay as they are. No view transitions, blur or per-row shadows on those lists. Every stage that touches rows is timed against the build before it (alternating builds, 16 or more opens each).
- **Web:** React, not vanilla JS. View transitions only where a state change is committed inside one; CSS transitions elsewhere.
- **Android:** Compose BOM 2026.02.01 already has `animateItem`, `animateContentSize` and shared elements; no library upgrade.
- **Haptics never replace information:** each buzz goes with something on screen. They follow a new Settings switch, "Vibrate on taps" (on by default, hidden on iPhone), separate from the rest alert setting.

## 4. Stages (each its own plan, build, checks, the owner's yes, and its own version)
1. **The new look (1.6, done and merged 2026-11-16):** soft cards, the grey powerlifting accent, DESIGN.md and tokens on both apps, dark and light, with every screenshot redone once. No behaviour changes.
2. **Press and haptics (1.7):** the motion-off switch for tests; press-in and spring-back on every tappable thing listed above; vibration on Android (app and web) and the Settings switch.
3. **Layout motion (1.8):** set rows, Today's open card, Coach and Plan lists.
4. **Data animation (1.9):** charts and bars filling on first show.

## 5. Waiting on the owner, separately
Thumbnails in the exercise picker (asked 2026-11-16) are paused. On the web, the picker's first open gets about 8 ms slower on average, and in 3 of 12 runs of the speed checks the first open went over 100 ms (worst 114 ms). Ask before building it, and whether it goes before or after this project.
