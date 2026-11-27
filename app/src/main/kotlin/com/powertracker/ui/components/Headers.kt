package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.motion.ModeSwitchStamp
import com.powertracker.ui.theme.LocalTrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/**
 * The top of every screen: small upper-case eyebrow, a big condensed title, actions on the right. The six top-level
 * screens pass [modeLabel], which shows [ModeLabel] under the eyebrow on the bodybuilding side.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    modeLabel: Boolean = false,
    navigation: (@Composable () -> Unit)? = null,
    /** One line by default; a screen whose titles are names it does not choose (a template's) lets them wrap. */
    titleMaxLines: Int = 1,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 72.dp).padding(start = if (navigation != null) Space.xs else Space.gutter, end = Space.s, top = Space.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigation?.invoke()
        Column(Modifier.weight(1f).padding(start = if (navigation != null) Space.xs else 0.dp)) {
            // Whole at any text size: on a small phone at large text a date or "Weekly template" takes a second line (1.24).
            if (eyebrow != null) Eyebrow(eyebrow, maxLines = Int.MAX_VALUE)
            if (modeLabel) ModeLabel(Modifier.padding(top = Space.xxs, bottom = Space.xs))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = titleMaxLines,
                modifier = Modifier.semantics { heading() },
            )
            // The hammer drop's POWERLIFTING stamp (1.15), just below the title so it never covers it. It takes no
            // height: the header keeps its size, and the stamp is drawn over whatever is below.
            if (modeLabel && LocalTrainingMode.current != TrainingMode.BODYBUILDING) {
                ModeSwitchStamp(Modifier.takesNoHeight().padding(top = Space.xxs))
            }
        }
        actions()
    }
}

/** Names a group of content inside a screen, with an optional action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(title, Modifier.weight(1f).semantics { heading() }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

/**
 * The small BODYBUILDING pill under a top-level screen's date or overline (bodybuilding mode spec §7.2). Drawn only on
 * the bodybuilding side, so the powerlifting side is unchanged. A screen reader hears it as "Bodybuilding mode".
 */
@Composable
fun ModeLabel(modifier: Modifier = Modifier) {
    if (LocalTrainingMode.current != TrainingMode.BODYBUILDING) return
    val pt = PowertrackerTheme.colors
    Text(
        "BODYBUILDING",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
        color = pt.onAccentContainer,
        maxLines = 1,
        modifier = modifier
            .testTag("mode-label")
            .background(pt.accentContainer, CircleShape)
            .padding(horizontal = Space.s, vertical = 2.dp)
            .semantics { contentDescription = "Bodybuilding mode" },
    )
}

/** Measured and drawn as it is, but its parent is told it is 0 tall. */
private fun Modifier.takesNoHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, 0) { placeable.place(0, 0) }
}
