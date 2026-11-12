package com.powertracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.ui.library.DayChooserContent
import com.powertracker.ui.library.ExerciseBrowserState
import com.powertracker.ui.library.ExercisePickerContent
import com.powertracker.ui.library.ExercisesContent
import com.powertracker.ui.library.FormGuideContent
import com.powertracker.ui.library.GuideActions
import com.powertracker.ui.library.MuscleFilter
import com.powertracker.ui.library.MusclesContent
import com.powertracker.ui.library.RpeContent
import kotlinx.datetime.DayOfWeek

/** The real picker as Plan opens it: the muscle grid, the lifter's recent and own exercises, then the catalogue. */
@Composable
fun ExercisePickerShowcase(muscle: MuscleGroup? = null) {
    val seeded = SeedLibrary.exercises()
    val own = Exercise(900, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)
    val recent = listOf("Squat", "Bench", "RDL").map { name -> seeded.first { it.name == name }.id }
    ExercisePickerContent("Add to Monday", seeded + own, recent, onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {}, initialMuscle = muscle)
}

private fun showcaseExercises(): Pair<List<Exercise>, List<Long>> {
    val seeded = SeedLibrary.exercises()
    val own = Exercise(900, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)
    return (seeded + own) to listOf("Squat", "Bench", "RDL").map { name -> seeded.first { it.name == name }.id }
}

/** The real Exercises tab: the count, the filters with the Muscles drop-down, the lifter's exercises, then the catalogue by body area. */
@Composable
fun ExercisesShowcase(muscles: Set<MuscleGroup> = emptySet()) {
    val (exercises, recent) = showcaseExercises()
    ExercisesContent(exercises, recent, onOpen = {}, browser = remember { ExerciseBrowserState(MuscleFilter.DROPDOWN, muscles = muscles) })
}

/** The tab with two muscle groups ticked, and the real Muscles drop-down over it, in a stand-in for its sheet. */
@Composable
fun ExercisesMusclesShowcase() {
    val ticked = setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
    SheetShowcase(behind = { ExercisesShowcase(ticked) }) { MusclesContent(ticked, onToggle = {}, onClose = {}) }
}

/** The guide as the Exercises tab opens it during a session: the actions stay under the guide as it scrolls. */
@Composable
fun GuideActionsShowcase() = SheetShowcase {
    FormGuideContent("Squat", onClose = {}) { GuideActions(inSession = true, onAddToPlan = {}, onAddToToday = {}) }
}

/** "Add to plan", second step: the lifter's training days. */
@Composable
fun DayChooserShowcase() = SheetShowcase(behind = { ExercisesShowcase() }) {
    DayChooserContent("Squat", listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), onPick = {}, onClose = {})
}

/** The real guide content for the squat, in a stand-in for the bottom sheet it opens in. */
@Composable
fun FormGuideShowcase() = SheetShowcase { FormGuideContent("Squat", onClose = {}) }

/** The real "What RPE means" content, in the same stand-in. */
@Composable
fun RpeSheetShowcase() = SheetShowcase { RpeContent(onClose = {}) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetShowcase(behind: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        behind()
        Box(Modifier.fillMaxSize().background(scheme.scrim.copy(alpha = 0.32f)))
        Surface(
            Modifier.fillMaxWidth().padding(top = 24.dp).align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = scheme.surface,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle(color = scheme.outline)
                content()
            }
        }
    }
}
