# Polish audit, 2026-11-24: findings, unverified, nothing changed

Asked for by the owner (`/polish the whole app`), then parked the same day for the iPhone "buggy" report (`docs/handoff-2026-11-24.md`). Four read-only audits of the code by helper agents, plus one browser sweep. **Nothing here was changed in the app, and no finding was verified unless marked [verified]:** each is a reading of the code by a helper, to be opened and checked before it is fixed. Paths: APP=app/src/main/kotlin/com/powertracker, WEB=web/src, DOM=domain/src/commonMain/kotlin/com/powertracker/domain.
Rule: fix departures from DESIGN.md / the app's own conventions after verifying; list taste and pinned behaviour for the owner. App change => 1.17.6 (versionCode 34). Do not touch Dialog, Menu, historyCloses, router while the iPhone report is open.

## My browser sweep (web, 390x844, both sides) [verified]
Clean: no console errors/warnings, no targets < 44px, no unnamed controls, no sideways scroll, focus shown everywhere (number fields via underline, text field via box ring, Muscles chip via its face). Only small text = 11px eyebrows (deliberate).

## Behaviour (states audit) — highest value
1. ANDROID: failed write on Today/Settings crashes (no catch, no CoroutineExceptionHandler). TodayViewModel: coach targets on open (~295-317), startSession, logSet, setMissPoint, deleteSet, applyTarget, dismissTarget, finishSession (coach run+insert), answerWelcomeBack; CoachViewModel:136; SettingsScreen.kt:143,199. Web catches all (TodayScreen.tsx:102-118 `reported`). Worst: coach pass on every open.
2. ANDROID: Finish has no double-tap guard (TodayViewModel.finishSession). Button stays tappable until summary set. Likely: summary replaced with newNotes=0; rarely: advice stored twice. Web guarded (useToday finishing). NOTE CheckInTest.kt:441,447 calls finishSession twice on purpose.
3. ANDROID: double tap on Start session can store two sessions for a day (starting flag released at insert return, not when shown). Web checks in transaction + unique date index.
4. Past midnight the apps differ (Android: per-day view model, session leaves screen on resume; web holds 6 h). Pinned on web; Android stated on purpose in code. OWNER QUESTION.
5. ANDROID restore: settings written after the data transaction; failure says "Couldn't restore" though data replaced. Runs in Settings screen scope (cancel on leave: suspected).
6. WEB: failed Add on Plan's add form loses the numbers (form closes at once); Android keeps the form.
7. ANDROID: applying a stall/weak-point card = two writes (plan change, then mark applied).
8-9, 13. ANDROID first-frame flashes: Plan "Known bests" before plan read (PlanViewModel.kt:135, PlanScreen.kt:441); Settings first frame default settings (SettingsScreen.kt:136); Exercises header count jumps (ExercisesScreen.kt:185).
10. Bodyweight dialog: Android red outline only, no message, accepts "80."; web "Enter a number from 20 to 400." (TodayDialogs.kt:68 vs TodayDialogs.tsx:28-31).
11. ANDROID clips one-line text without ellipsis (no TextOverflow.Ellipsis anywhere; Buttons.kt:36,110; Headers.kt:57). Web has ellipsis.
12. ANDROID Plan edit form says "Unknown exercise" for an archived exercise (PlanScreen.kt:618).
14. WEB plan builder: Back stays live while saving.
16. Both: Today's in-memory state not reset after a restore (suspected).
17. ANDROID: export/restore have no one-at-a-time guard (suspected).
Known/recorded: vibrateOnTaps resets on restore on BOTH apps (web backup.ts:533 sets default) — AppSettings comment says owner option A; OWNER QUESTION.

