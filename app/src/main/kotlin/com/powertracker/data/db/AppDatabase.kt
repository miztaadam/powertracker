package com.powertracker.data.db

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class, SessionEntity::class, SetEntity::class,
        PlanItemEntity::class, AdviceEntity::class, BaselineEntity::class,
    ],
    version = 1,
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

private const val CONFLICT_ABORT = android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT
