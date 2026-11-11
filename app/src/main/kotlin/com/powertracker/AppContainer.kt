package com.powertracker

import android.content.Context
import com.powertracker.data.AdviceRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase

/** Hand-wired dependencies, created once by [PowertrackerApp]. */
class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.build(context)
    val training = TrainingRepository(db)
    val advice = AdviceRepository(db)
}
