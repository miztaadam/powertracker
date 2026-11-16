package com.powertracker.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Exercise
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.pressScale
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.components.cardHighlight
import com.powertracker.ui.components.cardShadow
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** Tiles to a row in the muscle grid: three names fit a 360 dp phone. */
private const val MUSCLE_COLUMNS = 3

/** Muscle names to a row in the Muscles drop-down. */
private const val OPTION_COLUMNS = 2

/** How the browser narrows the list by muscle. */
enum class MuscleFilter {
    /** The picker: a grid of tiles, one muscle at a time, left by a search or a body area. */
    GRID,

    /** The Exercises tab: a "Muscles" drop-down, any number ticked, on top of the search and the other filters. */
    DROPDOWN,
}

/** What an [ExerciseBrowser] is searched and filtered by. Saved with the screen, not across a restart. */
@Stable
class ExerciseBrowserState(
    val muscleFilter: MuscleFilter,
    query: String = "",
    area: BodyArea? = null,
    equipment: Set<Equipment> = emptySet(),
    muscle: MuscleGroup? = null,
    muscles: Set<MuscleGroup> = emptySet(),
) {
    var query by mutableStateOf(query)
        private set

    /** One body area at a time; null is all of them. */
    var area by mutableStateOf(area)
        private set

    /** Any of these; empty is all of them. */
    var equipment by mutableStateOf(equipment)
        private set

    /** [MuscleFilter.GRID]: the muscle whose exercises are listed; null shows the grid. */
    var muscle by mutableStateOf(muscle)
        private set

    /** [MuscleFilter.DROPDOWN]: the ticked muscle groups. */
    var muscles by mutableStateOf(muscles)
        private set

    fun search(text: String) {
        query = text
        // Searching is over everything: it leaves the grid's muscle.
        if (text.isNotEmpty()) muscle = null
    }

    fun toggleArea(a: BodyArea?) {
        area = if (area == a) null else a
        if (a != null) muscle = null
    }

    fun toggleEquipment(e: Equipment) {
        equipment = if (e in equipment) equipment - e else equipment + e
    }

    fun browse(group: MuscleGroup?) {
        muscle = group
    }

    fun toggleMuscle(group: MuscleGroup) {
        muscles = if (group in muscles) muscles - group else muscles + group
    }

    fun clearMuscles() {
        muscles = emptySet()
    }

    companion object {
        fun saver(muscleFilter: MuscleFilter): Saver<ExerciseBrowserState, Any> = listSaver(
            save = { s -> listOf(s.query, s.area?.name.orEmpty(), s.equipment.joinToString(",") { it.name }, s.muscle?.name.orEmpty(), s.muscles.joinToString(",") { it.name }) },
            restore = { v ->
                fun names(text: String) = text.split(",").filter { it.isNotEmpty() }
                ExerciseBrowserState(
                    muscleFilter,
                    query = v[0],
                    area = v[1].takeIf { it.isNotEmpty() }?.let(BodyArea::valueOf),
                    equipment = names(v[2]).mapTo(LinkedHashSet(), Equipment::valueOf),
                    muscle = v[3].takeIf { it.isNotEmpty() }?.let(MuscleGroup::valueOf),
                    muscles = names(v[4]).mapTo(LinkedHashSet(), MuscleGroup::valueOf),
                )
            },
        )
    }
}

/** A browser's state, kept across a rotation and, inside a tab, across switching tabs. */
@Composable
fun rememberExerciseBrowserState(muscleFilter: MuscleFilter, initialMuscle: MuscleGroup? = null): ExerciseBrowserState =
    rememberSaveable(saver = ExerciseBrowserState.saver(muscleFilter)) { ExerciseBrowserState(muscleFilter, muscle = initialMuscle) }

/**
 * Browsing the catalogue and the lifter's own exercises, shared by the picker and the Exercises tab:
 * search, body-area chips (one at a time), equipment chips (any of the chosen), then the rows, each
 * with its info button and, on the Exercises tab, its thumbnail. What a row tap and an info tap do is up to the screen.
 *
 * By muscle it works one of two ways ([ExerciseBrowserState.muscleFilter]). As a grid, tiles lead
 * the list while nothing is searched and no body area is chosen; a tile lists that muscle's
 * exercises until "All muscles", the back button, a search or a body area leaves it. As a drop-down,
 * "Muscles" leads the equipment chips and opens a sheet of muscle groups to tick: the list is every
 * exercise that works any ticked group, within the search and the other filters, and the ticked
 * groups sit under the chips to take away one by one or all at once. With none ticked and nothing
 * searched, the catalogue is listed under its body areas.
 */
