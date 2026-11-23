package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.CheckInEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.data.db.SkipEntity
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Each side's plan. */
data class Plans(val powerlifting: List<PlanItem>, val bodybuilding: List<PlanItem>) {
    fun of(mode: TrainingMode): List<PlanItem> = if (mode == TrainingMode.BODYBUILDING) bodybuilding else powerlifting
}

/**
 * One side's plan as it was just before a whole-plan replace (Use this plan), for Undo (1.14.1): its [items] with their
 * ids, days, order and numbers; the skips that pointed at them ([skipLinks], skip id to plan item id), which the replace
 * cleared; and the side's training [days]. [replacedBy] is the ids the replace wrote. Undo runs only while [token] is
 * the side's latest snapshot, not yet undone, and the side's plan is still exactly those items: an old snackbar's Undo
 * never puts an older plan over a newer one.
 */
data class PlanSnapshot(
    val mode: TrainingMode,
    val items: List<PlanItem>,
    val skipLinks: Map<Long, Long>,
    val days: Set<DayOfWeek>,
    val replacedBy: Set<Long> = emptySet(),
    val token: Long = 0,
)

/** A logged set together with its row ids, for editing and deleting. */
data class StoredSet(val id: Long, val sessionId: Long, val set: LoggedSet)

class ExerciseInUseException(val exerciseId: Long) :
    IllegalStateException("Exercise $exerciseId has logged sets; archive it instead")

open class TrainingRepository(private val db: AppDatabase) {
    private val catalogue = CatalogueResolver(db)

    /** Each side's latest [PlanSnapshot.token], until it is undone: only the latest replace can be undone (1.14.1). */
    private val undoable = java.util.concurrent.ConcurrentHashMap<TrainingMode, Long>()
    private val tokens = java.util.concurrent.atomic.AtomicLong()

    fun history(): Flow<TrainingHistory> = combine(
        db.sessions().observeAll(),
        db.sets().observeAll(),
        db.exercises().observeAll(),
        db.baselines().observeAll(),
        ::historyOf,
    )

    /** The history as it was before the set [setId] was logged: everything stored, read once, but that set. */
    open suspend fun historyWithout(setId: Long): TrainingHistory =
        historyOf(
            db.sessions().observeAll().first(),
            db.sets().observeAll().first().filter { it.id != setId },
            db.exercises().observeAll().first(),
            db.baselines().observeAll().first(),
        )

    private fun historyOf(sessions: List<SessionEntity>, sets: List<SetEntity>, exercises: List<ExerciseEntity>, baselines: List<BaselineEntity>): TrainingHistory {
        val setsBySession = sets.groupBy { it.sessionId }
        return TrainingHistory(
            sessions = sessions.map { s -> s.toDomain(setsBySession[s.id].orEmpty().map { it.toDomain() }) },
            exercises = exercises.map { it.toDomain() }.associateBy { it.id },
            baselines = baselines.map { Baseline(it.exerciseId, it.e1rmKg, it.date) },
        )
    }

    fun exercises(): Flow<List<Exercise>> = db.exercises().observeAll().map { list -> list.map { it.toDomain() } }

    /** [mode]'s plan only: each side keeps its own (bodybuilding mode spec §6). */
    fun plan(mode: TrainingMode = TrainingMode.POWERLIFTING): Flow<List<PlanItem>> =
        db.plan().observeSide(mode).map { list -> list.map { it.toDomain() } }

    /**
     * Both sides' plans, watched together: a screen that follows the side picks one from each emission, so a switch
     * never opens a new query and the side and its plan always arrive together.
     */
    /** No session and no plan item on either side ([isNewUser]): the guide's first launch. */
    open suspend fun isNewUser(): Boolean = isNewUser(db.sessions(), db.plan())

    fun plans(): Flow<Plans> = combine(plan(TrainingMode.POWERLIFTING), plan(TrainingMode.BODYBUILDING), ::Plans)

    fun setsForSession(sessionId: Long): Flow<List<StoredSet>> =
        db.sets().observeForSession(sessionId).map { list -> list.map { StoredSet(it.id, it.sessionId, it.toDomain()) } }

    fun sessionOn(date: LocalDate): Flow<LoggedSession?> =
        db.sessions().observeOn(date).map { it?.toDomain(emptyList()) }

    /** [mode] is the side active when the session is started. */
    suspend fun startSession(date: LocalDate, bodyweightKg: Double?, readiness: Int?, mode: TrainingMode = TrainingMode.POWERLIFTING): Long =
        db.sessions().insert(SessionEntity(date = date, bodyweightKg = bodyweightKg, readiness = readiness, mode = mode))

