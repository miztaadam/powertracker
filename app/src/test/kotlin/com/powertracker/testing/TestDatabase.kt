package com.powertracker.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.SettingsRepository
import com.powertracker.data.db.AppDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
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

    /**
     * While true, work runs on the thread that hands it over, at once. A suspending query then answers before its caller
     * has suspended, as it does when this rule's thread finishes first (a busy machine): the caller goes on without
     * leaving the frame, or the tap, it was called from.
     */
    @Volatile
    var answerAtOnce = false

    @Volatile
    private var gate: java.util.concurrent.CountDownLatch? = null

    /**
     * Work handed over from now on waits, on this rule's threads, until the returned function is called. Call it in a
     * `finally`; should a test end without it, [after] lets the work go before waiting for it.
     */
    fun holdWork(): () -> Unit {
        val latch = java.util.concurrent.CountDownLatch(1)
        gate = latch
        return {
            gate = null
            latch.countDown()
        }
    }

    override fun before() {
        // The settings store is one per process and outlives each test's database, where session ids start again at 1:
        // a session finished in one test must not open another test's Today on the finish summary.
        runBlocking { SettingsRepository(ApplicationProvider.getApplicationContext<Context>()).setFinishedSession(null) }
        val threads = Executors.newCachedThreadPool { work -> Thread(work, "test-database").apply { isDaemon = true } }
        executor = threads
        val counted = java.util.concurrent.Executor { work ->
            running.incrementAndGet()
            if (answerAtOnce) {
                try {
                    work.run()
                } finally {
                    running.decrementAndGet()
                }
                return@Executor
            }
            val held = gate
            try {
                threads.execute {
                    try {
                        held?.await()
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
        // A held test that failed before letting its work go: let it go, or the wait below reports a query still running
        // instead of the test's own failure.
        gate?.countDown()
        gate = null
        threads.shutdown()
        check(threads.awaitTermination(10, TimeUnit.SECONDS)) { "a query was still running 10 s after the test" }
        opened?.close()
    }
}