@Composable
fun ExerciseBrowser(
    state: ExerciseBrowserState,
    /** The lifter's stored exercises. */
    exercises: List<Exercise>,
    /** Stored exercise ids, most recently used first. */
    recentIds: List<Long>,
    onRow: (PickerEntry) -> Unit,
    onInfo: (PickerEntry) -> Unit,
    /** What a row tap does, as a screen reader says it: "Add Squat", "Open Squat". */
    rowLabel: (name: String) -> String,
    modifier: Modifier = Modifier,
    /** Opens "Create your own"; null where exercises are only browsed. */
    onCreate: (() -> Unit)? = null,
    /** A small picture at the left of each row: the Exercises tab has them, the picker does not. */
    showThumbnails: Boolean = false,
    /**
     * Multi-select: when set, every row is a tick box showing whether its exercise is chosen, and a row tap ([onRow])
     * ticks or unticks it. Null (the picker, the Exercises tab) keeps rows as plain taps.
     */
    ticked: ((PickerEntry) -> Boolean)? = null,
    /**
     * Multi-select: every ticked exercise, listed under "Chosen" above the list whatever the search and filters, so none
     * is out of sight. Each is a tick box like the list's own row. Empty: no such section.
     */
    chosen: List<PickerEntry> = emptyList(),
) {
    val query = state.query
    val area = state.area
    val equipment = state.equipment
    val muscle = state.muscle
    val muscles = state.muscles
    val dropdown = state.muscleFilter == MuscleFilter.DROPDOWN
    var choosingMuscles by rememberSaveable { mutableStateOf(false) }

    val list = remember(query, area, equipment, exercises, recentIds, muscle, muscles) { pickerList(query, area, equipment, exercises, recentIds, muscle, muscles) }
    val searching = Library.normalise(query).isNotEmpty()
    val showGrid = !dropdown && muscle == null && area == null && !searching
    // Under body areas only while the list is the alphabetical catalogue: a search is ranked, ticked muscles cut across areas.
    val byArea = dropdown && muscles.isEmpty() && !searching
    val listState = rememberLazyListState()
    // A new search or filter starts from its best match, not from wherever the last list was scrolled to.
    LaunchedEffect(query, area, equipment, muscle, muscles) { listState.scrollToItem(0) }
    BackHandler(enabled = muscle != null) { state.browse(null) }
    if (showThumbnails) {
        val thumbnails = LocalThumbnails.current
        // The head start: from the list's first showing, the loader's worker works out every catalogue thumbnail,
        // in the order this list shows them. Only the first call counts, so it runs once per process.
        LaunchedEffect(thumbnails) { thumbnails.warmUp(thumbnailOrder(list, byArea)) }
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = state::search,
            placeholder = { Text("Search exercises", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (query.isEmpty()) null else ({ IconButton(onClick = { state.search("") }, modifier = Modifier.size(Space.touch)) { Icon(Icons.Filled.Close, "Clear search") } }),
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
                        ChoiceChip("All", area == null, { state.toggleArea(null) }, Modifier.testTag("area-all"))
                        for (a in BodyArea.entries) ChoiceChip(a.label(), area == a, { state.toggleArea(a) }, Modifier.testTag("area-${a.name}"))
                    }
                    ChipRow {
                        if (dropdown) DropdownChip("Muscles", muscles.size, { choosingMuscles = true }, Modifier.testTag("muscles-dropdown"))
                        for (e in Equipment.entries) ChoiceChip(e.label(), e in equipment, { state.toggleEquipment(e) }, Modifier.testTag("equipment-${e.name}"))
                    }
                    if (muscles.isNotEmpty()) TickedMuscles(muscles, onRemove = state::toggleMuscle, onClear = state::clearMuscles)
                }
            }
            if (showGrid) {
                item(key = "muscles") {
                    Column {
                        ListHeading("Browse by muscle")
                        MuscleGrid(Library.muscleGroups(), onPick = state::browse)
                    }
                }
            }
            muscle?.let { group ->
                item(key = "muscle") {
                    Column(Modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.m)) {
                        SecondaryButton("All muscles", { state.browse(null) }, icon = Icons.AutoMirrored.Filled.ArrowBack)
                    }
                    ListHeading("${group.label} exercises")
                }
            }
            val row: @Composable (PickerEntry) -> Unit = { entry -> BrowserRow(entry, rowLabel(entry.name), onRow, onInfo, showThumbnails, ticked?.invoke(entry)) }
            if (chosen.isNotEmpty()) {
                item(key = "chosen") { ListHeading("Chosen") }
                items(chosen, key = { "chosen-${it.rowKey}" }) { entry ->
                    BrowserRow(entry, rowLabel(entry.name), onRow, onInfo, showThumbnails, ticked?.invoke(entry) ?: true, tag = "chosen-${entry.name}")
                }
            }
            if (list.isEmpty) {
                item(key = "empty") {
                    if (onCreate != null) {
                        EmptyState(Icons.Filled.SearchOff, NO_MATCH, "Your own exercises work everywhere, without a form guide.") {
                            SecondaryButton("Create your own", onCreate, icon = Icons.Filled.Add)
                        }
                    } else {
                        EmptyState(Icons.Filled.SearchOff, NO_MATCH_BROWSING, "Try fewer filters.")
                    }
                }
            } else {
                if (list.yours.isNotEmpty()) {
                    item(key = "yours") { ListHeading("Your exercises") }
                    items(list.yours, key = { "yours-${it.exerciseId}" }) { row(it) }
                    if (!byArea) item(key = "all") { ListHeading("All exercises") }
                }
                // After "Chosen", the list needs a heading of its own; by area, the areas give it one.
                if (list.yours.isEmpty() && chosen.isNotEmpty() && !byArea) item(key = "all") { ListHeading("All exercises") }
                if (byArea) areaSections(list.all, row) else items(list.all, key = { it.rowKey }) { row(it) }
                if (onCreate != null) {
                    item(key = "create") {
                        Column(Modifier.fillMaxWidth().padding(top = Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
                            SecondaryButton("Create your own", onCreate, icon = Icons.Filled.Add)
                        }
                    }
                }
            }
        }
    }

    if (choosingMuscles) {
        GuideSheet(onDismiss = { choosingMuscles = false }) { MusclesContent(muscles, state::toggleMuscle, onClose = { choosingMuscles = false }) }
    }
}

