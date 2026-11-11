package com.powertracker.domain

/**
 * The [CoachSnapshot] text captured at BASE (ff633da) for [name] ("golden" or "mixed"), so a failure can show the
 * first line that changed. The JVM reads it from the test resources; elsewhere it is null and only the hash is checked.
 */
internal expect fun snapshotBaseline(name: String): String?
