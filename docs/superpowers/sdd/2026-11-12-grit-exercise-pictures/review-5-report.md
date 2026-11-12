# Review of Task 5 (21 main-lift pictures), commit 66edf08

Verdict: Needs fixes. Critical 0, Important 2, Minor 5.
Checked: all 21 rendered full size in dark (glanced light for contrast, thumbnails in each sheet); the code in figures/areas/MainLifts.kt, Kit.kt, Body.kt, Figure.kt, FigureChecks.kt; BatchMainLiftsTest and the KitTest additions; the commit message; the three BatchOneTest edits. Guide wording read against every caption.
Existing figures: Squat, Bench, Deadlift, Strict press and the other six Task 4 figures re-rendered and compared with the Task 4 sheets (t4-*, batch1-*): pixel-identical except the sheet label. No Task 4 pose changed.

| Figure | Verdict | Figure | Verdict |
|---|---|---|---|
| Pause squat | OK | Barbell floor press | OK |
| Front squat | OK | Wide-grip bench | OK |
| Pin squat | OK | Deficit deadlift | OK |
| Safety-bar squat | OK | Paused deadlift | OK |
| Box squat | OK | Paused deadlift at the knee | OK |
| High-bar squat | OK | Block pull | OK (Minor) |
| Tempo squat | OK | Sumo deadlift | Fix |
| Spoto press | Fix | Trap-bar deadlift | OK |
| Long-pause bench | OK | Pin press | OK |
| Larsen press | OK | Close-grip bench | OK |
| Board press | OK | | |

## Important
1. Sumo deadlift reads as a conventional deadlift (figures/areas/MainLifts.kt:974-993). Rendered START next to Deadlift START at 3x: same wedge torso (about 36 vs 31 degrees above level), same shins, thigh only slightly shorter. END is the Deadlift's END pose unchanged. The thumbnail (START) cannot be told from the Deadlift's. The guide's defining points (very wide stance, hands inside the legs) are invisible, and the caption "Grip between your legs" shows nothing between legs. The test only asks torso > Deadlift + 5 degrees. The comment says a front view cannot draw the set-up; I did not find that demonstrated. Fix: draw START and END in View.FRONT (the skeleton and rack have front support) with the hands hanging between the knees; if the solver truly cannot reach, keep the side view but push the torso to 50 degrees or more above level with hips clearly lower, tighten the test to that, and retitle START to something the side view does show ("Chest high, hips low" is in the guide).
2. Spoto press gap is invisible (figures/areas/MainLifts.kt:614-621, test spotoPress_stopsJustAboveTheChest, gap 2.0..3.0). The 2.5 cm gap puts the bar ring about 4 px above the chest at the sheet scale, inside the plate disc; the START panel looks the same as Bench START (3x crop compared). The gap is the whole point of the exercise, and the guide says "without touching". Fix: raise to 4-5 cm (1-2 in is the usual "just above"; the brief said about 2-3 cm, so the controller may want to rule), set BarAbove to 3.5..5.5 and the test to match, then re-crop at 3x to confirm daylight shows between ring and chest.

## Minor
- Block pull (figures/areas/MainLifts.kt:942-962): the blocks are drawn under the feet in both panels, and at END the bar is off them, so the picture reads like a deficit platform (feet on a slab). Plates are correctly raised at START. Suggest drawing the blocks a little narrower than the plate (about 45 cm) and, if the kit can, in front of the foot; or accept, since START shows the plates on them.
- Pin squat, Pin press, Barbell floor press: the pin/safety rail is drawn through the plate and forearm (floor press: through the forearm at 30 cm). Same side-view convention as Task 4's safety arms; clutter only.
- Safety-bar squat: the handle is a thin diagonal stick across the plate disc, so it reads as one bar through the chest. Recognisable with the grey pad and the cambered plate; accept.
- Trap bar: the 65 cm frame line crosses the thigh and hand at END. Physically right for a side view, slightly odd to read. Accept.
- Paused deadlift END is only 5 cm up (6 px offset); visible, but the START/END difference is small. Accept.