    /**
     * Saves [set]. With a [catalogueName] the set is for that catalogue exercise, whatever its
     * `exerciseId` says: the exercise row is found, restored or inserted in the same transaction.
     */
    suspend fun logSet(sessionId: Long, set: LoggedSet, catalogueName: String? = null): Long {
        if (catalogueName == null) return db.sets().insert(set.toEntity(0, sessionId))
        return db.withTransaction {
            db.sets().insert(set.copy(exerciseId = catalogue.resolve(catalogueName)).toEntity(0, sessionId))
        }
    }

    suspend fun updateSet(setId: Long, set: LoggedSet) {
        val existing = db.sets().byId(setId) ?: return
        db.sets().update(set.toEntity(setId, existing.sessionId))
    }

    suspend fun deleteSet(setId: Long) = db.sets().delete(setId)

    /**
     * Inserts a new item (id 0) or replaces the item with the same id. With a [catalogueName] the item
     * is for that catalogue exercise, whatever its `exerciseId` says: the exercise row is found,
     * restored or inserted in the same transaction. The item is stored on its own side ([PlanItem.mode]).
     */
    open suspend fun savePlanItem(item: PlanItem, catalogueName: String? = null): Long {
        fun entity(exerciseId: Long) =
            PlanItemEntity(item.id, item.weekday, exerciseId, item.sets, item.reps, item.targetRpe, item.order, item.mode, item.repsHigh)
        if (catalogueName == null) return db.plan().upsert(entity(item.exerciseId))
        return db.withTransaction { db.plan().upsert(entity(catalogue.resolve(catalogueName))) }
    }

    suspend fun deletePlanItem(id: Long) = db.plan().delete(id)

    /**
     * Replaces [mode]'s whole plan with [items], in one transaction: that side's items are deleted, each item's catalogue
     * name (the second of the pair; null for the lifter's own exercise, which keeps its `exerciseId`) is resolved as
     * [savePlanItem] does, and the items are inserted in the order given, each on [mode]. If anything fails, the old plan
     * and every exercise row are left as they were. The other side's plan is never touched.
     */
    suspend fun replacePlan(mode: TrainingMode, items: List<Pair<PlanItem, String?>>) = db.withTransaction {
        db.plan().clearSide(mode)
        for ((item, catalogueName) in items) {
            val exerciseId = if (catalogueName == null) item.exerciseId else catalogue.resolve(catalogueName)
            db.plan().upsert(PlanItemEntity(0, item.weekday, exerciseId, item.sets, item.reps, item.targetRpe, item.order, mode, item.repsHigh))
        }
    }

    /**
     * [mode]'s plan as stored now, with the skips that point at it and [days], ready for [restorePlan]. Call it inside
     * [inTransaction], just before the replace, and pass it to [saved] once that transaction has committed: only then
     * does it become the side's latest snapshot, so a save that rolls back leaves an earlier snapshot's Undo working.
     */
    suspend fun planSnapshot(mode: TrainingMode, days: Set<DayOfWeek>): PlanSnapshot {
        val items = db.plan().all().filter { it.mode == mode }.map { it.toDomain() }
        val ids = items.mapTo(HashSet()) { it.id }
        val links = db.skips().all().mapNotNull { skip -> skip.planItemId?.takeIf { it in ids }?.let { skip.id to it } }.toMap()
        return PlanSnapshot(mode, items, links, days, token = tokens.incrementAndGet())
    }

    /** The save [snapshot] was taken for has committed: it is now its side's latest, so any earlier one can no longer be undone. */
    fun saved(snapshot: PlanSnapshot) {
        undoable[snapshot.mode] = snapshot.token
    }

    /** [snapshot] was undone (its transaction committed): it, and every older snapshot of its side, can't be undone again. */
    fun undone(snapshot: PlanSnapshot) {
        undoable.remove(snapshot.mode, snapshot.token)
    }

    /** The ids of [mode]'s stored plan items. */
    suspend fun planIds(mode: TrainingMode): Set<Long> = db.plan().all().filter { it.mode == mode }.mapTo(HashSet()) { it.id }

    /**
     * Undo for a whole-plan replace: puts [snapshot]'s items back on [mode], with their ids, and relinks the skips that
     * pointed at them, in one transaction. It runs only while [snapshot] is [mode]'s latest, not [undone], and [mode]'s
     * plan is still exactly [PlanSnapshot.replacedBy] (the replace's items, with any edits); otherwise, or for a snapshot
     * of an empty side or of the other side, it changes nothing and returns false. The caller marks it [undone] once its
     * transaction has committed. Sessions, sets, advice, check-ins and skips are kept: a skip only gets its plan
     * item back, and only if it has none now. The training days are the caller's (see [PlanSnapshot.days]).
     */
    suspend fun restorePlan(mode: TrainingMode, snapshot: PlanSnapshot): Boolean = db.withTransaction {
        if (snapshot.mode != mode || snapshot.items.isEmpty() || undoable[mode] != snapshot.token) return@withTransaction false
        if (planIds(mode) != snapshot.replacedBy) return@withTransaction false
        db.plan().clearSide(mode)
        // A plain insert: an id already taken fails the whole undo rather than overwriting that row.
        db.plan().insertAll(
            snapshot.items.map { PlanItemEntity(it.id, it.weekday, it.exerciseId, it.sets, it.reps, it.targetRpe, it.order, mode, it.repsHigh) },
        )
        for ((skipId, itemId) in snapshot.skipLinks) {
            val skip = db.skips().byId(skipId) ?: continue
            if (skip.planItemId == null) db.skips().update(skip.copy(planItemId = itemId))
        }
        true
    }

