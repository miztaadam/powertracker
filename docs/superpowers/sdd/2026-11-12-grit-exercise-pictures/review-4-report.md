# Review of Task 4 (first ten exercise pictures), a56b982

Reviewed: the diff, the figure and test files, the guide text, and the dark and light contact sheets (cropped; the ten full-size panels and the ten thumbnails). Sources spot-checked with WebSearch limited to the allowed domains.

## Overall verdict

Not ready for the owner's style check: 6 of 10 pictures need fixes. The structure, checks, tests, captions and source comments are in good shape. The problems are in how a few poses read to an experienced lifter. There are no Critical (unsafe or impossible) findings. Four Important findings concern the pose or equipment, and none is hard.

Counts: Critical 0, Important 6, Minor 11.

| Picture | Verdict |
|---|---|
| Squat | OK (Minor only) |
| Bench | OK (Minor only) |
| Deadlift | Fix |
| Strict press | Fix |
| DB shoulder press | Fix |
| Lat pulldown | OK (Minor only) |
| Leg press | Fix |
| Pull-up | Fix |
| One-arm DB row | Fix |
| RDL | OK (Minor only) |

## Per picture

### Squat: OK
Both poses agree with the guide: the bar sits on the upper back, and the hip crease is below the knee, with the thigh sloping down to the hip. The shin and trunk angles look like a competent high-bar squat. The rack, J-hooks and safeties match the guide's safety line, and the rack stands in front of the lifter as it should when he has walked the bar out. The captions come from the guide.
- Minor: START is captioned "Stand tall, knees locked", but the torso is 12° forward and the whole body is tipped 4°. It reads as a slight hunch. Fix: the cause is the bar-over-midfoot check. Try the torso at 84° and the shin lean at 2° and see if BarOverMidfoot still passes. If not, leave it and note it.
- Minor: the safety-arm line runs at hip height through the standing lifter's thighs at START. Drawing arms behind the body is a fair side-view convention, but it looks like a bar through the legs. Fix: shorten `armLength` so the arms stop at the bar's x, plus about 5 cm.
- Minor: the thumbnail (END) is cluttered. The plate circle, the arms folded into a V, and the rack are all small and overlap. It is still recognisable as a squat. If the owner flags thumbnails, crop the view box tighter for this entry.

### Bench: OK
The bar touches mid chest at START. END is lockout over the shoulders. The head, upper back and hips are on the bench and the feet are flat. The safety arms are at chest height, which fits the guide's safety line. The forearm is 16° off vertical. I accept this: it is a 2D foreshortening limit, the test caps it at 20°, and PMC5968970 is cited for the 3D elbow of about 90°.
- Minor: at START the elbow drops below the bench-pad line and overlaps the bench rail, so it reads as clipping. Fix: tuck the elbow up by about 2 cm (shoulder angle 3–5° towards the head), or draw the bench's near rail behind the arm.
- Minor: the report says the spotter was kept from the draft. The render shows only safety arms, no spotter. The guide asks for "safety pins, or a spotter", so that is fine. Correct the report line only.

### Deadlift: Fix
- **Important: the START torso is too flat and the hips too high.**
  - The torso is about 19° from horizontal with the shins at 3° from vertical. That reads as a stiff-leg or RDL start, not a conventional pull. An experienced lifter expects the torso at about 30–45° from horizontal, the hips lower than the shoulders by a clear margin, and the knees slightly forward of the bar.
  - The cause is the vertical shins and the shoulder-over-bar rule. Fix: lean the shins forward by 8–12° (a bar 2.5 cm from the shin surface is then a shin touching the bar at the knee, which agrees with ACE and ExRx). Keep the shoulders 3–4 cm in front of the bar and raise the knee flexion until the torso is 30–35° from horizontal. The hips should end up at about 55–60 cm height.
  - Re-check BarOverMidfoot and BarNearLegs. If BarOverMidfoot fails when the shin leans, move the ankle x so the bar stays over mid-foot.
- Minor: the START thumbnail is hard to read. The figure is a small wedge with a circle. The fix above should help it.
- END is fine. Standing tall, the bar against the thighs, the arms angled to clear them. The caption is right.

### Strict press: Fix
- **Important: at START the bar floats about 15 cm in front of the face.** It reads as hovering at chin height, with the plate covering the face, rather than resting on the front of the shoulders and neck as the guide says ("front of your neck"). The cause is the 148° elbow cap.
  - Fix: use wrist extension in the solver. `armTo(..., wrist = +50..60)` with the hand bent back is how a real front rack looks. That brings the bar to 6–8 cm in front of the shoulder joint and about 2–3 cm below the chin.
  - Keep the elbows forward. Then lower the J-hook to match the new bar height.
  - If the figure cannot do it, move the bar down to collarbone height. That also takes the plate off the face.
