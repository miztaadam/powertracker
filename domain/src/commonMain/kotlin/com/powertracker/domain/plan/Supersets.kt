package com.powertracker.domain.plan

import com.powertracker.domain.model.PlanItem

/** Two plan items done as a superset: [first], then [second] (the one carrying the link), then the rest. */
data class SupersetPair(val first: PlanItem, val second: PlanItem)

/** Which exercise of a pair. */
enum class SupersetHalf { FIRST, SECOND }

/** What follows a set logged in a pair: the half whose card opens next (null: the card stays), and whether the rest starts. */
data class SupersetStep(val open: SupersetHalf?, val rest: Boolean)

/**
 * Supersets (1.24): two exercises of one day done back to back, a set of the first, a set of the second, then the rest.
 * The owner's design, 2026-11-26 and 2026-11-27: saved in the plan, linked once on the Plan tab ("Superset with the one
 * above"), pairs only, and when one exercise has more sets than the other it carries on alone.
 *
 * The link is one flag on the second item, [PlanItem.supersetWithAbove]. Everything that reads or keeps it is here, so
 * the two apps cannot pair differently: what "the one above" is, which flags count, which are lost when the plan
 * changes, and on Today whose turn it is.
 */
object Supersets {
    /** A side's plan in its own order: by day, then by the item's place in the day, then by id (two items given one place). */
    fun ordered(plan: List<PlanItem>): List<PlanItem> =
        plan.sortedWith(compareBy<PlanItem> { it.mode }.thenBy { it.weekday }.thenBy { it.order }.thenBy { it.id })

    /** The item directly above each item on its day and side, by id; an item first on its day has none. */
    private fun above(plan: List<PlanItem>): Map<Long, PlanItem> {
        val out = HashMap<Long, PlanItem>()
        var last: PlanItem? = null
        for (item in ordered(plan)) {
            val before = last
            if (before != null && before.weekday == item.weekday && before.mode == item.mode) out[item.id] = before
            last = item
        }
        return out
    }

    /**
     * The pairs in [plan], in the plan's order. A flag counts when the item has one above it on its day and side, that one
     * is not itself the second of a pair (pairs only: of three flagged in a row, the first two pair and the third is
     * alone), and the two are different exercises.
     */
    fun pairs(plan: List<PlanItem>): List<SupersetPair> {
        val above = above(plan)
        val seconds = HashSet<Long>()
        val out = ArrayList<SupersetPair>()
        for (item in ordered(plan)) {
            if (!item.supersetWithAbove) continue
            val first = above[item.id] ?: continue
            if (first.id in seconds || first.exerciseId == item.exerciseId) continue
            seconds += item.id
            out += SupersetPair(first, item)
        }
        return out
    }

    /**
     * Whether the item with [itemId] can be linked to the one above it: it is not linked now, there is one above it, the
     * two are different exercises, and neither is in a pair already (the one above is not a pair's second, and the one
     * below this item is not linked to it).
     */
    fun canLink(plan: List<PlanItem>, itemId: Long): Boolean {
        val item = plan.firstOrNull { it.id == itemId } ?: return false
        if (item.supersetWithAbove) return false
        val above = above(plan)
        val first = above[itemId] ?: return false
        if (first.exerciseId == item.exerciseId) return false
        val paired = pairs(plan).flatMap { listOf(it.first.id, it.second.id) }.toSet()
        return first.id !in paired && item.id !in paired
    }

    /** The items whose flag is set but counts for nothing in [plan]: cleared when a plan is read from a backup. */
    fun stale(plan: List<PlanItem>): List<Long> {
        val seconds = pairs(plan).map { it.second.id }.toSet()
        return plan.filter { it.supersetWithAbove && it.id !in seconds }.map { it.id }
    }

    /**
     * The one rule for every change to a plan: an item whose item above changed loses its link. Given the side's plan
     * [before] and [after] a change, the ids in [after] to unlink: a flagged item that is new, or now first on its day,
     * or under another item than before (the one above was deleted, moved, or swapped by the coach for another exercise,
     * which is a new item; or something was moved in between). An item that only changed its own sets, reps or exercise
     * keeps its link, and so does a pair moved as it stands (a whole plan put back by Undo: the same ids in the same order).
     */
    fun broken(before: List<PlanItem>, after: List<PlanItem>): List<Long> {
        val was = above(before)
        val known = before.map { it.id }.toSet()
        val now = above(after)
        return after.filter { it.supersetWithAbove && (it.id !in known || now[it.id]?.id != was[it.id]?.id || now[it.id] == null) }.map { it.id }
    }

    /**
     * Whose turn it is in a pair on Today, from the sets logged today alone (so it is the same after the app is closed
     * and opened, or a set is deleted): the first exercise leads, the second answers, set for set. With [firstDone] of
     * [firstTarget] sets logged on the first and [secondDone] of [secondTarget] on the second: null when both have
     * their sets; the other one when one has all of its sets; else the first while it has no more sets than the second,
     * and the second once the first is a set ahead.
     */
    fun due(firstDone: Int, firstTarget: Int, secondDone: Int, secondTarget: Int): SupersetHalf? {
        val firstLeft = firstDone < firstTarget
        val secondLeft = secondDone < secondTarget
        return when {
            !firstLeft && !secondLeft -> null
            !secondLeft -> SupersetHalf.FIRST
            !firstLeft -> SupersetHalf.SECOND
            firstDone <= secondDone -> SupersetHalf.FIRST
            else -> SupersetHalf.SECOND
        }
    }

    /**
     * What follows a set just logged on the [logged] half, the counts being those after it. After the first: the second's
     * card opens and no rest starts, while it is the second's turn; else (the second has all its sets) the rest starts
     * and the card stays. After the second: the rest starts, and the first's card opens when it is its turn. [keepCard]
     * (the set was a miss or a grind on the powerlifting side, where the card then asks where it stalled) leaves the card
     * open; the rest is decided the same way.
     */
    fun afterSet(logged: SupersetHalf, firstDone: Int, firstTarget: Int, secondDone: Int, secondTarget: Int, keepCard: Boolean = false): SupersetStep {
        val next = due(firstDone, firstTarget, secondDone, secondTarget)
        val rest = !(logged == SupersetHalf.FIRST && next == SupersetHalf.SECOND)
        val open = next?.takeIf { it != logged && !keepCard }
        return SupersetStep(open, rest)
    }
}
