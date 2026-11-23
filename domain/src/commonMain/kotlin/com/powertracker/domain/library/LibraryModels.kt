package com.powertracker.domain.library

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import kotlin.jvm.JvmInline

/** One exercise in the read-only catalogue. Names are unique ignoring case and are never renamed once shipped. */
data class LibraryExercise(
    val name: String,
    /** Search terms and former names, e.g. "Romanian deadlift" for RDL. */
    val aliases: List<String>,
    /** Primary grouping for browsing. */
    val area: BodyArea,
    /** What's needed; the first is the primary. */
    val equipment: List<Equipment>,
    /** Display names, the same vocabulary as `Exercise.muscles`. */
    val muscles: List<String>,
    /** Sets the default sets x reps when the exercise is added to a plan. */
    val kind: Kind,
    val category: Category,
    /** For VARIATION only. */
    val parentLift: MainLift?,
    val guide: FormGuide,
    /** At least one; each is a key of [Sources.all]. */
    val sources: List<SourceId>,
)

/** A short form guide. The UI appends [Library.SAFETY_LINE] after the last section. */
data class FormGuide(
    /** One or two sentences: what it trains and why. */
    val purpose: String,
    val setup: List<String>,
    val steps: List<String>,
    /** Two or three. */
    val cues: List<String>,
    /** Two to four. */
    val mistakes: List<String>,
)

enum class BodyArea { MAIN_LIFTS, CHEST, BACK, SHOULDERS, ARMS, QUADS, HAMSTRINGS_GLUTES, CALVES, CORE }

enum class Equipment { BARBELL, DUMBBELL, KETTLEBELL, CABLE, MACHINE, BODYWEIGHT, BAND, SPECIALTY_BAR, SMITH }

enum class Kind { COMPOUND, ACCESSORY }

/** A key into [Sources.all]. */
@JvmInline
value class SourceId(val key: String)
