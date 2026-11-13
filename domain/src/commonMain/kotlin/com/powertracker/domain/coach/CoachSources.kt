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
     */
    val BODYBUILDING_INCREASE = Source(
        title = "Weight Training Guidelines (summarising the ACSM position stands on progression models in resistance training)",
        publisher = "ExRx.net",
        url = "https://exrx.net/WeightTraining/Guidelines",
        accessed = "2026-11-13",
    )
}