## Design-system departures (mechanical)
ANDROID
- SegmentedChoice segments 40dp tall (Choice.kt:128 heightIn(min=40.dp)); rule 48dp; web is 48px; ChoiceChip does it right (Choice.kt:50).
- Scroll animations not gated by LocalMotion: TodayScreen.kt:316, TemplatesScreen.kt:236, PlanBuilderScreen.kt:374 (guide does gate: GuideScreen.kt:201).
- Sheet setup duplicated instead of GuideSheet: PlateCount.kt:107, SkipToday.kt:143 (FormGuideSheet.kt:66-80).
- Clickables without Role: PtCard.kt:50, SetRow.kt:119, ExerciseBrowser.kt:601, ProgressScreen.kt:421, PlanBuilderScreen.kt:593; SettingsScreen.kt:409 guide row uses plain clickable (no press sink).
- Stepper.kt:263 placeholder muted at 55% alpha (contrast not computed).
- Headers.kt:73 SectionHeader has statusBarsPadding (likely 0).
- Off-scale spacing: Pill.kt:29 (10), Choice.kt:41 (10), :73 (6), ExerciseBrowser.kt:400,440, Charts.kt:171, ProgressScreen.kt:364 (6), Buttons.kt:26 (20/14). Mirrors web values => probably deliberate component metrics; TASTE unless owner wants.
- extraLarge shape 28dp (Dimens.kt:38) on AlertDialogs; web Dialog.css:25 also 28px. Off the 6/10/14/20 scale on both => deliberate? OWNER QUESTION.
- ModeLabel 10sp (Headers.kt:89, ModeSwitchOverlay.kt:306; web ModeLabel.css:12 "owner's mockup option C") => deliberate, undocumented in DESIGN.md: document it.
WEB
- TodayScreen.tsx:162 scrollIntoView behavior:'smooth' not gated by data-motion (JS smooth scroll is not cut by the CSS clamp).
- ModeSwitchOverlay.css:39-44 raw #8db8ff / #0a1020, not scheme-aware (light uses dark blue?) — verify against modeSwitchPaint.ts.
- .settings-link (settings.css:33) no :active and not in press lists; .bb-row (progress.css:171) no :active tint (no feedback under reduced motion); .tpl-row tint but not in press lists.
- NumberField focus underline uses --accent (NumberField.css:55-59,68) though tokens.css:278 says focus is never the accent; TextField uses --text.
- Disabled colour 40% in StepperField.css:32 vs 38% elsewhere.
- Dead `// eslint-disable-next-line` useModeSwitch.ts:477 (no linter). Unused exports: useModeSwitchState (useModeSwitch.ts:358), TABLE_NAMES (db/schema.ts:146), SIDE_NS (SidePicture.tsx:97).
- GuideScreen.css:38 lone easing curve cubic-bezier(0.2,0,0,1) vs app's (0.2,0.8,0.2,1). TASTE.
- Off-scale spacing mirrors Android (Pill 10, Snackbar 14, Segmented 9+1, charts 6, Button 14/20). TASTE.
BOTH
- Arrow characters in text (DESIGN.md:155): "(3 → 4)" and "12 → 14 sets a week" (PhysiqueCards.kt:51,56; CoachAdviceCard.tsx:141,161); DOM/coach/StallRule.kt:71 "(5s → 3s or 8s)". Coach elsewhere writes "from X to Y". "›" in lock screen text (LockScreen.kt:81, LockScreen.tsx:21); web install card writes the path with commas.

## Wording
Cross-app
- Bodyweight dialog message (see 10).
- "Coach" eyebrow on check-in-due and welcome-back cards: Android yes (PhysiqueCards.kt:72, WelcomeBackCard.kt:29), web no.
- Plan row "3 × 5  @ 8" two spaces on Android (PlanScreen.kt:531) vs one on web (PlanScreen.tsx:379).
- Spoken set row: Android kg " @ 8" (SetRow.kt:91-92) vs web and Android plates " at RPE 8".
- Accessible names: web Plan day "Add" unnamed object (PlanScreen.tsx:335) vs Android "Add exercise to Monday" (PlanScreen.kt:494); web "Change" (PlanDialogs.tsx:88) vs Android "Change exercise" (PlanScreen.kt:695); Progress lift tile name sentence differs (ProgressScreen.kt:255 vs ProgressSections.tsx:70, StatTile.tsx:32).
- Curly apostrophe only in WEB guideLabels.ts:53 ("set’s").
- "Exercises" heading on bodybuilding Progress gated differently (ProgressScreen.kt:144 vs BodybuildingProgress.tsx:61).
Within-app (both apps unless said)
- "1 sets": Plan day line (PlanScreen.kt:484; PlanScreen.tsx:326); draft line handles neither word (PlanBuilderScreen.kt:498; DraftParts.tsx:65).
- Snackbar full stops: Coach tab's have none ("Already in your plan on Monday", "Deload week on — targets lightened for 7 days", "Couldn't update — try again"; CoachViewModel.kt:300,205,325; useCoach.ts:279,297,80), "Added to Monday", "Added to today" (ExercisesScreen.kt:132,156); others end with one.
- "failed reps" counts sets (DOM/coach/FatigueRule.kt:100-102).
- "physician" (DOM/coach/SkipCoach.kt:73) vs "doctor or physiotherapist" (DOM/library/Library.kt:20).
- "Add custom exercise" title vs "Create your own"/"Your own" (CustomExerciseDialog.kt:114; CustomExerciseDialog.tsx:46).
- Web: "tap to refresh" (platform/update.ts:14) vs "Reload"/"Update and reload" elsewhere.
- Headings ending in a full stop: "Welcome back to the better side.", "No advice right now. Keep logging.", "No exercises match."
- "See coach notes"/"See Coach" vs "Open Coach"/"Open Plan".
- Owner/voice questions (leave): Missed/Failed/"Where did it stall?"; Restore→"Replace all data?"; notes/suggestions/advice; mode/side/style; "Weekly template"; Finish vs Finish session; Floor/Mid/Lockout vs coach's names; guide "muscles" vs "body parts".

## Earlier notes' open items
- chart's dates at 160px (owner), Exercises tab ⓘ on its own line (narrow), 239–240px gap in media queries (could be fixed with width < 240px), two number forms in stored target payloads, assisted machines direction.
