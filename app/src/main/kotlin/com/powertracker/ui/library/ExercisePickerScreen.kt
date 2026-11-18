package com.powertracker.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.MuscleGroups
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.theme.Space
import com.powertracker.ui.components.PtIconButton

/** What the picker says when nothing matches. */
const val NO_MATCH = "No exercises match. Try fewer filters, or create your own."

/** The same where there is nothing to create: the Exercises tab. */
const val NO_MATCH_BROWSING = "No exercises match."

/** How many recently used exercises lead "Your exercises". */
private const val RECENT_LIMIT = 6

/** One row of the picker: a catalogue exercise, or one of the lifter's own that the catalogue does not have. */
data class PickerEntry(
    val name: String,
    val muscles: List<String>,
    /** Decides the sets × reps it joins a plan with. */
    val kind: Kind,
    /** The catalogue entry; null for the lifter's own exercise. */
    val library: LibraryExercise?,
    /** The stored row to use: for a catalogue entry, the one with its name if there is one yet. */
    val exerciseId: Long?,
) {
    /** Set when the exercise is to be found, restored or inserted by its catalogue name on saving. */
    val catalogueName: String? get() = library?.name

    /** 3 × 5 for a compound lift, 3 × 10 for an accessory. */
    val defaultSets: Int get() = 3
    val defaultReps: Int get() = if (kind == Kind.COMPOUND) 5 else 10

    companion object {
        /** Keeps a picked entry across a rotation; the catalogue entry is looked up again by name. */
        val Saver: Saver<PickerEntry?, Any> = listSaver(
            save = { e -> if (e == null) emptyList() else listOf(e.name, e.muscles.joinToString("\n"), e.kind.name, e.library != null, e.exerciseId ?: -1L) },
            restore = { v ->
                if (v.isEmpty()) return@listSaver null
                val name = v[0] as String
                PickerEntry(
                    name = name,
                    muscles = (v[1] as String).split("\n").filter { it.isNotEmpty() },
                    kind = Kind.valueOf(v[2] as String),
                    library = if (v[3] as Boolean) Library.find(name) else null,
                    exerciseId = (v[4] as Long).takeIf { it >= 0 },
                )
            },
        )
    }
}

/** The picker's rows. [yours] is only filled while nothing is searched or filtered; no exercise is in both. */
data class PickerList(val yours: List<PickerEntry>, val all: List<PickerEntry>) {
    val isEmpty: Boolean get() = yours.isEmpty() && all.isEmpty()
}

/** Exercise ids from the newest session back, each once. */
fun recentExerciseIds(history: TrainingHistory): List<Long> =
    history.sessions.sortedWith(compareByDescending<com.powertracker.domain.model.LoggedSession> { it.date }.thenByDescending { it.id })
        .flatMap { session -> session.sets.map { it.exerciseId } }
        .distinct()

/**
 * What the picker lists for a search and filters. The catalogue is browsed as it is; each entry is
 * tied to the lifter's row of the same name (ignoring case) when there is one, so an exercise that
 * was seeded, restored from a backup or made by hand under a catalogue name shows once. The lifter's
 * other exercises have no body area or equipment, so they only show while no filter chip is on.
 */
