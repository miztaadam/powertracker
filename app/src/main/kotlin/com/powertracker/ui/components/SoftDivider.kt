package com.powertracker.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.powertracker.ui.theme.PowertrackerTheme

/** How strongly a row divider draws the hairline (DESIGN.md: row dividers at reduced strength, 60%). */
const val DIVIDER_STRENGTH = 0.6f

/** The line between rows (and along a bar's edge): the hairline at [DIVIDER_STRENGTH]. Cards have no border. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) =
    HorizontalDivider(modifier, color = PowertrackerTheme.colors.hairline.copy(alpha = DIVIDER_STRENGTH))
