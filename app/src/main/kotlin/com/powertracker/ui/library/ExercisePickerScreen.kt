package com.powertracker.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** What the picker says when nothing matches. */
const val NO_MATCH = "No exercises match. Try fewer filters, or create your own."

/** How many recently used exercises lead "Your exercises". */
private const val RECENT_LIMIT = 6

/** Tiles to a row in the muscle grid: three names fit a 360 dp phone. */
private const val MUSCLE_COLUMNS = 3

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
): PickerList {
    val live = exercises.filter { !it.archived }.sortedBy { it.id }
    // Oldest first, like CatalogueResolver: with two rows of one name, the one shown is the one saved to.
    val rowByName = live.groupBy { it.name.lowercase() }.mapValues { it.value.first() }
    val catalogueNames = Library.all().mapTo(HashSet()) { it.name.lowercase() }

    fun catalogue(e: LibraryExercise) = PickerEntry(e.name, e.muscles, e.kind, e, rowByName[e.name.lowercase()]?.id)
    fun own(e: Exercise) = PickerEntry(e.name, e.muscles, if (e.category == Category.ACCESSORY) Kind.ACCESSORY else Kind.COMPOUND, null, e.id)
    val owned = live.filter { it.name.lowercase() !in catalogueNames }
    val q = Library.normalise(query)
    if (muscle != null && q.isEmpty() && area == null) {
        // One alphabetical list: the catalogue's exercises for the muscle, and the lifter's own that name it.
        val theirs = if (equipment.isEmpty()) owned.filter { MuscleGroups.works(it.muscles, muscle) }.map(::own) else emptyList()
        val all = Library.byMuscle(muscle, equipment).map(::catalogue) + theirs
        return PickerList(emptyList(), all.sortedWith(compareBy({ Library.normalise(it.name) }, { it.name })))
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
    // Searched like the catalogue: names starting with the query, then names containing it, then muscles.
    val mine = owned.mapNotNull { e ->
        val name = Library.normalise(e.name)
        val rank = when {
            name.startsWith(q) -> 0
            q in name -> 1
            e.muscles.any { q in Library.normalise(it) } -> 2
            else -> return@mapNotNull null
        }
        rank to e
    }.sortedWith(compareBy({ it.first }, { it.second.name.lowercase() })).map { own(it.second) }
    return PickerList(emptyList(), mine + found)
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
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ExercisePickerContent(title, exercises, recentIds, onPick, onCreateCustom, onDismiss)
        }
    }
}