fun pickerList(
    query: String,
    area: BodyArea?,
    equipment: Set<Equipment>,
    exercises: List<Exercise>,
    recentIds: List<Long>,
    /** The muscle group being browsed. It only counts while nothing is searched and no body area is chosen. */
    muscle: MuscleGroup? = null,
    /** The muscle groups ticked in the Muscles drop-down: any of them, on top of the search and the other filters. */
    muscles: Set<MuscleGroup> = emptySet(),
): PickerList {
    val live = exercises.filter { !it.archived }.sortedBy { it.id }
    // Oldest first, like CatalogueResolver: with two rows of one name, the one shown is the one saved to.
    val rowByName = live.groupBy { it.name.lowercase() }.mapValues { it.value.first() }
    val catalogueNames = Library.all().mapTo(HashSet()) { it.name.lowercase() }

    fun catalogue(e: LibraryExercise) = PickerEntry(e.name, e.muscles, e.kind, e, rowByName[e.name.lowercase()]?.id)
    fun own(e: Exercise) = PickerEntry(e.name, e.muscles, if (e.category == Category.ACCESSORY) Kind.ACCESSORY else Kind.COMPOUND, null, e.id)
    val owned = live.filter { it.name.lowercase() !in catalogueNames }
    val q = Library.normalise(query)
    // Searched like the catalogue: names starting with the query, then names containing it, then muscles.
    fun searched(own: List<Exercise>): List<PickerEntry> = own.mapNotNull { e ->
        val name = Library.normalise(e.name)
        val rank = when {
            name.startsWith(q) -> 0
            q in name -> 1
            e.muscles.any { q in Library.normalise(it) } -> 2
            else -> return@mapNotNull null
        }
        rank to e
    }.sortedWith(compareBy({ it.first }, { it.second.name.lowercase() })).map { own(it.second) }
    val alphabetical = compareBy<PickerEntry>({ Library.normalise(it.name) }, { it.name })

    if (muscles.isNotEmpty()) {
        // A chosen area as the catalogue's search has it: a main lift also counts in its lift's area (Bench under Chest).
        val inGroups = Library.byMuscles(muscles, equipment).filter { area == null || Library.inArea(it, area) }
        // The lifter's own have no body area or equipment: they only show while neither is chosen.
        val theirs = if (equipment.isEmpty() && area == null) owned.filter { e -> muscles.any { MuscleGroups.works(e.muscles, it) } } else emptyList()
        if (q.isEmpty()) return PickerList(emptyList(), (inGroups.map(::catalogue) + theirs.map(::own)).sortedWith(alphabetical))
        // Ranked like any search, kept to the ticked muscles.
        val names = inGroups.mapTo(HashSet()) { it.name }
        return PickerList(emptyList(), searched(theirs) + Library.search(query, setOfNotNull(area), equipment).filter { it.name in names }.map(::catalogue))
    }
    if (muscle != null && q.isEmpty() && area == null) {
        // One alphabetical list: the catalogue's exercises for the muscle, and the lifter's own that name it.
        val theirs = if (equipment.isEmpty()) owned.filter { MuscleGroups.works(it.muscles, muscle) }.map(::own) else emptyList()
        val all = Library.byMuscle(muscle, equipment).map(::catalogue) + theirs
        return PickerList(emptyList(), all.sortedWith(alphabetical))
    }
    val found = Library.search(query, setOfNotNull(area), equipment).map(::catalogue)
    if (q.isEmpty() && area == null && equipment.isEmpty()) {
        val byId = live.associateBy { it.id }
        val recent = recentIds.mapNotNull { byId[it] }.map { e -> Library.all().firstOrNull { it.name.equals(e.name, ignoreCase = true) }?.let(::catalogue) ?: own(e) }
            .distinctBy { it.exerciseId }
            .take(RECENT_LIMIT)
        val yours = (recent + owned.sortedBy { it.name.lowercase() }.map(::own)).distinctBy { it.exerciseId }
        val shown = yours.mapNotNull { it.catalogueName }.toSet()
        return PickerList(yours, found.filter { it.name !in shown })
    }
    if (area != null || equipment.isNotEmpty()) return PickerList(emptyList(), found)
    return PickerList(emptyList(), searched(owned) + found)
}

/** The full-screen exercise picker, over whatever screen opened it. */
@Composable
fun ExercisePickerScreen(
    title: String,
    exercises: List<Exercise>,
    recentIds: List<Long>,
    onPick: (PickerEntry) -> Unit,
    onCreateCustom: (name: String, Category, parentLift: MainLift?, muscles: List<String>) -> Unit,
    onDismiss: () -> Unit,
    /** See [ExercisePickerContent]. */
    createFailures: Int = 0,
) {
    // Out here, in the screen's own composition, "Create your own" and what is typed into it survive a rotation
    // (state saved inside the picker's window is not restored).
    val createForm = rememberCustomExerciseForm()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ExercisePickerContent(title, exercises, recentIds, onPick, onCreateCustom, onDismiss, createFailures = createFailures, createForm = createForm)
        }
    }
}

