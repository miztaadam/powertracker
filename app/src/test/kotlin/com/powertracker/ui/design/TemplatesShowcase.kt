package com.powertracker.ui.design

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.ui.plan.builder.REPLACE
import com.powertracker.ui.plan.templates.REPLACE_KEPT
import com.powertracker.ui.plan.templates.TemplateStep
import com.powertracker.ui.plan.templates.TemplatesActions
import com.powertracker.ui.plan.templates.TemplatesContent
import com.powertracker.ui.plan.templates.TemplatesState
import com.powertracker.ui.plan.templates.replaceTitle
import kotlinx.datetime.DayOfWeek

// Choosing a plan template (1.14): the list, a draft on each side and the confirm.

@Composable
fun TemplatesListShowcase(mode: TrainingMode = TrainingMode.POWERLIFTING) {
    TemplatesContent(TemplatesState(mode, loading = false), TemplatesActions())
}

/** [id]'s draft on its suggested days for a lifter training Monday, Wednesday and Friday, strict press off. */
private fun draftState(id: String): TemplatesState {
    val t = PlanTemplates.byId(id)!!
    val days = PlanTemplates.suggestedDays(t.dayCount, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
    return TemplatesState(
        t.mode, loading = false, step = TemplateStep.DRAFT, chosen = t, days = days.toSet(),
        draft = PlanTemplates.draft(t, days, strictPress = false), hasPlan = true,
    )
}

@Composable
fun TemplateDraftShowcase(id: String) {
    TemplatesContent(draftState(id), TemplatesActions())
}

/** The confirm over the 4-day powerlifting draft: the real words, in a stand-in for the AlertDialog window. */
@Composable
fun TemplateConfirmShowcase() {
    DialogShowcase(behind = { TemplateDraftShowcase("pl-4") }, title = replaceTitle(TrainingMode.POWERLIFTING), confirmText = REPLACE) {
        Text(REPLACE_KEPT)
    }
}