## Rulings
(a) Pause squat, High-bar squat, Long-pause bench, Close-grip bench reusing the parent pose: accept. The guide differences are a hold (not drawable in a still) or grip width (not visible side-on). High-bar: the approved Squat is already drawn high-bar (bar on top of the upper back, trunk 46 degrees, stated in the code comment and in PMC10143703); a more upright torso would make it differ from the approved Squat for no source reason. Close-grip: accept, but note the Wide-grip picture does show a different arm (forearm vertical, upper arm foreshortened), so Close-grip is the one grip picture that does not change.
(b) Sumo side-on: not accepted as drawn, see Important 1. Side view is fine only if the torso/hip difference is large enough to read.
(c) Near-duplicates: Sumo vs Deadlift and Spoto vs Bench fail (above). Pause squat/Squat, High-bar/Squat, Long-pause/Bench, Close-grip/Bench are identical by design and acceptable per (a). Block pull vs Deficit: distinguishable (plate raised vs feet raised), Minor above.

## What passed
- Poses vs guides: front squat (front rack, elbows level, upright, thighs level), pin squat (bar on pins, knees 85), SSB (pads on back, handles in front, thighs just below level), box squat (box behind, shins 5 degrees, hips on box), tempo (halfway then bottom), pin press (10 cm), Larsen (feet and legs off the floor, straight), board press (2 boards on the chest), floor press (on the floor, elbows on the floor, forearm vertical), wide-grip (forearm vertical, shorter lockout), deficit (5 cm platform, plates on floor), paused deadlifts (5 cm off the floor; bar 4 cm below the knee), trap bar (handles beside legs, more knee bend).
- Captions: all 42 are 24 characters or fewer and use the guide's wording; each fits its drawn pose. Pins/spotter appear only in Pin squat, Pin press and where the guide includes RACK_SAFETY (Floor press and Larsen both do).
- Equipment scale: box 38 cm high, 50 deep; boards 3.8 cm each, 30 long; platform 5; blocks 10 (guide: 5/10/15); hex frame 65; each new piece has a size comment and a KitTest case (squatBox, deficitPlatform, pullingBlocks+benchBoards, safetyBar/trapBar/frontRack, safetyArm surface).
- Tests are real, with bounds (hip depth, torso lean, knee range, forearm vertical, gap, plate height, BarRestsOn, HandlesBesideLegs). RED value of the sumo bound is the weak one (Important 1).
- Sources in new comments: exrx.net, pmc.ncbi.nlm.nih.gov, nsca.com, powerlifting.sport only. I did not re-open the PMC numbers.
- BatchOneTest edits: strict-press bar 7..10.5, row elbow above torso line > 8 and dumbbell 0..21, DB press grip 8..16 cm in front; no Task 4 pose touched (pixel compare above).
- Commit: title "feat(domain): main lift pictures", body short, ends with the two attribution lines from the brief (Opus 5.5 and session ...BboHw6FdBYrNDSMx1QWyzy), no other model names. The constraints.md copy has a different session id; the brief says to use its own, which was done.

## Tests
`./gradlew :domain:jvmTest --tests '*BatchMainLiftsTest*' --tests '*BatchOneTest*'`: BUILD SUCCESSFUL. BatchMainLiftsTest 20 tests, BatchOneTest 11 tests, 0 failures, 0 skipped. Not run (controller's): full domain, web, e2e.

## Other
- The working tree has unstaged edits to docs/design/screens/exercises-{dark,light}.png (not in the commit, which changed docs/design/web/screens/*). I did not touch them; the controller may want to check they are meant.
- Sheets I rendered are in /tmp/claude-0/-home-user-powertracker/530b1e32-ec0a-5578-ba2f-b77a61669676/scratchpad/review5/ (NN-dark.png and NN-light.png, in the brief's order; 22-25 are Squat, Bench, Deadlift, Strict press).
