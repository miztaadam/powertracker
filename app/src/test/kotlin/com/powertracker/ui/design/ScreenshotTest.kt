package com.powertracker.ui.design

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.Tab
import com.powertracker.ui.guide.GuideScreen
import com.powertracker.ui.library.CatalogueThumbnails
import com.powertracker.ui.lock.LockScreen
import com.powertracker.ui.plan.builder.BuilderStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the mockups (dark and light) from the real components.
 *
 * A normal test run is a smoke check: every screen must render, and the PNGs go to
 * `app/build/screenshots/`. The committed copies in `docs/design/screens/` are only rewritten with
 * `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`.
 *
 * The Today and Coach mockups render the real `TodayContent` / `CoachContent` with sample state
 * (Today: mid-session, before the session, an exercise finished, after Finish; Coach: the feed, the
 * empty feed, and the rarer re-entry and miss-point cards). Progress and lift history render the
 * real `ProgressContent` / `LiftHistoryContent` from a sample training history, Progress once more
 * with the strict press switch on. Settings is captured with that switch off and on, and with bodybuilding mode on; Today, Plan, Coach (with the welcome-back card) and Progress have bodybuilding captures too, in the blue look. The exercise picker
 * is the real `ExercisePickerContent`, on its muscle grid and inside one muscle; the form guide is the real `FormGuideContent` in a stand-in
 * for the bottom sheet, for the squat, the bench press and the lat pulldown, each with its two-pose picture, and the squat once more with bodybuilding mode on, in the blue look with its stretch and squeeze section (the sheet itself is its own window, which this capture cannot see). The Exercises tab is
 * the real `ExercisesContent`; its Muscles drop-down, the guide with its actions and the day chooser are their
 * real content in the same stand-in, the first and last over the tab. Plan's add form, on both sides, is the real
 * `PlanItemFields` in a stand-in for its AlertDialog window, over the Plan screen. Today, Plan (and its add form), the Exercises captures,
 * the exercise picker,
 * lift history, the empty Coach feed and the two lock screens are phone size (412 × 915 dp); the
 * other Coach captures, Progress, Settings, the four form guides and the RPE sheet are full-page captures of a scrolling screen, so
 * their height differs. The launcher icon is rendered by [LauncherIconTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    // Unit tests run with the module directory (app/) as the working directory.
    private val outDir = File(if (System.getProperty("screenshots.update") == "true") "../docs/design/screens" else "build/screenshots").apply { mkdirs() }

    /**
     * The Exercises tab works its thumbnails out in the background, so a capture could race the worker. Every one is
     * made ready first: each capture then shows them drawn, from the first frame, as a lifter sees them once the
     * warm-up is done.
     */
    @Before
    fun thumbnailsReady() = runBlocking {
        for (exercise in Library.all()) CatalogueThumbnails.load(exercise.name)
    }

    private fun render(name: String, tab: Tab?, mode: TrainingMode = TrainingMode.POWERLIFTING, content: @Composable () -> Unit) {
        var dark by mutableStateOf(true)
        compose.setContent { ShowcaseFrame(dark, tab, mode, content) }
        for (mode in listOf(true, false)) {
            dark = mode
            compose.waitForIdle()
            // Not captureToImage(): under Robolectric it waits for a frame-drawn callback that never
            // comes and times out. Drawing the Compose root view into a bitmap gives the same pixels
            // with native graphics.
            val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            assertEquals("$name should be 412 dp wide at 2x", 824, bitmap.width)
            // Something was actually drawn: the frame is not one flat colour.
            val corner = bitmap.getPixel(0, 0)
            val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
            assertTrue("$name rendered blank", pixels.count { it != corner } > pixels.size / 20)
            val file = File(outDir, "$name-${if (mode) "dark" else "light"}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun today() = render("today", Tab.TODAY) { TodayShowcase() }

    @Test
    fun todayStart() = render("today-start", Tab.TODAY) { TodayStartShowcase() }

    @Test
    fun todayNext() = render("today-next", Tab.TODAY) { TodayNextShowcase() }

    @Test
    fun todaySummary() = render("today-summary", Tab.TODAY) { TodaySummaryShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1440dp-xhdpi")
    fun coach() = render("coach", Tab.COACH) { CoachShowcase() }

    @Test
    fun coachEmpty() = render("coach-empty", Tab.COACH) { CoachEmptyShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1000dp-xhdpi")
    fun coachMore() = render("coach-more", Tab.COACH) { CoachMoreShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1830dp-xhdpi")
    fun progress() = render("progress", Tab.PROGRESS) { ProgressShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h2040dp-xhdpi")
    fun progressStrictPress() = render("progress-strict-press", Tab.PROGRESS) { ProgressShowcase(strictPress = true) }

    @Test
    fun liftHistory() = render("lift-history", null) { LiftHistoryShowcase() }

    @Test
    fun plan() = render("plan", Tab.PLAN) { PlanShowcase() }

    @Test
    fun planAddForm() = render("plan-add-form", Tab.PLAN) { PlanAddFormShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1400dp-xhdpi")
    fun settings() = render("settings", null) { SettingsShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1400dp-xhdpi")
    fun settingsStrictPress() = render("settings-strict-press", null) { SettingsShowcase(strictPress = true) }

    // The bodybuilding side (bodybuilding mode spec §7): its blue look and label, its own Today, Plan, Coach and Progress.
    @Test
    fun todayBodybuilding() = render("today-bodybuilding", Tab.TODAY, TrainingMode.BODYBUILDING) { TodayBodybuildingShowcase() }

    @Test
    fun planBodybuilding() = render("plan-bodybuilding", Tab.PLAN, TrainingMode.BODYBUILDING) { PlanBodybuildingShowcase() }

    @Test
    fun planAddFormBodybuilding() = render("plan-add-form-bodybuilding", Tab.PLAN, TrainingMode.BODYBUILDING) { PlanAddFormShowcase(TrainingMode.BODYBUILDING) }

    @Test
    fun coachBodybuilding() = render("coach-bodybuilding", Tab.COACH, TrainingMode.BODYBUILDING) { CoachBodybuildingShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1456dp-xhdpi")
    fun progressBodybuilding() = render("progress-bodybuilding", Tab.PROGRESS, TrainingMode.BODYBUILDING) { ProgressBodybuildingShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1330dp-xhdpi")
    fun settingsBodybuilding() = render("settings-bodybuilding", null, TrainingMode.BODYBUILDING) {
        SettingsShowcase(bodybuilding = true)
    }

    // Stage 3: the check-in's three steps, and Coach with the check-in due and the two new cards.
    @Test
    fun checkInLagging() = render("checkin-lagging-bodybuilding", null, TrainingMode.BODYBUILDING) { CheckInLaggingShowcase() }

    @Test
    fun checkInGrowing() = render("checkin-growing-bodybuilding", null, TrainingMode.BODYBUILDING) { CheckInGrowingShowcase() }

    @Test
    fun checkInSaved() = render("checkin-saved-bodybuilding", null, TrainingMode.BODYBUILDING) { CheckInSavedShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1150dp-xhdpi")
    fun coachCheckIn() = render("coach-checkin-bodybuilding", Tab.COACH, TrainingMode.BODYBUILDING) { CoachCheckInShowcase() }

    // The bodybuilding side's plan builder (stage 2): its four steps.
    @Test
    fun planBuilderDays() = render("plan-builder-days-bodybuilding", null, TrainingMode.BODYBUILDING) { PlanBuilderShowcase(BuilderStep.DAYS) }

    @Test
    fun planBuilderExercises() = render("plan-builder-exercises-bodybuilding", null, TrainingMode.BODYBUILDING) { PlanBuilderShowcase(BuilderStep.EXERCISES) }

    @Test
    fun planBuilderBodyParts() = render("plan-builder-body-parts-bodybuilding", null, TrainingMode.BODYBUILDING) { PlanBuilderShowcase(BuilderStep.BODY_PARTS) }

    @Test
    @Config(qualifiers = "w412dp-h3330dp-xhdpi")
    fun planBuilderDraft() = render("plan-builder-draft-bodybuilding", null, TrainingMode.BODYBUILDING) { PlanBuilderShowcase(BuilderStep.DRAFT) }

    // 1.14, plan templates: the list, a draft on each side and the confirm.
    @Test
    fun templatesList() = render("templates-list", null) { TemplatesListShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1720dp-xhdpi")
    fun templateDraft() = render("template-draft", null) { TemplateDraftShowcase("pl-4") }

    @Test
    @Config(qualifiers = "w412dp-h3160dp-xhdpi")
    fun templateDraftBodybuilding() = render("template-draft-bodybuilding", null, TrainingMode.BODYBUILDING) { TemplateDraftShowcase("bb-4") }

    @Test
    fun templateConfirm() = render("template-confirm", null) { TemplateConfirmShowcase() }

    @Test
    fun exercisePicker() = render("exercise-picker", null) { ExercisePickerShowcase() }

    @Test
    fun exercisePickerMuscle() = render("exercise-picker-muscle", null) { ExercisePickerShowcase(MuscleGroup.QUADS) }

    @Test
    fun exercises() = render("exercises", Tab.EXERCISES) { ExercisesShowcase() }

    @Test
    fun exercisesMuscles() = render("exercises-muscles", null) { ExercisesMusclesShowcase() }

    @Test
    fun exerciseGuideActions() = render("exercise-guide-actions", null) { GuideActionsShowcase() }

    @Test
    fun exerciseDayChooser() = render("exercise-day-chooser", null) { DayChooserShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1480dp-xhdpi")
    fun formGuide() = render("form-guide", null) { FormGuideShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1370dp-xhdpi")
    fun formGuidePicture() = render("form-guide-picture", null) { FormGuidePictureShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1370dp-xhdpi")
    fun formGuideLatPulldown() = render("form-guide-lat-pulldown", null) { FormGuideLatPulldownShowcase() }

    // The squat's guide with bodybuilding mode on: its "Stretch and squeeze" section after Cues (stage 4).
    @Test
    @Config(qualifiers = "w412dp-h1640dp-xhdpi")
    fun formGuideBodybuilding() = render("form-guide-bodybuilding", null, TrainingMode.BODYBUILDING) { FormGuideShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h560dp-xhdpi")
    fun rpeSheet() = render("rpe-sheet", null) { RpeSheetShowcase() }

    // 1.13, skipping a planned exercise: the row menu, the reason sheet, the "Skipped today" line and the SKIP cards.
    @Test
    fun todaySkipMenu() = render("today-skip-menu", Tab.TODAY) { TodaySkipMenuShowcase() }

    @Test
    fun todaySkipMenuBodybuilding() = render("today-skip-menu-bodybuilding", Tab.TODAY, TrainingMode.BODYBUILDING) { TodaySkipMenuShowcase(TrainingMode.BODYBUILDING) }

    @Test
    fun todaySkipSheet() = render("today-skip-sheet", Tab.TODAY) { TodaySkipSheetShowcase() }

    @Test
    fun todaySkipSheetBodybuilding() = render("today-skip-sheet-bodybuilding", Tab.TODAY, TrainingMode.BODYBUILDING) { TodaySkipSheetShowcase(TrainingMode.BODYBUILDING) }

    @Test
    fun todaySkipped() = render("today-skipped", Tab.TODAY) { TodaySkippedShowcase() }

    @Test
    fun todaySkippedBodybuilding() = render("today-skipped-bodybuilding", Tab.TODAY, TrainingMode.BODYBUILDING) { TodaySkippedShowcase(TrainingMode.BODYBUILDING) }

    @Test
    fun coachSkip() = render("coach-skip", Tab.COACH) { CoachSkipShowcase() }

    @Test
    fun coachSkipBodybuilding() = render("coach-skip-bodybuilding", Tab.COACH, TrainingMode.BODYBUILDING) { CoachSkipShowcase(TrainingMode.BODYBUILDING) }

    @Test
    fun lock() = render("lock", null) { LockScreen(noDeviceLock = false, onUnlock = {}, onContinue = {}) }

    @Test
    fun lockNoScreenLock() = render("lock-no-screen-lock", null) { LockScreen(noDeviceLock = true, onUnlock = {}, onContinue = {}) }

    /** The guide's screen 4 at its last step (motion off shows it), on each side (1.16). */
    @Test
    fun guideTodayPowerlifting() = render("guide-today-powerlifting", null) { GuideTodayShowcase(TrainingMode.POWERLIFTING) }

    @Test
    fun guideTodayBodybuilding() = render("guide-today-bodybuilding", null, TrainingMode.BODYBUILDING) { GuideTodayShowcase(TrainingMode.BODYBUILDING) }
}

@Composable
private fun GuideTodayShowcase(mode: TrainingMode) =
    GuideScreen(mode, firstLaunch = false, hasPlan = false, motionOn = false, onPickSide = {}, onClose = {}, startAt = GuideScreenId.TODAY)
