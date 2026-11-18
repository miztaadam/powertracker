package com.powertracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.MIGRATION_1_2
import com.powertracker.data.db.MIGRATION_2_3
import com.powertracker.data.db.MIGRATION_3_4
import com.powertracker.data.db.MIGRATION_4_5
import com.powertracker.data.db.StrictPressUpgrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migrations run against a real version-1 database built from the committed schema (app/schemas), with rows
 * written the way version 1 stored them: enums as names, booleans as 0/1, lists as JSON.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val name = "migration-test"

    private companion object {
        /** The name [AppDatabase.build] opens. */
        const val APP_DB = "powertracker.db"
    }

    @Test
    fun schemaV1_opens() {
        helper.createDatabase(name, 1).close()
    }

    /** Strict press spec §4.1: the catalogue's former "Overhead press" row becomes the main lift; its sets stay on it. */
    @Test
    fun upgrade_overheadPressRow_becomesStrictPress_keepsSets() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            exercise(db, 4, "Overhead press", "ACCESSORY", builtIn = false)
            session(db, 1, 20_400)
            set(db, 1, 1, 4, 60.0)
            set(db, 2, 1, 4, 62.5)
            set(db, 3, 1, 1, 140.0)
            planItem(db, 1, 4)
        }

        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)

        assertEquals(
            listOf(
                Row(1, "Squat", "SQUAT", true, false),
                Row(2, "Bench", "BENCH", true, false),
                Row(3, "Deadlift", "DEADLIFT", true, false),
                Row(4, "Strict press", "STRICT_PRESS", true, false),
            ),
            exercises(db),
        )
        assertEquals(listOf(1L to 4L, 2L to 4L, 3L to 1L), sets(db))
        assertEquals(listOf(1L to 4L), planItems(db))
        db.close()
    }

    /**
     * Review focus 3: the lifter's own "Strict press" and the catalogue's "Overhead press" both exist. The oldest becomes
     * the main lift, the other is re-categorised under its own name, and every set and plan item stays where it was.
     */
    @Test
    fun upgrade_twoMatchingRows_keepsAllHistory() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            exercise(db, 4, "Overhead press", "ACCESSORY", builtIn = true)
            exercise(db, 5, "Strict press", "ACCESSORY", builtIn = false)
            exercise(db, 6, "OHP", "VARIATION", builtIn = false, archived = true)
            exercise(db, 7, "military PRESS", "ACCESSORY", builtIn = false)
            session(db, 1, 20_400)
            session(db, 2, 20_407)
            set(db, 1, 1, 4, 60.0)
            set(db, 2, 1, 5, 57.5)
            set(db, 3, 2, 6, 55.0)
            set(db, 4, 2, 7, 50.0)
            planItem(db, 1, 4)
            planItem(db, 2, 5)
        }

        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)

        assertEquals(
            listOf(
                Row(1, "Squat", "SQUAT", true, false),
                Row(2, "Bench", "BENCH", true, false),
                Row(3, "Deadlift", "DEADLIFT", true, false),
                Row(4, "Strict press", "STRICT_PRESS", true, false),
                Row(5, "Strict press", "STRICT_PRESS", false, false),
                // Archived rows are left alone.
                Row(6, "OHP", "VARIATION", false, true),
                Row(7, "military PRESS", "STRICT_PRESS", false, false),
            ),
            exercises(db),
        )
        assertEquals(listOf(1L to 4L, 2L to 5L, 3L to 6L, 4L to 7L), sets(db))
        assertEquals(listOf(1L to 4L, 2L to 5L), planItems(db))
        db.close()
    }

    @Test
    fun upgrade_noMatch_noChange() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            exercise(db, 4, "Push press", "ACCESSORY", builtIn = false)
            exercise(db, 5, "DB shoulder press", "ACCESSORY", builtIn = true)
            exercise(db, 6, "Overhead press machine", "ACCESSORY", builtIn = false)
            session(db, 1, 20_400)
            set(db, 1, 1, 4, 70.0)
        }
        val before = helper.createDatabase("$name-copy", 1).use { copy ->
            seedMainLifts(copy)
            exercise(copy, 4, "Push press", "ACCESSORY", builtIn = false)
            exercise(copy, 5, "DB shoulder press", "ACCESSORY", builtIn = true)
            exercise(copy, 6, "Overhead press machine", "ACCESSORY", builtIn = false)
            exercises(copy)
        }

        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)

        assertEquals(before, exercises(db))
        assertEquals(listOf(1L to 4L), sets(db))
        db.close()
    }

    @Test
    fun upgrade_twice_isIdempotent() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            exercise(db, 4, "Strict press", "ACCESSORY", builtIn = false)
            exercise(db, 5, "Overhead press", "ACCESSORY", builtIn = true)
            session(db, 1, 20_400)
            set(db, 1, 1, 4, 60.0)
            set(db, 2, 1, 5, 60.0)
        }
        val db = helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)
        val once = exercises(db)

        StrictPressUpgrade.run(db)

        assertEquals(once, exercises(db))
        assertEquals(
            listOf(Row(4, "Strict press", "STRICT_PRESS", true, false), Row(5, "Overhead press", "STRICT_PRESS", true, false)),
            once.drop(3),
        )
        assertEquals(listOf(1L to 4L, 2L to 5L), sets(db))
        db.close()
    }

    @Test
    fun schemaV2_opens() {
        helper.createDatabase(name, 2).close()
    }

    /**
     * Bodybuilding mode spec §6: a version 2 database keeps every row, and every plan item, session and advice lands on
     * the powerlifting side with fixed reps.
     */
    @Test
    fun upgrade2To3_keepsEveryRow_stampsPowerlifting() {
        helper.createDatabase(name, 2).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            session(db, 2, 20_407)
            set(db, 1, 1, 1, 140.0)
            set(db, 2, 1, 2, 100.0)
            set(db, 3, 2, 3, 180.0)
            planItem(db, 1, 1)
            planItem(db, 2, 2)
            planItem(db, 3, 3)
            advice(db, 1, "TARGET", "OPEN")
            advice(db, 2, "DELOAD", "DISMISSED")
        }

        val db = helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3)

        assertEquals(listOf(1L to 1L, 2L to 2L, 3L to 3L), planItems(db))
        assertEquals(listOf(1L to 1L, 2L to 1L, 3L to 2L), pairs(db, "SELECT id, sessionId FROM logged_set ORDER BY id"))
        assertEquals(listOf(1L to 20_400L, 2L to 20_407L), pairs(db, "SELECT id, date FROM session ORDER BY id"))
        assertEquals(listOf(1L, 2L), ids(db, "SELECT id FROM advice ORDER BY id"))
        for (table in listOf("plan_item", "session", "advice")) {
            assertEquals(table, listOf("POWERLIFTING"), strings(db, "SELECT DISTINCT mode FROM $table"))
        }
        db.query("SELECT repsHigh FROM plan_item").use { c -> while (c.moveToNext()) assertNull(if (c.isNull(0)) null else c.getInt(0)) }
        // A row written after the upgrade without a mode still gets the powerlifting side from the column default.
        db.execSQL("INSERT INTO session (id, date, bodyweightKg, readiness) VALUES (3, 20414, NULL, NULL)")
        assertEquals(listOf("POWERLIFTING"), strings(db, "SELECT mode FROM session WHERE id = 3"))
        db.close()
    }

    /** A version 1 database still reaches version 3 through both upgrades in turn. */
    @Test
    fun upgrade1To3_runsBothSteps() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            exercise(db, 4, "Overhead press", "ACCESSORY", builtIn = false)
            session(db, 1, 20_400)
            set(db, 1, 1, 4, 60.0)
            planItem(db, 1, 4)
        }

        val db = helper.runMigrationsAndValidate(name, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        assertEquals(Row(4, "Strict press", "STRICT_PRESS", true, false), exercises(db).last())
        assertEquals(listOf(1L to 4L), sets(db))
        assertEquals(listOf("POWERLIFTING"), strings(db, "SELECT mode FROM plan_item"))
        assertEquals(listOf("POWERLIFTING"), strings(db, "SELECT mode FROM session"))
        db.close()
    }

    @Test
    fun schemaV3_opens() {
        helper.createDatabase(name, 3).close()
    }

    /** Stage 3: a version 3 database keeps every row and gains an empty check-in table. */
    @Test
    fun upgrade3To4_keepsEveryRow_addsAnEmptyCheckInTable() {
        helper.createDatabase(name, 3).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            set(db, 1, 1, 1, 140.0)
            planItem(db, 1, 1)
            advice(db, 1, "TARGET", "OPEN")
            db.execSQL("UPDATE session SET mode = 'BODYBUILDING' WHERE id = 1")
        }

        val db = helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4)

        assertEquals(3, exercises(db).size)
        assertEquals(listOf(1L to 1L), sets(db))
        assertEquals(listOf(1L to 1L), planItems(db))
        assertEquals(listOf(1L), ids(db, "SELECT id FROM advice"))
        assertEquals(listOf("BODYBUILDING"), strings(db, "SELECT mode FROM session"))
        assertEquals(emptyList<Long>(), ids(db, "SELECT id FROM check_ins"))
        // One check-in per date.
        db.execSQL("INSERT INTO check_ins (date, lagging, growingWell) VALUES (20450, 'CHEST', '')")
        val second = runCatching { db.execSQL("INSERT INTO check_ins (date, lagging, growingWell) VALUES (20450, '', 'QUADS')") }
        assertEquals(true, second.isFailure)
        db.close()
    }

    /** A version 1 database still reaches version 4 through every upgrade in turn. */
    @Test
    fun upgrade1To4_runsEveryStep() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            set(db, 1, 1, 1, 140.0)
        }

        val db = helper.runMigrationsAndValidate(name, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        assertEquals(listOf(1L to 1L), sets(db))
        assertEquals(listOf("POWERLIFTING"), strings(db, "SELECT mode FROM session"))
        db.close()
    }

    @Test
    fun schemaV4_opens() {
        helper.createDatabase(name, 4).close()
    }

    /** 1.13: a version 4 database keeps every row and gains an empty skips table, one skip per exercise, date and side. */
    @Test
    fun upgrade4To5_keepsEveryRow_addsAnEmptySkipsTable() {
        helper.createDatabase(name, 4).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            set(db, 1, 1, 1, 140.0)
            planItem(db, 1, 1)
            advice(db, 1, "TARGET", "OPEN")
            db.execSQL("UPDATE session SET mode = 'BODYBUILDING' WHERE id = 1")
            db.execSQL("INSERT INTO check_ins (date, lagging, growingWell) VALUES (20450, 'CHEST', '')")
        }

        val db = helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5)

        assertEquals(3, exercises(db).size)
        assertEquals(listOf(1L to 1L), sets(db))
        assertEquals(listOf(1L to 1L), planItems(db))
        assertEquals(listOf(1L), ids(db, "SELECT id FROM advice"))
        assertEquals(listOf("BODYBUILDING"), strings(db, "SELECT mode FROM session"))
        assertEquals(listOf(20_450L), ids(db, "SELECT date FROM check_ins"))
        assertEquals(emptyList<Long>(), ids(db, "SELECT id FROM skips"))
        db.execSQL("INSERT INTO skips (date, mode, exerciseId, planItemId, reason) VALUES (20450, 'BODYBUILDING', 1, 1, 'PAIN')")
        db.execSQL("INSERT INTO skips (date, mode, exerciseId, planItemId, reason) VALUES (20450, 'POWERLIFTING', 1, NULL, NULL)")
        val second = runCatching { db.execSQL("INSERT INTO skips (date, mode, exerciseId, planItemId, reason) VALUES (20450, 'BODYBUILDING', 1, NULL, 'BUSY')") }
        assertEquals(true, second.isFailure)
        assertEquals(listOf("PAIN"), strings(db, "SELECT reason FROM skips WHERE mode = 'BODYBUILDING'"))
        db.close()
    }

    /** The app's own database (not only this helper) carries the 4 to 5 step: a 1.12 phone opens on 1.13 with its data. */
    @Test
    fun appDatabase_opensAVersion4File() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(APP_DB)
        helper.createDatabase(APP_DB, 4).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            set(db, 1, 1, 1, 140.0)
        }

        val app = AppDatabase.build(context)
        try {
            val db = app.openHelper.writableDatabase
            assertEquals(5, db.version)
            assertEquals(listOf(1L to 1L), sets(db))
            assertEquals(emptyList<Long>(), ids(db, "SELECT id FROM skips"))
        } finally {
            app.close()
            context.deleteDatabase(APP_DB)
        }
    }

    /** A version 1 database still reaches version 5 through every upgrade in turn. */
    @Test
    fun upgrade1To5_runsEveryStep() {
        helper.createDatabase(name, 1).use { db ->
            seedMainLifts(db)
            session(db, 1, 20_400)
            set(db, 1, 1, 1, 140.0)
        }

        val db = helper.runMigrationsAndValidate(name, 5, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        assertEquals(listOf(1L to 1L), sets(db))
        assertEquals(listOf("POWERLIFTING"), strings(db, "SELECT mode FROM session"))
        assertEquals(emptyList<Long>(), ids(db, "SELECT id FROM skips"))
        db.close()
    }

    private data class Row(val id: Long, val name: String, val category: String, val builtIn: Boolean, val archived: Boolean)

    private fun seedMainLifts(db: SupportSQLiteDatabase) {
        exercise(db, 1, "Squat", "SQUAT", builtIn = true)
        exercise(db, 2, "Bench", "BENCH", builtIn = true)
        exercise(db, 3, "Deadlift", "DEADLIFT", builtIn = true)
    }

    private fun exercise(db: SupportSQLiteDatabase, id: Long, name: String, category: String, builtIn: Boolean, archived: Boolean = false) =
        db.execSQL(
            "INSERT INTO exercise (id, name, category, parentLift, muscles, isBuiltIn, archived) VALUES (?, ?, ?, NULL, '[\"Delts\"]', ?, ?)",
            arrayOf<Any>(id, name, category, if (builtIn) 1 else 0, if (archived) 1 else 0),
        )

    private fun session(db: SupportSQLiteDatabase, id: Long, epochDay: Long) =
        db.execSQL("INSERT INTO session (id, date, bodyweightKg, readiness) VALUES (?, ?, NULL, NULL)", arrayOf<Any>(id, epochDay))

    private fun set(db: SupportSQLiteDatabase, id: Long, sessionId: Long, exerciseId: Long, weightKg: Double) =
        db.execSQL(
            "INSERT INTO logged_set (id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet) VALUES (?, ?, ?, ?, 5, 8.0, 0, NULL, 1)",
            arrayOf<Any>(id, sessionId, exerciseId, weightKg),
        )

    private fun planItem(db: SupportSQLiteDatabase, id: Long, exerciseId: Long) =
        db.execSQL(
            "INSERT INTO plan_item (id, weekday, exerciseId, sets, reps, targetRpe, sortOrder) VALUES (?, 1, ?, 3, 5, 8.0, 0)",
            arrayOf<Any>(id, exerciseId),
        )

    private fun advice(db: SupportSQLiteDatabase, id: Long, type: String, status: String) =
        db.execSQL(
            "INSERT INTO advice (id, type, lift, createdAt, reason, severity, payload, status) VALUES (?, ?, NULL, 20407, 'Because', 1, '{}', ?)",
            arrayOf<Any>(id, type, status),
        )

    private fun strings(db: SupportSQLiteDatabase, sql: String): List<String> =
        db.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    private fun ids(db: SupportSQLiteDatabase, sql: String): List<Long> =
        db.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }

    private fun exercises(db: SupportSQLiteDatabase): List<Row> =
        db.query("SELECT id, name, category, isBuiltIn, archived FROM exercise ORDER BY id").use { c ->
            buildList {
                while (c.moveToNext()) add(Row(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3) == 1, c.getInt(4) == 1))
            }
        }

    private fun sets(db: SupportSQLiteDatabase): List<Pair<Long, Long>> = pairs(db, "SELECT id, exerciseId FROM logged_set ORDER BY id")

    private fun planItems(db: SupportSQLiteDatabase): List<Pair<Long, Long>> = pairs(db, "SELECT id, exerciseId FROM plan_item ORDER BY id")

    private fun pairs(db: SupportSQLiteDatabase, sql: String): List<Pair<Long, Long>> =
        db.query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0) to c.getLong(1)) } }
}
