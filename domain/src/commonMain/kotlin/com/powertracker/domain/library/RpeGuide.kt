package com.powertracker.domain.library

/**
 * The "What RPE means" sheet: RPE as reps in reserve (RIR), for lifters reading it between sets. No safety line:
 * this is not an exercise. What backs each line:
 * - RIR definition, 10 → [Sources.PMC_RIR_RPE_APPLICATION] (RPE 10 = 0 RIR, 9 = 1 RIR) and
 *   [Sources.PMC_RPE_SCALES_VALIDITY_REVIEW] (its RIR RPE table: 10 = maximum effort).
 * - 9.5, 8.5 → [Sources.PMC_RPE_SCALES_VALIDITY_REVIEW] (9.5 = no further repetitions but could increase load;
 *   8.5 = 1-2 repetitions remaining).
 * - 9, 8, 7 → [Sources.PMC_REGULATING_MONITORING_RT] (7 RPE = 3 RIR, 8 = 2, 9 = 1); 9 also [Sources.PMC_RIR_RPE_APPLICATION].
 * - 6 → [Sources.PMC_RPE_SCALES_VALIDITY_REVIEW] (5-6 = 4-6 repetitions remaining) and
 *   [Sources.PMC_RIR_RPE_APPLICATION] (5-6 grouped as 4-6 RIR, since a range is easier past 3 RIR).
 * - The 6-10 range in 0.5 steps is the app's own input rule, unsourced. 7.5 and 6.5 are left out: no source found.
 */
object RpeGuide {
    val title: String = "RPE: reps in reserve"

    val lines: List<String> = listOf(
        "RPE here counts reps in reserve: how many more reps you had left at the end of the set.",
        "10: no more reps possible.",
        "9.5: no more reps, but you could have added load.",
        "9: one more rep.",
        "8.5: one rep left, maybe two.",
        "8: two more.",
        "7: three more.",
        "6: four to six more.",
        "Grit takes 6 to 10, in steps of 0.5.",
    )

    val sources: List<SourceId> = listOf(
        Sources.PMC_RIR_RPE_APPLICATION,
        Sources.PMC_RPE_SCALES_VALIDITY_REVIEW,
        Sources.PMC_REGULATING_MONITORING_RT,
    )
}