/**
 * The picker without its window: search, body-area chips (one at a time), equipment chips (any of
 * the chosen), then the lifter's exercises and the catalogue. Tapping a row picks it; tapping its
 * info button only opens the guide.
 *
 * While nothing is searched and no body area is chosen, a grid of muscle groups leads the list.
 * Tapping one lists that muscle's exercises (equipment chips still narrow them) until "All muscles",
 * the back button, a search or a body area leaves it.
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
) {
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf<BodyArea?>(null) }
    var equipment by rememberSaveable { mutableStateOf(emptySet<Equipment>()) }
    var muscle by rememberSaveable { mutableStateOf(initialMuscle) }
    var creating by rememberSaveable { mutableStateOf(false) }
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

    val list = remember(query, area, equipment, exercises, recentIds, muscle) { pickerList(query, area, equipment, exercises, recentIds, muscle) }
    val showGrid = muscle == null && area == null && Library.normalise(query).isEmpty()
    val listState = rememberLazyListState()
    // A new search or filter starts from its best match, not from wherever the last list was scrolled to.
    LaunchedEffect(query, area, equipment, muscle) { listState.scrollToItem(0) }
    BackHandler(enabled = muscle != null) { muscle = null }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, navigation = { IconButton(onClick = onClose, modifier = Modifier.size(Space.touch)) { Icon(Icons.Filled.Close, "Close", tint = scheme.onSurfaceVariant) } })
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                // Searching is over everything: it leaves the muscle.
                if (it.isNotEmpty()) muscle = null
            },
            placeholder = { Text("Search exercises", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isEmpty()) null else ({ IconButton(onClick = { query = "" }, modifier = Modifier.size(Space.touch)) { Icon(Icons.Filled.Close, "Clear search") } }),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .semantics { contentDescription = "Search exercises" }
                .testTag("exercise-search"),
        )
        LazyColumn(Modifier.fillMaxSize().testTag("exercise-list"), state = listState, contentPadding = PaddingValues(bottom = Space.xl)) {
            // The chips scroll away with the list, so the results get the room once a filter is set.
            item(key = "filters") {
                Column(Modifier.padding(top = Space.s)) {
                    ChipRow {
                        ChoiceChip("All", area == null, { area = null }, Modifier.testTag("area-all"))
                        for (a in BodyArea.entries) {
                            ChoiceChip(
                                a.label(), area == a,
                                {
                                    area = if (area == a) null else a
                                    muscle = null
                                },
                                Modifier.testTag("area-${a.name}"),
                            )
                        }
                    }
                    ChipRow {
                        for (e in Equipment.entries) {
                            ChoiceChip(e.label(), e in equipment, { equipment = if (e in equipment) equipment - e else equipment + e }, Modifier.testTag("equipment-${e.name}"))
                        }
                    }
                }
            }
            if (showGrid) {
                item(key = "muscles") {
                    Column {
                        ListHeading("Browse by muscle")
                        MuscleGrid(Library.muscleGroups(), onPick = { muscle = it })
                    }
                }
            }
            muscle?.let { group ->
                item(key = "muscle") {
                    Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m)) {
                        SecondaryButton("All muscles", { muscle = null }, icon = Icons.AutoMirrored.Filled.ArrowBack)
                    }
                    ListHeading("${group.label} exercises")
                }
            }
            if (list.isEmpty) {
                item(key = "empty") {
                    EmptyState(Icons.Filled.SearchOff, NO_MATCH, "Your own exercises work everywhere, without a form guide.") {
                        SecondaryButton("Create your own", { creating = true }, icon = Icons.Filled.Add)
                    }
                }
            } else {
                if (list.yours.isNotEmpty()) {
                    item(key = "yours") { ListHeading("Your exercises") }
                    items(list.yours, key = { "yours-${it.exerciseId}" }) { entry -> PickerRow(entry, pick) }
                    item(key = "all") { ListHeading("All exercises") }
                }
                items(list.all, key = { it.catalogueName ?: "own-${it.exerciseId}" }) { entry -> PickerRow(entry, pick) }
                item(key = "create") {
                    Column(Modifier.fillMaxWidth().padding(top = Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
                        SecondaryButton("Create your own", { creating = true }, icon = Icons.Filled.Add)
                    }
                }
            }
        }
    }

    if (creating) {
        CustomExerciseDialog(
            initialName = query.trim(),
            onCreate = { name, category, parent, muscles ->
                creating = false
                createCustom(name, category, parent, muscles)
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun ChipRow(chips: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) { chips() }
}

/** The muscle groups as tonal tiles, three to a row. */
@Composable
private fun MuscleGrid(groups: List<MuscleGroup>, onPick: (MuscleGroup) -> Unit) {
    Column(Modifier.padding(horizontal = Space.gutter).padding(top = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        for (row in groups.chunked(MUSCLE_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                for (group in row) MuscleTile(group, onPick, Modifier.weight(1f))
                repeat(MUSCLE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MuscleTile(group: MuscleGroup, onPick: (MuscleGroup) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onPick(group) },
        modifier = modifier.heightIn(min = 56.dp).testTag("muscle-${group.name}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, PowertrackerTheme.colors.hairline),
    ) {
        Box(Modifier.padding(horizontal = Space.xs), contentAlignment = Alignment.Center) {
            Text(group.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ListHeading(text: String) {
    Eyebrow(
        text,
        Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.l, bottom = Space.xs).semantics { heading() },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PickerRow(entry: PickerEntry, onPick: (PickerEntry) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClickLabel = "Add ${entry.name}") { onPick(entry) }
                .padding(start = Space.gutter, end = Space.xs)
                .testTag("exercise-${entry.name}"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = Space.s)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                if (entry.muscles.isNotEmpty()) {
                    Text(entry.muscles.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
            Text(
                entry.library?.equipment?.firstOrNull()?.label() ?: "Your own",
                style = MaterialTheme.typography.labelMedium,
                color = PowertrackerTheme.colors.muted,
                maxLines = 1,
                modifier = Modifier.padding(start = Space.m),
            )
            // Its own button, outside the row's click: reading the guide must not pick the exercise.
            InfoButton(entry.name)
        }
        HorizontalDivider(Modifier.padding(start = Space.gutter), color = PowertrackerTheme.colors.hairline)
    }
}
