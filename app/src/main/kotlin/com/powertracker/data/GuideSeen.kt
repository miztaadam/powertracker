package com.powertracker.data

import com.powertracker.data.db.PlanDao
import com.powertracker.data.db.SessionDao

/*
 * The guide's first launch (1.16). "Seen" itself is a key of the settings store ([SettingsRepository.guideSeen]): the
 * store allows one instance per file, and the key lives outside AppSettings, so a restore (which rewrites only the
 * AppSettings keys) keeps it and a backup never carries it.
 */

/** A new user: no session and no plan item on either side. Two EXISTS queries, nothing else read. */
suspend fun isNewUser(sessions: SessionDao, plans: PlanDao): Boolean = !sessions.any() && !plans.any()