    /** Runs [block] in one transaction: the plan items it saves and the catalogue rows it resolves land together or not at all. */
    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)

    /** The row for the catalogue exercise [name], found, restored or inserted (see [CatalogueResolver.resolve]). Call it inside [inTransaction]. */
    suspend fun resolveCatalogue(name: String): Long = catalogue.resolve(name)

    suspend fun addExercise(e: Exercise): Long = db.exercises().insert(
        ExerciseEntity(0, e.name, e.category, e.parentLift, e.muscles, e.isBuiltIn, e.archived, e.plateCount),
    )

    /**
     * Sets the plate setting of an exercise (1.17), in one transaction, and returns the row's id. With a [catalogueName]
     * the setting is for that catalogue exercise, whatever [exerciseId] says: its row is found, restored or inserted
     * first ([CatalogueResolver.resolve]), as logging its first set would. No set, target or other row changes: history
     * is never rewritten.
     */
    suspend fun setPlateCount(exerciseId: Long?, catalogueName: String?, value: PlateCount): Long = db.withTransaction {
        val id = if (catalogueName != null) catalogue.resolve(catalogueName) else requireNotNull(exerciseId) { "An exercise id or a catalogue name" }
        db.exercises().setPlateCount(id, value)
        id
    }

    /** Throws [ExerciseInUseException] if sets reference it; use [archiveExercise] instead. */
    suspend fun deleteExercise(id: Long) = db.withTransaction {
        if (db.exercises().setCount(id) > 0) throw ExerciseInUseException(id)
        db.exercises().delete(id)
    }

    suspend fun archiveExercise(id: Long) = db.exercises().archive(id)

    /** The bodybuilding check-ins, oldest first. */
    fun checkIns(): Flow<List<CheckIn>> = db.checkIns().observeAll().map { list -> list.map { it.toDomain() } }

    /** Saves [checkIn]; one already stored on the same date is replaced. */
    suspend fun saveCheckIn(checkIn: CheckIn): Long = db.checkIns().upsert(checkIn.toEntity())

    /**
     * Carries out a coach's change to the bodybuilding plan in one transaction: [PlanChange.updated] saved,
     * [PlanChange.removed] deleted, and each of [PlanChange.added] inserted on its catalogue name's row (found, restored or
     * inserted as [savePlanItem] does). If anything fails, the plan and the exercise rows are left as they were.
     */
    suspend fun applyPlanChange(change: PlanChange) = db.withTransaction {
        for (item in change.updated) savePlanItem(item)
        for (item in change.removed) db.plan().delete(item.id)
        for ((item, name) in change.added) savePlanItem(item.copy(id = 0), name)
    }

    /** Every stored skip, both sides', oldest first. */
    fun skips(): Flow<List<Skip>> = db.skips().observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * Stores [skip] and the coach's cards for it, in one transaction, and returns the skip's id. A skip already stored for
     * the same date, side and exercise is updated in place (its reason and plan item replaced, its id kept), and its
     * still-OPEN SKIP cards are deleted first, so the new reason's card replaces the old one rather than stacking (and a
     * re-skip to "Out of time" leaves none). An answered card stays. A PAIN skip also deletes that day's OPEN TARGET
     * advice for the exercise on both sides, since the hold crosses sides: it was worked out without the hold, and Today
     * works a target out again only when none is stored for the day. A PAIN skip also deletes the exercise's OPEN busy or
     * repeat SKIP cards on its side, whichever skip made them, so its pain card replaces them: a pain card is held back
     * only by an open pain card for the exercise on that side, or by a dismissed one made in the last 7 days (the quiet
     * spell, counted from when the card was made; [com.powertracker.domain.coach.SkipCoach]). An open pain card or SWAP card stays.
     *
     * [cards] gets the stored skip (with its id) and every stored skip, both sides', and returns the advice to insert. It
     * runs inside the transaction: if it throws, nothing is stored. It should only compute, from what the caller read
     * beforehand.
     */
    suspend fun skip(skip: Skip, cards: suspend (stored: Skip, all: List<Skip>) -> List<Advice>): Long = db.withTransaction {
        val existing = db.skips().find(skip.date, skip.mode, skip.exerciseId)
        val row = SkipEntity(existing?.id ?: 0, skip.date, skip.mode, skip.exerciseId, skip.planItemId, skip.reason)
        val id = if (existing == null) db.skips().insert(row) else existing.id.also { db.skips().update(row) }
        if (existing != null) deleteOpenCards(existing.id)
        if (skip.reason == SkipReason.PAIN) {
            deleteOpenTargets(skip.date, skip.exerciseId)
            deleteOpenNonPainCards(skip.mode, skip.exerciseId)
        }
        val made = cards(row.copy(id = id).toDomain(), db.skips().all().map { it.toDomain() })
        if (made.isNotEmpty()) db.advice().insert(made.map { it.toEntity() })
        id
    }

    /**
     * Takes the skip [skipId] back (Undo, Bring back), in one transaction: the skip, its still-OPEN SKIP cards and that
     * day's OPEN TARGET advice for the exercise on both sides are deleted, so the target is worked out again without the
     * hold. A card already applied or dismissed stays. An unknown id changes nothing.
     */
    suspend fun unskip(skipId: Long) = db.withTransaction {
        val skip = db.skips().byId(skipId) ?: return@withTransaction
        deleteOpenCards(skipId)
        db.skips().delete(skipId)
        deleteOpenTargets(skip.date, skip.exerciseId)
    }

    /** Deletes the OPEN SKIP advice carrying [skipId]; the payloads are matched exactly, never by text. */
    private suspend fun deleteOpenCards(skipId: Long) {
        val cards = db.advice().openOf(AdviceType.SKIP).filter { (it.decoded() as? AdvicePayload.Skipped)?.skipId == skipId }
        if (cards.isNotEmpty()) db.advice().delete(cards.map { it.id })
    }

    /** Deletes [mode]'s OPEN SKIP advice for [exerciseId] whose skip was not for pain; the payloads are matched exactly. */
    private suspend fun deleteOpenNonPainCards(mode: TrainingMode, exerciseId: Long) {
        val cards = db.advice().openOf(AdviceType.SKIP).filter { a ->
            val p = a.decoded() as? AdvicePayload.Skipped
            a.mode == mode && p != null && p.exerciseId == exerciseId && p.reason != SkipReason.PAIN
        }
        if (cards.isNotEmpty()) db.advice().delete(cards.map { it.id })
    }

    /** Deletes [date]'s OPEN TARGET advice for [exerciseId] on both sides; the payloads are matched exactly, never by text. */
    private suspend fun deleteOpenTargets(date: LocalDate, exerciseId: Long) {
        val targets = db.advice().openOf(AdviceType.TARGET, date).filter { (it.decoded() as? AdvicePayload.Target)?.exerciseId == exerciseId }
        if (targets.isNotEmpty()) db.advice().delete(targets.map { it.id })
    }

    suspend fun setBaseline(b: Baseline) = db.baselines().upsert(BaselineEntity(b.exerciseId, b.e1rmKg, b.date))
}

