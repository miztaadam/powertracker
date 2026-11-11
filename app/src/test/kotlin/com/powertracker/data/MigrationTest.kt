package com.powertracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.powertracker.data.db.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun schemaV1_opens() {
        helper.createDatabase("migration-test", 1).close()
        // Future versions: helper.runMigrationsAndValidate("migration-test", 2, true, MIGRATION_1_2)
    }
}
