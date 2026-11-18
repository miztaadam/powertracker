package com.powertracker.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.rules.ExternalResource

/**
 * An in-memory [AppDatabase] for one test, closed only once nothing is reading it.
 *
 * Closing it while a view model's flow is still querying fails the test with "the connection pool has been closed"
 * (the app never closes its database, so only tests can do this). Two things prevent that:
 * - Order: make this rule the outermost (`@get:Rule(order = 0)`, the compose rule after it), so the compose rule has
 *   destroyed the activity, which disposes the composition and clears its view models, before this rule closes the
 *   database. A test that makes its own view models cancels their scopes in `@After`, which runs before this rule too.
 * - Draining: every query runs on this rule's own threads. Cancelling a coroutine does not stop a query already
 *   running there, so [after] stops taking new work and waits for what is running to finish, then closes.
 */
class TestDatabase : ExternalResource() {
    private var executor: ExecutorService? = null
    private var opened: AppDatabase? = null

    val db: AppDatabase get() = checkNotNull(opened) { "the database opens when the test starts" }

    private val running = AtomicInteger()

    /** Queries and writes handed to this rule's threads and not yet finished. */
    val inFlight: Int get() = running.get()

    override fun before() {
        val threads = Executors.newCachedThreadPool { work -> Thread(work, "test-database").apply { isDaemon = true } }
        executor = threads
        val counted = java.util.concurrent.Executor { work ->
            running.incrementAndGet()
            try {
                threads.execute {
                    try {
                        work.run()
                    } finally {
                        running.decrementAndGet()
                    }
                }
            } catch (e: java.util.concurrent.RejectedExecutionException) {
                running.decrementAndGet()
                throw e
            }
        }
        opened = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>(), counted)
    }

    override fun after() {
        val threads = executor ?: return
        threads.shutdown()
        check(threads.awaitTermination(10, TimeUnit.SECONDS)) { "a query was still running 10 s after the test" }
        opened?.close()
    }
}
