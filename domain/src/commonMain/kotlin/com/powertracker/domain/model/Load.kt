package com.powertracker.domain.model

/**
 * What a set's number means: kilograms, or the numbered plate a stack machine shows. The names are stored in
 * databases, backups and advice payloads: new values go last.
 */
enum class LoadUnit { KG, PLATES }

/**
 * An exercise's plate setting: off (kg), whole plates, or half steps too. The names are stored in databases and
 * backups: new values go last.
 */
enum class PlateCount { OFF, WHOLE, HALF }
