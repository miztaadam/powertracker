package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 1.13: skips are stored one per exercise, date and side, with their cards, and go or stay as their rows are deleted. */
@RunWith(RobolectricTestRunner::class)
class SkipStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository

    private val today = LocalDate(2026, 11, 18)
    private val yesterday = LocalDate(2026, 11, 17)
    private val bb = TrainingMode.BODYBUILDING
    private val pl = TrainingMode.POWERLIFTING

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun id(name: String) = training.exercises().first().first { it.name == name }.id

    private fun skipOf(exerciseId: Long, reason: SkipReason?, mode: TrainingMode = bb, date: LocalDate = today, planItemId: Long? = null) =
        Skip(0, date, mode, exerciseId, planItemId, reason)

    private suspend fun planItem(exerciseId: Long, mode: TrainingMode = bb) =
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, exerciseId, 3, 8, 8.0, 0, mode, if (mode == bb) 12 else null))

    private fun card(stored: Skip, alternatives: List<String> = listOf("Cable curl")) = Advice(
        AdviceType.SKIP, null, stored.date, "You skipped it.", 3,
        AdvicePayload.Skipped(stored.id, stored.exerciseId, "Curl", stored.reason, alternatives), mode = stored.mode,
    )

    private fun target(exerciseId: Long, mode: TrainingMode, createdAt: LocalDate = today, status: AdviceStatus = AdviceStatus.OPEN) = Advice(
        AdviceType.TARGET, null, createdAt, "Last time", 0, AdvicePayload.Target(exerciseId, 20.0, 8, 8.0), status, mode = mode,
    )

    private suspend fun noCards(@Suppress("UNUSED_PARAMETER") stored: Skip, @Suppress("UNUSED_PARAMETER") all: List<Skip>) = emptyList<Advice>()

    @Test
    fun skip_isStoredAndReadBack() = runTest {
        val curl = id("Curl")
        val item = planItem(curl)

        val skipId = training.skip(skipOf(curl, SkipReason.BUSY, planItemId = item), ::noCards)

        assertTrue(skipId > 0)
        assertEquals(listOf(Skip(skipId, today, bb, curl, item, SkipReason.BUSY)), training.skips().first())
    }

    @Test
    fun skipAgain_onTheSameKey_replacesTheReason_andKeepsTheId() = runTest {
        val curl = id("Curl")
        val first = training.skip(skipOf(curl, SkipReason.BUSY), ::noCards)

        val second = training.skip(skipOf(curl, null), ::noCards)

        assertEquals(first, second)
        assertEquals(listOf(Skip(first, today, bb, curl, null, null)), training.skips().first())
    }

    @Test
    fun skips_onAnotherSideOrDate_areSeparate() = runTest {
        val curl = id("Curl")
        training.skip(skipOf(curl, SkipReason.BUSY), ::noCards)
        training.skip(skipOf(curl, SkipReason.BUSY, mode = pl), ::noCards)
        training.skip(skipOf(curl, SkipReason.TIME, date = yesterday), ::noCards)

        assertEquals(3, training.skips().first().size)
        assertEquals(3, training.skips().first().map { it.id }.toSet().size)
    }

    @Test
    fun skip_givesCardsTheStoredSkipAndEveryStoredSkip_andStoresWhatTheyReturn() = runTest {
        val curl = id("Curl")
        val row = id("Row")
        training.skip(skipOf(row, SkipReason.TIME, mode = pl), ::noCards)
        var seen: Pair<Skip, List<Skip>>? = null

        val skipId = training.skip(skipOf(curl, SkipReason.PAIN)) { stored, all ->
            seen = stored to all
            listOf(card(stored))
        }

        val (stored, all) = seen!!
        assertEquals(Skip(skipId, today, bb, curl, null, SkipReason.PAIN), stored)
        assertEquals(training.skips().first().toSet(), all.toSet())
        assertEquals(2, all.size)
        val cards = advice.all().filter { it.type == AdviceType.SKIP }
        assertEquals(listOf(skipId), cards.map { (it.payload as AdvicePayload.Skipped).skipId })
        assertEquals(bb, cards.single().mode)
    }

    @Test
    fun skip_whenCardsFail_storesNothing() = runTest {
        val curl = id("Curl")
        advice.insert(listOf(target(curl, bb)))

        val failed = runCatching { training.skip(skipOf(curl, SkipReason.PAIN)) { _, _ -> error("coach failed") } }

        assertTrue(failed.isFailure)
        assertEquals(emptyList<Skip>(), training.skips().first())
        assertEquals(listOf(AdviceType.TARGET), advice.all().map { it.type })
    }

    /**
     * Task 1's finding: today's stored target was worked out without the hold, so a pain skip deletes it, on both sides,
     * since the hold crosses sides (coordinator's ruling).
     */
    @Test
    fun painSkip_deletesTodaysOpenTarget_forThatExerciseOnBothSides() = runTest {
        val curl = id("Curl")
        val row = id("Row")
        advice.insert(
            listOf(
                target(curl, bb), // goes
                target(curl, pl), // the other side: goes too
                target(row, bb), // another exercise: stays
                target(curl, bb, createdAt = yesterday), // yesterday's: stays
                target(curl, bb, status = AdviceStatus.APPLIED), // already applied: stays
            ),
        )
        var advicePassed: List<Advice>? = null

        training.skip(skipOf(curl, SkipReason.PAIN)) { _, _ -> advicePassed = advice.all(); emptyList() }

        val left = advice.all().map { (it.payload as AdvicePayload.Target).exerciseId to (it.mode to it.status) }
        assertEquals(listOf(row to (bb to AdviceStatus.OPEN), curl to (bb to AdviceStatus.OPEN), curl to (bb to AdviceStatus.APPLIED)), left)
        // The targets are gone before the coach looks.
        assertEquals(3, advicePassed!!.size)
        assertEquals(yesterday, advice.all()[1].createdAt)
    }

    @Test
    fun skipForAnotherReason_keepsTodaysTarget() = runTest {
        val curl = id("Curl")
        advice.insert(listOf(target(curl, bb)))

        for (reason in listOf(SkipReason.BUSY, SkipReason.TIME, null)) training.skip(skipOf(curl, reason), ::noCards)

        assertEquals(1, advice.all().size)
    }

    /** Exercise ids are matched exactly: a target for exercise 12 is not taken for one for exercise 1. */
    @Test
    fun painSkip_matchesTheExerciseIdExactly() = runTest {
        val all = training.exercises().first()
        val one = all.first { it.id == 1L }.id
        val twelve = all.first { it.id == 12L }.id
        advice.insert(listOf(target(twelve, bb)))

        training.skip(skipOf(one, SkipReason.PAIN), ::noCards)

        assertEquals(1, advice.all().size)
    }

    @Test
    fun unskip_deletesTheSkipAndItsOpenCard_keepsAppliedAndDismissedCards_andOtherSkipsCards() = runTest {
        val curl = id("Curl")
        val row = id("Row")
        val skipId = training.skip(skipOf(curl, SkipReason.PAIN)) { stored, _ -> listOf(card(stored)) }
        val other = training.skip(skipOf(row, SkipReason.PAIN)) { stored, _ -> listOf(card(stored)) }
        // Two cards from the same skip that were already answered: Undo leaves them.
        val earlier = Skip(skipId, yesterday, bb, curl, null, SkipReason.BUSY)
        advice.insert(
            listOf(
                card(earlier).copy(status = AdviceStatus.APPLIED),
                card(earlier).copy(status = AdviceStatus.DISMISSED),
            ),
        )

        training.unskip(skipId)

        assertEquals(listOf(other), training.skips().first().map { it.id })
        val cards = advice.all().map { (it.payload as AdvicePayload.Skipped).skipId to it.status }
        assertEquals(listOf(other to AdviceStatus.OPEN, skipId to AdviceStatus.APPLIED, skipId to AdviceStatus.DISMISSED), cards)
    }

    /** Task 1's finding: today's target may have been worked out with the hold, so taking the skip back deletes it. */
    @Test
    fun unskip_deletesTodaysOpenTarget_forThatExerciseOnBothSides() = runTest {
        val curl = id("Curl")
        val row = id("Row")
        val skipId = training.skip(skipOf(curl, SkipReason.BUSY), ::noCards)
        advice.insert(listOf(target(curl, bb), target(curl, pl), target(row, bb), target(curl, bb, status = AdviceStatus.DISMISSED)))

        training.unskip(skipId)

        val left = advice.all().map { (it.payload as AdvicePayload.Target).exerciseId to (it.mode to it.status) }
        assertEquals(listOf(row to (bb to AdviceStatus.OPEN), curl to (bb to AdviceStatus.DISMISSED)), left)
    }

    /** A re-skip with a new reason replaces the skip's open card rather than stacking a second one (coordinator's ruling). */
    @Test
    fun reSkip_withANewReason_replacesItsOpenCard() = runTest {
        val curl = id("Curl")
        val row = id("Row")
        val skipId = training.skip(skipOf(curl, SkipReason.PAIN)) { stored, _ -> listOf(card(stored, listOf("Cable curl"))) }
        val other = training.skip(skipOf(row, SkipReason.PAIN)) { stored, _ -> listOf(card(stored)) }
        // An answered card from the same skip stays.
        advice.insert(listOf(card(Skip(skipId, today, bb, curl, null, SkipReason.PAIN)).copy(status = AdviceStatus.DISMISSED)))
        var priorSeen: List<Advice>? = null

        val again = training.skip(skipOf(curl, SkipReason.BUSY)) { stored, _ ->
            priorSeen = advice.all()
            listOf(card(stored, listOf("EZ-bar curl")))
        }

        assertEquals(skipId, again)
        val cards = advice.all().map { it.payload as AdvicePayload.Skipped }.map { Triple(it.skipId, it.reason, it.alternatives) }
        assertEquals(
            listOf(
                Triple(other, SkipReason.PAIN, listOf("Cable curl")),
                Triple(skipId, SkipReason.PAIN, listOf("Cable curl")),
                Triple(skipId, SkipReason.BUSY, listOf("EZ-bar curl")),
            ),
            cards,
        )
        assertEquals(listOf(AdviceStatus.OPEN, AdviceStatus.DISMISSED, AdviceStatus.OPEN), advice.all().map { it.status })
        // The old open card is gone before the coach looks, so it does not block the new one.
        assertEquals(2, priorSeen!!.size)
    }

    /** A re-skip to "Out of time", for which the coach gives no card, leaves no card. */
    @Test
    fun reSkip_toOutOfTime_leavesNoCard() = runTest {
        val curl = id("Curl")
        training.skip(skipOf(curl, SkipReason.PAIN)) { stored, _ -> listOf(card(stored)) }

        training.skip(skipOf(curl, SkipReason.TIME), ::noCards)

        assertEquals(emptyList<Advice>(), advice.all())
        assertEquals(SkipReason.TIME, training.skips().first().single().reason)
    }

    @Test
    fun unskip_ofAnUnknownId_changesNothing() = runTest {
        val curl = id("Curl")
        training.skip(skipOf(curl, SkipReason.PAIN)) { stored, _ -> listOf(card(stored)) }
        advice.insert(listOf(target(curl, bb)))

        training.unskip(9_999)

        assertEquals(1, training.skips().first().size)
        assertEquals(2, advice.all().size)
    }

    @Test
    fun deletingAnExercise_deletesItsSkips() = runTest {
        val own = training.addExercise(Exercise(0, "Zercher curl", Category.ACCESSORY, null, listOf("Biceps"), false))
        val curl = id("Curl")
        training.skip(skipOf(own, SkipReason.BUSY), ::noCards)
        training.skip(skipOf(own, SkipReason.BUSY, mode = pl), ::noCards)
        training.skip(skipOf(curl, SkipReason.BUSY), ::noCards)

        training.deleteExercise(own)

        assertEquals(listOf(curl), training.skips().first().map { it.exerciseId })
    }

    @Test
    fun deletingAPlanItem_keepsItsSkips_withNoPlanItem() = runTest {
        val curl = id("Curl")
        val item = planItem(curl)
        val kept = planItem(curl, mode = pl)
        training.skip(skipOf(curl, SkipReason.BUSY, planItemId = item), ::noCards)
        training.skip(skipOf(curl, SkipReason.BUSY, mode = pl, planItemId = kept), ::noCards)

        training.deletePlanItem(item)

        val skips = training.skips().first().associateBy { it.mode }
        assertNull(skips.getValue(bb).planItemId)
        assertEquals(kept, skips.getValue(pl).planItemId)
    }

    @Test
    fun replacingASidesPlan_keepsItsSkips_withNoPlanItem() = runTest {
        val curl = id("Curl")
        val item = planItem(curl)
        training.skip(skipOf(curl, SkipReason.BUSY, planItemId = item), ::noCards)

        training.replacePlan(bb, emptyList())

        assertNull(training.skips().first().single().planItemId)
    }

    /** Editing a plan item (same id) is not deleting it: its skips keep their plan item. */
    @Test
    fun editingAPlanItem_keepsItsSkipsPlanItem() = runTest {
        val curl = id("Curl")
        val item = planItem(curl)
        training.skip(skipOf(curl, SkipReason.BUSY, planItemId = item), ::noCards)

        training.savePlanItem(PlanItem(item, DayOfWeek.WEDNESDAY, curl, 4, 8, 8.0, 0, bb, 12))

        assertEquals(item, training.skips().first().single().planItemId)
    }

    @Test
    fun skips_neverCountAsSets() = runTest {
        val curl = id("Curl")
        training.skip(skipOf(curl, SkipReason.PAIN), ::noCards)

        val history = training.history().first()
        assertTrue(history.sessions.isEmpty())
        assertEquals(emptyList<Any>(), db.sets().all())
    }
}