- Minor: the safety-arm line crosses the chest at shoulder height in both panels, as in the squat.
- END is fine. Straight arms overhead, the bar over mid-foot, the 3° forward lean is not visible.

### DB shoulder press: Fix
Side view instead of front is acceptable. The kit has no seated front view, so this was a necessary call and it is explained in the comment. I accept the deviation, but the START pose must change.
- **Important: START reads as a curl or front raise.** The upper arm angles forward and down, the forearm slopes up and forward, and the dumbbell hangs about 17 cm in front of the shoulder at 15° up. The guide says "beside your shoulders, with your elbows under your wrists". In a side view that means a vertical forearm with the dumbbell directly above the elbow, and the dumbbell at ear or shoulder level in line with the shoulder (±3 cm).
  - Fix: place the dumbbell target at about `shoulder + (3, -14)` and make the elbow sit directly under it, which needs the upper arm to point down and slightly forward. The elbow is then about 12 cm below the dumbbell, and the forearm is vertical.
  - The solver's elbow limit should allow this, because it is a less-folded arm than now.
- Minor: the dumbbell is a small ring with no visible head shape. It is readable at full size and not at thumbnail size. Accept for the style check.
- END is fine and the back pad, head and hips contacts are right.

### Lat pulldown: OK
Seated, thighs under the roller, feet flat, torso back 10° ("only slightly"). The cable and bar land at the upper chest at END, the thigh pad is drawn, and the stack stands at the right scale.
- Minor: at END the elbow sits forward of the torso, in front of the belly, and the forearm slopes up and forward. A real pulldown ends with the elbows down at the sides or slightly behind the torso line, the forearm near vertical. Fix: rotate the shoulder 20–30° backward at END (so the elbow is level with or behind the torso) and keep the bar on the upper chest. The upper-chest check should still pass with the forearm leaning slightly back.

### Leg press: Fix
- **Important: the END pose is too deep for the guide.** The thigh is nearly vertical and the knee is at shoulder height. The trunk–thigh angle works out at roughly 130° of hip flexion, close to the 140° limit that was widened for the squat. The guide says to stop sooner if the hips lift or the thighs press into the ribs, and this pose is at that point. The conservative reading (Global Constraints) is the shallower one.
  - Fix: reduce the hip flexion at END to about 110–115°. The easiest way is to lower `PLATE_RISE` (66) towards 40–45 so the plate is nearer the rail line, or to steepen the back pad a little (`BACK_ANGLE` 140 → 132). Keep the knee at about 90°. Check the ankle stays inside the 35° limit.
  - This also stops the leg press from needing the hip widening at all.
- Minor: the carriage looks like a long "Y". The plate line crosses the rail and the carriage arm ends in a point. Fix: draw the plate as a short thick bar of about 55 cm perpendicular to the rail, ending on the rail with a T-joint, and clip the rail so it does not extend past the plate on the lower side.
- Minor: the seat is a small stool on a single post. It reads as a stool, not a machine seat. Fix: widen the seat top to about 30 cm and add a second leg, or draw a base rail under the seat.
- START (knees 15°) is fine.

### Pull-up: Fix
- **Important: the bar reads as a vertical pole.** The bar is a 3.2 cm circle in a side view and the hand covers it. What shows is the 40 cm upright capsule drawn above the bar, which looks like a rope or pole beside the face. A viewer cannot see "chin over bar" because the bar itself is hidden under the hand. I measured the chin only about 3 cm above the bar top, so the margin is thin as well.
  - Fix 1: draw the bar circle on top of the hand in the held-prop style (ACCENT or EQUIPMENT, radius at least 2.5 cm for visibility) and shorten the upright to about 15 cm, drawn behind the figure.
  - Fix 2: raise the END shoulder so the chin clears the bar top by 5–6 cm (shoulder 8 cm below the bar centre instead of 4).
  - Better still, consider the front view for this entry: the bar becomes a horizontal line, the wide grip shows, and chin-over-bar is obvious. The brief allows it because the pull is in the frontal plane. If the kit cannot do a front view yet, keep side view and do both fixes above.
- Minor: at START the head hides behind the straight arms, and the figure is thin and tiny in its frame. The thumbnail (END) is a faint stick figure. Fix: use the larger view box or crop to the figure's height.
- Minor: the far leg drawn in BODY_FAR reads as a shadow rather than a leg. Accept for now; the crossed-leg look is in the guide.

