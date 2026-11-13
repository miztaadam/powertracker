package com.powertracker.domain.coach

import com.powertracker.domain.library.Source

/**
 * Where the coach's training numbers come from, in the style of the catalogue's `library/Sources.kt`. Each citation
 * says what the page says and when it was checked. Never shown in the app.
 */
object CoachSources {
    /**
     * The size of a bodybuilding weight increase, [BodybuildingCoach.INCREASE] = 0.02.
     *
     * What the page says: ExRx.net's weight training guidelines summarise the American College of Sports Medicine
     * position stands on progression models in resistance training for healthy adults (2002 and 2009): increase
     * the load by 2-10% once the current load can be done for one to two repetitions beyond the desired number
     * (2009: beyond the upper end of the rep range). The band is 2-10%; the coach uses its low end, 2%, and then
     * rounds to a loadable weight at least one step above the last one, so a small load still goes up.
     *
     * The trigger differs: the source raises the load at one to two reps past the range, the owner's trigger is
     * every planned set reaching the top of the range at or under the target RPE. The owner's trigger is kept.
     *
     * Found with a web search restricted to the allowed sources; the page itself was not opened.
     *
     * The 2-10% figure was confirmed again on 2026-11-13 by a web search restricted to exrx.net, acsm.org and
     * pmc.ncbi.nlm.nih.gov (the ExRx page attributes it to ACSM's 2002 and 2009 position stands). ACSM published a
     * new resistance training position stand in 2026 (https://acsm.org/resistance-training-guidelines-update-2026/),
     * which should be read before stage 3's weekly-volume numbers are chosen.
     */
    val BODYBUILDING_INCREASE = Source(
        title = "Weight Training Guidelines (summarising the ACSM position stands on progression models in resistance training)",
        publisher = "ExRx.net",
        url = "https://exrx.net/WeightTraining/Guidelines",
        accessed = "2026-11-13",
    )

    /**
     * The plan builder's weekly aim, [com.powertracker.domain.plan.PlanBuilder.WEEKLY_SETS] = 10 counted sets per
     * muscle group a week, and its frequency aim, every target group trained on at least two days a week.
     *
     * What the page says: ACSM's announcement of its 2026 position stand on resistance training ("Resistance Training
     * Prescription for Muscle Function, Hypertrophy, and Physical Performance in Healthy Adults", an overview of 137
     * systematic reviews, the first update since 2009) prescribes at least 10 sets per muscle group per week for
     * muscle growth, with more volume able to bring more growth, and training all major muscle groups at least twice
     * a week. The builder takes 10 as its aim, not as a ceiling.
     *
     * Checked on 2026-11-13 by a web search restricted to the allowed sources; the page itself was not opened.
     */
    val BUILDER_WEEKLY_SETS = Source(
        title = "ACSM Unveils Landmark 2026 Resistance Training Guidelines",
        publisher = "American College of Sports Medicine (ACSM)",
        url = "https://acsm.org/resistance-training-guidelines-update-2026/",
        accessed = "2026-11-13",
    )

    /**
     * The plan builder's frequency aim: every target group trained on at least two days a week.
     *
     * What the page says: ACSM's infographic for the 2026 position stand, "5 Things to Know About Creating an
     * Effective Resistance Training Plan" (published March 2026), says to train all major muscle groups at least two
     * days per week and to build up gradually, and that the best plan is the one the lifter keeps doing.
     *
     * Checked on 2026-11-13 by a web search restricted to the allowed sources; the page itself was not opened.
     */
    val BUILDER_FREQUENCY = Source(
        title = "5 Things to Know About Creating an Effective Resistance Training Plan",
        publisher = "American College of Sports Medicine (ACSM)",
        url = "https://acsm.org/effective-resistance-training-program-infographic/",
        accessed = "2026-11-13",
    )

    /**
     * The plan builder's order inside a session: compound before accessory, and the lifter's priority body parts first.
     *
     * What the page says: ExRx.net's weight training guidelines summarise the 2026 ACSM position stand as "compound /
     * primary lifts before accessory work" and "compound movements before isolated exercises" (the 2009 and 2002
     * stands, on the same page, put multi-joint before single-joint and larger muscle groups before smaller ones).
     * The same summary adds that exercise order within a session does not consistently affect muscle growth: the
     * order matters mostly for strength, so the builder's order is a sound default rather than a growth rule.
     * Putting the priority body parts' exercises first follows "primary lifts first": the lifter's priorities are the
     * primary work of their plan.
     *
     * Checked on 2026-11-13 by a web search restricted to the allowed sources; the page itself was not opened.
     *
     * Not sourced, the designer's own numbers, flagged for the owner: the session templates and their sizes (8
     * exercises on the first and third full-body days and 9 on the second, 8 on an upper day, 6 on a lower day) and at most one held-over pick a session
     * ([com.powertracker.domain.plan.PlanBuilder.HELD_OVER_PER_SESSION]); 4 sets for a session's first exercise and
     * for a priority body part's exercises where the rest get 3 ([com.powertracker.domain.plan.PlanBuilder.LEAD_SETS],
     * [com.powertracker.domain.plan.PlanBuilder.PRIORITY_SETS]); at most 3 priority body parts, chosen from the 12
     * target groups ([com.powertracker.domain.plan.PlanBuilder.MAX_PRIORITIES]); the hand-picked default exercises
     * ([com.powertracker.domain.plan.PlanBuilder.SlotKind]). The 3 sets of 8-12 at RPE 8 are the owner's and need no
     * source.
     */
    val BUILDER_ORDER = Source(
        title = "Weight Training Guidelines (summarising the ACSM 2026 position stand on resistance training)",
        publisher = "ExRx.net",
        url = "https://exrx.net/WeightTraining/Guidelines",
        accessed = "2026-11-13",
    )
}