/**
 * The picker without its window: the shared [ExerciseBrowser] on its muscle grid, each row with its thumbnail, under a
 * title and a close button. Tapping a row picks it; tapping its info button only opens the guide.
 */
@Composable
fun ExercisePickerContent(
    title: String,
    /** The lifter's stored exercises. */
    exercises: List<Exercise>,
    /** Stored exercise ids, most recently used first. */
    recentIds: List<Long>,
    onPick: (PickerEntry) -> Unit,
    onCreateCustom: (name: String, Category, parentLift: MainLift?, muscles: List<String>) -> Unit,
    onClose: () -> Unit,
    /** The muscle group it opens on; null opens on the grid. */
    initialMuscle: MuscleGroup? = null,
    /**
     * How many of [onCreateCustom]'s creates have failed, as counted by whoever opened the picker. "Create your own"
     * stays open after Create until the picker is closed, so when this goes up the form is still there with everything
     * typed: it says so, and Create works again.
     */
    createFailures: Int = 0,
    /** "Create your own": whether it is open and what is typed into it. */
    createForm: CustomExerciseForm = rememberCustomExerciseForm(),
) {
    val scheme = MaterialTheme.colorScheme
    val browser = rememberExerciseBrowserState(MuscleFilter.GRID, initialMuscle)
    var guideFor by rememberSaveable { mutableStateOf<String?>(null) }
    // The picker answers once: a second tap landing before it closes (a double tap) picks or creates nothing more.
    val answered = remember { mutableStateOf(false) }
    val pick: (PickerEntry) -> Unit = { entry ->
        if (!answered.value) {
            answered.value = true
            onPick(entry)
        }
    }
    val createCustom: (String, Category, MainLift?, List<String>) -> Unit = { name, category, parent, muscles ->
        if (!answered.value) {
            answered.value = true
            onCreateCustom(name, category, parent, muscles)
        }
    }
    // Failures counted before this picker opened (or was restored) are not its own.
    val failuresBefore = remember { createFailures }
    val failures = (createFailures - failuresBefore).coerceAtLeast(0)
    // Failures that came once "Create your own" had been cancelled: the picker says them over its list, since the
    // screen's own snackbar is behind the picker.
    var listFailures by remember { mutableIntStateOf(0) }
    LaunchedEffect(failures) {
        // A failed create has answered nothing: Create may be tapped again.
        if (failures > 0) {
            answered.value = false
            if (!createForm.open) listFailures++
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, navigation = { PtIconButton(onClick = onClose, modifier = Modifier.size(Space.touch)) { Icon(Icons.Filled.Close, "Close", tint = scheme.onSurfaceVariant) } })
        ExerciseBrowser(
            state = browser,
            exercises = exercises,
            recentIds = recentIds,
            onRow = pick,
            // Reading the guide must not pick the exercise.
            onInfo = { guideFor = it.name },
            rowLabel = { "Add $it" },
            onCreate = { createForm.start(browser.query.trim()) },
            showThumbnails = true,
            // Under 309 dp of text room (a narrow phone at large text) the thumbnail would break long names.
            thumbnailMinRoomDp = PICKER_THUMBNAIL_MIN_ROOM_DP,
        )
    }

    FormMessage(SAVE_FAILED, showId = listFailures)

    guideFor?.let { name -> FormGuideSheet(name, onDismiss = { guideFor = null }) }

    if (createForm.open) {
        CustomExerciseDialog(
            form = createForm,
            // The form stays open until the picker closes on the new exercise; if the create fails, it is still here.
            onCreate = createCustom,
            onDismiss = { createForm.open = false },
            failures = failures,
        )
    }
}
