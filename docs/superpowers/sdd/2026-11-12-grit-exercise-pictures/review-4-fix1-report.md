# Scoped re-review of Task 4 fix round 1 (757f2ee)

Reviewer: sonnet-checker, new cloud session. Verdict: **PASS**. All six Important findings are resolved in the code and in the sheets (dark and light). No new Important findings.

| # | Finding | Status | Measured |
|---|---|---|---|
| 1 | Deadlift START | Resolved | shin 12.0°, torso 30.65°, hips 26 cm below shoulders, shoulder 1.0 cm ahead of bar |
| 2 | Strict press START | Resolved | bar 10.0 cm in front of shoulder, 5.4 cm below chin; plate still overlaps jaw (unavoidable) |
| 3 | DB shoulder press START | Resolved | forearm vertical, DB over elbow, ear height, true elbow 120° |
| 4 | Leg press END | Resolved | hip 113.3°, knee 85.0°, ankle 31.7° |
| 5 | Pull-up | Resolved | bar visible, chin 5.5 cm over bar top, ChinOverBar margin 4 cm with failing test |
| 6 | One-arm DB row END | Resolved | shoulder −25°, elbow 14.1 cm above back line, DB under lower ribs |

Deviations accepted: DB press dumbbell 13 cm in front (side view); leg press pad 25° + plate tilt 10°; pull-up side view, still small.

New Minor (parked; apply with the style changes before Task 5, when Gradle can run):
1. BatchOneTest.kt:104 strict press bound 6..11 vs actual 10.0; fix report said "about 9". Tighten to ~7..10.5.
2. BatchOneTest.kt:165 row bound 0..22 vs actual 20; add elbow-above-back-line > 8 cm assertion.
3. BatchOneTest.kt:89 deadlift shin at the top of 8..12 (12.0) — no slack.
4. DB press: no test on dumbbell x offset; assert grip.x − shoulder.x in 8..16.
5. Leg press thigh drawn ~88° from horizontal; "knee near chest" look partly remains — owner's style check.
Also Minor: squat END rack arm tip touches the lifter's head/back region.

Tests: Gradle could not run in this session — the proxy blocks dl.google.com and the Gradle plugin portal (Android Gradle plugin and foojay plugin unresolved); no Android SDK installed. Reviewer instead compiled the library with kotlin-compiler-embeddable outside the repo, ran FigureChecks.check on all ten figures (no problems) and re-evaluated the BatchOneTest assertions for the six entries by hand (all pass). Implementer's counts (jvm 262/1 skipped etc.) not re-verified here. No code changed in this round, so the commit 757f2ee stands.