private val PickerEntry.rowKey: String get() = catalogueName ?: "own-$exerciseId"

/**
 * The catalogue names of [list] in the order it shows them (under body areas when [byArea]), then the rest of the
 * catalogue, each once: the warm-up's order. Names only: building it works nothing out.
 */
internal fun thumbnailOrder(list: PickerList, byArea: Boolean): List<String> {
    // The same order as areaSections: the entries with no area first, then area by area, each area in the order given.
    val all = if (byArea) list.all.sortedBy { it.library?.area?.ordinal ?: -1 } else list.all
    return ((list.yours + all).mapNotNull { it.catalogueName } + Library.all().map { it.name }).distinct()
}

/** [entries] under their body areas, in the catalogue's area order; each area keeps the order it came in. */
private fun LazyListScope.areaSections(entries: List<PickerEntry>, row: @Composable (PickerEntry) -> Unit) {
    val byArea = entries.groupBy { it.library?.area }
    // The lifter's own have no area. While nothing is filtered they are under "Your exercises", so none is expected here.
    items(byArea[null].orEmpty(), key = { it.rowKey }) { row(it) }
    for (area in BodyArea.entries) {
        val inArea = byArea[area] ?: continue
        item(key = "area-heading-${area.name}") { ListHeading(area.label(), Modifier.testTag("area-heading-${area.name}")) }
        items(inArea, key = { it.rowKey }) { row(it) }
    }
}

@Composable
private fun ChipRow(chips: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) { chips() }
}

