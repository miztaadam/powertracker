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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.ui.library.ExercisePickerContent
import com.powertracker.ui.library.FormGuideContent
import com.powertracker.ui.library.RpeContent

/** The real picker as Plan opens it: the muscle grid, the lifter's recent and own exercises, then the catalogue. */
@Composable
fun ExercisePickerShowcase(muscle: MuscleGroup? = null) {
    val seeded = SeedLibrary.exercises()
    val own = Exercise(900, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)
    val recent = listOf("Squat", "Bench", "RDL").map { name -> seeded.first { it.name == name }.id }
    ExercisePickerContent("Add to Monday", seeded + own, recent, onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {}, initialMuscle = muscle)
}

/** The real guide content for the squat, in a stand-in for the bottom sheet it opens in. */
@Composable
fun FormGuideShowcase() = SheetShowcase { FormGuideContent("Squat", onClose = {}) }

/** The real "What RPE means" content, in the same stand-in. */
@Composable
fun RpeSheetShowcase() = SheetShowcase { RpeContent(onClose = {}) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetShowcase(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(scheme.scrim.copy(alpha = 0.32f))) {
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