private fun ExerciseEntity.toDomain() = Exercise(id, name, category, parentLift, muscles, isBuiltIn, archived, plateCount)

private fun SessionEntity.toDomain(sets: List<LoggedSet>) = LoggedSession(id, date, bodyweightKg, readiness, sets, mode)

private fun SetEntity.toDomain() = LoggedSet(exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet, unit)

private fun LoggedSet.toEntity(id: Long, sessionId: Long) =
    SetEntity(id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet, unit)

private fun SkipEntity.toDomain() = Skip(id, date, mode, exerciseId, planItemId, reason)

private fun com.powertracker.data.db.AdviceEntity.decoded(): AdvicePayload = Json.decodeFromString(AdvicePayload.serializer(), payload)

private fun PlanItemEntity.toDomain() = PlanItem(id, weekday, exerciseId, sets, reps, targetRpe, sortOrder, mode, repsHigh)

/** The stored form of a check-in's groups: Kotlin names, comma-separated, in [MuscleGroup] order. */
internal fun groupsToDb(groups: Set<MuscleGroup>): String = MuscleGroup.entries.filter { it in groups }.joinToString(",") { it.name }

internal fun groupsFromDb(text: String): Set<MuscleGroup> =
    text.split(",").filter { it.isNotEmpty() }.mapTo(LinkedHashSet()) { MuscleGroup.valueOf(it) }

private fun CheckIn.toEntity() = CheckInEntity(id, date, groupsToDb(lagging), groupsToDb(growingWell))

private fun CheckInEntity.toDomain() = CheckIn(date, groupsFromDb(lagging), groupsFromDb(growingWell), id)
