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

    /**
     * The lagging-muscle coach's weekly ceiling, [PhysiqueCoach.WEEKLY_CEILING] = at most 20 counted sets a week for
     * one body part.
     *
     * What the page says: Baz-Valle, Balsalobre-Fernandez, Alix-Fages and Santos-Concejero (2022), "A Systematic
     * Review of The Effects of Different Resistance Training Volumes on Muscle Hypertrophy", Journal of Human
     * Kinetics 81: 199-210. The review sorts training into low (under 12), moderate (12-20) and high (over 20) weekly
     * sets per muscle group, over seven studies, and concludes that 12-20 weekly sets per muscle group may be the best
     * standard recommendation for growth in young, trained men; for the quadriceps and the biceps, training each
     * muscle twice a week, there was no added benefit from going above that range. The search summary also notes the
     * review found a different effect for the triceps than for the quadriceps or biceps. The coach takes 20 as a
     * ceiling, not as an aim.
     *
     * Our count gives a set in full to every group an exercise works (the plan builder's rule), where the study counts
     * a set for the muscle it is aimed at, so a body part reaches 20 counted sets sooner than it would by the study's
     * count: the ceiling errs on the side of less.
     *
     * Checked on 2026-11-14 by a web search restricted to the allowed sources; the page itself was not opened.
     *
     * Not sourced, the designer's own numbers, flagged for the owner and approved by the owner on 2026-11-14: a
     * check-in falls due 28 days after the last one ([PhysiqueCoach.CHECK_IN_DAYS]); one round of suggestions for each
     * check-in; a round adds one set to at most two exercises for a body part, or else one new exercise of 3 sets; no
     * exercise is taken above 4 sets ([PhysiqueCoach.MAX_SETS]); at most 3 body parts get more work from one check-in
     * ([PhysiqueCoach.MAX_GROUPS]); no session grows above 10 exercises ([PhysiqueCoach.MAX_EXERCISES]); an exercise
     * is stuck when it has not moved for 21 days and was done at least twice in that time ([PhysiqueCoach.STUCK_DAYS]);
     * a swap is not suggested again for the same exercise within 28 days ([PhysiqueCoach.SWAP_COOLDOWN_DAYS]); up to 3
     * alternatives are offered ([PhysiqueCoach.ALTERNATIVES]). No allowed source gives a ceiling on a whole week's
     * sets; the limits of 3 body parts a check-in and 10 exercises a session stand in for one.
     */
    val WEEKLY_CEILING = Source(
        title = "A Systematic Review of The Effects of Different Resistance Training Volumes on Muscle Hypertrophy (J Hum Kinet 2022; 81: 199-210)",
        publisher = "PubMed Central (peer-reviewed)",
        url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC8884877/",
        accessed = "2026-11-14",
    )

    /**
     * The pain skip card's last line, [SkipCoach.PAIN_LINE] = "If the pain persists or gets worse, see a physician.",
     * and its hold: the exercise's weight does not go up until it is done again ([SkipCoach.held]).
     *
     * What the page says: ExRx.net's "Exercise Adaptation / Injury Prevention Tidbits" advises seeing a physician when
     * pain persists or worsens (a physical therapist can refer the lifter on), and progressing again only once the load
     * can be done pain-free. The card says no more than that: it diagnoses nothing, and the hold makes no increase
     * until the lifter has done the exercise again.
     *
     * Found on 2026-11-18 by a web search restricted to the allowed sources; the page itself was not opened.
     *
     * Not sourced, the designer's own product rules (no training numbers), approved with the plan on 2026-11-18: skips
     * are counted over 28 days ([SkipCoach.WINDOW_DAYS]); the 2nd busy skip ([SkipCoach.BUSY_SKIPS]), the 1st pain skip
     * and the 3rd skip of any reason but time ([SkipCoach.REPEAT_SKIPS]) give a card; no second card for an exercise
     * within 7 days ([SkipCoach.CARD_COOLDOWN_DAYS]); a dismissed card is remembered for 28 days
     * ([SkipCoach.DISMISSED_MEMORY_DAYS]); up to 3 alternatives ([SkipCoach.ALTERNATIVES]).
     */
    val SKIP_PAIN_PHYSICIAN = Source(
        title = "Exercise Adaptation / Injury Prevention Tidbits",
        publisher = "ExRx.net",
        url = "https://exrx.net/Injury/InjuryTidbits",
        accessed = "2026-11-18",
    )

    /**
     * The pain skip card's offer of a gentler alternative for the same muscles: training goes on around the painful
     * area rather than stopping.
     *
     * What the page says: ACSM's "Resting After an Injury: A Thing of the Past" supports staying active and training
     * around an injured or painful area rather than complete rest.
     *
     * Found on 2026-11-18 by a web search restricted to the allowed sources; the page itself was not opened.
     */
    val SKIP_PAIN_TRAIN_AROUND = Source(
        title = "Resting After an Injury: A Thing of the Past",
        publisher = "American College of Sports Medicine (ACSM)",
        url = "https://acsm.org/resting-after-an-injury-a-thing-of-the-past/",
        accessed = "2026-11-18",
    )

    // ---- plan templates (1.14): `plan/PlanTemplates.kt` ----

    /**
     * The templates' days a week and their "who it suits" lines: 3 days for newer lifters, 4 for lifters with some
     * training behind them, 5 for experienced lifters with a year or more of training.
     *
     * What the page says: NSCA's "Determination of Resistance Training Frequency" gives beginners 2-3 sessions a week
     * (whole body, on non-consecutive days), intermediates 3-4 (split routines) and advanced lifters 4-6; an NSCA
     * classification table gives advanced as a year or more of training. ExRx.net's Weight Training Guidelines
     * (https://exrx.net/WeightTraining/Guidelines, the same page as [BUILDER_ORDER]) summarise the ACSM 2009
     * progression models as novice 2-3 days, intermediate 3-4 and advanced 4-5, and say beginners train the whole body.
     * 3, 4 and 5 days sit inside every one of these bands. NSCA sources disagree on how many months make a beginner or
     * an intermediate, so the lines name no months; all agree on "a year or more" for advanced.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_DAYS = Source(
        title = "Determination of Resistance Training Frequency",
        publisher = "National Strength and Conditioning Association (NSCA)",
        url = "https://www.nsca.com/education/articles/kinetic-select/determination-of-resistance-training-frequency/",
        accessed = "2026-11-18",
    )

    /**
     * The powerlifting templates' main lifts: heavy sets of few reps, 2-3 sets an exercise on the 3-day template, each
     * competition lift on at least 2 sessions a week.
     *
     * What the page says: ExRx.net's summary of the 2026 ACSM position stand: for strength, lift heavier loads (at
     * least 80% of 1RM) for 2-3 sets per exercise, at least 2 sessions a week, upper limit undetermined. The same page
     * summarises ACSM 2009: intermediate and advanced lifters train 1-12 RM periodised, eventually emphasising heavier
     * loads (1-6 RM); advanced lifters 3-6 sets per exercise at 70-100% of 1RM, which the 4-day (4 x 4) and 5-day
     * (5 x 3) templates' sets rest on. ACSM states "at least 2 sessions a week" for strength training and muscle
     * groups; applying it to each lift is the designer's reading.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_STRENGTH_LOAD = Source(
        title = "Weight Training Guidelines (summarising the ACSM 2026 and 2009 position stands)",
        publisher = "ExRx.net",
        url = "https://exrx.net/WeightTraining/Guidelines",
        accessed = "2026-11-18",
    )

    /**
     * The main lifts' sets and reps: 3 x 5, 4 x 4 and 5 x 3.
     *
     * What the page says: NSCA's Kinetic Select "Preparatory Period" (an excerpt from Essentials of Strength Training
     * and Conditioning) gives a basic strength phase of 80-95% of 1RM for 2-6 sets of 2-6 reps.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_STRENGTH_PHASE = Source(
        title = "Preparatory Period",
        publisher = "National Strength and Conditioning Association (NSCA)",
        url = "https://www.nsca.com/education/articles/kinetic-select/preparatory-period/",
        accessed = "2026-11-18",
    )

    /**
     * The NSCA training-goal table, for the main lifts' reps and the range around the accessories' count.
     *
     * What the page says: ACE's "How to Select the Right Intensity and Repetitions for Your Clients" gives the NSCA
     * Essentials table: maximum strength 6 reps or fewer at 85% of 1RM or more; hypertrophy 6-12 reps at 67-85% of
     * 1RM. The powerlifting accessories' 10 reps sit inside 6-12.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_NSCA_TABLE = Source(
        title = "How to Select the Right Intensity and Repetitions for Your Clients",
        publisher = "American Council on Exercise (ACE)",
        url = "https://www.acefitness.org/resources/pros/expert-articles/4922/how-to-select-the-right-intensity-and-repetitions-for-your-clients/",
        accessed = "2026-11-18",
    )

    /**
     * The floor every powerlifting template sits at or above: 3-6 working sets a week of 1-5 reps per lift, over 1-3
     * sessions, above 80% of 1RM, at RPE 7.5-9.5. A floor only, not a target: the templates give 6, 8 and 10 sets a
     * week of 5, 4 and 3 reps, on 2 sessions, at RPE 8.
     *
     * What the page says: "The Minimum Effective Training Dose Required for 1RM Strength in Powerlifters" suggests
     * about 3-6 weekly working sets of 1-5 reps across 1-3 sessions per lift, at loads above 80% of 1RM and an RPE of
     * 7.5-9.5. The search returned the article on www.ncbi.nlm.nih.gov/pmc/; it is cited here on the allowed
     * pmc.ncbi.nlm.nih.gov host, the same article.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_STRENGTH_FLOOR = Source(
        title = "The Minimum Effective Training Dose Required for 1RM Strength in Powerlifters",
        publisher = "PubMed Central (peer-reviewed)",
        url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC8435792/",
        accessed = "2026-11-18",
    )

    /**
     * The reps-to-load step behind the templates' approximate %1RM (notes only, never shown): n reps at RPE 8 is about
     * an (n + 2)-rep max, since RPE 8 is 2 reps in reserve (PMC7706636, as `RpeGuide`).
     *
     * What the page says: ExRx.net's "Predicting One-rep Max" (Baechle column): 5 reps = 87%, 6 = 85%, 8 = 80% of 1RM;
     * the same textbook table is quoted in PMC10933212. NSCA's Training Load Chart (nsca.com PDF) gives 8 reps = 80%
     * in the rows the search returned. Joining the two steps is an inference, not a sourced value.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_LOAD_CHART = Source(
        title = "Predicting One-rep Max",
        publisher = "ExRx.net",
        url = "https://exrx.net/Calculators/OneRepMax",
        accessed = "2026-11-18",
    )

    /**
     * The ranges around the powerlifting accessories' count. The count itself, 3 x 10 at RPE 8
     * ([com.powertracker.domain.plan.PlanTemplates.ACCESSORY_SETS], [com.powertracker.domain.plan.PlanTemplates.ACCESSORY_REPS]),
     * is the designer's fixed number, not a sourced value: no allowed source gives a fixed accessory rep count for
     * powerlifting, and the owner settled 3 x 10 at RPE 8 on 2026-11-18. No source was found for the accessory set
     * count or RPE either.
     *
     * What the page says: ExRx.net's "Sample Powerlifting Program" (variable reps) gives auxiliary exercises 3 x 8-10
     * in the off-season. With the NSCA hypertrophy band of 6-12 ([TEMPLATE_NSCA_TABLE]), 10 sits inside both ranges.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_ACCESSORY_RANGE = Source(
        title = "Sample Powerlifting Program (variable reps)",
        publisher = "ExRx.net",
        url = "https://exrx.net/WeightTraining/Powerlifting/VariableReps",
        accessed = "2026-11-18",
    )

    /**
     * The 5-day bodybuilding split for experienced lifters: 4-6 days a week, 1-3 muscle groups a session.
     *
     * What the page says: ACSM's simplified summary of its 2009 progression models (a Chinese-language PDF on
     * acsm.org, read only through the search summary and not checked against the English original) gives advanced
     * lifters 4-6 days a week, training 1-3 muscle groups each session.
     *
     * What the template does: 3 main groups on every day, except that both Legs days end with a Cable crunch, the
     * owner's addition of 2026-11-18, which makes them 4 main groups (abs the fourth). Open for the owner.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_SPLIT_ADVANCED = Source(
        title = "Progression Models in Resistance Training for Healthy Adults (simplified summary)",
        publisher = "American College of Sports Medicine (ACSM)",
        url = "https://acsm.org/wp-content/uploads/2025/01/Progression-Models-in-Resistance-Training-for-Healthy-Adults-Simplified.pdf",
        accessed = "2026-11-18",
    )

    /**
     * The 5-day split's shape, beside [TEMPLATE_SPLIT_ADVANCED].
     *
     * What the page says: a narrative review of bodybuilding training (PMC7698840) reports that elite bodybuilders
     * used a 5-day split routine, no more than 2 muscle groups a session (a 2013 survey), with the major groups trained
     * at least twice a week. The search summary gave no exact title, so the title below describes the article.
     *
     * Checked on 2026-11-18 from the search summary (a web search restricted to the allowed sources); the page itself
     * was not opened.
     */
    val TEMPLATE_SPLIT_BODYBUILDERS = Source(
        title = "Bodybuilding training practices (narrative review)",
        publisher = "PubMed Central (peer-reviewed)",
        url = "https://pmc.ncbi.nlm.nih.gov/articles/PMC7698840/",
        accessed = "2026-11-18",
    )
}