/** A chip that opens a list to choose from. Tonal, like a selected chip, once [count] of its choices are made. */
@Composable
private fun DropdownChip(text: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val chosen = count > 0
    Row(
        modifier
            .sizeIn(minHeight = Space.touch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Choose", onClick = onClick)
            .padding(vertical = 4.dp)
            .background(if (chosen) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh, CircleShape)
            .border(1.dp, if (chosen) scheme.outline else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (chosen) "$text · $count" else text,
            style = MaterialTheme.typography.labelLarge,
            color = if (chosen) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** The ticked muscle groups as chips that take themselves away, then "Clear" for all of them. They wrap, so none is hidden. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TickedMuscles(ticked: Set<MuscleGroup>, onRemove: (MuscleGroup) -> Unit, onClear: () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // In the drop-down's order, whatever order they were ticked in.
        for (group in MuscleGroup.entries.filter { it in ticked }) RemovableChip(group.label, { onRemove(group) }, Modifier.testTag("muscle-chip-${group.name}"))
        QuietButton("Clear", onClear, Modifier.heightIn(min = Space.touch).testTag("muscles-clear"))
    }
}

/** A chosen filter, tonal like a selected chip, with a cross: tapping it anywhere takes it away. */
@Composable
private fun RemovableChip(text: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .sizeIn(minHeight = Space.touch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Remove", onClick = onRemove)
            .padding(vertical = 4.dp)
            .background(scheme.surfaceContainerHighest, CircleShape)
            .border(1.dp, scheme.outline, CircleShape)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, maxLines = 1)
        Icon(Icons.Filled.Close, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

/** The Muscles drop-down's content without its sheet: every muscle group, in the enum's order, to tick. */
@Composable
fun MusclesContent(ticked: Set<MuscleGroup>, onToggle: (MuscleGroup) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.xl, end = Space.s, bottom = Space.xl)
            .testTag("muscles-sheet"),
    ) {
        GuideHeader("Muscles", "Exercises that work any of them", closeLabel = "Close", onClose = onClose)
        MuscleOptions(MuscleGroup.entries, ticked, onToggle, Modifier.padding(top = Space.m, end = Space.l))
    }
}

/**
 * [groups] as tick boxes, two to a row, in the order given: the Muscles drop-down's list, also the plan builder's body
 * parts and the check-in's. A group in [locked] is shown but cannot be ticked.
 */
@Composable
fun MuscleOptions(
    groups: List<MuscleGroup>,
    ticked: Set<MuscleGroup>,
    onToggle: (MuscleGroup) -> Unit,
    modifier: Modifier = Modifier,
    locked: Set<MuscleGroup> = emptySet(),
) {
    Column(modifier) {
        for (row in groups.chunked(OPTION_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                for (group in row) MuscleOption(group, group in ticked, onToggle, Modifier.weight(1f), enabled = group !in locked)
                repeat(OPTION_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MuscleOption(group: MuscleGroup, ticked: Boolean, onToggle: (MuscleGroup) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Row(
        modifier
            .heightIn(min = Space.touch)
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = ticked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle(group) })
            .testTag("muscle-option-${group.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        // The row is the control; the box only shows its state.
        Checkbox(
            checked = ticked,
            onCheckedChange = null,
            enabled = enabled,
            // A locked box keeps the outline (3:1, the design system's minimum for a control's edge) rather than
            // Material's faint disabled grey; its label goes to muted text (4.5:1). TokenContrastTest checks both.
            colors = CheckboxDefaults.colors(
                checkedColor = pt.accent, checkmarkColor = pt.onAccent, uncheckedColor = scheme.outline, disabledUncheckedColor = scheme.outline,
            ),
        )
        Text(group.label, style = MaterialTheme.typography.titleMedium, color = if (enabled) scheme.onSurface else pt.muted, modifier = Modifier.weight(1f))
    }
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
    val shape = MaterialTheme.shapes.medium
    Surface(
        onClick = { onPick(group) },
        modifier = modifier.heightIn(min = 56.dp).cardShadow(shape).testTag("muscle-${group.name}"),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.cardHighlight(shape).padding(horizontal = Space.xs), contentAlignment = Alignment.Center) {
            Text(group.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ListHeading(text: String, modifier: Modifier = Modifier) {
    Eyebrow(
        text,
        modifier.padding(start = Space.gutter, end = Space.gutter, top = Space.l, bottom = Space.xs).semantics { heading() },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BrowserRow(
    entry: PickerEntry,
    clickLabel: String,
    onRow: (PickerEntry) -> Unit,
    onInfo: (PickerEntry) -> Unit,
    showThumbnail: Boolean,
    /** Multi-select: whether the row is ticked; null for a plain row. */
    ticked: Boolean? = null,
    tag: String = "exercise-${entry.name}",
) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    // The row sinks while pressed; a press that becomes a scroll of the list springs back and picks nothing. With
    // motion off there is no sink, and no source of its own: the click then sets up its ripple only when first touched,
    // as before, so a list of rows costs nothing more to open.
    val interaction = if (LocalMotion.current) remember { MutableInteractionSource() } else null
    val indication = LocalIndication.current
    Column {
        Row(
            Modifier
                .then(if (interaction != null) Modifier.pressScale(interaction) else Modifier)
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .then(
                    if (ticked == null) Modifier.clickable(interaction, indication, onClickLabel = clickLabel) { onRow(entry) }
                    else Modifier.toggleable(value = ticked, interactionSource = interaction, indication = indication, role = Role.Checkbox, onValueChange = { onRow(entry) }),
                )
                .padding(start = if (ticked == null) Space.gutter else Space.xs, end = Space.xs)
                .testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The row is the control; the box only shows its state.
            if (ticked != null) {
                Checkbox(
                    checked = ticked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = pt.accent, checkmarkColor = pt.onAccent, uncheckedColor = scheme.outline),
                    modifier = Modifier.padding(horizontal = Space.s),
                )
            }
            // The lifter's own exercise has no catalogue name, so its tile is empty.
            if (showThumbnail) ExerciseThumbnail(entry.catalogueName, Modifier.padding(end = Space.m))
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
            // Its own button, outside the row's click.
            InfoIconButton(entry.name, onClick = { onInfo(entry) })
        }
        SoftDivider(Modifier.padding(start = Space.gutter))
    }
}
