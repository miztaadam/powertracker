package com.powertracker.data.db

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executor

@Database(
    entities = [
        ExerciseEntity::class, SessionEntity::class, SetEntity::class,
        PlanItemEntity::class, AdviceEntity::class, BaselineEntity::class, CheckInEntity::class, SkipEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun sessions(): SessionDao
    abstract fun sets(): SetDao
    abstract fun plan(): PlanDao
    abstract fun advice(): AdviceDao
    abstract fun baselines(): BaselineDao
    abstract fun checkIns(): CheckInDao
    abstract fun skips(): SkipDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "powertracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(SeedCallback)
                .build()

        /**
         * For tests. [queryExecutor] runs every query and transaction when given, so a test can wait for the ones
         * still running before it closes the database; the app's database is never closed.
         */
        fun inMemory(context: Context, queryExecutor: Executor? = null): AppDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(SeedCallback)
                .apply { if (queryExecutor != null) setQueryExecutor(queryExecutor) }
                .build()
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val converters = Converters()
            for (e in SeedLibrary.exercises()) {
                val values = ContentValues().apply {
                    put("name", e.name)
                    put("category", converters.categoryToDb(e.category))
                    put("parentLift", converters.liftToDb(e.parentLift))
                    put("muscles", converters.stringsToDb(e.muscles))
                    put("isBuiltIn", if (e.isBuiltIn) 1 else 0)
                    put("archived", if (e.archived) 1 else 0)
                    // plateCount is left to its column default, off (version 6).
                }
                db.insert("exercise", CONFLICT_ABORT, values)
            }
        }
    }
}

/**
 * Version 2, the strict press release: no table changes, only data ([StrictPressUpgrade]). Room runs it in a
 * transaction, so a failure leaves version 1 as it was. Never pair this database with a destructive fallback: it is the
 * lifter's training log.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) = StrictPressUpgrade.run(db)
}

/**
 * Version 3, bodybuilding mode: plan items, sessions and advice know their side. Every row already stored is on the
 * powerlifting side and has fixed reps (`repsHigh` null). Columns are only added, so nothing is lost.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_item ADD COLUMN mode TEXT NOT NULL DEFAULT 'POWERLIFTING'")
        db.execSQL("ALTER TABLE plan_item ADD COLUMN repsHigh INTEGER")
        db.execSQL("ALTER TABLE session ADD COLUMN mode TEXT NOT NULL DEFAULT 'POWERLIFTING'")
        db.execSQL("ALTER TABLE advice ADD COLUMN mode TEXT NOT NULL DEFAULT 'POWERLIFTING'")
    }
}

/** Version 4, stage 3: the bodybuilding check-ins, in a new table. Nothing already stored changes. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `check_ins` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, " +
                "`lagging` TEXT NOT NULL, `growingWell` TEXT NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_check_ins_date` ON `check_ins` (`date`)")
    }
}

/** Version 5 (1.13): skipped planned exercises, in a new table. Nothing already stored changes. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `skips` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, " +
                "`mode` TEXT NOT NULL, `exerciseId` INTEGER NOT NULL, `planItemId` INTEGER, `reason` TEXT, " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`planItemId`) REFERENCES `plan_item`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_skips_date_mode_exerciseId` ON `skips` (`date`, `mode`, `exerciseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_skips_exerciseId` ON `skips` (`exerciseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_skips_planItemId` ON `skips` (`planItemId`)")
    }
}

/**
 * Version 6 (1.17): counting plates on stack machines. Each set gains its unit and each exercise its plate setting,
 * as columns with defaults: every row already stored reads kg and off, and nothing stored changes.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE logged_set ADD COLUMN unit TEXT NOT NULL DEFAULT 'KG'")
        db.execSQL("ALTER TABLE exercise ADD COLUMN plateCount TEXT NOT NULL DEFAULT 'OFF'")
    }
}

/**
 * Version 7 (1.24): supersets. Each plan item gains the flag that links it to the item above it, as a column with a
 * default: every item already stored reads as not linked, and nothing stored changes.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_item ADD COLUMN supersetWithAbove INTEGER NOT NULL DEFAULT 0")
    }
}

private const val CONFLICT_ABORT = android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
