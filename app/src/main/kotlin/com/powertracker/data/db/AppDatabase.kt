package com.powertracker.data.db

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class, SessionEntity::class, SetEntity::class,
        PlanItemEntity::class, AdviceEntity::class, BaselineEntity::class,
    ],
    version = 3,
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

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "powertracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(SeedCallback)
                .build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(SeedCallback)
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

private const val CONFLICT_ABORT = android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
