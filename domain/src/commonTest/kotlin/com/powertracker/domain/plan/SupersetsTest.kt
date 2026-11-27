package com.powertracker.domain.plan

import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.SupersetHalf.FIRST
import com.powertracker.domain.plan.SupersetHalf.SECOND
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupersetsTest {
    /** An item on Wednesday with [id], at [order], of exercise [exercise] (its id times ten when not given). */
    private fun item(id: Long, order: Int, linked: Boolean = false, exercise: Long = id * 10, day: DayOfWeek = DayOfWeek.WEDNESDAY, mode: TrainingMode = TrainingMode.POWERLIFTING) =
        PlanItem(id, day, exercise, 3, 8, 8.0, order, mode, supersetWithAbove = linked)

    private fun ids(pairs: List<SupersetPair>) = pairs.map { it.first.id to it.second.id }

    // ---- pairs ----

    @Test
    fun pairs_aFlaggedItemPairsWithTheOneAboveIt_inThePlansOrder() {
        // Given out of order: the plan's order is by place in the day.
        val plan = listOf(item(3, 2), item(2, 1, linked = true), item(1, 0), item(4, 3, linked = true))
        assertEquals(listOf(1L to 2L, 3L to 4L), ids(Supersets.pairs(plan)))
        assertEquals(emptyList(), Supersets.pairs(listOf(item(1, 0), item(2, 1))))
    }

    @Test
    fun pairs_twoItemsGivenOnePlace_goById() {
        // Android's move swaps two places; items can share one. The lower id is above.
        val plan = listOf(item(7, 0, linked = true), item(5, 0))
        assertEquals(listOf(5L to 7L), ids(Supersets.pairs(plan)))
    }

    @Test
    fun pairs_theFirstOfTheDay_hasNoneAbove_andDaysAndSidesDoNotMix() {
        assertEquals(emptyList(), Supersets.pairs(listOf(item(1, 0, linked = true), item(2, 1))))
        // The last of Monday is not above the first of Wednesday; nor the powerlifting item above the bodybuilding one.
        val days = listOf(item(1, 5, day = DayOfWeek.MONDAY), item(2, 0, linked = true))
        assertEquals(emptyList(), Supersets.pairs(days))
        val sides = listOf(item(1, 0), item(2, 1, linked = true, mode = TrainingMode.BODYBUILDING))
        assertEquals(emptyList(), Supersets.pairs(sides))
    }

    @Test
    fun pairs_onlyPairs_ofThreeFlaggedInARowTheThirdIsAlone_andTheFourthPairsAgain() {
        val plan = listOf(item(1, 0), item(2, 1, linked = true), item(3, 2, linked = true), item(4, 3, linked = true))
        assertEquals(listOf(1L to 2L, 3L to 4L), ids(Supersets.pairs(plan)))
        assertEquals(listOf(1L to 2L), ids(Supersets.pairs(plan.take(3))))
    }

    @Test
    fun pairs_oneExerciseTwice_isNoPair() {
        val plan = listOf(item(1, 0, exercise = 50), item(2, 1, linked = true, exercise = 50))
        assertEquals(emptyList(), Supersets.pairs(plan))
    }

    // ---- canLink ----

    @Test
    fun canLink_needsOneAbove_aDifferentExercise_andNeitherInAPair() {
        val plan = listOf(item(1, 0), item(2, 1), item(3, 2), item(4, 3, exercise = 30), item(5, 0, day = DayOfWeek.FRIDAY))
        assertFalse(Supersets.canLink(plan, 1), "first on its day")
        assertTrue(Supersets.canLink(plan, 2))
        assertTrue(Supersets.canLink(plan, 3))
        assertFalse(Supersets.canLink(plan, 4), "the same exercise as the one above")
        assertFalse(Supersets.canLink(plan, 5), "first on Friday")
        assertFalse(Supersets.canLink(plan, 99), "no such item")

        val paired = listOf(item(1, 0), item(2, 1, linked = true), item(3, 2), item(4, 3))
        assertFalse(Supersets.canLink(paired, 2), "linked already")
        assertFalse(Supersets.canLink(paired, 3), "the one above is a pair's second")
        assertTrue(Supersets.canLink(paired, 4))
        // The one below is linked to this one: it would be a second and a first at once.
        val below = listOf(item(1, 0), item(2, 1), item(3, 2, linked = true))
        assertFalse(Supersets.canLink(below, 2))
    }

    // ---- stale ----

    @Test
    fun stale_isEveryFlagThatCountsForNothing() {
        val plan = listOf(
            item(1, 0, linked = true), // first on its day
            item(2, 1, linked = true), // pairs with 1
            item(3, 2, linked = true), // the one above is a second
            item(4, 3, exercise = 70),
            item(5, 4, linked = true, exercise = 70), // the same exercise
        )
        assertEquals(listOf(1L to 2L), ids(Supersets.pairs(plan)))
        assertEquals(listOf(1L, 3L, 5L), Supersets.stale(plan))
        assertEquals(emptyList(), Supersets.stale(listOf(item(1, 0), item(2, 1, linked = true))))
    }

    // ---- broken: an item whose item above changed loses its link ----

    private val before = listOf(item(1, 0), item(2, 1, linked = true), item(3, 2), item(4, 3, linked = true))

    @Test
    fun broken_nothingChanged_orOnlyTheItemsOwnNumbers_keepsEveryLink() {
        assertEquals(emptyList(), Supersets.broken(before, before))
        val edited = before.map { if (it.id == 2L) it.copy(sets = 5, exerciseId = 999) else if (it.id == 1L) it.copy(reps = 3) else it }
        assertEquals(emptyList(), Supersets.broken(before, edited))
        // A whole day renumbered in the same order (the web's move renumbers) changes nothing either.
        assertEquals(emptyList(), Supersets.broken(before, before.map { it.copy(order = it.order * 10 + 5) }))
    }

    @Test
    fun broken_theOneAboveDeleted_unlinksTheOneUnderIt() {
        assertEquals(listOf(2L), Supersets.broken(before, before.filter { it.id != 1L }))
        // The second deleted: nothing is left to unlink in that pair, and the pair below still stands.
        assertEquals(emptyList(), Supersets.broken(before, before.filter { it.id != 2L }))
        // Item 3 deleted: 4 is now under 2.
        assertEquals(listOf(4L), Supersets.broken(before, before.filter { it.id != 3L }))
    }

    @Test
    fun broken_aMoveThatChangesWhatIsAbove_unlinks() {
        fun order(vararg ids: Long) = ids.mapIndexed { i, id -> before.first { it.id == id }.copy(order = i) }
        // The first two swapped: 2 is now first on the day.
        assertEquals(listOf(2L), Supersets.broken(before, order(2, 1, 3, 4)))
        // 3 moved up between the first pair: 2 is now under 3, and 4 under 2.
        assertEquals(listOf(2L, 4L), Supersets.broken(before, order(1, 3, 2, 4)))
        // The second pair moved up as it stands... one step at a time, 3 over 2: 4 is no longer under 3.
        assertEquals(listOf(4L), Supersets.broken(before, order(1, 2, 4, 3)))
    }

    @Test
    fun broken_aNewItem_neverCarriesALink_andTheCoachsSwapUnlinksBothWays() {
        // The coach swaps an exercise by deleting its item and adding one in its place, with a new id.
        val firstSwapped = before.map { if (it.id == 1L) it.copy(id = 0, exerciseId = 77) else it }
        assertEquals(listOf(2L), Supersets.broken(before, firstSwapped))
        val secondSwapped = before.map { if (it.id == 2L) it.copy(id = 50, exerciseId = 77) else it }
        assertEquals(listOf(50L), Supersets.broken(before, secondSwapped))
        // An item added at the end with a flag it should not have.
        assertEquals(listOf(9L), Supersets.broken(before, before + item(9, 9, linked = true)))
    }

    @Test
    fun broken_aPlanPutBackWhole_keepsItsLinks() {
        // Undo of a plan replace: the same ids in the same places. Before it, the side held another plan.
        val other = listOf(item(20, 0), item(21, 1))
        assertEquals(listOf(2L, 4L), Supersets.broken(other, before), "judged against another plan, every link is new")
        assertEquals(emptyList(), Supersets.broken(before, before), "judged against itself, none is")
    }

    // ---- due ----

    @Test
    fun due_theFirstLeads_theSecondAnswers_setForSet() {
        assertEquals(FIRST, Supersets.due(0, 3, 0, 3))
        assertEquals(SECOND, Supersets.due(1, 3, 0, 3))
        assertEquals(FIRST, Supersets.due(1, 3, 1, 3))
        assertEquals(SECOND, Supersets.due(3, 3, 2, 3))
        assertNull(Supersets.due(3, 3, 3, 3))
    }

    @Test
    fun due_unevenSets_theLongerOneCarriesOnAlone() {
        // 4 and 2: after two rounds the second is done, the first has two sets left.
        assertEquals(FIRST, Supersets.due(2, 4, 2, 2))
        assertEquals(FIRST, Supersets.due(3, 4, 2, 2))
        assertNull(Supersets.due(4, 4, 2, 2))
        // 2 and 4.
        assertEquals(SECOND, Supersets.due(2, 2, 2, 4))
        assertEquals(SECOND, Supersets.due(2, 2, 3, 4))
    }

    @Test
    fun due_outOfOrder_orPastTheTarget_stillAnswers() {
        // The second logged first: the first is due until it has caught up and led again.
        assertEquals(FIRST, Supersets.due(0, 3, 1, 3))
        assertEquals(FIRST, Supersets.due(0, 3, 2, 3))
        assertEquals(FIRST, Supersets.due(2, 3, 2, 3))
        // "Add another set" past the plan's sets: done is done.
        assertNull(Supersets.due(4, 3, 3, 3))
        assertEquals(SECOND, Supersets.due(5, 3, 1, 3))
    }

    // ---- afterSet ----

    @Test
    fun afterSet_theFirst_opensTheSecond_withNoRest() {
        assertEquals(SupersetStep(SECOND, rest = false), Supersets.afterSet(FIRST, 1, 3, 0, 3))
        assertEquals(SupersetStep(SECOND, rest = false), Supersets.afterSet(FIRST, 3, 3, 2, 3))
    }

    @Test
    fun afterSet_theSecond_startsTheRest_andOpensTheFirst() {
        assertEquals(SupersetStep(FIRST, rest = true), Supersets.afterSet(SECOND, 1, 3, 1, 3))
        // The last set of both: the rest, and the card stays (its own button leads on).
        assertEquals(SupersetStep(null, rest = true), Supersets.afterSet(SECOND, 3, 3, 3, 3))
    }

    @Test
    fun afterSet_onceOneIsDone_theOtherRestsAfterEverySet_onItsOwnCard() {
        // The second has its two sets; the first's third and fourth are alone.
        assertEquals(SupersetStep(null, rest = true), Supersets.afterSet(FIRST, 3, 4, 2, 2))
        assertEquals(SupersetStep(null, rest = true), Supersets.afterSet(FIRST, 4, 4, 2, 2))
        // The first has its two; the second's third is alone.
        assertEquals(SupersetStep(null, rest = true), Supersets.afterSet(SECOND, 2, 2, 3, 4))
    }

    @Test
    fun afterSet_theSecondLoggedOutOfTurn_restsAndGoesBackToTheFirst() {
        assertEquals(SupersetStep(FIRST, rest = true), Supersets.afterSet(SECOND, 0, 3, 1, 3))
    }

    @Test
    fun afterSet_keepCard_leavesTheCardOpen_theRestDecidedAsBefore() {
        // A missed first set on the powerlifting side: the card stays to ask where it stalled; still no rest.
        assertEquals(SupersetStep(null, rest = false), Supersets.afterSet(FIRST, 1, 3, 0, 3, keepCard = true))
        assertEquals(SupersetStep(null, rest = true), Supersets.afterSet(SECOND, 1, 3, 1, 3, keepCard = true))
    }
}