### One-arm DB row: Fix
- **Important: at END the dumbbell is about 25 cm below the torso, not at the ribs.** The caption says "Pull to your ribs". The guide allows "touches your ribs or your upper arm is just past level with your torso", and the picture shows the second. The forearm hangs vertical from an elbow at torso level, so the dumbbell ends up level with the bench top. An experienced lifter expects the elbow above the back line and the dumbbell near the lower ribs.
  - Fix: rotate the shoulder further back, to about -25..-30° (it is -10° now), so the elbow rises about 10–13 cm above the torso line, and keep the forearm vertical. The dumbbell then sits within about 8–10 cm of the ribs.
  - Or change the caption to the guide's other wording that fits. "Upper arm past level" is 21 characters and fits. Prefer the pose fix, because it also matches the caption.
- The rest is right: far hand and knee on the bench, the near foot on the floor and slightly back, the torso close to level (12°), the arm straight at START. The kit's `nearLimbsBeside` flag is a sensible fix.
- Minor: the far arm in grey has a zig-zag elbow bend. It reads, but it is a little odd. Accept.

### RDL: OK
Knees slightly bent (18°) in both panels. The hips go back, the shins come back to vertical, the bar slides down the legs to just below the knees, and the torso is about 29° from horizontal. The rack matches the guide's safety line, and the posts are clear of the head. This is the best of the batch.
- Minor: the safety-arm line crosses the shins and plate at END. Fix: as for the squat, shorten the arms to about 5 cm past the bar.

## Joint-limit changes

- **Ankle dorsiflexion 20 → 35°: accept.** PMC6249959 and PMC7932177 are on the allowed list. The search results show young healthy adults at about 39° (knee extended) and 45° (knee flexed) in the weight-bearing lunge, and 35° as the line for limitation in older adults. 35° is therefore a conservative healthy-adult value. The squat's 33° shin lean sits inside it.
- **Side-view hip flexion 120 → 140°: accept, with a note.** The model's hip is the trunk–thigh angle, which includes pelvic tilt, so a parallel squat with a 46° trunk lean gives about 135°. PMC9403702 and PMC10143703 are on the allowed domain. I did not verify those two PMC numbers by search, because the one search I made for hip flexion returned nothing relevant. The controller should open them once. Because the leg press END also sits near 130°, the widening should be limited to the squat. The fix above brings the leg press back under 120°, so no other picture needs the new limit.

## Implementer's other concerns

- **DB press side-on:** accepted. Fix the START pose as above.
- **Deadlift torso 19°:** not accepted. See the Important finding above.
- **Strict press bar 15 cm in front:** not accepted as drawn. A wrist-extension fix is cheap.
- **Bench forearm 16°:** accepted.
- **Leg press carriage "Y":** Minor. Redraw as a T as above.

## Code, checks, tests, sources

- Checks per entry match the brief's list (bench, squat, deadlift and RDL, strict press, pull-up with `ChinOverBar`, lat pulldown with `BarTouches(UPPER_CHEST)`, leg press and row by contacts). The row and the leg press have empty `checks`, which the brief allows because it asks for contacts only.
- `BatchOneTest` is real. It asserts the contacts and checks per exercise, the forearm limit on the bench, the knee ranges on the leg press, and the alias lookups. The RED/GREEN evidence in the report is credible.
- Weak spot: `ChinOverBar` measures the neck–head junction, and its margin is only about 3 cm. Add a 4 cm margin to the check when the pull-up is fixed, so it cannot pass by a hair.
- Weak spot: there is no check on hip flexion for the leg press END, which is why it slid to 130°. Add `assertTrue(hipFlexion < 120)` to the leg press test after the fix.
- Sources are cited in comments for every non-guide position, with allowed domains only (exrx.net, acefitness.org, nsca.com, pmc.ncbi.nlm.nih.gov). Neutral values are marked as neutral.
- Captions: all 20 are 24 characters or fewer and come from the guide's wording. Capitalisation is plain and consistent.
- Equipment and scale: bar and plates at 45 cm, benches at 42–45 cm, racks near 2 m. Sensible.
- Light and dark: both read well. Minor: the plate outline (ACCENT) is pale in the light theme. Confirm the 3:1 test covers the light surface, since it looks faint on the sheet at thumbnail size.

## Fix list (for the controller)

1. Deadlift START: shin lean 8–12°, torso 30–35° from horizontal.
2. Pull-up: draw the bar visibly and shorten the upright, raise the chin margin to 5–6 cm, and consider the front view.
3. DB shoulder press START: forearm vertical with the dumbbell above the elbow, dumbbell in line with the shoulder.
4. One-arm DB row END: shoulder about -25..-30° so the dumbbell reaches the ribs, or change the caption.
5. Strict press START: wrist extension, bar to 6–8 cm in front of the shoulder, off the face.
6. Leg press END: hip flexion down to 110–115° (lower `PLATE_RISE` or steepen the pad).
7. Optional Minor items: the lat pulldown END elbow, the safety-arm lengths, the leg press carriage and seat, the bench elbow overlap, and thumbnail cropping.
