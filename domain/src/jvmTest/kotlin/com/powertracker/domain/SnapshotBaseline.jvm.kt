package com.powertracker.domain

internal actual fun snapshotBaseline(name: String): String? =
    object {}.javaClass.getResource("/snapshots/$name.txt")?.readText()
