package com.powertracker.domain.library.focus

import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.Sources

/** BodyArea.CALVES and BodyArea.CORE: each exercise's stretch and squeeze, keyed by catalogue name. */
internal val calvesCoreFocus: List<Pair<String, MuscleFocus>> by lazy {
    listOf<Pair<String, MuscleFocus>>(
        // A hold: the abs keep the trunk still, so there is nothing to stretch or squeeze.
        "Front plank" to MuscleFocus.NoFocus(listOf(Sources.ACE_FRONT_PLANK, Sources.EXRX_FRONT_PLANK)),
    )
}
