package com.alananasss.kittytune.core

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProcessInitializerTest {
    @Test fun simultaneousCallersPerformWorkOnceAndWaitUntilComplete() {
        val once = ProcessInitializer()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val returned = AtomicInteger()
        val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val threads = List(8) { Thread {
            try {
                once.initialize { calls.incrementAndGet(); entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
                check(once.isInitialized)
                returned.incrementAndGet()
            } catch(t: Throwable) { failures.add(t) }
        } }
        threads.forEach { it.start() }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        try { assertFalse(once.isInitialized); assertEquals(0, returned.get()) }
        finally { release.countDown(); threads.forEach { it.join(5000) } }
        assertTrue(failures.toString(), failures.isEmpty())
        assertEquals(1, calls.get()); assertEquals(8, returned.get())
        once.initialize { fail("already initialized") }
    }

    @Test fun failedInitializationCanRetry() {
        val once = ProcessInitializer()
        try { once.initialize { error("disk failure") }; fail() } catch(_: IllegalStateException) {}
        assertFalse(once.isInitialized)
        once.initialize {}
        assertTrue(once.isInitialized)
    }
}
